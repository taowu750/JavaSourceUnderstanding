`java.util.AbstractSequentialList`抽象类的声明如下：
```java
public abstract class AbstractSequentialList<E> extends AbstractList<E>
```
此类提供了`List`接口的基本实现，以最大程度地减少实现由“顺序访问”数据存储（例如链表）支持的该接口所需的工作。
对于随机访问数据（例如数组），应优先使用`AbstractList`来代替此类。

该类与`AbstractList`类相反，因为它使用`ListIterator`实现了“随机访问”方法（`get(int index)`，`set(int index, E element）`，
`add(int index, E element)`和`remove(int index)`）；
而`AbstractList`类使用这些方法实现了`ListIterator`。

要实现`AbstractSequentialList`，程序员仅需扩展此类并为`listIterator`和`size`方法提供实现。
对于不可修改的列表，程序员只需要实现列表迭代器的`hasNext`，`next`，`hasPrevious`，`previous`和`index`方法。

对于可修改的列表，程序员应该另外实现列表迭代器的`set`方法。对于可变大小的列表，
程序员应该另外实现列表迭代器的`remove`和`add`方法。

根据`Collection`接口规范中的建议，程序员通常应提供一个无参构造器和具有一个`collection`参数的构造函数。

# 1. 构造器
```java
protected AbstractSequentialList() {
}
```

# 2. 方法

## 2.1 iterator
```java
public Iterator<E> iterator() {
    return listIterator();
}

public abstract ListIterator<E> listIterator(int index);
```

## 2.2 get
```java
/*
返回此列表中指定位置的元素。

此实现首先获取一个指向索引元素的列表迭代器（使用 listIterator(index)）。然后，
它使用 ListIterator.next 方法获取元素并返回它。
*/
public E get(int index) {
    try {
        return listIterator(index).next();
    } catch (NoSuchElementException exc) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }
}
```

## 2.3 add
```java
/*
将指定的元素插入此列表中的指定位置（可选操作）。将当前在该位置的元素（如果有）
和任何后续元素右移（将其索引加一）。

此实现首先获取一个指向索引元素的列表迭代器（使用 listIterator(index)）。
然后，它使用 ListIterator.add 方法插入指定的元素。

请注意，如果列表迭代器未实现添加操作，则此实现将引发 UnsupportedOperationException。
*/
public void add(int index, E element) {
    try {
        listIterator(index).add(element);
    } catch (NoSuchElementException exc) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }
}

/*
将指定集合中的所有元素插入此列表中的指定位置（可选操作）。将当前在该位置的元素（如果有）
和任何后续元素右移（增加其索引）。新元素将按照指定集合的​​迭代器返回的顺序显示在此列表中。
如果在操作进行过程中修改了指定的集合，则此操作的行为是不确定的。

此实现在指定集合上获得一个迭代器，并在此列表上获得一个指向索引元素的列表迭代器
（使用listIterator(index)）。然后，对指定的集合进行迭代，
使用 ListIterator.add 和 ListIterator.next（跳过添加的元素），一次将从迭代器获得的元素插入到此列表中。
*/
public boolean addAll(int index, Collection<? extends E> c) {
    try {
        boolean modified = false;
        ListIterator<E> e1 = listIterator(index);
        Iterator<? extends E> e2 = c.iterator();
        while (e2.hasNext()) {
            e1.add(e2.next());
            modified = true;
        }
        return modified;
    } catch (NoSuchElementException exc) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }
}
```

## 2.4 remove
```java
/*
删除此列表中指定位置的元素（可选操作）。将所有后续元素向左移动（从其索引中减去一个）。返回从列表中删除的元素。

此实现首先获取一个指向索引元素的列表迭代器（使用 listIterator(index)）。然后，
它使用 ListIterator.remove 删除该元素。

请注意，如果列表迭代器未实现 remove 操作，则此实现将引发 UnsupportedOperationException。
*/
public E remove(int index) {
    try {
        ListIterator<E> e = listIterator(index);
        E outCast = e.next();
        e.remove();
        return outCast;
    } catch (NoSuchElementException exc) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }
}
```

## 2.5 set
```java
/*
用指定的元素替换此列表中指定位置的元素（可选操作）。

此实现首先获取一个指向索引元素的列表迭代器（使用 listIterator(index)）。
然后，它使用 ListIterator.next 获取当前元素，并使用 ListIterator.set 进行替换。

请注意，如果列表迭代器未实现 set 操作，则此实现将引发 UnsupportedOperationException。
*/
public E set(int index, E element) {
    try {
        ListIterator<E> e = listIterator(index);
        E oldVal = e.next();
        e.set(element);
        return oldVal;
    } catch (NoSuchElementException exc) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }
}
```