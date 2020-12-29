`java.util.List`接口的声明如下：
```java
public interface List<E> extends Collection<E>
```
`List`有序集合（也称为序列）。该接口的用户可以精确控制`List`中每个元素的插入位置。用户可以通过其整数索引（`List`中的位置）访问元素，
并在`List`中搜索元素。

与`set`不同，`List`通常允许重复的元素。更正式地讲，`List`通常允许存在元素`e1`和`e2`，使得`e1.equals(e2)`，
并且如果它们允许`null`值，则通常可以有多个`null`。

除了在`Collection`接口中指定的规则外，`List`接口还在`iterator`，`add`，`remove`，`equals`和`hashCode`方法的上增加了规则。
为方便起见，此处还包含其他继承方法的声明。

`List`接口提供了四种位置（索引）方式访问列表元素的方法。`List`（像`Java`数组）一样从零开始。请注意，
对于某些实现（例如，`LinkedList`类），这些操作可能在时间上与索引值成正比。因此，如果调用者不知道实现，
则遍历列表中的元素通常比对其进行索引更可取。

`List`接口提供了一个称为`ListIterator`的特殊迭代器（参见 [ListIterator.md][list-iterator]），
除了`Iterator`接口提供的常规操作之外，该迭代器还允许插入元素、替换元素以及双向访问。提供了一种获取`ListIterator`的方法，
使得`ListIterator`可以从`List`中的指定位置开始。

`List`接口提供了两种搜索指定对象的方法。从性能的角度来看，应谨慎使用这些方法。在许多实现中，它们将执行昂贵的线性搜索。
`List`接口提供了两种方法，可以有效地在列表中的任意点插入和删除多个元素。

注意：尽管允许列表可以包含自身，但应该格外小心： `equals`和`hashCode`方法在这样的列表上将可能无法正常工作。

一些列表实现对它们可能包含的元素有限制。例如，某些实现禁止使用`null`元素，而某些实现对其元素类型进行限制。
尝试添加不合格元素会引发异常，通常是`NullPointerException`或`ClassCastException`。
尝试查询不合格元素的也可能会引发异常，或者仅返回`false`。一些实现将表现出前一种行为，而某些将表现出后者。
更一般地，尝试对不合格元素进行操作，该操作结束后不会将不合格元素插入列表中，这可能会导致异常或静默返回，具体取决于实现方式。
此类异常在此接口的规范中标记为“可选”。

# 1. 方法

## 1.1 equals
```java
/*
比较指定对象与此列表是否相等。当且仅当指定对象也是一个列表，两个列表具有相同的大小，并且两个列表中所有对应的元素对都相等时，
返回 true。

换句话说，如果两个列表包含相同顺序的相同元素，则它们被定义为相等。此定义确保 equals 方法可在 List 接口的不同实现中正常工作。
*/
boolean equals(Object o);
```

## 1.2 hashCode
```java
/*
返回此列表的哈希码值。 列表的哈希码定义为以下计算的结果：
 
     int hashCode = 1;
     for (E e : list)
         hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
 
这可以确保给定任意两个 list1 和 list2，当 list1.equals(list2) 时，同样有 list1.hashCode() == list2.hashCode()，
这遵循了 Object.hashCode 的规范。
*/
int hashCode();
```
`hashCode`的规范参见 [Object.md][object]；`hashCode`的实现原理参见 [哈希算法.md][hash]。

## 1.3 iterator
```java
// 以正确的顺序返回此列表中元素的迭代器。
Iterator<E> iterator();
```
参见 [Iterator.md][iterator]。

## 1.4 listIterator
```java
// 返回此列表中的元素的列表迭代器（按适当顺序）
ListIterator<E> listIterator();

// 从列表中的指定位置开始，以适当的顺序返回此列表中的元素的列表迭代器。指定的索引指示首次调用 next 将返回的第一个元素。
// 首次调用 previous 将返回指定索引减一的元素。
ListIterator<E> listIterator(int index);
```
参见 [ListIterator.md][list-iterator]。

## 1.5 spliterator
```java
/*
在此列表中的元素上创建 Spliterator。

此 Spliterator 声明 Spliterator.SIZED 和 Spliterator.ORDERED 特征。
实现应在文档中记录其他类型的特征值，如果有的话。

此方法于 Java8 加入
*/
@Override
default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.ORDERED);
}
```
参见 [Spliterator.md][spliterator]。

