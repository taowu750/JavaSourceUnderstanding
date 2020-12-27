`java.lang.Long`类的声明如下：
```java
public final class Long extends Number implements Comparable<Long>
```
`Long`类是基本类型`long`的包装器类。此类提供了几种将`long`转换为`String`和将`String`转换为`long`的方法，
以及其他在处理`long`时有用的常量和方法。

`Long`类的大部分方法原理和`Integer`一样，因此，一些方法的详细解读可在[Integer.md][integer]中找到。

`Long`代码中比较值得注意的有：
1. 2.5 `toUnsignedString` & 2.6 `parseUnsignedLong`：分离最后一位数字和其余数字，来求无符号结果

使用`Long`类时可能会有[装箱拆箱操作][box]。

# 1. 成员字段

## 1.1 范围
```java
// long 最小值，-2^63，-922,3372,0368,5477,5808
@Native public static final long MIN_VALUE = 0x8000000000000000L;

// long 最大值，2^63 - 1，922,3372,0368,5477,5807
@Native public static final long MAX_VALUE = 0x7fffffffffffffffL;
```
`@Native`注解参见[Native.md][native]。可以看到，`long`的范围是非常夸张的，有百亿亿数量级。相比之下，
`int`只有十亿数量级。

## 1.2 TYPE
```java
public static final Class<Long>     TYPE = (Class<Long>) Class.getPrimitiveClass("long");
```
`Class.getPrimitiveClass()`方法是一个`native`方法，专门用来获取基本类型的`Class`对象。
需要注意的是，`long.class`等于`Long.TYPE`，但是`long.class`不等于`Long.class`。

有 9 个预定义的类对象来表示 8 个基本类型和`void`。它们是由`Java`虚拟机创建的，与它们所表示的原始类型具有相同的名称，
即`boolean`、`byte`、`char`、`short`、`int`、`long`、`float`和`double`。这些预定义基本类型的类对象主要是为了实现反射系统的完整性。

# 2. 方法

## 2.1 无符号运算
```java
// 比较无符号数 x, y 的大小
public static int compareUnsigned(long x, long y) {
    return compare(x + MIN_VALUE, y + MIN_VALUE);
}

// 比较 x, y 的大小
public static int compare(long x, long y) {
    // 正数加上 MIN_VALUE 会变成负数，正数越小则结果越小
    // 负数加上 MIN_VALUE 会变成正数，负数越小则结果越大
    // 负数在计算机中采用补码存储，越接近 0 的负数，如果符号位为 0 则它的正数形式越大（对应的无符号形式也就越大）。
    // 加上 MIN_VALUE 会使得负号的符号位溢出变成 0，此时负数越小结果也就越大
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
}

public static long divideUnsigned(long dividend, long divisor) {
    // 如果除数 divisor 小于 0，那么它的无符号形式肯定比任何正数都大；而其他负数都不会有 divisor 的两倍大。
    // 此时，结果只会为 0 或 1
    if (divisor < 0L) { // signed comparison
        // Answer must be 0 or 1 depending on relative magnitude
        // of dividend and divisor.
        return (compareUnsigned(dividend, divisor)) < 0 ? 0L :1L;
    }

    // 两个都不是负数，可以正常除
    if (dividend > 0) //  Both inputs non-negative
        return dividend/divisor;
    else {
        /*
         * For simple code, leveraging BigInteger.  Longer and faster
         * code written directly in terms of operations on longs is
         * possible; see "Hacker's Delight" for divide and remainder
         * algorithms.
         */
        return toUnsignedBigInteger(dividend).
            divide(toUnsignedBigInteger(divisor)).longValue();
    }
}

public static long remainderUnsigned(long dividend, long divisor) {
    // 两个都是正数，正常求余
    if (dividend > 0 && divisor > 0) { // signed comparisons
        return dividend % divisor;
    } else {
        // dividend 无符号形式小于 divisor，直接返回 dividend
        if (compareUnsigned(dividend, divisor) < 0) // Avoid explicit check for 0 divisor
            return dividend;
        else
            // 调用 BigInteger 的方法求余数
            return toUnsignedBigInteger(dividend).
                remainder(toUnsignedBigInteger(divisor)).longValue();
    }
}

// 将 i 视作无符号格式的 long，转化为 BigInteger 
private static BigInteger toUnsignedBigInteger(long i) {
    if (i >= 0L)
        // i 大于等于 0，使用 BigInteger.valueOf 方法
        return BigInteger.valueOf(i);
    else {
        // 高 32 位
        int upper = (int) (i >>> 32);
        // 低 32 位
        int lower = (int) i;

        // return (upper << 32) + lower
        return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
            add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }
}
```
可以看到，无符号`long`的运算操作，多数情况下需要使用`BigInteger`的方法。

