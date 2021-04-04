`java.util.concurrent.atomic.Striped64` 抽象类声明如下：
```java
@SuppressWarnings("serial")
abstract class Striped64 extends Number
```
一个包私有抽象类，用来支持累加器的并发组件。`Striped64` 的设计思路是在竞争激烈的时候尽量分散竞争。

此类维护一个由原子更新的变量组成的惰性初始化表 `cells`，以及一个额外的 `base` 字段。表的大小是 2 的幂。
索引使用每个线程哈希码的掩码结果。此类中的几乎所有声明都是包私有的，可以直接由子类访问。

`cells` 元素是 `Cell` 类对象，它是 `AtomicLong` 的变体，通过 `@sun.misc.Contended`增加 padding 以防止出现伪共享。
对于大多数原子操作而言，填充带来的坏处更多，因为这些原子变量通常不规则地散布在内存中，因此彼此之间不会产生太多干扰。
但是对于在数组中的原子变量将倾向于处于相邻位置，因此在没有这种预防措施的情况下，大多数情况下它们将共享缓存行，
这会对性能产生巨大的负面影响。

单个自旋锁 `cellsBusy` 用于初始化、调整表的大小，以及使用新的 `Cell` 填充插槽。这里不需要阻塞锁，当锁不可用时，
线程会尝试其他插槽。在重试期间，竞争会增加而位置会减少，但这仍然比替代方法更好。

通过 `ThreadLocalRandom` 维护的线程探针字段（`Thread.threadLocalRandomProbe`）用作每个线程的哈希码。
这些哈希码未初始化时为零，初始化为通常不经常与其他对象冲突的值。

由于 `Cell` 相对较大，因此避免在需要它们之前创建它们（惰性初始化）。最开始时，表被初始化为大小 2。表插槽在需要之前保持为 `null`。
当执行更新操作时，失败的 CAS 表示出现了竞争或表冲突。发生冲突时，如果表大小小于“容量”（大于等于 CPU 数的 2 的幂），则表大小将增加一倍。
如果哈希槽为 `null`，并且锁可用，则会创建一个新的 `Cell`；否则，则尝试 CAS 该 `Cell` 的值。
寻找其他插槽的过程通过“二次哈希”进行，使用辅助哈希（Marsaglia XorShift）尝试查找空闲插槽。

如果初始化 `cells` 失败了，表示其他线程持有锁，则会使用 `base`。也就是说 `base` 作为 `cells` 的后备值。

表的大小是有上限的，因为当线程数量多于 CPU 时，假设每个线程都绑定在一个 CPU 上，就会存在一个完美的哈希函数将线程与槽位映射，
从而消除碰撞。当我们达到容量时，我们通过随机改变碰撞线程的哈希码来搜索这个映射。因为搜索是随机的，
因为碰撞只有通过 CAS 失败才会被知道，所以收敛可能很慢。而且因为线程通常不会永远绑定在 CPU 上，所以可能根本不会发生。
然而，尽管有这些限制，在这些情况下，观察到的竞争率通常很低。

当曾经对其进行哈希的线程终止时，一个 `Cell` 有可能成为未使用的单元格，也有可能在扩展掩码下，将表翻倍导致没有线程对其进行哈希。
我们并不试图检测或删除这样的 `Cell`，因为对于长期运行的实例，竞争会反复出现，所以最终会再次需要这些 `Cell`；而对于短期的实例，
则无所谓。

总结下来：在实现上，`Striped64` 维护了一个 `base` 和一个 `Cell` 数组，计数线程会首先试图更新 `base` 变量（这个过程体现在子类 `LongAdder` 类中），
如果成功则退出计数，否则会认为当前竞争是很激烈的，那么就会通过 `Cell` 数组来分散计数，`Striped64` 根据线程来计算哈希，
然后将不同的线程分散到不同的 `Cell` 数组的 `index` 上，然后这个线程的计数内容就会保存在该 `Cell` 的位置上面。
基于这种设计，最后的总计数需要结合 · 以及散落在 `Cell` 数组中的计数内容。
这种设计思路类似于 Java7 的 `ConcurrentHashMap` 实现，也就是所谓的分段锁算法。

