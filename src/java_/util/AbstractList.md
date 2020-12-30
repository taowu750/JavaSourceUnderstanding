`java.util.AbstractList`抽象类的声明如下：
```java
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>
```
此类提供了`List`接口的基本实现，以最小化实现**随机访问**数据存储（例如数组）所需的工作。
对于**顺序访问**数据存储（例如链表），应优先使用`AbstractSequentialList`代替此类。

要实现不可修改的列表，程序员仅需扩展此类并为`get(int)`和`size()`方法提供实现。
要实现可修改的列表，程序员必须另外重写`set(int, E)`方法（否则将抛出`UnsupportedOperationException`）。
如果列表是可变大小的，则程序员必须另外重写`add(int, E)`和`remove(int)`方法。

根据`Collection`接口规范中的建议，程序员通常应提供一个无参构造器和接受一个`Collection`参数的构造器。

不像其他的抽象集合实现，程序员不必提供迭代器实现; 迭代器和列表迭代器使用此类的“随机访问”方法实现：
 - `get(int)`
 - `set(int, E)`
 - `add(int, E)`
 - `remove(int)`

此类中每个非抽象方法的文档都详细描述了其实现。如果有更高效的实现，则可以覆盖这些方法。

方法的约定和其他一些默认实现参见 [List.md][list] 和 [AbstractCollection.md][abstract-collection]。

# 1. 成员字段
```java
/*
已对该列表进行结构修改的次数。结构修改是指更改列表大小，或者以某种方式扰乱列表，使得正在进行的迭代可能产生不正确的结果。

该字段由 iterator 和 listIterator 方法返回的迭代器和列表迭代器实现使用。如果此字段的值意外更改，
则迭代器（或列表迭代器）将在 next，remove，previous，set 或 add 调用时抛出 ConcurrentModificationException。
面对迭代期间的并发修改，这提供了 fail-fast 保证。

子类对该字段的使用是可选的。如果子类希望提供 fail-fast 的迭代器（和列表迭代器），
则只需在其 add(int, E) 和 remove(int) 方法，以及任何其他覆盖该方法而导致修改列表结构的方法中递增此字段。
一次调用 add(int, E) 或 remove(int) 不得增加多次此字段，否则迭代器（和列表迭代器）将错误的抛出 ConcurrentModificationException。
如果实现不希望提供 fail-fast 迭代器，则可以忽略此字段。
*/
protected transient int modCount = 0;
```

# 2. 构造器
```java
protected AbstractList() {
}
```

# 3. 方法

## 3.1 抽象方法
```java
abstract public E get(int index);
```

## 3.2 equals
```java
public boolean equals(Object o) {
    if (o == this)
        return true;
    if (!(o instanceof List))
        return false;

    ListIterator<E> e1 = listIterator();
    ListIterator<?> e2 = ((List<?>) o).listIterator();
    while (e1.hasNext() && e2.hasNext()) {
        E o1 = e1.next();
        Object o2 = e2.next();
        if (!(o1==null ? o2==null : o1.equals(o2)))
            return false;
    }
    return !(e1.hasNext() || e2.hasNext());
}
```

## 3.3 hashCode
```java
public int hashCode() {
    int hashCode = 1;
    for (E e : this)
        hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
    return hashCode;
}
```

## 3.4 iterator
```java
/*
此实现依赖于列表的 size()，get(int) 和 remove(int) 方法。

请注意，除非重写列表的 remove(int) 方法，否则此方法返回的迭代器将引发 UnsupportedOperationException。

此实现使用 modCount 字段针对并发修改而抛出 ConcurrentModificationException。这提供了 fail-fast 保证。
*/
public Iterator<E> iterator() {
    return new Itr();
}
```

