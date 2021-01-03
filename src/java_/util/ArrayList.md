`java.util.ArrayList`类的声明如下：
```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```
`ArrayList`是`List`接口的可调整大小的数组实现。实现所有可选的列表操作，并允许所有元素，包括`null`。
除了实现`List`接口之外，此类还提供一些方法来操纵内部用于存储列表的数组的大小。（此类与`Vector`大致等效，但它是不同步的。）

`ArrayList`的`size`，`isEmpty`，`get`，`set`，`iterator`和`listIterator`方法在常数时间内完成操作。
`add`方法以均摊的常数时间运行，即添加`n`个元素需要`O(n)`时间。所有其他操作均以线性时间运行（大致而言）。
与`LinkedList`实现相比，`ArrayList`方法时间复杂度的常数因子较低。

每个`ArrayList`实例都有一个容量。容量是用于在列表中存储元素的数组的大小。它总是至少与列表大小一样大。
将元素添加到`ArrayList`后，其容量会自动增长。除了添加一个元素具有固定的均摊时间成本这一事实之外，增长策略的细节没有被指定。
应用程序可以使用`ensureCapacity`操作在添加大量元素之前增加`ArrayList`实例的容量。这可以减少容量增长的大小。

请注意，此实现未同步。如果多个线程同时访问`ArrayList`实例，并且至少有一个线程在结构上修改列表，则必须在外部进行同步。
（结构修改是添加或删除一个或多个元素或显式调整底层数组大小的任何操作；仅修改元素的值不是结构修改。）
这通常是通过对某些使用了`ArrayList`的对象进行同步来实现的。如果不存在这样的对象，
则应使用`Collections.synchronizedList`方法“包装”`ArrayList`。并且最好在创建时完成此操作，以防止意外的不同步操作访问列表：
```java
List list = Collections.synchronizedList(new ArrayList(...));
```

此类的`iterator`和`listIterator`方法返回的`Iterator`是`fail-fast`的：
除了通过迭代器自己的`remove`或`add`方法之外，如果在创建迭代器后的任何时候以任何方式对列表进行结构修改，
则迭代器会抛出`ConcurrentModificationException`。因此，面对并发修改，迭代器会快速失败，
而不会在未来的不确定时间冒着不确定行为的风险。

请注意，迭代器的`fail-fast`行为无法得到保证，因为通常来说，在存在不同步的并发修改的情况下，
不可能做出任何严格的保证。`fail-fast`的迭代器会尽最大努力抛出`ConcurrentModificationException`。

`ArrayList`代码中值得注意的有：
1. 3.2 forEach：只在末尾进行并发修改检查
2. 3.7 容量改变：`overflow-consicous code`
3. 3.13 remove
    - 使用 BitSet 记录删除元素的位置
    - 在数组中一次性删除多个元素
4. 3.20 序列化：兼容性考虑
5. 4.3 ArrayListSpliterator：使用延迟初始化缩小发生冲突的时间窗口
6. 4.4 SubList：视图类在有子视图时，结构改变方法和非结构改变方法需要调用不同的底层方法

`ArrayList`的方法说明和一些继承的方法实现参见 [List.md][list]、[AbstractCollection.md][abstract-collection]、
[AbstractList.md][abstract-list]。

# 1. 成员字段

## 1.1 容量和大小
```java
// 默认初始容量。
private static final int DEFAULT_CAPACITY = 10;

// 要分配的最大数组大小（可以超过这个值，最大为 Integer.MAX_VALUE）。一些虚拟机在数组中保留一些头字。
// 尝试分配更大的数组可能会导致 OutOfMemoryError
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

// ArrayList 的大小（它包含的元素数）。
private int size;
```

