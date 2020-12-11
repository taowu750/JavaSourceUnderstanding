`java.lang.String`类的声明如下：
```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence
```
`String`类表示字符串。`Java`程序中的所有字符串文字（例如“abc”）都实现为此类的实例。

`String`类包含一些方法，这些方法可以检查序列中的各个字符，比较字符串，搜索字符串，提取子字符串以及创建字符串的副本，
并将所有字符均转换为大写或小写。大小写映射基于`Character`类指定的`Unicode`标准版本。

`String`是`UTF-16`编码的字符串，它的底层是`char`数组。其中增补字符由代理对表示（有关`UTF-16`的更多信息，
请参见[字符集编码.md][charset]）。索引指的是代码单位，因此补充字符在`String`中使用两个索引。
除了用于处理`UTF-16`代码单元（即`char`值）的方法外，`String`类还提供用于处理`Unicode`代码点（即字符）的方法。

`String`是一个不可变对象，在它上所做的任何更改都会生成一个新的字符串。

`String`代码中比较值得注意的有：
1. 3.7 getBytes: 避免`getfield`频繁调用
2. 3.9 toCharArray: `native`方法和类初始化
3. 3.12 lastIndexOf: continue LABEL
4. 3.15 大小写转换
    - 使用 break LABEL 跳出代码块
    - 找到第一个符合的字符再操作，避免没有符合的字符时创建数组
5. 3.22 split
    - 使用括号表达式同时赋值和运算
    - 使用`|`运算符判断是否在范围内

# 1. 成员字段

## 1.1 底层表示
```java
// 用于存储字符的 char 数组
private final char value[];

// 缓存字符串 hashCode 的变量
private int hash;
```
因为字符数组有属性`length`表示长度，所以`Java`里面的字符串不需要像`C`语言中的字符串那样用一个结尾`\0`字符表示字符串结束。

## 1.2 序列化
```java
// String 的序列化版本号
private static final long serialVersionUID = -6849794470754667710L;

// String 在序列化流协议中是特殊情况。根据对象序列化规范第6.2节“流元素”，将 String 实例写入 ObjectOutputStream 中。
private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
```

## 1.3 CASE_INSENSITIVE_ORDER
```java
// 一个 String 对象的比较器，它会以忽略大小写的方式解析比较。该比较器是可序列化的。
// 请注意，此比较器未考虑到语言环境，并且会导致某些语言环境的排序不理想。java.text 包提供了 Collators
// 以允许对语言环境敏感的排序。
public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
```

# 2. 构造器/块

## 2.1 无参构造器
```java
// 初始化一个新创建的 String 对象，使其代表一个空字符序列。注意，由于字符串是不可变的，因此不需要使用此构造函数。
// 一般情况下，可以直接使用 "" 代替这个构造器。
public String() {
    this.value = "".value;
}
```

## 2.2 从字符序列构造
```java
// 初始化一个新创建的 String 对象，使其表示与参数相同的字符序列； 换句话说，新创建的字符串是参数字符串的副本。
// 除非需要显式的 original 副本，否则不需要使用此构造函数，因为字符串是不可变的。
public String(String original) {
    // 这个副本底层数组和 original 是同一个对象，因为字符串是不可变的，所以这样是安全的
    this.value = original.value;
    this.hash = original.hash;
}

// 从 StringBuilder 复制字符构造 String。StringBuilder 的内容被复制过来，它的后续修改不会影响新创建的字符串。
// 通过 toString 方法从 StringBuilder 获取字符串可能会运行得更快，通常是首选。
public String(StringBuilder builder) {
    this.value = Arrays.copyOf(builder.getValue(), builder.length());
}

// 从 StringBuffer 复制字符构造 String。StringBuffer 的内容被复制过来，它的后续修改不会影响新创建的字符串。
public String(StringBuffer buffer) {
    // 因为 StringBuffer 可能被用于多线程，所以需要对 buffer 加锁
    synchronized(buffer) {
        this.value = Arrays.copyOf(buffer.getValue(), buffer.length());
    }
}
```

## 2.3 从字符数组构造
```java
// 从 char 数组复制字符构造 String。char 数组的内容被复制过来，它的后续修改不会影响新创建的字符串。
public String(char value[]) {
    this.value = Arrays.copyOf(value, value.length);
}

// 从 char 数组复制字符构造 String。offset 参数是子数组第一个字符的索引，而 count 参数指定子数组的长度。
// 子数组的内容被复制，它的的后续修改不会影响新创建的字符串。
public String(char value[], int offset, int count) {
    // 检查 offset 是否小于 0
    if (offset < 0) {
        throw new StringIndexOutOfBoundsException(offset);
    }
    if (count <= 0) {
        // 检查 count 是否小于 0
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        // count 为 0 且 offset 小于等于 value 的长度，则创建空字符串
        if (offset <= value.length) {
            this.value = "".value;
            return;
        }
    }
    // offset 或 count 可能会接近 Integer.MAX_VALUE，所以不能直接相加
    if (offset > value.length - count) {
        throw new StringIndexOutOfBoundsException(offset + count);
    }
    // Arrays.copyOfRange 复制指定区域内的数组
    this.value = Arrays.copyOfRange(value, offset, offset+count);
}

// 这是个包私有构造器，这个构造器是为了速度而提供的。此构造函数应总是在 share==true 的情况下调用。
String(char[] value, boolean share) {
    // assert share : "unshared not supported";
    this.value = value;
}
```

## 2.4 从代码点构造
```java
// 从 Unicode 代码点数组构造 String。offset 参数是子数组第一个字符的索引，而 count 参数指定子数组的长度。
public String(int[] codePoints, int offset, int count) {
    // 检查 offset 和 count 合法性
    if (offset < 0) {
        throw new StringIndexOutOfBoundsException(offset);
    }
    if (count <= 0) {
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        // count 为 0 且 offset 小于等于 value 的长度，则创建空字符串
        if (offset <= codePoints.length) {
            this.value = "".value;
            return;
        }
    }
    // offset 或 count 可能会接近 Integer.MAX_VALUE，所以不能直接相加
    if (offset > codePoints.length - count) {
        throw new StringIndexOutOfBoundsException(offset + count);
    }
    
    // 需要复制的代码点结束位置
    final int end = offset + count;

    // 第一步：计算从代码点转换来的 char 数组的精确大小
    int n = count;
    for (int i = offset; i < end; i++) {
        int c = codePoints[i];
        // 如果是 BMP 字符，则一个 char 足以表示
        if (Character.isBmpCodePoint(c))
            continue;
        // 如果是增补字符，则需要两个 char
        else if (Character.isValidCodePoint(c))
            n++;
        // 非法字符，抛出异常
        else throw new IllegalArgumentException(Integer.toString(c));
    }

    // 第二步：创建和填充 char 数组
    final char[] v = new char[n];

    for (int i = offset, j = 0; i < end; i++, j++) {
        int c = codePoints[i];
        // BMP 字符可以直接赋值
        if (Character.isBmpCodePoint(c))
            v[j] = (char)c;
        // 增补字符使用 Character.toSurrogates 方法将其分解为高代理字符和低代理字符
        else
            Character.toSurrogates(c, v, j++);
    }

    this.value = v;
}
```

