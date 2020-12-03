`java.lang.Double`类的声明如下：
```java
public final class Double extends Number implements Comparable<Double>
```
`Double`类是基本类型`double`的包装器类。此类提供了几种将`double`转换为`String`和将`String`转换为`double`的方法，
以及其他在处理`double`时有用的常量和方法。

`Java`浮点数遵循`IEEE-754`标准。`double`是双精度浮点数，[浮点数.md][floating]中讲述了浮点数和`IEEE-754`标准的相关知识。

# 1. 成员字段

## 1.1 范围
```java
// 最大正值，等于 Double.longBitsToDouble(0x7fefffffffffffffL)
public static final double MAX_VALUE = 0x1.fffffffffffffP+1023; // 1.7976931348623157e+308

// 最小规格化正数，等于 Double.longBitsToDouble(0x0010000000000000L)
public static final double MIN_NORMAL = 0x1.0p-1022; // 2.2250738585072014E-308

// 最小正值，等于 Double.longBitsToDouble(0x1L)
public static final double MIN_VALUE = 0x0.0000000000001P-1022; // 4.9e-324

// 最大阶数（二进制）
public static final int MAX_EXPONENT = 1023;

/// 最小阶数（二进制）
public static final int MIN_EXPONENT = -1022;
```
有关规格化、阶数的相关知识在[浮点数.md][floating]中均可找到。可以看到，`double`能够表示非常大的范围，但需要注意的是，
浮点数绝对值越大，精度也就越小。

在代码中，还可以看到一个有趣的写法：0x1.fffffffffffff**P**+1023, 0x1.0**p**-1022。`P`、`p`表示 2 的幂，就如同科学计数法
中的`e`表示 10 的幂一样。不过这种写法只能出现在 16 进制数里面，也就是`0x`开头的数。 

## 1.2 特殊值
```java
// 正无穷大，等于 Double.longBitsToDouble(0x7ff0000000000000L)
public static final double POSITIVE_INFINITY = 1.0 / 0.0;

// 负无穷大，等于 Double.longBitsToDouble(0xfff0000000000000L)
public static final double NEGATIVE_INFINITY = -1.0 / 0.0;

// 非数值，等于 Double.longBitsToDouble(0x7ff8000000000000L)
public static final double NaN = 0.0d / 0.0;
```
负无穷大也可以写成`1.0 / -0.0`，`IEEE-754`浮点数的 0 是区分正负的，虽然它们相等，但是它们的二进制表示不同。
[浮点数.md][floating]第 5.2 节对`NaN`的性质进行了详细说明。[DoubleTest.java][test]对`NaN`和无穷大进行了测试和验证。

## 1.3 TYPE
```java
public static final Class<Double>   TYPE = (Class<Double>) Class.getPrimitiveClass("double");
```

# 2. 方法

