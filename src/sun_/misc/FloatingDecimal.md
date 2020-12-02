`sun.misc.FloatingDecimal`类声明如下：
```java
public class FloatingDecimal
```
这一个用于在单精度或双精度浮点数的`ASCII`和十进制表示之间转换的类。尽管可以获取并重用`BinaryToASCIIConverter`实例，
但大多数转换都是通过静态方法提供的。

**待续...**

subnormal 和 sub 均表示非规格化数。

# 1. 成员字段

## 1.1 
```java
// 二进制移位值
static final int    EXP_SHIFT = DoubleConsts.SIGNIFICAND_WIDTH - 1;

static final long   FRACT_HOB = ( 1L<<EXP_SHIFT ); // assumed High-Order bit

// 用来表示为 1 的 double 指数。x-01111111111-xxx...xxx，2^0 = 1
static final long   EXP_ONE   = ((long)DoubleConsts.EXP_BIAS)<<EXP_SHIFT; // exponent of 1.0

// 
static final int    MAX_SMALL_BIN_EXP = 62;
static final int    MIN_SMALL_BIN_EXP = -( 63 / 3 );
static final int    MAX_DECIMAL_DIGITS = 15;

// 十进制指数最大值
static final int    MAX_DECIMAL_EXPONENT = 308;

// 十进制指数最小值
static final int    MIN_DECIMAL_EXPONENT = -324;
static final int    BIG_DECIMAL_EXPONENT = 324; // i.e. abs(MIN_DECIMAL_EXPONENT)
static final int    MAX_NDIGITS = 1100;

static final int    SINGLE_EXP_SHIFT  =   FloatConsts.SIGNIFICAND_WIDTH - 1;
static final int    SINGLE_FRACT_HOB  =   1<<SINGLE_EXP_SHIFT;
static final int    SINGLE_MAX_DECIMAL_DIGITS = 7;
static final int    SINGLE_MAX_DECIMAL_EXPONENT = 38;
static final int    SINGLE_MIN_DECIMAL_EXPONENT = -45;
static final int    SINGLE_MAX_NDIGITS = 200;

static final int    INT_DECIMAL_DIGITS = 9;
```

# 2. 方法

# 3. 内部类/接口/枚举

## 3.1 BinaryToASCIIConverter
```java
// 可以将单精度或双精度浮点值处理为 ASCII 字符串表示形式的转换器。
public interface BinaryToASCIIConverter {

    // 将浮点值转换为 ASCII 字符串
    public String toJavaFormatString();

    // 将浮点值附加到 Appendable 上
    public void appendTo(Appendable buf);

    // 检索最接近此值的十进制指数
    public int getDecimalExponent();

    // 将浮点数值以数字字符的形式写入到 digits 数组中。返回复制到数组中的有效位数
    public int getDigits(char[] digits);

    // 表示值是不是负数
    public boolean isNegative();

    // 如果值是无限大或 NaN 返回 true
    public boolean isExceptional();

    // 指示在二进制到 ASCII 转换期间是否将值四舍五入
    public boolean digitsRoundedUp();

    // 指示二进制到 ASCII 的转换是否精确
    public boolean decimalDigitsExact();
}
```

## 3.2 ExceptionalBinaryToASCIIBuffer
```java
// BinaryToASCIIConverter 的实现类。它仅用作无限大或 NaN 的 BinaryToASCIIConverter
private static class ExceptionalBinaryToASCIIBuffer implements BinaryToASCIIConverter {
    final private String image;
    private boolean isNegative;

    // image 也就是对应无限大或 NaN 的 ASCII 字符串
    public ExceptionalBinaryToASCIIBuffer(String image, boolean isNegative) {
        this.image = image;
        this.isNegative = isNegative;
    }

    @Override
    public String toJavaFormatString() {
        return image;
    }

    @Override
    public void appendTo(Appendable buf) {
        // 只接受 StringBuilder 和 StringBuffer
        if (buf instanceof StringBuilder) {
            ((StringBuilder) buf).append(image);
        } else if (buf instanceof StringBuffer) {
            ((StringBuffer) buf).append(image);
        } else {
            assert false;
        }
    }

    @Override
    public int getDecimalExponent() {
        throw new IllegalArgumentException("Exceptional value does not have an exponent");
    }

    @Override
    public int getDigits(char[] digits) {
        throw new IllegalArgumentException("Exceptional value does not have digits");
    }

    @Override
    public boolean isNegative() {
        return isNegative;
    }

    @Override
    public boolean isExceptional() {
        return true;
    }

    @Override
    public boolean digitsRoundedUp() {
        throw new IllegalArgumentException("Exceptional value is not rounded");
    }

    @Override
    public boolean decimalDigitsExact() {
        throw new IllegalArgumentException("Exceptional value is not exact");
    }
}
```

