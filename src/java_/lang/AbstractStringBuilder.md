`java.lang.AbstractStringBuilder`抽象类的声明如下：
```java
abstract class AbstractStringBuilder implements Appendable, CharSequence
```
一个可变的字符序列。它在任何时间点都包含一些特定的字符序列，但是可以通过某些方法调用来更改序列的长度和内容。
除非另有说明，否则将`null`参数传递给此类中的构造函数或方法将导致引发`NullPointerException`。

`AbstractStringBuilder`有两个实现类，就是我们熟悉的`StringBuilder`和`StringBuffer`。
`AbstractStringBuilder`自己是个包私有抽象类，这就可以给`java.lang`包内的其他类提供更高效的操作。

# 1. 成员字段

## 1.1 底层表示
```java
// 用来存储字符的数组
char[] value;

// 字符数组中实际的 char 数量
int count;
```
这两个字段都是包私有的，供包内其他类快速访问。

## 1.2 MAX_ARRAY_SIZE
```java
// 可以分配的最大数组大小。一些虚拟机可能在数组中保留一些头字。
// 需要注意的是，它不是 value 数组的最大长度限制。
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

# 2. 构造器/块

## 2.1 无参构造器
```java
AbstractStringBuilder() {
}
```
这个无参构造器对于序列化子类是必要的。

## 2.1 指定容量
```java
// 创建一个指定容量的 AbstractStringBuilder 对象。注意，此方法未对 capacity 做任何验证
AbstractStringBuilder(int capacity) {
    value = new char[capacity];
}
```

# 3. 方法

## 3.1 toString
```java
// 返回表示此序列中数据的字符串。分配并初始化一个新的 String 对象，以包含该对象当前表示的字符序列。然后返回此 String。
// 对该序列的后续更改不会影响 String 的内容。
@Override
public abstract String toString();
```
注意到这个方法是`abstract`的，这将强制其实现子类实现这个方法。

## 3.2 capacity 方法
```java
// 返回当前容量。容量是可用于插入字符的存储量，超过该容量将进行分配。
public int capacity() {
    // 底层字符数组的长度
    return value.length;
}

/*
确保容量至少等于指定的最小值 minimumCapacity。如果当前容量小于该参数，那么将分配一个具有更大容量的新内部数组。
新容量是以下中的较大者：
 - minimumCapacity 参数。
 - 原来容量的两倍，再加上 2 。

如果 minimumCapacity 参数为非正数，则此方法不执行任何操作，仅返回。请注意，
对该对象进行的后续操作可能会将实际容量减少到此处要求的以下值。
*/
public void ensureCapacity(int minimumCapacity) {
    if (minimumCapacity > 0)
        ensureCapacityInternal(minimumCapacity);
}

// 对于正值 minimumCapacity，此方法的行为类似于 ensureCapacity。
// 如果 minimumCapacity 由于数字溢出而为非正数，则此方法将抛出 OutOfMemoryError 。
private void ensureCapacityInternal(int minimumCapacity) {
    // minimumCapacity 为负值时，minimumCapacity - value.length 可能会因为溢出大于 0
    if (minimumCapacity - value.length > 0) {
        // 给 value 分配新的空间，并将旧值复制到新的空间中 
        value = Arrays.copyOf(value, newCapacity(minimumCapacity));
    }
}

