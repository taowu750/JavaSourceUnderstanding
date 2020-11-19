`java.lang.Character`类的声明如下：
```java
public final class Character implements java.io.Serializable, Comparable<Character>
```
此类是`char`的包装器类。`Java SE 8`平台使用`Unicode 6.2`版，此外还加了两个扩展。首先，
`Java SE 8`允许`Character`类的实现使用 6.2 版本之后的`Unicode`标准第一个版本中的日语时代代码点 U+32FF。
其次，为了包含新货币符号，`Java SE 8`允许`Character`类的实现使用`Unicode 10.0`版中的`Currency Symbols`部分。
因此，在处理上述代码点（版本6.2之外）时，`Character`类的字段和方法的行为在`Java SE 8`的实现中可能会有所不同，
但除了以下定义`Java`标识符的方法外：`isJavaIdentifierStart(int)`，`isJavaIdentifierStart(char)`，
`isJavaIdentifierPart(int)`和`isJavaIdentifierPart(char)`。`Java`标识符中的代码点必须来自`Unicode 6.2`版。

[有关 Unicode 和 Java 增补字符集的知识请点击这个链接][charset]。

# 1. 成员字段

## 1.1 常量

### 1.1.1 进制位数
```java
// 最小2进制
public static final int MIN_RADIX = 2;

// 最大36进制，26个英文字母+10个数字
public static final int MAX_RADIX = 36;
```
进制转换方法包括：`digit(char ch, int radix)`, `forDigit(int digit, int radix)`, `Integer.toString(int i, int radix)`, 
`Integer.valueOf(String s)`。

### 1.1.2 范围
```java
public static final char MIN_VALUE = '\u0000';

public static final char MAX_VALUE = '\uFFFF';
```

### 1.1.3 TYPE
```java
public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");
```

### 1.1.4 Unicode 字符一般类别属性
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


### 1.1.5 双向字符类型
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


[charset]: 字符集编码.md
[bidirectional]: Unicode中的BIDI双向性算法.md
[combine-char]: ../../../res/img/char-combine.png