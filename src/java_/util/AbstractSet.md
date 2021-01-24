`java.util.AbstractSet`抽象类的声明如下：
```java
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E>
```
此类提供`Set`接口的基本实现，以最大程度地减少实现此接口所需的工作。

通过扩展此类来实现`Set`的过程与通过扩展`AbstractCollection`来实现`Collection`的过程相同，
不同之处在于，此类的子类中的所有方法和构造函数都必须服从`Set`接口施加的附加约束
（例如，`add`方法不得允许将一个对象的多个实例添加到集合中）。

请注意，除了`removeAll`方法，此类不会覆盖`AbstractCollection`类的其他方法。
它还添加了`equals`、`hashCode`的实现。

参见 [AbstractCollection.md][abstract-collection]。

# 1. 方法

## 1.1 equals
```java
public boolean equals(Object o) {
    if (o == this)
        return true;

    if (!(o instanceof Set))
        return false;
    Collection<?> c = (Collection<?>) o;
    if (c.size() != size())
        return false;
    try {
        return containsAll(c);
    } catch (ClassCastException unused)   {
        return false;
    } catch (NullPointerException unused) {
        return false;
    }
}
```

## 1.2 hashCode
```java
public int hashCode() {
    int h = 0;
    Iterator<E> i = iterator();
    // 将每个元素的哈希码直接相加，null 元素哈希码为 0
    while (i.hasNext()) {
        E obj = i.next();
        if (obj != null)
            h += obj.hashCode();
    }
    return h;
}
```
可以看到，`AbstractSet`的`hashCode`计算仅仅只是相加元素的`hashCode`。
而`AbstractList`和`Arrays`对`hashCode`的计算还会将每个元素的`hashCode`乘以一个质数`31`。
之所以有这样的差别，有如下原因：

`List`和数组，可能存在元素相同而顺序不相同的多个序列。而`List`和数组的相等性规定这些序列不应该相等，
因此它们的`hashCode`也应该尽量不相等。采用乘以质数的方式可以符合这个要求。而`Set`则不关心元素的顺序，
或者有着固定的顺序（如`TreeSet`），只要元素都相等，则两个`Set`相等。

## 1.3 removeAll
```java
public boolean removeAll(Collection<?> c) {
    Objects.requireNonNull(c);
    boolean modified = false;

    // 如果此 Set 中元素数量大于 c 中元素数量，则使用 remove() 方法进行删除
    if (size() > c.size()) {
        for (Iterator<?> i = c.iterator(); i.hasNext(); )
            modified |= remove(i.next());

    // 否则，使用 c.contains 检查元素，然后使用迭代器的 remove 方法进行删除
    // 这保证检查元素的次数是两者容量的较小值
    } else {
        for (Iterator<?> i = iterator(); i.hasNext(); ) {
            if (c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
    }
    return modified;
}
```


[abstract-collection]: AbstractCollection.md