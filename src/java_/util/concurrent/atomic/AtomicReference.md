`java.util.concurrent.atomic.AtomicReference`类的声明如下：
```java
public class AtomicReference<V> implements java.io.Serializable
```
一个可以原子化更新的对象引用。关于原子变量的属性描述，请参见 `java.util.concurrent.atomic` 包规范。

# 1. 成员字段
```java
private static final long serialVersionUID = -1848883965231344442L;

// 获取 Unsafe 类
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;

static {
    // 获取 value 字段的偏移量
    try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicReference.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

private volatile V value;
```
参见 [Unsafe][unsafe] 类的说明。

# 2. 构造器
```java
public AtomicReference(V initialValue) {
    value = initialValue;
}

public AtomicReference() {
}
```

# 3. 方法

## 3.1 get
```java
public final V get() {
    return value;
}
```

## 3.2 set
```java
public final void set(V newValue) {
    value = newValue;
}
```

## 3.3 lazySet
```java
/*
lazySet 不会立刻(但是最终会)修改旧引用，别的线程看到新引用的时间会延迟一些。
lazySet 比 set 具有性能优势，但是使用场景很有限，在编写非阻塞数据结构微调代码时可能会很有用。

其语义是保证写的内容不会与之前的任何写的内容重新排序，但可能会与后续的操作重新排序（或者等价地，可能对其他线程不可见），
直到其他一些易失性写或同步操作发生）。

lazySet 提供了一个前置的 store-store 屏障（这在当前的平台上要么是无操作，要么是非常快速地的），
但没有 store-load 屏障（这通常是 volatile-write 的耗时操作）。
*/
public final void lazySet(V newValue) {
    unsafe.putOrderedObject(this, valueOffset, newValue);
}
```
参见 StackOverflow 上的[讨论][lazyset]。

## 3.4 compareAndSet
```java
// 如果当前引用==expect，则原子地将该引用设置为给定的 update。返回是否更新成功
public final boolean compareAndSet(V expect, V update) {
    return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
}
```

## 3.5 weakCompareAndSet
```java
/*
如果当前引用==expect，则原子地将该引用设置为给定的 update。返回是否更新成功
可能会杂乱地失败，并且不提供排序保证，所以只有在很少情况下才适合作为 compareAndSet 的替代方法。
*/
public final boolean weakCompareAndSet(V expect, V update) {
    return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
}
```

## 3.6 getAndSet
```java
// 原子化地设置为给定引用并返回旧引用。
@SuppressWarnings("unchecked")
public final V getAndSet(V newValue) {
    return (V)unsafe.getAndSetObject(this, valueOffset, newValue);
}
```

## 3.7 update
```java
/*
使用当前引用作为 updateFunction 的参数，并将结果更新当前值，返回旧引用。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final V getAndUpdate(UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get();
        next = updateFunction.apply(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}

/*
使用当前引用作为 updateFunction 的参数，并将结果更新当前值，返回新引用。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final V updateAndGet(UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get();
        next = updateFunction.apply(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

## 3.8 accumulate
```java
/*
使用给定函数计算当前引用和 x，并将结果原子化地更新当前引用，返回旧引用。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前引用作为第一个参数，以 x 作为第二个参数来使用。
*/
public final V getAndAccumulate(V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get();
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}

/*
使用给定函数计算当前引用和 x，并将结果原子化地更新当前引用，返回旧引用。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前引用作为第一个参数，以 x 作为第二个参数来使用。
*/
public final V accumulateAndGet(V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get();
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```


[unsafe]: ../../../../sun_/misc/Unsafe.md
[lazyset]: https://stackoverflow.com/questions/1468007/atomicinteger-lazyset-vs-set