`java.util.concurrent.ConcurrentHashMap`类的声明如下：
```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable
```

# 0. 介绍

## 0.1 基本信息

一个支持检索和更新的高并发的哈希表。该类服从与 `Hashtable` 相同的功能规范，并包含与 `Hashtable` 的每个方法相对应的方法版本。
此类的操作都是线程安全的，检索操作并不需要锁定，也不会锁定整个表。在依赖其线程安全但不依赖其同步细节的程序中，
该类完全可以与 `Hashtable` 互操作。

检索操作（包括 `get`）一般不会阻塞，所以可能会与更新操作（包括 `put` 和 `remove`）重叠。
检索反映的是最近完成的更新操作在开始时保持的结果。更正式地说，一个给定键的更新操作与该键的任何(非 `null`)检索都有一个 happens-before 的关系，
报告更新后的值。

对于集合操作，如 `putAll` 和 `clear`，并发检索可能只反映了某些条目的插入或删除。类似地，迭代器、
`Spliterator` 和 `Enumerations` 都会返回反映哈希表在迭代器/枚举创建时或创建后某个时刻的状态的元素。
它们不会抛出 `ConcurrentModificationException`。然而，迭代器被设计为一次只能由一个线程使用。

请记住，包括 `size`、`isEmpty` 和 `containsValue` 在内的集合状态方法的结果通常只在 `Map` 没有在其他线程中进行并发更新时才有用。
这些方法的结果反映了瞬时状态，这些状态可以用于监控或估计目的，但不能用于程序控制。

当碰撞过多时（即具有不同哈希码的键落入哈希表中同一存储桶），表会被动态扩展，预计平均效果是每个映射大约对应两个存储桶
（对应于 0.75 的负载因子阈值来扩容）。随着映射的添加和删除，这个平均值周围可能会有很大的差异，但总体而言，
这保持了一个普遍接受的哈希表的时间/空间权衡。然而，调整这个或任何其他类型的哈希表的大小可能是一个相对缓慢的操作。
在可能的情况下，最好提供一个估计大小 `initialCapacity` 作为构造函数参数。
另外一个可选的 `loadFactor` 构造参数提供了进一步的方法来定制初始表容量，通过指定表密度来计算给定元素数的空间分配量。

另外，为了与该类以前的版本兼容，构造函数可以选择性地指定一个预期的并发量级别（`concurrencyLevel`）作为内部大小的额外提示。
为了改善影响，当键是 `Comparable` 时，这个类可以使用键之间的比较顺序来较少哈希碰撞的影响。

当只想要查看键时，可以使用 `newKeySet()` 或 `newKeySet(int)` 创建一个 `ConcurrentHashMap` 的 `Set` 视图，
也可以使用 `keySet(Object)` 简单地查看。

使用 `java.util.concurrent.atomic.LongAdder` 作为值，并通过 `computeIfAbsent` 进行初始化，
可以将 `ConcurrentHashMap` 作为可扩展的频率 `Map`（直方图或 Multiset 的一种形式）。
例如，要向 `ConcurrentHashMap<String,LongAdder> freqs` 添加一个计数，可以使用 `freqs.computeIfAbsent(k -> new LongAdder()).increment()`。

这个类和它的视图和迭代器实现了 `Map` 和 `Iterator` 接口的所有可选方法。像 `Hashtable` 一样，但与 `HashMap` 不同的是，
这个类不允许使用 `null` 作为键或值。

`ConcurrentHashMap` 支持一组顺序和并行的批量操作（`forEach`、`search`、`reduce`），与大多数 `Stream` 方法不同的是，这些操作是并发安全的。
这些批量操作接受一个并行度阈值参数，如果估计当前映射大小小于给定的阈值，则按顺序进行方法。使用 `Long.MAX_VALUE` 会抑制所有的并行性。
使用 1，会将其分割成足够多的子任务来充分利用 `ForkJoinPool.commonPool()`，从而达到最大的并行性。
通常情况下，你最初会选择这些极端值中的一个，然后测量使用中间值的性能，以权衡开销与吞吐量。

批量操作的并发属性与 `ConcurrentHashMap` 的并发属性相同。任何从 `get(key)` 和相关访问方法返回的非 `null` 结果，
都与相关的插入或更新有 happens-before 的关系。`ConcurrentHashMap` 中的键和值永远不会是 `null` 的。

## 0.2 底层实现

这个哈希表的主要设计目标是保持并发可读性（典型的是方法 `get()`，也包括迭代器和相关方法），同时尽量减少更新操作的线程竞争。
次要目标是保持空间消耗与 `java.util.HashMap` 差不多或更好，并支持大量线程在空表上的高初始插入率。

`ConcurrentHashMap` 通常作为一个拉链哈希表。每个键值对都保存在一个 `Node` 中。大多数节点都是基本 `Node` 类的实例，
具有 `hash、key、value` 和 `next` 字段。除了 `Node` 外，还有各种用途的其他节点类。
`TreeNode` 是红黑树的节点，`TreeBin` 存放 `TreeNode` 红黑树的根。`ForwardingNode` 在扩容时被放置在存储桶开头。
`ReservationNode` 在 `computeIfAbsent` 和相关方法中计算值时被用作占位符。
`TreeBin`、`ForwardingNode` 和 `ReservationNode` 节点不持有一般的键、值或哈希码，在搜索等过程中很容易区分。
它们有负的散列字段和空的键和值字段。这些特殊节点要么不常见，要么是短暂存在的，所以携带一些未使用的字段没什么影响。

表在第一次插入时被惰性初始化为 2 的幂。 表中的每个存储桶通常包含一个 `Node` 列表（大多数情况下，列表中只有零或一个 `Node`）。
表的访问需要进行 `volatile`/原子读、写和 CAS。我们使用 `sun.misc.Unsafe` 进行操作。

我们将 `Node` 哈希字段的前两个 bit 位用于控制目的--由于寻址约束，它无论如何都是可用的。在方法中，具有负哈希码的节点会被特殊处理或忽略。

在空存储桶中插入（通过 `put` 或其变体）第一个节点，只需将其 CAS 到存储桶中即可。这是目前大多数 key/hash 分布下 `put` 操作最常见的情况。
其他更新操作（插入、删除和替换）需要锁。 我们不想浪费空间为每个节点关联一个不同的锁对象，所以使用列表的第一个节点作为锁。
对这些锁的锁定支持依赖于内置的"同步"Monitor。

但使用列表的第一个节点作为锁本身并不足够。当一个节点被锁定时，任何更新都必须首先验证它是否还是锁定后的第一个节点，如果不是则重试。
因为新的节点总是被附加到列表中，一旦一个节点在存储桶中排在第一位，它就会一直排在第一位，直到被删除或存储桶变得无效（在扩容时）。

实践中遇到的哈希码分布有时会严重偏离均匀随机性，或者出现元素数量大于 (1<<30) 的情况，所以有些哈希码肯定会碰撞。
因此我们使用了 `TreeNode` 红黑树来缓解碰撞带来的性能损失，将搜索时间约束在 O(log N)。
`TreeNode` 也和普通节点一样维护着 `next` 遍历指针，所以可以用同样的方式在迭代器中遍历。

当「元素数量/哈希表大小」比例超过一个百分比阈值（默认是 0.75）时，表会扩容为原来的两倍。
在初始化线程分配和设置替换数组之后，任何注意到存储桶过满的线程都可以帮助扩容。但是，其他线程可能会继续执行插入等操作，而不会暂停。
在扩容的过程中，使用 `TreeBin` 可以使我们免受最坏情况下过满的影响。

通过一个接一个地将存储桶从表转移到下一个表来扩容。线程在这样做之前声明小的索引块进行转移（通过字段 `transferIndex`），
从而减少争用。字段 `sizeCtl` 中的指定容量标识确保扩容不会重叠。我们通过重用旧节点的情况来消除不必要的节点创建（因为它们的 `next` 字段不会更改）。
平均来说，当一张表大小翻一番时，只有六分之一的节点需要克隆。
转移时，旧表中的存储桶只包含一个特殊的 `ForwardingNode`（哈希字段为 `MOVED`），该节点包含下一个表。遇到转发节点时，
使用新表重新进行访问和更新操作。

每个存储桶转移都需要该存储桶的锁，在扩容时可能会暂停来等待锁。但是，由于其他线程可以加入并帮助扩容，而不是争夺锁，
因此随着扩容的进行，平均总等待时间会变短。转移操作还必须确保任何遍历操作都可以使用旧表和新表中的所有可访问存储桶。
这一定程度上是通过最后一个存储桶(`table.length-1`)到第一个存储桶进行安排的。

在看到一个转发节点时，遍历器（参见类 `Traverser`）会移动到新表，而无需重新访问节点。
为了确保即使在无序移动的情况下也不会跳过任何中间节点，遍历期间第一次遇到转发节点时会创建一个堆栈（参见类 `TableStack`），
以便在以后处理当前表时保持其位置。对这种保存/恢复机制的需求相对较少，当遇到一个转发节点时，通常会遇到更多的转发节点。
所以 `Traverser` 使用一个简单的缓存方案来避免创建那么多新的 `TableStack`。

