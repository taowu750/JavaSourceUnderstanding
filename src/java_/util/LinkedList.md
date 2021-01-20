`java.util.LinkedList`类的声明如下：
```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```
`List`和`Deque`接口的双链表实现。实现所有可选的列表操作，并允许所有元素（包括`null`）。
所有操作均按双链表的预期执行。使用索引的操作将遍历列表，以更接近指定索引的位置为准。

请注意，此实现未同步。如果多个线程同时访问链表，并且至少一个线程在结构上修改了链表，则必须在外部进行同步。
（结构修改是添加或删除一个或多个元素的任何操作；仅设置元素的值不是结构修改。）
通常通过在封装链表的某个对象上进行同步来完成此操作。如果不存在这样的对象，
则应使用`Collections.synchronizedList`方法“包装”列表。最好在创建时完成此操作，
以防止意外的不同步访问列表：
```java
List list = Collections.synchronizedList(new LinkedList(...));
```

此类的迭代器和`listIterator`方法返回的迭代器是快速失败的：
如果在创建迭代器之后的任何时间以任何方式对列表进行结构修改，则除了通过迭代器自己的`remove`或`add`方法之外，
迭代器都会抛出`ConcurrentModificationException`。因此，面对并发修改，迭代器会快速干净地失败，
而不会在未来的不确定时间冒着任意，不确定的行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为通常来说，在存在不同步的并发修改的情况下，
不可能做出任何严格的保证。快速失败的迭代器会尽最大努力抛出`ConcurrentModificationException`。
因此，编写依赖于此异常的程序的正确性是错误的：迭代器的快速失败行为应仅用于检测错误。

此类双端队列方法说明参见 [Deque.md][deque]。一些方法的实现存在于 [AbstractCollection.md][abstract-collection]、
[AbstractList.md][abstract-list] 和 [AbstractSequentialList.md][abstract-sequential-list] 中。

# 1. 成员字段
```java
transient int size = 0;

// 头结点，注意它会执行实际的元素。
// 满足不等式: (first == null && last == null) || (first.prev == null && first.item != null)
transient Node<E> first;

// 尾结点，注意它会执行实际的元素。
// 满足不等式: (first == null && last == null) || (last.next == null && last.item != null)
transient Node<E> last;
```

# 2. 构造器
```java
public LinkedList() {
}

public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

# 3. 内部类

## 3.1 Node
```java
// 结点类
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

## 3.2 ListItr
```java
// 列表迭代器
private class ListItr implements ListIterator<E> {
    // 最后一次调用 next 或 prev 方法得到的结点
    private Node<E> lastReturned;
    // 下次调用 next 方法得到的结点
    private Node<E> next;
    // 下次调用 nextIndex 方法返回下标
    private int nextIndex;
    // modCount 来自 AbstractList
    private int expectedModCount = modCount;

    ListItr(int index) {
        // assert isPositionIndex(index);
        // 如果 index 等于 size，则下一结点为 null；否则获取 index 处的结点
        next = (index == size) ? null : node(index);
        nextIndex = index;
    }

    public boolean hasNext() {
        return nextIndex < size;
    }

    public E next() {
        checkForComodification();
        if (!hasNext())
            throw new NoSuchElementException();

        lastReturned = next;
        next = next.next;
        nextIndex++;
        return lastReturned.item;
    }

    public boolean hasPrevious() {
        return nextIndex > 0;
    }

    public E previous() {
        checkForComodification();
        if (!hasPrevious())
            throw new NoSuchElementException();

        // next 等于 null，返回尾结点的值；
        // 否则返回 next 前一个结点的值
        lastReturned = next = (next == null) ? last : next.prev;
        nextIndex--;
        return lastReturned.item;
    }

    public int nextIndex() {
        return nextIndex;
    }

    public int previousIndex() {
        return nextIndex - 1;
    }

    public void remove() {
        checkForComodification();
        // 如果之前没有调用过 next 或 previous 方法；
        // 或调用这些方法之后又调用了 add 方法，则抛出异常
        if (lastReturned == null)
            throw new IllegalStateException();

        // 删除上次的结点
        Node<E> lastNext = lastReturned.next;
        unlink(lastReturned);
        if (next == lastReturned)
            next = lastNext;
        else
            nextIndex--;
        // 删除之后将 lastReturned 置为 null
        lastReturned = null;
        expectedModCount++;
    }

    public void set(E e) {
        if (lastReturned == null)
            throw new IllegalStateException();
        // set 不改变 expectedModCount
        checkForComodification();
        lastReturned.item = e;
    }

    public void add(E e) {
        checkForComodification();
        // 将 lastReturned 置为 null
        lastReturned = null;
        if (next == null)
            // 如果 next 为 null，插入一个尾结点
            linkLast(e);
        else
            // 否则在 next 之前插入一个结点
            linkBefore(e, next);
        nextIndex++;
        expectedModCount++;
    }

    public void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (modCount == expectedModCount && nextIndex < size) {
            action.accept(next.item);
            lastReturned = next;
            next = next.next;
            nextIndex++;
        }
        checkForComodification();
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```
参见 [ListIterator.md][list-iterator]。

