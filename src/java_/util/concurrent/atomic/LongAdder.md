`java.util.concurrent.atomic.LongAdder` 类的声明如下：
```java
public class LongAdder extends Striped64 implements Serializable
```
一个并发累加器，设计思路是在竞争激烈的时候尽量分散竞争。原理是使用多个变量，共同维持一个 `long` 和。
当更新(方法 `add`)出现线程竞争时，变量集可以动态增长以减少争用。方法 `sum`(或者，等价的 `longValue`)各个变量的当前总和。

当多个线程更新一个共同的总和时，这个类通常比 `AtomicLong` 更高效。通常这个总和用于收集统计数据等目的，而不是用于细粒度的同步控制。
在低更新争用下，这两个类具有类似的特性。但在高争用下，`LongAdder` 的预期吞吐量明显提高，但代价是空间消耗较大。

`LongAdder` 可以和 `java.util.concurrent.ConcurrentHashMap` 一起使用，组合成一个可扩展的频率图（直方图或 Multiset 的一种形式）。
例如，要在 `ConcurrentHashMap<String,LongAdder> freqs` 中添加一个计数，如果还没有出现，则先进行初始化，
可以使用 `freqs.computeIfAbsent(k -> new LongAdder()).increment()`。

这个类扩展了 `Number`，但没有定义 `equals`、`hashCode` 和 `compareTo` 等方法。因为它会被经常改变，所以作为集合键没有用。

底层原理及实现参见 [Striped64][striped64] 抽象类。

# 1. 内部类
```java
// 序列化代理类，用于避免在序列化中引用非公共的 Striped64 父类。
private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final long value;

    SerializationProxy(LongAdder a) {
        // 使用 a 的当前和初始化
        value = a.sum();
    }

    private Object readResolve() {
        LongAdder a = new LongAdder();
        a.base = value;
        return a;
    }
}
```

# 2. 方法

## 2.1 add
```java
public void add(long x) {
    Cell[] as; long b, v; int m; Cell a;
    // 如果 cells 已经初始化，则使用 cells。
    // 否则先尝试将值更新到 base 中，失败表示出现线程竞争，此时会使用 cells
    if ((as = cells) != null || !casBase(b = base, b + x)) {
        // 表示接下来的 CAS 操作是否失败
        boolean uncontended = true;
        // 如果根据线程探针哈希码找到了对应的 Cell，并且 CAS 成功，则返回；
        // 否则，使用父类的 longAccumulate 方法进行处理
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[getProbe() & m]) == null ||
            !(uncontended = a.cas(v = a.value, v + x)))
            longAccumulate(x, null, uncontended);
    }
}
```

## 2.2 increment
```java
public void increment() {
    add(1L);
}
```

## 2.3 decrement
```java
public void decrement() {
    add(-1L);
}
```

## 2.4 sum
```java
/*
返回当前的总和。返回的值不是一个原子快照；在没有并发更新的情况下调用会返回一个准确的结果，
但在计算总和时发生的并发更新可能不会被纳入。
*/
public long sum() {
    Cell[] as = cells; Cell a;
    // 计算当前 base 和 cells 的总和
    long sum = base;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
```

## 2.5 reset
```java
/*
重置变量，使总和为零。这个方法可能是创建一个新的加法器的一个有用的替代方法，但只有在没有并发更新的情况下才有效。
因为这个方法本质上是很麻烦的，所以只有在知道没有线程并发更新的情况下才可以使用。
*/
public void reset() {
    Cell[] as = cells; Cell a;
    // 将 base 和 cells 都置 0
    base = 0L;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                a.value = 0L;
        }
    }
}
```

## 2.6 sumThenReset
```java
/*
实际上等同于 reset 后 sum。本方法可以应用于多线程计算之间的静止点（即没有线程竞争的情况）。如果有更新与本方法同时进行，
返回的值不保证是重置前的最终值。
*/
public long sumThenReset() {
    Cell[] as = cells; Cell a;
    long sum = base;
    base = 0L;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null) {
                sum += a.value;
                a.value = 0L;
            }
        }
    }
    return sum;
}
```

## 2.7 toString
```java
public String toString() {
    return Long.toString(sum());
}
```

## 2.8 Number 方法
```java
public long longValue() {
    return sum();
}

public int intValue() {
    return (int)sum();
}

public float floatValue() {
    return (float)sum();
}

public double doubleValue() {
    return (double)sum();
}
```

## 2.9 序列化
```java
// 返回一个代表当前对象状态的 SerializationProxy。
private Object writeReplace() {
    return new SerializationProxy(this);
}

private void readObject(java.io.ObjectInputStream s)
    throws java.io.InvalidObjectException {
    throw new java.io.InvalidObjectException("Proxy required");
}
```


[striped64]: Striped64.md