<!-- TODO: 分析为何不能只使用 long 进行无符号操作 -->

## 2.2 前导零和后导零
```java
// 返回无符号 i 的二进制补码形式中最高位（最左的 1）之前的零位数目。如果等于零，则返回 64
public static int numberOfLeadingZeros(long i) {
    // i 等于 0，返回 64
    // HD, Figure 5-6
    if (i == 0)
        return 64;
    int n = 1;
    // x 是 i 高 32 位的值
    int x = (int)(i >>> 32);
    // 如果 i 高 32 位等于 0，则至少有 32 个前导 0，那么令 x 为 i 低 32 位的值
    if (x == 0) { n += 32; x = (int)i; }
    // 如果 x 高 16 位等于 0，则至少有 16 个前导 0，那么将后 16 位移到前 16 位去
    if (x >>> 16 == 0) { n += 16; x <<= 16; }
    // 如果 x 高 8 位等于 0，则至少有 8 个前导 0，那么将后 8 位移到前 8 位去
    if (x >>> 24 == 0) { n +=  8; x <<=  8; }
    // 如果 x 高 4 位等于 0，则至少有 4 个前导 0，那么将后 4 位移到前 4 位去
    if (x >>> 28 == 0) { n +=  4; x <<=  4; }
    // 如果 x 高 2 位等于 0，则至少有 2 个前导 0，那么将后 2 位移到前 2 位去
    if (x >>> 30 == 0) { n +=  2; x <<=  2; }
    // 对最后两个数判断
    n -= x >>> 31;
    return n;
}

// 返回无符号整型 i 的二进制补码形式中最低位（最右的 1）之后的零位数目。如果等于零，则返回 64
public static int numberOfTrailingZeros(long i) {
    // HD, Figure 5-14
    int x, y;
    // i 等于 0，返回 64
    if (i == 0) return 64;
    int n = 63;
    // y 是 i 低 32 位的值。如果 y 不等于 0，则后 32 位有 1，那么令 x 为 i 低 32 位的值；
    // 否则令 x 位 i 高 32 位的值
    y = (int)i; if (y != 0) { n = n -32; x = y; } else x = (int)(i>>>32);
    // 左移 16 位，判断后 16 位有没有 1
    y = x <<16; if (y != 0) { n = n -16; x = y; }
    // 左移 8 位，判断后 8 位有没有 1
    y = x << 8; if (y != 0) { n = n - 8; x = y; }
    // 左移 4 位，判断后 4 位有没有 1
    y = x << 4; if (y != 0) { n = n - 4; x = y; }
    // 左移 2 位，判断后 2 位有没有 1
    y = x << 2; if (y != 0) { n = n - 2; x = y; }
    // 判断最后一位是不是 1
    return n - ((x << 1) >>> 31);
}
```
以上方法中用到了**二分法**，原理和`Integer`的对应方法一样，在此不再详述。

## 2.3 hashCode
```java
@Override
public int hashCode() {
    return Long.hashCode(value);
}

public static int hashCode(long value) {
    return (int)(value ^ (value >>> 32));
}
```
`long`的`hashCode`是将高 32 位和低 32 位异或得来的。