遍历方案也适用于部分遍历（通过一个备用的 `Traverser` 构造函数），以支持分区聚合操作。另外，只读操作在转发到空表时就会放弃，
这提供了对关闭式清除的支持，该功能目前也未实现。

惰性初始化可以最大限度地减少第一次使用前的占用空间，而且当第一次插入来自 `putAll`、带 `map` 参数的构造函数或反序列化时，
还可以避免重新扩容。

元素计数是使用 `LongAdder`（用于多线程统计计数）的改编版本 `CounterCell` 来维护的。我们需要使用这个特殊的版本，
而不仅仅是使用 `LongAdder`，以便控制导致创建多个 `CounterCell` 的隐式争用感应。计数器避免了更新时的争用，但如果在并发访问过程中读得太频繁，
可能会遇到缓存崩溃。为了避免频繁读取，只有在插入一个已经有两个或更多节点的存储桶时，才会尝试在竞争中扩容。
在均匀哈希分布下，`threshold` 处出现这种情况的概率约为 13%，也就是说，只有约 1/8 的 `put` 操作需要检查 `threshold`
（扩容后，就会更少了）。

`TreeBin` 使用一种特殊的比较形式来进行搜索和相关操作，这也是我们不能使用现有集合（如 `TreeMap`）的主要原因。
`TreeBin` 中包含 `Comparable` 元素，但也可能包含不是同一个 `Comparable` 的元素，所以我们不能在它们之间调用 `compareTo`。
为了处理这个问题，红黑树先按照哈希值排序，如果可以的话，再按 `Comparable.compareTo` 排序。在查找某个节点时，
如果元素不可比较或 `compare` 为 0，可能需要同时搜索左子树和右子树。在插入时，我们使用类名和 `identityHashCode` 作为最终的比较手段。

`TreeBin` 还需要一个额外的锁定机制。读取线程即使在更新期间也总是可以进行列表遍历，而树的遍历则不可以，
主要是因为树的旋转可能会改变根节点或其链接。`TreeBin` 包括一个简单的读写锁机制，该机制作为主要的存储桶同步策略的辅助。
与插入或删除相关的结构调整会被存储桶锁定（因此不能与其他写入者冲突），但必须先等待正在进行的读取者完成。
由于只能有一个这样的等待者，我们使用一个简单的方案，使用一个单一的 `waiter` 字段来阻塞写入者。然而，读者永远不需要阻塞。
如果根锁被持有，它们就沿着缓慢的遍历路径（通过 `next` 指针）继续前进，直到锁变得可用或遍历完成，以先到者为准。
这些情况的速度并不快，但却能使总的预期吞吐量最大化。

为了保持 API 和序列化与该类以前的版本兼容，会引入一些奇怪的现象。主要是：我们保留了未被使用的 `concurrencyLevel` 的构造函数参数。
我们接受了一个 `loadFactor` 构造函数参数，但只将其应用于初始表容量。我们还声明了一个未使用的 `Segment` 类，
该类只有在序列化时才会以最小形式实例化。

另外，为了兼容以前版本的这个类，它扩展了 `AbstractMap`，但重载了所有方法，所以它只是一个无用的包袱。

此说明是为了让大家在阅读源码的时候更容易理解。首先需要阅读主要的静态声明和静态工具方法，然后是成员变量，
然后是主要的公有方法，然后是大小相关方法、树、遍历器和批量操作。

# 1. 成员字段

## 1.1 常量
```java
// 最大的表容量。32 位哈希字段的前两位用于控制目的。
private static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认容量，必须是 2 的幂
private static final int DEFAULT_CAPACITY = 16;

// 最大的（非 2 的幂）数组大小。由 toArray 和相关方法所需要。
static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

// 默认并发级别，由之前版本的 ConcurrentHashMap 使用。现在只是为了兼容性
private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

// 负载因子，仅在构造器中初始化表容量时用到。通常不使用实际的浮点值--对于相关的大小阈值，使用 n-(n>>2) 这样的表达式更为简单。
private static final float LOAD_FACTOR = 0.75f;

// 将链表转为红黑树的阈值。
static final int TREEIFY_THRESHOLD = 8;

// 将红黑树转为链表的阈值。
static final int UNTREEIFY_THRESHOLD = 6;

// 可以对存储桶进行树化的最小表容量。这个阈值下，如果一个 bin 中的节点太多，表会被扩容。
// 这个值应该至少是 4 * TREEIFY_THRESHOLD，以避免扩容和树化阈值之间的冲突。
static final int MIN_TREEIFY_CAPACITY = 64;

// 每个转移任务的最小存储桶数。范围被细分，以允许多个扩容线程。
// 这个值是一个下限，以避免扩容线程遇到过多的内存竞争。该值至少应该是 DEFAULT_CAPACITY。
private static final int MIN_TRANSFER_STRIDE = 16;

// sizeCtl 中用于指定容量标识的位数。对于 32 位数组，必须至少为 6 位。
private static int RESIZE_STAMP_BITS = 16;

// sizeCtl 中记录指定容量标识的位移。
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

// 可以帮助扩容的最大线程数（65535）。
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

// 节点哈希字段的编码，表示一些特殊节点
static final int MOVED     = -1; // 转发节点（ForwardingNode）
static final int TREEBIN   = -2; // 红黑树根结点（TreeBin）
static final int RESERVED  = -3; // 短暂占位节点（ReservationNode）

// 普通节点中可用的哈希码 bit
static final int HASH_BITS = 0x7fffffff;

// CPU 的数量
static final int NCPU = Runtime.getRuntime().availableProcessors();
```

## 1.2 成员变量
```java
// 为了序列化的兼容性
private static final ObjectStreamField[] serialPersistentFields = {
    new ObjectStreamField("segments", Segment[].class),
    new ObjectStreamField("segmentMask", Integer.TYPE),
    new ObjectStreamField("segmentShift", Integer.TYPE)
};

// 哈希表，存储桶的数组。第一次插入时惰性初始化。大小总是 2 的幂。可以直接通过迭代器访问。
transient volatile Node<K,V>[] table;

// 扩容时使用的哈希表，只在扩容时非 null
private transient volatile Node<K,V>[] nextTable;

/*
表的初始化和大小调整控制。

初始化前：
    = 0     未指定初始容量
    > 0     由指定的初始容量计算而来，再找最近的 2 的幂次方。

初始化中：
    = -1    table 正在初始化
    = -N    N 是 int 类型，分为两部分，高 15 位是指定容量标识，低 16 位表示并行扩容线程数+1（加 1 是因为 -1 已用于初始化），
            具体见 resizeStamp 函数。

初始化后：
    = n - (n >>> 2) = table.length * 0.75   扩容阈值，为 table 容量大小的 0.75 倍
*/
private transient volatile int sizeCtl;

// 在扩容的同时，要拆分的下一个表索引（加一）。
private transient volatile int transferIndex;

// 基础计数器值。主要用于无竞争时，也可作为表初始化时竞争的后备值。通过 CAS 更新。
private transient volatile long baseCount;

// 当扩容或创建 counterCells 时，所使用的自旋锁。
private transient volatile int cellsBusy;

// 计数单元表。当非 null 时，大小为 2 的幂。
private transient volatile CounterCell[] counterCells;

// 视图
private transient KeySetView<K,V> keySet;
private transient ValuesView<K,V> values;
private transient EntrySetView<K,V> entrySet;
```

## 1.3 Unsafe
```java
// Unsafe 实例
private static final sun.misc.Unsafe U;

// sizeCtl 字段偏移量
private static final long SIZECTL;
// transferIndex 字段偏移量
private static final long TRANSFERINDEX;
// baseCount 字段偏移量
private static final long BASECOUNT;
// cellsBusy 字段偏移量
private static final long CELLSBUSY;
// CounterCell.value 字段偏移量
private static final long CELLVALUE;
// Node 数组基地址
private static final long ABASE;
// Node 数组元素大小位移量
private static final int ASHIFT;

static {
    try {
        U = sun.misc.Unsafe.getUnsafe();
        Class<?> k = ConcurrentHashMap.class;
        SIZECTL = U.objectFieldOffset
            (k.getDeclaredField("sizeCtl"));
        TRANSFERINDEX = U.objectFieldOffset
            (k.getDeclaredField("transferIndex"));
        BASECOUNT = U.objectFieldOffset
            (k.getDeclaredField("baseCount"));
        CELLSBUSY = U.objectFieldOffset
            (k.getDeclaredField("cellsBusy"));
        Class<?> ck = CounterCell.class;
        CELLVALUE = U.objectFieldOffset
            (ck.getDeclaredField("value"));
        Class<?> ak = Node[].class;
        ABASE = U.arrayBaseOffset(ak);
        int scale = U.arrayIndexScale(ak);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    } catch (Exception e) {
        throw new Error(e);
    }
}
```
参见 [Unsafe.md][unsafe]。