## 1.2 元素数组
```java
// 用于空 ArrayList 实例之间的共享空数组。
private static final Object[] EMPTY_ELEMENTDATA = {};

// 共享的空数组，用于具有默认大小的空实例。我们将它与 EMPTY_ELEMENTDATA 区别开来，
// 以了解当添加第一个元素时需要增加多少容量。
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

/*
存储 ArrayList 元素的数组。ArrayList 的容量是此数组的长度。添加第一个元素时，
如果 elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA，将扩容为 DEFAULT_CAPACITY。

非私有以简化嵌套类访问。

之所以是 transient，是因为我们向在序列化时写入 size 个存在的元素，而不是整个数组。
*/
transient Object[] elementData;
```

# 2. 构造器

## 2.1 默认构造器
```java
// 构造一个初始容量为 10 的空列表。
public ArrayList() {
    // 实际上构造时仍没有为数组分配空间，将分配空间的操作延迟到添加元素时
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
```

## 2.2 指定容量
```java
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        // 初始容量为 0 的话，使用 EMPTY_ELEMENTDATA
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    }
}
```

## 2.3 ArrayList(Collection<? extends E>)
```java
public ArrayList(Collection<? extends E> c) {
    Object[] a = c.toArray();
    if ((size = a.length) != 0) {
        if (c.getClass() == ArrayList.class) {
            // 如果 c 是 ArrayList，则直接使用 a
            elementData = a;
        } else {
            // 否则复制 a
            elementData = Arrays.copyOf(a, size, Object[].class);
        }
    } else {
        // c 是空的，则使用 EMPTY_ELEMENTDATA
        elementData = EMPTY_ELEMENTDATA;
    }
}
```

# 3. 方法

## 3.1 clone
```java
public Object clone() {
    try {
        ArrayList<?> v = (ArrayList<?>) super.clone();
        // 因为 clone() 方法是浅拷贝，所以需要复制当前 ArrayList 的底层数组
        v.elementData = Arrays.copyOf(elementData, size);
        // 复制的 ArrayList 将其 modCount 置 0。modCount 来自 AbstractList
        v.modCount = 0;
        return v;
    } catch (CloneNotSupportedException e) {
        // 这里按常理来说不可能发生
        throw new InternalError(e);
    }
}
```

## 3.2 forEach
```java
@Override
public void forEach(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    final int expectedModCount = modCount;
    // 将 Object[] 转成 E[] 时编译器会警告，但可以运行。因为运行时 T 被擦除为 Object
    @SuppressWarnings("unchecked")
    final E[] elementData = (E[]) this.elementData;
    final int size = this.size;
    for (int i=0; modCount == expectedModCount && i < size; i++) {
        action.accept(elementData[i]);
    }
    // 遍历所有元素之后检查结构是否改变，以减少对性能的影响
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```

## 3.3 iterator
```java
// 以正确的顺序返回此列表中元素的迭代器。返回的迭代器是 fail-fast 的。
public Iterator<E> iterator() {
    return new Itr();
}
```

## 3.4 listIterator
```java
public ListIterator<E> listIterator(int index) {
    if (index < 0 || index > size)
        throw new IndexOutOfBoundsException("Index: "+index);
    return new ListItr(index);
}

public ListIterator<E> listIterator() {
    return new ListItr(0);
}
```

## 3.5 spliterator
```java
/*
在此列表中的元素上创建后期绑定和 fail-fast 的 Spliterator。
此 Spliterator 具有 Spliterator.SIZED，Spliterator.SUBSIZED 和 Spliterator.ORDERED 的特征。
覆盖此方法的子类应在文档中写明其他特征值（如果有的话）。 
*/
@Override
public Spliterator<E> spliterator() {
    return new ArrayListSpliterator<>(this, 0, -1, 0);
}
```
参见 [Spliterator.md][spliterator]。

## 3.6 大小查询
```java
public int size() {
    return size;
}

public boolean isEmpty() {
    return size == 0;
}
```