// 如果没有溢出，返回当前容量乘 2 加 2 和 minCapacity 中的较大值。
// 否则，如果 minCapacity 小于 0，抛出 OutOfMemoryError；反之返回大于等于 MAX_ARRAY_SIZE 的容量
private int newCapacity(int minCapacity) {
    // overflow-conscious code
    // 新的容量是原来容量的 2 倍加 2
    int newCapacity = (value.length << 1) + 2;
    // 1. 当两个数都是正值时，使用 newCapacity、minCapacity 中的较大值
    // 2. newCapacity 可能因为溢出变成负值
    //  - 如果 minCapacity 为正值
    //      - 两者相减可能为负值，因此 newCapacity 等于 minCapacity，最终为正
    //      - 两者相减可能因为溢出为正值，因此 newCapacity 不变，最终为负
    //  - 如果 minCapacity 为负值，两者相减为正值，因此 newCapacity 不变，最终为负
    //  - 如果 minCapacity 为 0，两者相减为负值，因此 newCapacity 不变，最终为负
    // 3. newCapacity 可能因为溢出变为 0（当 value.length 等于 Integer.MAX_VALUE 就会发生这种情况）
    //  - 如果 minCapacity 为正值，两者相减为负值，因此 newCapacity 等于 minCapacity，最终为正
    //  - 如果 minCapacity 为负值，两者相减为正值，因此 newCapacity 不变，最终为 0
    //  - 如果 minCapacity 为 0，两者相减为 0，最终为 0
    if (newCapacity - minCapacity < 0) {
        newCapacity = minCapacity;
    }
    // 通过上面的分析，newCapacity 可能小于等于 0

    // 当 newCapacity 小于等于 0 或大于 MAX_ARRAY_SIZE 时，调用 hugeCapacity(minCapacity)。
    // 此时如果 minCapacity 小于 0 抛出异常，否则返回大于等于 MAX_ARRAY_SIZE 的值
    return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
        ? hugeCapacity(minCapacity)
        : newCapacity;
}

// 当 minCapacity 小于 0 时抛出异常。如果 minCapacity 小于等于 MAX_ARRAY_SIZE，返回 MAX_ARRAY_SIZE；
// 否则返回 minCapacity
private int hugeCapacity(int minCapacity) {
    // 如果 minCapacity 小于 0 时，抛出 OutOfMemoryError
    if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
        throw new OutOfMemoryError();
    }
    // 如果 minCapacity 大于 MAX_ARRAY_SIZE 时，返回 minCapacity。这表明 MAX_ARRAY_SIZE 并不是数组最大长度的限制
    return (minCapacity > MAX_ARRAY_SIZE) ? minCapacity : MAX_ARRAY_SIZE;
}
```
参见 [overflow-conscious code.md][overflow]。

## 3.3 CharSequence 方法
```java
// 当前由该对象表示的字符序列的长度，也就是 char 的数量。
@Override
public int length() {
    return count;
}

// 返回此序列指定索引 index 处的 char 值，这个 index 也就是底层字符数组下标。
@Override
public char charAt(int index) {
    if ((index < 0) || (index >= count))
        throw new StringIndexOutOfBoundsException(index);
    return value[index];
}

// 返回一个新的字符序列，范围是 [start, end)。它和 substring(start, end) 完全相同。
@Override
public CharSequence subSequence(int start, int end) {
    return substring(start, end);
}
```

## 3.4 Appendable 方法
```java
// 将 char c 追加到此序列，然后返回此序列。
@Override
public AbstractStringBuilder append(char c) {
    ensureCapacityInternal(count + 1);
    value[count++] = c;
    return this;
}

// 将指定 CharSequence 序列 s 的子序列追加到当前序列。其中子序列的范围是 [start, end)。
// 如果 s 为 null ，则此方法将附加 "null"。
@Override
public AbstractStringBuilder append(CharSequence s, int start, int end) {
    if (s == null)
        s = "null";
    if ((start < 0) || (start > end) || (end > s.length()))
        throw new IndexOutOfBoundsException("start " + start + ", end " + end + ", s.length() " + s.length());
    int len = end - start;
    ensureCapacityInternal(count + len);
    for (int i = start, j = count; i < end; i++, j++)
        value[j] = s.charAt(i);
    count += len;
    return this;
}

// 将指定 CharSequence 序列 s 追加到当前序列。此方法将由于子类同步的差异，子类中的文档不一样
@Override
public AbstractStringBuilder append(CharSequence s) {
    // 对于 null、String 和其他 AbstractStringBuilder 的情况分别处理，提高性能
    if (s == null)
        return appendNull();
    if (s instanceof String)
        return this.append((String)s);
    if (s instanceof AbstractStringBuilder)
        return this.append((AbstractStringBuilder)s);

    // 添加其他 CharSequence
    return this.append(s, 0, s.length());
}

