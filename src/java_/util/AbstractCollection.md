`java.util.AbstractCollection`抽象类的声明如下：
```java
public abstract class AbstractCollection<E> implements Collection<E>
```
此类提供了`Collection`接口的基本实现，以最大程度地减少实现此接口所需的工作。
要实现不可修改的集合，程序员只需扩展此类并为`iterator`和`size`方法提供实现。
（由`iterator`方法返回的迭代器必须实现`hasNext`和`next`。）
要实现可修改的集合，程序员必须另外重写此类的`add`方法（否则将抛出`UnsupportedOperationException`），
并且`iterator`方法返回的迭代器必须另外实现其`remove`方法。

根据`Collection`接口规范中的建议，程序员通常应提供一个无参构造器和接受一个`Collection`参数的构造器。

此类中每个非抽象方法的文档都详细描述了其实现，它们都依赖于`iterator`方法。如果有更高效的实现，则可以覆盖这些方法。

`AbstractCollection`代码中比较值得注意的有：
 - 3.4 toArray: 在并发下依旧可以工作。

方法的约定等信息参见 [Collection.md][collection]。

# 1. 成员字段
```java
// 要分配的最大数组大小（注意不是限制）。一些虚拟机在数组中保留一些头字。尝试分配更大的数组可能会导致 OutOfMemoryError
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

# 2. 构造器
```java
protected AbstractCollection() {
}
```

# 3. 方法

## 3.1 抽象方法
```java
public abstract Iterator<E> iterator();

public abstract int size();
```

## 3.2 toString
```java
/*
返回此集合的字符串表示形式。字符串表示形式包含一个集合元素的列表，这些元素按其迭代器返回的顺序排列，
并括在方括号“[]”中。相邻元素由字符“,”（逗号和空格）分隔。元素通过 String.valueOf(Object) 转化为字符串。
*/
public String toString() {
    Iterator<E> it = iterator();
    if (! it.hasNext())
        return "[]";

    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (;;) {
        E e = it.next();
        // 防止包含自身导致无限递归
        sb.append(e == this ? "(this Collection)" : e);
        if (! it.hasNext())
            return sb.append(']').toString();
        sb.append(',').append(' ');
    }
}
```

## 3.3 isEmpty
```java
public boolean isEmpty() {
    return size() == 0;
}
```

## 3.4 toArray
```java
/*
即使此集合的大小在迭代过程中发生更改，返回的数组的长度也等于迭代器返回的元素数
（如果该集合允许在迭代过程中进行并发修改，则可能会发生这种情况）。

size 方法的返回值作为估计值； 即使迭代器返回不同数量的元素，也将返回正确的结果。
*/
public Object[] toArray() {
    // 估计数组的大小，可能会有比 size() 返回值更多或更少的元素数量
    Object[] r = new Object[size()];
    Iterator<E> it = iterator();
    for (int i = 0; i < r.length; i++) {
        if (! it.hasNext()) // 比预期的元素数量要少
            return Arrays.copyOf(r, i);
        r[i] = it.next();
    }
    return it.hasNext() ? finishToArray(r, it) : r;
}

/*
即使此集合的大小在迭代过程中发生更改，返回的数组的长度也等于迭代器返回的元素数
（如果该集合允许在迭代过程中进行并发修改，则可能会发生这种情况）。

size 方法的返回值作为估计值； 即使迭代器返回不同数量的元素，也将返回正确的结果。
*/
@SuppressWarnings("unchecked")
public <T> T[] toArray(T[] a) {
    // 估计数组的大小，可能会有比 size() 返回值更多或更少的元素数量
    int size = size();
    // Class.getComponentType() 获取数组元素的类型
    T[] r = a.length >= size ? a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
    Iterator<E> it = iterator();

    for (int i = 0; i < r.length; i++) {
        if (! it.hasNext()) { // 比预期的元素数量要少
            if (a == r) {
                r[i] = null; // 将多余的空间置为 null
            } else if (a.length < i) {
                return Arrays.copyOf(r, i);
            } else {
                System.arraycopy(r, 0, a, 0, i);
                if (a.length > i) {
                    a[i] = null;
                }
            }
            return a;
        }
        r[i] = (T)it.next();
    }
    // 比预期的元素数量要多时
    return it.hasNext() ? finishToArray(r, it) : r;
}

// 当迭代器返回的元素比预期的多时，重新分配 toArray 中使用的数组，并使用剩余的元素填充它。
private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
    int i = r.length;
    while (it.hasNext()) {
        int cap = r.length;
        if (i == cap) {
            // 新的大小 ≈ 旧的大小 * 3 / 2
            // overflow-conscious code
            int newCap = cap + (cap >> 1) + 1;
            if (newCap - MAX_ARRAY_SIZE > 0)
                newCap = hugeCapacity(cap + 1);
            r = Arrays.copyOf(r, newCap);
        }
        r[i++] = (T)it.next();
    }
    // 如果分配的大小超出需要，对其进行裁剪
    return (i == r.length) ? r : Arrays.copyOf(r, i);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // 数字溢出
        throw new OutOfMemoryError("Required array size too large");
    return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
}
```
有关`overflow-conscious code`的说明参见 [overflow-conscious code.md][overflow]。

## 3.5 contains
```java
public boolean contains(Object o) {
    Iterator<E> it = iterator();
    if (o==null) {
        while (it.hasNext())
            if (it.next()==null)
                return true;
    } else {
        while (it.hasNext())
            if (o.equals(it.next()))
                return true;
    }
    return false;
}

public boolean containsAll(Collection<?> c) {
    for (Object e : c)
        if (!contains(e))
            return false;
    return true;
}
```

## 3.6 add
```java
// 此默认实现不支持 add(E) 操作
public boolean add(E e) {
    throw new UnsupportedOperationException();
}

// 请注意，除非重写 add(E)，否则此实现将引发 UnsupportedOperationException。
public boolean addAll(Collection<? extends E> c) {
    boolean modified = false;
    for (E e : c)
        if (add(e))
            modified = true;
    return modified;
}
```

## 3.7 remove
```java
// 此实现要求 Iterator 的 remove 方法有效
public boolean remove(Object o) {
    Iterator<E> it = iterator();
    if (o==null) {
        while (it.hasNext()) {
            if (it.next()==null) {
                it.remove();
                return true;
            }
        }
    } else {
        while (it.hasNext()) {
            if (o.equals(it.next())) {
                it.remove();
                return true;
            }
        }
    }
    return false;
}

// 此实现要求 Iterator 的 remove 方法有效
public boolean removeAll(Collection<?> c) {
    Objects.requireNonNull(c);
    boolean modified = false;
    Iterator<?> it = iterator();
    while (it.hasNext()) {
        if (c.contains(it.next())) {
            it.remove();
            modified = true;
        }
    }
    return modified;
}
```

## 3.8 retainAll
```java
// 此实现要求 Iterator 的 remove 方法有效
public boolean retainAll(Collection<?> c) {
    Objects.requireNonNull(c);
    boolean modified = false;
    Iterator<E> it = iterator();
    while (it.hasNext()) {
        if (!c.contains(it.next())) {
            it.remove();
            modified = true;
        }
    }
    return modified;
}
```

## 3.9 clear
```java
// 此实现要求 Iterator 的 remove 方法有效
public void clear() {
    Iterator<E> it = iterator();
    while (it.hasNext()) {
        it.next();
        it.remove();
    }
}
```


[collection]: Collection.md
[overflow]: overflow-conscious%20code.md