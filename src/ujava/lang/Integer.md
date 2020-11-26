`java.lang.Integer`类的声明如下：
```java
public final class Integer extends Number implements Comparable<Integer>
```
`Integer`是基本类型`int`的包装器类。此类提供了几种将`int`转换为`String`并将`String`转换为`int`的方法、
无符号运算、位操作以及其他在处理`int`时有用的常量和方法。

`Integer`代码中比较值得注意的有：
1. 2.2 `numberOfLeadingZeros`：移位和**二分法**求先导 0 个数
2. 2.3 有符号`toString`
    - 负数比正数多 1
    - 表驱动法（表驱动法具有良好的[局部性][locality]）可以用来确定范围、获取值。在这里使用它计算数字串长度、求数字字符
    - 左移和加法组合代替乘法
    - 右移和乘法组合代替除法
    - 使用`>>>`将`int`当成无符号数使用
3. 2.4 无符号`toString`
    - 同一包之间的类可以提供快速的包内方法调用
    - 使用整数操作计算向上取整
    - 当使用 2 的幂时，可以使用掩码和移位操作加速
4. 2.9 最高位和最低位
    - 使用移位和**二次递增法**覆盖低位`bit`
    - 一个数的负数就是将它按位取反再加 1
5. 2.10 `bitCount`：使用**归并法**和掩码求`bit`数
6. 2.11 旋转位：在移位的时候，如果移位值小于 0，会根据被移位数的长度进行转换。
7. 2.13 `reverse`转置位也用到了**归并法**和掩码思想。

`Integer`类的测试和验证在[IntegerTest.java][integer]文件中。

# 1. 成员字段

## 1.1 范围
```java
// int 最小值，-2^31，-21,4748,3648
@Native public static final int   MIN_VALUE = 0x80000000;

// int 最大值，2^31 - 1，21,4748,3647
@Native public static final int   MAX_VALUE = 0x7fffffff;
```
`@Native`注解参见[Native.md][native]。

## 1.2 TYPE
```java
public static final Class<Integer>  TYPE = (Class<Integer>) Class.getPrimitiveClass("int");
```

## 1.3 查找表
```java
// 在字符串中表示数字的字符
final static char[] digits = {
    '0' , '1' , '2' , '3' , '4' , '5' ,
    '6' , '7' , '8' , '9' , 'a' , 'b' ,
    'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
    'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
    'o' , 'p' , 'q' , 'r' , 's' , 't' ,
    'u' , 'v' , 'w' , 'x' , 'y' , 'z'
};

// 整数的十进制字符串长度查找表
final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

// 十位字符查找表
final static char [] DigitTens = {
    '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
    '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
    '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
    '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
    '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
    '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
    '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
    '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
    '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
    '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
} ;

// 个位字符查找表
final static char [] DigitOnes = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
} ;
```
为转成不同进制字符串所做的查找表。

# 2. 方法

 ## 2.1 无符号运算
```java
// 将 int x 转换为无符号 long
public static long toUnsignedLong(int x) {
    return ((long) x) & 0xffffffffL;
}

// 比较 x, y 的大小
public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
}

// 比较无符号数 x, y 的大小
public static int compareUnsigned(int x, int y) {
    // 正数加上 MIN_VALUE 会变成负数，正数越小则结果越小
    // 负数加上 MIN_VALUE 会变成正数，负数越小则结果越大
    // 负数在计算机中采用补码存储，越接近 0 的负数，如果符号位为 0 则它的正数形式越大（对应的无符号形式也就越大）。
    // 加上 MIN_VALUE 会使得负号的符号位溢出变成 0，此时负数越小结果也就越大
    return compare(x + MIN_VALUE, y + MIN_VALUE);
}

// 将 dividend 和 divisor 视为无符号数相除
public static int divideUnsigned(int dividend, int divisor) {
    // In lieu of tricky code, for now just use long arithmetic.
    return (int)(toUnsignedLong(dividend) / toUnsignedLong(divisor));
}

// 将 dividend 和 divisor 视为无符号数进行取余运算
public static int remainderUnsigned(int dividend, int divisor) {
    // In lieu of tricky code, for now just use long arithmetic.
    return (int)(toUnsignedLong(dividend) % toUnsignedLong(divisor));
}
```

