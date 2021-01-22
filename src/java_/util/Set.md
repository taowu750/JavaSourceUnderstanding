`java.util.Set`接口声明如下：
```java
public interface Set<E> extends Collection<E>
```
不包含重复元素的集合。顾名思义，此接口对数学集合抽象进行建模。

除了从`Collection`接口继承的规定外，`Set`接口还对所有构造函数以及`add`，
`equals`和`hashCode`等方法规定了其他准则。

对构造函数的附加规定是，所有构造函数都必须创建一个不包含重复元素的`Set`。
注意：如果将可变对象用作`Set`元素，则必须格外小心。如果对象的值更改会影响`equals`方法，
且该对象是`Set`中的元素，则`Set`的​​行为是未定义的。此外，不允许`Set`包含自身。

一些`Set`实现对它们可能包含的元素有限制。例如，某些实现禁止使用`null`元素，
而某些实现对其元素类型进行限制。尝试添加不合格元素会引发异常，
通常为`NullPointerException`或`ClassCastException`。尝试查询不合格元素的存在可能会引发异常，
或者可能仅返回`false`。更一般地，尝试对不合格元素进行操作，
该操作的完成不会导致将不合格元素插入`Set`中，这可能会导致异常或返回特殊值，具体取决于实现方式。
此类异常在此接口的规范中标记为“可选”。

# 1. 方法

## 1.1 equals
```java
/*
比较指定对象与此 Set 是否相等。如果指定对象也是一个 Set，并且两个 Set 具有相同的大小，
并且指定 Set 的​​每个成员都包含在此 Set 中（或者等效地，这个 Set 的每个成员都包含在指定 Set 中），
则返回 true。

此定义确保 equals 方法可在 set 接口的不同实现中正常工作。
*/
boolean equals(Object o);
```

## 1.2 hashCode
```java
/*
返回此 Set 的哈希码值。Set 的哈希码定义为 Set 中元素的哈希码之和，其中空元素的哈希码定义为零。
这可以确保当 s1.equals(s2) 时，有 s1.hashCode() == s2.hashCode()，
符合 Object.hashCode 的一般约定要求。
*/
int hashCode();
```

## 1.3 iterator
```java
// 返回此 Set 中元素的迭代器。元素以不特定的顺序返回（除非此 Set 是提供了顺序保证的某些类的实例）。
Iterator<E> iterator();
```

## 1.4 spliterator
```java
/*
在此 Set 中的元素上创建 Spliterator。

此 Spliterator 具有 Spliterator.DISTINCT 特征值。子类实现如果有其他特征值，
应记录在文档中。
*/
@Override
default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.DISTINCT);
}
```

## 1.5 元素数量查询
```java
// 返回此集合中的元素数。如果此集合包含多于 Integer.MAX_VALUE 个元素，
// 则返回 Integer.MAX_VALUE。
int size();

boolean isEmpty();
```

## 1.6 toArray
```java
/*
返回一个包含此 Set 中所有元素的数组。如果此 Set 规定了迭代器返回其元素的顺序，
则此方法必须以相同的顺序返回元素。

返回的数组将是一个副本，因此，调用者可以自由修改返回的数组。

此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
*/
Object[] toArray();

/*
返回一个包含此 Set 中所有元素的数组；返回数组的运行时类型是指定数组的运行时类型。
如果 Set 元素数量小于等于指定的数组长度，则使用指定数组。否则，
将使用指定数组的类型和此 Set 的大小分配一个新数组。

如果指定数组有剩余空间（即，数组中的元素多于此 Set ），则紧接该 Set 结尾的数组中的元素将设置为 null。
（仅当调用者知道此 Set 不包含任何 null 元素时，此方法才可用于确定此集合的长度。）

如果此 Set 规定了迭代器返回元素的顺序，则此方法必须以相同的顺序返回元素。

与 toArray() 方法一样，此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
此外，此方法允许对f返回数组的类型进行精确控制，并且在某些情况下可以用于节省分配成本。

假设 x 是一个已知仅包含字符串的 Set。以下代码可用于将 Set 转储到新分配的 String 数组中：
    String [] y = x.toArray(new String[0]);

注意，toArray(new Object[0]) 在功能上与 toArray() 相同。
*/
<T> T[] toArray(T[] a);
```

## 1.7 contains
```java
/*
如果此 Set 包含指定的元素，则返回 true。更正式地说，
当且仅当此 Set 包含元素 e 使得 (o == null？e == null：o.equals(e)) 时，返回 true。
*/
boolean contains(Object o);

/*
如果此 Set 包含指定集合的​​所有元素，则返回 true。如果指定的集合也是一个 Set，
且它是该 Set 的子集，则此方法返回 true。
*/
boolean containsAll(Collection<?> c);
```

## 1.8 add
```java
/*
如果指定的元素尚不存在，则将其添加到该 Set 中（可选操作）。更正式地讲，如果 Set 中不包含任何元素 e2，
使得 (e == null？e2 == null：e.equals(e2)), 则将元素 e 添加到该 Set 中。

如果此 Set 已包含该元素，则调用 add 将使该 Set 保持不变并返回 false。结合对构造函数的限制，
这可以确保 Set 永远不会包含重复的元素。

上面的规定并不意味着 Set 必须接受所有元素。 Set 可能拒绝添加任何特定元素（包括 null），
并引发异常，如 Collection.add 的规范中所述。实现应明确记录对它们可能包含的元素的任何限制。
*/
boolean add(E e);

/*
将指定集合中不存在于此 Set 的元素添加到此 Set 中（可选操作）。
如果指定的集合也是一个 Set，则 addAll 操作会高效地修改此 Set，以使其值为两个 Set 的并集。

如果在操作进行过程中修改了指定的集合，则此操作的行为是不确定的。
*/
boolean addAll(Collection<? extends E> c);
```

## 1.9 remove
```java
/*
如果存在指定元素，则从该 Set 中删除该元素（可选操作）。更正式地说，如果此 Set 包含这样的元素 e，
使得 (e == null？e == null：o.equals(e))，则删除 e。

如果此 Set 包含元素 o（或等效，如果此 Set 由于调用而更改），则返回 true。
（一旦调用返回，此 Set 将不包含该元素。）
*/
boolean remove(Object o);

/*
从该 Set 中删除所有包含在指定集合中的元素（可选操作）。如果指定的集合也是一个 Set，
则此操作会高效地修改此 Set，以使其值为两个集合的差。
*/
boolean removeAll(Collection<?> c);
```

## 1.10 retainAll
```java
/*
仅保留此 Set 中包含在指定集合中的元素（可选操作）。换句话说，
从该 Set 中删除所有未包含在指定集合中的元素。
如果指定的集合也是一个 Set，则此操作会高效地修改此 Set，以使其值为两个 Set 的交集。
*/
boolean retainAll(Collection<?> c);
```

## 1.11 clear
```java
// 从该 Set 中删除所有元素（可选操作）。该调用返回后，该 Set 将为空。
void clear();
```