## 2.1 位转换
```java
/*
根据 IEEE 754 双精度浮点数位布局，返回指定浮点值的表示形式。
位 63（由掩码 0x8000000000000000L 选择的位）表示浮点数的符号。
位 62-52（由掩码 0x7ff0000000000000L 选择的位）表示指数。
位 51-0（由掩码 0x000fffffffffffffL 选择的位）表示 0x000fffffffffffffL 的有效位（有时称为尾数）。
如果参数为正无穷大，则结果为 0x7ff0000000000000L。
如果参数为负无穷大，则结果为 0xfff0000000000000L。
如果参数为NaN，则结果为 0x7ff8000000000000L。
在所有情况下，结果都是一个 long 整数，将其提供给 longBitsToDouble(long) 方法时，将产生一个与 doubleToLongBits 参数相同的浮点值
（除了所有NaN值均折叠为单个“规范” NaN 值，即 0x7ff8000000000000L）。
 */
public static long doubleToLongBits(double value) {
    // 使用 doubleToRawLongBits 获取原始为表示，此表示返回的 NaN 值没有折叠为单个“规范” NaN 值
    long result = doubleToRawLongBits(value);
    // 如果 result 在 NaN 的范围内（指数部分全为 1 且尾数部分非 0），将其折叠为规范 NaN 值
    if ( ((result & DoubleConsts.EXP_BIT_MASK) ==
          DoubleConsts.EXP_BIT_MASK) &&
         (result & DoubleConsts.SIGNIF_BIT_MASK) != 0L)
        result = 0x7ff8000000000000L;
    return result;
}

// 和 doubleToLongBits 方法类似，不同的是它不会将编码 NaN 的所有位模式折叠为单个“规范” NaN 值。
public static native long doubleToRawLongBits(double value);

/*
返回与给定位表示形式对应的 double 值。

如果参数为 0x7ff0000000000000L，则结果为正无穷大。
如果参数为 0xfff0000000000000L，则结果为负无穷大。
如果参数是 0x7ff0000000000001L 到 0x7fffffffffffffffL 或 0xfff0000000000001L 到 0xffffffffffffffffL 范围内的任何值，
则结果为 NaN。Java 提供的 IEEE 754 浮点运算无法区分具有不同位模式的相同类型的两个 NaN 值。
NaN 的不同值只能通过使用 Double.doubleToRawLongBits 方法来区分。

在所有其他情况下，令s，e和m为可以从参数计算的三个值：
 
 int s = ((bits >> 63) == 0) ? 1 : -1;
 int e = (int)((bits >> 52) & 0x7ffL);
 long m = (e == 0) ?
                 (bits & 0xfffffffffffffL) << 1 :
                 (bits & 0xfffffffffffffL) | 0x10000000000000L;
 
然后，浮点结果等于数学表达式 s·m·2^(e-1075) 的值。

请注意，此方法可能无法以与 long 参数完全相同的位模式返回 double NaN。
IEEE 754区分两种 NaN，即 quiet NaN（也就是 Java 中的规范 NaN） 和 signaling NaN。两种 NaN 之间的差异通常在 Java 中不可见。
对 signaling NaN 进行算术运算会将其转换为具有不同但通常相似的位模式的 quiet NaN。但是，在某些处理器上，
仅复制 signaling NaN 也会执行该转换。特别地，复制 signaling NaN 以将其返回到调用方法可以执行该转换。
因此 longBitsToDouble 可能无法返回 signaling NaN 位模式的 double。因此，对于某些 long 值，
doubleToRawLongBits(longBitsToDouble(start)) 可能不等于 start。此外，哪些特定的位模式表示 signaling NaN 是平台相关的； 
尽管所有的 NaN 位模式（quiet 或 signaling）都必须在上面确定的 NaN 范围内。
 */
public static native double longBitsToDouble(long bits);
```

## 2.2 equals
```java
public boolean equals(Object obj) {
    return (obj instanceof Double)
           && (doubleToLongBits(((Double)obj).value) ==
                  doubleToLongBits(value));
}
```
`equals`方法只和其他`Double`做比较。使用`doubleToLongBits`对`double`值的每一位做精确比较。

## 2.3 hashCode
```java
public static int hashCode(double value) {
    long bits = doubleToLongBits(value);
    return (int)(bits ^ (bits >>> 32));
}

@Override
public int hashCode() {
    return Double.hashCode(value);
}
```
和`Long`的`hashCode`非常相似，都是将高 32 位和低 32 位异或。

## 2.4 compare
```java
public static int compare(double d1, double d2) {
    if (d1 < d2)
        return -1;
    if (d1 > d2)
        return 1;

    // 如果 d1，d2 中含有 NaN，则上面两个比较均为 false
    // 由于可能有非规范化 NaN，所以不能使用 doubleToRawLongBits
    long thisBits    = Double.doubleToLongBits(d1);
    long anotherBits = Double.doubleToLongBits(d2);

    return (thisBits == anotherBits ?  0 : // 按位比较保证一下值是相等的：
            (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
             1));                          // (0.0, -0.0) or (NaN, !NaN)
}

@Override
public int compareTo(Double anotherDouble) {
    return Double.compare(value, anotherDouble.value);
}
```
`compare`方法和`Java`比较符号`<`、`==`、`>`的效果大致相同，只是它保证了特殊值(`NaN`、`0`)的比较结果符合常理。
但是由于它是精确比较每一位的值，因此对于`Double.compare(0.3, 0.1 + 0.2)`的情况将不会返回 0。