## 2.2 前导零和后导零
```java
// 返回无符号整型 i 的二进制补码形式中最高位（最左的 1）之前的零位数目。如果等于零，则返回 32
public static int numberOfLeadingZeros(int i) {
    // HD, Figure 5-6
    if (i == 0)
        return 32;
    int n = 1;
    // 如果 i 的前 16 位为 0，将 i 左移 16 位
    if (i >>> 16 == 0) { n += 16; i <<= 16; }
    // 如果 i 的前 24 位为 0，将 i 左移 8 位
    if (i >>> 24 == 0) { n +=  8; i <<=  8; }
    // 如果 i 的前 28 位为 0，将 i 左移 4 位
    if (i >>> 28 == 0) { n +=  4; i <<=  4; }
    // 如果 i 的前 30 位为 0，将 i 左移 2 位
    if (i >>> 30 == 0) { n +=  2; i <<=  2; }
    // 每次移一半，二分法
    // 对最后两个数判断
    n -= i >>> 31;
    return n;
}

// 返回无符号整型 i 的二进制补码形式中最低位（最右的 1）之后的零位数目。如果等于零，则返回 32
public static int numberOfTrailingZeros(int i) {
    // HD, Figure 5-14
    int y;
    if (i == 0) return 32;
    int n = 31;
    // 左移 16 位，判断后 16 位有没有 1，不等于 0 表示有 1。注意一旦后 16 位有 1，i 就会被赋值为 y
    y = i <<16; if (y != 0) { n = n -16; i = y; }
    // 再左移 8 位，判断后 8 位有没有 1
    y = i << 8; if (y != 0) { n = n - 8; i = y; }
    // 再左移 4 位，判断后 4 位有没有 1
    y = i << 4; if (y != 0) { n = n - 4; i = y; }
    // 再左移 2 位，判断后 2 位有没有 1
    y = i << 2; if (y != 0) { n = n - 2; i = y; }
    // 判断最后一位是不是 1
    // 如果最后一位是 1，则之前所有移位操作都会作用到 i 上，1 会被移到开头第 2 位上
    return n - ((i << 1) >>> 31);
}
```
`numberOfLeadingZeros`是一种二分法，不断的从中间的位置向左计算 0 的个数。如果左边都为 0，那么就把右边的数移动左边，
以此计算右边零的个数。它的实现分为以下步骤：
1. 判断整型`i`的值是否为 0，如果是，那么表明 0 的个数为32个，直接返回即可
2. 判断`i`右移 16 位后的值是否为 0。判断的是高 16 位的第一个非0数字是否存在。条件成立，则表明第一个非 0 位在低 16 位；
否则，第一个非零值在高 16 位。
3. 如果前面的判断条件成立，那么进入`if`里面的语句之后，会将 i 向左移，将低 16 位变成高 16 位。如果判断不成立，
即使非零位在高`16`为那么就没有必要进行移位的处理。
4. 后面的判断与前面类似。
5. `n -= i >>> 31`实际上就是二分法对最后两个数的判断。`n`初始化为1，因此，如果判断最后的位为`1`，那么就减少一个`n`的数，
否则为`i >>> 31`为 0 的话，那么就不操作（`n` 原先有一个 1）。

`numberOfTrailingZeros`和`numberOfLeadingZeros`类似，也是使用二分法。只不过它是使用左移，判断右边一半是不是都是 0。

