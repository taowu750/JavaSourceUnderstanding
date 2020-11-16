java.lang.Boolean类，下面是它的声明:
```java
public final class Boolean implements java.io.Serializable, Comparable<Boolean>
```

## 1. 属性

### 1.1 TYPE
```java
public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean")
```
这个属性等同于类字面常量`Boolean.class`，其中`Class.getPrimitiveClass()`方法是一个native方法，
专门用来获取基本类型的`Class`对象。

## 2. 方法

### 2.1 getBoolean()
```java
public static boolean getBoolean(String name)
```
参数`name`是系统属性名称，当这个属性值不为`true`或不存在时返回`false`，否则返回`true`


## 3. 要点

### 3.1 生成Boolean对象
要生成一个`Boolean`对象，最好使用`Boolean.valueof(b)`方法，这会直接返回`Boolean`中预定义的`TRUE`或者`FALSE`对象，
而不会重新生成一个`Boolean`对象。

### 3.2 hashCode()
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
[原始文章链接][hashCode]

`1231`和`1237`是两个比较大的素数(质数)，实际上这里采用其它任意两个较大的质数也是可以的。
而采用1231和1237更大程度的原因是Boolean的作者个人爱好(看到这句别打我)。

`HaspMap`中使用的hash表下标计算公式是：`(n - 1) & hash`，[其中`n`是2的幂][surplus]。
`n`是hash表的长度，这个式子等同于`hash % n`。我们需要尽可能保证`hashCode`不重复。

Java中散列算法的结果等价于下面的等式：
> s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]

其中`n`是因子个数，`s[i]`是第`i`个参与散列计算的因子。可以看到，它们都乘以了**31**这个素数。
之所以使用素数，是因为[素数在做取模运算时，余数的个数是最多的][prime]。
上面的hash的计算表达式里相当于每项都有了素数，那么`hash % n`时也就近似相当于素数对n取模，
这个时候余数也就会尽可能的多。

既然素数越大越好，素数又那么多，为什么要选择31？这个结论是个数学上的结论，但是实际上，
我们写程序又不能选择太大的素数毕竟`hashCode`的值为`int`类型，计算结果不能溢出。
之所以选择31，一是因为计算机计算31比较快（可以直接采用位移操作得到 1<<5-1），
二是因为大多数情况下我们都是采用`String`作为key，而这又是英语国家写出来的语言，
曾有人对超过5W个英文单词做了测试,在常量取31情况下,碰撞的次数都不超过7次。

为什么会是1231和1237,`Boolean`只有`true`和`false`两个值,为什么不能是3或者7,或者其它的素数?

诚然,`Boolean`只有`true`和`false`两个值,理论上任何两个素数都可以.但是在实际使用时,
可能作为key的不只是`Boolean`一种类型,比如最常见的字符串作为key,
还有`Integer`作为key.至少要保证避开常见`hashCode`取值范围.
但是太大了也没意义,比如说字符串`"00"`的`hashCode`为1536,`Boolean`的`hashCode`取值太大的话,
指不定又跟字符串的`hashCode`撞上了,更别说其它对象的了.


[surplus]: https://www.jianshu.com/p/0711e9eb8cef 
[hashCode]: https://blog.csdn.net/qq_21251983/article/details/52164403
[prime]: https://blog.csdn.net/afei__/article/details/83010897