所以比较两个浮点数是否相等，更好的做法是指定一个误差范围，两个浮点数的差值在范围之内，则认为是相等的。
使用`Math.abs()`计算差值，然后和阈值比较。`double`的阈值可以设为`1e-15`。

或者可以使用`BigDecimal`。`BigDecimal`是不可变的，能够精确地表示十进制数字。需要注意的是，创建`BigDecimal`对象时，
要使用参数为`String`的构造方法，不要使用构造参数为`double`的，如果非要使用`double`创建，
一定要用`BigDecimal.valueOf`静态方法，防止丢失精度。然后调用`compareTo`方法比较即可。

关于浮点数比较可以参见这个[链接][compare]。

此方法的测试代码参见[DoubleTest.java][test]。

## 2.5 特殊值判断
```java
// 判断 v 是不是 NaN
public static boolean isNaN(double v) {
    // NaN 是唯一不等于自身的数
    return (v != v);
}

// 判断 v 是不是无穷大
public static boolean isInfinite(double v) {
    return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
}

// 判断 v 是不是有限数
public static boolean isFinite(double d) {
    return Math.abs(d) <= DoubleConsts.MAX_VALUE;
}
```

## 2.6 toString
```java
/**
 * 返回参数 d 的十进制字符串表示形式。 下面提到的所有字符都是ASCII字符。
 *  - 如果参数为 NaN，则结果为字符串“ NaN ”。
 *  - 否则，结果是一个字符串，代表参数的符号和大小（绝对值）。如果符号为负，则结果的第一个字符为 '-'; 
 *  如果符号为正，则结果中不显示符号字符。 
 *  - 对于大小 m ：
 *     - 如果 m 为无穷大，则用字符 "Infinity"; 正无穷大产生结果"Infinity"而负无穷大产生结果"-Infinity" 。
 *     - 如果 m 为零，则用字符 "0.0"；负零产生结果 "-0.0" 而正零产生结果 "0.0" 。
 *     - 如果 m 大于等于 10^-3 并且小于 10^7，则将其表示为：整数部分(十进制形式，不带前导零)+'.'+小数部分。
 *     - 如果 m 小于 10^-3 或大于或等于 10^7，则以科学计数法表示：有效数字(x.xxx)+E+指数。
 * 
 * 此方法返回的字符串至少有一位小数。设 x 是参数 d生成的十进制表示，那么 d 是最接近 x 的 double 值。
 */
public static String toString(double d) {
    return FloatingDecimal.toJavaFormatString(d);
}

// 返回 Double 对象持有 double 值的 10 进制字符串表示
@Override
public String toString() {
    return toString(value);
}
```
`toString`的参数`d`是最接近它的字符串表示的`double`值，这其中的舍入规则参见[浮点数.md][floating]第 7 节。
此方法的测试代码参见[DoubleTest.java][test]。

