`sun.nio.ch.Interruptible` 接口如下：
```java
public interface Interruptible {

    public void interrupt(Thread t);

}
```
这个接口表示可以响应在 I/O 操作中阻塞的线程的中断信号，也就是 `Thread.interrupt()` 方法。