## 2.4 有符号 toString
```java
// 将 i 转换成 radix 进制的字符串
public static String toString(long i, int radix) {
    // radix 非法则使用 10 进制
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        radix = 10;
    // 对 10 进制使用更快的方法
    if (radix == 10)
        return toString(i);
    char[] buf = new char[65];
    int charPos = 64;
    boolean negative = (i < 0);

    // 转成负数统一处理。注意负数比正数多 1
    if (!negative) {
        i = -i;
    }

    // 求商取余法
    while (i <= -radix) {
        // 使用 Integer 的查找表 Integer.digits
        buf[charPos--] = Integer.digits[(int)(-(i % radix))];
        i = i / radix;
    }
    buf[charPos] = Integer.digits[(int)(-i)];

    // 如果为负，添加负号
    if (negative) {
        buf[--charPos] = '-';
    }

    // public String(char value[], int offset, int count)
    return new String(buf, charPos, (65 - charPos));
}

// 将 i 转换成 10 进制的字符串
public static String toString(long i) {
    // 对最小值单独处理
    if (i == Long.MIN_VALUE)
        return "-9223372036854775808";
    // 计算 i 的十进制数字个数
    int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
    char[] buf = new char[size];
    getChars(i, size, buf);

    // 下面是 String 的包构造器，直接将数组 buf 作为 String 的底层数组
    // String(char[] value, boolean share) 
    return new String(buf, true);
}

// 将整数 i 转化为 10 进制字符串，并写入 buf 数组中。index 表示字符写入的最后下标，字符从 index(不包括)开始后向写入 buf。
// 这个方法当 i == Long.MIN_VALUE 的时候会出错，这也是 toString(int i) 方法中对 Long.MIN_VALUE 特别处理的原因
static void getChars(long i, int index, char[] buf) {
    long q;
    int r;
    int charPos = index;
    char sign = 0;

    // 统一转为正数
    if (i < 0) {
        sign = '-';
        i = -i;
    }

    // 此方法和 Integer.getChars 类似，只不过对超出 int 范围的数做了特殊处理。
    // 可以看到，第一个 while 比第二个 while 循环多了转型操作
    // Get 2 digits/iteration using longs until quotient fits into an int
    while (i > Integer.MAX_VALUE) {
        // 获取个位数和百位数
        q = i / 100;
        // really: r = i - (q * 100);
        r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
        i = q;
        buf[--charPos] = Integer.DigitOnes[r];
        buf[--charPos] = Integer.DigitTens[r];
    }

    // Get 2 digits/iteration using ints
    int q2;
    int i2 = (int)i;
    while (i2 >= 65536) {
        q2 = i2 / 100;
        // really: r = i2 - (q * 100);
        r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
        i2 = q2;
        buf[--charPos] = Integer.DigitOnes[r];
        buf[--charPos] = Integer.DigitTens[r];
    }

    // 对小数字（小于等于 65536）进行加速操作。使用左移和加法组合代替乘法，右移和乘法组合代替除 10
    // Fall thru to fast mode for smaller numbers
    // assert(i2 <= 65536, i2);
    for (;;) {
        // 下面等同于 q2 = i2 / 10
        q2 = (i2 * 52429) >>> (16+3);
        r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
        buf[--charPos] = Integer.digits[r];
        i2 = q2;
        if (i2 == 0) break;
    }
    // 写入负号
    if (sign != 0) {
        buf[--charPos] = sign;
    }
}

// 计算正整数 x 的十进制长度
static int stringSize(long x) {
    // Integer.stringSize 方法使用了查找表
    long p = 10;
    for (int i=1; i<19; i++) {
        if (x < p)
            return i;
        p = 10*p;
    }
    return 19;
}
```
`Long`的`toString`方法原理和`Integer`一样，在此不再详述。

