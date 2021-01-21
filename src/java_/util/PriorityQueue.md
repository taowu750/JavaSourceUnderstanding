`java.util.PriorityQueue`类的声明如下：
```java
public class PriorityQueue<E> extends AbstractQueue<E>
    implements java.io.Serializable
```
基于堆的无界优先级队列。优先级队列的元素根据其自然顺序进行排序，
或者通过在队列创建时提供的`Comparator`进行排序。优先级队列不允许`null`元素。
依赖自然顺序的优先级队列也不允许插入不可比较的对象（这样做可能会导致`ClassCastException`）。

就指定的顺序而言，此队列的头是最小的元素。如果最小元素有多个，那么头就是这些元素之一。
队列检索操作`poll`，`remove`，`peek`和`element`访问队列开头的元素。

优先级队列是无界的，但是具有内部容量来控制用于在队列上存储元素的数组的大小。
它总是至少与队列大小一样大。将元素添加到优先级队列时，其容量会自动增长。

此类及其迭代器实现`Collection`和`Iterator`接口的所有可选方法。
不保证方法`iterator()`中提供的`Iterator`以任何特定顺序遍历优先级队列的元素。
如果需要有序遍历，请考虑使用`Arrays.sort(pq.toArray())`。

请注意，此实现未同步。如果任何线程修改了队列，则多个线程不应同时访问`PriorityQueue`实例。
而是使用线程安全的`java.util.concurrent.PriorityBlockingQueue`类。

注意，此实现的入队和出队方法`offer`、`poll`、`remove`和`add`方法需要 O(log(n)) 时间；
`remove(Object)`和`contains(Object)`方法需要线性时间；
检索方法（`peek`，`element`和`size`）需要常数时间。

其他一些实现参见 [AbstractCollection.md][abstract-collection] 和
[AbstractQueue.md][abstract-queue]。 

# 1. 成员字段
```java
// 默认初始化容量
private static final int DEFAULT_INITIAL_CAPACITY = 11;

// 要分配的最大数组大小（可以超过这个值，最大为 Integer.MAX_VALUE）。一些虚拟机在数组中保留一些头字。
// 尝试分配更大的数组可能会导致 OutOfMemoryError
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

// 比较器；如果优先级队列使用元素的自然顺序，则为 null。
private final Comparator<? super E> comparator;

// 队列大小，也就是元素数量
private int size = 0;

/*
优先级队列的堆，其中 queue[n] 的两个子结点是 queue[2*n+1] 和 queue[2*(n+1)]。

如果比较器为 null，则按元素的自然顺序对优先级队列进行排序。
对于堆中的每个节点 n 和 n 的每个后代 d，n <= d。

如果队列为非空，则最小值位于 queue[0] 中。

之所以为 transient，是为了序列化时不把 null 元素也序列化，
只序列化实际的元素。
*/
transient Object[] queue;  // 非私有以简化嵌套类访问

// 此优先级队列结构被修改的次数。详细信息请参见 AbstractList。
transient int modCount = 0;
```

# 2. 构造器

## 2.1 使用容量和比较器构造
```java
public PriorityQueue() {
    this(DEFAULT_INITIAL_CAPACITY, null);
}

public PriorityQueue(int initialCapacity) {
    this(initialCapacity, null);
}

public PriorityQueue(Comparator<? super E> comparator) {
    this(DEFAULT_INITIAL_CAPACITY, comparator);
}

public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
    // 注意：实际上并不需要“至少一个”限制，但是为了和 1.5 兼容，该限制仍然存在
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.queue = new Object[initialCapacity];
    this.comparator = comparator;
}
```

