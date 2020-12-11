`java.lang.Character`类的声明如下：
```java
public final class Character implements java.io.Serializable, Comparable<Character>
```
首先需要知道的是，`Java`中的`char`表示一个 UTF-16 代码单元。

`Character`是`char`的包装器类。`Java SE 8`平台使用`Unicode 6.2`版，此外还加了两个扩展。首先，
`Java SE 8`允许`Character`类的实现使用 6.2 版本之后的`Unicode`标准第一个版本中的日语时代代码点 U+32FF。
其次，为了包含新货币符号，`Java SE 8`允许`Character`类的实现使用`Unicode 10.0`版中的`Currency Symbols`部分。
因此，在处理上述代码点（版本6.2之外）时，`Character`类的字段和方法的行为在`Java SE 8`的实现中可能会有所不同，
但除了以下定义`Java`标识符的方法外：`isJavaIdentifierStart(int)`，`isJavaIdentifierStart(char)`，
`isJavaIdentifierPart(int)`和`isJavaIdentifierPart(char)`。`Java`标识符中的代码点必须来自`Unicode 6.2`版。

`Character`不仅包含了对字符的操作方法，也提供了一组判断、转换代码点的方法。

`Character`代码中比较值得注意的有：
1. 2.4 节中的常量折叠
2. 2.6 节中的 "backwards"
3. 2.9 节中的位操作替代布尔判断。
4. 3.2 节中的表驱动法（表驱动法具有良好的[局部性][locality]）；`enum`的使用

有关 Unicode 和 Java 增补字符集的知识参见[字符集编码][charset]。

使用`Character`类时可能会有[装箱拆箱操作][box]。

# 1. 成员字段

## 1.1 进制位数
```java
// 最小2进制
public static final int MIN_RADIX = 2;

// 最大36进制，26个英文字母+10个数字
public static final int MAX_RADIX = 36;
```
进制转换方法包括：`digit(char ch, int radix)`, `forDigit(int digit, int radix)`, `Integer.toString(int i, int radix)`, 
`Integer.valueOf(String s)`。

## 1.2 范围
```java
public static final char MIN_VALUE = '\u0000';

public static final char MAX_VALUE = '\uFFFF';
```

## 1.3 TYPE
```java
public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");
```
`Class.getPrimitiveClass()`方法是一个`native`方法，专门用来获取基本类型的`Class`对象。
需要注意的是，`char.class`等于`Character.TYPE`，但是`char.class`不等于`Character.class`。

## 1.4 Unicode 字符一般类别属性
```java
// "Cn"：其他，未赋值（不存在任何字符具有此属性）
public static final byte UNASSIGNED = 0;

// "Lu"：字母，大写
public static final byte UPPERCASE_LETTER = 1;

// "Ll"：字母，小写
public static final byte LOWERCASE_LETTER = 2;

// "Lt"：字母，词首字母大写
public static final byte TITLECASE_LETTER = 3;

// "Lm"：字母，修饰符
public static final byte MODIFIER_LETTER = 4;

// "Lo"：字母，其他
public static final byte OTHER_LETTER = 5;

// "Mn"：标记，非间距
public static final byte NON_SPACING_MARK = 6;

// "Me"：标记，间距组合
public static final byte COMBINING_SPACING_MARK = 8;

// "Nd"：数字，十进制数
public static final byte DECIMAL_DIGIT_NUMBER = 9;

// "Nl"：数字，字母
public static final byte LETTER_NUMBER = 10;

// "No"：数字，其他
public static final byte OTHER_NUMBER = 11;

// "Zs"：分隔符，空白
public static final byte SPACE_SEPARATOR = 12;

// "Zl"：分隔符，行
public static final byte LINE_SEPARATOR = 13;

// "Zp"：分隔符，段落
public static final byte PARAGRAPH_SEPARATOR = 14;

// "Cc"：其他，控制
public static final byte CONTROL = 15;

// "Cf"：其他，格式
public static final byte FORMAT = 16;

// "Co"：其他，代理项
public static final byte PRIVATE_USE = 18;

// "Cs"：其他，私用
public static final byte SURROGATE = 19;

// "Pd"：标点，短划线
public static final byte DASH_PUNCTUATION = 20;

// "Ps"：标点，开始
public static final byte START_PUNCTUATION = 21;

// "Pe"：标点，结束
public static final byte END_PUNCTUATION = 22;

// "Pc"：标点，连接符
public static final byte CONNECTOR_PUNCTUATION = 23;

// "Po"：标点，其他
public static final byte OTHER_PUNCTUATION = 24;

// "Sm"：符号，数学
public static final byte MATH_SYMBOL = 25;

// "Sc"：符号，货币
public static final byte CURRENCY_SYMBOL = 26;

// "Sk"：符号，修饰符
public static final byte MODIFIER_SYMBOL = 27;

// "So"：符号，其他
public static final byte OTHER_SYMBOL = 28;

// "Pi"：标点，前引号（根据用途可能表现为类似 Ps 或 Pe）
public static final byte INITIAL_QUOTE_PUNCTUATION = 29;

// "Pf"：标点，后引号（根据用途可能表现为类似 Ps 或 Pe）
public static final byte FINAL_QUOTE_PUNCTUATION = 30;

// 错误码，使用 int 代码点避免和 U+FFFF 冲突
static final int ERROR = 0xFFFFFFFF;
```
除了具有一种属性的字符外，某些字符可能有多个属性。<strong>组合字符(Combining character)</strong>是指一般类别属性的值为`Mc`、
`Mn`、`Me`的所有字符。组合字符通常用于与它的基本字符组合为一个字符，比如:

![组合字符][combine-char]

以上字符由字符 g(u+0067) 和 U+0308 组合而成，其中字符 g 就是基本字符，而 U+0308 就是组合字符。


## 1.5 双向字符类型
```java
// 未定义的方向类型。在 Unicode 标准里未定义的 char 具有这个方向类型 
public static final byte DIRECTIONALITY_UNDEFINED = -1;

// 强字符：L，left to right
public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;

// 强字符：R，right to left
public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;

// 强字符：AL，right to left Arabic
public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;

// 弱字符：EN，欧洲数字(European Number)
public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;

// 弱字符：ES，欧洲数字分隔符
public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;

// 弱字符：ET，欧洲数字终止符
public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;

// 弱字符：AN，阿拉伯数字
public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;

// 弱字符：CS，普通数字分隔符
public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;

// 弱字符：NSM，无间距标记(Nonspacing mark)
public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;

// 弱字符：BN，中性边界
public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;

// 中性字符：B，段落分隔符
public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;

// 中性字符：S，节分隔符(Segment Separator)
public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;

// 中性字符：WS，空白(Whitespace)
public static final byte DIRECTIONALITY_WHITESPACE = 12;

// 中性字符：ON，其他中性符
public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;

// 显示定向嵌入和重写格式化字符：LRE，left to right mark
public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;

// 显示定向嵌入和重写格式化字符：LRO，left to right override
public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;

// 显示定向嵌入和重写格式化字符：RLE，right to left embedding
public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;

// 显示定向嵌入和重写格式化字符：RLO，right to left override
public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;

// 显示定向嵌入和重写格式化字符：PDF，pop directional formatting
public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
```
[有关双向性的问题参见这个链接][bidirectional]。
由于`Java8`采用的是`Unicode 6.2`，所以*运行等级和隔离运行序列*相关内容没有被加入其中。

## 1.6 代理范围
```java
// 高代理范围最小值
public static final char MIN_HIGH_SURROGATE = '\uD800';

// 高代理范围最大值
public static final char MAX_HIGH_SURROGATE = '\uDBFF';

// 低代理范围最小值
public static final char MIN_LOW_SURROGATE  = '\uDC00';

// 低代理范围最大值
public static final char MAX_LOW_SURROGATE  = '\uDFFF';

// 代理范围最小值
public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;

// 代理范围最大值
public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
```

## 1.7 代码点范围
```java
// 增补字符最小代码点
public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;

// 最小代码点
public static final int MIN_CODE_POINT = 0x000000;

// 最大代码点
public static final int MAX_CODE_POINT = 0X10FFFF;
```

# 2. 方法

## 2.1 hashCode
```java
@Override
public int hashCode() {
    // value 是 Character 包装的基本类型字符
    return Character.hashCode(value);
}

public static int hashCode(char value) {
    return (int)value;
}
```
可以看到，`Character`的`hashCode`方法就是返回字符本身。

## 2.2 toString
```java
public String toString() {
    char buf[] = {value};
    return String.valueOf(buf);
}
```
`toString`先将字符包装在数组中，再使用`String.valueOf()`方法返回字符串表示。实际上，`String.valueOf(Char c)`内部也是这样操作的。

## 2.3 代理部分转换
```java
public static char highSurrogate(int codePoint) {
    return (char) ((codePoint >>> 10)
        + (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
}

public static char lowSurrogate(int codePoint) {
    return (char) ((codePoint & 0x3ff) + MIN_LOW_SURROGATE);
}
```
代码点编码为代理部分的方法可以参见[字符集编码][charset]。

