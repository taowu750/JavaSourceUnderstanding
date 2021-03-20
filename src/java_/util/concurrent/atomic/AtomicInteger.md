`java.util.concurrent.AtomicInteger`类的声明如下：
```java
public class AtomicInteger extends Number implements java.io.Serializable
```
一个可以原子化更新的 `int` 值。关于原子变量的属性描述，请参见 `java.util.concurrent.atomic` 包规范。

`AtomicInteger` 用于原子增量计数器等应用中，但不是用来替代 `Integer` 的。此类扩展了 `Number`，
允许处理基于数值的类的工具和实用程序统一访问。

# 1. 成员字段
```java
private static final long serialVersionUID = 6214790243416807050L;

// 获取 Unsafe 类
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;

static {
    // 获取 value 字段的偏移量
    try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

// 使用 volatile 保证可见性和有序性
private volatile int value;
```
参见 [Unsafe][unsafe] 类的说明。

# 2. 构造器
```java
public AtomicInteger(int initialValue) {
    value = initialValue;
}

public AtomicInteger() {
}
```

# 3. 方法

## 3.1 toString
```java
public String toString() {
    return Integer.toString(get());
}
```

## 3.2 Number 方法
```java
public int intValue() {
    return get();
}

public long longValue() {
    return (long)get();
}

public float floatValue() {
    return (float)get();
}

public double doubleValue() {
    return (double)get();
}
```

## 3.3 get
```java
public final int get() {
    return value;
}
```

## 3.4 set
```java
public final void set(int newValue) {
    value = newValue;
}
```

## 3.5 lazySet
```java
/*
lazySet 不会立刻(但是最终会)修改旧值，别的线程看到新值的时间会延迟一些。
lazySet 比 set 具有性能优势，但是使用场景很有限，在编写非阻塞数据结构微调代码时可能会很有用。

其语义是保证写的内容不会与之前的任何写的内容重新排序，但可能会与后续的操作重新排序（或者等价地，可能对其他线程不可见），
直到其他一些易失性写或同步操作发生）。

lazySet 提供了一个前置的 store-store 屏障（这在当前的平台上要么是无操作，要么是非常快速地的），
但没有 store-load 屏障（这通常是 volatile-write 的耗时操作）。
*/
public final void lazySet(int newValue) {
    unsafe.putOrderedInt(this, valueOffset, newValue);
}
```
参见 StackOverflow 上的[讨论][lazyset]。

## 3.6 getAndSet
```java
// 原子化地设置为给定值并返回旧值。
public final int getAndSet(int newValue) {
    return unsafe.getAndSetInt(this, valueOffset, newValue);
}
```

## 3.7 compareAndSet
```java
// 如果当前值==expect，则原子地将该值设置为给定的更新值。返回是否更新成功
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

## 3.8 weakCompareAndSet
```java
/*
如果当前值==expect，则原子地将该值设置为给定的更新值。
可能会杂乱地失败，并且不提供排序保证，所以只有在很少情况下才适合作为 compareAndSet 的替代方法。
*/
public final boolean weakCompareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

## 3.9 increment
```java
// 将当前值原子地递增 1 并返回旧值。相当于 i++
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}

// 将当前值原子地递增 1 并返回新值。相当于 ++i
public final int incrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}
```

## 3.10 decrement
```java
// 将当前值原子地递减 1 并返回旧值。相当于 i--
public final int getAndDecrement() {
    return unsafe.getAndAddInt(this, valueOffset, -1);
}

// 将当前值原子地递减 1 并返回新值。相当于 --i
public final int decrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
}
```

## 3.11 add
```java
// 原子地将给定值加到当前值上，并返回旧值
public final int getAndAdd(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta);
}

// 原子地将给定值加到当前值上，并返回新值
public final int addAndGet(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
}
```

## 3.12 update
```java
/*
用应用给定函数的结果原子化地更新当前值，返回旧值。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final int getAndUpdate(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}

/*
用应用给定函数的结果原子化地更新当前值，返回新值。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final int updateAndGet(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

## 3.13 accumulate
```java
/*
将给定函数应用于当前值和给定值的结果原子化地更新当前值，返回旧值。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前值作为第一个参数，以给定的更新作为第二个参数来使用。
*/
public final int getAndAccumulate(int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}

/*
将给定函数应用于当前值和给定值的结果原子化地更新当前值，返回新值。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前值作为第一个参数，以给定的更新作为第二个参数来使用。
*/
public final int accumulateAndGet(int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```


[unsafe]: ../../../../sun_/misc/Unsafe.md
[lazyset]: https://stackoverflow.com/questions/1468007/atomicinteger-lazyset-vs-set