## 2.5 无符号 toString
```java
// 将 i 转化成无符号格式的字符串，radix 是进制
public static String toUnsignedString(long i, int radix) {
    // i 大于等于 0，使用有符号的转换方法
    if (i >= 0)
        return toString(i, radix);
    else {
        switch (radix) {
            // 2 进制使用 toBinaryString 方法
            case 2:
                return toBinaryString(i);

            // 4 进制使用 toUnsignedString0 方法
            case 4:
                return toUnsignedString0(i, 2);

            // 8 进制使用 toOctalString 方法
            case 8:
                return toOctalString(i);

            // 10 进制转换
            case 10:
                /*
                 * 我们可以通过先右移得到一个正值，然后再除以 5，这等同于 long 值除以 10 的无符号效果。
                 * 这使得最后一个数字和前面的数字能够比最初转换为 BigInteger 更快地分离出来。
                 */
                /*
                 * We can get the effect of an unsigned division by 10
                 * on a long value by first shifting right, yielding a
                 * positive value, and then dividing by 5.  This
                 * allows the last digit and preceding digits to be
                 * isolated more quickly than by an initial conversion
                 * to BigInteger.
                 */
                long quot = (i >>> 1) / 5;
                // 虽然 i 是负数，但我们可以将 i 看成是一个无符号数，这样下面的操作就相当于将最后一位十进制数字分离了出来
                long rem = i - quot * 10;
                // quot 是 i 无符号格式的(n - 1)位数，其中 n 等于 i 无符号格式数字长度
                return toString(quot) + rem;

            // 16 进制使用 toHexString 方法
            case 16:
                return toHexString(i);

            // 32 进制使用 toUnsignedString0 方法
            case 32:
                return toUnsignedString0(i, 5);

            // 其他情况使用 toUnsignedBigInteger 方法
            default:
                return toUnsignedBigInteger(i).toString(radix);
        }
    }
}

// 将 i 转化成无符号格式的 10 进制字符串
public static String toUnsignedString(long i) {
    return toUnsignedString(i, 10);
}

// 将 i 转化为无符号形式的 2 进制字符串
public static String toBinaryString(long i) {
    return toUnsignedString0(i, 1);
}

// 将 i 转化为无符号形式的 8 进制字符串
public static String toOctalString(long i) {
    return toUnsignedString0(i, 3);
}

// 将 i 转化为无符号形式的 16 进制字符串
public static String toHexString(long i) {
    return toUnsignedString0(i, 4);
}

// 将 val 转化为无符号形式的字符串，进制为 2^shift
static String toUnsignedString0(long val, int shift) {
    // assert shift > 0 && shift <=5 : "Illegal shift value";
    // mag 表示无符号整数 val 有效位（除前导 0）的个数
    int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
    // 表示 val 的字符数应该为 ⌈mag / shift⌉
    int chars = Math.max(((mag + (shift - 1)) / shift), 1);
    char[] buf = new char[chars];

    formatUnsignedLong(val, shift, buf, 0, chars);
    return new String(buf, true);
}

// 将无符号整数 val 转化成 2^shift 进制的字符串并写入到 buf 中。返回最低的字符位置
static int formatUnsignedLong(long val, int shift, char[] buf, int offset, int len) {
    int charPos = len;
    int radix = 1 << shift;
    int mask = radix - 1;
    // 还是求商取余法，只是因为进制是 2 的指数，所以可以使用掩码和移位操作加速
    do {
        buf[offset + --charPos] = Integer.digits[((int) val) & mask];
        val >>>= shift;
    } while (val != 0 && charPos > 0);

    return charPos;
}
```
`Integer`的`toUnsignedString`直接使用了`Long`的`toUnsignedString`和`toString`。可以看到，除了 2 的幂进制和 10 进制，
其他进制都使用了`java.math.BigInteger`来生成字符串。