## 2.5 从字节数组构造
```java
// 使用指定的字符集编码解析字节数组，构造字符串。offset 参数是字节数组第一个字节的索引，而 length 参数指定子数组的长度。
// 当给定字节在给定字符集中无效时，此构造函数的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public String(byte bytes[], int offset, int length, String charsetName) throws UnsupportedEncodingException {
    if (charsetName == null)
        throw new NullPointerException("charsetName");
    checkBounds(bytes, offset, length);
    this.value = StringCoding.decode(charsetName, bytes, offset, length);
}

// 使用 Charset 编码解析字节数组（一般使用 StandardCharsets 定义的 Charset），构造字符串。
// offset 参数是字节数组第一个字节的索引，而 length 参数指定子数组的长度。
// 当给定字节在给定字符集中无效时，此构造函数的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public String(byte bytes[], int offset, int length, Charset charset) {
    if (charset == null)
        throw new NullPointerException("charset");
    checkBounds(bytes, offset, length);
    this.value =  StringCoding.decode(charset, bytes, offset, length);
}

public String(byte bytes[], String charsetName) throws UnsupportedEncodingException {
    this(bytes, 0, bytes.length, charsetName);
}

public String(byte bytes[], Charset charset) {
    this(bytes, 0, bytes.length, charset);
}

// 使用平台默认的字符集编码解析字节数组，构造字符串。offset 参数是字节数组第一个字节的索引，而 length 参数指定子数组的长度。
// 当给定字节在给定字符集中无效时，此构造函数的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public String(byte bytes[], int offset, int length) {
    checkBounds(bytes, offset, length);
    this.value = StringCoding.decode(bytes, offset, length);
}

public String(byte bytes[]) {
    this(bytes, 0, bytes.length);
}

// 检查对于给定的字节数组，offset 和 length 是否合法
private static void checkBounds(byte[] bytes, int offset, int length) {
    if (length < 0)
        throw new StringIndexOutOfBoundsException(length);
    if (offset < 0)
        throw new StringIndexOutOfBoundsException(offset);
    if (offset > bytes.length - length)
        throw new StringIndexOutOfBoundsException(offset + length);
}
```
<!-- TODO: 在解读 util,io,nio 时弄懂 Locale、StringEncoding、Charset 类及其相关类 -->
从字节数组创建`String`可以指定编码，这在面向字节的`IO`流中很有用。

## 2.6 被废弃的构造函数
```java
/*
从 ASCII 码字节数组中构造 String。offset参数是子数组第一个字节的索引，而count参数指定子数组的长度。
如果 hibyte 参数不为 0，则将它作为 ascii 数组中每个字节对应的 16 位 Unicode 代码单元的高 8 位。

废弃原因：此方法现在不能正确将字节转换为字符。从 JDK 1.1 开始，执行此操作的首选方法是通过 3.5 节中的字节数组构造函数，
这些构造函数采用 Charset，Charset 名称或使用平台的默认 Charset。
*/
@Deprecated
public String(byte ascii[], int hibyte, int offset, int count) {
    checkBounds(ascii, offset, count);
    char value[] = new char[count];

    if (hibyte == 0) {
        // hibyte 等于 0，将 ascii 数组视为 ASCII 码字节数组
        for (int i = count; i-- > 0;) {
            value[i] = (char)(ascii[i + offset] & 0xff);
        }
    } else {
        // hibyte 不等于 0，将它作为 16 位 Unicode 代码单元的高 8 位
        hibyte <<= 8;
        for (int i = count; i-- > 0;) {
            value[i] = (char)(hibyte | (ascii[i + offset] & 0xff));
        }
    }
    this.value = value;
}

@Deprecated
public String(byte ascii[], int hibyte) {
    this(ascii, hibyte, 0, ascii.length);
}
```

# 3. 方法

## 3.1 equals
```java
@Override
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String)anObject;
        int n = value.length;
        if (n == anotherString.value.length) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = 0;
            // 比较每个字符是否相等
            while (n-- != 0) {
                if (v1[i] != v2[i])
                    return false;
                i++;
            }
            return true;
        }
    }
    return false;
}

// 忽略大小写比较两个字符串是否相等。
public boolean equalsIgnoreCase(String anotherString) {
    return (this == anotherString) ? true
            : (anotherString != null)
            && (anotherString.value.length == value.length)
            && regionMatches(true, 0, anotherString, 0, value.length);
}

// 比较两个字符串区域是否相等。要比较的 String 对象的区域从索引 toffset 开始，长度为 len。要比较的 other
// 的区域从索引 ooffset 开始，长度为 len。当 ignoreCase 为 true 时，忽略大小写进行比较。
public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
    char ta[] = value;
    int to = toffset;
    char pa[] = other.value;
    int po = ooffset;
    // 由于 toffset、ooffset 和 len 可能接近 Integer.MAX_VALUE，因此检查它们范围合法性的时候注意不能溢出
    if ((ooffset < 0) || (toffset < 0)
            || (toffset > (long)value.length - len)
            || (ooffset > (long)other.value.length - len)) {
        return false;
    }
    while (len-- > 0) {
        char c1 = ta[to++];
        char c2 = pa[po++];
        if (c1 == c2) {
            continue;
        }
        if (ignoreCase) {
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            // 不幸的是，对于格鲁吉亚（Georgian）字母表来说，转换为大写字母并不能正常工作，
            // 因为格鲁吉亚字母表的大小写转换规则很奇怪。所以我们需要再做一次检查。
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
        }
        return false;
    }
    return true;
}

// 比较两个字符串区域是否相等。要比较的 String 对象的区域从索引 toffset 开始，长度为 len。要比较的 other
// 的区域从索引 ooffset 开始，长度为 len。
public boolean regionMatches(int toffset, String other, int ooffset, int len) {
    char ta[] = value;
    int to = toffset;
    char pa[] = other.value;
    int po = ooffset;
    // 由于 toffset、ooffset 和 len 可能接近 Integer.MAX_VALUE，因此检查它们范围合法性的时候注意不能溢出
    if ((ooffset < 0) || (toffset < 0)
            || (toffset > (long)value.length - len)
            || (ooffset > (long)other.value.length - len)) {
        return false;
    }
    while (len-- > 0) {
        if (ta[to++] != pa[po++]) {
            return false;
        }
    }
    return true;
}

// 比较 String 与 CharSequence 是否有相同的字符序列。
// 请注意，如果 CharSequence 是 StringBuffer 则该方法将在其上同步。
public boolean contentEquals(CharSequence cs) {
    // 判断 cs 是不是 StringBuilder 或 StringBuffer，它们都是 AbstractStringBuilder 的子类
    if (cs instanceof AbstractStringBuilder) {
        if (cs instanceof StringBuffer) {
            // cs 是 StringBuffer 的话，就需要在 cs 上面同步
            synchronized(cs) {
                return nonSyncContentEquals((AbstractStringBuilder)cs);
            }
        } else {
            return nonSyncContentEquals((AbstractStringBuilder)cs);
        }
    }
    // 如果 cs 是字符串，直接使用 equals 方法
    if (cs instanceof String) {
        return equals(cs);
    }
    // Argument is a generic CharSequence
    char v1[] = value;
    int n = v1.length;
    if (n != cs.length()) {
        return false;
    }
    for (int i = 0; i < n; i++) {
        if (v1[i] != cs.charAt(i)) {
            return false;
        }
    }
    return true;
}

// 比较 String 与 StringBuffer 是否有相同的字符序列。
public boolean contentEquals(StringBuffer sb) {
    return contentEquals((CharSequence)sb);
}

// 比较字符串和 AbstractStringBuilder 是否相同。此方法未同步
private boolean nonSyncContentEquals(AbstractStringBuilder sb) {
    // 可以获取 AbstractStringBuilder 的底层数组，这会比 CharSequence 的方法调用快
    char v1[] = value;
    char v2[] = sb.getValue();
    int n = v1.length;
    if (n != sb.length()) {
        return false;
    }
    for (int i = 0; i < n; i++) {
        if (v1[i] != v2[i]) {
            return false;
        }
    }
    return true;
}
```