## 2.3 有符号 toString
```java
// 将整数 i 转换成进制 radix 表示的字符串
public static String toString(int i, int radix) {
    // 进制非法，转成 10 进制
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        radix = 10;

    // 对 10 进制调用更快的版本
    /* Use the faster version */
    if (radix == 10) {
        return toString(i);
    }

    // 转成二进制的话会是最长字符串，可能会有 32 位加一个符号位
    char buf[] = new char[33];
    boolean negative = (i < 0);
    int charPos = 32;

    // 将 i 转成负数，统一处理。之所以统一转成负数，是因为最小负数比最大整数多一个，如果转成正数就会出错
    if (!negative) {
        i = -i;
    }

    // 下面的算法也就是一般的求商取余法
    // i <= -radix，在 i 是正数的时候也就是 i >= radix
    while (i <= -radix) {
        // java 中的负数取余和正数类似，只是结果是负数
        buf[charPos--] = digits[-(i % radix)];
        i = i / radix;
    }
    buf[charPos] = digits[-i];

    // 是负数就加上负号
    if (negative) {
        buf[--charPos] = '-';
    }

    // public String(char value[], int offset, int count)
    return new String(buf, charPos, (33 - charPos));
}

// 将整数 i 转换成十进制字符串
public static String toString(int i) {
    if (i == Integer.MIN_VALUE)
        return "-2147483648";
    // 计算长度
    int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
    char[] buf = new char[size];
    getChars(i, size, buf);
    // 下面是 String 的包构造器，直接将数组 buf 作为 String 的底层数组
    return new String(buf, true);
}

@Override
public String toString() {
    return toString(value);
}

// 计算整数的十进制字符串长度，需要是正整数
static int stringSize(int x) {
    for (int i=0; ; i++)
        // 表驱动法计算长度
        if (x <= sizeTable[i])
            return i+1;
}

// 将整数 i 转化为 10 进制字符串，并写入 buf 数组中。index 表示字符写入的最后下标，字符从 index(不包括)开始后向写入 buf。
// 这个方法当 i == Integer.MIN_VALUE 的时候会出错，这也是 toString(int i) 方法中对 Integer.MIN_VALUE 特别处理的原因
static void getChars(int i, int index, char[] buf) {
    int q, r;
    int charPos = index;
    char sign = 0;

    // 将 i 转换为正数。-Integer.MIN_VALUE 仍然会是 Integer.MIN_VALUE (0x80000000)
    if (i < 0) {
        sign = '-';
        i = -i;
    }
    
    /*
     * 此方法使用 "invariant division by multiplication" trick 来加速 Integer.toString。特别避免被 10 除。
     * 这个 trick 与非 JIT 虚拟机上的经典 Integer.toString 代码具有大致相同的性能。这个 trick 可以避免调用 .rem 和 .div 指令，
     * 但代码路径较长，因此由调度开销控制。在 JIT 的情况下，调度开销不存在，所以它会比经典代码快得多。
     */

    // 下面的代码每轮生成两位整数，个位和十位
    // Generate two digits per iteration
    while (i >= 65536) {
        // 下面求 q 和 r 的目的是得到十位数 r，这样就能用查找表得到个位和十位字符
        q = i / 100;
        // 使用移位操作代替乘法
        // really: r = i - (q * 100);
        r = i - ((q << 6) + (q << 5) + (q << 2));
        i = q;
        // 不包括 index，所以是 --charPos
        // 表驱动法查找个位数和十位数
        buf [--charPos] = DigitOnes[r];
        buf [--charPos] = DigitTens[r];
    }

    // 对于较小的数字（小于等于 65536），使用更快速的方法
    // Fall thru to fast mode for smaller numbers
    // assert(i <= 65536, i);
    for (;;) {
        // 下面的代码等同于: q = (i * 52429) / 524288。那么就相当于 q = i * 0.1 也就是 q = i / 10，
        // 这样通过乘法和向右移位的组合的形式代替了除法，能提高效率。
        q = (i * 52429) >>> (16+3);
        r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
        buf [--charPos] = digits [r];
        i = q;
        if (i == 0) break;
    }
    if (sign != 0) {
        buf [--charPos] = sign;
    }
}
```
`invariant division by multiplication`技巧参见[Division by Invariant Integers using Multiplication][division]这篇论文。
下面来说明一下`getChars`方法中`65536`和`q = (i * 52429) >>> (16+3)`是怎么来的。

我们使用`num1`,`num2`,`num3`三个变量代替源代码中的数字`65536`、`52429`、`16+3`，便于后面分析使用。
既然我们要使用`q = (i * num2) >>> (num3)`的形式使用乘法和移位代替除法，那`么num2`和`num3`就要有这样的关系：
```java
num2 = (2 ^ num3 / 10 + 1)  // 整数相除是整数，加 1 是防止 0 除，保证数值稳定性
```
只有这样才能保证`(i * num2) >>> (num3)`结果接近于 0.1。

