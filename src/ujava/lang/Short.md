`java.lang.Short`类的声明如下：
```java
public final class Short extends Number implements Comparable<Short>
```
`Short`是基本类型`short`的包装器类，此类提供了几种将`short`转换为`String`和将`String`转换为`short`的方法，
以及在处理`short`时有用的其他常量和方法。

`Short`类的大部分方法原理和`Integer`一样，方法比`Integer`少很多。实际上，它的很多操作都使用`Integer`方法实现。
看完了`Integer`类，你就会发现这是一个乏善可陈的类。

# 1. 成员字段

## 1.1 范围
```java
public static final short   MIN_VALUE = -32768;

public static final short   MAX_VALUE = 32767
```

## 1.2 TYPE
```java
public static final Class<Short>    TYPE = (Class<Short>) Class.getPrimitiveClass("short");
```

# 2. 方法

## 2.1 toString
```java
// 将 s 解析为 10 进制字符串
public static String toString(short s) {
    // 使用 Integer.toString 方法
    return Integer.toString((int)s, 10);
}

// 返回 Short 的 10 进制字符串表示
public String toString() {
    return Integer.toString((int)value);
}
```
和`Integer`不同的是，`Short`没有转成任意进制字符串的方法，也不区分无符号有符号。

## 2.2 parseShort
```java
// 将 radix 进制字符串解析为 short 值
public static short parseShort(String s, int radix) throws NumberFormatException {
    // 使用了 Integer.parseInt 方法进行解析
    int i = Integer.parseInt(s, radix);
    // 判断是否在 short 范围内
    if (i < MIN_VALUE || i > MAX_VALUE)
        throw new NumberFormatException(
            "Value out of range. Value:\"" + s + "\" Radix:" + radix);
    return (short)i;
}

// 将 10 进制字符串解析为 short 值
public static short parseShort(String s) throws NumberFormatException {
    return parseShort(s, 10);
}
```

## 2.3 valueOf
```java
// 将 radix 进制字符串解析为 Short 对象
public static Short valueOf(String s, int radix) throws NumberFormatException {
    return valueOf(parseShort(s, radix));
}

// 将 10 进制字符串解析为 Short 对象
public static Short valueOf(String s) throws NumberFormatException {
    return valueOf(s, 10);
}
```

## 2.4 decode
```java
// 将 nm 解析为 short 并包装在 Short 对象中。这个整数的进制由其前导符号表示。
// 前导 "0x"、"0X" 和 "#" 表示 16 进制字符串；前导 "0" 表示 8 进制字符串
public static Short decode(String nm) throws NumberFormatException {
    // 使用了 Integer.decode 方法进行解析
    int i = Integer.decode(nm);
    // 判断是否在 short 范围内
    if (i < MIN_VALUE || i > MAX_VALUE)
        throw new NumberFormatException(
                "Value " + i + " out of range from input " + nm);
    return valueOf((short)i);
}
```

## 2.5 toUnsigned
```java
// 将 x 视为无符号数转为 int 值
public static int toUnsignedInt(short x) {
    return ((int) x) & 0xffff;
}

// 将 x 视为无符号数转为 long 值
public static long toUnsignedLong(short x) {
    return ((long) x) & 0xffffL;
}
```

## 2.6 reverseBytes
```java
// 以字节为单位将 s 反转
public static short reverseBytes(short i) {
    return (short) (((i & 0xFF00) >> 8) | (i << 8));
}
```

# 3. 内部类/接口/枚举

## 3.1 ShortCache
```java
private static class ShortCache {
    private ShortCache(){}

    // 缓存 [-128, 128] 范围内的 Short 对象
    static final Short cache[] = new Short[-(-128) + 127 + 1];

    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new Short((short)(i - 128));
    }
}
```