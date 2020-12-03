`java.lang.Float`类的声明如下：
```java
public final class Float extends Number implements Comparable<Float>
```
`Float`类是基本类型`float`的包装器类。此类提供了几种将`float`转换为`String`和将`String`转换为`float`的方法，
以及其他在处理`float`时有用的常量和方法。

`Java`浮点数遵循`IEEE-754`标准。`float`是单精度浮点数，[浮点数.md][floating]中讲述了浮点数和`IEEE-754`标准的相关知识。

# 1. 成员字段

## 1.1 范围

```java
// 最大正值，等于 Float.intBitsToFloat(0x7f7fffff)
public static final float MAX_VALUE = 0x1.fffffeP+127f; // 3.4028235e+38f

// 最小规格化正数，等于 Float.intBitsToFloat(0x00800000)
public static final float MIN_NORMAL = 0x1.0p-126f; // 1.17549435E-38f

// 最小正值，等于 Float.intBitsToFloat(0x1)
public static final float MIN_VALUE = 0x0.000002P-126f; // 1.4e-45f

// 非无穷数最大阶数（二进制），等于 Math.getExponent(Float.MAX_VALUE)
public static final int MAX_EXPONENT = 127;

// 规范化数最小阶数（二进制），等于 Math.getExponent(Float.MIN_NORMAL)
public static final int MIN_EXPONENT = -126;
```
有关规格化、阶数的相关知识在[浮点数.md][floating]中均可找到。可以看到，`float`能够表示很大的范围，但需要注意的是，
浮点数绝对值越大，精度也就越小。

在代码中，还可以看到一个有趣的写法：0x1.fffffe**P**+1023, 0x1.0**p**-126f。`P`、`p`表示 2 的幂，就如同科学计数法
中的`e`表示 10 的幂一样。不过这种写法只能表示 16 进制浮点数，也就是`0x`开头的数。 

## 1.2 特殊值
```java
// 正无穷大，等于 Float.intBitsToFloat(0x7f800000)
public static final float POSITIVE_INFINITY = 1.0f / 0.0f;

// 负无穷大，等于 Float.intBitsToFloat(0xff800000)
public static final float NEGATIVE_INFINITY = -1.0f / 0.0f;

// 非数值，等于 Float.intBitsToFloat(0x7fc00000)
public static final double NaN = 0.0d / 0.0;
```
负无穷大也可以写成`1.0f / -0.0f`，`IEEE-754`浮点数的 0 是区分正负的，虽然它们相等，但是它们的二进制表示不同。
[浮点数.md][floating]第 5.2 节对`NaN`的性质进行了详细说明。

[FloatTest.java][test]对`Float`特殊值和`Double`特殊值进行了比较。

## 1.3 TYPE
```java
public static final Class<Float> TYPE = (Class<Float>) Class.getPrimitiveClass("float");
```
`Class.getPrimitiveClass()`方法是一个`native`方法，专门用来获取基本类型的`Class`对象。
需要注意的是，`float.class`等于`Float.TYPE`，但是`float.class`不等于`Float.class`。

# 2. 方法

