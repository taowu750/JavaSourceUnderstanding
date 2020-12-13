`java.lang.StringBuffer`类的声明如下：
```java
public final class StringBuffer
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
```
一个线程安全的可变字符序列。它在任何时间点都包含一些特定的字符序列，但是可以通过某些方法调用来更改序列的长度和内容。
`StringBuffer`的方法在必要时进行同步。

此类是`AbstractStringBuilder`的实现子类。`StringBuffer`和它的父类大部分方法实现一致，
只是使用协变返回类型将父类方法中的`AbstractStringBuilder`返回值替换为了`StringBuffer`，以及保证了方法的同步性。
详细情况参见[AbstractStringBuilder.md][abstract-string-builder]。

# 1. 成员字段

## 1.1 toStringCache
```java
// toString 方法的缓存。每当修改 StringBuffer 时清除。它是 transient，不会被序列化
private transient char[] toStringCache;
```

## 1.2 serialPersistentFields
```java
// StringBuffer 的可序列化字段。
private static final java.io.ObjectStreamField[] serialPersistentFields =
{
    // 此 StringBuffer 的底层字符数组
    new java.io.ObjectStreamField("value", char[].class),
    // 此 StringBuffer 的实际字符数量
    new java.io.ObjectStreamField("count", Integer.TYPE),
    // 指示 value 数组是否共享的标志。反序列化时忽略该值。
    new java.io.ObjectStreamField("shared", Boolean.TYPE),
};
```

# 2. 方法

## 2.1 toString
```java
@Override
public synchronized String toString() {
    if (toStringCache == null) {
        toStringCache = Arrays.copyOfRange(value, 0, count);
    }
    // 使用 String 的包私有构造器将 toStringCache 作为了 String 的底层数组
    return new String(toStringCache, true);
}
```
之所以使用`toStringCache`，而不直接使用`String(char value[], int offset, int count)`构造器，
是因为可能会有多个线程使用`toString`方法。使用`toStringCache`配合`String`包私有构造器就只需要复制一次数组，
达到了优化性能的效果。

而在`StringBuilder`中没有使用`toStringCache`优化，是因为`StringBuilder`的典型应用场景是拼接字符串，返回`String`对象后，
就不再使用。还有一个更重要的原因是：`String`必须是个不可变对象，而`StringBuilder`可能被误用在多线程中。
如果`StringBuilder`也采用`toStringCache`加上`String`包私有构造器的话，那么就可能会出现构造的`String`不一致的情况，
所以`StringBuilder`不能和`String`共享数组，这是不安全的方式。<sup id="a1">[\[1\]](#f1)</sup>

## 2.2 从父类继承的方法

`StringBuffer`绝大多数方法直接使用了`AbstractStringBuilder`的实现，只是如下面的方法一样加上了`synchronized`同步修饰符，
并改变了返回类型为自身：
```java
@Override
public synchronized StringBuffer append(String str) {
    toStringCache = null;
    super.append(str);
    return this;
}
```
此外，对于会修改`StringBuffer`的方法，还会将`toStringCache`置为`null`，以保证`toString`方法会生成`StringBuffer`最新的字符串。

## 2.3 必要时同步方法

查看`StringBuffer`的源码可以看到下面的方法：
```java
@Override
public StringBuffer insert(int dstOffset, CharSequence s) {
    super.insert(dstOffset, s);
    return this;
}
```
可以看到，此方法既没有使用`synchornized`修饰，也没有将`toStringCache`置为`null`，那是不是这个方法实现的有问题呢？

我们可以查看`insert`方法中`super.insert(dstOffset, s)`的实现：
```java
public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
    if (s == null)
        s = "null";
    if (s instanceof String)
        return this.insert(dstOffset, (String)s);
    return this.insert(dstOffset, s, 0, s.length());
}
```
可以看到，此方法针对特定类型调用了不同的方法，而`StringBuffer`的`insert(int offset, String str)`和
`insert(int dstOffset, CharSequence s, int start, int end)`方法都是同步且将`toStringCache`置为`null`的。
这也就是`StringBuffer`在**必要时进行同步**的意思。

## 2.4 序列化方法
```java
private synchronized void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    java.io.ObjectOutputStream.PutField fields = s.putFields();
    fields.put("value", value);
    fields.put("count", count);
    fields.put("shared", false);
    s.writeFields();
}

private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    java.io.ObjectInputStream.GetField fields = s.readFields();
    value = (char[])fields.get("value", null);
    count = fields.get("count", 0);
}
```
<!-- TODO: 弄懂 ObjectStreamField 和序列化的关系 -->


[abstract-string-builder]: AbstractStringBuilder.md

<b id="f1">\[1\]</b> 参考 https://stackoverflow.com/questions/46294579/why-stringbuffer-has-a-tostringcache-while-stringbuilder-not。 [↩](#a1)