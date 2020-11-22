`java.lang.annotation.Native`注解代码如下：
```java
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Native {
}
```
这是一个应用在成员字段上，并且只在源码级别保留的注解。被它注解的字段需要是一个常量，这个常量可能会从`native`代码中引用。
这个可以被生成`native`头文件的工具作为提示，以确定是否需要头文件，如果需要，它应该包含哪些声明。