## 2.6 parseLong
```java
// 将字符串解析为 radix 进制的数字。当 radix 非法时将会抛出异常
public static long parseLong(String s, int radix) throws NumberFormatException {
    if (s == null) {
        throw new NumberFormatException("null");
    }

    if (radix < Character.MIN_RADIX) {
        throw new NumberFormatException("radix " + radix +
                                        " less than Character.MIN_RADIX");
    }
    if (radix > Character.MAX_RADIX) {
        throw new NumberFormatException("radix " + radix +
                                        " greater than Character.MAX_RADIX");
    }

    long result = 0;
    boolean negative = false;
    // 需要注意 String.length 返回的是底层字符数组的长度。不过数字和字母字符一个 char 都能容纳下，所以这是没问题的
    int i = 0, len = s.length();
    long limit = -Long.MAX_VALUE;
    long multmin;
    int digit;

    if (len > 0) {
        char firstChar = s.charAt(0);
        // 首先判断开头是不是正负号
        if (firstChar < '0') { // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true;
                // 负数的话，limit 变成最小负数
                limit = Long.MIN_VALUE;
            } else if (firstChar != '+')
                throw NumberFormatException.forInputString(s);

            if (len == 1) // Cannot have lone "+" or "-"
                throw NumberFormatException.forInputString(s);
            i++;
        }
        multmin = limit / radix;
        // result 统一为负数
        while (i < len) {
            // 将单个字符解析为数字，非法字符会返回 -1
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit(s.charAt(i++),radix);
            if (digit < 0) {
                throw NumberFormatException.forInputString(s);
            }
            if (result < multmin) {
                throw NumberFormatException.forInputString(s);
            }
            result *= radix;
            if (result < limit + digit) {
                throw NumberFormatException.forInputString(s);
            }
            result -= digit;
        }
    } else {
        throw NumberFormatException.forInputString(s);
    }
    return negative ? result : -result;
}

// 将 10 进制字符串解析为 long
public static long parseLong(String s) throws NumberFormatException {
    return parseLong(s, 10);
}

// 将字符串解析为 radix 进制的无符号数字
public static long parseUnsignedLong(String s, int radix) throws NumberFormatException {
    if (s == null)  {
        throw new NumberFormatException("null");
    }

    int len = s.length();
    if (len > 0) {
        char firstChar = s.charAt(0);
        // 无符号数不能有负号
        if (firstChar == '-') {
            throw new
                NumberFormatException(String.format("Illegal leading minus sign " +
                                                   "on unsigned string %s.", s));
        } else {
            // 不超出 long 能表示的正整数范围，则直接调用 parseLong(String s, int radix)
            if (len <= 12 || // Long.MAX_VALUE in Character.MAX_RADIX is 13 digits
                (radix == 10 && len <= 18) ) { // Long.MAX_VALUE in base 10 is 19 digits
                return parseLong(s, radix);
            }

            // 由于进行了上述测试，因此无需对 len 进行范围检查
            // No need for range checks on len due to testing above.
            // 分离最后一位数字和其他数字
            long first = parseLong(s.substring(0, len - 1), radix);
            int second = Character.digit(s.charAt(len - 1), radix);
            if (second < 0) {
                throw new NumberFormatException("Bad digit at end of " + s);
            }
            // first 由于除去了最后一位，因此只要不超出范围，它会是一个正数
            long result = first * radix + second;
            // 如果无符号结果 result 比 first 还小，说明发生了溢出
            if (compareUnsigned(result, first) < 0) {
                /*
                 * The maximum unsigned value, (2^64)-1, takes at
                 * most one more digit to represent than the
                 * maximum signed value, (2^63)-1.  Therefore,
                 * parsing (len - 1) digits will be appropriately
                 * in-range of the signed parsing.  In other
                 * words, if parsing (len -1) digits overflows
                 * signed parsing, parsing len digits will
                 * certainly overflow unsigned parsing.
                 *
                 * The compareUnsigned check above catches
                 * situations where an unsigned overflow occurs
                 * incorporating the contribution of the final
                 * digit.
                 */
                throw new NumberFormatException(String.format("String value %s exceeds " +
                                                              "range of unsigned long.", s));
            }
            return result;
        }
    } else {
        throw NumberFormatException.forInputString(s);
    }
}
```
`Integer.parseUnsignedInt`方法使用了`Long.parseLong`方法。

## 2.7 valueOf
```java
// 将字符串解析为 radix 进制的 Long 对象
public static Long valueOf(String s, int radix) throws NumberFormatException {
    return Long.valueOf(parseLong(s, radix));
}

// 将字符串解析为 10 进制的 Long 对象
public static Long valueOf(String s) throws NumberFormatException {
    return Long.valueOf(parseLong(s, 10));
}

// 将 long i 包装成 Integer 对象
public static Long valueOf(long l) {
    final int offset = 128;
    // cache 范围内的数字直接取创建好的对象
    if (l >= -128 && l <= 127) { // will cache
        return LongCache.cache[(int)l + offset];
    }
    return new Long(l);
}
```
这个方法和`parseLong`方法都能从字符串中解析整数，不过`parseLong`返回的是`long`，而它返回`Long`对象。
在你一定要使用`Long`对象的时候可以使用此方法，否则强烈推荐使用`parseLong`方法。