## 3.3 DescendingIterator
```java
// 逆向迭代器，使用 ListItr.hasPrevious 和 ListItr.previous
private class DescendingIterator implements Iterator<E> {
    private final ListItr itr = new ListItr(size());
    public boolean hasNext() {
        return itr.hasPrevious();
    }
    public E next() {
        return itr.previous();
    }
    public void remove() {
        itr.remove();
    }
}
```

## 3.4 LLSpliterator
```java
// 双链表的 Spliterator 实现
static final class LLSpliterator<E> implements Spliterator<E> {

    /*
    和 ArrayList 的 Spliterator 实现相似，此类也直到调用方法的时候才对状态进行初始化，
    这缩小了发生冲突的时间窗口。

    通过延迟初始化，LLSpliterator 实现了后期绑定。并且它也是 fail-fast 的。
     */

    static final int BATCH_UNIT = 1 << 10 /* 1024 */;  // 切分数组大小增量
    static final int MAX_BATCH = 1 << 25 /* 33554432 */;  // 最大的切分数组大小
    final LinkedList<E> list; // null OK unless traversed
    Node<E> current;      // 当前结点；直到第一次使用之前是 null
    int est;              // 估计的大小; 直到第一次使用之前是 -1
    int expectedModCount; // 当 est 被初始化时设置
    int batch;            // 切分大小

    LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
        this.list = list;
        this.est = est;
        this.expectedModCount = expectedModCount;
    }

    // 返回估计的大小。此方法直到调用的时候才会初始化
    final int getEst() {
        int s;
        final LinkedList<E> lst;
        if ((s = est) < 0) {
            if ((lst = list) == null)
                s = est = 0;
            else {
                expectedModCount = lst.modCount;
                // current 初始化为链表头结点
                current = lst.first;
                s = est = lst.size;
            }
        }
        return s;
    }

    public long estimateSize() { return (long) getEst(); }

    public Spliterator<E> trySplit() {
        Node<E> p;
        int s = getEst();
        if (s > 1 && (p = current) != null) {
            // 在上一次批量的大小基础上增加一个单元
            int n = batch + BATCH_UNIT;
            // 保证批量大小不超过链表大小和 MAX_BATCH
            if (n > s)
                n = s;
            if (n > MAX_BATCH)
                n = MAX_BATCH;
            // 将切分区域的元素写入数组中
            Object[] a = new Object[n];
            int j = 0;
            do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
            current = p;
            batch = j;
            est = s - j;
            return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
        }
        return null;
    }

    public void forEachRemaining(Consumer<? super E> action) {
        Node<E> p; int n;
        if (action == null) throw new NullPointerException();
        if ((n = getEst()) > 0 && (p = current) != null) {
            current = null;
            est = 0;
            do {
                E e = p.item;
                p = p.next;
                action.accept(e);
            } while (p != null && --n > 0);
        }
        // 最后检查一次 modCount
        if (list.modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }

    public boolean tryAdvance(Consumer<? super E> action) {
        Node<E> p;
        if (action == null) throw new NullPointerException();
        if (getEst() > 0 && (p = current) != null) {
            --est;
            E e = p.item;
            current = p.next;
            action.accept(e);
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }
        return false;
    }

    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
    }
}
```
参见 [Spliterator.md][spliterator] 和 [ArrayList.md][array-list] 第 4.3 节 ArrayListSpliterator。

# 4. 方法

## 4.1 node
```java
// 获取 index 处的结点。此方法不对 index 检查
Node<E> node(int index) {
    // assert isElementIndex(index);

    // 如果 index 更靠近头结点，从头结点开始移动
    if (index < (size >> 1)) {
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;

    // 否则如果 index 更靠近尾结点，从尾结点开始移动
    } else {
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

## 4.2 link
```java
// 插入头结点
private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;
    else
        f.prev = newNode;
    size++;
    modCount++;
}

// 插入尾结点
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}