## 3.7 容量改变
```java
// 将此 ArrayList 实例的容量调整为列表的当前大小。应用程序可以使用此方法来最小化 ArrayList 实例的存储空间。
public void trimToSize() {
    modCount++;
    if (size < elementData.length) {
        elementData = (size == 0)
          ? EMPTY_ELEMENTDATA
          : Arrays.copyOf(elementData, size);
    }
}

// 如有必要，增加此 ArrayList 实例的容量，以确保它至少有 minCapacity 大小。
public void ensureCapacity(int minCapacity) {
    // elementData 可能是 DEFAULTCAPACITY_EMPTY_ELEMENTDATA，此时它的容量应该为 DEFAULT_CAPACITY
    int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
        ? 0
        : DEFAULT_CAPACITY;

    if (minCapacity > minExpand) {
        // 保证 minCapacity 大于 0
        ensureExplicitCapacity(minCapacity);
    }
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;

    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

// 用在 add(E) 和 add(Collection) 方法中
private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

private static int calculateCapacity(Object[] elementData, int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}

private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    // 新的容量大小是原来的 1.5 倍
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```
有关`overflow-conscious code`的说明参见 [overflow-conscious code.md][overflow]。

## 3.8 toArray
```java
@Override
public Object[] toArray() {
    return Arrays.copyOf(elementData, size);
}

@Override
@SuppressWarnings("unchecked")
public <T> T[] toArray(T[] a) {
    // 参数数组长度小于 size，则新建一个数组返回
    if (a.length < size)
        return (T[]) Arrays.copyOf(elementData, size, a.getClass());
    // 否则填充原来的旧数组
    System.arraycopy(elementData, 0, a, 0, size);
    // 将末尾设为 null，而不是后面所有设为 null。
    if (a.length > size)
        a[size] = null;
    return a;
}
```

## 3.9 indexOf
```java
public int indexOf(Object o) {
    if (o == null) {
        for (int i = 0; i < size; i++)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = 0; i < size; i++)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}

public int lastIndexOf(Object o) {
    if (o == null) {
        for (int i = size-1; i >= 0; i--)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = size-1; i >= 0; i--)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}
```

## 3.10 contains
```java
public boolean contains(Object o) {
    return indexOf(o) >= 0;
}

// containsAll 方法从 AbstractCollection 继承
```

## 3.11 get
```java
public E get(int index) {
    rangeCheck(index);

    return elementData(index);
}

private void rangeCheck(int index) {
    if (index >= size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private String outOfBoundsMsg(int index) {
    return "Index: "+index+", Size: "+size;
}

@SuppressWarnings("unchecked")
E elementData(int index) {
    return (E) elementData[index];
}
```

## 3.12 add
```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

public void add(int index, E element) {
    rangeCheckForAdd(index);

    ensureCapacityInternal(size + 1);  // Increments modCount!!
    // 移动元素
    System.arraycopy(elementData, index, elementData, index + 1, size - index);
    elementData[index] = element;
    size++;
}

public boolean addAll(Collection<? extends E> c) {
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityInternal(size + numNew);  // Increments modCount
    System.arraycopy(a, 0, elementData, size, numNew);
    size += numNew;
    return numNew != 0;
}

public boolean addAll(int index, Collection<? extends E> c) {
    rangeCheckForAdd(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityInternal(size + numNew);  // Increments modCount

    int numMoved = size - index;
    // 如果 index 及之后的位置存在元素，移动它们
    if (numMoved > 0)
        System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

    System.arraycopy(a, 0, elementData, index, numNew);
    size += numNew;
    return numNew != 0;
}
```