## 2.4 codePoint 判断方法
```java
// codePoint 是否 Unicode 规定范围内
public static boolean isValidCodePoint(int codePoint) {
    // Optimized form of:
    //     codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT
    // 下面利用位操作达到了和上面注释中一样的效果
    // Unicode 最大代码点是 0X10FFFF
    // 注意，此方法并未检测处于代理范围内的代码点是否合法
    int plane = codePoint >>> 16;
    return plane < ((MAX_CODE_POINT + 1) >>> 16);
}

// codePoint 是否在基本多语言面（Basic Multilingual Plane，BMP）内
public static boolean isBmpCodePoint(int codePoint) {
    return codePoint >>> 16 == 0;
}

// codePoint 是否是增补字符
public static boolean isSupplementaryCodePoint(int codePoint) {
    return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT
        && codePoint <  MAX_CODE_POINT + 1;
}

// codePoint 是否在高代理部分
public static boolean isHighSurrogate(char ch) {
    // 常量折叠参见《常量折叠.md》一文
    // Help VM constant-fold; MAX_HIGH_SURROGATE + 1 == MIN_LOW_SURROGATE
    return ch >= MIN_HIGH_SURROGATE && ch < (MAX_HIGH_SURROGATE + 1);
}

// codePoint 是否在低代理部分
public static boolean isLowSurrogate(char ch) {
    return ch >= MIN_LOW_SURROGATE && ch < (MAX_LOW_SURROGATE + 1);
}

// codePoint 是否在代理部分
public static boolean isSurrogate(char ch) {
    return ch >= MIN_SURROGATE && ch < (MAX_SURROGATE + 1);
}

// high 和 low 是否是一对代理对
public static boolean isSurrogatePair(char high, char low) {
    return isHighSurrogate(high) && isLowSurrogate(low);
}
```
常量折叠参见[常量折叠.md][constant-fold]。

## 2.5 字符转化为 codePoint
```java
// 将代理对转化为代码点
public static int toCodePoint(char high, char low) {
    // 代理对转化为代码点：
    //  - 高位代理减去 0xD800，低位代理减去 0xDC00
    //  - 高位代理左移 10 位，再和低位代理相加，得到一个 20 位的整数
    //  - 将上一步的结果加上 0x10000，得到代码点
    //  解析方法是编码方法的逆过程，编码方法在《字符集编码.md》中已有论述
    // Optimized form of:
    // return ((high - MIN_HIGH_SURROGATE) << 10)
    //         + (low - MIN_LOW_SURROGATE)
    //         + MIN_SUPPLEMENTARY_CODE_POINT;
    return ((high << 10) + low) + (MIN_SUPPLEMENTARY_CODE_POINT
                                   - (MIN_HIGH_SURROGATE << 10)
                                   - MIN_LOW_SURROGATE);
}

// 计算 CharSequence 中 index 处的代码点，索引范围 [0, length)。
// 如果 index 处是低代理或 BMP 字符，直接返回这个 char；
// 如果 index 处是高代理，看看后面是不是低代理，是的话返回解析的代码点；不是（或超出范围）返回这个高代理
public static int codePointAt(CharSequence seq, int index) {
    char c1 = seq.charAt(index);
    // 如果 c1 是高代理部分且还有字符
    if (isHighSurrogate(c1) && ++index < seq.length()) {
        char c2 = seq.charAt(index);
        // 如果 c2 是低代理部分，则可以和 c1 组成代码点
        if (isLowSurrogate(c2)) {
            return toCodePoint(c1, c2);
        }
    }
    return c1;
}

// 计算字符数组中 index 处的代码点
public static int codePointAt(char[] a, int index) {
    return codePointAtImpl(a, index, a.length);
}

// 计算字符数组中 index 处的代码点，且用来计算的最大下标不超过 limit
public static int codePointAt(char[] a, int index, int limit) {
    if (index >= limit || limit < 0 || limit > a.length) {
        throw new IndexOutOfBoundsException();
    }
    return codePointAtImpl(a, index, limit);
}

static int codePointAtImpl(char[] a, int index, int limit) {
    char c1 = a[index];
    if (isHighSurrogate(c1) && ++index < limit) {
        char c2 = a[index];
        if (isLowSurrogate(c2)) {
            return toCodePoint(c1, c2);
        }
    }
    return c1;
}

// 计算 CharSequence 中 index 前一处代码点，索引范围 [1, length]。
// 如果 index - 1 处是高代理或 BMP 字符，直接返回这个 char；
// 如果 index - 1 处是低代理，看看 index - 2 是不是高代理，是的话返回解析的代码点；不是（或超出范围）返回这个低代理
public static int codePointBefore(CharSequence seq, int index) {
    char c2 = seq.charAt(--index);
    if (isLowSurrogate(c2) && index > 0) {
        char c1 = seq.charAt(--index);
        if (isHighSurrogate(c1)) {
            return toCodePoint(c1, c2);
        }
    }
    return c2;
}

// codePointBefore 的数组方法和 codePointAt 类似，不再列出
```

