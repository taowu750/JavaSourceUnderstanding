```java
public interface RandomAccess {
}
```
`java.util.RandomAccess`接口是`List`实现使用的标记接口，指示它们支持快速（通常为常数时间）随机访问。

该接口的主要目的是允许通用算法更改其行为，以便在应用于随机访问或顺序访问列表时提供良好的性能。
应用于随机访问列表（例如`ArrayList`）的最佳算法在应用于顺序访问列表（例如`LinkedList`）时会是平方时间复杂度。
我们鼓励使用通用列表算法时，检查给定列表是否为该接口的实例，然后再应用另一种适合随机访问列表的算法。

随机访问和顺序访问之间的区别通常是模糊的。例如，某些`List`实现在一般情况下提供常数访问时间，
而在变得很大时又会提供近似线性的访问时间。这样的`List`实现通常应该实现此接口。根据经验，对于某种实现，如果下面的循环
```java
for (int i=0, n=list.size(); i < n; i++)
    list.get(i);
```
比使用迭代器循环运行地更快
```java
for (Iterator i=list.iterator(); i.hasNext(); )
    i.next();
```
则`List`实现应实现此接口。