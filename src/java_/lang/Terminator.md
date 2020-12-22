`java.lang.Terminator`类的声明如下：
```java
class Terminator
```
包私有的实用程序类，用于为终止时触发的关闭操作设置和删除特定于平台的支持。

有关关闭操作参见[Shutdown.md][shutdown]。

# 1. 实现
```java
import sun.misc.Signal;
import sun.misc.SignalHandler;

class Terminator {

    private static SignalHandler handler = null;

    /* 设置和取消的调用已在 Shutdown 上同步，因此此处不需要进一步同步 */

    static void setup() {
        if (handler != null) return;
        SignalHandler sh = new SignalHandler() {
            public void handle(Signal sig) {
                Shutdown.exit(sig.getNumber() + 0200);  // 0200 是 8 进制数字，也就是 10 进制数 128
            }
        };
        handler = sh;

        // 指定 -Xrs 参数时，用户负责通过调用 System.exit() 确保关闭钩子运行
        // -Xrs 参数会禁用 JVM 处理任何内部或外部生成的信号（如 SIGSEGV 和 SIGABRT）。
        // 引发的任何信号都由操作系统默认处理程序处理。在 JVM 中禁用信号处理会降低大约 2-4% 的性能，具体取决于应用程序。
        try {
            // 响应 INT 信号，由 handler 来处理
            Signal.handle(new Signal("INT"), sh);
        } catch (IllegalArgumentException e) {
        }
        try {
            // 响应 TERM 信号，由 handler 来处理
            Signal.handle(new Signal("TERM"), sh);
        } catch (IllegalArgumentException e) {
        }
    }

    static void teardown() {
        /* 当前 sun.misc.Signal 不支持取消处理程序 */
    }

}
```


[shutdown]: Shutdown.md