那么`52429`这个数是怎么来的呢?来看以下数据：
```java
2^10 = 1024, 103/1024 = 0.1005859375
2^11 = 2048, 205/2048 = 0.10009765625
2^12 = 4096, 410/4096 = 0.10009765625
2^13 = 8192, 820/8192 = 0.10009765625
2^14 = 16384, 1639/16384 = 0.10003662109375
2^15 = 32768, 3277/32768 = 0.100006103515625
2^16 = 65536, 6554/65536 = 0.100006103515625
2^17 = 131072, 13108/131072 = 0.100006103515625
2^18 = 262144, 26215/262144 = 0.10000228881835938
2^19 = 524288, 52429/524288 = 0.10000038146972656
2^20 = 1048576, 104858/1048576 = 0.1000003815
2^21 = 2097152, 209716/2097152 = 0.1000003815
2^22 = 4194304, 419431/4194304 = 0.1000001431
```
超过`22`的数字就不列举了，因为如果`num3`越大，就会要求`i`比较小，必须保证`(i * num2) >>> (num3)`的过程不会因为溢出而导致数据不准确。
因为要保证不能溢出，所以`num1`和`num2`就相互约束，`num1`增大，`num2`就要随之减小。
那么是怎么敲定`num1 = 65536, num2 = 524288, num3 = 19`的呢？有如下原因：
1. 52429 / 524288 = 0.10000038146972656 精度足够高。
2. 下一个精度较高的`num2`和`num3`的组合是`419431`和`22`，这样`num3`就等于`2^31 / 2^22 = 2^9 = 512`。`512`这个数字实在是太小了。
`65536`正好是 2^16。一个整数占 4 个字节，`65536`正好占了 2 个字节，选定这样一个数字有利于`CPU`访问数据。

其实`65536 * 52429`是超过了`int`的最大值的，一旦超过就要溢出，那么为什么还能保证`(num1 * num2) >>> num3`能得到正确的结果呢？
这和`>>>`有关，因为`>>>`表示无符号右移，他会在忽略符号位，空位都以 0 补齐。一个有符号的整数能表示的范围是`-2147483648`至`2147483647`，
但是无符号的整数能表示的范围就是`0-42,9496,7296(2^32)`，所以，只要保证`num2 * num3`的值不超过 2^32 次方就可以了。
`65536`是 2^16, `52429`正好小于 2^16,所以，他们的乘积在无符号向右移位就能[保证数字的准确性][integer]。

以上解读参考自[这篇文章][get-chars]。

## 2.4 无符号 toString
```java
// 将 i 转化成无符号格式的字符串，radix 是进制。
public static String toUnsignedString(int i, int radix) {
    // 使用 Long.toUnsignedString
    return Long.toUnsignedString(toUnsignedLong(i), radix);
}

// 将 i 转化成无符号格式的 10 进制字符串。
public static String toUnsignedString(int i) {
    // 使用 Long.toUnsignedString
    return Long.toString(toUnsignedLong(i));
}

// 将 i 转化为无符号形式的 16 进制字符串
public static String toHexString(int i) {
    return toUnsignedString0(i, 4);
}

// 将 i 转化为无符号形式的 8 进制字符串
public static String toOctalString(int i) {
    return toUnsignedString0(i, 3);
}

// 将 i 转化为无符号形式的 2 进制字符串
public static String toBinaryString(int i) {
    return toUnsignedString0(i, 1);
}

// 将 val 转化为无符号形式的字符串，进制为 2^shift
private static String toUnsignedString0(int val, int shift) {
    // Integer.numberOfLeadingZeros 返回 i 的二进制补码形式中最高位之后的零位数目。
    // mag 表示无符号整数 val 有效位（除前导 0）的个数
    // assert shift > 0 && shift <=5 : "Illegal shift value";
    int mag = Integer.SIZE - Integer.numberOfLeadingZeros(val);
    // 表示 val 的字符数应该为 ⌈mag / shift⌉
    int chars = Math.max(((mag + (shift - 1)) / shift), 1);
    char[] buf = new char[chars];

    formatUnsignedInt(val, shift, buf, 0, chars);

    // Use special constructor which takes over "buf".
    return new String(buf, true);
}

// 将无符号整数 val 转化成 2^shift 进制的字符串并写入到 buf 中。返回最低的字符位置
static int formatUnsignedInt(int val, int shift, char[] buf, int offset, int len) {
    int charPos = len;
    int radix = 1 << shift;
    int mask = radix - 1;
    // 还是求商取余法，只是因为进制是 2 的指数，所以可以使用掩码和移位操作加速
    do {
        buf[offset + --charPos] = Integer.digits[val & mask];
        val >>>= shift;
    } while (val != 0 && charPos > 0);

    return charPos;
}
```