## 3.13 remove
```java
public boolean remove(Object o) {
    if (o == null) {
        for (int index = 0; index < size; index++)
            if (elementData[index] == null) {
                fastRemove(index);
                return true;
            }
    } else {
        for (int index = 0; index < size; index++)
            if (o.equals(elementData[index])) {
                fastRemove(index);
                return true;
            }
    }
    return false;
}

public E remove(int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
    elementData[--size] = null; // clear to let GC do its work

    return oldValue;
}

public boolean removeAll(Collection<?> c) {
    Objects.requireNonNull(c);
    return batchRemove(c, false);
}

@Override
public boolean removeIf(Predicate<? super E> filter) {
    Objects.requireNonNull(filter);
    // 先找出所有要删除的元素，在此阶段从 filter 引发的任何异常都将使集合保持不变
    int removeCount = 0;
    // 使用 BitSet 记录删除元素的位置。BitSize 设置时所有位置最初都是 false
    final BitSet removeSet = new BitSet(size);
    final int expectedModCount = modCount;
    final int size = this.size;
    for (int i=0; modCount == expectedModCount && i < size; i++) {
        @SuppressWarnings("unchecked")
        final E element = (E) elementData[i];
        if (filter.test(element)) {
            // 将需要删除的位置设为 true
            removeSet.set(i);
            removeCount++;
        }
    }
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }

    // shift surviving elements left over the spaces left by removed elements
    final boolean anyToRemove = removeCount > 0;
    // 如果有需要删除的元素的话
    if (anyToRemove) {
        // 保留的元素数量
        final int newSize = size - removeCount;
        for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
            // nextClearBit 返回指定的位置及之后第一个设置为 false 的位置，
            // 在这里也就是需要保留的元素位置
            i = removeSet.nextClearBit(i);
            elementData[j] = elementData[i];
        }
        // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
        for (int k=newSize; k < size; k++) {
            elementData[k] = null;  // Let gc do its work
        }
        this.size = newSize;
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    return anyToRemove;
}

// 删除 [fromIndex, toIndex] 范围内的元素。此方法给 SubList 使用
@Override
protected void removeRange(int fromIndex, int toIndex) {
    modCount++;
    int numMoved = size - toIndex;
    System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

    // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
    // clear to let GC do its work
    int newSize = size - (toIndex-fromIndex);
    for (int i = newSize; i < size; i++) {
        elementData[i] = null;
    }
    size = newSize;
}

// 私有 remove 方法，跳过边界检查，并且不返回删除的值。
private void fastRemove(int index) {
    modCount++;
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
    elementData[--size] = null; // clear to let GC do its work
}

/*
此方法会被 removeAll 和 retainAll 调用。

当 complement 为 false 时，保留和 c 中不同的元素，这用在 removeAll 方法中；
当 complement 为 true 时，保留与 c 中相同的元素，这用在 retailAll 方法中。
*/
private boolean batchRemove(Collection<?> c, boolean complement) {
    final Object[] elementData = this.elementData;
    int r = 0, w = 0;
    boolean modified = false;
    try {
        for (; r < size; r++)
            if (c.contains(elementData[r]) == complement)
                // w 表示需要保留的元素移动的下标
                elementData[w++] = elementData[r];
    } finally {
        // finally 块是为了防止 c.contains() 抛出异常，保持与 AbstractCollection 的兼容性

        // 如果 c.contains 抛了异常，那么 r 就会不等于 size。
        if (r != size) {
            // 将 r 及之后的元素移动到最后写入的 w 位置处
            System.arraycopy(elementData, r, elementData, w, size - r);
            w += size - r;
        }
        // w 不等于 size，说明有元素被删除了。
        if (w != size) {
            // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
            // clear to let GC do its work
            for (int i = w; i < size; i++)
                elementData[i] = null;
            modCount += size - w;
            size = w;
            modified = true;
        }
    }
    return modified;
}
```

## 3.14 set
```java
public E set(int index, E element) {
    rangeCheck(index);

    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
}
```

## 3.15 retailAll
```java
public boolean retainAll(Collection<?> c) {
    Objects.requireNonNull(c);
    return batchRemove(c, true);
}
```

## 3.16 replaceAll
```java
@Override
@SuppressWarnings("unchecked")
public void replaceAll(UnaryOperator<E> operator) {
    Objects.requireNonNull(operator);
    final int expectedModCount = modCount;
    final int size = this.size;
    for (int i=0; modCount == expectedModCount && i < size; i++) {
        elementData[i] = operator.apply((E) elementData[i]);
    }
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
    modCount++;
}
```

## 3.17 sort
```java
@Override
@SuppressWarnings("unchecked")
public void sort(Comparator<? super E> c) {
    final int expectedModCount = modCount;
    Arrays.sort((E[]) elementData, 0, size, c);
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
    modCount++;
}
```

