`java.lang.Number`抽象类，下面是它的声明：
```java
public abstract class Number implements java.io.Serializable
```
这个抽象类规定了一系列`xxxValue()`方法，其中`xxx`表示各种基本数字类型。
除了`byteValue()`方法调用了`intValue()`方法外，其余的都是抽象方法。

可以看出，这个抽象类表示一个可以转换为基本数字类型的类。