// 快速添加 "null"
private AbstractStringBuilder appendNull() {
    int c = count;
    ensureCapacityInternal(c + 4);
    final char[] value = this.value;
    // 不使用 charAt 添加更快
    value[c++] = 'n';
    value[c++] = 'u';
    value[c++] = 'l';
    value[c++] = 'l';
    count = c;
    return this;
}
```
和`AbstractStringBuilder`规范不同的是，这些`Appendable`实现方法在添加`null`时不会抛出异常，
而是添加`"null"`字符串。

## 3.5 trimToSize
```java
// 如果容量大于实际字符数量，则将容量缩减为实际字符数量。
// 调用此方法可能会影响随后对 capacity() 方法的调用返回的值。
public void trimToSize() {
    if (count < value.length) {
        value = Arrays.copyOf(value, count);
    }
}
```

## 3.6 setLength
```java
/*
设置字符序列的长度。

如果 newLength 小于 0，抛出异常。
如果 newLength 小于 count，不会发生任何事
如果 newLength 大于 count，数组或进行扩容，类似于 ensureCapacity，并将大于 count 的部分值设为 '\0'
*/
public void setLength(int newLength) {
    if (newLength < 0)
        throw new StringIndexOutOfBoundsException(newLength);
    ensureCapacityInternal(newLength);

    if (count < newLength) {
        Arrays.fill(value, count, newLength, '\0');
    }

    count = newLength;
}
```

## 3.7 codePoint 方法
```java
// 计算当前对象中 index 处的代码点，索引范围 [0, count)。
// 如果 index 处是低代理或 BMP 字符，直接返回这个 char；
// 如果 index 处是高代理，看看后面是不是低代理，是的话返回解析的代码点；不是（或超出范围）返回这个高代理
public int codePointAt(int index) {
    if ((index < 0) || (index >= count)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointAtImpl(value, index, count);
}

// 计算当前对象中 index 前一处代码点，索引范围 [1, count]。
// 如果 index - 1 处是高代理或 BMP 字符，直接返回这个 char；
// 如果 index - 1 处是低代理，看看 index - 2 是不是高代理，是的话返回解析的代码点；不是（或超出范围）返回这个低代理
public int codePointBefore(int index) {
    int i = index - 1;
    if ((i < 0) || (i >= count)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointBeforeImpl(value, index, 0);
}

// 计算当前对象中代码点的数量，范围为 [beginIndex, endIndex)。文本范围内的每个不成对代理都计为一个代码点。
public int codePointCount(int beginIndex, int endIndex) {
    if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
        throw new IndexOutOfBoundsException();
    }
    return Character.codePointCountImpl(value, beginIndex, endIndex-beginIndex);
}

// 返回从 index 处偏移 codePointOffset 个代码点的索引。codePointOffset 为正表示向右，为负表示向左。
// 文本范围内的每个不成对代理都计为一个代码点。
public int offsetByCodePoints(int index, int codePointOffset) {
    if (index < 0 || index > count) {
        throw new IndexOutOfBoundsException();
    }
    return Character.offsetByCodePointsImpl(value, 0, count, index, codePointOffset);
}
```
可以看到，`AbstractStringBuilder`的代码点相关方法都是调用了`Character`的代码点方法，
可以查看[Character.md][char]获取更多信息。

## 3.8 indexOf 和 lastIndex
```java
// 从指定的索引开始，返回指定子字符串首次出现在当前对象中的索引。
// 返回的索引 k 满足：k >= fromIndex  && this.toString().startsWith(str, k)
// 如果 str 不存在，则返回 -1
public int indexOf(String str, int fromIndex) {
    return String.indexOf(value, 0, count, str, fromIndex);
}

public int indexOf(String str) {
    return indexOf(str, 0);
}

// 从指定的索引开始，返回指定子字符串首次出现在当前对象中的索引。
// 返回的索引 k 满足：k <= fromIndex && this.startsWith(str, k)
// 如果 str 不存在，则返回 -1
public int lastIndexOf(String str, int fromIndex) {
    return String.lastIndexOf(value, 0, count, str, fromIndex);
}

public int lastIndexOf(String str) {
    return lastIndexOf(str, count);
}
```
`AbstractStringBuilder`的`index`相关方法使用了`String`的`index`方法，可以查看[String.md][string]获取更多信息。

## 3.9 getChars
```java
// 将当前对象 [srcBegin, srcEnd) 的字符复制到 dst 数组的 dstBegin 处
public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    if (srcBegin < 0)
        throw new StringIndexOutOfBoundsException(srcBegin);
    if ((srcEnd < 0) || (srcEnd > count))
        throw new StringIndexOutOfBoundsException(srcEnd);
    if (srcBegin > srcEnd)
        throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
    System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
}
```

## 3.9 getValue
```java
// 这个方法被 String.contentEquals 方法使用，为了加快速度
final char[] getValue() {
    return value;
}
```

## 3.10 setCharAt
```java
// 将指定索引处的字符设置为 ch 
public void setCharAt(int index, char ch) {
    if ((index < 0) || (index >= count))
        throw new StringIndexOutOfBoundsException(index);
    value[index] = ch;
}
```

## 3.11 replace
```java
// 将 [start, end) 处的字符替换成 str。如果 end 大于 count，则令 end 等于 count。
public AbstractStringBuilder replace(int start, int end, String str) {
    if (start < 0)
        throw new StringIndexOutOfBoundsException(start);
    if (start > count)
        throw new StringIndexOutOfBoundsException("start > length()");
    if (start > end)
        throw new StringIndexOutOfBoundsException("start > end");

    // 如果 end 大于 count，则令 end 等于 count
    if (end > count)
        end = count;
    int len = str.length();
    // 新的字符数量是原来字符数量加上 str 长度减去替换字符数量
    int newCount = count + len - (end - start);
    ensureCapacityInternal(newCount);

    // 将被替换字符序列后的所有字符复制到新的数组结尾（newCount）
    System.arraycopy(value, end, value, start + len, count - end);
    // 将 str 内容复制到指定位置
    str.getChars(value, start);
    count = newCount;
    return this;
}
```

## 3.12 substring
```java
// 使用当前对象中 [start, end) 范围内的字符构造 String
public String substring(int start, int end) {
    if (start < 0)
        throw new StringIndexOutOfBoundsException(start);
    if (end > count)
        throw new StringIndexOutOfBoundsException(end);
    if (start > end)
        throw new StringIndexOutOfBoundsException(end - start);
    return new String(value, start, end - start);
}

