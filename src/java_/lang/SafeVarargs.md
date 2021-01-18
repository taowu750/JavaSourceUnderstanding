`java.lang.SafeVarargs`注解如下：
```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SafeVarargs {}
```
带有`@SafeVarargs`注解的方法或构造函数的不会对其可变参数执行潜在的不安全操作。
将此注解应用于方法或构造函数可抑制警告。

除了其`@Target`元注解所施加的使用限制外，编译器还会对`@SafeVarargs`实施额外的使用限制。
如果用`@SafeVarargs`注解对方法或构造函数进行注解，并且是以下情况，将产生编译时错误：
 - 声明是固定参数的方法或构造函数
 - 声明是一个可变参数方法，当它既不是`static`的也不是`final`的。