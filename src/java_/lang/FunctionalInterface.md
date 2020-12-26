`java.lang.FunctionalInterface`注解如下：
```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionalInterface {}
```
用于指示接口类型作为`Java`语言规范中定义的功能接口。从概念上讲，功能接口仅具有一种抽象方法。由于默认方法具有实现，
它们不是抽象的，因此功能接口中可以有多种默认方法。如果接口声明的抽象方法覆盖了`java.lang.Object`的公共方法之一，
则该方法也不会计入接口的抽象方法计数。

此接口的最大用处就是用在`lambda`表达式、方法引用或构造函数引用中，来简化代码，使流程更加清晰。