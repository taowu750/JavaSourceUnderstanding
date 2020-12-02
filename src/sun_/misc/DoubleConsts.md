`sun.misc.DoubleConsts`类的声明如下：
```java
public class DoubleConsts
```
此类包含了`double`类型的其他一些常量，比如阶码相关。

# 1. 成员字段

## 1.1 范围
```java
// 正无穷
public static final double POSITIVE_INFINITY = java.lang.Double.POSITIVE_INFINITY;

// 负无穷
public static final double NEGATIVE_INFINITY = java.lang.Double.NEGATIVE_INFINITY;

// 非数值
public static final double NaN = java.lang.Double.NaN;

// 最大值
public static final double MAX_VALUE = java.lang.Double.MAX_VALUE;

// 最小值
public static final double MIN_VALUE = java.lang.Double.MIN_VALUE;

// 最小规格化数
public static final double  MIN_NORMAL      = 2.2250738585072014E-308;
```

## 1.2 阶码相关
```java
// 尾数部分宽度（位数）。注意，应该是 52 位，这里算上了符号位
public static final int SIGNIFICAND_WIDTH   = 53;

// 最大指数（二进制）
public static final int     MAX_EXPONENT    = 1023;

// 最小指数（二进制）
public static final int     MIN_EXPONENT    = -1022;

// 最小的正 double 非规范化值所具有的指数（二进制）。它等于 FpUtils.ilogb(Double.MIN_VALUE) 的返回值
public static final int     MIN_SUB_EXPONENT = MIN_EXPONENT -
                                               (SIGNIFICAND_WIDTH - 1);

// 阶码偏移量
public static final int     EXP_BIAS        = 1023;
```

## 1.3 掩码
```java
// 符号位掩码
public static final long    SIGN_BIT_MASK   = 0x8000000000000000L;

// 阶码部分掩码
public static final long    EXP_BIT_MASK    = 0x7FF0000000000000L;

// 尾数有效位部分掩码
public static final long    SIGNIF_BIT_MASK = 0x000FFFFFFFFFFFFFL;
```

# 2. 构造器/块

## 2.1 static 初始化块
```java
static {
    // 验证位掩码覆盖所有位位置，并且位掩码不重叠
    // verify bit masks cover all bit positions and that the bit
    // masks are non-overlapping
    assert(((SIGN_BIT_MASK | EXP_BIT_MASK | SIGNIF_BIT_MASK) == ~0L) &&
           (((SIGN_BIT_MASK & EXP_BIT_MASK) == 0L) &&
            ((SIGN_BIT_MASK & SIGNIF_BIT_MASK) == 0L) &&
            ((EXP_BIT_MASK & SIGNIF_BIT_MASK) == 0L)));
}
```