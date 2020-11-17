`java.util.PrimitiveIterator`接口的声明如下：
```java
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T>
```
这是一个针对基本类型的`Iterator`扩展接口，`Java8`加入。泛化参数`T`表示基本类型的包装器类型，`T_CONS`表示基本类型的`Consumer`。
它有三个静态内部子接口`OfInt`、`OfLong`和`OfDouble`。

## 1. 方法

### 1.1 forEachRemaining() 
```java
void forEachRemaining(T_CONS action)
```
方法在剩余的元素上应用`T_CONS`

## 2. 内部类/接口

### 2.1 OfInt
```java
public static interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {

    int nextInt();

    default void forEachRemaining(IntConsumer action) {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(nextInt());
    }

    @Override
    default Integer next() {
        if (Tripwire.ENABLED)
            Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.nextInt()");
        return nextInt();
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> action) {
        // 由于 Java8 的 lambda 特性，编译器能正确处理包装器类和其对应基本类型的参数。
        // TODO: 经过测试下面的方法将始终为 false，深入理解还需要了解 java.util.Spliterators
        if (action instanceof IntConsumer) {
            forEachRemaining((IntConsumer) action);
        }
        else {
            // The method reference action::accept is never null
            Objects.requireNonNull(action);
            // 不使用 IntConsumer 是不推荐的方式，因此在这里用 java.util.Tripwire 类发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.forEachRemainingInt(action::accept)");
            forEachRemaining((IntConsumer) action::accept);
        }
    }
}
```
`OfLong`、`OfDouble`与`OfInt`类似。这本身是一个简单的接口，但有一个技巧：使用`Tripwire`类对不推荐方式给予警告。
这在写类库的时候是个好习惯。