## 2.6 codePoint 转化为字符
```java
// 将 codePoint 转化为字符并写入 dst 数组的 dstIndex 处。返回转换的字符数。
// 注意，这个方法不会检测 dstIndex 的合法性
public static int toChars(int codePoint, char[] dst, int dstIndex) {
    // BMP 字符可以直接转型
    if (isBmpCodePoint(codePoint)) {
        dst[dstIndex] = (char) codePoint;
        return 1;
    } else if (isValidCodePoint(codePoint)) {
        // 合法代码点使用 toSurrogates 进行转换
        toSurrogates(codePoint, dst, dstIndex);
        return 2;
    } else {
        throw new IllegalArgumentException();
    }
}

static void toSurrogates(int codePoint, char[] dst, int index) {
    // 由于没有检测 index 是否合法，因此从后往前复制，就防止了当 index 不合法抛出异常，不会有赋值了一半的情况。
    // We write elements "backwards" to guarantee all-or-nothing
    dst[index+1] = lowSurrogate(codePoint);
    dst[index] = highSurrogate(codePoint);
}
```

## 2.7 codePoint 计数
```java
// 计算 CharSequence 中代码点的数量，范围为 [beginIndex, endIndex - 1]。文本范围内的每个不成对代理都计为一个代码点。
public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
    int length = seq.length();
    if (beginIndex < 0 || endIndex > length || beginIndex > endIndex) {
        throw new IndexOutOfBoundsException();
    }
    int n = endIndex - beginIndex;
    for (int i = beginIndex; i < endIndex; ) {
        if (isHighSurrogate(seq.charAt(i++)) && i < endIndex &&
            isLowSurrogate(seq.charAt(i))) {
            n--;
            i++;
        }
    }
    return n;
}

// 计算字符数组中代码点的数量。offset 参数是子数组第一个 char 的索引，而 count 参数指定子数组的长度。
// 子数组中每个不成对代理都计为一个代码点。
public static int codePointCount(char[] a, int offset, int count) {
    if (count > a.length - offset || offset < 0 || count < 0) {
        throw new IndexOutOfBoundsException();
    }
    return codePointCountImpl(a, offset, count);
}

static int codePointCountImpl(char[] a, int offset, int count) {
    int endIndex = offset + count;
    int n = count;
    for (int i = offset; i < endIndex; ) {
        if (isHighSurrogate(a[i++]) && i < endIndex &&
            isLowSurrogate(a[i])) {
            n--;
            i++;
        }
    }
    return n;
}
```

## 2.8 offsetByCodePoints
```java
// 返回从 index 处偏移 codePointOffset 个代码点的索引。codePointOffset 为正表示向右，为负表示向左。
// 文本范围内的每个不成对代理都计为一个代码点。
public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset) {
    int length = seq.length();
    if (index < 0 || index > length) {
        throw new IndexOutOfBoundsException();
    }

    int x = index;
    if (codePointOffset >= 0) {
        int i;
        for (i = 0; x < length && i < codePointOffset; i++) {
            if (isHighSurrogate(seq.charAt(x++)) && x < length &&
                isLowSurrogate(seq.charAt(x))) {
                x++;
            }
        }

        if (i < codePointOffset) {
            throw new IndexOutOfBoundsException();
        }
    } else {
        int i;
        for (i = codePointOffset; x > 0 && i < 0; i++) {
            if (isLowSurrogate(seq.charAt(--x)) && x > 0 &&
                isHighSurrogate(seq.charAt(x-1))) {
                x--;
            }
        }

        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
    return x;
}

// 返回从 index 处偏移 codePointOffset 个代码点的索引。codePointOffset 为正表示向右，为负表示向左。
// 其中 start 和 count 参数指定数组 a 的子数组。 
public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset) {
    if (count > a.length-start || start < 0 || count < 0
        || index < start || index > start+count) {
        throw new IndexOutOfBoundsException();
    }
    return offsetByCodePointsImpl(a, start, count, index, codePointOffset);
}

static int offsetByCodePointsImpl(char[]a, int start, int count, int index, int codePointOffset) {
    int x = index;
    if (codePointOffset >= 0) {
        int limit = start + count;
        int i;
        for (i = 0; x < limit && i < codePointOffset; i++) {
            if (isHighSurrogate(a[x++]) && x < limit &&
                isLowSurrogate(a[x])) {
                x++;
            }
        }
        if (i < codePointOffset) {
            throw new IndexOutOfBoundsException();
        }
    } else {
        int i;
        for (i = codePointOffset; x > start && i < 0; i++) {
            if (isLowSurrogate(a[--x]) && x > start &&
                isHighSurrogate(a[x-1])) {
                x--;
            }
        }
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
    return x;
}
```