## 2.5 parseInt
```java
// 将字符串解析为 radix 进制的数字。当 radix 非法时将会抛出异常，这和 2.3 toString 不一样
public static int parseInt(String s, int radix) throws NumberFormatException {
    // 此方法可能在虚拟机初始化 IntegerCache 时被调用。注意不要使用 valueOf 方法
    /*
     * WARNING: This method may be invoked early during VM initialization
     * before IntegerCache is initialized. Care must be taken to not use
     * the valueOf method.
     */

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

    int result = 0;
    boolean negative = false;
    // 需要注意 String.length 返回的是底层字符数组的长度。不过数字和字母字符一个 char 都能容纳下，所以这是没问题的
    int i = 0, len = s.length();
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
        char firstChar = s.charAt(0);
        // 首先判断开头是不是正负号
        if (firstChar < '0') { // Possible leading "+" or "-"
            if (firstChar == '-') {
                // 负数的话，limit 变成最小负数
                negative = true;
                limit = Integer.MIN_VALUE;
            } else if (firstChar != '+')
                throw NumberFormatException.forInputString(s);

            if (len == 1) // Cannot have lone "+" or "-"
                throw NumberFormatException.forInputString(s);
            i++;
        }
        // 将字符串解析为整数，不仅要关注格式是否正确，还要检测是否会溢出
        // result 在下面的循环中会是负数，因为负数比正数多 1 个，所以统一生成负数
        multmin = limit / radix;
        while (i < len) {
            // 将单个字符解析为数字，非法字符会返回 -1
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit(s.charAt(i++),radix);
            // 非法字符抛出异常
            if (digit < 0) {
                throw NumberFormatException.forInputString(s);
            }
            if (result < multmin) {
                throw NumberFormatException.forInputString(s);
            }
            // 将上一次的结果乘以进制 radix
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

// 将 10 进制字符串解析为 int
public static int parseInt(String s) throws NumberFormatException {
    return parseInt(s,10);
}

// 将字符串解析为 radix 进制的无符号数字
public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
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
            // 不超出 int 能表示的正整数范围，则直接调用 parseInt(String s, int radix)
            if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
                (radix == 10 && len <= 9) ) { // Integer.MAX_VALUE in base 10 is 10 digits
                return parseInt(s, radix);
            } else {
                // 使用 Long.parseLong 解析较大的整数
                long ell = Long.parseLong(s, radix);
                // 如果解析结果能够用四个字节表示，即在无符号 int 范围内，则返回解析结果，否则抛出异常表示超出范围
                if ((ell & 0xffff_ffff_0000_0000L) == 0) {
                    return (int) ell;
                } else {
                    throw new
                        NumberFormatException(String.format("String value %s exceeds " +
                                                            "range of unsigned int.", s));
                }
            }
        }
    } else {
        throw NumberFormatException.forInputString(s);
    }
}

// 将字符串解析为 10 进制的无符号数字
public static int parseUnsignedInt(String s) throws NumberFormatException {
    return parseUnsignedInt(s, 10);
}
```

## 2.6 valueOf
```java
// 将 int i 包装成 Integer 对象
public static Integer valueOf(int i) {
    // i 在缓存池中就直接返回已创建好的对象
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}

// 将字符串解析为 radix 进制的 Integer 对象
public static Integer valueOf(String s, int radix) throws NumberFormatException {
    return Integer.valueOf(parseInt(s,radix));
}

// 将字符串解析为 10 进制的 Integer 对象
public static Integer valueOf(String s) throws NumberFormatException {
    return Integer.valueOf(parseInt(s, 10));
}
```
这个方法和`parseInt`方法都能从字符串中解析整数，不过`parseInt`返回的是`int`，而它返回`Integer`对象。
在你一定要使用`Integer`对象的时候可以使用此方法，否则强烈推荐使用`parseInt`方法。