// 使用当前对象中 [start, count) 范围内的字符构造 String
public String substring(int start) {
    return substring(start, count);
}
```

## 3.13 append
```java
// 将指定的字符串附加到此字符序列。如果 str 为 null ，则附加四个字符 "null" 。
public AbstractStringBuilder append(String str) {
    if (str == null)
        return appendNull();
    int len = str.length();
    ensureCapacityInternal(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
}

// 将 obj 的字符串表示附加到此字符序列。如果 obj 为 null ，则附加四个字符 "null"。
public AbstractStringBuilder append(Object obj) {
    return append(String.valueOf(obj));
}

// 此方法于 1.8 加入。
// 将 asb 附加到此字符序列。如果 asb 为 null ，则附加四个字符 "null"。
AbstractStringBuilder append(AbstractStringBuilder asb) {
    if (asb == null)
        return appendNull();
    int len = asb.length();
    ensureCapacityInternal(count + len);
    asb.getChars(0, len, value, count);
    count += len;
    return this;
}

// 将 sb 附加到此字符序列。如果 sb 为 null ，则附加四个字符 "null"。
// 注意此方法并未同步，而它在子类中的行为也不同。
public AbstractStringBuilder append(StringBuffer sb) {
    if (sb == null)
        return appendNull();
    int len = sb.length();
    ensureCapacityInternal(count + len);
    sb.getChars(0, len, value, count);
    count += len;
    return this;
}

// 将 str 中从索引 offset 开始的 len 个字符按顺序附加到此序列的内容中。
public AbstractStringBuilder append(char str[], int offset, int len) {
    if (len > 0)                // let arraycopy report AIOOBE for len < 0
        ensureCapacityInternal(count + len);
    System.arraycopy(str, offset, value, count, len);
    count += len;
    return this;
}

// 将 str 按顺序附加到此序列的内容中。
public AbstractStringBuilder append(char[] str) {
    int len = str.length;
    ensureCapacityInternal(count + len);
    System.arraycopy(str, 0, value, count, len);
    count += len;
    return this;
}

// 将 b 添加到序列中。true 添加 "true"，false 添加 "false"
public AbstractStringBuilder append(boolean b) {
    if (b) {
        ensureCapacityInternal(count + 4);
        value[count++] = 't';
        value[count++] = 'r';
        value[count++] = 'u';
        value[count++] = 'e';
    } else {
        ensureCapacityInternal(count + 5);
        value[count++] = 'f';
        value[count++] = 'a';
        value[count++] = 'l';
        value[count++] = 's';
        value[count++] = 'e';
    }
    return this;
}

// 将整数 i 的 10 进制字符串形式添加到序列中
public AbstractStringBuilder append(int i) {
    // 对 Integer.MIN_VALUE 单独处理，因为 Integer.getChars 方法不能处理 Integer.MIN_VALUE
    if (i == Integer.MIN_VALUE) {
        append("-2147483648");
        return this;
    }
    int appendedLength = (i < 0) ? Integer.stringSize(-i) + 1
                                 : Integer.stringSize(i);
    int spaceNeeded = count + appendedLength;
    ensureCapacityInternal(spaceNeeded);
    // 使用 Integer.getChars 方法将整数 i 快速转为字符序列并写入 value 中
    Integer.getChars(i, spaceNeeded, value);
    count = spaceNeeded;
    return this;
}

// 将长整数 i 的 10 进制字符串形式添加到序列中
public AbstractStringBuilder append(long l) {
    // 对 Long.MIN_VALUE 单独处理，因为 Long.getChars 方法不能处理 Long.MIN_VALUE
    if (l == Long.MIN_VALUE) {
        append("-9223372036854775808");
        return this;
    }
    int appendedLength = (l < 0) ? Long.stringSize(-l) + 1
                                 : Long.stringSize(l);
    int spaceNeeded = count + appendedLength;
    ensureCapacityInternal(spaceNeeded);
    // 使用 Long.getChars 方法将长整数 i 快速转为字符序列并写入 value 中
    Long.getChars(l, spaceNeeded, value);
    count = spaceNeeded;
    return this;
}

// 将浮点数 f 的字符串形式添加到序列中。参见 Float.md-2.6-toString
public AbstractStringBuilder append(float f) {
    FloatingDecimal.appendTo(f,this);
    return this;
}

// 将浮点数 d 的字符串形式添加到序列中。参见 Double.md-2.6-toString
public AbstractStringBuilder append(double d) {
    FloatingDecimal.appendTo(d,this);
    return this;
}
```

## 3.14 insert
```java
// 将 str 插入到 offset 处，offset 范围 [0, count]。如果 str 为 null，则插入四个字符 "null" 。
public AbstractStringBuilder insert(int offset, String str) {
    if ((offset < 0) || (offset > length()))
        throw new StringIndexOutOfBoundsException(offset);
    if (str == null)
        str = "null";
    int len = str.length();
    ensureCapacityInternal(count + len);
    System.arraycopy(value, offset, value, offset + len, count - offset);
    str.getChars(value, offset);
    count += len;
    return this;
}

// 将 str 插入到 offset 处，offset 范围 [0, count]。
public AbstractStringBuilder insert(int offset, char[] str) {
    if ((offset < 0) || (offset > length()))
        throw new StringIndexOutOfBoundsException(offset);
    int len = str.length;
    ensureCapacityInternal(count + len);
    System.arraycopy(value, offset, value, offset + len, count - offset);
    System.arraycopy(str, 0, value, offset, len);
    count += len;
    return this;
}

// 将 str 的子数组插入到当前对象的 index 处，index 范围 [0, count]。子数组从 offset 开始，长度为 len
public AbstractStringBuilder insert(int index, char[] str, int offset,
                                        int len)
{
    if ((index < 0) || (index > length()))
        throw new StringIndexOutOfBoundsException(index);
    if ((offset < 0) || (len < 0) || (offset > str.length - len))
        throw new StringIndexOutOfBoundsException(
            "offset " + offset + ", len " + len + ", str.length "
            + str.length);
    ensureCapacityInternal(count + len);
    System.arraycopy(value, index, value, index + len, count - index);
    System.arraycopy(str, offset, value, index, len);
    count += len;
    return this;
}

// 将 str 插入到 offset 处，offset 范围 [0, count]。
public AbstractStringBuilder insert(int offset, char[] str) {
    if ((offset < 0) || (offset > length()))
        throw new StringIndexOutOfBoundsException(offset);
    int len = str.length;
    ensureCapacityInternal(count + len);
    System.arraycopy(value, offset, value, offset + len, count - offset);
    System.arraycopy(str, 0, value, offset, len);
    count += len;
    return this;
}

// 将 s [start, end) 范围内的字符插入到当前对象的 dstOffset 处。如果 str 为 null，则将其视为 "null" 。
public AbstractStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
    if (s == null)
        s = "null";
    if ((dstOffset < 0) || (dstOffset > this.length()))
        throw new IndexOutOfBoundsException("dstOffset "+dstOffset);
    if ((start < 0) || (end < 0) || (start > end) || (end > s.length()))
        throw new IndexOutOfBoundsException(
             "start " + start + ", end " + end + ", s.length() " + s.length());
    int len = end - start;
    ensureCapacityInternal(count + len);
    System.arraycopy(value, dstOffset, value, dstOffset + len, count - dstOffset);
    for (int i=start; i<end; i++)
        value[dstOffset++] = s.charAt(i);
    count += len;
    return this;
}