## 2.1 位转换
```java
/*
根据 IEEE 754 单精度浮点数位布局，返回指定浮点值的表示形式。

位 31（由掩码 0x80000000 选择的位）表示浮点数的符号。
位 30-23（由掩码 0x7f800000 选择的位）表示指数。
位 22-0（由掩码 0x007fffff 选择的位）表示 0x007fffff 的有效位（有时称为尾数）。

如果参数为正无穷大，则结果为 0x7f800000。
如果参数为负无穷大，则结果为 0xff800000。
如果参数为 NaN，则结果为 0x7fc00000。

在所有情况下，结果都是一个整数，将其提供给 intBitsToFloat(int) 方法时，将产生一个与 floatToIntBits 参数相同的浮点值
（除了所有NaN值均折叠为单个“规范” NaN 值，即 0x7fc00000） 
*/
public static int floatToIntBits(float value) {
    // // 使用 floatToRawIntBits 获取原始表示，此表示返回的 NaN 值没有折叠为单个“规范” NaN 值
    int result = floatToRawIntBits(value);
    // 如果 result 在 NaN 的范围内（指数部分全为 1 且尾数部分非 0），将其折叠为规范 NaN 值
    if ( ((result & FloatConsts.EXP_BIT_MASK) ==
          FloatConsts.EXP_BIT_MASK) &&
         (result & FloatConsts.SIGNIF_BIT_MASK) != 0)
        result = 0x7fc00000;
    return result;
}

// 和 floatToIntBits 方法类似，不同的是它不会将编码 NaN 的所有位模式折叠为单个“规范” NaN 值。
public static native int floatToRawIntBits(float value);

/*
返回与给定位表示形式对应的 float 值。

如果参数为 0x7f800000 ，则结果为正无穷大。
如果参数为 0xff800000 ，则结果为负无穷大。
如果参数是 0x7f800001 到 0x7fffffff 范围内的任何值，或者 0xff800001 到 0xffffffff 范围内的值，则结果为 NaN。
Java提供的 IEEE 754 浮点运算无法区分具有不同位模式的相同类型的两个 NaN 值。 NaN 的不同值只能通过使用
Float.floatToRawIntBits方法来区分。

在所有其他情况下，令s ， e和m为可以从参数计算的三个值：
 
 int s = ((bits >> 31) == 0) ? 1 : -1;
 int e = ((bits >> 23) & 0xff);
 int m = (e == 0) ?
                 (bits & 0x7fffff) << 1 :
                 (bits & 0x7fffff) | 0x800000;
 
然后，浮点结果等于数学表达式 s·m·2^(e-150) 的值。

请注意，此方法可能无法返回与 int 参数完全相同的位模式的 float NaN。IEEE 754区分两种 NaN，
即 quiet NaN（也就是 Java 中的规范 NaN） 和 signaling NaN。两种 NaN 之间的差异通常在 Java 中不可见。
对 signaling NaN 进行算术运算会将其转换为具有不同但通常相似的位模式的 quiet NaN。但是，在某些处理器上，
仅复制 signaling NaN 也会执行该转换。特别地，复制 signaling NaN 以将其返回到调用方法可以执行该转换。
因此 intBitsToFloat 可能无法返回 signaling NaN 位模式的 float。因此，对于某些 int 值，
intBitsToFloat(floatToIntBits(start)) 可能不等于 start。此外，哪些特定的位模式表示 signaling NaN 是平台相关的； 
尽管所有的 NaN 位模式（quiet 或 signaling）都必须在上面确定的 NaN 范围内。
*/
public static native float intBitsToFloat(int bits);
```

## 2.2 equals
```java
public boolean equals(Object obj) {
    return (obj instanceof Float)
           && (floatToIntBits(((Float)obj).value) == floatToIntBits(value));
}
```
`equals`方法只和其他`Float`做比较。使用`floatToIntBits`对`float`值的每一位做精确比较。

## 2.3 hashCode
```java
public static int hashCode(float value) {
    return floatToIntBits(value);
}

@Override
public int hashCode() {
    return Float.hashCode(value);
}
```
`Float`的`hashCode`就是它的位表示。

## 2.4 compare
```java
public static int compare(float f1, float f2) {
    if (f1 < f2)
        return -1;
    if (f1 > f2)
        return 1;

    // 如果 d1，d2 中含有 NaN，则上面两个比较均为 false
    // 由于可能有非规范化 NaN，所以不能使用 doubleToRawLongBits
    int thisBits    = Float.floatToIntBits(f1);
    int anotherBits = Float.floatToIntBits(f2);

    return (thisBits == anotherBits ?  0 : // 按位比较保证以下值是相等的：
            (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
             1));                          // (0.0, -0.0) or (NaN, !NaN)
}

public int compareTo(Float anotherFloat) {
    return Float.compare(value, anotherFloat.value);
}
```
`compare`方法和`Java`比较符号`<`、`==`、`>`的效果大致相同，只是它保证了特殊值(`NaN`、`0`)的比较结果符合常理。
但是由于它是精确比较每一位的值，因此对于`Float.compare(1.4f, 1.1f + 0.3f)`的情况将不会返回 0。

所以比较两个浮点数是否相等，更好的做法是指定一个误差范围，两个浮点数的差值在范围之内，则认为是相等的。
使用`Math.abs()`计算差值，然后和阈值比较。`float`的阈值可以设为`1e-6`。

或者可以使用`BigDecimal`。`BigDecimal`是不可变的，能够精确地表示十进制数字。需要注意的是，创建`BigDecimal`对象时，
要使用参数为`String`的构造方法，不要使用构造参数为`float`的，如果非要使用`float`创建，
一定要用`BigDecimal.valueOf`静态方法，防止丢失精度。然后调用`compareTo`方法比较即可。

关于浮点数比较可以参见这个[链接][compare]，浮点数的精度测试可以参见[FloatingNumberTest][floating-test]。

此方法的测试代码参见[FloatTest.java][test]。

## 2.5 特殊值判断
```java
// 判断 v 是不是 NaN
public static boolean isNaN(float v) {
    // NaN 是唯一不等于自身的数
    return (v != v);
}

// 判断 v 是不是无穷大
public static boolean isInfinite(float v) {
    return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
}

// 判断 v 是不是有限数
public static boolean isFinite(float f) {
    return Math.abs(f) <= FloatConsts.MAX_VALUE;
}
```