// 在 succ 前面插入结点
void linkBefore(E e, Node<E> succ) {
    // assert succ != null;
    final Node<E> pred = succ.prev;
    final Node<E> newNode = new Node<>(pred, e, succ);
    succ.prev = newNode;
    if (pred == null)
        first = newNode;
    else
        pred.next = newNode;
    size++;
    modCount++;
}
```

## 4.3 unlink
```java
// 将结点 x 从链表中删除，返回删除的值
E unlink(Node<E> x) {
    // assert x != null;
    final E element = x.item;
    final Node<E> next = x.next;
    final Node<E> prev = x.prev;

    // 处理前一个结点，注意头结点的处理
    if (prev == null) {
        first = next;
    } else {
        prev.next = next;
        x.prev = null;
    }

    // 处理后一个结点，注意尾结点的处理
    if (next == null) {
        last = prev;
    } else {
        next.prev = prev;
        x.next = null;
    }

    x.item = null;
    size--;
    modCount++;
    return element;
}

// 删除头结点
private E unlinkFirst(Node<E> f) {
    // assert f == first && f != null;
    final E element = f.item;
    final Node<E> next = f.next;
    f.item = null;
    f.next = null; // help GC
    first = next;
    if (next == null)
        last = null;
    else
        next.prev = null;
    size--;
    modCount++;
    return element;
}

// 删除尾结点
private E unlinkLast(Node<E> l) {
    // assert l == last && l != null;
    final E element = l.item;
    final Node<E> prev = l.prev;
    l.item = null;
    l.prev = null; // help GC
    last = prev;
    if (prev == null)
        first = null;
    else
        prev.next = null;
    size--;
    modCount++;
    return element;
}
```

## 4.4 check
```java
// 判断参数是否为现有元素的索引。
private boolean isElementIndex(int index) {
    return index >= 0 && index < size;
}

// 判断参数是迭代器或添加操作的有效位置的索引。
private boolean isPositionIndex(int index) {
    return index >= 0 && index <= size;
}

private void checkElementIndex(int index) {
    if (!isElementIndex(index))
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private void checkPositionIndex(int index) {
    if (!isPositionIndex(index))
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private String outOfBoundsMsg(int index) {
    return "Index: "+index+", Size: "+size;
}
```

## 4.5 clone
```java
// 返回此 LinkedList 的浅拷贝。（元素本身不会被克隆。）
public Object clone() {
    LinkedList<E> clone = superClone();

    // 将副本变为初始状态
    clone.first = clone.last = null;
    clone.size = 0;
    clone.modCount = 0;

    // 将当前链表的值添加到副本中
    for (Node<E> x = first; x != null; x = x.next)
        clone.add(x.item);

    return clone;
}

private LinkedList<E> superClone() {
    try {
        return (LinkedList<E>) super.clone();
    } catch (CloneNotSupportedException e) {
        throw new InternalError(e);
    }
}
```

## 4.6 iterator
```java
/*
从列表中的指定位置开始（按适当顺序）返回此列表中元素的列表迭代器。遵守 List.listIterator(int) 的常规协定。

列表迭代器是快速失败的：如果在创建迭代器之后的任何时间对列表进行结构修改，
则除了通过列表迭代器自身的 remove 或 add 方法之外，
列表迭代器都会抛出 ConcurrentModificationException。因此，面对并发修改，
迭代器会快速干净地失败，而不会在未来的不确定时间冒着任意，不确定的行为的风险。
*/
public ListIterator<E> listIterator(int index) {
    checkPositionIndex(index);
    return new ListItr(index);
}

// 逆序迭代器，Deque 的方法
public Iterator<E> descendingIterator() {
    return new DescendingIterator();
}
```

## 4.7 spliterator
```java
/*
在此列表中的元素上创建后期绑定和快速失败的 Spliterator。

此 Spliterator 具有 Spliterator.SIZED、Spliterator.ORDERED 和 Spliterator.SUBSIZED 特征。
*/
@Override
public Spliterator<E> spliterator() {
    return new LLSpliterator<E>(this, -1, 0);
}
```

## 4.8 插入开头
```java
public void addFirst(E e) {
    linkFirst(e);
}

public boolean offerFirst(E e) {
    addFirst(e);
    return true;
}

public void push(E e) {
    addFirst(e);
}
```

## 4.9 插入末尾
```java
public void addLast(E e) {
    linkLast(e);
}

public boolean offerLast(E e) {
    addLast(e);
    return true;
}

public boolean add(E e) {
    linkLast(e);
    return true;
}

public boolean offer(E e) {
    return add(e);
}
```

## 4.10 删除开头
```java
public E removeFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return unlinkFirst(f);
}

public E pollFirst() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}

