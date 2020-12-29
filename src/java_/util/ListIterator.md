`java.util.ListIterator`接口的声明如下：
```java
public interface ListIterator<E> extends Iterator<E>
```
列表的迭代器，允许程序员在任一方向上遍历列表，在迭代过程中修改列表，并获取迭代器在列表中的当前位置。
`ListIterator`没有当前元素。它的光标位置始终位于通过调用`previous()`返回的元素和通过调用`next()`返回的元素之间。
长度为`n`的`ListIterator`具有`n+1`可能的光标位置，如下面的插入符号 ^ 所示：
```
                        Element(0)   Element(1)   Element(2)   ... Element(n-1)
   cursor positions:  ^            ^            ^            ^                  ^
```
注意，`remove`和`set(Object)`方法不是根据光标位置定义的。它们被定义为对调用`next()`或`previous()`返回的最后一个元素进行操作。

# 1. 方法

## 1.1 next
```java
// 如果向右遍历列表时此 ListIterator 包含更多元素，则返回 true。换句话说，
// 如果 next 将返回一个元素而不是引发异常，则返回 true。
boolean hasNext();

/*
返回列表中的下一个元素（当前光标的右边元素）并将光标位置向右移动。可以重复调用此方法以遍历列表，也可以将其与 previous 混合调用来回移动。
注意交替调用 next 和 previous 将重复返回相同的元素。
*/
E next();

// 返回对 next 的后续调用的元素索引。如果此 ListIterator 位于列表的末尾，则返回列表大小。
int nextIndex();
```

## 1.2 previous
```java
// 如果向左遍历列表时此 ListIterator 包含更多元素，则返回 true。换句话说，
// 如果 previous 将返回一个元素而不是引发异常，则返回 true。
boolean hasPrevious();

// 返回列表中的前一个元素（当前光标的左边元素），并将光标位置向左移动。可以重复调用此方法以向后遍历列表，也可以将其与 next 混合调用来回移动。
// 注意交替调用 next 和 previous 将重复返回相同的元素。
E previous();

// 返回对 previous 的后续调用的元素索引。如果此 ListIterator 位于列表的开头，则返回 -1。
int previousIndex();
```

## 1.3 add
```java
/*
将指定的元素插入列表（可选操作）。该元素将立即插入 next 将返回的元素左边（如果有的话），
然后插入 previous 将返回的元素右边（如果有的话）之后。如果列表中没有元素，新的元素成为列表中的唯一元素。

新元素被插入到光标之前。后续的 next 调用不会受到影响，而后续的 previous 调用将返回插入的新元素。
此调用将使 nextIndex 或 previousIndex 的调用返回的值加 1。
*/
void add(E e);
```
参见测试 [ListIteratorTest.java][test]。

## 1.4 remove
```java
/*
从列表中删除 next 或 previous 方法返回的元素（可选操作）。每次调用 next 或 previous 方法之后只能进行一次此调用。
仅当在上一次调用 next 或 previous 方法之后没有调用 add 方法才可以进行 remove。
*/
void remove();
```
参见测试 [ListIteratorTest.java][test]。

## 1.5 set
```java
/*
用指定的元素替换 next 或 previous 返回的元素（可选操作）。
仅当在上一次调用 next 或 previous 方法之后没有调用 add 方法才可以进行 set。
*/
void set(E e);
```
参见测试 [ListIteratorTest.java][test]。


[test]: ../../../test/java_/util/ListIteratorTest.java