## 2.9 判断字符/代码点的一般属性类别
<!-- TODO: 解读 CharacterName 和 CharacterData 类 -->
```java
// 判断字符 ch 是不是小写字母 
public static boolean isLowerCase(char ch) {
    return isLowerCase((int)ch);
}

// 判断 codePoint 是不是小写字母
public static boolean isLowerCase(int codePoint) {
    return getType(codePoint) == Character.LOWERCASE_LETTER ||
           CharacterData.of(codePoint).isOtherLowercase(codePoint);
}

// 还有很多判断方法，它们的实现和 isLowerCase 类似，不再列出

// 判断 codePoint 是否在 Unicode 中被分配给字符
public static boolean isDefined(int codePoint) {
    return getType(codePoint) != Character.UNASSIGNED;
}

// 判断 codePoint 是不是字母。
public static boolean isLetter(int codePoint) {
    // 下面的五个常量值是 [1, 5]。结果移位和与操作之后，值为 0b111110。
    // 这个值在右移 getType() 步，如果 Type 不在 [1, 5] 范围内，和 1 进行与操作将会是 0
    // 下面的代码是一个非常精彩的操作。当你有一些从 0 开始定义表示类别的常量时，
    // 使用位运算将比一连串的布尔运算要快。
    return ((((1 << Character.UPPERCASE_LETTER) |
        (1 << Character.LOWERCASE_LETTER) |
        (1 << Character.TITLECASE_LETTER) |
        (1 << Character.MODIFIER_LETTER) |
        (1 << Character.OTHER_LETTER)) >> getType(codePoint)) & 1)
        != 0;
}

// 判断 codePoint 是不是空格类字符（空白分隔符、行分隔符、段分隔符）
public static boolean isSpaceChar(int codePoint) {
    return ((((1 << Character.SPACE_SEPARATOR) |
        (1 << Character.LINE_SEPARATOR) |
        (1 << Character.PARAGRAPH_SEPARATOR)) >> getType(codePoint)) & 1)
        != 0;
}

// 判断 codePoint 是不是 ISO 控制字符
public static boolean isISOControl(int codePoint) {
    // Optimized form of:
    //     (codePoint >= 0x00 && codePoint <= 0x1F) ||
    //     (codePoint >= 0x7F && codePoint <= 0x9F);
    return codePoint <= 0x9F &&
        (codePoint >= 0x7F || (codePoint >>> 5 == 0));
}

// 确判断 codePoint 是不是根据 Unicode 规范指定的镜像字符。例如，'\u0028'左括号在语义上被定义为左括号。
// 在从左到右的文本中显示为“（”，但在从右到左的文本中显示为“）”。
public static boolean isMirrored(int codePoint) {
    return CharacterData.of(codePoint).isMirrored(codePoint);
}

// 判断 codePoint 是否为 Unicode 标准定义的 CJKV（中文，日文，韩文和越南文）表意文字。
public static boolean isIdeographic(int codePoint) {
    return CharacterData.of(codePoint).isIdeographic(codePoint);
}

// 判断 codePoint 可否为合法 Java 标识符的第一个字符
public static boolean isJavaIdentifierStart(int codePoint) {
    return CharacterData.of(codePoint).isJavaIdentifierStart(codePoint);
}

// 判断 codePoint 可否为合法 Java 标识符的字符
public static boolean isJavaIdentifierPart(int codePoint) {
    return CharacterData.of(codePoint).isJavaIdentifierPart(codePoint);
}

// 确定在 Java 标识符或 Unicode 标识符中，应将 codePoint 视为可忽略字符。
public static boolean isIdentifierIgnorable(int codePoint) {
    return CharacterData.of(codePoint).isIdentifierIgnorable(codePoint);
}

// 获取 codePoint 的一般属性类别，这些类别在 1.1.4 中定义
public static int getType(int codePoint) {
    return CharacterData.of(codePoint).getType(codePoint);
}
```

