`java.lang.Iterable`接口的声明如下：
```java
public interface Iterable<T>
```
实现此接口的对象可用在“for-each”循环中。

# 1. 方法

## 1.1 iterator
```java
// 返回一个新的的迭代器。
Iterator<T> iterator();
```
参见 [Iterator.md][iterator]。

## 1.2 forEach
```java
// 对 Iterable 每个元素执行给定的操作，直到处理完所有元素或该操作引发异常为止。
// 除非实现类另行指定，否则操作将按照迭代顺序执行。该操作引发的异常将中继到调用方。
default void forEach(Consumer<? super T> action) {
    Objects.requireNonNull(action);
    for (T t : this) {
        action.accept(t);
    }
}
```

## 1.3 spliterator
```java
// 在此 Iterable 上创建一个 Spliterator。
// Spliterator 是可拆分迭代器，也用于遍历数据源中的元素，但它是为了并行执行而设计的。
default Spliterator<T> spliterator() {
    return Spliterators.spliteratorUnknownSize(iterator(), 0);
}
```


[iterator]: ../util/Iterator.md