## 2.7 toHexString
```java
/**
 * 返回参数 d 的 16 进制字符串表示形式。下面提到的所有字符都是ASCII字符。
 *  - 如果参数为 NaN，则结果为字符串“NaN”。
 *  - 否则，结果是一个字符串，代表参数的符号和大小（绝对值）。如果符号为负，则结果的第一个字符为 '-'; 
 *  如果符号为正，则结果中不显示符号字符。 
 *  - 对于大小 m ：
 *     - 如果 m 为无穷大，则用字符 "Infinity"; 正无穷大产生结果"Infinity"而负无穷大产生结果"-Infinity" 。
 *     - 如果 m 为零，则有字符串 "0x0.0p0" ；负零产生结果 "-0x0.0p0" ，正零产生结果 "0x0.0p0" 。
 *     - 如果 m 是规范化数，则使用 16 进制的科学计数法表示结果。
 *     也就是 "0x1." + 尾数部分(16 进制) + "p" + 指数部分(10 进制)。其中 "p" 表示 2 的幂。尾数部分至少会有一个 0。
 *     - 如果 m 是非规范化数，则为 "0x0." + 尾数部分(16 进制) + "p" + 指数部分(10 进制)。
 */
public static String toHexString(double d) {
    /*
     * Modeled after the "a" conversion specifier in C99, section
     * 7.19.6.1; however, the output of this method is more
     * tightly specified.
     */
    if (!isFinite(d) )
        // 对 NaN、无穷大调用 Double.toString
        return Double.toString(d);
    else {
        // 字符串最大长度为 24。首先负号"-"长度 1；"0x1."长度 4；尾数为 52 位换成 16 进制长度 13；
        // 指数位最长"p-1023"长度 6。加起来总共 24 位。
        StringBuilder answer = new StringBuilder(24);

        // Math.copySign 返回带有第二个参数符号的第一个参数
        if (Math.copySign(1.0, d) == -1.0)
            answer.append("-");
        answer.append("0x");
        // 统一为正值。和 int 不一样，double 的正负值个数是相等的，绝对值相同的两个正负值除了符号位，其他位都相同
        d = Math.abs(d);

        if(d == 0.0) {
            // d 等于 0 的话直接添加 0。这里使用 "==" 表明严格等于 0 的情形
            answer.append("0.0p0");
        } else {
            // 判断 d 是不是非规范数
            boolean subnormal = (d < DoubleConsts.MIN_NORMAL);
            // 将尾数有效位分离出来
            long signifBits = (Double.doubleToLongBits(d)
                               & DoubleConsts.SIGNIF_BIT_MASK) |
                0x1000000000000000L;

            // 非规范化数以 0 开头，规范化数以 1 开头
            answer.append(subnormal ? "0." : "1.");

            // 后 13 个 16 进制数表示的是尾数位
            String signif = Long.toHexString(signifBits).substring(3,16);
            // 如果尾数都是 0，那只添加一个 0；否则去掉前面的所有 0
            answer.append(signif.equals("0000000000000") ? // 13 zeros
                          "0":
                          signif.replaceFirst("0{1,12}$", ""));

            // 添加指数符号 p
            answer.append('p');
            // 如果是非规格化数，那么指数部分就是 DoubleConsts.MIN_EXPONENT: -1022。
            // 规格化数则使用 Math.getExponent 获取指数值。注意，这个指数是二进制指数
            answer.append(subnormal ?
                          DoubleConsts.MIN_EXPONENT:
                          Math.getExponent(d));
        }
        return answer.toString();
    }
}
```
此方法的测试代码参见[DoubleTest.java][test]。

## 2.8 parseDouble
```java
// 将字符串解析为 double 值
public static double parseDouble(String s) throws NumberFormatException {
    return FloatingDecimal.parseDouble(s);
}
```
`parseDouble`可以解析十进制`double`值、科学计数法表示的`double`值和 16 进制`double`值。
此方法的测试代码参见[DoubleTest.java][test]。

## 2.9 valueOf
```java
// 解析字符串并返回 Double 对象
public static Double valueOf(String s) throws NumberFormatException {
    return new Double(parseDouble(s));
}

// 将基本类型 double 包装为 Double 对象。注意 Double 没有常量池
public static Double valueOf(double d) {
    return new Double(d);
}
```


[floating]: 浮点数.md
[test]: ../../../test/java_/lang/DoubleTest.java
[compare]: https://www.jianshu.com/p/4679618fd28c