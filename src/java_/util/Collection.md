`java.util.Collection`接口的声明如下：
```java
public interface Collection<E> extends Iterable<E>
```
`Collection`是集合层次结构中的根接口。集合表示一组对象，称为其元素。一些集合允许重复的元素，而另一些则不允许 一些是有序的，而其他则是无序的。
`JDK`不提供此接口的任何直接实现：它提供更具体的子接口（例如`Set`和`List`）的实现。该接口通常用于传递集合，
并在需要最大通用性的地方操作它们。

`Bags`或`multisets`（可能包含重复元素的无序集合）应直接实现此接口。

所有通用的`Collection`实现类（通常通过其子接口之一间接实现`Collection` ）都应提供两个“标准”构造函数：
一个无参构造函数，用于创建一个空集合，以及具有单个`Collection`类型参数的构造函数，它将创建一个元素与其参数相同新集合。
后一个构造函数允许用户复制任何集合。没有强制执行此约定的方法（因为接口不能包含构造函数），
但是`Java`平台库中的所有通用`Collection`实现都遵从此约定。

如果一些实现不支持某些方法，则指定调用不支持方法将引发`UnsupportedOperationException`。例如，
对一个不可修改的集合调用`addAll(Collection)`方法可能（但并非必须）引发异常。

一些集合实现对它们可能包含的元素有限制。 例如，某些实现禁止使用`null`元素，而某些实现对其元素类型进行限制。
尝试添加不合格元素会引发异常，通常是`NullPointerException`或`ClassCastException`。
尝试查询不合格元素的存在也可能会引发异常，或者可能仅返回`false`。一些实现将表现出前一种行为，而某些将表现出后者。
更一般地，尝试对不合格元素进行操作，该操作的不会导致将不合格元素插入集合中，具体取决于实现方式。
此类异常在此接口的规范中标记为“可选”。

由每个集合决定自己的同步策略。在实现没有更强有力的保证的情况下，未定义的行为可能发生在另一个线程调用正在被修改的集合上的任何方法；
这包括直接调用，将集合传递给可能执行调用的方法，以及使用迭代器遍历集合。

当集合是直接或间接包含其自身的的自引用实例时，某些执行集合的递归遍历的操作可能会抛出异常。
这包括`clone()`， `equals()`，`hashCode()`和`toString()`方法。实现可以有选择地处理自引用场景，但是大多数当前实现不这样做。

# 1. 方法

## 1.1 equals
```java
/*
比较指定对象与此集合的相等性。

尽管 Collection 接口没有为 equals 的默认规定增加其他规则，但是直接实现 Collection 接口的程序员必须小心实现 equals。
不必这样做，最简单的实现过程是依赖于 Object 的实现，但是实现者可能希望实现“值比较”来代替默认的“引用比较” 
（List 和 Set 接口要求进行这种值比较。）

equals 方法地准则规定，equals 必须是对称的。List.equals 和 Set.equals 规定，List 仅等于其他 List，
Set 仅等于其他 Set。因此，一个仅实现了 Collection 接口而没有实现 List 和 Set接口的集合类，
它的自定义 equals 方法与 List 和 Set 比较必须返回 false。
*/
boolean equals(Object o);
```
`equals`方法的实现准则参见 [Object.md][object] 1.1 节 equals。

## 1.2 hashCode
```java
// 返回此集合的哈希码值。尽管 Collection 接口没有为 Object.hashCode 方法的常规准则添加任何规定，
// 但程序员应注意，重写 equals 方法的任何类也必须重写 hashCode 方法，以满足 Object 的规定。
// 特别的，当 c1.equals(c2) 时，必须有 c1.hashCode()==c2.hashCode()。
int hashCode();
```
`hashCode`方法的实现准则参见 [Object.md][object] 1.2 节 hashCode。

## 1.3 iterator
```java
// 返回此集合的迭代器。没有关于元素返回顺序的保证（除非此集合是某个提供保证的类的实例）。
Iterator<E> iterator();
```
参见 [Iterator.md][iterator]。

## 1.4 spliterator
```java
/*
创建此集合的 Spliterator。实现类应写明自己的 Spliterator 的特征值。
如果此 Spliterator 是 Spliterator.SIZED 并且此集合不包含任何元素，则不需要声明。

子类可以覆盖默认实现，返回更高效的 Spliterator。为了确保 stream() 和 parallelStream() 方法的惰性行为，
此 Spliterator 要么具有 IMMUTABLE 或 CONCURRENT 特征，或者具有后期绑定特性。如果这些都不可行，
则覆盖的类应描述 Spliterator 的绑定和结构改变策略，并应覆盖 stream() 和 parallelStream() 方法
以使用返回 Spliterator 的 Supplier 创建流，如：

     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
 
这些要求确保从 stream() 和 parallelStream() 方法产生的 stream() 在终端操作启动时将反映集合的内容。
*/
@Override
default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, 0);
}
```
参见 [Spliterator.md][spliterator]。

## 1.5 元素数量查询
```java
// 返回此集合中的元素数。 如果此集合包含多于 Integer.MAX_VALUE 个元素，则返回 Integer.MAX_VALUE。
int size();

// 如果此集合不包含任何元素，则返回 true。
boolean isEmpty();
```

