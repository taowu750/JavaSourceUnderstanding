`java.lang.Byte`类，下面是它的声明：
```java
public final class Byte extends Number implements Comparable<Byte>
```
`Byte`类是基本类型`byte`的包装器类型。

使用`Byte`类时可能会有[装箱拆箱操作][box]。

# 内部类/接口

## ByteCache 静态类
```java
private static class ByteCache {
    private ByteCache(){}

    static final Byte cache[] = new Byte[-(-128) + 127 + 1];

    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new Byte((byte)(i - 128));
    }
}
```
见名知意，这个类缓存了静态类缓存了从`byte`最小值`-128`到最大值`127`的所有`byte`值。


[box]: 自动装箱与拆箱.md