## 2.7 decode
```java
// 将 nm 解析为整数并包装在 Integer 对象中。这个整数的进制由其前导符号表示。
// 前导 "0x"、"0X" 和 "#" 表示 16 进制字符串；前导 "0" 表示 8 进制字符串
public static Integer decode(String nm) throws NumberFormatException {
        int radix = 10;
        int index = 0;
        boolean negative = false;
        Integer result;

        if (nm.length() == 0)
            throw new NumberFormatException("Zero length string");
        char firstChar = nm.charAt(0);
        // 解析符号
        // Handle sign, if present
        if (firstChar == '-') {
            negative = true;
            index++;
        } else if (firstChar == '+')
            index++;

        // 解析前导进制
        // Handle radix specifier, if present
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        }
        else if (nm.startsWith("#", index)) {
            index ++;
            radix = 16;
        }
        else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
            index ++;
            radix = 8;
        }

        if (nm.startsWith("-", index) || nm.startsWith("+", index))
            throw new NumberFormatException("Sign character in wrong position");

        try {
            // 下面两行能够应付大多数情况，只是不能处理 Integer.MIN_VALUE
            // 因为它是把字符串当成正整数来解析的，但是负数比正数要多一
            result = Integer.valueOf(nm.substring(index), radix);
            result = negative ? Integer.valueOf(-result.intValue()) : result;
        } catch (NumberFormatException e) {
            // 抛出异常，可能是 Integer.MIN_VALUE
            // If number is Integer.MIN_VALUE, we'll end up here. The next line
            // handles this case, and causes any genuine format error to be
            // rethrown.
            String constant = negative ? ("-" + nm.substring(index))
                                       : nm.substring(index);
            result = Integer.valueOf(constant, radix);
        }
        return result;
    }
```

## 2.8 getInteger
```java
// 获取系统属性 nm 对应的 Integer 值。如果 nm 不存在或者解析错误，返回 val
public static Integer getInteger(String nm, Integer val) {
    String v = null;
    try {
        v = System.getProperty(nm);
    } catch (IllegalArgumentException | NullPointerException e) {
    }
    if (v != null) {
        try {
            return Integer.decode(v);
        } catch (NumberFormatException e) {
        }
    }
    return val;
}

// 获取系统属性 nm 对应的 Integer 值。如果 nm 不存在或者解析错误，返回 val
public static Integer getInteger(String nm, int val) {
    Integer result = getInteger(nm, null);
    return (result == null) ? Integer.valueOf(val) : result;
}

// 获取系统属性 nm 对应的 Integer 值。如果 nm 不存在或者解析错误，返回 null
public static Integer getInteger(String nm) {
    return getInteger(nm, null);
}
```

## 2.9 最高位和最低位
```java
// 参数 i 二进制最高位 1 表示的整数，比如 9 返回 1 << 3
public static int highestOneBit(int i) {
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
    // 上面的操作结束后，i 从最高位开始往右都是 1。下面的操作会取 i 的最高位 1，也就是最高位 1
    return i - (i >>> 1);
}

// 参数 i 二进制最低位 1 表示的整数
public static int lowestOneBit(int i) {
    // HD, Section 2-1
    // 一个数的负数就是将它按位取反再加 1。因此它和它的负数按位与，除最后一位 1 外，其他都会变成 0
    return i & -i;
}
```