## 2.6 toString
```java
/**
 * 返回参数 f 的十进制字符串表示形式。 下面提到的所有字符都是 ASCII 字符。
 *  - 如果参数为 NaN，则结果为字符串“ NaN ”。
 *  - 否则，结果是一个字符串，代表参数的符号和大小（绝对值）。如果符号为负，则结果的第一个字符为 '-'; 
 *  如果符号为正，则结果中不显示符号字符。 
 *  - 对于大小 m ：
 *     - 如果 m 为无穷大，则用字符 "Infinity"; 正无穷大产生结果 "Infinity" 而负无穷大产生结果 "-Infinity"。
 *     - 如果 m 为零，则用字符 "0.0"；负零产生结果 "-0.0" 而正零产生结果 "0.0" 。
 *     - 如果 m 大于等于 10^-3 并且小于 10^7，则将其表示为：整数部分(十进制形式，不带前导零)+'.'+小数部分。
 *     - 如果 m 小于 10^-3 或大于或等于 10^7，则以科学计数法表示：有效数字(x.xxx)+E+指数。
 * 
 * 此方法返回的字符串至少有一位小数。设 x 是参数 f 生成的十进制表示，那么 f 是最接近 x 的 float 值。
 */
public static String toString(float f) {
    return FloatingDecimal.toJavaFormatString(f);
}

// 返回 Float 对象持有 float 值的 10 进制字符串表示
@Override
public String toString() {
    return Float.toString(value);
}
```
参数`f`是最接近`toString`返回值的`float`值，这其中的舍入规则参见[浮点数.md][floating]第 7 节。

## 2.7 toHexString
```java
/**
 * 返回参数 f 的 16 进制字符串表示形式。下面提到的所有字符都是 ASCII 字符。
 *  - 如果参数为 NaN，则结果为字符串 “NaN”。
 *  - 否则，结果是一个字符串，代表参数的符号和大小（绝对值）。如果符号为负，则结果的第一个字符为 '-'; 
 *  如果符号为正，则结果中不显示符号字符。 
 *  - 对于大小 m ：
 *     - 如果 m 为无穷大，则用字符 "Infinity"; 正无穷大产生结果 "Infinity"而负无穷大产生结果 "-Infinity" 。
 *     - 如果 m 为零，则有字符串 "0x0.0p0" ；负零产生结果 "-0x0.0p0" ，正零产生结果 "0x0.0p0" 。
 *     - 如果 m 是规范化数，则使用 16 进制的科学计数法表示结果。
 *     也就是 "0x1." + 尾数部分(16 进制) + "p" + 指数部分(10 进制)。其中 "p" 表示 2 的幂。尾数部分至少会有一个 0。
 *     - 如果 m 是非规范化数，则为 "0x0." + 尾数部分(16 进制) + "p" + 指数部分(10 进制)。
 */
public static String toHexString(float f) {
    // 如果 f 是非规范化数
    if (Math.abs(f) < FloatConsts.MIN_NORMAL
        &&  f != 0.0f ) {
        // Math.scalb 返回 d×2^scaleFactor 的舍入值
        // 调整指数然后调用 Double.toHexString
        String s = Double.toHexString(Math.scalb((double)f,
                                                 /* -1022+126 */
                                                 DoubleConsts.MIN_EXPONENT-
                                                 FloatConsts.MIN_EXPONENT));
        // 将非规范化数的双精度指数替换为单精度指数
        return s.replaceFirst("p-1022$", "p-126");
    }
    else
        // double 字符串和 float 字符串相同
        return Double.toHexString(f);
}
```

## 2.8 parseFloat
```java
// 将字符串解析为 float 值
public static float parseFloat(String s) throws NumberFormatException {
    return FloatingDecimal.parseFloat(s);
}
```
`parseFloat`可以解析十进制`float`值、科学计数法表示的`float`值和 16 进制`float`值。

## 2.9 valueOf
```java
// 解析字符串并返回 Float 对象
public static Float valueOf(String s) throws NumberFormatException {
    return new Float(parseFloat(s));
}

// 将基本类型 float 包装为 Float 对象。注意 Float 没有常量池
public static Float valueOf(float f) {
    return new Float(f);
}
```


[floating]: 浮点数.md
[double]: Double.md
[compare]: https://www.jianshu.com/p/4679618fd28c
[test]: ../../../test/java_/lang/FloatTest.java
[floating-test]: ../../../test/java_/lang/FloatingNumberTest.java