## 3.5 listIterator
```java
/*
此实现扩展了 iterator() 方法返回的 Iterator 接口的实现。ListIterator 实现依赖于列表的 get(int)，set(int, E)，
add(int, E) 和 remove(int) 方法。

请注意，除非重写列表的 remove(int)，set(int, E) 和 add(int, E) 方法，否则此实现返回的列表迭代器将
抛出 UnsupportedOperationException。

此实现使用 modCount 字段针对并发修改而抛出 ConcurrentModificationException。这提供了 fail-fast 保证。
*/
public ListIterator<E> listIterator(final int index) {
    rangeCheckForAdd(index);

    return new ListItr(index);
}

public ListIterator<E> listIterator() {
    return listIterator(0);
}

private void rangeCheckForAdd(int index) {
    if (index < 0 || index > size())
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private String outOfBoundsMsg(int index) {
    return "Index: "+index+", Size: "+size();
}
```

## 3.6 add
```java
public void add(int index, E element) {
    throw new UnsupportedOperationException();
}

/*
此实现调用add(size(), e)。

请注意，除非重写 add(int, E) 方法否则此实现将引发 UnsupportedOperationException。
*/
public boolean add(E e) {
    add(size(), e);
    return true;
}

/*
此实现在指定的集合上获取一个迭代器并对其进行迭代，使用 add(int, E) 将迭代器中获得的元素插入到此列表的适当位置，
一次插入一个。为了提高效率，许多实现将覆盖此方法。

请注意，除非重写 add(int, E) 否则此实现将引发 UnsupportedOperationException。
*/
public boolean addAll(int index, Collection<? extends E> c) {
    rangeCheckForAdd(index);
    boolean modified = false;
    for (E e : c) {
        add(index++, e);
        modified = true;
    }
    return modified;
}
```

## 3.7 remove
```java
public E remove(int index) {
    throw new UnsupportedOperationException();
}

/*
从此列表中删除索引在 fromIndex （包括）和 toIndex （不包括）之间的所有元素。将所有后续元素向左移动（减少其索引）。
此调用将使列表减少 (toIndex - fromIndex) 个元素。（如果 toIndex==fromIndex，则此操作不做任何事。）

此列表及其子列表的 clear 操作会调用此方法。重写此方法可以大大提高对该列表及其子列表进行 clear 操作的性能。

注意：如果 ListIterator.remove 需要线性时间，则此实现需要二次时间。
*/
protected void removeRange(int fromIndex, int toIndex) {
    ListIterator<E> it = listIterator(fromIndex);
    for (int i=0, n=toIndex-fromIndex; i<n; i++) {
        it.next();
        it.remove();
    }
}
```

## 3.8 set
```java
public E set(int index, E element) {
    throw new UnsupportedOperationException();
}
```

## 3.9 index
```java
public int indexOf(Object o) {
    ListIterator<E> it = listIterator();
    if (o==null) {
        while (it.hasNext())
            if (it.next()==null)
                return it.previousIndex();
    } else {
        while (it.hasNext())
            if (o.equals(it.next()))
                return it.previousIndex();
    }
    return -1;
}

public int lastIndexOf(Object o) {
    ListIterator<E> it = listIterator(size());
    if (o==null) {
        while (it.hasPrevious())
            if (it.previous()==null)
                return it.nextIndex();
    } else {
        while (it.hasPrevious())
            if (o.equals(it.previous()))
                return it.nextIndex();
    }
    return -1;
}
```

## 3.10 clear
```java
/*
此实现调用removeRange(0, size())。

请注意，除非重写 remove(int index) 或 removeRange(int fromIndex, int toIndex) 方法，
否则此实现将引发 UnsupportedOperationException。
*/
public void clear() {
    removeRange(0, size());
}
```