## 3.2 hashCode
```java
@Override
public int hashCode() {
    int h = hash;
    // 第一次获取哈希码时计算，之后使用缓存变量
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}
```
`hashCode`的原理参见[哈希码.md][hashCode]。

## 3.3 toString
```java
// 返回对象本身（它已经是个字符串了）
@Override
public String toString() {
    return this;
}
```

## 3.4 compare
```java
@Override
public int compareTo(String anotherString) {
    int len1 = value.length;
    int len2 = anotherString.value.length;
    int lim = Math.min(len1, len2);
    char v1[] = value;
    char v2[] = anotherString.value;

    int k = 0;
    // 比较两个字符串的公共最长前缀的字符
    while (k < lim) {
        char c1 = v1[k];
        char c2 = v2[k];
        if (c1 != c2) {
            return c1 - c2;
        }
        k++;
    }
    // 如果两个字符串的公共最长前缀相等，则谁更长谁就更大。一样长则相等
    return len1 - len2;
}

// 忽略大小写，比较两个字符串的大小。
// 此方法未考虑语言环境，并且会导致某些语言环境的排序不令人满意。java.text 包提供 Collators 进行语言环境敏感的排序。
public int compareToIgnoreCase(String str) {
    return CASE_INSENSITIVE_ORDER.compare(this, str);
}
```

## 3.5 CharSequence 方法
```java
// 返回此字符串的长度。长度等于字符串中 UTF-8 代码单元的数量。
@Override
public int length() {
    // 返回底层字符数组的长度
    return value.length;
}

// 返回指定索引处的 char 值。索引的范围是 0 到 length() - 1。对于增补字符，此方法将会返回代理。
@Override
public char charAt(int index) {
    if ((index < 0) || (index >= value.length)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return value[index];
}

// 返回一个字符序列，该字符序列是该序列的子序列。此方法和 substring(beginIndex, endIndex) 行为完全一样
@Override
public CharSequence subSequence(int beginIndex, int endIndex) {
    // 就是调用 substring
    return this.substring(beginIndex, endIndex);
}
```
`String`的`length`方法返回的是字符数组的长度，当字符串中只含有`BMP`字符时，使用此方法可以表示字符串中字符数。
如果字符串中含有增补字符，推荐使用`Character.codePointCount`方法或`String.codePointCount`方法获取字符串中字符数量。

## 3.6 代码点方法
```java
// 返回指定索引处的 Unicode 代码点。索引范围 [0, length)。
// 如果 index 处是低代理或 BMP 字符，直接返回这个 char；
// 如果 index 处是高代理，看看后面是不是低代理，是的话返回解析的代码点；不是（或超出范围）返回这个高代理
public int codePointAt(int index) {
    if ((index < 0) || (index >= value.length)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointAtImpl(value, index, value.length);
}

// 返回指定索引之前的 Unicode 代码点。索引范围 [1, length]。
// 如果 index - 1 处是高代理或 BMP 字符，直接返回这个 char；
// 如果 index - 1 处是低代理，看看 index - 2 是不是高代理，是的话返回解析的代码点；不是返回这个低代理
public int codePointBefore(int index) {
    int i = index - 1;
    if ((i < 0) || (i >= value.length)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointBeforeImpl(value, index, 0);
}

// 返回此 String 指定文本范围内的 Unicode 代码点数。范围为 [beginIndex, endIndex) 的 char。
// 因此，文本范围的长度（以 char 单位）为 endIndex-beginIndex。文本范围内的每个不成对代理都计为一个代码点。
public int codePointCount(int beginIndex, int endIndex) {
    if (beginIndex < 0 || endIndex > value.length || beginIndex > endIndex) {
        throw new IndexOutOfBoundsException();
    }
    return Character.codePointCountImpl(value, beginIndex, endIndex - beginIndex);
}

// 返回从给定 index 偏移 codePointOffset 个代码点的索引。在 index 和 codePointOffset 给定的文本范围内
// 未配对的每个代理都计为一个代码点。
public int offsetByCodePoints(int index, int codePointOffset) {
    if (index < 0 || index > value.length) {
        throw new IndexOutOfBoundsException();
    }
    return Character.offsetByCodePointsImpl(value, 0, value.length, index, codePointOffset);
}
```
可以看到，`String`的代码点相关方法都是调用了`Character`的代码点方法，可以查看[Character.md][char]获取更多信息。

## 3.7 getBytes
```java
// 使用命名的字符集将此 String 编码为字节序列，并将结果存储到新的字节数组中。
// 当给定字节在给定字符集中无效时，此方法的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
    if (charsetName == null) throw new NullPointerException();
    return StringCoding.encode(charsetName, value, 0, value.length);
}

// 使用 Charset （一般使用 StandardCharsets 定义的 Charset）将此 String 编码为字节序列，并将结果存储到新的字节数组中。
// 当给定字节在给定字符集中无效时，此方法的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public byte[] getBytes(Charset charset) {
    if (charset == null) throw new NullPointerException();
    return StringCoding.encode(charset, value, 0, value.length);
}

// 使用平台默认字符集将此 String 编码为字节序列，并将结果存储到新的字节数组中。
// 当给定字节在给定字符集中无效时，此方法的行为未定义。
// 当需要对解码过程进行更多控制时，应使用 java.nio.charset.CharsetDecoder 类。
public byte[] getBytes() {
    return StringCoding.encode(value, 0, value.length);
}

/*
将字符串 [srcBegin, srcEnd) 范围内的字符复制到字节数组 dst 的 dstBegin 处。
字符串中的每个字符都将被转型成一个字节，这就意味着这个方法只对 ASCII 码字符串正常运作。

Deprecated: 从 JDK 1.1 开始，执行此操作的首选方法是通过 getBytes() 方法，该方法使用平台的默认字符集。
*/
@Deprecated
public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
    if (srcBegin < 0) {
        throw new StringIndexOutOfBoundsException(srcBegin);
    }
    if (srcEnd > value.length) {
        throw new StringIndexOutOfBoundsException(srcEnd);
    }
    if (srcBegin > srcEnd) {
        throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
    }
    Objects.requireNonNull(dst);

    int j = dstBegin;
    int n = srcEnd;
    int i = srcBegin;
    char[] val = value;   /* avoid getfield opcode */

    while (i < n) {
        dst[j++] = (byte)val[i++];
    }
}
```
关于`avoid getfield opcode`注释的解释参见避免[getfield频繁调用.md][getfield]。

