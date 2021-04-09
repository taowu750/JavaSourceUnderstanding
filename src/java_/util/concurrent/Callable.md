`java.util.concurrent.Callable` 接口代码如下：
```java
@FunctionalInterface
public interface Callable<V> {
    
    // 计算一个结果，可能过程中会抛出异常
    V call() throws Exception;
}
```
一个返回结果并可能抛出异常的任务，定义了一个没有参数的单一方法，称为 `call`。

`Callable` 接口与 `Runnable` 类似，两者都是为实例可能被另一个线程执行的类而设计的，但 `Runnable` 不返回结果，也不能抛出受检的异常。

`Executors` 类包含了一些实用方法，用于从其他常见形式转换为 `Callable` 类。