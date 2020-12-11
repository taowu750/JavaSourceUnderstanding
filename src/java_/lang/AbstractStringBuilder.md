`java.lang.AbstractStringBuilder`抽象类的声明如下：
```java
abstract class AbstractStringBuilder implements Appendable, CharSequence
```
一个可变的字符序列。它在任何时间点都包含一些特定的字符序列，但是可以通过某些方法调用来更改序列的长度和内容。
除非另有说明，否则将`null`参数传递给此类中的构造函数或方法将导致引发`NullPointerException`。

`AbstractStringBuilder`有两个实现类，就是我们熟悉的`StringBuilder`和`StringBuffer`。

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

## 3.2 CharSequence 方法
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

## 3.3 Appendable 方法
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

## 3.4 capacity 方法
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
// 计算 CharSequence 中 index 处的代码点，索引范围 [0, count)。
// 如果 index 处是低代理或 BMP 字符，直接返回这个 char；
// 如果 index 处是高代理，看看后面是不是低代理，是的话返回解析的代码点；不是（或超出范围）返回这个高代理
public int codePointAt(int index) {
    if ((index < 0) || (index >= count)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointAtImpl(value, index, count);
}

// 计算 CharSequence 中 index 前一处代码点，索引范围 [1, count]。
// 如果 index - 1 处是高代理或 BMP 字符，直接返回这个 char；
// 如果 index - 1 处是低代理，看看 index - 2 是不是高代理，是的话返回解析的代码点；不是（或超出范围）返回这个低代理
public int codePointBefore(int index) {
    int i = index - 1;
    if ((i < 0) || (i >= count)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointBeforeImpl(value, index, 0);
}
```