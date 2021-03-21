`java.util.concurrent.atomic.AtomicStampedReference`类的声明如下：
```java
public class AtomicStampedReference<V>
```
`AtomicStampedReference` 维护一个对象引用和一个整数"版本号"，它可以原子地更新。
它的内部使用 `Pair` 来存储引用及其版本号。

`AtomicStampedReference` 主要是用来解决 CAS 中的 ABA 问题的。ABA就是：
如果假设 V 值原来是A，另一个线程修改将其先修改成 B，再修改回成 A。这样当前线程的 CAS 操作无法分辨当前 V 值是否发生过变化。

ABA 会带来什么问题呢？试想如下情况：
```
   top
    |
    V   
| Node A | --> |  Node B | --> |  Node C | --> ……
```
有一个栈中有 `top` 和节点 `A`，节点 `A` 目前位于栈顶，`top` 指针指向 `A`。现在有一个线程 P1 想要 `pop` 一个节点，
因此按照如下无锁操作进行：
```c
pop()
{
  do{
    ptr = top;            // ptr = top = NodeA
    next_prt = top->next; // next_ptr = NodeB
  } while(CAS(top, ptr, next_ptr) != true);
  return ptr;   
}
```

而线程 P2 在执行 CAS 操作之前打断了 P1，并对栈进行了一系列的 `pop` 和 `push` 操作，使栈变为如下结构：
```
   top
    |
    V  
| Node A | --> | Node C | --> ……
```
P2 首先 `pop` 出 `NodeA` 和 `NodeB`，之后又对 `NodeA` 进行了 `push`。

这时 P1 又开始继续运行，在执行 CAS 操作时，由于 `top` 依旧指向的是 `NodeA`，
因此将 `top` 的值修改为了 `NodeB` ，这时栈结构如下：
```
    top
     |
     V
 | Node B |     |  Node A | --> | Node C | ……
```
经过 CAS 操作后，`top` 指针错误的指向了 `NodeB` 而不是 `NodeC`。

再想象一个电商中的场景：
1. 商品 Y 的库存是 10（A）
2. 用户 m 购买了 5 件（B）
3. 运营人员乙补货 5 件（A）（乙执行了一个 ABA 操作）
4. 运营人员甲看到库存还是 10，就认为一件也没有卖出去（不考虑交易记录），其实已经卖出去了 5 件。

`AtomicStampedReference` 解决 ABA 问题的办法是增加一个版本号信息，每次变量更新时把版本号加 1，
那么 `A-B-A` 就会变成 `1A-2B-3A`。通过版本号我们就能够区分出变量有没有被别的线程动过，
从而避免不正确的操作。

# 1. 内部类

## 1.1 Pair
```java
private static class Pair<T> {
    // 注意都是 final 字段。这表示只能通过创建新的 Pair 对象进行更新
    final T reference;
    final int stamp;

    private Pair(T reference, int stamp) {
        this.reference = reference;
        this.stamp = stamp;
    }

    static <T> Pair<T> of(T reference, int stamp) {
        return new Pair<T>(reference, stamp);
    }
}
```

# 2. 成员字段
```java
// 获取 Unsafe 实例
private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
// 计算 pair 字段的偏移量
private static final long pairOffset =
    objectFieldOffset(UNSAFE, "pair", AtomicStampedReference.class);

static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                              String field, Class<?> klazz) {
    try {
        return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
    } catch (NoSuchFieldException e) {
        // Convert Exception to corresponding Error
        NoSuchFieldError error = new NoSuchFieldError(field);
        error.initCause(e);
        throw error;
    }
}

private volatile Pair<V> pair;
```

# 3. 构造器
```java
public AtomicStampedReference(V initialRef, int initialStamp) {
    pair = Pair.of(initialRef, initialStamp);
}
```

# 4. 方法

## 4.1 getReference
```java
public V getReference() {
    return pair.reference;
}
```

## 4.2 getStamp
```java
public int getStamp() {
    return pair.stamp;
}
```

## 4.3 get
```java
/*
返回引用和版本号的当前值。典型的用法是：
    int[1] holder;
    ref = v.get(holder);
*/
public V get(int[] stampHolder) {
    Pair<V> pair = this.pair;
    stampHolder[0] = pair.stamp;
    return pair.reference;
}
```

## 4.4 set
```java
// 当新的引用和当前引用不同，或新的版本号和当前版本号不同，进行更新
public void set(V newReference, int newStamp) {
    Pair<V> current = pair;
    if (newReference != current.reference || newStamp != current.stamp)
        // 使用新的 Pair 对象进行更新
        this.pair = Pair.of(newReference, newStamp);
}
```

## 4.5 compareAndSet
```java
// 如果当前引用==expectedReference，且当前版本号==expectedStamp，则原子地将它们都设置为给定的更新值。
public boolean compareAndSet(V   expectedReference,
                             V   newReference,
                             int expectedStamp,
                             int newStamp) {
    Pair<V> current = pair;
    return
        // 期望的引用和版本号和当前值相同才可以进行更新
        expectedReference == current.reference &&
        expectedStamp == current.stamp &&
        // 如果新的引用和版本号和当前值相同，则不需要更新
        ((newReference == current.reference &&
          newStamp == current.stamp) ||
         // 原子地使用新的 Pair 对象进行更新
         casPair(current, Pair.of(newReference, newStamp)));
}

private boolean casPair(Pair<V> cmp, Pair<V> val) {
    return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
}
```

## 4.6 weakCompareAndSet
```java
/*
如果当前引用==expectedReference，且当前版本号==expectedStamp，则原子地将它们都设置为给定的更新值。

可能会杂乱地失败，并且不提供排序保证，所以只有在很少情况下才适合作为 compareAndSet 的替代方法。
*/
public boolean weakCompareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
    return compareAndSet(expectedReference, newReference,
                         expectedStamp, newStamp);
}
```

## 4.7 attemptStamp
```java
/*
如果当前引用==expectedReference，则原子地将版本号的值设置为给定的更新值。

调用可能失败（返回 false），但当当前引用==expectedReference且没有其他线程也在尝试设置该值时，
重复调用将最终成功。
*/
public boolean attemptStamp(V expectedReference, int newStamp) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        (newStamp == current.stamp ||
         casPair(current, Pair.of(expectedReference, newStamp)));
}
```
