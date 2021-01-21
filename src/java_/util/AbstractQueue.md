`java.util.AbstractQueue`抽象类的声明如下：
```java
public abstract class AbstractQueue<E>
    extends AbstractCollection<E>
    implements Queue<E>
```
此类提供一些`Queue`操作的基本实现。当基本实现不允许空元素时，此类中的实现是合适的。
方法`add`，`remove`和`element`分别基于`offer`，`poll`和`peek`，
通过抛出异常而不是通过返回`false`或`null`表示失败。

扩展此类的`Queue`实现必须最少定义一个不允许插入空元素的`Queue.offer`方法，
以及`Queue.peek`，`Queue.poll`，`Collection.size`和`Collection.iterator`方法。
通常，其他方法也会被覆盖。如果打算不遵守这些约定，请考虑继承`AbstractCollection`。

# 1. 方法

## 1.1 add
```java
public boolean add(E e) {
    if (offer(e))
        return true;
    else
        throw new IllegalStateException("Queue full");
}

public boolean addAll(Collection<? extends E> c) {
    if (c == null)
        throw new NullPointerException();
    if (c == this)
        throw new IllegalArgumentException();
    boolean modified = false;
    for (E e : c)
        if (add(e))
            modified = true;
    return modified;
}
```

## 1.2 remove
```java
public E remove() {
    E x = poll();
    if (x != null)
        return x;
    else
        throw new NoSuchElementException();
}
```

## 1.3 element
```java
public E element() {
    E x = peek();
    if (x != null)
        return x;
    else
        throw new NoSuchElementException();
}
```

## 1.4 clear
```java
public void clear() {
    while (poll() != null)
        ;
}
```