# 2. 工具方法

## 2.1 表元素访问
```java
/*
在调整大小时，对表元素以及正在进行中的下一个表的元素使用的 volatile 访问方法。

所有对 tab 参数的使用都必须由调用者进行 null 检查。所有调用者也会预先检查 tab 的长度是否为零（或等价的检查），
从而确保任何采取哈希值形式并带有（length-1）的索引参数是一个有效的索引。

请注意，为了正确处理用户的任意并发错误，这些检查必须在局部变量上操作，这就解释了下面一些看起来很奇怪的内联赋值。

请注意，对 setTabAt 的调用应该总是发生在锁定的区域内，因此原则上只需要按顺序释放，而不需要完整的 volatile 语义，
但为了保守起见，目前被编码为 volatile 写。
*/

@SuppressWarnings("unchecked")
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}

static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}

static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
}
```

## 2.2 compare
```java
// 如果 x 的 Class 是 "class C implements Comparable<C>" 这种形式的话，则返回 Class；否则返回 null
static Class<?> comparableClassFor(Object x) {
    if (x instanceof Comparable) {
        Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
        if ((c = x.getClass()) == String.class) // bypass checks
            return c;
        if ((ts = c.getGenericInterfaces()) != null) {
            for (int i = 0; i < ts.length; ++i) {
                if (((t = ts[i]) instanceof ParameterizedType) &&
                    ((p = (ParameterizedType)t).getRawType() ==
                     Comparable.class) &&
                    (as = p.getActualTypeArguments()) != null &&
                    as.length == 1 && as[0] == c) // type arg is c
                    return c;
            }
        }
    }
    return null;
}

// 如果 x 的 Class 等于 kc，则返回 k.compareTo(x)，否则返回 0。
@SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
static int compareComparables(Class<?> kc, Object k, Object x) {
    return (x == null || x.getClass() != kc ? 0 :
            ((Comparable)k).compareTo(x));
}
```

## 2.3 spread
```java
/*
计算 key.hashCode()，并将较高位的哈希值扩散（XOR）到较低位，同时将最高位置为 0。

由于 HashMap 使用了二幂掩码，这样大多数情况下只会利用到哈希码低位的值，所以只使用当前掩码的哈希表总是会发生碰撞。
因此我们应用一个变换，将高位向下扩散。

在速度、实用性和位扩散的质量之间有一个权衡。因为很多常见的哈希码已经是合理分布的（所以并没有从扩散中获益），
而且因为我们使用红黑树来处理大哈希表的哈希碰撞，所以我们只是用最简单的方式：将高位和低位进行异或，以减少性能损耗，
以及合并最高位的影响。
*/
static final int spread(int h) {
    return (h ^ (h >>> 16)) & HASH_BITS;
}
```

## 2.4 tableSizeFor
```java
// 返回大于等于 c 的最小 2 次幂
private static final int tableSizeFor(int c) {
    // c 可能等于 n 的 2 次幂.为了保证接下来的操作正确，需要减 1
    int n = c - 1;
    n |= n >>> 1;  // n 的最高 2 位设置为 1
    n |= n >>> 2;  // n 的最高 4 位设置为 1
    n |= n >>> 4;  // n 的最高 8 位设置为 1
    n |= n >>> 8;  // n 的最高 16 位设置为 1
    n |= n >>> 16;  // n 的最高 32 位设置为 1
    // 移位和或运算的结果是，n 的最高位往后都变成了 1，再加 1 就是大于等于 n 的 2 次幂

    // n 小于 0 就返回 1；n 大于等于容量最大值就返回 MAXIMUM_CAPACITY；
    // 否则返回 n + 1，也就是大于等于 n 的 2 次幂
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

# 2. 普通节点

## .1 Node
```java
/*
键值对条目。

这个类永远不会作为一个用户可修改的 Map.Entry（即支持 setValue，见下面的 MapEntry）导出给用户，但可以用于批量任务中只读遍历。

具有负哈希字段的 Node 的子类是特殊的，包含空键和值。
*/
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    public final K getKey()       { return key; }
    public final V getValue()     { return val; }
    public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
    public final String toString(){ return key + "=" + val; }
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == (u = val) || v.equals(u)));
    }

    // 对 map.get() 的支持；在子类中重载。
    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        // 注意 ConcurrentMap 不支持 null 键和值
        if (k != null) {
            // 链表遍历，查找节点
            do {
                K ek;
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}
```

## .2 MapEntry
```java
// EntryIterator 的导出条目
static final class MapEntry<K,V> implements Map.Entry<K,V> {
    final K key; // non-null
    V val;       // non-null
    final ConcurrentHashMap<K,V> map;
    
    MapEntry(K key, V val, ConcurrentHashMap<K,V> map) {
        this.key = key;
        this.val = val;
        this.map = map;
    }
    
    public K getKey()        { return key; }
    public V getValue()      { return val; }
    public int hashCode()    { return key.hashCode() ^ val.hashCode(); }
    public String toString() { return key + "=" + val; }

    public boolean equals(Object o) {
        Object k, v; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == val || v.equals(val)));
    }

    /*
    设置条目的值并写入 Map。这里返回的值是有些随意的。由于我们不一定要跟踪异步变化，
    最近的先前的值可能与我们返回的值不同（甚至可能已经被删除，在这种情况下，put 将执行插入）。
    */
    public V setValue(V value) {
        if (value == null) throw new NullPointerException();
        V v = val;
        val = value;
        map.put(key, value);
        return v;
    }
}
```

# 3. 特殊节点

## .1 Segment
```java
// 以前版本中使用的帮助类的简化版本，为了序列化兼容性而声明。
static class Segment<K,V> extends ReentrantLock implements Serializable {
    private static final long serialVersionUID = 2249069246763182397L;
    final float loadFactor;
    Segment(float lf) { this.loadFactor = lf; }
}
```

## .2 ForwardingNode
```java
// 在转移操作中插入存储桶头部的节点。将对旧表的查询和更新操作转发到新表
static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;
    
    ForwardingNode(Node<K,V>[] tab) {
        super(MOVED, null, null, null);
        this.nextTable = tab;
    }

    Node<K,V> find(int h, Object k) {
        // 使用循环，避免对转发节点进行深度递归。
        outer: 
        for (Node<K,V>[] tab = nextTable;;) {
            Node<K,V> e; int n;
            // 如果 k 是 null，或表为空，或者指定的元素所在的存储桶为空，返回 null
            if (k == null || tab == null || (n = tab.length) == 0 ||
                (e = tabAt(tab, (n - 1) & h)) == null)
                return null;
            for (;;) {
                int eh; K ek;
                // 找到匹配的节点就返回
                if ((eh = e.hash) == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
                // 如果节点是特殊节点
                if (eh < 0) {
                    // 如果它又是一个转发节点，则使用这个转发节点继续循环
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        continue outer;
                    }
                    // 否则，使用 find 方法查找
                    else
                        return e.find(h, k);
                }
                // 移动到下一个节点。如果已经到末尾，返回 null
                if ((e = e.next) == null)
                    return null;
            }
        }
    }
}
```

## .3 ReservationNode
```java
// 一个占位符节点，用在 computeIfAbsent 和 compute 方法中
static final class ReservationNode<K,V> extends Node<K,V> {
    ReservationNode() {
        super(RESERVED, null, null, null);
    }

    Node<K,V> find(int h, Object k) {
        return null;
    }
}
```

# 4. 红黑树

## .1 TreeNode
```java
// 红黑树节点，被 TreeBin 使用
static final class TreeNode<K,V> extends Node<K,V> {
    TreeNode<K,V> parent;  // 父结点指针
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // needed to unlink next upon deletion
    boolean red;

    TreeNode(int hash, K key, V val, Node<K,V> next,
             TreeNode<K,V> parent) {
        super(hash, key, val, next);
        this.parent = parent;
    }

    Node<K,V> find(int h, Object k) {
        return findTreeNode(h, k, null);
    }