## 2.2 提供其他容器进行构造
```java
/*
创建一个包含指定集合中元素的 PriorityQueue。如果指定的集合是 SortedSet 的实例
或另一个 PriorityQueue，则将根据相同的顺序对此优先级队列进行排序。
否则，该优先级队列将根据其元素的自然顺序进行排序。
*/
@SuppressWarnings("unchecked")
public PriorityQueue(Collection<? extends E> c) {
    // 如果 c 是 SortedSet 或其他 PriorityQueue，则使用 c 的比较器
    if (c instanceof SortedSet<?>) {
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator();
        initElementsFromCollection(ss);
    }
    else if (c instanceof PriorityQueue<?>) {
        PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator();
        initFromPriorityQueue(pq);
    }
    else {
        this.comparator = null;
        initFromCollection(c);
    }
}

@SuppressWarnings("unchecked")
public PriorityQueue(PriorityQueue<? extends E> c) {
    this.comparator = (Comparator<? super E>) c.comparator();
    initFromPriorityQueue(c);
}

public PriorityQueue(SortedSet<? extends E> c) {
    this.comparator = (Comparator<? super E>) c.comparator();
    initElementsFromCollection(c);
}

// 使用 c.toArray 作为堆数组，不进行堆数组构建操作
private void initElementsFromCollection(Collection<? extends E> c) {
    Object[] a = c.toArray();
    if (c.getClass() != ArrayList.class)
        a = Arrays.copyOf(a, a.length, Object[].class);
    int len = a.length;
    if (len == 1 || this.comparator != null)
        for (int i = 0; i < len; i++)
            if (a[i] == null)
                throw new NullPointerException();
    this.queue = a;
    this.size = a.length;
}

// 使用 c 中的元素构建堆数组
private void initFromCollection(Collection<? extends E> c) {
    initElementsFromCollection(c);
    heapify();
}

private void initFromPriorityQueue(PriorityQueue<? extends E> c) {
    // 如果 c 是 PriorityQueue，则直接复制 c 的底层堆数组
    if (c.getClass() == PriorityQueue.class) {
        this.queue = c.toArray();
        this.size = c.size();

    // 否则，使用 c 中的元素构建堆数组
    } else {
        initFromCollection(c);
    }
}
```

# 3. 方法

## 3.1 上浮和下沉
```java
/*
将元素 x 插入位置 k，通过将 x 上浮直到其大于或等于其父级或成为根，从而保持堆不变。
Comparable 版本和 Comparator 版本分为不同的方法，这些方法在其他方面是相同的。
*/
private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x);
    else
        siftUpComparable(k, x);
}

@SuppressWarnings("unchecked")
private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        // 和获取父结点
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        // 和父节点元素进行比较，如果小于父元素，继续上浮；否则终止
        if (key.compareTo((E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}

@SuppressWarnings("unchecked")
private void siftUpUsingComparator(int k, E x) {
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        if (comparator.compare(x, (E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = x;
}

// 在位置 k 插入项 x，将 x 不断下沉直到其小于或等于其子级或为叶子结点，从而保持堆不变。
private void siftDown(int k, E x) {
    if (comparator != null)
        siftDownUsingComparator(k, x);
    else
        siftDownComparable(k, x);
}

@SuppressWarnings("unchecked")
private void siftDownComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>)x;
    // 叶子结点的分界点
    int half = size >>> 1;
    while (k < half) {
        // 首先获取左子结点
        int child = (k << 1) + 1; // assume left child is least
        Object c = queue[child];
        // 如果右子结点存在且比左子结点小的话，就使用右子结点
        int right = child + 1;
        if (right < size &&
            ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
            c = queue[child = right];
        // 如果插入元素大于子结点，就下沉；否则终止
        if (key.compareTo((E) c) <= 0)
            break;
        queue[k] = c;
        k = child;
    }
    queue[k] = key;
}
```

## 3.2 heapify
```java
// 在 queue 数组上构建堆
@SuppressWarnings("unchecked")
private void heapify() {
    // 从最后一个非叶结点开始，不断下沉
    for (int i = (size >>> 1) - 1; i >= 0; i--)
        siftDown(i, (E) queue[i]);
}
```

## 3.3 iterator
```java
public Iterator<E> iterator() {
    return new Itr();
}
```

## 3.4 spliterator
```java
public final Spliterator<E> spliterator() {
    return new PriorityQueueSpliterator<E>(this, 0, -1, 0);
}
```

## 3.5 size
```java
public int size() {
    return size;
}
```

## 3.6 toArray
```java
public Object[] toArray() {
    return Arrays.copyOf(queue, size);
}

@SuppressWarnings("unchecked")
public <T> T[] toArray(T[] a) {
    final int size = this.size;
    if (a.length < size)
        return (T[]) Arrays.copyOf(queue, size, a.getClass());
    System.arraycopy(queue, 0, a, 0, size);
    if (a.length > size)
        a[size] = null;
    return a;
}
```

## 3.7 contains
```java
public boolean contains(Object o) {
    return indexOf(o) != -1;
}

private int indexOf(Object o) {
    if (o != null) {
        for (int i = 0; i < size; i++)
            if (o.equals(queue[i]))
                return i;
    }
    return -1;
}
```