## 3.8 getChars
```java
// 将字符串 [srcBegin, srcEnd) 范围内的字符复制到 dst 的 dstBegin 处
public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
    if (srcBegin < 0) {
        throw new StringIndexOutOfBoundsException(srcBegin);
    }
    if (srcEnd > value.length) {
        throw new StringIndexOutOfBoundsException(srcEnd);
    }
    if (srcBegin > srcEnd) {
        throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
    }
    // 调用 System.arraycopy
    System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
}

// 将字符串全部字符复制到 dst 的 dstBegin 处
void getChars(char dst[], int dstBegin) {
    System.arraycopy(value, 0, dst, dstBegin, value.length);
}
```

## 3.9 toCharArray
```java
// 将此字符串转换为新的字符数组。
public char[] toCharArray() {
    // Cannot use Arrays.copyOf because of class initialization order issues
    char result[] = new char[value.length];
    System.arraycopy(value, 0, result, 0, value.length);
    return result;
}
```
注意到注释中说无法使用`Arrays.copyOf`方法，因为在调用`String.toCharArray`方法之前`Arrays`还未初始化。

如果将`toCharArray`方法中的`System.arraycopy`替换成`Arrays.copyOf`方法，将会抛出以下异常：
```java
Error occurred during initialization of VM
java.lang.NullPointerException
    at java.util.Hashtable.remove(Hashtable.java:491)
    at java.lang.System.initProperties(Native Method)
    at java.lang.System.initializeSystemClass(System.java:1166)
```
可以看到`System.initProperties`方法调用了`toCharArray`方法，所以发生了下面的操作：
1. `System.initProperties`方法初始化系统属性的时候需要使用`String`。
2. 当进行初始化时，它调用了`toCharArray`方法获取`String`的字符数组。
3. `String`调用`Arrays.copyOf`，但此时尚未加载/初始化`Arrays`。
4. 由于`System.initProperties`是`native`方法，它和普通`Java`方法不同的是，它不会进行类初始化请求，
这将导致`JVM`抛出异常并退出。

此解释来自于[StackOverflow上的一篇回答][to-char-array]。

## 3.10 valueOf
```java
// 返回对象 obj 的字符串表示。为 null 返回 "null"
public static String valueOf(Object obj) {
    return (obj == null) ? "null" : obj.toString();
}

// 返回字符数组的字符串表示。字符数组的内容被复制，它的后续修改不会影响返回的字符串。
public static String valueOf(char data[]) {
    return new String(data);
}

// 等同于 valueOf 方法
public static String copyValueOf(char data[]) {
    return new String(data);
}

// 返回字符数组的字符串表示。字符数组的内容被复制，它的后续修改不会影响返回的字符串。
// offset 参数是子数组第一个字符的索引。count 参数指定子数组的长度。
public static String valueOf(char data[], int offset, int count) {
    return new String(data, offset, count);
}

// 等同于 valueOf(char data[], int offset, int count) 方法
public static String copyValueOf(char data[], int offset, int count) {
    return new String(data, offset, count);
}

// 返回 boolean 参数的字符串表示
public static String valueOf(boolean b) {
    return b ? "true" : "false";
}

// 返回字符参数的字符串表示
public static String valueOf(char c) {
    // 将 c 包装在字符数组里，然后使用包私有构造器 String(char[] value, boolean share) 快速地构造字符串
    char data[] = {c};
    return new String(data, true);
}

// 返回 int 参数的字符串表示
public static String valueOf(int i) {
    // 直接调用 Integer.toString
    return Integer.toString(i);
}

// 返回 long 参数的字符串表示
public static String valueOf(long l) {
    // 直接调用 Long.toString
    return Long.toString(l);
}

// 返回 float 参数的字符串表示
public static String valueOf(float f) {
    // 直接调用 Float.toString
    return Float.toString(f);
}

// 返回 double 参数的字符串表示
public static String valueOf(double d) {
    // 直接调用 Double.toString
    return Double.toString(d);
}
```
当需要将字符数组转成`String`时，可以直接使用`String`构造器，`valueOf`方法只会间接调用`String`构造器；
类似的，当要将基本类型转成`String`时，也推荐使用包装器的`toString`静态方法。

## 3.11 start/end
```java
// 判断字符串是否以 prefix 开头。tooffset 是当前字符串对象的偏移下标
public boolean startsWith(String prefix, int toffset) {
    char ta[] = value;
    int to = toffset;
    char pa[] = prefix.value;
    int po = 0;
    int pc = prefix.value.length;
    // toffset 可能接近 Integer.MAX_VALUE，因此检查它的范围合法性的时候注意不能溢出
    if ((toffset < 0) || (toffset > value.length - pc)) {
        return false;
    }
    while (--pc >= 0) {
        if (ta[to++] != pa[po++]) {
            return false;
        }
    }
    return true;
}

// 判断字符串是否以 prefix 开头
public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
}

// 判断字符串是否以 suffix 结尾
public boolean endsWith(String suffix) {
    // 也就是调用了 startsWith 方法
    return startsWith(suffix, value.length - suffix.value.length);
}
```

