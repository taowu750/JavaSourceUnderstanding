`java.util.Iterator`接口的声明如下：
```java
public interface Iterator<E>
```
集合上的迭代器。在`Java Collections Framework`中，`Iterator`代替了`Enumeration`。迭代器与枚举有以下两种不同：
 - 迭代器允许调用者在迭代期间使用定义明确的语义从基础集合中删除元素。
 - 方法名称已得到改进。
 
更多信息参见 [Iterable.md][iterable]。

# 1. 方法

## 1.1 next
```java
// 如果迭代具有更多元素，则返回 true。换句话说，如果 next 将返回一个元素而不是引发异常，则返回 true。
boolean hasNext();

// 返回迭代中的下一个元素。如果没有下一个元素，抛出 NoSuchElementException
E next();
```

## 1.2 remove
```java
// 从基础集合中移除此迭代器返回的最后一个元素（可选操作）。每次调用 next 只能调用一次此方法。
// 如果在迭代进行过程中以其他方式（而不是通过调用此方法）修改了基础集合，则迭代器的行为未指定。
default void remove() {
    throw new UnsupportedOperationException("remove");
}
```

## 1.3 forEachRemaining
```java
// 对剩余的每个元素执行给定的操作，直到所有元素都已处理或该操作引发异常。
// 除非实现类另行指定，否则操作将按照迭代顺序执行。该操作引发的异常将传递到到调用方。
default void forEachRemaining(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    while (hasNext())
        action.accept(next());
}
```


[iterable]: ../lang/Iterable.md