public E pop() {
    return removeFirst();
}

public E remove() {
    return removeFirst();
}

public E poll() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}
```

## 4.11 删除末尾
```java
public E removeLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return unlinkLast(l);
}

public E pollLast() {
    final Node<E> l = last;
    return (l == null) ? null : unlinkLast(l);
}
```

## 4.12 删除指定下标/值
```java
public E remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));
}

public boolean remove(Object o) {
    if (o == null) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}

public boolean removeFirstOccurrence(Object o) {
    return remove(o);
}

public boolean removeLastOccurrence(Object o) {
    if (o == null) {
        for (Node<E> x = last; x != null; x = x.prev) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        for (Node<E> x = last; x != null; x = x.prev) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}
```

## 4.13 检查开头
```java
public E getFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return f.item;
}

public E peekFirst() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}

public E element() {
    return getFirst();
}

public E peek() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}
```

## 4.14 检查末尾
```java
public E getLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return l.item;
}

public E peekLast() {
    final Node<E> l = last;
    return (l == null) ? null : l.item;
}
```

## 4.15 size
```java
public int size() {
    return size;
}
```

## 4.16 toArray
```java
public Object[] toArray() {
    Object[] result = new Object[size];
    int i = 0;
    for (Node<E> x = first; x != null; x = x.next)
        result[i++] = x.item;
    return result;
}

@SuppressWarnings("unchecked")
public <T> T[] toArray(T[] a) {
    if (a.length < size)
        a = (T[])java.lang.reflect.Array.newInstance(
                a.getClass().getComponentType(), size);
    int i = 0;
    Object[] result = a;
    for (Node<E> x = first; x != null; x = x.next)
        result[i++] = x.item;

    if (a.length > size)
        a[size] = null;

    return a;
}
```

## 4.17 indexOf
```java
public int indexOf(Object o) {
    int index = 0;
    if (o == null) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null)
                return index;
            index++;
        }
    } else {
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item))
                return index;
            index++;
        }
    }
    return -1;
}

public int lastIndexOf(Object o) {
    int index = size;
    if (o == null) {
        for (Node<E> x = last; x != null; x = x.prev) {
            index--;
            if (x.item == null)
                return index;
        }
    } else {
        for (Node<E> x = last; x != null; x = x.prev) {
            index--;
            if (o.equals(x.item))
                return index;
        }
    }
    return -1;
}
```

## 4.18 contains
```java
public boolean contains(Object o) {
    return indexOf(o) != -1;
}
```

## 4.19 get
```java
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}
```

## 4.20 add
```java
public void add(int index, E element) {
    checkPositionIndex(index);

    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));
}

public boolean addAll(Collection<? extends E> c) {
    return addAll(size, c);
}

public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    if (numNew == 0)
        return false;

    // 找到插入的前驱和后继结点
    Node<E> pred, succ;
    if (index == size) {
        succ = null;
        pred = last;
    } else {
        succ = node(index);
        pred = succ.prev;
    }

    // 将所有元素插入到指定位置处
    for (Object o : a) {
        @SuppressWarnings("unchecked") E e = (E) o;
        Node<E> newNode = new Node<>(pred, e, null);
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        pred = newNode;
    }

    // 链接后继结点
    if (succ == null) {
        last = pred;
    } else {
        pred.next = succ;
        succ.prev = pred;
    }

    size += numNew;
    modCount++;
    return true;
}
```

## 4.21 set
```java
public E set(int index, E element) {
    checkElementIndex(index);
    Node<E> x = node(index);
    E oldVal = x.item;
    x.item = element;
    return oldVal;
}
```

## 4.22 序列化
```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
    // Write out any hidden serialization magic
    s.defaultWriteObject();

    // Write out size
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (Node<E> x = first; x != null; x = x.next)
        s.writeObject(x.item);
}

@SuppressWarnings("unchecked")
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // Read in any hidden serialization magic
    s.defaultReadObject();

    // Read in size
    int size = s.readInt();

    // Read in all elements in the proper order.
    for (int i = 0; i < size; i++)
        linkLast((E)s.readObject());
}
```


[deque]: Deque.md
[abstract-collection]: AbstractCollection.md
[abstract-list]: AbstractList.md
[abstract-sequential-list]: AbstractSequentialList.md
[list-iterator]: ListIterator.md
[spliterator]: Spliterator.md
[array-list]: ArrayList.md