## 2.10 转换
```java
// 将 char 转化为小写形式
public static char toLowerCase(char ch) {
    return (char)toLowerCase((int)ch);
}

// 将 codePoint 转换为小写形式
public static int toLowerCase(int codePoint) {
    return CharacterData.of(codePoint).toLowerCase(codePoint);
}

// 其他类似转换不再列出

// 以指定的基数返回 codePoint 的数值。基数范围 [MIN_RADIX, MAX_RADIX]。codePoint 需要是数字或英文字母。
// 不合法的参数将返回 -1
public static int digit(int codePoint, int radix) {
    return CharacterData.of(codePoint).digit(codePoint, radix);
}

// 返回 codePoint 表示的int值。例如，字符'\u216C'(罗马数字五十)将返回一个值为 50 的 int。
// 大写字母，小写字母和他们的全角变体的数字值从 10 到 35。这独立于 Unicode 规范，Unicode 规范不将数字值分配给这些 char 值。
// 如果字符没有数字值，则返回-1。 如果字符的数字值不能表示为非负整数（例如，小数），则返回-2。
public static int getNumericValue(int codePoint) {
    return CharacterData.of(codePoint).getNumericValue(codePoint);
}

// 将 digit 转换为对应进制 radix 下的字符
public static char forDigit(int digit, int radix) {
    // 如果数字大于进制或是附属，返回空字符
    if ((digit >= radix) || (digit < 0)) {
        return '\0';
    }
    // 进制不在合法范围内，返回空字符
    if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
        return '\0';
    }
    if (digit < 10) {
        return (char)('0' + digit);
    }
    return (char)('a' - 10 + digit);
}
```

## 2.11 getDirectionality
```java
// 获取 char ch 的方向性
public static byte getDirectionality(char ch) {
    return getDirectionality((int)ch);
}

// 获取 codePoint 的方向性
public static byte getDirectionality(int codePoint) {
    return CharacterData.of(codePoint).getDirectionality(codePoint);
}
```

## 2.12 compare
```java
@Override
public int compareTo(Character anotherCharacter) {
    return compare(this.value, anotherCharacter.value);
}

public static int compare(char x, char y) {
    return x - y;
}
```
由于字符加减在`int`范围内肯定不会溢出，因此这里之间将`x`和`y`相减。

## 2.13 reverseBytes
```java
// 将 ch 的两个字节调换
public static char reverseBytes(char ch) {
    return (char) (((ch & 0xFF00) >> 8) | (ch << 8));
}
```

## 2.14 getName
```java
// 返回 codePoint 的 Unicode 名称
public static String getName(int codePoint) {
    if (!isValidCodePoint(codePoint)) {
        throw new IllegalArgumentException();
    }
    String name = CharacterName.get(codePoint);
    if (name != null)
        return name;
    if (getType(codePoint) == UNASSIGNED)
        return null;
    UnicodeBlock block = UnicodeBlock.of(codePoint);
    if (block != null)
        return block.toString().replace('_', ' ') + " "
               + Integer.toHexString(codePoint).toUpperCase(Locale.ENGLISH);
    // should never come here
    return Integer.toHexString(codePoint).toUpperCase(Locale.ENGLISH);
}
```

## 2.15 toUpperCase 变种
```java
// 使用 UnicodeData 文件中的信息将 codePoint 转换为大写。
static int toUpperCaseEx(int codePoint) {
    assert isValidCodePoint(codePoint);
    return CharacterData.of(codePoint).toUpperCaseEx(codePoint);
}

// 使用 Unicode 规范 SpecialCasing 文件中的大小写映射信息，将 codePoint 转换为大写。如果字符没有显式的大写映射，
// 则 char 本身将在 char[] 返回。
static char[] toUpperCaseCharArray(int codePoint) {
    // As of Unicode 6.0, 1:M uppercasings only happen in the BMP.
    assert isBmpCodePoint(codePoint);
    return CharacterData.of(codePoint).toUpperCaseCharArray(codePoint);
}
```
这两个方法被`java.lang.String`和其工具类所使用。

# 3. 内部接口/类/枚举

## 3.1 Subset
```java
public static class Subset  {

    private String name;

    protected Subset(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    public final boolean equals(Object obj) {
        return (this == obj);
    }

    public final int hashCode() {
        return super.hashCode();
    }

    public final String toString() {
        return name;
    }
}
```
这个类用来表示`Unicode`的一个子集。在`Character`类中`UnicodeBlock`继承了它。因为每个实例表示一个子集，
而对每个子集不需要更多的对象来表示，所以它的`equals`、`hashCode`方法都使用了`Object`里面的默认实现，并且都是`final`的，
这保证对它的每个子类都适用。`Java API`的其他部分可能出于自己的目的定义其他子集。

