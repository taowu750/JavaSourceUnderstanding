`java.util.concurrent.RunnableFuture` 接口的代码如下：
```java
public interface RunnableFuture<V> extends Runnable, Future<V> {
    void run();
}
```
`Runnable` 和 `Future` 的组合接口。`run` 方法的成功执行将导致 `Future` 的完成，并允许访问其结果。