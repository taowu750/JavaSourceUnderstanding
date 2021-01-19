`java.util.Deque`接口的声明如下：
```java
public interface Deque<E> extends Queue<E>
```
支持在两端插入和删除元素的线性集合。名称`deque`是“double end queue”（双端队列）的缩写，
通常发音为“deck”。大多数`Deque`实现对它们可能包含的元素数量没有固定的限制，
但是此接口支持容量受限的双端队列以及没有固定大小限制的双端队列。

该接口定义了访问双端队列两端的元素的方法。提供了用于插入，删除和检查元素的方法。
这些方法中的每一种都以两种形式存在：一种在操作失败时引发异常，另一种返回一个特殊值（根据操作而为`null`或`false`）。
插入操作的后一种形式是专门为容量受限的`Deque`实现而设计的。在大多数实现中，插入操作不会失败。

下表总结了上述十二种方法：

<table>
    <tr>
        <th></th>
        <th colspan="2">第一个元素（头）</th>
        <th colspan="2">第二个元素（尾）</th>
    </tr>
    <tr>
        <td></td>
        <td><i>抛出异常</i></td>
        <td><i>返回特殊值</i></td>
        <td><i>抛出异常</i></td>
        <td><i>返回特殊值</i></td>
    </tr>
    <tr>
        <td>插入</td>
        <td><code>addFirst(e)</code></td>
        <td><code>offerFirst(e)</code></td>
        <td><code>addLast(e)</code></td>
        <td><code>offerLast(e)</code></td>
    </tr>
    <tr>
        <td>删除</td>
        <td><code>removeFirst()</code></td>
        <td><code>pollFirst()</code></td>
        <td><code>removeLast()</code></td>
        <td><code>pollLast()</code></td>
    </tr>
    <tr>
        <td>检查</td>
        <td><code>getFirst()</code></td>
        <td><code>peekFirst()</code></td>
        <td><code>getLast()</code></td>
        <td><code>peekLast()</code></td>
    </tr>
</table>

该接口扩展了`Queue`接口。当双端队列用作队列时，表现为`FIFO`（先进先出）。
元素在双端队列的末尾添加，并从开头删除。从`Queue`接口继承的方法与`Deque`方法完全等效，如下表所示：

| `Queue`方法 | 等价的`Deque`方法 |
| ---------- | --------------- |
| `add(e)` | `addLast(e)` |
| `offer(e)` | `offerLast(e)` |
| `remove()` | `removeFirst()` |
| `poll()` | `pollFirst()` |
| `element()` | `getFirst()` |
| `peek()` | `peekFirst()` |

双端队列也可以用作`LIFO`（后进先出）堆栈。此接口应优先于旧版`Stack`类使用。当双端队列用作堆栈时，
元素从双端队列的开始处被压入并弹出。堆栈方法完全等同于`Deque`方法，如下表所示：

| `Stack`方法 | 等价的`Deque`方法 |
| ---------- | ---------------- |
| `push(e)` | `addFirst(e)` |
| `pop()` | `removeFirst()` |
| `peek()` | `peekFirst()` |

注意，当双端队列用作队列或堆栈时，`peek`作用相同。无论哪种情况，元素都是从双端队列的头部获取的。

该接口提供了两种删除内部元素的方法：`removeFirstOccurrence`和`removeLastOccurrence`。
与`List`接口不同，此接口不支持对元素的索引访问。

尽管严格不要求`Deque`实现禁止插入`null`元素，但强烈建议这样做。
并且建议任何确实允许`null`元素的`Deque`实现的用户不要使用插入`null`的功能。之所以如此，
是因为各种方法将`null`用作特殊的返回值，以指示双端队列为空。

`Deque`实现通常不定义`equals`和`hashCode`方法，而是从`Object`类继承基于标识的版本，
因为对于具有相同元素但顺序属性不同的`Deque`，基于元素的相等性并不总是能够很好地定义。

# 1. 方法

## 1.1 size
```java
// 返回此双端队列的元素数量。
public int size();
```

## 1.2 contains
```java
/*
如果此双端队列包含指定的元素，则返回 true。更正式地讲，
当且仅当此双端队列包含至少一个元素（e == null？e == null：o.equals(e)）时，才返回 true。
*/
boolean contains(Object o);
```

## 1.3 iterator
```java
/*
以适当的顺序返回此双端队列中的元素的迭代器。元素将按照从头（头）到后（尾）的顺序返回。
*/
Iterator<E> iterator();

/*
以相反的顺序返回此双端队列中的元素的迭代器。元素将按从最后（尾）到第一个（头）的顺序返回。
*/
Iterator<E> descendingIterator();
```