## 1.6 toArray
```java
/*
返回一个包含此集合中所有元素的数组。如果此集合对由其迭代器返回其元素的顺序做出任何保证，则此方法必须按相同顺序返回元素。

返回的数组将是“安全的”，因为此集合不维护对其的引用。（换句话说，即使此集合由数组支持，此方法也必须分配一个新数组）。
因此，调用者可以自由修改返回的数组。此方法充当基于数组 API 和集合 API 之间的桥梁。
*/
Object[] toArray();

/*
返回一个包含此集合中所有元素的数组； 返回数组的类型是指定数组的类型。如果指定的数组大小大于等于集合中元素数量，则将其返回。
否则，将使用指定数组的类型和此集合的大小分配一个新数组。

如果指定的数组有剩余空间（即，数组比该集合具有更多的元素），则紧接集合结束后的数组中的元素设置为 null。
（仅当调用者知道此集合不包含任何 null 元素时，此方法才可用于确定此集合的长度。）

如果此集合对由其迭代器返回其元素的顺序做出任何保证，则此方法必须按相同顺序返回元素。

与 toArray() 方法一样，此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
此外，此方法允许对输出数组的运行时类型进行精确控制，并且在某些情况下可以用于节省分配成本。
假设 x 是一个已知仅包含字符串的集合。以下代码可用于将集合转储到新分配的 String 数组中：
           String[] y = x.toArray(new String[0]);

注意，toArray(new Object [0]) 在功能上与 toArray() 相同。
*/
<T> T[] toArray(T[] a);
```

## 1.7 contains
```java
// 如果此集合包含指定的元素，则返回 true。更正式地说，当且仅当这个集合至少包含一个元素 e 满足
// (o == null ? e == null : o.equals(e)) 时返回 true。
boolean contains(Object o);

// 如果此集合包含指定集合中的所有元素，则返回 true。
boolean containsAll(Collection<?> c);
```

## 1.8 add
```java
/*
确保此集合包含指定的元素（可选操作）。如果此集合由于 add 调用而更改，则返回 true 。
（如果此集合不允许重复并且已经包含指定的元素，则返回 false。）

支持此操作的集合可能会对可以添加到此集合中的元素施加限制。特别是，某些集合将拒绝添加 null 元素，
而其他集合将对可能添加的元素类型施加限制。集合类应在其文档中明确指定对可以添加哪些元素的任何限制。

如果某个集合由于已包含该元素以外的其他原因拒绝添加该元素，则它必须引发异常（而不是返回 false）。
*/
boolean add(E e);

/*
将指定集合中的所有元素添加到此集合中（可选操作）。如果在操作进行过程中修改了 c，则此操作的行为是不确定的。
（这意味着如果 c 是此集合本身，并且此集合是非空的，则此调用的行为是不确定的。）
*/
boolean addAll(Collection<? extends E> c);
```

## 1.9 remove
```java
/*
如果存在，则从此集合中删除指定元素的单个实例（可选操作）。更正式的，将要删除的元素 e 满足
(o == null ? e == null : o.equals(e))。

如果此集合包含指定的元素（或者等效地，如果此集合由于调用而更改），则返回 true。
*/
boolean remove(Object o);

// 删除此集合中与指定集合中的元素相同的元素（可选操作）。在此调用返回之后，此集合将不包含与指定集合相同的元素。
boolean removeAll(Collection<?> c);

// 删除此集合中满足给定 filter 的所有元素。在迭代过程中或 filter 中引发的异常将传递给调用方。
// 此方法于 Java8 引入
default boolean removeIf(Predicate<? super E> filter) {
    Objects.requireNonNull(filter);
    boolean removed = false;
    final Iterator<E> each = iterator();
    while (each.hasNext()) {
        if (filter.test(each.next())) {
            each.remove();
            removed = true;
        }
    }
    return removed;
}
```

## 1.10 retainAll
```java
// 仅保留此集合中包含在指定集合中的元素（可选操作）。换句话说，从此集合中删除所有未包含在指定集合中的元素（求交集）。
boolean retainAll(Collection<?> c);
```

## 1.11 clear
```java
// 从此集合中删除所有元素（可选操作）。此方法返回后，集合将为空。
void clear();
```

## 1.12 stream
```java
/*
返回以此集合为源的顺序 Stream。

当 spliterator() 方法无法返回 IMMUTABLE，CONCURRENT 或后期绑定的 Spliterator 时，则应重写此方法。
（有关详细信息，请参见spliterator()。）

此方法于 Java8 引入
*/
default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
}

/*
返回一个可能以此集合为源的并行 Stream。此方法允许返回顺序流。

当 spliterator() 方法无法返回 IMMUTABLE，CONCURRENT 或后期绑定的 Spliterator 时，则应重写此方法。
（有关详细信息，请参见spliterator()。）

此方法于 Java8 引入
*/
default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
}
```


[object]: ../lang/Object.md
[iterator]: Iterator.md
[spliterator]: Spliterator.md