## 3.2 UnicodeBlock
```java
public static final class UnicodeBlock extends Subset {

    private static Map<String, UnicodeBlock> map = new HashMap<>(256);

    private UnicodeBlock(String idName) {
        super(idName);
        map.put(idName, this);
    }

    private UnicodeBlock(String idName, String alias) {
        this(idName);
        map.put(alias, this);
    }

    private UnicodeBlock(String idName, String... aliases) {
        this(idName);
        for (String alias : aliases)
            map.put(alias, this);
    }

    public static final UnicodeBlock  BASIC_LATIN =
            new UnicodeBlock("BASIC_LATIN",
                             "BASIC LATIN",
                             "BASICLATIN");

    public static final UnicodeBlock LATIN_1_SUPPLEMENT =
            new UnicodeBlock("LATIN_1_SUPPLEMENT",
                             "LATIN-1 SUPPLEMENT",
                             "LATIN-1SUPPLEMENT");

    // ... 还有很多不同的 UnicodeBlock，分别代表一种 Unicode 子集
    // 关于 UnicodeBlock 的最新情报参见：http://www.unicode.org/Public/UNIDATA/Blocks.txt
    
    // 每种子集的开始代码点
    private static final int blockStarts[] = {
        0x0000,   // 0000..007F; Basic Latin
        0x0080,   // 0080..00FF; Latin-1 Supplement
        0x0100,   // 0100..017F; Latin Extended-A
        0x0180,   // 0180..024F; Latin Extended-B
        0x0250,   // 0250..02AF; IPA Extensions
        0x02B0,   // 02B0..02FF; Spacing Modifier Letters
        0x0300,   // 0300..036F; Combining Diacritical Marks
        0x0370,   // 0370..03FF; Greek and Coptic
        0x0400,   // 0400..04FF; Cyrillic
        0x0500,   // 0500..052F; Cyrillic Supplement
        0x0530,   // 0530..058F; Armenian
        0x0590,   // 0590..05FF; Hebrew
        0x0600,   // 0600..06FF; Arabic
        0x0700,   // 0700..074F; Syriac
        0x0750,   // 0750..077F; Arabic Supplement
        0x0780,   // 0780..07BF; Thaana
        0x07C0,   // 07C0..07FF; NKo
        0x0800,   // 0800..083F; Samaritan
        0x0840,   // 0840..085F; Mandaic
        // 注意下面的 "unassigned"，这一段代码点没有分配字符
        0x0860,   //             unassigned
        // ... 还有很多
    };

    // 和 blockStarts 一一对应
    private static final UnicodeBlock[] blocks = {
        BASIC_LATIN,
        LATIN_1_SUPPLEMENT,
        LATIN_EXTENDED_A,
        LATIN_EXTENDED_B,
        IPA_EXTENSIONS,
        SPACING_MODIFIER_LETTERS,
        COMBINING_DIACRITICAL_MARKS,
        GREEK,
        CYRILLIC,
        CYRILLIC_SUPPLEMENTARY,
        ARMENIAN,
        HEBREW,
        ARABIC,
        SYRIAC,
        ARABIC_SUPPLEMENT,
        THAANA,
        NKO,
        SAMARITAN,
        MANDAIC,
        null, // 未分配的代码点

        // ... 还有很多
    };

    public static UnicodeBlock of(char c) {
        return of((int)c);
    }

    /**
     * 判断 codePoint 属于哪个 UnicodeBlock。
     */
    public static UnicodeBlock of(int codePoint) {
        // isValidCodePoint 是 Character 的方法
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException();
        }
    
        int top, bottom, current;
        bottom = 0;
        top = blockStarts.length;
        current = top/2;
    
        // 下面代码使用二分查找确定范围。这是表驱动法的应用，为了高效的查找，构造了 blockStarts 和 blocks 两个查找数组
        // invariant: top > current >= bottom && codePoint >= unicodeBlockStarts[bottom]
        while (top - bottom > 1) {
            if (codePoint >= blockStarts[current]) {
                bottom = current;
            } else {
                top = current;
            }
            current = (top + bottom) / 2;
        }
        return blocks[current];
    }

    /**
     * 通过名称或别名获取 UnicodeBlock。
     */
    public static final UnicodeBlock forName(String blockName) {
        UnicodeBlock block = map.get(blockName.toUpperCase(Locale.US));
        if (block == null) {
            throw new IllegalArgumentException();
        }
        return block;
    }
}
```
`UnicodeBlock`继承了`Subset`，在内部预定义了大量的`Unicode`子集。从源码中可以看出，为了实现高效的查找操作，
运用了**表驱动法**这一编程技术。`UnicodeBlock`类没有定义其他属性和方法，因此这是一个为查找确定`Unicode`子集及其名称而生的类。