## 3.12 indexOf
```java
/*
返回 ch 在此字符串内第一次出现的的索引，从 fromIndex 处开始搜索。

对于介于 0 到 0xFFFF（包括 0）之间的 ch 值，返回值 k 满足 (this.charAt(k) == ch) && (k >= fromIndex)
对于 ch 其他值，返回值 k 满足 (this.codePointAt(k) == ch)  && (k >= fromIndex)
在任何一种情况下，如果在 fromIndex 及之后在此字符串中没有出现此类字符，则返回 -1 。

对 fromIndex 的值没有限制。如果为负，则其效果与为零相同：可以搜索整个字符串。
如果它大于此字符串的长度，则具有与等于此字符串的长度相同的效果：返回 -1 。

所有索引均以 char 值指定，即为 String 对象 value 中的索引。
*/
public int indexOf(int ch, int fromIndex) {
    final int max = value.length;
    if (fromIndex < 0) {
        fromIndex = 0;
    } else if (fromIndex >= max) {
        return -1;
    }

    // 如果 ch 是 BMP 字符
    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        final char[] value = this.value;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == ch) {
                return i;
            }
        }
        return -1;
    } else {
        // 否则使用 indexOfSupplementary 
        return indexOfSupplementary(ch, fromIndex);
    }
}

// 处理增补字符
private int indexOfSupplementary(int ch, int fromIndex) {
    // 只有当 ch 是合法代码点时，才进行搜索。
    if (Character.isValidCodePoint(ch)) {
        final char[] value = this.value;
        final char hi = Character.highSurrogate(ch);
        final char lo = Character.lowSurrogate(ch);
        final int max = value.length - 1;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == hi && value[i + 1] == lo) {
                return i;
            }
        }
    }
    return -1;
}

public int indexOf(int ch) {
    return indexOf(ch, 0);
}

// 从指定的索引开始，返回指定子字符串首次出现在该字符串中的索引。
// 返回的索引 k 满足：k >= fromIndex  && this.startsWith(str, k)
public int indexOf(String str, int fromIndex) {
    return indexOf(value, 0, value.length, str.value, 0, str.value.length, fromIndex);
}

public int indexOf(String str) {
    return indexOf(str, 0);
}

// 在 source 字符数组中查找 target 字符串首次出现的下标。fromIndex 是开始搜索的下标。
// 结果是减去偏移量 sourceOffset 的值
static int indexOf(char[] source, int sourceOffset, int sourceCount, String target, int fromIndex) {
    return indexOf(source, sourceOffset, sourceCount,
                   target.value, 0, target.value.length,
                   fromIndex);
}

// 在 source 字符数组中查找 target 字符数组首次出现的下标。fromIndex 是开始搜索的下标
static int indexOf(char[] source, int sourceOffset, int sourceCount,
            char[] target, int targetOffset, int targetCount,
            int fromIndex) {
    if (fromIndex >= sourceCount) {
        return (targetCount == 0 ? sourceCount : -1);
    }
    if (fromIndex < 0) {
        fromIndex = 0;
    }
    if (targetCount == 0) {
        return fromIndex;
    }

    char first = target[targetOffset];
    int max = sourceOffset + (sourceCount - targetCount);

    for (int i = sourceOffset + fromIndex; i <= max; i++) {
        // 在 source 中查找 target 的第一个字符
        if (source[i] != first) {
            while (++i <= max && source[i] != first);
        }

        // 找到第一个字符之后，在判断剩余字符是否匹配。可以看出，这是暴力子字符串搜索法
        if (i <= max) {
            int j = i + 1;
            int end = j + targetCount - 1;
            for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);
            if (j == end) {
                // 找到之后，再减去偏移量
                return i - sourceOffset;
            }
        }
    }
    return -1;
}
```
`Java`中的子字符串使用了简单的暴力搜索算法。除了这种算法，还有[KMP算法][KMP]也可以应用于子字符串搜索。

## 3.13 lastIndexOf
```java
/*
返回 ch 在此字符串内最后一次出现的的索引，从 fromIndex 处开始向左搜索。

对于介于 0 到 0xFFFF（包括 0）之间的 ch 值，返回值 k 满足 (this.charAt(k) == ch) && (k <= fromIndex)
对于 ch 其他值，返回值 k 满足 (this.codePointAt(k) == ch)  && (k <= fromIndex)
在任何一种情况下，如果在 fromIndex 及之前在此字符串中没有出现此类字符，则返回 -1 。

对 fromIndex 的值没有限制。如果它大于或等于此字符串的长度，则可以搜索整个字符串。如果为负则返回 -1。

所有索引均以 char 值指定，即为 String 对象 value 中的索引。
*/
public int lastIndexOf(int ch, int fromIndex) {
    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        // 如果是 BMP 字符，则可以直接比较
        final char[] value = this.value;
        int i = Math.min(fromIndex, value.length - 1);
        for (; i >= 0; i--) {
            if (value[i] == ch) {
                return i;
            }
        }
        return -1;
    } else {
        return lastIndexOfSupplementary(ch, fromIndex);
    }
}

public int lastIndexOf(int ch) {
    return lastIndexOf(ch, value.length - 1);
}

// 处理增补字符
private int lastIndexOfSupplementary(int ch, int fromIndex) {
    if (Character.isValidCodePoint(ch)) {
        final char[] value = this.value;
        char hi = Character.highSurrogate(ch);
        char lo = Character.lowSurrogate(ch);
        // 增补字符用两个 char 表示，从 fromIndex 开始需要比较两个字符
        int i = Math.min(fromIndex, value.length - 2);
        for (; i >= 0; i--) {
            if (value[i] == hi && value[i + 1] == lo) {
                return i;
            }
        }
    }
    return -1;
}

// 从指定的索引开始，返回指定子字符串首次出现在该字符串中的索引。
// 返回的索引 k 满足：k <= fromIndex && this.startsWith(str, k)
public int lastIndexOf(String str, int fromIndex) {
    return lastIndexOf(value, 0, value.length, str.value, 0, str.value.length, fromIndex);
}

public int lastIndexOf(String str) {
    return lastIndexOf(str, value.length);
}

// 在 source 字符数组中查找 target 字符数组最后出现的下标。fromIndex 是开始搜索的下标。
// 结果是减去偏移量 sourceOffset 的值
static int lastIndexOf(char[] source, int sourceOffset, int sourceCount,
        char[] target, int targetOffset, int targetCount,
        int fromIndex) {
    /*
     * Check arguments; return immediately where possible. For
     * consistency, don't check for null str.
     */
    int rightIndex = sourceCount - targetCount;
    if (fromIndex < 0) {
        return -1;
    }
    // 从 fromIndex 开始需要先向右比较 targetCount 个字符
    if (fromIndex > rightIndex) {
        fromIndex = rightIndex;
    }
    // 空字符串总是会匹配
    if (targetCount == 0) {
        return fromIndex;
    }

    // 获取 target 匹配区域的最后一个字符 strLastChar
    int strLastIndex = targetOffset + targetCount - 1;
    char strLastChar = target[strLastIndex];
    // 因为是从 strLastChar 开始向左比较，所以左边需要有 targetCount - 1 个字符
    int min = sourceOffset + targetCount - 1;
    // fromIndex 向右有 targetCount 个字符
    int i = min + fromIndex;
    
startSearchForLastChar:
    while (true) {
        // 找到 source 中和 strLastChar 匹配的位置
        while (i >= min && source[i] != strLastChar) {
            i--;
        }
        // 左边字符数小于 targetCount - 1，将不足以比较，返回 -1
        if (i < min) {
            return -1;
        }
        int j = i - 1;
        // 找到了 source 中 strLastChar 位置 i。start 是可能匹配的子字符串的开始位置减 1
        int start = j - (targetCount - 1);
        int k = strLastIndex - 1;

        while (j > start) {
            if (source[j--] != target[k--]) {
                i--;
                // 跳过最外面循环一次，进行下一次查找
                continue startSearchForLastChar;
            }
        }
        // 减去偏移量
        return start - sourceOffset + 1;
    }
}
```
`break LABEL`会跳出指定的循环，而`continue LABEL`会跳过指定的循环一次。