## 2.8 decode
```java
// 将 nm 解析为整数并包装在 Long 对象中。这个整数的进制由其前导符号表示。
// 前导 "0x"、"0X" 和 "#" 表示 16 进制字符串；前导 "0" 表示 8 进制字符串
public static Long decode(String nm) throws NumberFormatException {
    int radix = 10;
    int index = 0;
    boolean negative = false;
    Long result;

    if (nm.length() == 0)
        throw new NumberFormatException("Zero length string");
    char firstChar = nm.charAt(0);
    // 处理符号
    // Handle sign, if present
    if (firstChar == '-') {
        negative = true;
        index++;
    } else if (firstChar == '+')
        index++;

    // 根据前导符号，确定进制
    // Handle radix specifier, if present
    if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
        index += 2;
        radix = 16;
    } else if (nm.startsWith("#", index)) {
        index ++;
        radix = 16;
    } else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
        index ++;
        radix = 8;
    }

    if (nm.startsWith("-", index) || nm.startsWith("+", index))
        throw new NumberFormatException("Sign character in wrong position");

    try {
        // 下面两行能够应付大多数情况，只是不能处理 Long.MIN_VALUE
        // 因为它是把字符串当成正整数来解析的，但是负数比正数要多一
        result = Long.valueOf(nm.substring(index), radix);
        result = negative ? Long.valueOf(-result.longValue()) : result;
    } catch (NumberFormatException e) {
        // If number is Long.MIN_VALUE, we'll end up here. The next line
        // handles this case, and causes any genuine format error to be
        // rethrown.
        String constant = negative ? ("-" + nm.substring(index))
                                   : nm.substring(index);
        result = Long.valueOf(constant, radix);
    }
    return result;
}
```

## 2.9 getLong
```java
// 获取系统属性 nm 对应的 Long 值。如果 nm 不存在或者解析错误，返回 val
public static Long getLong(String nm, Long val) {
    String v = null;
    try {
        v = System.getProperty(nm);
    } catch (IllegalArgumentException | NullPointerException e) {
    }
    if (v != null) {
        try {
            return Long.decode(v);
        } catch (NumberFormatException e) {
        }
    }
    return val;
}

// 获取系统属性 nm 对应的 Long 值。如果 nm 不存在或者解析错误，返回 val
public static Long getLong(String nm, long val) {
    Long result = Long.getLong(nm, null);
    return (result == null) ? Long.valueOf(val) : result;
}

// 获取系统属性 nm 对应的 Long 值。如果 nm 不存在或者解析错误，返回 null
public static Long getLong(String nm) {
    return getLong(nm, null);
}
```

## 2.10 最高位和最低位
```java
// 参数 i 二进制最高位 1 表示的整数，比如 9 返回 1 << 3
public static long highestOneBit(long i) {
    // HD, Figure 3-1
    // 注意下面是 >>，不是 >>>
    // 把最高位 1 右移一位，并与原数据按位取或。那么这就使得最高位(原数字)和它的下一位是连续两个 1
    i |= (i >>  1);
    // 把刚刚移位得到连续两个 1 继续右移两位并与原数据按位取或。那么这就使得最高两位和它的下两个连续位组成四个连续的 1
    i |= (i >>  2);
    // 把刚刚移位得到连续四个 1 继续右移四位并与原数据按位取或。那么这就使得最高四位和它的下四个连续位组成八个连续的 1
    i |= (i >>  4);
    // 把刚刚移位得到连续八个 1 继续右移八位并与原数据按位取或。那么这就使得最高八位和它的下八个连续位组成十六个连续的 1
    i |= (i >>  8);
    // 把刚刚移位得到连续十六个 1 继续右移十六位并与原数据按位取或。那么这就使得最高十六位和它的下十六个连续位组成三十二个连续的 1
    i |= (i >> 16);
    // 把刚刚移位得到连续三十二个 1 继续右移三十二位并与原数据按位取或。那么这就使得最高三十二位和它的下三十二个连续位
    // 组成六十四个连续的 1
    i |= (i >> 32);
    // 上面的操作结束后，i 从最高位开始往右都是 1。下面的操作会取 i 的最高位 1，也就是最高位 1
    return i - (i >>> 1);
}

// 参数 i 二进制最低位 1 表示的整数
public static long lowestOneBit(long i) {
    // HD, Section 2-1
    // 一个数的负数就是将它按位取反再加 1。因此它和它的负数按位与，除最后一位 1 外，其他都会变成 0
    return i & -i;
}
```

