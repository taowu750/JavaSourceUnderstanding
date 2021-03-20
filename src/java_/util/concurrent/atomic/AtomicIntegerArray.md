`java.util.concurrent.atomic.AtomicIntegerArray`类的声明如下：
```java
public class AtomicIntegerArray implements java.io.Serializable
```
一个 `int` 数组，其中的元素可以原子化地更新。关于原子变量的属性描述，请参见 `java.util.concurrent.atomic` 包规范。

# 1. 成员字段
```java
private static final long serialVersionUID = 2862133569453604235L;

private static final Unsafe unsafe = Unsafe.getUnsafe();

// int 数组中第一个元素的偏移地址
private static final int base = unsafe.arrayBaseOffset(int[].class);
// n = 1 << shift，n 是数组元素大小
private static final int shift;
private final int[] array;

static {
    // int 数组中一个元素占用的字节大小
    int scale = unsafe.arrayIndexScale(int[].class);
    // 保证大小是 2 的幂
    if ((scale & (scale - 1)) != 0)
        throw new Error("data type scale not a power of two");
    shift = 31 - Integer.numberOfLeadingZeros(scale);
}
```
参见 [Unsafe][unsafe] 类的说明。

# 2. 构造器
```java
public AtomicIntegerArray(int length) {
    array = new int[length];
}

public AtomicIntegerArray(int[] array) {
    // final 字段保证可见性
    this.array = array.clone();
}
```
参见 [关键字：final详解][final]。

# 3. 方法

## 3.1 checkedByteOffset
```java
// 检查并计算数组第 i 个元素的起始地址
private long checkedByteOffset(int i) {
    if (i < 0 || i >= array.length)
        throw new IndexOutOfBoundsException("index " + i);

    return byteOffset(i);
}

// 根据数组下标计算对应的元素起始地址
private static long byteOffset(int i) {
    return ((long) i << shift) + base;
}
```

## 3.2 toString
```java
public String toString() {
    int iMax = array.length - 1;
    if (iMax == -1)
        return "[]";

    StringBuilder b = new StringBuilder();
    b.append('[');
    for (int i = 0; ; i++) {
        b.append(getRaw(byteOffset(i)));
        if (i == iMax)
            return b.append(']').toString();
        b.append(',').append(' ');
    }
}
```

## 3.3 length
```java
public final int length() {
    return array.length;
}
```

## 3.4 get
```java
// 获取数组位置 i 的当前值。
public final int get(int i) {
    return getRaw(checkedByteOffset(i));
}

private int getRaw(long offset) {
    return unsafe.getIntVolatile(array, offset);
}
```

## 3.5 set
```java
// 将数组位置 i 的元素设置为给定值。
public final void set(int i, int newValue) {
    unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
}
```

## 3.6 lazySet
```java
/*
lazySet 不会立刻(但是最终会)修改旧值，别的线程看到新值的时间会延迟一些。
lazySet 比 set 具有性能优势，但是使用场景很有限，在编写非阻塞数据结构微调代码时可能会很有用。

其语义是保证写的内容不会与之前的任何写的内容重新排序，但可能会与后续的操作重新排序（或者等价地，可能对其他线程不可见），
直到其他一些易失性写或同步操作发生）。

lazySet 提供了一个前置的 store-store 屏障（这在当前的平台上要么是无操作，要么是非常快速地的），
但没有 store-load 屏障（这通常是 volatile-write 的耗时操作）。
*/
public final void lazySet(int i, int newValue) {
    unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
}
```
参见 StackOverflow 上的[讨论][lazyset]。

## 3.7 getAndSet
```java
// 原子化地将数组位置 i 的元素设置为给定值，并返回旧值。
public final int getAndSet(int i, int newValue) {
    return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
}
```

## 3.8 compareAndSet
```java
// 如果数组位置 i 的元素==expect，则原子地将该值设置为给定的更新值。返回是否更新成功
public final boolean compareAndSet(int i, int expect, int update) {
    return compareAndSetRaw(checkedByteOffset(i), expect, update);
}

private boolean compareAndSetRaw(long offset, int expect, int update) {
    return unsafe.compareAndSwapInt(array, offset, expect, update);
}
```

## 3.9 weakCompareAndSet
```java
/*
如果数组位置 i 的元素==expect，则原子地将该值设置为给定的更新值。返回是否更新成功。
可能会杂乱地失败，并且不提供排序保证，所以只有在很少情况下才适合作为 compareAndSet 的替代方法。
*/
public final boolean weakCompareAndSet(int i, int expect, int update) {
    return compareAndSet(i, expect, update);
}
```

## 3.10 increment
```java
// 将数组位置 i 的元素原子地递增 1 并返回旧值。
public final int getAndIncrement(int i) {
    return getAndAdd(i, 1);
}

// 将数组位置 i 的元素原子地递增 1 并返回新值。
public final int incrementAndGet(int i) {
    return getAndAdd(i, 1) + 1;
}
```

## 3.11 decrement
```java
// 将数组位置 i 的元素原子地递减 1 并返回旧值。
public final int getAndDecrement(int i) {
    return getAndAdd(i, -1);
}

// 将数组位置 i 的元素原子地递减 1 并返回新值。
public final int decrementAndGet(int i) {
    return getAndAdd(i, -1) - 1;
}
```

## 3.12 add
```java
// 将 delta 原子地加到数组位置 i 上，并返回旧值。
public final int getAndAdd(int i, int delta) {
    return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
}

// 将 delta 原子地加到数组位置 i 上，并返回新值。
public final int addAndGet(int i, int delta) {
    return getAndAdd(i, delta) + delta;
}
```

## 3.13 update
```java
/*
用应用给定函数的结果原子化地更新数组位置 i 的元素，返回旧值。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}

/*
用应用给定函数的结果原子化地更新数组位置 i 的元素，返回新值。

该函数需要是无副作用的，因为当线程之间的竞争导致尝试更新失败时，该函数可能会被重新应用。
*/
public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```

## 3.14 accumulate
```java
/*
使用给定函数计算数组位置 i 的元素和给定值，并将结果原子化地更新到数组位置 i 上，返回旧值。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前值作为第一个参数，以给定的更新作为第二个参数来使用。
*/
public final int getAndAccumulate(int i, int x,
                                  IntBinaryOperator accumulatorFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}

/*
使用给定函数计算数组位置 i 的元素和给定值，并将结果原子化地更新到数组位置 i 上，返回新值。

该函数应该是无副作用的，因为当线程之间的争夺导致尝试更新失败时，该函数可能会被重新应用。
该函数以当前值作为第一个参数，以给定的更新作为第二个参数来使用。
*/
public final int accumulateAndGet(int i, int x,
                                  IntBinaryOperator accumulatorFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```


[unsafe]: ../../../../sun_/misc/Unsafe.md
[final]: ../关键字：final详解.md
[lazyset]: https://stackoverflow.com/questions/1468007/atomicinteger-lazyset-vs-set