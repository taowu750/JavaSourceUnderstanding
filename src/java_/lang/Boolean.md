java.lang.Boolean类，下面是它的声明:
```java
public final class Boolean implements java.io.Serializable, Comparable<Boolean>
```
`Boolean`类是基本类型`boolean`的包装器类。

使用`Boolean`类时可能会有[装箱拆箱操作][box]。

# 1. 属性

## 1.1 TYPE
```java
public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean")
```
`Class.getPrimitiveClass()`方法是一个`native`方法，专门用来获取基本类型的`Class`对象。
需要注意的是，`boolean.class`等于`Boolean.TYPE`，但是`boolean.class`不等于`Boolean.class`。

# 2. 方法

## 2.1 parseBoolean()
```java
    public static boolean parseBoolean(String s) {
        return ((s != null) && s.equalsIgnoreCase("true"));
    }
```
`parseBoolean`方法只在参数等于`"true"`的情况下返回`true`。

## 2.2 getBoolean()
```java
public static boolean getBoolean(String name) {
    boolean result = false;
    try {
        result = parseBoolean(System.getProperty(name));
    } catch (IllegalArgumentException | NullPointerException e) {
    }
    return result;
}
```
参数`name`是系统属性名称，当这个属性值不为`true`或不存在时返回`false`，否则返回`true`


# 3. 要点

## 3.1 生成Boolean对象
要生成一个`Boolean`对象，最好使用`Boolean.valueof()`方法，这会直接返回`Boolean`中预定义的`TRUE`或者`FALSE`对象，
而不会重新生成一个`Boolean`对象。将`true`和`false`赋值给`Boolean`引用时，也会自动调用`Boolean.valueof()`方法。

## 3.2 hashCode()
`Boolean`的`hashCode()`源码如下：
```java
@Override
public int hashCode() {
    return Boolean.hashCode(value);
}

public static int hashCode(boolean value) {
    return value ? 1231 : 1237;
}
```
[原始文章链接][article]

`1231`和`1237`是两个比较大的素数(质数)，实际上这里采用其它任意两个较大的质数也是可以的。[使用质数的原因是为了hashCode的质量][hashCode]。
而采用1231和1237更大程度的原因是Boolean的作者个人爱好(看到这句别打我)。

为什么会是1231和1237,`Boolean`只有`true`和`false`两个值,为什么不能是3或者7,或者其它的素数?

诚然,`Boolean`只有`true`和`false`两个值,理论上任何两个素数都可以.但是在实际使用时,
可能作为key的不只是`Boolean`一种类型,比如最常见的字符串作为key,
还有`Integer`作为key.至少要保证避开常见`hashCode`取值范围.
但是太大了也没意义,比如说字符串`"00"`的`hashCode`为1536,`Boolean`的`hashCode`取值太大的话,
指不定又跟字符串的`hashCode`撞上了,更别说其它对象的了.


[box]: 自动装箱与拆箱.md
[article]: https://blog.csdn.net/qq_21251983/article/details/52164403
[hashCode]: 哈希算法.md