// 将 s 插入到当前对象的 dstOffset 处。如果 str 为 null，则将其视为 "null" 。
public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
    if (s == null)
        s = "null";
    // 转成 String 可以使用 System.arraycopy，速度更快
    if (s instanceof String)
        return this.insert(dstOffset, (String)s);
    return this.insert(dstOffset, s, 0, s.length());
}

// 将 b 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, boolean b) {
    return insert(offset, String.valueOf(b));
}

// 将 c 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, char c) {
    ensureCapacityInternal(count + 1);
    System.arraycopy(value, offset, value, offset + 1, count - offset);
    value[offset] = c;
    count += 1;
    return this;
}

// 将 i 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, int i) {
    return insert(offset, String.valueOf(i));
}

// 将 l 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, long l) {
    return insert(offset, String.valueOf(l));
}

// 将 f 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, float f) {
    return insert(offset, String.valueOf(f));
}

// 将 d 插入到当前对象的 offset 处。
public AbstractStringBuilder insert(int offset, double d) {
    return insert(offset, String.valueOf(d));
}
```

## 3.15 delete
```java
// 将当前对象 [start, end) 的字符删除
public AbstractStringBuilder delete(int start, int end) {
    if (start < 0)
        throw new StringIndexOutOfBoundsException(start);
    if (end > count)
        end = count;
    if (start > end)
        throw new StringIndexOutOfBoundsException();
    int len = end - start;
    if (len > 0) {
        System.arraycopy(value, start+len, value, start, count-end);
        count -= len;
    }
    return this;
}

