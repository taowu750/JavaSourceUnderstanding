`java.lang.Byte`类，下面是它的声明：
```java
public final class Byte extends Number implements Comparable<Byte>
```

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