# 1. 内部类
```java
@sun.misc.Contended 
static final class Cell {
    volatile long value;
    
    Cell(long x) { value = x; }
    
    final boolean cas(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
    }

    private static final sun.misc.Unsafe UNSAFE;
    // value 字段的偏移量
    private static final long valueOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> ak = Cell.class;
            valueOffset = UNSAFE.objectFieldOffset
                (ak.getDeclaredField("value"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```
参见 [Contended][contended] 注解和 [Unsafe][unsafe] 类。

# 2. 成员字段
```java
// CPU 核心数量
static final int NCPU = Runtime.getRuntime().availableProcessors();

// Cell 表格。当非 null 时，大小为 2 的幂
transient volatile Cell[] cells;

// 基础值，主要用于无竞争时，但也可作为表初始化竞争时的后备值。通过 CAS 更新。
transient volatile long base;

// 当调整大小或创建 Cell 时，所使用的自旋锁（通过 CAS 锁定）。
// 0 表示未锁定，1 表示锁定
transient volatile int cellsBusy;

// Unsafe 实例
private static final sun.misc.Unsafe UNSAFE;
// base 字段的偏移量
private static final long BASE;
// cellsBusy 字段的偏移量
private static final long CELLSBUSY;
// Thread.threadLocalRandomProbe 字段的偏移量
private static final long PROBE;
static {
    try {
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> sk = Striped64.class;
        BASE = UNSAFE.objectFieldOffset
            (sk.getDeclaredField("base"));
        CELLSBUSY = UNSAFE.objectFieldOffset
            (sk.getDeclaredField("cellsBusy"));
        Class<?> tk = Thread.class;
        PROBE = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomProbe"));
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

# 3. 方法

## 3.1 casBase
```java
// CAS base 字段
final boolean casBase(long cmp, long val) {
    return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
}
```

## 3.2 casCellsBusy
```java
// CAS cellsBusy 字段从 0 到 1，以获取锁
final boolean casCellsBusy() {
    return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
}
```

## 3.3 getProbe
```java
// 获取当前线程的探针值。
static final int getProbe() {
    return UNSAFE.getInt(Thread.currentThread(), PROBE);
}
```

## 3.4 advanceProbe
```java
// 计算下一个探针值。
static final int advanceProbe(int probe) {
    probe ^= probe << 13;   // xorshift
    probe ^= probe >>> 17;
    probe ^= probe << 5;
    UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
    return probe;
}
```

## 3.5 longAccumulate
```java
/*
使用 fn 将 x 更新到 cells 或 base 中。处理涉及初始化、调整大小、创建新 Cell 和竞争的情况，工作原理见上文。

@param x 更新值
@param fn 将旧值和 x 组合的函数，如果为 null 则相加（这个约定避免了在 LongAdder 中需要一个额外的字段或函数）。
@param wasUncontended 如果之前的 CAS 调用失败则为 false，表示发生了线程竞争
*/
final void longAccumulate(long x, LongBinaryOperator fn,
                          boolean wasUncontended) {
    int h;
    // 获取探针值
    if ((h = getProbe()) == 0) {
        // 没有初始化则先进行初始化
        ThreadLocalRandom.current(); // force initialization
        h = getProbe();
        wasUncontended = true;
    }
    // 出现竞争，并且可以扩容，则为 true
    boolean collide = false;
    for (;;) {
        // 将字段值保存在局部变量中，因为多线程环境下原来的值随时会改变
        Cell[] as; Cell a; int n; long v;
        // 如果 cells 已经初始化过了
        if ((as = cells) != null && (n = as.length) > 0) {
            // 根据线程探针值计算它在 cells 表中的下标，再根据下标获取 Cell 对象。
            // 如果为 null，尝试创建一个新的 Cell 对象
            if ((a = as[(n - 1) & h]) == null) {
                // 如果还未锁定
                if (cellsBusy == 0) {       // Try to attach new Cell
                    // 创建包含值 x 的 Cell 对象
                    Cell r = new Cell(x);   // Optimistically create
                    // 如果获取锁成功
                    if (cellsBusy == 0 && casCellsBusy()) {
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            // 防呆检查
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                // 将创建的 Cell 对象放入指定位置
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        // 成功设值，跳出循环
                        if (created)
                            break;
                        // 失败，继续循环
                        continue;           // 插槽此时非空
                    }
                    // 获取锁失败，继续循环
                }
                collide = false;
            }
            // 否则插槽不为 null，Cell 对象已经存在。
            // 如果 CAS 之前已经失败过了，重置 wasUncontended，在重哈希后继续循环。
            // ？这里的操作可能是为了减缓线程竞争，让 CAS 失败的线程先缓一缓再试？
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            // 否则之前 CAS 还没有失败或被重置，则尝试使用更新函数（或相加） CAS Cell 的值
            else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                         fn.applyAsLong(v, x))))
                // 成功则完成操作，跳出循环
                break;
            /*
            此时 CAS 失败，出现冲突。

            如果 cells 容量大于等于 NCPU，说明已经无法扩容，只能循环继续尝试 CAS 或找下一个位置；
            或者 cells 在此判断前扩容过，则可能找到新的 null 插槽。
            */
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            // 如果 collide 为 false，表示 cells 可能还可以扩容
            else if (!collide)
                collide = true;
            // 此时 collide 为 true。
            // 尝试获取锁进行扩容
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {      // Expand table unless stale
                        // 扩容为原来的两倍大
                        Cell[] rs = new Cell[n << 1];
                        // 扩容后将旧表中的元素复制到新表的相同下标处
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                // 扩容后继续进行尝试
                continue;                   // Retry with expanded table
            }
            // 以上操作都失败后，计算下一个线程探针值，为当前线程找到下一个槽位
            h = advanceProbe(h);
        }
        // 否则先对 cells 进行初始化。先检查有没有其他线程干扰，然后获取锁
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    // 最开始 cells 的大小为 2
                    Cell[] rs = new Cell[2];
                    // 将 x 放到新创建的 Cell 中
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            // 初始化并设值成功，跳出循环
            if (init)
                break;
        }
        // 如果初始化失败了，表示其他线程持有锁，则尝试使用更新函数（或相加） CAS base 的值
        else if (casBase(v = base, ((fn == null) ? v + x :
                                    fn.applyAsLong(v, x))))
            // 成功则跳出循环
            break;                          // Fall back on using base
    }
}
```

## 3.6 doubleAccumulate
```java
// 和 longAccumulate 类似，只不过处理的是 double 值。
// 使用 Double.doubleToRawLongBits 和 Double.longBitsToDouble 进行 long 和 double 的位表示转换
final void doubleAccumulate(double x, DoubleBinaryOperator fn,
                            boolean wasUncontended) {
    int h;
    if ((h = getProbe()) == 0) {
        ThreadLocalRandom.current(); // force initialization
        h = getProbe();
        wasUncontended = true;
    }
    boolean collide = false;                // True if last slot nonempty
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        if ((as = cells) != null && (n = as.length) > 0) {
            if ((a = as[(n - 1) & h]) == null) {
                if (cellsBusy == 0) {       // Try to attach new Cell
                    Cell r = new Cell(Double.doubleToRawLongBits(x));
                    if (cellsBusy == 0 && casCellsBusy()) {
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            else if (a.cas(v = a.value,
                           ((fn == null) ?
                            Double.doubleToRawLongBits
                            (Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits
                            (fn.applyAsDouble
                             (Double.longBitsToDouble(v), x)))))
                break;
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = advanceProbe(h);
        }
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        else if (casBase(v = base,
                         ((fn == null) ?
                          Double.doubleToRawLongBits
                          (Double.longBitsToDouble(v) + x) :
                          Double.doubleToRawLongBits
                          (fn.applyAsDouble
                           (Double.longBitsToDouble(v), x)))))
            break;                          // Fall back on using base
    }
}
```


[unsafe]: ../../../../sun_/misc/Unsafe.md
[contended]: ../../../../sun_/misc/Contended.md