## 2.10 bitCount
```java
// 计算 i 二进制补码形式中 1 的个数
public static int bitCount(int i) {
    // HD, Figure 5-2
    // 0x55555555 二进制 01010101010101010101010101010101
    i = i - ((i >>> 1) & 0x55555555);
    // 0x33333333 二进制 00110011001100110011001100110011
    i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
    // 0x0f0f0f0f 二进制 00001111000011110000111100001111
    i = (i + (i >>> 4)) & 0x0f0f0f0f;
    i = i + (i >>> 8);
    i = i + (i >>> 16);

    // 0x3f 二进制 111111
    return i & 0x3f;
}
```
在介绍`bitCount`原理之前，我们先看一个最直观计算`bitCount`算法：
```java
public int bitCount1(int num) {
    int count = 0;
    do {
        if ((num & 1) == 1) {
            count++;
        }
        num >>= 1;
    } while (num > 0);
    return count;
}
```
从最低位开始，一位一位地统计是否为 1，时间复杂度为`O(n)`，`n`为总`bit`数。将这个算法优化一下形式如下：
```java
public int bitCount2(int num) {
    int count = 0;
    while (num > 0) {
        num = num & (num - 1);
        count++;
    }
    return count;
}
```
`n-1`后，`n`的最低位的 1 被消除了，然后与`n`进行位与，`n`变为最低位 1 置为 0 后的新整数，如：
`0b101100`减一变为`0b101011`，可以看到最低位的 1 消除了，然后与运算得到`0b101100 & 0b101011 = 0b101000`。
每循环一次，最低位的 1 被消除，如此循环多少次就有多少个 1，时间复杂度也是`O(n)`，但是这个`n`表示`bit`位为1的个数，
总体是要比上一个算法更优。

现在，想象一下，当一列的 1 摆在我们的面前，我们会怎么数？一个一个数，这是第一个算法的原理。或者两个两个地数？
`bitCount`就是如此实现的。如下图：
```
            二进制                       十进制
1  0   1  1   1  1   1  1   1  1     10 11 11 11 11
 01     10     10     10     10       1 2  2  2  2
         \     /       \     /           \/    \/
 01       0100           0100         1   4    4
               \       /                   \  /
 01               1000                1      8
     \          /                       \   /
         1001                             9

             767的二进制中的1的位数计算过程
```
每两位`bit`为一组，分别统计有几个 1，然后把结果存到这两个`bit`位上，如：11 有 2 个 1，结果为 10，10 替代 11 的存储到原位置。
然后进行加法计算，把所有的结果加起来。加的过程中又可以两两相加，减少计算流程。可以看出，这里用到了**归并思想**。

知道算法原理之后，我们得出以下步骤：
1. `i = i - ((i >>> 1) & 0x55555555)`
    - `i >>> 1`记为`a1`。将两个`bit`的左边一位移到右边
    - `a1 & 0x55555555`记为`a2`。`0x55555555`二进制是`01010101010101010101010101010101`，这样就取出了两个`bit`的左边一位
    - `i - a2`。使用两个`bit`计算 1 的数量：`0b11 - 0b01 = 0b10 = 2, 0b10 - 0b01 = 0b01 = 1, 0b01 - 0b00 = 1, 0b00 - 0b00 = 0`。
    这样就把两个`bit`的 1 的数量保存到了这两个`bit`上。
    - 这时`i`中存储了每两位的统计结果，可以进行两两相加，最后求和。
2. `i = (i & 0x33333333) + ((i >>> 2) & 0x33333333)`
    - `i & 0x33333333`记为`b1`。`0x33333333`二进制是`00110011001100110011001100110011`，这样就取出了四个`bit`的右边两位。
    - `(i >>> 2) & 0x33333333`记为`b2`。这样取出了四个`bit`的左边两位。
    - `b1 + b2`。将保存的`bit`数相加，存储到四个`bit`上。
3. `i = (i + (i >>> 4)) & 0x0f0f0f0f`
    - `i + (i >>> 4)`记为`c1`。将四个`bit`中记录的`bit`数和另外四个`bit`中记录的`bit`数相加。
    - `c1 & 0x0f0f0f0f`。`0x0f0f0f0f`二进制是`00001111000011110000111100001111`，因为 8 个`bit`位上的`bit`数最多 8 个，
    所以结果不会超过 4 个`bit`位能表达的范围，因此只需要后 4 个`bit`位上的结果。
4. `i = i + (i >>> 8)`和`i = i + (i >>> 16)`。毫无疑问，就是两两相加，最后的结果存到低 16 位中。
5. `i & 0x3f`。`0x3f`二进制`111111`。`bit`数量最多只有 32 个，因此最后 6 位数足够表示，最终的`bit`数量也就存在这里面。

原始文章参见[这里][bit-count]。