## 1.6 元素数量查询
```java
// 返回此列表中的元素数。如果此列表包含多于 Integer.MAX_VALUE 个元素，则返回 Integer.MAX_VALUE。
int size();

// 如果此列表不包含任何元素，则返回 true。
boolean isEmpty();
```

## 1.7 toArray
```java
/*
以正确的顺序（从第一个元素到最后一个元素）返回包含此列表中所有元素的数组。

返回的数组将是“安全的”，因为此列表不保留对其的引用。换句话说，即使此列表由数组支持，此方法也必须分配一个新数组。
因此，调用者可以自由修改返回的数组。

此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
*/
Object[] toArray();

/*
返回一个数组，该数组按正确的顺序包含此列表中的所有元素（从第一个元素到最后一个元素）； 
返回数组的类型是参数数组的类型。

如果指定的数组大小大于等于列表中元素数量，则将其返回。否则，将使用指定数组的类型和此列表的大小分配一个新数组。
如果指定的数组有剩余空间（即，数组比该列表具有更多的元素），则紧接列表结束后的数组中的元素设置为 null。
（仅当调用者知道此列表不包含任何 null 元素时，此方法才可用于确定此集合的长度。）

与 toArray() 方法一样，此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。此外，
此方法允许对输出数组的类型进行精确控制，并且在某些情况下可以用来节省分配成本。

假设 x 是一个已知仅包含字符串的列表。以下代码可用于将列表转储到新分配的 String 数组中：
 
     String[] y = x.toArray(new String[0]);
 
注意， toArray(new Object[0]) 在功能上与 toArray() 相同。
*/
<T> T[] toArray(T[] a);
```

## 1.8 contains
```java
// 如果此列表包含指定的元素，则返回 true。更正式地说，当且仅当这个列表至少包含一个元素 e 满足
// (o == null ? e == null : o.equals(e)) 时返回 true。
boolean contains(Object o);

// 如果此列表包含指定集合中的所有元素，则返回 true。
boolean containsAll(Collection<?> c);
```

## 1.9 get
```java
// 返回此列表中指定位置的元素。
E get(int index);
```

## 1.10 add
```java
/*
将指定的元素追加到此列表的末尾（可选操作）。

支持此操作的列表可能会限制可以添加到此列表的元素。特别是，某些列表将拒绝添加 null，而另一些列表将对可能添加的元素类型施加限制。
列表类应在其文档中明确指定对可以添加哪些元素的任何限制。
*/
boolean add(E e);

/*
将指定集合中的所有元素追加到到此列表的末尾（可选操作）。如果在操作进行过程中修改了 c，则此操作的行为是不确定的。
（这意味着如果 c 是此列表本身，并且此列表是非空的，则此调用的行为是不确定的。）
*/
boolean addAll(Collection<? extends E> c);

/*
将指定集合中的所有元素插入此列表中的指定位置（可选操作）。将当前在该位置的元素（如果有）和任何后续元素右移（增加其索引）。 
新元素将按照指定集合的迭代器返回的顺序插入到此列表中。

如果在操作进行过程中修改了 c，则此操作的行为是不确定的。（这意味着如果 c 是此列表本身，并且此列表是非空的，
则此调用的行为是不确定的。）*/
boolean addAll(int index, Collection<? extends E> c);

/*
将指定的元素插入此列表中的指定位置（可选操作）。将当前在该位置的元素（如果有）和任何后续元素右移（将其索引加一）。
*/
void add(int index, E element);
```

## 1.11 remove
```java
/*
从列表中删除第一次出现的指定元素（如果存在）（可选操作）。更正式地讲，删除最低索引 i 的元素，
使得 (o == null ? get(i) == null : o.equals(get(i)))（如果存在这样的元素）。

如果此列表包含指定的元素，则返回 true（或者等效地，如果此列表由于调用而更改），则返回 true。
*/
boolean remove(Object o);

// 从此列表中删除指定集合中包含的所有其元素（可选操作）。
boolean removeAll(Collection<?> c);

// 删除此列表中指定位置的元素（可选操作）。将所有后续元素向左移动（从其索引中减去一个）。返回从列表中删除的元素。
E remove(int index);
```

## 1.12 set
```java
// 用指定的元素替换此列表中指定位置的元素（可选操作）。
E set(int index, E element);
```