## 3.11 subList
```java
/*
此实现返回一个列表，该列表是 AbstractList 的子类。子类在专用字段中存储底层支持列表中子列表的偏移量，
子列表的大小（可以在其生存期内更改）以及底层支持列表的 modCount 值。

子类有两种变体，其中一种实现 RandomAccess。如果此列表实现 RandomAccess 则返回的列表将是实现 RandomAccess 的子类的实例。
子类的 set(int, E)，get(int)，add(int, E)，remove(int)，addAll(int, Collection) 和 removeRange(int, int)
方法在进行边界检查并调整偏移量之后委托给底层支持列表。addAll(Collection c) 方法使用 addAll(size, c)。

listIterator(int) 方法返回底层支持列表的列表迭代器上的“包装对象”，该列表迭代器是使用底层支持列表上的相应方法创建的。
iterator 方法仅返回 listIterator()，而 size 方法仅返回子类的 size 字段。

所有方法都首先检查底层支持列表的实际 modCount 是否等于其期望值，如果不是，则抛出 ConcurrentModificationException。
*/
public List<E> subList(int fromIndex, int toIndex) {
    return (this instanceof RandomAccess ?
            new RandomAccessSubList<>(this, fromIndex, toIndex) :
            new SubList<>(this, fromIndex, toIndex));
}
```

# 4. 内部类

## 4.1 Iter
```java
// AbstractList 的 Iterator 的默认实现
private class Itr implements Iterator<E> {
    // 后续调用 next 返回的元素的索引。
    int cursor = 0;
    // 最近一次调用 next 或 previous 返回的元素的索引。如果调用 remove 删除了此元素，则重置为 -1。
    int lastRet = -1;
    // 迭代器认为底层支持列表应该具有的 modCount 值。如果不相等，则迭代器检测到并发修改。
    int expectedModCount = modCount;

    public boolean hasNext() {
        return cursor != size();
    }

    public E next() {
        checkForComodification();
        try {
            int i = cursor;
            // 此迭代器使用 get(int) 方法获取元素
            E next = get(i);
            lastRet = i;
            cursor = i + 1;
            return next;
        } catch (IndexOutOfBoundsException e) {
            checkForComodification();
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();

        try {
            // 此迭代器使用 remove(int) 方法删除元素
            AbstractList.this.remove(lastRet);
            // 调用 next 时，lastRet 会小于 cursor；而调用 previous 时，lastRet 会大于 cursor。
            // 注意删除元素，右边的元素会往左移，而左边的元素不动。
            if (lastRet < cursor)
                cursor--;
            lastRet = -1;
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

## 4.2 ListItr
```java
private class ListItr extends Itr implements ListIterator<E> {
    ListItr(int index) {
        cursor = index;
    }

    public boolean hasPrevious() {
        return cursor != 0;
    }

    public E previous() {
        checkForComodification();
        try {
            int i = cursor - 1;
            // 此迭代器使用 get(int) 方法获取元素
            E previous = get(i);
            lastRet = cursor = i;
            return previous;
        } catch (IndexOutOfBoundsException e) {
            checkForComodification();
            throw new NoSuchElementException();
        }
    }

    public int nextIndex() {
        return cursor;
    }

    public int previousIndex() {
        return cursor-1;
    }