// 将 index 处的字符删除
public AbstractStringBuilder deleteCharAt(int index) {
    if ((index < 0) || (index >= count))
        throw new StringIndexOutOfBoundsException(index);
    System.arraycopy(value, index+1, value, index, count-index-1);
    count--;
    return this;
}
```

## 3.16 reverse
```java
public AbstractStringBuilder reverse() {
    boolean hasSurrogates = false;
    int n = count - 1;
    // 把数组分为两半，交换左半边和右半边的数据
    for (int j = (n-1) >> 1; j >= 0; j--) {
        // j 从左半部分的最右边开始，k 从右半部分的最左边开始
        int k = n - j;
        char cj = value[j];
        char ck = value[k];
        value[j] = ck;
        value[k] = cj;
        // 如果有代理字符，记录一下
        if (Character.isSurrogate(cj) ||
            Character.isSurrogate(ck)) {
            hasSurrogates = true;
        }
    }
    if (hasSurrogates) {
        // 数组中有代理字符，使用 reverseAllValidSurrogatePairs 方法
        reverseAllValidSurrogatePairs();
    }
    return this;
}

// 调整颠倒的代理
private void reverseAllValidSurrogatePairs() {
    for (int i = 0; i < count - 1; i++) {
        // 之前的 reverse 操作会将高代理和低代理部分位置颠倒，在这里调整回来
        char c2 = value[i];
        if (Character.isLowSurrogate(c2)) {
            char c1 = value[i + 1];
            if (Character.isHighSurrogate(c1)) {
                value[i++] = c1;
                value[i] = c2;
            }
        }
    }
}
```


[char]: Character.md
[string]: String.md
[overflow]: ../util/overflow-conscious%20code.md