## 3.8 添加元素
```java
public boolean add(E e) {
    return offer(e);
}

public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    if (i >= queue.length)
        grow(i + 1);
    size = i + 1;
    if (i == 0)
        // 队列为空直接放在开头
        queue[0] = e;
    else
        // 否则放在堆尾然后上浮
        siftUp(i, e);
    return true;
}

private void grow(int minCapacity) {
    int oldCapacity = queue.length;
    // Double size if small; else grow by 50%
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                         (oldCapacity + 2) :
                                         (oldCapacity >> 1));
    // overflow-conscious code
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    queue = Arrays.copyOf(queue, newCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```
`overflow-conscious code`参见 [overflow-conscious code.md][overflow]。

## 3.9 删除元素
```java
public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1)
        return false;
    else {
        removeAt(i);
        return true;
    }
}

@SuppressWarnings("unchecked")
public E poll() {
    if (size == 0)
        return null;
    int s = --size;
    modCount++;
    // 删除第一个元素，把堆最后一个元素放在首位上，然后下沉它
    E result = (E) queue[0];
    E x = (E) queue[s];
    queue[s] = null;
    if (s != 0)
        siftDown(0, x);
    return result;
}


// 删除与 0 相等的第一个元素
boolean removeEq(Object o) {
    for (int i = 0; i < size; i++) {
        if (o == queue[i]) {
            removeAt(i);
            return true;
        }
    }
    return false;
}

/*
从队列中删除第 i 个元素。通常，此方法不影响堆中的前 i-1（包括 i-1）个元素。
在这种情况下，它返回 null。

有时，为了保持堆序不变，它必须将堆中最后一个元素与 i 之前的元素交换。
在这种情况下，此方法返回之前堆末尾的元素。

iterator.remove 使用此方法，以避免丢失遍历元素。
*/
@SuppressWarnings("unchecked")
private E removeAt(int i) {
    // assert i >= 0 && i < size;
    modCount++;
    int s = --size;
    if (s == i) // 删除最后一个元素
        queue[i] = null;
    else {
        // 将堆中最后一个元素放到删除的位置上，然后下沉
        E moved = (E) queue[s];
        queue[s] = null;
        siftDown(i, moved);
        // 如果没有下沉成功，则将其上浮
        if (queue[i] == moved) {
            siftUp(i, moved);
            if (queue[i] != moved)
                return moved;
        }
    }
    return null;
}
```

## 3.10 peek
```java
@SuppressWarnings("unchecked")
public E peek() {
    return (size == 0) ? null : (E) queue[0];
}
```

## 3.11 comparator
```java
public Comparator<? super E> comparator() {
    return comparator;
}
```

## 3.12 clear
```java
public void clear() {
    modCount++;
    for (int i = 0; i < size; i++)
        queue[i] = null;
    size = 0;
}
```

## 3.13 序列化
```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
    // Write out element count, and any hidden stuff
    s.defaultWriteObject();

    // Write out array length, for compatibility with 1.5 version
    s.writeInt(Math.max(2, size + 1));

    // Write out all elements in the "proper order".
    for (int i = 0; i < size; i++)
        s.writeObject(queue[i]);
}

private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in (and discard) array length
    s.readInt();

    SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, size);
    queue = new Object[size];

    // Read in all elements.
    for (int i = 0; i < size; i++)
        queue[i] = s.readObject();

    // Elements are guaranteed to be in "proper order", but the
    // spec has never explained what that might be.
    heapify();
}
```
有关和`Java1.5`兼容性的原因，参见 [ArrayList.md][array-list] 第 3.20 节序列化。

# 4. 内部类

