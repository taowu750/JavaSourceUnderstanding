`java.lang.Comparable`接口的声明如下：
```java
public interface Comparable<T>
```
实现该接口的类的是可排序的。该排序称为类的**自然排序**，而该类的`compareTo`方法被称为其自然比较方法。

可以通过`Collections.sort`（和`Arrays.sort`）自动对实现此接口的对象的列表（和数组）进行排序。
实现此接口的对象可以用作`SortedMap`中的键或`SortedSet`中的元素，而无需指定比较器`Comparator`。

当且仅当`e1.compareTo(e2) == 0`对于此类的每个`e1`和`e2`具有与`e1.equals(e2)`相同的布尔值时，
才认为此类的自然排序与`equals`一致。请注意， `null`不是任何类的实例，即使`e.equals(null)`返回`false`，
`e.compareTo(null)`仍应抛出`NullPointerException`。

强烈建议（尽管不是必需的）自然排序应与`equals`保持一致。之所以如此，是因为没有显式比较器的`SortedSet`和`SortedMap`
在与自然排序与`equals`不一致的元素一起使用时，表现的会很奇怪。例如，
如果两个键`a`和`b`的关系是`(!a.equals(b) && a.compareTo(b) == 0)`，将它们添加到`SortedSet`或`SortedMap`中时，
第二个`add`操作将返回`false`（而且集合大小不会增加），因为从排序集的角度来看，`a`和`b`是等效的，尽管`equals`返回`false`。

几乎所有实现`Comparable`的`Java`核心类都具有与`equals`一致的自然顺序。一个例外是`java.math.BigDecimal`，
其自然顺序使`BigDecimal`对象具有相等的值和不同的精度（例如`4.0`和`4.00`）。

# 1. 方法
```java
// 将此对象与指定对象进行比较。当此对象小于，等于或大于指定的对象时，返回负整数，零或正整数。
public int compareTo(T o);
```