## 2.11 旋转位
```java
// 将 i 中左边的 distance 个 bit 移到右边
public static int rotateLeft(int i, int distance) {
    // 在移位的时候，如果移位值小于 0（比如下面的 -distance），会根据被移位数的长度进行转换。
    // 假设 distance 大于 0，i >>> -distance 就等同于 i >>> (SIZE - distance)
    return (i << distance) | (i >>> -distance);
}

// 将 i 中右边的 distance 个 bit 移到左边
public static int rotateRight(int i, int distance) {
    return (i >>> distance) | (i << -distance);
}
```

## 2.12 signum
```java
// 返回指定值的符号。如果 i 大于 0，返回 1；i 等于 0，返回 0；i 小于 0，返回 -1
public static int signum(int i) {
    // HD, Section 2-7
    // i >> 31，如果 i 是正数，结果就是 0；i 是负数，结果就是 0xffffffff，也就是 -1；i 是 0，结果是 0
    // -i >>> 31，如果 i 是正数，结果就是 1；i 是负数，结果就是 0 或 1（MIN_VALUE）；i 是 0，结果是 0
    // 因此，i 是正数结果肯定为 1；i 是负数结果肯定是 0xffffffff，也就是 -1；i 是 0，结果就是 0
    return (i >> 31) | (-i >>> 31);
}
```

## 2.13 转置
```java
// 返回通过反转 i 的二进制补码中的位顺序而获得的值
public static int reverse(int i) {
    // 这个方法用到的思路和 bitCount 很像
    // HD, Figure 7-1
    // 0x55555555 二进制 01010101010101010101010101010101
    // (i & 0x55555555) << 1 将每两个 bit 中的右边一位取出并放到左边
    // (i >>> 1) & 0x55555555 将每两个 bit 中的左边一位取出并放到右边
    // 两者或运算，就将两个 bit 进行了调换
    i = (i & 0x55555555) << 1 | (i >>> 1) & 0x55555555;
    // 0x33333333 二进制 00110011001100110011001100110011
    // 类似的，下面的操作将四个 bit 中两个两个进行了调换
    i = (i & 0x33333333) << 2 | (i >>> 2) & 0x33333333;
    // 类似的，下面的操作将八个 bit 中四个四个进行了调换
    i = (i & 0x0f0f0f0f) << 4 | (i >>> 4) & 0x0f0f0f0f;
    // 以每 8 个 bit 为单位，进行倒置操作。就这样，最终结果就是位反过来的 i
    i = (i << 24) | ((i & 0xff00) << 8) |
    ((i >>> 8) & 0xff00) | (i >>> 24);
    return i;
}

// 以字节为单位将 i 反转
public static int reverseBytes(int i) {
    //     将第一个字节移到第四个字节位置
    return ((i >>> 24)           ) |
    //     将第二个字节移到第三个字节位置，注意是">>"，保证负数不会出问题
           ((i >>   8) &   0xFF00) |
    //     将第三个字节移到第二个字节位置
           ((i <<   8) & 0xFF0000) |
    //     将第四个字节移到第一个字节位置
           ((i << 24));
}
```

# 3. 内部类/接口/枚举

## 3.1 IntegerCache
```java
// Integer 对象缓存池
private static class IntegerCache {
    static final int low = -128;
    static final int high;
    static final Integer cache[];

    static {
        // high value may be configured by property
        int h = 127;
        // Integer 缓存池所能缓存的最大值可在配置文件中配置，最小值固定为 -128
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                // 这里使用到了 parseInt 方法
                int i = parseInt(integerCacheHighPropValue);
                // 至少会缓存到 127
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                // 因为还要缓存负数值，所以注意数字长度不能超出 MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
            } catch( NumberFormatException nfe) {
                // If the property cannot be parsed into an int, ignore it.
            }
        }
        high = h;

        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);

        // range [-128, 127] must be interned (JLS7 5.1.7)
        assert IntegerCache.high >= 127;
    }

    private IntegerCache() {}
}
```
`Integer`对象缓存池，缓存范围为`[-128, 127]`，其中最大值可以由`JVM`配置。


[integer]: ../../../test/ujava/lang/IntegerTest.java
[locality]: 程序的局部性原理.md
[native]: annotation/Native.md
[division]: https://gmplib.org/~tege/divcnst-pldi94.pdf
[get-chars]: https://www.hollischuang.com/archives/1058
[bit-count]: https://www.php.cn/java-article-407093.html