## 3.3 UnicodeScript
```java
public static enum UnicodeScript {
    /**
     * Unicode script "Common".
     */
    COMMON,

    /**
     * Unicode script "Latin".
     */
    LATIN,

    /**
     * Unicode script "Greek".
     */
    GREEK,

    // ... 还有很多

    // 不同 UnicodeScript 的代码点范围
    private static final int[] scriptStarts = {
        0x0000,   // 0000..0040; COMMON
        0x0041,   // 0041..005A; LATIN
        0x005B,   // 005B..0060; COMMON
        0x0061,   // 0061..007A; LATIN
        0x007B,   // 007B..00A9; COMMON
        0x00AA,   // 00AA..00AA; LATIN
        0x00AB,   // 00AB..00B9; COMMON
        0x00BA,   // 00BA..00BA; LATIN
        0x00BB,   // 00BB..00BF; COMMON
        0x00C0,   // 00C0..00D6; LATIN
        0x00D7,   // 00D7..00D7; COMMON
        0x00D8,   // 00D8..00F6; LATIN
        0x00F7,   // 00F7..00F7; COMMON
        0x00F8,   // 00F8..02B8; LATIN
        0x02B9,   // 02B9..02DF; COMMON
        0x02E0,   // 02E0..02E4; LATIN
        0x02E5,   // 02E5..02E9; COMMON
        0x02EA,   // 02EA..02EB; BOPOMOFO

        // ... 还有很多
    }

    // 和 scriptStarts 对应
    private static final UnicodeScript[] scripts = {
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        LATIN,
        COMMON,
        BOPOMOFO,

        // ... 还有很多
    }

    // 别名
    private static HashMap<String, Character.UnicodeScript> aliases;
    static {
        aliases = new HashMap<>(128);
        aliases.put("ARAB", ARABIC);
        aliases.put("ARMI", IMPERIAL_ARAMAIC);
        aliases.put("ARMN", ARMENIAN);
        aliases.put("AVST", AVESTAN);
        aliases.put("BALI", BALINESE);
        aliases.put("BAMU", BAMUM);
        aliases.put("BATK", BATAK);
        aliases.put("BENG", BENGALI);
        aliases.put("BOPO", BOPOMOFO);
        aliases.put("BRAI", BRAILLE);

        // ... 还有很多
    }

    /**
     * 判断 codePoint 属于哪个 UnicodeScript。
     */
    public static UnicodeScript of(int codePoint) {
        if (!isValidCodePoint(codePoint))
            throw new IllegalArgumentException();
        // getType() 是 Character 的方法，获取 codePoint 的一般属性类别
        int type = getType(codePoint);
        // leave SURROGATE and PRIVATE_USE for table lookup
        if (type == UNASSIGNED)
            return UNKNOWN;
        // 目标值不在数组内的，返回 -(第一个大于目标值的元素的下标+1)
        int index = Arrays.binarySearch(scriptStarts, codePoint);
        if (index < 0)
            index = -index - 2;
        return scripts[index];
    }

    public static final UnicodeScript forName(String scriptName) {
        scriptName = scriptName.toUpperCase(Locale.ENGLISH);
                             //.replace(' ', '_'));
        UnicodeScript sc = aliases.get(scriptName);
        if (sc != null)
            return sc;
        // scriptName 在别名里面找不到，而 UnicodeScript 是 enum，它有 valueOf 方法可以根据名称查找枚举。
        return valueOf(scriptName);
    }
}
```
`UnicodeScript`表示的是字符子集族，它在 [Unicode Standard Annex #24: Script Names][script-name] 中定义。
每个`Unicode`字符都分配给单个`Unicode`脚本，该脚本可以是诸如拉丁之类的特定脚本，也可以是以下三个特殊值之一，
即`Inherited`，`Common`或`Unknown`。

`UnicodeBlock`不是`enum`，而`UnicodeScript`是`enum`。之所这样是因为`UnicodeBlock`是继承自`Subset`，而`enum`无法继承其他类。

## 3.4 CharacterCache

```java
private static class CharacterCache {
    private CharacterCache(){}

    static final Character cache[] = new Character[127 + 1];

    static {
        for (int i = 0; i < cache.length; i++)
            cache[i] = new Character((char)i);
    }
}
```
对象缓存池，缓存了 128 个`ASCII`字符。


[box]: 自动装箱与拆箱.md
[charset]: 字符集编码.md
[bidirectional]: Unicode中的BIDI双向性算法.md
[combine-char]: ../../../res/img/char-combine.png
[script-name]: http://www.unicode.org/reports/tr24/
[constant-fold]: 常量折叠.md
[locality]: 程序的局部性原理.md