    // 以当前节点为根，查找和键 k 匹配的结点。
    // h 是 k 的哈希码；kc 是 k 的 Class
    final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
        if (k != null) {
            TreeNode<K,V> p = this;
            do  {
                int ph, dir; K pk; TreeNode<K,V> q;
                TreeNode<K,V> pl = p.left, pr = p.right;
                // 首先使用哈希码进行比较
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                // hash 码相等，就比较 key 是否相等
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                // 否则如果左子结点为 null，则移动到右子节点
                else if (pl == null)
                    p = pr;
                // 否则如果右子结点为 null，则移动到左子节点
                else if (pr == null)
                    p = pl;
                // 否则如果左右子结点都不为 null
                else if ((kc != null ||
                          // 如果 k 的形式为 “class C implements Comparable<C>”，
                          // 则返回 k 的 Class，否则返回 null。
                          (kc = comparableClassFor(k)) != null) &&
                          // 如果 pk 的 Class 不等于 kc，则返回 0；否则返回 k.compareTo(pk)。
                          // 由于之前使用过 k.equals(pk)，所以按常理来说 k.compareTo(pk) 不会返回 0
                         (dir = compareComparables(kc, k, pk)) != 0)
                    // 根据比较结果决定移到左子结点还是右子结点
                    p = (dir < 0) ? pl : pr;     
                // 否则如果不能进行 Comparable 比较，就先在右子树中查找。
                // 之所以不使用 “p = pr” 的方式，是因为此时不能确定查找键在哪个子树里面，
                // 需要都搜索一遍才能确定
                else if ((q = pr.findTreeNode(h, k, kc)) != null)
                    return q;
                // 否则如果右子树中没有找到，就移到左子结点
                else
                    p = pl;
            } while (p != null);
        }
        return null;
    }
}
```

## .2 TreeBin
```java
static final class TreeBin<K,V> extends Node<K,V>
```
包含红黑树的根结点，表示一个红黑树存储桶。

它还保持着一个读写锁：
1. 如果读取线程先获取到锁，其他读取线程也能获取这个锁；但是写入线程将被阻塞。
2. 如果写入线程先获取到锁，读取线程不会被阻塞，但此时读取线程只能使用 next 链接进行线性搜索。
写入操作，外面还会使用 `synchronized` 锁定首节点，所以两个线程不会同时进行写入。

注意，这里的写入线程指的是进行插入、删除等改变树的结构的线程，而那些只是更新值的线程不会使用这个读写锁。

红黑树相关算法细节在 [HashMap.md][hash-map] 1.2 节 `TreeNode` 中已有说明，下面不再重复表述。

### 2.7.1 成员字段
```java
// 下面是 lockState 状态值
static final int WRITER = 1; // 表示持有写锁
static final int WAITER = 2; // 表示正在等待写锁
static final int READER = 4; // 读锁的增量值。每次在读状态读取时都加上这个增量

// 根结点
TreeNode<K,V> root;
volatile TreeNode<K,V> first;
// 等待的写入者线程
volatile Thread waiter;
volatile int lockState;