## 4.1 Itr
```java
private final class Itr implements Iterator<E> {
    // 下一次调用 next 返回的元素的索引
    private int cursor = 0;
    // 上一次调用 next 返回的元素的索引；除了来自 forgetMeNot 的元素。
    // 如果元素通过调用 remove 删除，则设置为 -1。
    private int lastRet = -1;
    /*
    某些元素可能会因为迭代过程中删除而导致其从堆的未访问部分移至已访问部分（参见 removeAt 方法）。
    使用 forgetMeNot 存储这些元素，在完成“常规”迭代之后遍历 forgetMeNot 完成迭代。
     */
    private ArrayDeque<E> forgetMeNot = null;
    // 上一次调用 next 返回的元素，且来自 forgetMeNot
    private E lastRetElt = null;
    private int expectedModCount = modCount;

    public boolean hasNext() {
        return cursor < size ||
            (forgetMeNot != null && !forgetMeNot.isEmpty());
    }

    @SuppressWarnings("unchecked")
    public E next() {
        if (expectedModCount != modCount)
            throw new ConcurrentModificationException();
        // 首先遍历堆中的元素
        if (cursor < size)
            return (E) queue[lastRet = cursor++];
        // 然后遍历 forgetMeNot 列表中的元素
        if (forgetMeNot != null) {
            lastRet = -1;
            lastRetElt = forgetMeNot.poll();
            if (lastRetElt != null)
                return lastRetElt;
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        if (expectedModCount != modCount)
            throw new ConcurrentModificationException();
        if (lastRet != -1) {
            // 删除指定位置的元素。如果 moved 不等于 null，
            // 则它是从堆的未访问部分移至已访问部分的元素，需要添加到 forgetMeNot 列表中
            E moved = PriorityQueue.this.removeAt(lastRet);
            lastRet = -1;
            if (moved == null)
                cursor--;
            else {
                if (forgetMeNot == null)
                    forgetMeNot = new ArrayDeque<>();
                forgetMeNot.add(moved);
            }
        } else if (lastRetElt != null) {
            PriorityQueue.this.removeEq(lastRetElt);
            lastRetElt = null;
        } else {
            throw new IllegalStateException();
        }
        expectedModCount = modCount;
    }
}
```

## 4.2 PriorityQueueSpliterator
```java
static final class PriorityQueueSpliterator<E> implements Spliterator<E> {
    /*
     * 此类类似于 ArrayList 的 Spliterator，除了额外的 null 检查。
     * 此类也直到调用方法的时候才对状态进行初始化，这缩小了发生冲突的时间窗口。
     * 通过延迟初始化，PriorityQueueSpliterator 实现了后期绑定。并且它也是 fail-fast 的。
     */
    private final PriorityQueue<E> pq;
    private int index;            // 当前索引，在 tryAdvance/split 时修改
    private int fence;            // 直到使用之前都是 -1
    private int expectedModCount; // 当 fence 被初始化时设置

    PriorityQueueSpliterator(PriorityQueue<E> pq, int origin, int fence,
                         int expectedModCount) {
        this.pq = pq;
        this.index = origin;
        this.fence = fence;
        this.expectedModCount = expectedModCount;
    }

    private int getFence() { // 直到第一次使用时对 fence 和 expectedModCount 初始化
        int hi;
        if ((hi = fence) < 0) {
            expectedModCount = pq.modCount;
            hi = fence = pq.size;
        }
        return hi;
    }

    public PriorityQueueSpliterator<E> trySplit() {
        // 将左半边切分出去
        int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
        return (lo >= mid) ? null :
            new PriorityQueueSpliterator<E>(pq, lo, index = mid,
                                            expectedModCount);
    }

    @SuppressWarnings("unchecked")
    public void forEachRemaining(Consumer<? super E> action) {
        int i, hi, mc; // hoist accesses and checks from loop
        PriorityQueue<E> q; Object[] a;
        if (action == null)
            throw new NullPointerException();
        if ((q = pq) != null && (a = q.queue) != null) {
            // 初始化 hi 和 mc
            if ((hi = fence) < 0) {
                mc = q.modCount;
                hi = q.size;
            }
            else
                mc = expectedModCount;
            // 遍历剩余元素
            if ((i = index) >= 0 && (index = hi) <= a.length) {
                for (E e;; ++i) {
                    if (i < hi) {
                        // 如果遇到了 null 元素，则退出循环
                        if ((e = (E) a[i]) == null)
                            break;
                        action.accept(e);
                    }
                    else if (q.modCount != mc)
                        break;
                    else
                        return;
                }
            }
        }
        throw new ConcurrentModificationException();
    }

    public boolean tryAdvance(Consumer<? super E> action) {
        if (action == null)
            throw new NullPointerException();
        int hi = getFence(), lo = index;
        if (lo >= 0 && lo < hi) {
            index = lo + 1;
            @SuppressWarnings("unchecked") E e = (E)pq.queue[lo];
            if (e == null)
                throw new ConcurrentModificationException();
            action.accept(e);
            if (pq.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }
        return false;
    }

    public long estimateSize() {
        return (long) (getFence() - index);
    }

    public int characteristics() {
        return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL;
    }
}
```
参见 [ArrayList.md][array-list] 第 4.3 节 ArrayListSpliterator。


[abstract-collection]: AbstractCollection.md
[abstract-queue]: AbstractQueue.md
[overflow]: overflow-conscious%20code.md
[array-list]: ArrayList.md