    public void set(E e) {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();

        try {
            // 此迭代器使用 set(int, E) 方法设置元素
            AbstractList.this.set(lastRet, e);
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }

    public void add(E e) {
        checkForComodification();

        try {
            int i = cursor;
            // 此迭代器使用 add(int, E) 方法添加元素。
            AbstractList.this.add(i, e);
            // 此 add 方法不使用 lastRet 字段，并会重置 lastRet
            lastRet = -1;
            cursor = i + 1;
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }
}
```

# 5. 其他类

## 5.1 SubList
```java
class SubList<E> extends AbstractList<E>
```
`SubList`为`AbstractList.subList`方法提供，表示`AbstractList`的一个视图。

### 5.1.1 成员字段
```java
// 底层支持列表
private final AbstractList<E> l;
// 此 SubList 在 AbstractList 中的偏移量，也就是子列表在 AbstractList 的开始下标。
private final int offset;
// 此 SubList 的元素数量
private int size;
``` 

### 5.1.2 构造器
```java
SubList(AbstractList<E> list, int fromIndex, int toIndex) {
    if (fromIndex < 0)
        throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
    if (toIndex > list.size())
        throw new IndexOutOfBoundsException("toIndex = " + toIndex);
    if (fromIndex > toIndex)
        throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                           ") > toIndex(" + toIndex + ")");
    l = list;
    offset = fromIndex;
    size = toIndex - fromIndex;
    this.modCount = l.modCount;
}
```

### 5.1.3 方法

#### 5.1.3.1 检查方法
```java
private void rangeCheck(int index) {
    if (index < 0 || index >= size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private void rangeCheckForAdd(int index) {
    if (index < 0 || index > size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private String outOfBoundsMsg(int index) {
    return "Index: "+index+", Size: "+size;
}

private void checkForComodification() {
    if (this.modCount != l.modCount)
        throw new ConcurrentModificationException();
}
```

#### 5.1.3.2 iterator
```java
public Iterator<E> iterator() {
    return listIterator();
}

public ListIterator<E> listIterator(final int index) {
    checkForComodification();
    rangeCheckForAdd(index);

    return new ListIterator<E>() {
        // 使用底层支持列表的 ListIterator
        private final ListIterator<E> i = l.listIterator(index+offset);

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return i.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return i.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            // 返回的 index 是相对于此 SubList，所以减去偏移量
            return i.nextIndex() - offset;
        }

        public int previousIndex() {
            // 返回的 index 是相对于此 SubList，所以减去偏移量
            return i.previousIndex() - offset;
        }

        public void remove() {
            i.remove();
            SubList.this.modCount = l.modCount;
            size--;
        }

        public void set(E e) {
            i.set(e);
        }

        public void add(E e) {
            i.add(e);
            SubList.this.modCount = l.modCount;
            size++;
        }
    };
}
```

#### 5.1.3.3 size
```java
public int size() {
    checkForComodification();
    return size;
}
```

#### 5.1.3.4 get
```java
public E get(int index) {
    rangeCheck(index);
    checkForComodification();
    // 使用底层支持列表的 get 方法
    return l.get(index+offset);
}
```

#### 5.1.3.5 add
```java
public void add(int index, E element) {
    rangeCheckForAdd(index);
    checkForComodification();
    // 使用底层支持列表的 add 方法
    l.add(index+offset, element);
    this.modCount = l.modCount;
    size++;
}

public boolean addAll(Collection<? extends E> c) {
    return addAll(size, c);
}

public boolean addAll(int index, Collection<? extends E> c) {
    rangeCheckForAdd(index);
    int cSize = c.size();
    if (cSize==0)
        return false;

    checkForComodification();
    // 使用底层支持列表的 addAll 方法
    l.addAll(offset+index, c);
    this.modCount = l.modCount;
    size += cSize;
    return true;
}
```

#### 5.1.3.6 remove
```java
public E remove(int index) {
    rangeCheck(index);
    checkForComodification();
    // 使用底层支持列表的 remove 方法
    E result = l.remove(index+offset);
    this.modCount = l.modCount;
    size--;
    return result;
}

protected void removeRange(int fromIndex, int toIndex) {
    checkForComodification();
    // 使用底层支持列表的 removeRange 方法
    l.removeRange(fromIndex+offset, toIndex+offset);
    this.modCount = l.modCount;
    size -= (toIndex-fromIndex);
}
```

#### 5.1.3.7 subList
```java
public List<E> subList(int fromIndex, int toIndex) {
    return new SubList<>(this, fromIndex, toIndex);
}
```

## 5.2 RandomAccessSubList
```java
// 实现 RandomAccess 标记接口
class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(AbstractList<E> list, int fromIndex, int toIndex) {
        super(list, fromIndex, toIndex);
    }

    // 让 subList 返回 RandomAccessSubList
    public List<E> subList(int fromIndex, int toIndex) {
        return new RandomAccessSubList<>(this, fromIndex, toIndex);
    }
}
```
参见 [RandomAccess.md][random-access]。


[list]: List.md
[abstract-collection]: AbstractCollection.md
[random-access]: RandomAccess.md