private static final sun.misc.Unsafe U;
// lockState 字段偏移量
private static final long LOCKSTATE;
static {
    try {
        U = sun.misc.Unsafe.getUnsafe();
        Class<?> k = TreeBin.class;
        LOCKSTATE = U.objectFieldOffset
                (k.getDeclaredField("lockState"));
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

### 2.7.2 构造器
```java
// 创建以 b 为根结点的初始节点集的存储桶。
TreeBin(TreeNode<K,V> b) {
    super(TREEBIN, null, null, null);
    this.first = b;
    // 保存根结点
    TreeNode<K,V> r = null;
    for (TreeNode<K,V> x = b, next; x != null; x = next) {
        // 获取下一个节点
        next = (TreeNode<K,V>)x.next;
        // 取消掉左右链接，将会在下面重新构造
        x.left = x.right = null;
        // 设置根结点
        if (r == null) {
            x.parent = null;
            x.red = false;
            r = x;
        }
        else {
            K k = x.key;
            int h = x.hash;
            Class<?> kc = null;
            // 从根结点开始插入
            for (TreeNode<K,V> p = r;;) {
                int dir, ph;
                K pk = p.key;
                // 先比较哈希码
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                // 哈希码相等，就比较键是否相等
                else if ((kc == null &&
                              (kc = comparableClassFor(k)) == null) ||
                              (dir = compareComparables(kc, k, pk)) == 0)
                    // 如果不能使用 Comparable 比较，或比较结果为 0。则使用最后的比较手段：比较类名和 identityHashCode
                    dir = tieBreakOrder(k, pk);
                TreeNode<K,V> xp = p;
                // 根据 dir 的结果，让 p 移动到左结点或右结点；
                // 如果 p 等于 null，则这里就是插入 x 的位置
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    // 将 xp 和 x 连接起来
                    x.parent = xp;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    // 对插入进行平衡
                    r = balanceInsertion(r, x);
                    break;
                }
            }
        }
    }
    // 设置根结点
    this.root = r;
    // 检查红黑树结构是否合法
    assert checkInvariants(root);
}
```

### 2.7.3 方法

#### 2.7.3.1 tieBreakOrder
```java
static int tieBreakOrder(Object a, Object b) {
    int d;
    // 如果 a、b 的类名相等，则比较它们的 identityHashCode；
    // 否则比较 a、b 的类名
    if (a == null || b == null ||
            (d = a.getClass().getName().
             compareTo(b.getClass().getName())) == 0)
        // 如果 a、b 的 identityHashCode 相等，也返回 -1。
        // 这样就会导致向左子树插入
        d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                 -1 : 1);
    return d;
}
```

#### 2.7.3.2 checkInvariants
```java
// 递归地检查红黑树是否合法
static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
    TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
            tb = t.prev, tn = (TreeNode<K,V>)t.next;
    // 如果 t 的 prev 结点的 next 指针不等于 t，返回 false
    if (tb != null && tb.next != t)
        return false;
    // 如果 t 的 next 结点的 prev 指针不等于 t, 返回 false
    if (tn != null && tn.prev != t)
        return false;
    // 如果 t 不是它的 parent 结点的子结点，返回 false
    if (tp != null && t != tp.left && t != tp.right)
        return false;
    // 如果 t 的左子节点 tl 的 parent 指针不等于 t，或者 tl 的 hash 大于 t 的 hash，
    // 返回 false
    if (tl != null && (tl.parent != t || tl.hash > t.hash))
        return false;
    // 如果 t 的右子节点 tr 的 parent 指针不等于 t，或者 tr 的 hash 小于 t 的 hash，
    // 返回 false
    if (tr != null && (tr.parent != t || tr.hash < t.hash))
        return false;
    // 如果有连续的红结点，返回 false
    if (t.red && tl != null && tl.red && tr != null && tr.red)
        return false;
    // 左子结点存在，递归地检查左子结点
    if (tl != null && !checkInvariants(tl))
        return false;
    // 右子结点存在，递归地检查右子结点
    if (tr != null && !checkInvariants(tr))
        return false;
    return true;
}
```

#### 2.7.3.3 同步控制
```java
// 获取树结构改变的写入锁。
private final void lockRoot() {
    // 如果锁不被任何线程持有，则获取到读锁
    if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
        // 否则调用 contendedLock 循环获取锁
        contendedLock();
}

// 释放树结构改变的写入锁
private final void unlockRoot() {
    lockState = 0;
}

/*
循环尝试获取写入锁。只有一个线程能获取到写入锁。
 - 如果有线程获取到了读锁，则当前线程将被阻塞（unpark）
 - 如果有线程获取到了写锁，则当前线程将被阻塞（unpark）
 - 由于外面还会使用 synchronized 锁定首节点，所以不会有多个线程同时写入
*/
private final void contendedLock() {
    boolean waiting = false;
    for (int s;;) {
        // 如果没有设置 WAITER 以外的其他状态（WRITER、READER 增量）
        if (((s = lockState) & ~WAITER) == 0) {
            // 尝试将状态设置为 WRITER 写入锁状态
            if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                // 如果之前处于等待状态，将等待线程置为 null
                if (waiting)
                    waiter = null;
                return;
            }
        }
        // 否则如果没有被设置为 WAITER 状态
        else if ((s & WAITER) == 0) {
            // 添加 WAITER 状态
            if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                // 设置当前线程为等待线程
                waiting = true;
                waiter = Thread.currentThread();
            }
        }
        // 如果已经设置了 WAITER 状态，并且当前线程是设置 WAITER 状态的线程，则阻塞它。
        else if (waiting)
            LockSupport.park(this);
    }
}
```

#### 2.7.3.4 旋转
```java
/*
对 p 进行左旋转，操作如下：
    pp            pp
    |             |
    p      =>     r
   / \           / \
  l   r         p  rr
     / \       / \
    rl  rr    l  rl
*/
static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                      TreeNode<K,V> p) {
    TreeNode<K,V> r, pp, rl;
    if (p != null && (r = p.right) != null) {
        if ((rl = p.right = r.left) != null)
            rl.parent = p;
        if ((pp = r.parent = p.parent) == null)
            (root = r).red = false;
        else if (pp.left == p)
            pp.left = r;
        else
            pp.right = r;
        r.left = p;
        p.parent = r;
    }
    return root;
}

/*
对 p 进行右旋转，操作如下：
    pp            pp
    |             |
    p      =>     l
   / \           / \
  l   r         ll  p
 / \               / \
ll  lr            lr  r
*/
static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                       TreeNode<K,V> p) {
    TreeNode<K,V> l, pp, lr;
    if (p != null && (l = p.left) != null) {
        if ((lr = p.left = l.right) != null)
            lr.parent = p;
        if ((pp = l.parent = p.parent) == null)
            (root = l).red = false;
        else if (pp.right == p)
            pp.right = l;
        else
            pp.left = l;
        l.right = p;
        p.parent = l;
    }
    return root;
}
```

#### 2.7.3.5 find
```java
/*
返回匹配的节点；如果没有，则返回null。

尝试从根节点使用红黑树方式进行搜索，但是在锁不可用时继续线性搜索（使用 next 链接）。
*/
final Node<K,V> find(int h, Object k) {
    if (k != null) {
        for (Node<K,V> e = first; e != null; ) {
            int s; K ek;
            // 如果锁状态包含 WAITER 和/或 WRITER，表示有线程正在写入，或者有写入线程被阻塞，
            // 或者两者都有，则使用线性搜索
            if (((s = lockState) & (WAITER|WRITER)) != 0) {
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
                e = e.next;
            }
            // 否则将锁状态加上 READER 增量，表示设置读锁。
            // 设置失败表示可能有其他线程也在读（或在写），则循环获取锁
            else if (U.compareAndSwapInt(this, LOCKSTATE, s,
                                         s + READER)) {
                TreeNode<K,V> r, p;
                try {
                    // 使用红黑树的搜索方法，从树根开始搜索
                    p = ((r = root) == null ? null :
                         r.findTreeNode(h, k, null));
                } finally {
                    // 最后进行解锁
                    Thread w;
                    // 减去一个 READER 增量。如果之前的结果等于 READER|WAITER 并且有写入线程在等待，则唤醒它。
                    // READER|WAITER 表示之前有一个读取线程在正常读取，而有另一个写入线程被阻塞
                    if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                        (READER|WAITER) && (w = waiter) != null)
                        LockSupport.unpark(w);
                }
                return p;
            }
        }
    }
    return null;
}
```

#### 2.7.3.6 putTreeVal
```java
// 将键值对插入树中。如果 k 已经存在就返回该结点。
final TreeNode<K,V> putTreeVal(int h, K k, V v) {
    Class<?> kc = null;
    boolean searched = false;
    for (TreeNode<K,V> p = root;;) {
        int dir, ph; K pk;
        // 根结点不存在则创建新的根结点，然后跳出循环
        if (p == null) {
            first = root = new TreeNode<K,V>(h, k, v, null, null);
            break;
        }
        // 先比较哈希码
        else if ((ph = p.hash) > h)
            dir = -1;
        else if (ph < h)
            dir = 1;
        // 哈希码相等，就比较键是否相等
        else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
            // 键相等直接返回此结点
            return p;
        // 如果键也不相等，且不能使用 Comparable 比较
        else if ((kc == null &&
                  (kc = comparableClassFor(k)) == null) ||
                 (dir = compareComparables(kc, k, pk)) == 0) {
            if (!searched) {
                TreeNode<K,V> q, ch;
                // 使用 searched 标志变量，只进行一次局部搜索；
                // 因为这次的局部搜索会搜索当前整个子树，因此只需要进行一次就可以了
                searched = true;
                // 在左右子树中搜索看看能不能找到匹配结点
                if (((ch = p.left) != null &&
                     (q = ch.findTreeNode(h, k, kc)) != null) ||
                    ((ch = p.right) != null &&
                     (q = ch.findTreeNode(h, k, kc)) != null))
                    return q;
            }
            // 使用最后的比较手段：比较类名和 identityHashCode
            dir = tieBreakOrder(k, pk);
        }

        TreeNode<K,V> xp = p;
        // 根据 dir 的结果，让 p 移动到左结点或右结点；
        // 如果 p 等于 null，则需要插入新结点
        if ((p = (dir <= 0) ? p.left : p.right) == null) {
            TreeNode<K,V> x, f = first;
            first = x = new TreeNode<K,V>(h, k, v, f, xp);
            if (f != null)
                f.prev = x;
            // 根据 dir，将新结点作为左子结点或右子结点
            if (dir <= 0)
                xp.left = x;
            else
                xp.right = x;
            // 如果父节点是黑节点，则插入一个红节点不影响红黑树的性质，不需要平衡操作
            if (!xp.red)
                x.red = true;
            else {
                // 否则需要进行平衡操作。先尝试获取锁
                lockRoot();
                try {
                    root = balanceInsertion(root, x);
                } finally {
                    // 完成后解锁
                    unlockRoot();
                }
            }
            break;
        }
    }
    assert checkInvariants(root);
    return null;
}
```

#### 2.7.3.7 balanceInsertion
```java
// 平衡插入，保持红黑树的性质。参见 HashMap.TreeNode.balanceInsertion()
static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                            TreeNode<K,V> x) {
    x.red = true;
    for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
        if ((xp = x.parent) == null) {
            x.red = false;
            return x;
        }
        else if (!xp.red || (xpp = xp.parent) == null)
            return root;
        if (xp == (xppl = xpp.left)) {
            if ((xppr = xpp.right) != null && xppr.red) {
                xppr.red = false;
                xp.red = false;
                xpp.red = true;
                x = xpp;
            }
            else {
                if (x == xp.right) {
                    root = rotateLeft(root, x = xp);
                    xpp = (xp = x.parent) == null ? null : xp.parent;
                }
                if (xp != null) {
                    xp.red = false;
                    if (xpp != null) {
                        xpp.red = true;
                        root = rotateRight(root, xpp);
                    }
                }
            }
        }
        else {
            if (xppl != null && xppl.red) {
                xppl.red = false;
                xp.red = false;
                xpp.red = true;
                x = xpp;
            }
            else {
                if (x == xp.left) {
                    root = rotateRight(root, x = xp);
                    xpp = (xp = x.parent) == null ? null : xp.parent;
                }
                if (xp != null) {
                    xp.red = false;
                    if (xpp != null) {
                        xpp.red = true;
                        root = rotateLeft(root, xpp);
                    }
                }
            }
        }
    }
}
```

#### 2.7.3.8 removeTreeNode
```java
// 删除给定的节点 p
final boolean removeTreeNode(TreeNode<K,V> p) {
    TreeNode<K,V> next = (TreeNode<K,V>)p.next;
    TreeNode<K,V> pred = p.prev;  // unlink traversal pointers
    TreeNode<K,V> r, rl;
    // 将 p 的前驱节点和后继节点连接起来
    if (pred == null)
        first = next;
    else
        pred.next = next;
    if (next != null)
        next.prev = pred;
    // 树中只有一个节点，删除之后直接返回
    if (first == null) {
        root = null;
        return true;
    }
    // 如果树很小，则删除之后也可以直接返回
    if ((r = root) == null || r.right == null || // too small
        (rl = r.left) == null || rl.left == null)
        return true;
    // 加速，下面的操作将会对树的结构进行改变。详细说明参见 HashMap.TreeNode.removeTreeNode()
    lockRoot();
    try {
        TreeNode<K,V> replacement;
        TreeNode<K,V> pl = p.left;
        TreeNode<K,V> pr = p.right;
        if (pl != null && pr != null) {
            TreeNode<K,V> s = pr, sl;
            while ((sl = s.left) != null) // find successor
                s = sl;
            boolean c = s.red; s.red = p.red; p.red = c; // swap colors
            TreeNode<K,V> sr = s.right;
            TreeNode<K,V> pp = p.parent;
            if (s == pr) { // p was s's direct parent
                p.parent = s;
                s.right = p;
            }
            else {
                TreeNode<K,V> sp = s.parent;
                if ((p.parent = sp) != null) {
                    if (s == sp.left)
                        sp.left = p;
                    else
                        sp.right = p;
                }
                if ((s.right = pr) != null)
                    pr.parent = s;
            }
            p.left = null;
            if ((p.right = sr) != null)
                sr.parent = p;
            if ((s.left = pl) != null)
                pl.parent = s;
            if ((s.parent = pp) == null)
                r = s;
            else if (p == pp.left)
                pp.left = s;
            else
                pp.right = s;
            if (sr != null)
                replacement = sr;
            else
                replacement = p;
        }
        else if (pl != null)
            replacement = pl;
        else if (pr != null)
            replacement = pr;
        else
            replacement = p;
        if (replacement != p) {
            TreeNode<K,V> pp = replacement.parent = p.parent;
            if (pp == null)
                r = replacement;
            else if (p == pp.left)
                pp.left = replacement;
            else
                pp.right = replacement;
            p.left = p.right = p.parent = null;
        }

        root = (p.red) ? r : balanceDeletion(r, replacement);

        if (p == replacement) {  // detach pointers
            TreeNode<K,V> pp;
            if ((pp = p.parent) != null) {
                if (p == pp.left)
                    pp.left = null;
                else if (p == pp.right)
                    pp.right = null;
                p.parent = null;
            }
        }
    } finally {
        // 删除之后进行解锁
        unlockRoot();
    }
    assert checkInvariants(root);
    return false;
}
```

#### 2.7.3.9 balanceDeletion
```java
// 平衡删除。参见 HashMap.TreeNode.balanceDeletion()
static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                           TreeNode<K,V> x) {
    for (TreeNode<K,V> xp, xpl, xpr;;)  {
        if (x == null || x == root)
            return root;
        else if ((xp = x.parent) == null) {
            x.red = false;
            return x;
        }
        else if (x.red) {
            x.red = false;
            return root;
        }
        else if ((xpl = xp.left) == x) {
            if ((xpr = xp.right) != null && xpr.red) {
                xpr.red = false;
                xp.red = true;
                root = rotateLeft(root, xp);
                xpr = (xp = x.parent) == null ? null : xp.right;
            }
            if (xpr == null)
                x = xp;
            else {
                TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                if ((sr == null || !sr.red) &&
                    (sl == null || !sl.red)) {
                    xpr.red = true;
                    x = xp;
                }
                else {
                    if (sr == null || !sr.red) {
                        if (sl != null)
                            sl.red = false;
                        xpr.red = true;
                        root = rotateRight(root, xpr);
                        xpr = (xp = x.parent) == null ?
                            null : xp.right;
                    }
                    if (xpr != null) {
                        xpr.red = (xp == null) ? false : xp.red;
                        if ((sr = xpr.right) != null)
                            sr.red = false;
                    }
                    if (xp != null) {
                        xp.red = false;
                        root = rotateLeft(root, xp);
                    }
                    x = root;
                }
            }
        }
        else { // symmetric
            if (xpl != null && xpl.red) {
                xpl.red = false;
                xp.red = true;
                root = rotateRight(root, xp);
                xpl = (xp = x.parent) == null ? null : xp.left;
            }
            if (xpl == null)
                x = xp;
            else {
                TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                if ((sl == null || !sl.red) &&
                    (sr == null || !sr.red)) {
                    xpl.red = true;
                    x = xp;
                }
                else {
                    if (sl == null || !sl.red) {
                        if (sr != null)
                            sr.red = false;
                        xpl.red = true;
                        root = rotateLeft(root, xpl);
                        xpl = (xp = x.parent) == null ?
                            null : xp.left;
                    }
                    if (xpl != null) {
                        xpl.red = (xp == null) ? false : xp.red;
                        if ((sl = xpl.left) != null)
                            sl.red = false;
                    }
                    if (xp != null) {
                        xp.red = false;
                        root = rotateRight(root, xp);
                    }
                    x = root;
                }
            }
        }
    }
}
```

## .3 红黑树和链表转化
```java
// 将 tab 中 index 下标处的链表转化为红黑树
private final void treeifyBin(Node<K,V>[] tab, int index) {
    Node<K,V> b; int n, sc;
    if (tab != null) {
        // 如果 tab 容量小于 MIN_TREEIFY_CAPACITY，则不转红黑树，而是将其扩容
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
            tryPresize(n << 1);
        // 如果 index 处存储桶不为 null，且哈希值大于 0（说明是 Node 链表节点）
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
            // 对此节点加锁，进行树化操作。注意，读取操作是不加锁的
            synchronized (b) {
                if (tabAt(tab, index) == b) {
                    TreeNode<K,V> hd = null, tl = null;
                    // 遍历链表
                    for (Node<K,V> e = b; e != null; e = e.next) {
                        // 将 Node 转化为 TreeNode
                        TreeNode<K,V> p =
                            new TreeNode<K,V>(e.hash, e.key, e.val,
                                              null, null);
                        // 将新的节点放到末尾
                        if ((p.prev = tl) == null)
                            hd = p;
                        else
                            tl.next = p;
                        tl = p;
                    }
                    // 创建 TreeBin（它的构造器中会将链表转化为红黑树），然后重新设置到原来的下标处
                    setTabAt(tab, index, new TreeBin<K,V>(hd));
                }
            }
        }
    }
}

// 将以 b 开头的红黑树转化为链表
static <K,V> Node<K,V> untreeify(Node<K,V> b) {
    Node<K,V> hd = null, tl = null;
    for (Node<K,V> q = b; q != null; q = q.next) {
        Node<K,V> p = new Node<K,V>(q.hash, q.key, q.val, null);
        if (tl == null)
            hd = p;
        else
            tl.next = p;
        tl = p;
    }
    return hd;
}
```

# 5. 计数器

## .1 CounterCell
```java
// 一个用于计数的单元。改编自 LongAdder 和 Striped64。解释请参考它们的内部文档。
@sun.misc.Contended 
static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```
参见 [Contended][contended] 注解、[Striped64][striped64] 抽象类和 [LongAdder][long-adder]。

## .2 sumCount
```java
// 统计计数总和，将 baseCount 和 counterCells 中的所有值相加
final long sumCount() {
    CounterCell[] as = counterCells; CounterCell a;
    long sum = baseCount;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
```

## .3 fullAddCount
```java
/*
增加计数。此方法和 Striped64.longAccumulate 方法几乎一样。

@param x 新增的计数
@param wasUncontended 之前的 CAS 调用没有失败，则为 true，否则为 false
*/
private final void fullAddCount(long x, boolean wasUncontended) {
    // 此方法没有 LongBinaryOperator 参数，只进行相加操作

    int h;
    if ((h = ThreadLocalRandom.getProbe()) == 0) {
        ThreadLocalRandom.localInit();
        h = ThreadLocalRandom.getProbe();
        wasUncontended = true;
    }
    boolean collide = false;                // True if last slot nonempty
    for (;;) {
        CounterCell[] as; CounterCell a; int n; long v;
        if ((as = counterCells) != null && (n = as.length) > 0) {
            if ((a = as[(n - 1) & h]) == null) {
                if (cellsBusy == 0) {            // Try to attach new Cell
                    CounterCell r = new CounterCell(x); // Optimistic create
                    if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                        boolean created = false;
                        try {               // Recheck under lock
                            CounterCell[] rs; int m, j;
                            if ((rs = counterCells) != null &&
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
            else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                break;
            else if (counterCells != as || n >= NCPU)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 &&
                     U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                try {
                    if (counterCells == as) {// Expand table unless stale
                        CounterCell[] rs = new CounterCell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        counterCells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = ThreadLocalRandom.advanceProbe(h);
        }
        else if (cellsBusy == 0 && counterCells == as &&
                 U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
            boolean init = false;
            try {                           // Initialize table
                if (counterCells == as) {
                    CounterCell[] rs = new CounterCell[2];
                    rs[h & 1] = new CounterCell(x);
                    counterCells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
            break;                          // Fall back on using base
    }
}
```

# 6. 哈希表初始化和扩容

## .1 resizeStamp
```java
/*
根据容量 n，计算它的指定容量标识。

扩容时 sizeCtl 格式如下：
 - 最高位为 1，使得整数为负数，与元素数量阈值区分
 - 高 15 位是指定容量标识，用于标识是对该大小的扩容。
 - 低 16 位表示并行扩容线程数 + 1。用于记录当前参与扩容的线程数量，用于控制参与扩容的线程数。

此方法返回值第 16 位为 1，后 15 位是指定容量标识，因此使用时还需要左移 RESIZE_STAMP_SHIFT 位
*/
static final int resizeStamp(int n) {
    // Integer.numberOfLeadingZeros(n) 计算 n 二进制表示有几个前导零。因为容量必定是 2 的幂，所以不同容量前导零数量不同
    return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
}
```

## .2 initTable
```java
// 初始化哈希表，使用记录在 sizeCtl 里面的初始容量
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    // 但哈希表未创建时，尝试创建哈希表
    while ((tab = table) == null || tab.length == 0) {
        // sizeCtl 小于 0，表示哈希表正在被其他线程初始化或扩容。因此旋转
        if ((sc = sizeCtl) < 0)
            Thread.yield();
        // 尝试将 sizeCtl 设为 -1，表示正在初始化
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    // 如果 sc 等于 0，表示使用默认容量；否则使用构造器传递进来的容量
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    // sc 缩减为容量的 3/4 倍
                    sc = n - (n >>> 2);
                }
            } finally {
                // 初始化完成，sizeCtl 被赋值为 sc
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

## .3 addCount
```java
/*
使用 x 更新元素计数。

如果元素数量大于 sizeCtl 且尚未调整大小，则准备扩容。
如果正在扩容，且还有任务可做，则让当前线程帮助执行转移。

转移后重新检查占用率，看是否已经需要再次调整大小，因为调整大小过程中可能有其他线程还在插入。

@param x 表示键值对个数的变化值，如果为正，表示新增了元素，如果为负，表示删除了元素
@param check 在 put、compute 方法中，为存储桶中元素计数（粗略估计）；在删除、替换方法中，为 -1
*/
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    // 如果 counterCells 是 null，则将 x CAS 到 baseCount 中，并且 s = b + x，作为元素总量。
    // 否则如果 counterCells 不是 null，或者 CAS 失败，则将 x 添加到 counterCells 中。
    if ((as = counterCells) != null ||
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
        boolean uncontended = true;
        // 首先根据线程探针哈希码找到了对应的 Cell，尝试将 x CAS 到这个 Cell 中；
        // 如果 Cell 为 null，或者 CAS 失败，则使用 fullAddCount 方法添加计数然后返回
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            // 调用 fullAddCount，表示 CAS 冲突严重，则当前线程放弃扩容。
            fullAddCount(x, uncontended);
            return;
        }
        // 更新元素计数完成，并且此时 CAS 冲突并不严重。check <= 1，检查到无线程竞争，于是返回
        if (check <= 1)
            return;
        // 计算元素数量总量
        s = sumCount();
    }
    // 新元素加入，才检查是否扩容
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        // 当更新后的元素数量大于等于阈值 sizeCtl，表已初始化过且容量还没有达到最大值，进行扩容操作
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            // 计算当前容量 n 的指定容量标识
            int rs = resizeStamp(n);
            // 如果 sizeCtl 小于 0，表示正在扩容
            if (sc < 0) {
                /*
                如果 sizeCtl 中的指定容量标识与 rs 不同，则当前线程不能参加这次扩容操作。

                而其它判断是有 bug 的，因为 rs 是正数，sc 是负数， sc == rs + 1 || sc == rs + MAX_RESIZERS 
                是不可能成立的。。这样导致的后果是无法控制执行扩容方法 transfer 的线程数。不过影响并不严重，
                transfer 方法本身是线程安全的，只是有可能会加剧该方法的资源竞争。

                正确的写法应该是 sc == (rs << RESIZE_STAMP_SHIFT) + 1 || sc == (rs << RESIZE_STAMP_SHIFT) + MAX_RESIZERS。
                sc == (rs << RESIZE_STAMP_SHIFT) + 1 表示扩容已经结束。
                sc == (rs << RESIZE_STAMP_SHIFT) + MAX_RESIZERS 表示参与扩容的线程已经达到最大值。
                */
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    // 跳出循环，无需扩容
                    break;
                // 当前线程参与扩容，sizeCtl 加 1
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    // 使用 transfer 方法，进行扩容和转移元素操作
                    transfer(tab, nt);
            }
            // 否则没有其它线程正在扩容，则当前线程开启扩容操作。将 sizeCtl 更新为扩容模式
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                // 使用 transfer 方法，进行扩容和转移元素操作
                transfer(tab, null);
            // 更新元素计数。如果有线程在扩容过程中进行元素插入操作，则下次可能需要重新扩容
            s = sumCount();
        }
    }
}
```
在 Oracle Java Bug Database 上可以看到在 2018 年有人提了这个 Bug ：
[Bug ID:JDK-8214427 : probable bug in logic of ConcurrentHashMap.addCount()][bug]。
可以看到该 BUG 已经在 JDK 12 版本中解决（也就是 JDK 8 中没有解决，说好的维护至 2030 年呢）。

## .4 transfer
```java
/*
进行实际的扩容操作，并移动或复制 tab 每个存储桶中的节点到新表 nextTab 中。

下面是扩容过程的概述：
1. 创建 nextTable，新容量是旧容量的 2 倍。
2. 将原 table 的所有桶逆序分配给多个线程，每个线程每次最小分配 16 个桶，防止资源竞争导致的效率下降。
指定范围的桶可能分配给多个线程同时处理。
3. 扩容时遇到空的桶，采用 CAS 设置为 ForwardingNode 节点，表示该桶扩容完成。
4. 扩容时遇到 ForwardingNode 节点，表示该桶已扩容过了，直接跳过。
5. 单个桶内元素的迁移是加锁的，将旧 table 的 i 位置上所有元素拆分成高低两部分，并迁移到 nextTable 上，
低位索引是 i，高位索引是 i + n，其中 n 为扩容前的容量。
6. 最后将旧 table 的 i 位置设置为 ForwardingNode 节点。
7. 所有桶扩容完毕，将 table 指向 nextTable，设置 sizeCtl 为新容量 0.75 倍
*/
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    // 计算每个线程（一个 CPU 上最多 8 个线程）要帮助处理多少个桶，并且这里每个线程处理都是平均的。最小每个线程处理 16 个桶。
    // 因此，如果长度是 16 的时候，扩容的时候只会有一个线程扩容。
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
        stride = MIN_TRANSFER_STRIDE; // subdivide range

    // 如果 nextTab 为 null，先分配 nextTab 的空间 
    if (nextTab == null) {            // initiating
        try {
            // 新表是旧表的两倍大
            @SuppressWarnings("unchecked")
            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
            nextTab = nt;
        } catch (Throwable ex) {      // try to cope with OOME
            // 如果出现了 OOM 异常，则不能再扩容。将 sizeCtl 设为最大值，然后返回
            sizeCtl = Integer.MAX_VALUE;
            return;
        }
        nextTable = nextTab;
        // transferIndex 初始化为旧表大小。转移操作是从 n-1 到 0 进行的
        transferIndex = n;
    }

    int nextn = nextTab.length;
    // 生成一个转发节点，包含 nextTab。
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
    // advance 为 true，表示继续寻找下一个准备转移的存储桶
    boolean advance = true;
    // finishing 为 true，表示已经完成了全部的转移操作
    boolean finishing = false; // to ensure sweep before committing nextTab

    // i 是待处理的存储桶下标；
    // bound 是该线程此次可以处理的区间的最小下标，超过这个下标，就需要重新领取区间或者结束扩容
    for (int i = 0, bound = 0;;) {
        Node<K,V> f; int fh;
        while (advance) {
            int nextIndex, nextBound;
            /*
            对 i 减一，判断是否大于等于 bound
             - 不成立，说明该线程上次领取的任务已经完成了。进行下面的判断，看看能否继续领取任务。
             - 成立，表示还有元素需要转移。修改推进状态为 false，表示已经找到需要转移的存储桶，跳出循环

            或者如果已经完成了全部的转移操作，修改推进状态为 false，跳出循环。
            
            通常，第一次进入循环，--i 这个判断会无法通过，从而走下面的 nextIndex 赋值操作（获取最新的转移下标）。
            */
            if (--i >= bound || finishing)
                advance = false;
            /*
            这里的目的是：
            1. 当一个线程进入时，会选取最新的转移下标。
            2. 当一个线程处理完自己的区间时，如果还有剩余区间的没有别的线程处理。再次获取区间。

            如果转移操作全部完成了，则跳出循环
            */
            else if ((nextIndex = transferIndex) <= 0) {
                i = -1;
                advance = false;
            }
            // 否则更新 transferIndex，分配任务区间。减去一个 stride，或在不满一个 stride 的情况下设为 0，
            // 留下剩余的区间值供后面的线程使用
            else if (U.compareAndSwapInt
                     (this, TRANSFERINDEX, nextIndex,
                      nextBound = (nextIndex > stride ?
                                   nextIndex - stride : 0))) {
                // CAS 成功则改变状态，退出 while 循环

                // 当前线程可以处理的区间的最小下标
                bound = nextBound;
                // 当前线程可以处理的区间的最大下标
                i = nextIndex - 1;
                advance = false;
            }
        }
        // 如果满足以下条件，按照上面的判断，当前线程自己的活已经做完，或所有线程的活都已做完
        if (i < 0 || i >= n || i + n >= nextn) {
            int sc;
            // 如果已经完成全部的转移，就将旧表替换为新表，sizeCtl 变为新容量的 3/4，作为新的阈值。最后返回
            if (finishing) {
                nextTable = null;
                table = nextTab;
                sizeCtl = (n << 1) - (n >>> 1);
                return;
            }
            // 否则尝试将 sc - 1，也就是将 sc 的低 16 位减一，表示当前线程结束转移任务了。
            if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                // 如果 sc - 2 不等于指定容量标识左移 16 位，说明还有线程转移任务没结束，则当前线程结束方法。
                // 如果相等了，说明没有线程在帮助转移，也就是说，所有任务都完成了。
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                    return;
                // 所有任务都完成了
                finishing = advance = true;
                i = n; // recheck before commit
            }
        }
        // 否则转移任务还没有结束，继续进行。
        // 如果 tab[i] 为 null，将转发节点 CAS 到 i 处。保证了只要是看到的 null 存储槽都会被设为转发节点，
        // 这样在插入操作中就不会出现将节点放到 null 存储槽上，导致节点没有被转移从而错过的情形。
        else if ((f = tabAt(tab, i)) == null)
            advance = casTabAt(tab, i, null, fwd);
        // 否则如果 tab[i] 是转发节点，则需要继续寻找下一个其它节点
        else if ((fh = f.hash) == MOVED)
            advance = true; // already processed
        // 否则 tab 下标 i 处存在其他节点（Node、TreeBin）
        else {
            // 将这个节点上锁。注意，读取操作是不加锁的
            synchronized (f) {
                // 确认 tab[i] == f
                if (tabAt(tab, i) == f) {
                    Node<K,V> ln, hn;
                    // 如果 fh >= 0，表示 f 是 Node 节点
                    if (fh >= 0) {
                        /*
                        计算节点哈希码和容量的与。
                        当结果为 0（最高位），表示此节点在旧表和新表中的哈希码掩码结果相同，下标也相同，因此无需变换位置。
                        当结果为 1，则它在新表中的下标将会发生变化。

                        ln 链表会记录那些为 0 的节点，hn 链表会记录那些为 1 的节点
                        */
                        int runBit = fh & n;
                        // 最后一个和上一个节点哈希与不相等的节点。这样 lastRun 及之后的节点可以不用遍历了
                        Node<K,V> lastRun = f;
                        // 从 f 的下个节点开始，遍历链表
                        for (Node<K,V> p = f.next; p != null; p = p.next) {
                            // 依次计算每个节点的哈希与
                            int b = p.hash & n;
                            // 如果结果和上一个节点的哈希与不同，则更新 runBit 和 lastRun
                            if (b != runBit) {
                                runBit = b;
                                lastRun = p;
                            }
                        }
                        // 如果最后更新的 runBit 是 0，设置 ln 为 lastRun
                        if (runBit == 0) {
                            ln = lastRun;
                            hn = null;
                        }
                        // 否则最后更新的 runBit 是 1（高位），设置 hn 为 lastRun
                        else {
                            hn = lastRun;
                            ln = null;
                        }
                        // 再次遍历链表，直到 lastRun。
                        // 注意，ln 和 hn 和原来的链表顺序相比是倒序的（除了 lastRun 及之后的节点）。
                        // 但由于 ConcurrentHashMap 迁移桶上链表的时候，加了锁，因此迁移前后顺序不一致没有问题
                        for (Node<K,V> p = f; p != lastRun; p = p.next) {
                            int ph = p.hash; K pk = p.key; V pv = p.val;
                            // 如果哈希与为 0，将其复制到 ln 链表结尾
                            if ((ph & n) == 0)
                                ln = new Node<K,V>(ph, pk, pv, ln);
                            // 否则哈希与为 1，将其复制到 hn 链表结尾
                            else
                                hn = new Node<K,V>(ph, pk, pv, hn);
                        }
                        // ln 链表在新哈希表中的位置不变
                        setTabAt(nextTab, i, ln);
                        // hn 链表在新哈希表中的位置改变（加上之前最高位 1）
                        setTabAt(nextTab, i + n, hn);
                        // 将旧哈希表原来下标处设置为转发节点
                        setTabAt(tab, i, fwd);
                        // 可以继续查找下一个存储桶
                        advance = true;
                    }
                    // 否则如果节点是 TreeBin
                    else if (f instanceof TreeBin) {
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        // 和上面一样，也是分为两组
                        TreeNode<K,V> lo = null, loTail = null;
                        TreeNode<K,V> hi = null, hiTail = null;
                        int lc = 0, hc = 0;
                        // 使用 TreeNode 的 next 指针进行遍历分组
                        for (Node<K,V> e = t.first; e != null; e = e.next) {
                            int h = e.hash;
                            TreeNode<K,V> p = new TreeNode<K,V>
                                (h, e.key, e.val, null, null);
                            // 如果哈希与为 0，将其移动到 lo 链表结尾
                            if ((h & n) == 0) {
                                if ((p.prev = loTail) == null)
                                    lo = p;
                                else
                                    loTail.next = p;
                                loTail = p;
                                ++lc;
                            }
                            // 否则哈希与为 1，将其移动到 hi 链表结尾
                            else {
                                if ((p.prev = hiTail) == null)
                                    hi = p;
                                else
                                    hiTail.next = p;
                                hiTail = p;
                                ++hc;
                            }
                        }
                        // 如果 lo 链表结点数小于等于 UNTREEIFY_THRESHOLD，将其转化为链表。
                        // 否则如果 hi 链表是空的，则重用之前的 TreeBin 节点；否则新建一个  TreeBin 节点
                        ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                            (hc != 0) ? new TreeBin<K,V>(lo) : t;
                        // 如果 hi 链表结点数小于等于 UNTREEIFY_THRESHOLD，将其转化为链表。
                        // 否则如果 lo 链表是空的，则重用之前的 TreeBin 节点；否则新建一个  TreeBin 节点
                        hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                            (lc != 0) ? new TreeBin<K,V>(hi) : t;
                        // ln 链表在新哈希表中的位置不变
                        setTabAt(nextTab, i, ln);
                        // hn 链表在新哈希表中的位置改变（加上之前最高位 1）
                        setTabAt(nextTab, i + n, hn);
                        // 将旧哈希表原来下标处设置为转发节点
                        setTabAt(tab, i, fwd);
                        // 可以继续查找下一个存储桶
                        advance = true;
                    }
                }
            }
        }
    }
}
```

## .5 helpTransfer
```java
// 如果正在进行扩容，则使用当前线程帮助转移操作。
// 参数 tab 是旧的哈希表，f 是转发节点
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    // 判断是否满足转移条件
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        // 计算指定容量标识
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            // 和 addCount 中一样的判断条件
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            // 当前线程参与扩容，sizeCtl 加 1
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```

## .6 tryPresize
```java
// 尝试预先确定哈希表的大小，以容纳给定数量的元素。
private final void tryPresize(int size) {
    // 如果 size 大于等于 MAXIMUM_CAPACITY 的一半，则容量指定为 MAXIMUM_CAPACITY。
    // 否则指定为大于等于 1.5 倍 size 的 2 的幂。
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
        tableSizeFor(size + (size >>> 1) + 1);
    int sc;
    // 如果此时不处于扩容阶段，则进行尝试
    while ((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
        // 如果哈希表还未创建，则进行初始化操作
        if (tab == null || (n = tab.length) == 0) {
            n = (sc > c) ? sc : c;
            // 将 sizeCtl 设为 -1，表示进入初始化状态
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        // 创建哈希表
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        // sizeCtl 设为容量的 3/4 倍
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        // 否则如果指定容量小于原有元素阈值，或者容量已经到达最大值，则不能扩容，退出循环
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        // 否则进行扩容
        else if (tab == table) {
            // 和 addCount 方法中的相同
            int rs = resizeStamp(n);
            if (sc < 0) {
                Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
        }
    }
}
```

# 7. put 元素

## .1 putVal
```java
// 实现 put 和 putIfAbsent 方法。参数 onlyIfAbsent 为 true 表示只在键值对不存在的情况下进行插入
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    // 对哈希码进行变换
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        // 如果哈希表还未初始化，则先进行初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        // 如果计算的下标位置存储桶为 null
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // cas 一个新的节点，此时不需要加锁
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        // 如果 f 是一个转发节点，表示正在进行扩容，则先让当前线程帮助进行转移操作
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        // 否则要向一个非 null 的存储槽进行 put 操作
        else {
            V oldVal = null;
            // 首先锁定存储槽的首节点。注意，读取操作是不加锁的
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 如果 fh >= 0，表示 f 是 Node 节点
                    if (fh >= 0) {
                        // binCount 计算当前存储槽节点数量
                        binCount = 1;
                        // 遍历链表
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            // 判断节点是否与给定的键值对相等
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                // 如果 onlyIfAbsent 为 false，则替换旧值
                                if (!onlyIfAbsent)
                                    e.val = value;
                                // 跳出循环
                                break;
                            }
                            Node<K,V> pred = e;
                            // 如果下个节点是 null，则在队尾插入一个新节点
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                // 跳出遍历链表的 for 循环
                                break;
                            }
                        }
                    }
                    // 否则如果 f 是 TreeBin 节点
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        // 指向红黑树的 put 操作。返回节点不为 null 表示已经存在键
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            // 如果 onlyIfAbsent 为 false，则替换旧值
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                // 如果节点数大于等于 TREEIFY_THRESHOLD，将其转化为红黑树
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                // 旧值存在，则表示进行了替换操作，则返回它，不执行后面的 addCount 方法
                if (oldVal != null)
                    return oldVal;
                // 退出循环
                break;
            }
        }
    }
    // 添加元素计数，如果元素数量已经超过阈值，进行扩容操作
    addCount(1L, binCount);
    return null;
}
```

# 8. get 元素

## .1 get
```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    // 对哈希码进行预处理
    int h = spread(key.hashCode());
    // 如果指定的位置存在存储桶
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        // 如果开头节点和指定的键相同，则直接返回它的值
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        // 如果哈希码小于 0（表示是一个特殊节点），则使用它的 find 方法
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        // 否则这是一个链表，那么遍历链表进行查找
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```


[unsafe]: ../../../sun_/misc/Unsafe.md
[hash-map]: ../HashMap.md
[contended]: ../../../sun_/misc/Contended.md
[striped64]: atomic/Striped64.md
[long-adder]: atomic/LongAdder.md
[bug]: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8214427