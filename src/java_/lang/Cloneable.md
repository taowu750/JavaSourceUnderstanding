`java.lang.Cloneable`接口如下所示：
```java
public interface Cloneable {
}
```
`Cloneable`接口是一个标记接口。一个类实现`Cloneable`接口，指示`Object.clone()`方法为该类的实例进行逐域复制是合法的。
在未实现`Cloneable`接口的实例上调用`Object`的`clone`方法会导致抛出`CloneNotSupportedException`异常。

按照约定，实现此接口的类应使用公共方法重写`Object.clone`方法。有关重写此方法的详细信息和实例，
请参见 [Object.md][object]。

请注意，此接口不包含`clone`方法。因此，不可能仅仅因为实现了这个接口就可以克隆对象。即使克隆方法使用反射调用，也不能保证它会成功。


[object]: Object.md