## 3.3
```java
// BinaryToASCIIConverter 的实现类。和 ExceptionalBinaryToASCIIBuffer 不同，它对无限大或 NaN 的其余数执行转换
static class BinaryToASCIIBuffer implements BinaryToASCIIConverter {
    private boolean isNegative;
    // 10 进制指数
    private int decExponent;
    // 第一个数字在 digits 数组中的下标
    private int firstDigitIndex;
    // 数字个数
    private int nDigits;
    // 存放浮点数 ASCII 码的数组
    private final char[] digits;
    private final char[] buffer = new char[26];

    // 下面的字段提供了有关在 dtoa() 和 roundup() 方法中完成的二进制到十进制数字转换结果的附加信息。
    // 如果这两种方法需要，它们会被改变。

    // 如果 dtoa() 从二进制到十进制的转换是精确的，则为 true。
    private boolean exactDecimalConversion = false;

    // 如果二进制到十进制转换的结果在转换过程结束时四舍五入，即调用 roundUp() 方法，则为 true。
    private boolean decimalDigitsRoundedUp = false;

    // 默认构造函数，用于非零值。BinaryToASCIIBuffer 可以是线程本地的并可重用
    BinaryToASCIIBuffer(){
        this.digits = new char[20];
    }

    // 创建一个特殊值（正负零）
    BinaryToASCIIBuffer(boolean isNegative, char[] digits){
        this.isNegative = isNegative;
        this.decExponent  = 0;
        this.digits = digits;
        this.firstDigitIndex = 0;
        this.nDigits = digits.length;
    }

    @Override
    public String toJavaFormatString() {
        int len = getChars(buffer);
        return new String(buffer, 0, len);
    }

    @Override
    public void appendTo(Appendable buf) {
        // 仅接受 StringBuilder 和 StringBuffer
        int len = getChars(buffer);
        if (buf instanceof StringBuilder) {
            ((StringBuilder) buf).append(buffer, 0, len);
        } else if (buf instanceof StringBuffer) {
            ((StringBuffer) buf).append(buffer, 0, len);
        } else {
            assert false;
        }
    }

    @Override
    public int getDecimalExponent() {
        return decExponent;
    }

    @Override
    public int getDigits(char[] digits) {
        System.arraycopy(this.digits,firstDigitIndex,digits,0,this.nDigits);
        return this.nDigits;
    }

    @Override
    public boolean isNegative() {
        return isNegative;
    }

    @Override
    public boolean isExceptional() {
        return false;
    }

    @Override
    public boolean digitsRoundedUp() {
        return decimalDigitsRoundedUp;
    }

    @Override
    public boolean decimalDigitsExact() {
        return exactDecimalConversion;
    }

    // 设置值的符号
    private void setSign(boolean isNegative) {
        this.isNegative = isNegative;
    }

    /**
     * 将浮点数转换为 ASCII 字符串并写入数组中。
     *
     * @param binExp 二进制指数
     * @param fractBits 尾数位数
     * @param nSignificantBits 有效位数
     * @param isCompatibleFormat 是不是兼容格式
     */
    private void dtoa( int binExp, long fractBits, int nSignificantBits, boolean isCompatibleFormat) {

    }

    /**
     * 这个方法处理一种简单的子情况：缩放后，所有有效位都保存在 lvalue 中。negSign 和 decExponent 告诉我们已经完成了哪些处理和缩放。
     * 特殊情况已经排除在外。特别是：lvalue 是一个有限数（不是 Inf，也不是 NaN），lvalue> 0L（不是零，也不是负数）。
     * 我们使用此方法处理数字而不是调用 Long.toString() 的唯一原因是我们可以更快地完成它，并且除了要特别对待尾随 0 之外。
     * 如果 Long.toString 发生变化，我们应该重新评估该策略！
     *
     * @param decExponent 十进制指数
     * @param lvalue long 值
     * @param insignificantDigits 尾数个数
     */
    private void developLongDigits( int decExponent, long lvalue, int insignificantDigits ){
        if ( insignificantDigits != 0 ){
            // 舍弃非有效低位，同时舍入至尾数值
            // 10^insignificantDigits
            long pow10 = FDBigInteger.LONG_5_POW[insignificantDigits] << insignificantDigits; // 10^i == 5^i * 2^i;
            long residue = lvalue % pow10;
            lvalue /= pow10;
            decExponent += insignificantDigits;
            if ( residue >= (pow10>>1) ){
                // round up based on the low-order bits we're discarding
                lvalue++;
            }
        }
        int  digitno = digits.length -1;
        int  c;
        if ( lvalue <= Integer.MAX_VALUE ){
            assert lvalue > 0L : lvalue; // lvalue <= 0
            // even easier subcase!
            // can do int arithmetic rather than long!
            int  ivalue = (int)lvalue;
            c = ivalue%10;
            ivalue /= 10;
            while ( c == 0 ){
                decExponent++;
                c = ivalue%10;
                ivalue /= 10;
            }
            while ( ivalue != 0){
                digits[digitno--] = (char)(c+'0');
                decExponent++;
                c = ivalue%10;
                ivalue /= 10;
            }
            digits[digitno] = (char)(c+'0');
        } else {
            // same algorithm as above (same bugs, too )
            // but using long arithmetic.
            c = (int)(lvalue%10L);
            lvalue /= 10L;
            while ( c == 0 ){
                decExponent++;
                c = (int)(lvalue%10L);
                lvalue /= 10L;
            }
            while ( lvalue != 0L ){
                digits[digitno--] = (char)(c+'0');
                decExponent++;
                c = (int)(lvalue%10L);
                lvalue /= 10;
            }
            digits[digitno] = (char)(c+'0');
        }
        this.decExponent = decExponent+1;
        this.firstDigitIndex = digitno;
        this.nDigits = this.digits.length - digitno;
    }
}
```