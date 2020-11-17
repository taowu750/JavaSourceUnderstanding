`java.util.PrimitiveIterator`接口的声明如下：
```java
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T>
```
这是一个针对基本类型的`Iterator`扩展接口，泛化参数`T`表示基本类型的包装器类型，`T_CONS`表示基本类型的`Consumer`。
它有三个静态内部子接口`OfInt`、`OfLong`和`OfDouble`。

## 方法

### forEachRemaining()  
    `void forEachRemaining(T_CONS action)`方法在剩余的元素上应用`T_CONS`