## 3.18 clear
```java
public void clear() {
    modCount++;

    // 删除元素时需要注意将其置为 null，不再持有它的引用从而让 GC 清除它，防止内存泄漏
    // clear to let GC do its work
    for (int i = 0; i < size; i++)
        elementData[i] = null;

    size = 0;
}
```

## 3.19 subList
```java
public List<E> subList(int fromIndex, int toIndex) {
    subListRangeCheck(fromIndex, toIndex, size);
    return new SubList(this, 0, fromIndex, toIndex);
}

static void subListRangeCheck(int fromIndex, int toIndex, int size) {
    if (fromIndex < 0)
        throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
    if (toIndex > size)
        throw new IndexOutOfBoundsException("toIndex = " + toIndex);
    if (fromIndex > toIndex)
        throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                           ") > toIndex(" + toIndex + ")");
}
```

## 3.20 序列化
```java
private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out size as capacity for behavioural compatibility with clone()
    s.writeInt(size);

    // 写入 size 个元素，而不是整个数组。注意底层数组是 transient 的。
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}

private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    elementData = EMPTY_ELEMENTDATA;

    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in capacity
    s.readInt(); // ignored

    if (size > 0) {
        // 像 clone() 一样，根据大小而不是容量分配数组
        int capacity = calculateCapacity(elementData, size);
        // SharedSecrets 用于在不使用反射的情况下在另一个程序包中调用私有方法
        SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, capacity);
        ensureCapacityInternal(size);

        Object[] a = elementData;
        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            a[i] = s.readObject();
        }
    }
}
```
之所以写入`size`，是为了兼容性考虑，`JDK6`的相关代码如下：
```java
private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out array length
    s.writeInt(elementData.length);

    // Write out all elements in the proper order.
    for (int i=0; i<size; i++)
        s.writeObject(elementData[i]);

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}

private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in array length and allocate array
    int arrayLength = s.readInt();
    Object[] a = elementData = new Object[arrayLength];

    // Read in all elements in the proper order.
    for (int i=0; i<size; i++)
        a[i] = s.readObject();
}
```
可以看到`JDK6`将数组的实际大小写入，在反序列化出对象时也分配这么多大小。而`JDK8`根据`size`而不是容量分配新的数组。

# 4. 内部类

## 4.1 Itr
```java
// AbstractList.Iter 的优化版本
private class Itr implements Iterator<E> {
    int cursor;       // 下一个将被返回的元素（next 或 previous）的索引。
    int lastRet = -1; // 最近一次调用 next 或 previous 返回的元素的索引。如果调用 remove 删除了此元素，则重置为 -1。
    int expectedModCount = modCount;

    Itr() {}

    public boolean hasNext() {
        return cursor != size;
    }

    @SuppressWarnings("unchecked")
    public E next() {
        checkForComodification();
        int i = cursor;
        if (i >= size)
            throw new NoSuchElementException();
        Object[] elementData = ArrayList.this.elementData;
        // 一般情况下 cursor 绝不会大于等于数组大小，只有在并发修改的时候才会这样
        if (i >= elementData.length)
            throw new ConcurrentModificationException();
        cursor = i + 1;
        return (E) elementData[lastRet = i];
    }

    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();

        try {
            ArrayList.this.remove(lastRet);
            // 这里的 remove 实现和 AbstractList 略有不同
            cursor = lastRet;
            lastRet = -1;
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEachRemaining(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer);
        final int size = ArrayList.this.size;
        int i = cursor;
        if (i >= size) {
            return;
        }
        final Object[] elementData = ArrayList.this.elementData;
        if (i >= elementData.length) {
            throw new ConcurrentModificationException();
        }
        while (i != size && modCount == expectedModCount) {
            consumer.accept((E) elementData[i++]);
        }
        // 在迭代结束时进行一次更新，以减少堆写入流量
        cursor = i;
        lastRet = i - 1;
        checkForComodification();
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

## 4.2 ListItr
```java
// AbstractList.ListItr 的优化版本
private class ListItr extends Itr implements ListIterator<E> {
    ListItr(int index) {
        super();
        cursor = index;
    }

