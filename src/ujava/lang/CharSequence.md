`java.lang.CharSequence`接口的声明如下：
```java
public interface CharSequence
```
这个接口表示`char`值的**只读**序列，此接口对许多不同种类的`char`序列提供统一的只读访问。
此接口不修改`equals`和`hashCode`方法的常规协定，因此通常未定义比较两个实现了`CharSequence`的对象的结果。
它有几个实现类：`CharBuffer`、`String`、`StringBuffer`、`StringBuilder`。

在Java8之后，`CharSequence`多了两个默认方法：`chars()`和`codePoints()`。
它们返回`IntStream`，分别产生字符和代码点流。