## 1.13 index
```java
/*
返回指定元素在此列表中首次出现的索引；如果此列表不包含该元素，则返回 -1。更正式地，返回最低索引 i，
使得 (o == null ? get(i) == null : o.equals(get(i)))；如果没有这样的索引，则返回 -1。
*/
int indexOf(Object o);

/*
返回指定元素在此列表中最后一次出现的索引；如果此列表不包含该元素，则返回 -1。更正式地，返回最高索引 i，
使得 (o == null ? get(i) == null : o.equals(get(i)))；如果没有这样的索引，则返回 -1。
*/
int lastIndexOf(Object o);
```

## 1.14 retainAll
```java
// 仅保留此列表中指定集合中包含的元素（可选操作）。换句话说，从该列表中删除所有未包含在指定集合中的元素（求交集）。
boolean retainAll(Collection<?> c);
```

## 1.15 replaceAll
```java
/*
用 operator 应用于元素的结果替换此列表中的每个元素。抛出的异常将传播给调用者。

此方法于 Java8 加入
*/
default void replaceAll(UnaryOperator<E> operator) {
    Objects.requireNonNull(operator);
    final ListIterator<E> li = this.listIterator();
    while (li.hasNext()) {
        li.set(operator.apply(li.next()));
    }
}
```

## 1.16 sort
```java
/*
根据指定 Comparator 对该列表进行排序。使用指定的比较器，此列表中的所有元素必须可以相互比较
（即，c.compare(e1, e2) 不得为列表中的任何元素 e1 和 e2 抛出 ClassCastException。）

如果指定的比较器为 null 则此列表中的所有元素都必须实现 Comparable 接口，并且使用元素的自然顺序。

该列表必须是可修改的，但可以不是可调整大小的。

默认实现获取一个包含此列表中所有元素的数组，对该数组进行排序，并在此列表上进行迭代，从数组中的相应位置重置每个元素。
（这避免了由于尝试对链表进行排序而导致的 n^2*log(n) 时间复杂度。）

此实现是一种稳定的，自适应的迭代归并排序，当输入数组已部分排序时，所需的比较次数少于 nlg(n)，
而在对输入数组元素排列是随机的，它提供了和传统归并排序相同的性能。
如果输入数组已近似排序，则该实现需要大约 n 个比较。

对于近似排序的输入数组的只需要小常数的存储开销；而对于随机排序的输入数组则需要 n/2 的存储开销。

该实现可以利用在同一输入数组中不同的升序和降序部分。它非常适合合并两个或多个排序后的数组：
简单地将数组连接起来并对结果数组进行排序。

该实现改编自 Tim Peters 针对 Python 的列表排序（TimSort）。它使用了 Peter McIlroy 的“乐观排序和信息理论复杂性”技术，
该技术在 1993 年 1 月举行的第四届 ACM-SIAM 离散算法年会上发表，位于第 467-474 页。

此方法于 Java8 加入
*/
default void sort(Comparator<? super E> c) {
    Object[] a = this.toArray();
    Arrays.sort(a, (Comparator) c);
    ListIterator<E> i = this.listIterator();
    for (Object e : a) {
        i.next();
        i.set((E) e);
    }
}
```

## 1.17 clear
```java
// 从此列表中删除所有元素（可选操作）。该调用返回后，该列表将为空
void clear();
```

## 1.18 subList
```java
/*
返回此列表中从 fromIndex（包括）到 toIndex（不包括）之间的元素视图。（如果 fromIndex 和 toIndex 相等，则返回列表为空。）
由于是视图，因此返回列表中的非结构性更改会反映在此列表中，反之亦然。返回的列表支持此列表支持的所有可选列表操作。

此方法消除了对显式范围操作（数组通常存在的那种范围）的需要。通过传递 subList 视图而不是整个列表，
可以对列表进行范围操作。例如，以下语句从列表中删除了一组元素：
 
      list.subList(from, to).clear();
 
可以为 indexOf 和 lastIndexOf 使用同样的操作，并且 Collection 类中的所有算法都可以应用于 subList。

如果对此列表进行了结构修改（而不是通过返回列表进行结构修改），则此方法返回的列表将处于不确定的状态。
（结构修改是指更改此列表的大小的结构修改，或以其他方式修改此列表的方式，使得正在进行的迭代可能会产生错误的结果。）
*/
List<E> subList(int fromIndex, int toIndex);
```


[list-iterator]: ListIterator.md
[object]: ../lang/Object.md
[hash]: ../lang/哈希算法.md
[iterator]: Iterator.md
[spliterator]: Spliterator.md