    public boolean hasPrevious() {
        return cursor != 0;
    }

    public int nextIndex() {
        return cursor;
    }

    public int previousIndex() {
        return cursor - 1;
    }

    @SuppressWarnings("unchecked")
    public E previous() {
        checkForComodification();
        int i = cursor - 1;
        if (i < 0)
            throw new NoSuchElementException();
        Object[] elementData = ArrayList.this.elementData;
        if (i >= elementData.length)
            throw new ConcurrentModificationException();
        // 注意 cursor 在获取元素之前就被设为 i
        cursor = i;
        return (E) elementData[lastRet = i];
    }

    public void set(E e) {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();

        try {
            ArrayList.this.set(lastRet, e);
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }

    public void add(E e) {
        checkForComodification();

        try {
            int i = cursor;
            ArrayList.this.add(i, e);
            cursor = i + 1;
            // add 调用之后不能再次调用 remove
            lastRet = -1;
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }
}
```
参见 [ListIterator.md][list-iterator]。

## 4.3 ArrayListSpliterator
```java
static final class ArrayListSpliterator<E> implements Spliterator<E> {
    /*
    如果 ArrayList 是不可变的，或者在结构上是不可变的（没有添加、删除等），我们可以用 Arrays.spliterator。
    我们在遍历过程中检测尽可能多的干扰，而不牺牲太多性能。我们主要依靠 modCount。它不能保证检测到并发冲突，
    而且有时对线程内干扰过于保守，但足以检测到问题。为了实现这一点，我们
    （1）延迟初始化 fence 和 expectedModCount，直到我们需要提交到要检查的状态的最新点。这缩小了发生冲突的时间窗口。
        （SubList 直接使用当前非延迟值创建 Spliterator，它不使用延迟初始化）。
    （2） 在 forEach（对性能最敏感的方法）方法中，我们只在它的的末尾执行一个 ConcurrentModificationException 检查。

    通过延迟初始化，ArrayListSpliterator 实现了后期绑定。并且它也是 fail-fast 的。
    
    当使用 forEach，我们通常只能在操作之后检测干扰，而不能在操作之前检测（与迭代器不同）。
    更多 CME 异常触发检查适用于所有其他可能违反假设的情况：例如，如果 elementData 为 null 或 size() 方法返回更小值，
    可能是由于干扰而发生的。这使得 forEach 的内部循环无需进行检查，并简化 lambda 解析。
     */

    private final ArrayList<E> list;
    private int index; // 当前索引，在 advance/split 时修改
    private int fence; // 最后一个元素（不包括）的索引。直到使用之前是 -1
    private int expectedModCount; // 当 fence 初始化时设置

    // 在 ArrayList 的 spliterator() 方法中，fence 为 -1，expectedModCount 为 0
    ArrayListSpliterator(ArrayList<E> list, int origin, int fence, int expectedModCount) {
        this.list = list; // list 为 null 是可以的，除非开始遍历
        this.index = origin;
        this.fence = fence;
        this.expectedModCount = expectedModCount;
    }

    // 在第一次使用 fence 时（fence 为 -1），将 fence 初始化为 list 的大小，并初始化 expectedModCount
    private int getFence() { // initialize fence to size on first use
        int hi; // (a specialized variant appears in method forEach)
        ArrayList<E> lst;
        if ((hi = fence) < 0) {
            if ((lst = list) == null)
                hi = fence = 0;
            else {
                expectedModCount = lst.modCount;
                hi = fence = lst.size;
            }
        }
        return hi;
    }

    public ArrayListSpliterator<E> trySplit() {
        // 使用 getFence() 进行初始化
        int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
        return (lo >= mid) ? null : // 除非太小，否则分半
            new ArrayListSpliterator<E>(list, lo, index = mid, expectedModCount);
    }

    public boolean tryAdvance(Consumer<? super E> action) {
        if (action == null)
            throw new NullPointerException();
        // 使用 getFence() 进行初始化
        int hi = getFence(), i = index;
        if (i < hi) {
            index = i + 1;
            @SuppressWarnings("unchecked") E e = (E)list.elementData[i];
            action.accept(e);
            // 进行并发修改检查
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }
        return false;
    }

    public void forEachRemaining(Consumer<? super E> action) {
        int i, hi, mc;
        ArrayList<E> lst; Object[] a;
        if (action == null)
            throw new NullPointerException();
        // 如果 list 或 elementData 为 null，则可能发生了并发修改
        if ((lst = list) != null && (a = lst.elementData) != null) {
            if ((hi = fence) < 0) {
                // 还没有初始化就先初始化
                mc = lst.modCount;
                hi = lst.size;
            }
            else
                mc = expectedModCount;
            if ((i = index) >= 0 && (index = hi) <= a.length) {
                for (; i < hi; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) a[i];
                    action.accept(e);
                }
                // 遍历结束之后执行一次并发修改检查
                if (lst.modCount == mc)
                    return;
            }
        }
        throw new ConcurrentModificationException();
    }

    public long estimateSize() {
        return (long) (getFence() - index);
    }

    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
    }
}
```
参见 [Spliterator.md][spliterator]。

## 4.4 SubList
```java
private class SubList extends AbstractList<E> implements RandomAccess {
    private final AbstractList<E> parent;
    private final int parentOffset;  // 父列表的开始下标。SubList 还可以进行 subList 操作
    private final int offset;  // 在 ArrayList 中的开始下标
    int size;