## 3.14 contains
```java
// 判断当前字符串是否包含 s
public boolean contains(CharSequence s) {
    return indexOf(s.toString()) > -1;
}
```

## 3.15 大小写转换
```java
// 使用给定 Locale 的规则将此 String 所有字符转换为小写。转换后的字符串长度可能不等于原字符串长度
public String toLowerCase(Locale locale) {
    if (locale == null) {
        throw new NullPointerException();
    }

    int firstUpper;
    final int len = value.length;

    // 检查第一个可转小写字符的位置
    scan: {
        for (firstUpper = 0 ; firstUpper < len; ) {
            char c = value[firstUpper];
            // 如果 c 是在高代理部分
            if ((c >= Character.MIN_HIGH_SURROGATE)
                    && (c <= Character.MAX_HIGH_SURROGATE)) {
                // 解析增补字符。但也可能 c 后面不是低代理字符，此时 codePointAt 返回 c
                int supplChar = codePointAt(firstUpper);
                // 如果解析出的字符和它的小写形式不一致，则找到第一个可以转成小写的字符，跳出代码块
                if (supplChar != Character.toLowerCase(supplChar)) {
                    break scan;
                }
                // 如果是增补字符 + 2，否则 + 1
                firstUpper += Character.charCount(supplChar);
            } else {
                // 判断 c 和它的小写形式不一致，则找到第一个可以转成小写的字符，跳出代码块
                if (c != Character.toLowerCase(c)) {
                    break scan;
                }
                firstUpper++;
            }
        }
        // 如果所有字符都不能转成小写或已经是小写形式，则返回当前字符串
        return this;
    }

    char[] result = new char[len];
    // result 可能增长，增补字符转小写可能变成 BMP 字符。
    // resultOffset 用来记录转换后的放缩大小，i + resultOffset 就是写入的位置
    int resultOffset = 0;

    // 将前面的小写字符写入 result
    System.arraycopy(value, 0, result, 0, firstUpper);

    String lang = locale.getLanguage();
    // tr 土耳其语；az 阿塞拜疆语；lt 立陶宛语
    boolean localeDependent = (lang == "tr" || lang == "az" || lang == "lt");
    char[] lowerCharArray;
    int lowerChar;
    int srcChar;
    int srcCount;
    for (int i = firstUpper; i < len; i += srcCount) {
        // 解析字符，可能为增补字符，所以用 int
        srcChar = (int)value[i];
        if ((char)srcChar >= Character.MIN_HIGH_SURROGATE
                && (char)srcChar <= Character.MAX_HIGH_SURROGATE) {
            // 解析增补字符。但也可能 value[i] 后面不是低代理字符，此时 codePointAt 返回 value[i]
            srcChar = codePointAt(i);
            // 增补字符为 2，只有高代理部分返回 1
            srcCount = Character.charCount(srcChar);
        } else {
            srcCount = 1;
        }
        if (localeDependent ||
            srcChar == '\u03A3' || // 希腊大写字母 SIGMA
            srcChar == '\u0130') { // 上面带点的拉丁文大写字母 I
            // 有些语言和字符的小写规则特殊，需要特殊处理
            lowerChar = ConditionalSpecialCasing.toLowerCaseEx(this, i, locale);
        } else {
            lowerChar = Character.toLowerCase(srcChar);
        }
        // 如果解析的小写字符出错或者是增补字符
        if ((lowerChar == Character.ERROR)
                || (lowerChar >= Character.MIN_SUPPLEMENTARY_CODE_POINT)) {
            if (lowerChar == Character.ERROR) {
                lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(this, i, locale);
            } else if (srcCount == 2) {
                // Character.toChars 操作会写入 result。增补字符转成小写，可能会变成 BMP 字符
                resultOffset += Character.toChars(lowerChar, result, i + resultOffset) - srcCount;
                continue;
            } else {
                lowerCharArray = Character.toChars(lowerChar);
            }

            /* Grow result if needed */
            int mapLen = lowerCharArray.length;
            // 如果转换后的字符长度出现增长，则需要创建新的字符数组容纳旧的
            if (mapLen > srcCount) {
                char[] result2 = new char[result.length + mapLen - srcCount];
                System.arraycopy(result, 0, result2, 0, i + resultOffset);
                result = result2;
            }
            for (int x = 0; x < mapLen; ++x) {
                result[i + resultOffset + x] = lowerCharArray[x];
            }
            resultOffset += (mapLen - srcCount);
        } else {
            result[i + resultOffset] = (char)lowerChar;
        }
    }
    return new String(result, 0, len + resultOffset);
}

// 使用默认语言环境的规则将此 String 所有字符转换为小写。
public String toLowerCase() {
    return toLowerCase(Locale.getDefault());
}

// 使用给定 Locale 的规则将此 String 所有字符转换为大写。转换后的字符串长度可能不等于原字符串长度
public String toUpperCase(Locale locale) {
    if (locale == null) {
        throw new NullPointerException();
    }

    int firstLower;
    final int len = value.length;

    // 检查第一个可转大写字符的位置
    scan: {
        for (firstLower = 0 ; firstLower < len; ) {
            int c = (int)value[firstLower];
            int srcCount;
            // 如果 c 是在高代理部分
            if ((c >= Character.MIN_HIGH_SURROGATE)
                    && (c <= Character.MAX_HIGH_SURROGATE)) {
                // 解析增补字符。但也可能 c 后面不是低代理字符，此时 codePointAt 返回 c
                c = codePointAt(firstLower);
                // 增补字符为 2，只要高代理部分返回 1
                srcCount = Character.charCount(c);
            } else {
                srcCount = 1;
            }
            int upperCaseChar = Character.toUpperCaseEx(c);
            // 如果它的解析出错，或者判断 c 和它的大写形式不一致，则找到第一个可以转成大写的字符，跳出代码块
            if ((upperCaseChar == Character.ERROR)
                    || (c != upperCaseChar)) {
                break scan;
            }
            firstLower += srcCount;
        }
        // 如果所有字符都不能转成大写或已经是大写形式，则返回当前字符串
        return this;
    }

    // result 可能增长，增补字符转大写可能变成 BMP 字符。
    // resultOffset 用来记录转换后的放缩大小，i + resultOffset 就是写入的位置
    int resultOffset = 0;
    char[] result = new char[len];

    // 将前面的大写字符写入 result
    System.arraycopy(value, 0, result, 0, firstLower);

    String lang = locale.getLanguage();
    // tr 土耳其语；az 阿塞拜疆语；lt 立陶宛语
    boolean localeDependent = (lang == "tr" || lang == "az" || lang == "lt");
    char[] upperCharArray;
    int upperChar;
    int srcChar;
    int srcCount;
    for (int i = firstLower; i < len; i += srcCount) {
        // 解析字符，可能为增补字符，所以用 int
        srcChar = (int)value[i];
        if ((char)srcChar >= Character.MIN_HIGH_SURROGATE &&
            (char)srcChar <= Character.MAX_HIGH_SURROGATE) {
            // 解析增补字符。但也可能 value[i] 后面不是低代理字符，此时 codePointAt 返回 value[i]
            srcChar = codePointAt(i);
            // 增补字符为 2，只有高代理部分返回 1
            srcCount = Character.charCount(srcChar);
        } else {
            srcCount = 1;
        }
        if (localeDependent) {
            // 有些语言和字符的大写规则特殊，需要特殊处理
            upperChar = ConditionalSpecialCasing.toUpperCaseEx(this, i, locale);
        } else {
            upperChar = Character.toUpperCaseEx(srcChar);
        }
        // 如果解析的大写字符出错或者是增补字符
        if ((upperChar == Character.ERROR)
                || (upperChar >= Character.MIN_SUPPLEMENTARY_CODE_POINT)) {
            if (upperChar == Character.ERROR) {
                if (localeDependent) {
                    upperCharArray = ConditionalSpecialCasing.toUpperCaseCharArray(this, i, locale);
                } else {
                    upperCharArray = Character.toUpperCaseCharArray(srcChar);
                }
            } else if (srcCount == 2) {
                // Character.toChars 操作会写入 result。增补字符转成大写，可能会变成 BMP 字符
                resultOffset += Character.toChars(upperChar, result, i + resultOffset) - srcCount;
                continue;
            } else {
                upperCharArray = Character.toChars(upperChar);
            }

            int mapLen = upperCharArray.length;
            // 如果转换后的字符长度出现增长，则需要创建新的字符数组容纳旧的
            if (mapLen > srcCount) {
                char[] result2 = new char[result.length + mapLen - srcCount];
                System.arraycopy(result, 0, result2, 0, i + resultOffset);
                result = result2;
            }
            for (int x = 0; x < mapLen; ++x) {
                result[i + resultOffset + x] = upperCharArray[x];
            }
            resultOffset += (mapLen - srcCount);
        } else {
            result[i + resultOffset] = (char)upperChar;
        }
    }
    return new String(result, 0, len + resultOffset);
}

// 使用默认语言环境的规则将此 String 所有字符转换为大写。
public String toUpperCase() {
    return toUpperCase(Locale.getDefault());
}
```
之所以找到第一个符合条件的字符再继续操作，是为了当字符串中没有符合条件的字符时，不创建新的数组。