## 1.4 插入开头
```java
/*
如果可以在不违反容量限制的情况下立即执行此操作，则将指定的元素插入此双端队列的前面，
如果当前没有可用空间，则抛出 IllegalStateException。

当使用容量受限的双端队列时，通常最好使用方法 offerFirst。
*/
void addFirst(E e);

/*
将指定的元素插入此双端队列的前面，除非会违反容量限制。
当使用容量受限的双端队列时，此方法通常比 addFirst 方法更可取，后者无法插入元素时会引发异常。
*/
boolean offerFirst(E e);

/*
在不违反容量限制的情况下立即将元素压入此双端队列表示的堆栈（换句话说，此双端队列的头部），
则在当前没有可用空间的情况下抛出 IllegalStateException。

此方法等效于 addFirst。
*/
void push(E e);
```

## 1.5 插入末尾
```java
/*
如果可以立即执行此操作，而不会违反容量限制，则在此双端队列的末尾插入指定的元素，
如果当前没有可用空间，则抛出 IllegalStateException。

使用容量受限的双端队列时，通常最好使用方法 offerLast。

此方法等效于 add。
*/
void addLast(E e);

/*
在此双端队列的末尾插入指定的元素，除非会违反容量限制。当使用容量受限的双端队列时，
此方法通常比 addLast 方法更可取，后者无法插入元素时会引发异常。
*/
boolean offerLast(E e);

/*
如果可以在不违反容量限制的情况下立即执行操作，则将指定的元素插入此双端队列表示的队列中
（换句话说，在此双端队列的末尾），如果成功，则返回 true，如果当前没有可用空间，
则抛出 IllegalStateException。

使用容量受限的双端队列时，通常最好使用 offer。

此方法等效于 addLast。
*/
boolean add(E e);

/*
如果可以在不违反容量限制的情况下立即执行操作，则将指定的元素插入此双端队列表示的队列中
（换句话说，在此双端队列的末尾），如果成功，则返回 true，如果当前没有可用空间，则返回 false。

当使用容量受限的双端队列时，此方法通常优于 add 方法，后者无法插入元素时会引发异常。

此方法等效于 offerLast。
*/
boolean offer(E e);
```

## 1.6 删除开头
```java
// 返回并删除此双端队列的第一个元素。此方法与 pollFirst 的不同之处仅在于，
// 如果此双端队列为空，则它将引发异常。
E removeFirst();

// 返回并删除此双端队列的第一个元素，如果此双端队列为空，则返回 null。
E pollFirst();

// 从此双端队列表示的堆栈中弹出一个元素。换句话说，删除并返回此双端队列的第一个元素。
// 此方法等效于 removeFirst()。
E pop();

/*
返回并删除此双端队列代表的队列的头部（换句话说，此双端队列的第一个元素）。
此方法与 poll 的不同之处仅在于，如果此双端队列为空，则它将引发异常。

此方法等效于 removeFirst()。
*/
E remove();

/*
检索并删除此双端队列表示的队列的头部（换句话说，此双端队列的第一个元素），
如果此双端队列为空，则返回 null。

此方法等效于 pollFirst()。
*/
E poll();
```

## 1.7 删除末尾
```java
/*
返回并删除此双端队列的最后一个元素。此方法与 pollLast 的不同之处仅在于，
如果此双端队列为空，则此方法将引发异常。
*/
E removeLast();

// 返回并删除此双端队列的最后一个元素，如果此双端队列为空，则返回 null。
E pollLast();
```

## 1.8 删除给定值
```java
/*
从此双端队列删除第一次出现的指定元素。如果双端队列不包含元素，则它保持不变。
更正式地讲，删除第一个元素 e，使得（o == null？e == null：o.equals(e)）（如果存在这样的元素）。

如果此双端队列包含指定的元素（或者等效地，如果此双端队列由于调用而发生更改），则返回 true。
*/
boolean removeFirstOccurrence(Object o);

/*
从此双端队列移除最后一次出现的指定元素。如果双端队列不包含元素，则它保持不变。
更正式地讲，删除最后一个元素 e，以使（o == null？e == null：o.equals(e)）（如果存在这样的元素）。

如果此双端队列包含指定的元素（或者等效地，如果此双端队列由于调用而发生更改），则返回 true。
*/
boolean removeLastOccurrence(Object o);
```

## 1.9 检查开头
```java
/*
返回但不删除此双端队列的第一个元素。此方法与 peekFirst 的不同之处仅在于，
如果此双端队列为空，则它将引发异常。
*/
E getFirst();

// 返回但不删除此双端队列的第一个元素，如果此双端队列为空，则返回 null。
E peekFirst();

/*
返回但不删除此双端队列代表的队列的头（换句话说，此双端队列的第一个元素）。
此方法与 peek 的不同之处仅在于，如果此双端队列为空，则它将引发异常。

此方法等效于 getFirst()。
*/
E element();

/*
返回但不删除此双端队列表示的队列的头（换句话说，此双端队列的第一个元素），
如果此双端队列为空，则返回 null。

此方法等效于 peekFirst()。
*/
E peek();
```

## 1.10 检查末尾
```java
/*
返回但不删除此双端队列的最后一个元素。此方法与 peekLast 的不同之处仅在于，
如果此双端队列为空，则它将引发异常。
*/
E getLast();

// 返回但不删除此双端队列的最后一个元素，如果此双端队列为空，则返回 null。
E peekLast();
```