    SubList(AbstractList<E> parent, int offset, int fromIndex, int toIndex) {
        this.parent = parent;
        this.parentOffset = fromIndex;
        this.offset = offset + fromIndex;
        this.size = toIndex - fromIndex;
        this.modCount = ArrayList.this.modCount;
    }

    public E set(int index, E e) {
        rangeCheck(index);
        checkForComodification();
        // set 不引起结构改变，使用 ArrayList 的设置元素方法。
        // 这样当此 SubList 有父 SubList 时，就不用一层一层调用方法
        E oldValue = ArrayList.this.elementData(offset + index);
        ArrayList.this.elementData[offset + index] = e;
        return oldValue;
    }

    public E get(int index) {
        rangeCheck(index);
        checkForComodification();
        // get 不引起结构改变，使用 ArrayList 的获取元素方法
        return ArrayList.this.elementData(offset + index);
    }

    public int size() {
        checkForComodification();
        return this.size;
    }

    public void add(int index, E e) {
        rangeCheckForAdd(index);
        checkForComodification();
        // add 引起结构改变，使用父列表的添加元素方法。如果直接调用 ArrayList 的方法，
        // 那么父 SubList 的数据就不会更新，导致出错
        parent.add(parentOffset + index, e);
        this.modCount = parent.modCount;
        this.size++;
    }