## 3.16 trim
```java
// 返回删除当前字符串中所有前导和尾随空格（以及换行、tab 和其他控制字符）的结果字符串
public String trim() {
    int len = value.length;
    int st = 0;
    char[] val = value;    /* avoid getfield opcode */

    // 在 ASCII 码中，小于空格字符代码点的都是一些换行、分隔、控制字符
    while ((st < len) && (val[st] <= ' ')) {
        st++;
    }
    while ((st < len) && (val[len - 1] <= ' ')) {
        len--;
    }
    return ((st > 0) || (len < value.length)) ? substring(st, len) : this;
}
```
在`ASCII`码中，小于空格字符代码点的都是一些换行、分隔、控制字符，详情可参见[ASCII码表][ascii]。

`Java`中没有`ltrim`、`rtrim`这样实用的方法，可以自己模仿`trim`方法写一个类似的；
也不能自己指定去掉的字符，这可以通过正则表达式`^`和`$`搭配其他模式实现。

## 3.17 substring
```java
// 截取子字符串，截取范围为 [beginIndex, endIndex)
public String substring(int beginIndex, int endIndex) {
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > value.length) {
        throw new StringIndexOutOfBoundsException(endIndex);
    }
    int subLen = endIndex - beginIndex;
    if (subLen < 0) {
        throw new StringIndexOutOfBoundsException(subLen);
    }
    // 如果截取的子字符串和源串长度相等，则直接返回源串。因为字符串是不可变对象，所以这样做没问题
    return ((beginIndex == 0) && (endIndex == value.length)) ? this
            : new String(value, beginIndex, subLen);
}

// 截取子字符串，从 beginIndex 开始截取
public String substring(int beginIndex) {
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    int subLen = value.length - beginIndex;
    if (subLen < 0) {
        throw new StringIndexOutOfBoundsException(subLen);
    }
    return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
}
```

## 3.18 concat
```java
// 将当前字符串和 str 拼接起来返回新的字符串
public String concat(String str) {
    int otherLen = str.length();
    if (otherLen == 0) {
        return this;
    }
    int len = value.length;
    // 将当前字符串的内容复制到新的数组中
    char buf[] = Arrays.copyOf(value, len + otherLen);
    // 将 str 的内容加到后面
    str.getChars(buf, len);
    // 使用 String 包私有构造器直接将 buf 数组作为 value
    return new String(buf, true);
}
```

## 3.19 format
```java
// 使用指定的语言环境、格式字符串和参数，返回格式化的字符串。
public static String format(Locale l, String format, Object... args) {
    return new Formatter(l).format(format, args).toString();
}

// 使用默认语言环境、格式字符串和参数，返回格式化的字符串。
public static String format(String format, Object... args) {
    return new Formatter().format(format, args).toString();
}
```

## 3.20 matches
```java
// 判断此字符串是否与给定的正则表达式匹配。
public boolean matches(String regex) {
    return Pattern.matches(regex, this);
}
```

## 3.21 replace
```java
// 将当前字符串中所有等于 oldChar 的字符替换为 newChar
public String replace(char oldChar, char newChar) {
    if (oldChar != newChar) {
        int len = value.length;
        int i = -1;
        char[] val = value; /* avoid getfield opcode */

        // 找到第一个等于 oldChar 的位置
        while (++i < len) {
            if (val[i] == oldChar) {
                break;
            }
        }
        if (i < len) {
            char buf[] = new char[len];
            // 将前面不等于 oldChar 的字符复制到新的 buf 中
            for (int j = 0; j < i; j++) {
                buf[j] = val[j];
            }
            // 继续替换接下来的字符
            while (i < len) {
                char c = val[i];
                buf[i] = (c == oldChar) ? newChar : c;
                i++;
            }
            return new String(buf, true);
        }
    }
    return this;
}

// 用给定的替换项替换与给定的正则表达式匹配的此字符串的第一个子字符串。
// 请注意，replacement 中的反斜杠（\）和美元符号（$）可能导致结果与 replacement 被视为文字替换字符串时的结果有所不同。
// 参见 Matcher.replaceFirst。如果需要，请使用 Matcher.quoteReplacement 取消这些字符的特殊含义
public String replaceFirst(String regex, String replacement) {
    return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
}

// 用给定的替换项替换该字符串中与给定的正则表达式匹配的每个子字符串。
// 请注意，replacement 中的反斜杠（\）和美元符号（$）可能导致结果与 replacement 被视为文字替换字符串时的结果有所不同。
// 参见 Matcher.replaceAll。如果需要，请使用 Matcher.quoteReplacement 取消这些字符的特殊含义
public String replaceAll(String regex, String replacement) {
    return Pattern.compile(regex).matcher(this).replaceAll(replacement);
}

// 用 replacement 替换该字符串中与 target 匹配的每个子字符串。替换从字符串的开头到结尾进行，例如，
// 在字符串“aaa”中用“b”替换“aa”将得到“ba”而不是“ab”。
public String replace(CharSequence target, CharSequence replacement) {
    return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
            this).replaceAll(Matcher.quoteReplacement(replacement.toString()));
}
```