## 2.11 bitCount
```java
// 计算 i 二进制补码形式中 1 的个数
public static int bitCount(long i) {
    // HD, Figure 5-14
    // 将每两个 bit 的 bit 数计算后放到这两个 bit 上
    i = i - ((i >>> 1) & 0x5555555555555555L);
    // 两两合并上一步的结果并放到 4 个 bit 上
    i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
    // 两两合并上一步的结果并放到 4 个 bit 上
    i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
    // 两两合并上一步的结果并放到 8 个 bit 上
    i = i + (i >>> 8);
    // 两两合并上一步的结果并放到 16 个 bit 上
    i = i + (i >>> 16);
    // 两两合并上一步的结果并放到 32 个 bit 上
    i = i + (i >>> 32);
    // 计算最终结果
    return (int)i & 0x7f;
}
```
`Long.bitCount`方法原理和`Integer.bitCount`一样，在此不再详述。

## 2.12 旋转位
```java
// 将 i 中左边的 distance 个 bit 移到右边
public static long rotateLeft(long i, int distance) {
    // 在移位的时候，如果移位值小于 0（比如下面的 -distance），会根据被移位数的长度进行转换。
    // 假设 distance 大于 0，i >>> -distance 就等同于 i >>> (SIZE - distance)
    return (i << distance) | (i >>> -distance);
}

// 将 i 中右边的 distance 个 bit 移到左边
public static long rotateRight(long i, int distance) {
    return (i >>> distance) | (i << -distance);
}
```

## 2.13 signum
```java
// 返回指定值的符号。如果 i 大于 0，返回 1；i 等于 0，返回 0；i 小于 0，返回 -1
public static int signum(long i) {
    // HD, Section 2-7
    // i >> 63，如果 i 是正数，结果就是 0；i 是负数，结果就是 0xffffffffffffffff，也就是 -1；i 是 0，结果是 0
    // -i >>> 63，如果 i 是正数，结果就是 1；i 是负数，结果就是 0 或 1（MIN_VALUE）；i 是 0，结果是 0
    // 因此，i 是正数结果肯定为 1；i 是负数结果肯定是 0xffffffffffffffff，也就是 -1；i 是 0，结果就是 0
    return (int) ((i >> 63) | (-i >>> 63));
}
```

## 2.14 转置
```java
// 返回通过反转 i 的二进制补码中的位顺序而获得的值
public static long reverse(long i) {
    // HD, Figure 7-1
    // 将每两位 bit 之间进行调换
    i = (i & 0x5555555555555555L) << 1 | (i >>> 1) & 0x5555555555555555L;
    // 类似的，下面的操作将四个 bit 中两个两个进行了调换
    i = (i & 0x3333333333333333L) << 2 | (i >>> 2) & 0x3333333333333333L;
    // 类似的，下面的操作将八个 bit 中四个四个进行了调换
    i = (i & 0x0f0f0f0f0f0f0f0fL) << 4 | (i >>> 4) & 0x0f0f0f0f0f0f0f0fL;
    // 类似的，下面的操作将十六个 bit 中八个八个进行了调换
    i = (i & 0x00ff00ff00ff00ffL) << 8 | (i >>> 8) & 0x00ff00ff00ff00ffL;
    // 以每 16 个 bit 为单位，进行倒置操作。就这样，最终结果就是位反过来的 i
    i = (i << 48) | ((i & 0xffff0000L) << 16) |
        ((i >>> 16) & 0xffff0000L) | (i >>> 48);
    return i;
}

// 以字节为单位将 i 反转
public static long reverseBytes(long i) {
    // 将每两个字节之间进行调换
    i = (i & 0x00ff00ff00ff00ffL) << 8 | (i >>> 8) & 0x00ff00ff00ff00ffL;
    // 以每 16 个 bit 为单位，进行倒置操作
    return (i << 48) | ((i & 0xffff0000L) << 16) |
        ((i >>> 16) & 0xffff0000L) | (i >>> 48);
}
```

# 3. 内部类/接口/枚举

## 3.1 LongCache
```java
private static class LongCache {
    private LongCache(){}

    static final Long cache[] = new Long[-(-128) + 127 + 1];

    // 缓存 [-128, 127] 范围内的 Long 对象
    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new Long(i - 128);
    }
}
```


[box]: 自动装箱与拆箱.md
[integer]: Integer.md
[native]: annotation/Native.md