    public E remove(int index) {
        rangeCheck(index);
        checkForComodification();
        // remove 引起结构改变，使用父列表的删除元素方法
        E result = parent.remove(parentOffset + index);
        this.modCount = parent.modCount;
        this.size--;
        return result;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        checkForComodification();
        parent.removeRange(parentOffset + fromIndex,
                           parentOffset + toIndex);
        this.modCount = parent.modCount;
        this.size -= toIndex - fromIndex;
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(this.size, c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        int cSize = c.size();
        if (cSize==0)
            return false;

        checkForComodification();
        parent.addAll(parentOffset + index, c);
        this.modCount = parent.modCount;
        this.size += cSize;
        return true;
    }

    public Iterator<E> iterator() {
        // iterator() 同样用的是 listIterator
        return listIterator();
    }

    public ListIterator<E> listIterator(final int index) {
        checkForComodification();
        rangeCheckForAdd(index);
        final int offset = this.offset;

        return new ListIterator<E>() {
            int cursor = index;
            int lastRet = -1;
            int expectedModCount = ArrayList.this.modCount;

            public boolean hasNext() {
                return cursor != SubList.this.size;
            }

            @SuppressWarnings("unchecked")
            public E next() {
                checkForComodification();
                int i = cursor;
                if (i >= SubList.this.size)
                    throw new NoSuchElementException();
                // next 方法不引起结构改变，使用 ArrayList 的获取元素方法
                Object[] elementData = ArrayList.this.elementData;
                if (offset + i >= elementData.length)
                    throw new ConcurrentModificationException();
                cursor = i + 1;
                return (E) elementData[offset + (lastRet = i)];
            }

            public boolean hasPrevious() {
                return cursor != 0;
            }

            @SuppressWarnings("unchecked")
            public E previous() {
                checkForComodification();
                int i = cursor - 1;
                if (i < 0)
                    throw new NoSuchElementException();
                // previous 方法不引起结构改变，使用 ArrayList 的获取元素方法
                Object[] elementData = ArrayList.this.elementData;
                if (offset + i >= elementData.length)
                    throw new ConcurrentModificationException();
                cursor = i;
                return (E) elementData[offset + (lastRet = i)];
            }

            @SuppressWarnings("unchecked")
            public void forEachRemaining(Consumer<? super E> consumer) {
                Objects.requireNonNull(consumer);
                final int size = SubList.this.size;
                int i = cursor;
                if (i >= size) {
                    return;
                }
                // forEach 方法不引起结构改变，使用 ArrayList 的获取元素方法
                final Object[] elementData = ArrayList.this.elementData;
                if (offset + i >= elementData.length) {
                    throw new ConcurrentModificationException();
                }
                while (i != size && modCount == expectedModCount) {
                    consumer.accept((E) elementData[offset + (i++)]);
                }
                lastRet = cursor = i;
                checkForComodification();
            }

            public int nextIndex() {
                return cursor;
            }

            public int previousIndex() {
                return cursor - 1;
            }

            public void remove() {
                if (lastRet < 0)
                    throw new IllegalStateException();
                checkForComodification();

                try {
                    // remove 方法引起结构改变，使用 SubList 的删除元素方法
                    SubList.this.remove(lastRet);
                    cursor = lastRet;
                    lastRet = -1;
                    expectedModCount = ArrayList.this.modCount;
                } catch (IndexOutOfBoundsException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            public void set(E e) {
                if (lastRet < 0)
                    throw new IllegalStateException();
                checkForComodification();

                try {
                    // set 方法不引起结构改变，使用 ArrayList 的设置元素方法
                    ArrayList.this.set(offset + lastRet, e);
                } catch (IndexOutOfBoundsException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            public void add(E e) {
                checkForComodification();

                try {
                    int i = cursor;
                    // add 方法引起结构改变，使用 SubList 的添加元素方法
                    SubList.this.add(i, e);
                    cursor = i + 1;
                    lastRet = -1;
                    expectedModCount = ArrayList.this.modCount;
                } catch (IndexOutOfBoundsException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            final void checkForComodification() {
                if (expectedModCount != ArrayList.this.modCount)
                    throw new ConcurrentModificationException();
            }
        };
    }

    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, offset, fromIndex, toIndex);
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= this.size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > this.size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+this.size;
    }

    private void checkForComodification() {
        if (ArrayList.this.modCount != this.modCount)
            throw new ConcurrentModificationException();
    }

    public Spliterator<E> spliterator() {
        checkForComodification();
        return new ArrayListSpliterator<E>(ArrayList.this, offset,
                                           offset + this.size, this.modCount);
    }
}
```
参见 [AbstractList.md][abstract-list] 和 [RandomAccess.md][random-access]。


[list]: List.md
[abstract-collection]: AbstractCollection.md
[abstract-list]: AbstractList.md
[spliterator]: Spliterator.md
[overflow]: overflow-conscious%20code.md
[list-iterator]: ListIterator.md
[random-access]: RandomAccess.md