## 3.22 split
```java
/*
使用给定正则表达式的匹配项切分当前字符串。

此方法返回的数组包含当前字符串的每个子字符串，该子字符串由给定表达式终止或由当前字符串的结尾终止。
数组中的子字符串按它们在当前字符串中出现的顺序排列。
 - 如果表达式与输入的任何部分都不匹配，则结果数组只有一个元素，即当前字符串。
 - 如果当前字符串的开头和 regex 匹配，则在结果数组的开头将包含一个空的字符串。

limit 参数控制应用 regex 的次数，因此会影响所得数组的长度。
 - 如果 limit 大于 0，则将最多应用 limit - 1 次 regex，该数组的长度将小于等于 limit，
并且该数组的最后一个条目将包含除最后一次应用 regex 匹配的字符之后的所有字符。
 - 如果 limit 小于 0，则将尽可能多地应用 regex，并且数组可以具有任何长度。
 - 如果 limit 等于 0，则该模式将被尽可能多地应用，该数组可以具有任何长度，并且尾随的所有空字符串将被丢弃。
*/
public String[] split(String regex, int limit) {
    /* fastpath 如果 regex 是以下情况
     (1)只有一个字符，并且这个字符不是正则表达式元字符".$|()[{^?*+\\"之一
     (2)只有两个字符，并且第一个字符是反斜杠"\\"，第二个字符不是 ASCII 数字或字母。
        也就是 regex 是个正则表达式转义字符或普通字符，regex 和第二个字符等同
     */
    char ch = 0;
    // 判断是不是 fastpath，fastpath 可以不使用正则表达式
    if ((
         (regex.value.length == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
         (regex.length() == 2 &&
          regex.charAt(0) == '\\' &&
          // 使用括号表达式同时赋值和运算；使用 | 运算符判断是否在范围内
          (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
          ((ch-'a')|('z'-ch)) < 0 &&
          ((ch-'A')|('Z'-ch)) < 0)
        ) &&
        // ch 不在代理部分
        (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE))
    {
        // 如果 regex 只有一个字符，ch 等于那个字符；如果 regex 只有两个字符，ch 等于第二个字符
        int off = 0;  // 偏移量
        int next = 0;  // 字符串中下一个 ch 位置
        boolean limited = limit > 0;  // 是否限制切分次数
        ArrayList<String> list = new ArrayList<>();
        while ((next = indexOf(ch, off)) != -1) {
            if (!limited || list.size() < limit - 1) {
                // 如果不限制切分次数或子字符串数量小于 limit - 1，则将切分的子字符串加入到 list 中
                list.add(substring(off, next));
                off = next + 1;
            } else {
                // 将剩余字符作为最后一个子字符串，让 off 等于当前字符串长度
                // 此时 list 长度为 limit
                list.add(substring(off, value.length));
                off = value.length;
                break;
            }
        }
        // 如果没有任何匹配，返回当前字符串
        if (off == 0)
            return new String[]{this};

        // 有可能当前字符串中匹配 ch 的字符不足 limit - 1，此时将剩余字符添加到 list 中
        if (!limited || list.size() < limit)
            list.add(substring(off, value.length));

        int resultSize = list.size();
        if (limit == 0) {
            // limit 等于 0，将会去掉尾随的所有空字符串
            while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                resultSize--;
            }
        }
        String[] result = new String[resultSize];
        return list.subList(0, resultSize).toArray(result);
    }
    // 其他情况使用正则表达式对象
    return Pattern.compile(regex).split(this, limit);
}

public String[] split(String regex) {
    return split(regex, 0);
}
```

## 3.23 join
```java
// 返回一个新的 String，该字符串使用 delimiter 作为连接符，将 elements 连接起来。
// 此方法于 Java8 加入
public static String join(CharSequence delimiter, CharSequence... elements) {
    Objects.requireNonNull(delimiter);
    Objects.requireNonNull(elements);
    // Number of elements not likely worth Arrays.stream overhead.
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence cs: elements) {
        joiner.add(cs);
    }
    return joiner.toString();
}

// 返回一个新的 String，该字符串使用 delimiter 作为连接符，将 elements 连接起来。
// 此方法于 Java8 加入
public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
    Objects.requireNonNull(delimiter);
    Objects.requireNonNull(elements);
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence cs: elements) {
        joiner.add(cs);
    }
    return joiner.toString();
}
```
<!-- TODO: 为所有 Java8 添加的方法增加注释 -->

## 3.24 intern
```java
/*
返回字符串对象的规范表示。也就是与该字符串具有相同内容的字符串，但保证来自全局字符串常量池。

调用 intern 方法时，如果池已经包含与当前 String 对象内容相同的字符串，则返回池中的字符串。
否则，将当前 String 对象添加到池中，并返回当前 String 对象的引用。

所有字符串字面量和字符串值常量表达式均已在字符串池中。
*/
public native String intern();
```
全局字符串常量池的内容参见[常量池.md][const-pool]。此方法的测试参见[StringTest.java][test]。

# 4. 内部类/接口/枚举

## 4.1 CaseInsensitiveComparator
```java
/*
一个忽略大小写的比较器，该比较器是可序列化的。

请注意，该比较器未考虑语言环境，并且会导致某些语言环境的排序不令人满意。java.text 包提供了 Collators
以允许对语言环境敏感的排序。
*/
private static class CaseInsensitiveComparator implements Comparator<String>, java.io.Serializable {
    // use serialVersionUID from JDK 1.2.2 for interoperability
    private static final long serialVersionUID = 8575799808933029326L;

    public int compare(String s1, String s2) {
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) {
                // 当 c1 和 c2 不相等时，比较它们的大写和小写形式。
                // 之所以大写和小写都比较，是为了防止在某些语言环境下出错。参见 3.1 节。
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        // No overflow because of numeric promotion
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    }

    /** Replaces the de-serialized object. */
    private Object readResolve() { return CASE_INSENSITIVE_ORDER; }
}
```
`readResolve`方法是为了防止单例模式被破坏，参见[这个链接][read-resolve]。


[charset]: 字符集编码.md
[hashCode]: 哈希码.md
[char]: Character.md
[getfield]: 避免getfield频繁调用.md
[to-char-array]: https://stackoverflow.com/questions/49715328/why-doesnt-string-tochararray-use-arrays-copyof
[KMP]: https://www.zhihu.com/question/21923021/answer/1032665486
[ascii]: http://c.biancheng.net/c/ascii/
[read-resolve]: https://blog.csdn.net/huangbiao86/article/details/6896565
[const-pool]: 常量池.md
[test]: ../../../test/java_/lang/StringTest.java