`java.lang.Process`抽象类的声明如下：
```java
public abstract class Process
```
使用`ProcessBuilder.start()`和`Runtime.exec`方法创建本地进程，并返回`Process`子类的实例，
该实例可用于控制该进程并获取有关该进程的信息。 `Process`类提供了一些方法，用于执行来自流程的输入，执行至流程的输出，
等待流程完成，检查流程的退出状态以及销毁（杀死）流程。

创建进程的方法可能不适用于某些平台上的特殊进程，例如本地窗口进程，守护进程，Microsoft Windows 上的 Win16/DOS进程或 Shell 脚本。

默认情况下，创建的子进程没有自己的终端或控制台。其所有标准 I/O（即`stdin`，`stdout`，`stderr`）操作都将重定向到父进程，
在此可以通过使用`getOutputStream()`，`getInputStream()`和`getErrorStream()`方法获得的流来访问它们。
父流程使用这些流将输入馈入子流程并从子流程获取输出。由于某些平台仅为标准输入和输出流提供了有限的缓冲区大小，
因此未能及时写入子流程的输入流或读取子流程的输出流可能导致子流程阻塞甚至死锁。

如果需要，还可以使用`ProcessBuilder`类的方法来重定向子流程 I/O。当不再有对`Process`对象的引用时，子`Process`不会被杀死，
而会继续异步执行。不要求由`Process`对象表示的进程对拥有`Process`对象的`Java`进程异步或并发地执行。
从`Java5`开始，`ProcessBuilder.start()`是创建`Process`的首选方法。

有关进程的概念和与线程的关系，参见[进程和线程.md][pt]。

# 1. 方法

## 1.1 getInputStream
```java
/*
返回连接到子进程常规输出的输入流。该流从此 Process 对象的标准输出中获取通过管道传输的数据。

如果已使用 ProcessBuilder.redirectOutput 重定向了子进程的标准输出，则此方法将返回空输入流。
否则，如果已使用 ProcessBuilder.redirectErrorStream 重定向了子进程的标准错误，则此方法返回的输入流
将一并接收子进程的标准输出和标准错误。

最好对返回的输入流进行缓冲。
*/
public abstract InputStream getInputStream();
```

## 1.2 getOutputStream
```java
/*
返回连接到子进程的常规输入的输出流。流的输出将通过管道传递到此 Process 的标准输入中。

如果已使用 ProcessBuilder.redirectInput 重定向了子进程的标准输入，则此方法将返回空输出流。

最好对返回的输出流进行缓冲。
*/
public abstract OutputStream getOutputStream();
```

## 1.3 getErrorStream
```java
/*
返回连接到子进程的错误输出的输入流。流从该 Process对象的错误输出中获取通过管道传输的数据。

如果已使用 ProcessBuilder.redirectError 或 ProcessBuilder.redirectErrorStream 重定向了子进程的标准错误，
则此方法将返回空输入流。
*/
public abstract InputStream getErrorStream();
```

## 1.4 exitValue
```java
/*
返回子进程的退出值。按照惯例，值 0 表示正常终止。

如果子进程未退出，抛出 IllegalThreadStateException。
*/
public abstract int exitValue();
```

## 1.5 waitFor
```java
/*
如有必要，使当前线程等待，直到此 Process 对象表示的进程终止。如果子进程已经终止，则此方法立即返回。
如果子进程尚未终止，则调用线程将被阻塞，直到子进程退出。

@return 此 Process 对象表示的子进程的退出值。按照惯例，值 0 表示正常终止。
*/
public abstract int waitFor() throws InterruptedException;

/*
如有必要，使当前线程等待，直到此 Process 对象表示的进程终止或经过指定的等待时间为止。
如果子进程已经终止，则此方法立即返回 true 值。如果进程尚未终止，并且超时值小于或等于零，则此方法立即返回 false 值。

此方法的默认实现会轮询 exitValue 以检查进程是否已终止。强烈建议使用此类的具体实现，以更有效的实现覆盖此方法。
*/
public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    long startTime = System.nanoTime();
    // 剩余等待时间，以纳秒为单位
    long rem = unit.toNanos(timeout);

    do {
        try {
            // 检查退出值
            exitValue();
            return true;
        } catch(IllegalThreadStateException ex) {
            // 抛出异常表示子进程未退出
            if (rem > 0)
                // 让当前线程至少等待 100 毫秒
                Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
        }
        // 计算新的等待时间
        rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
    } while (rem > 0);
    return false;
}
```

## 1.6 isAlive
```java
/*
测试此 Process 代表的子进程是否处于活动状态。
*/
public boolean isAlive() {
    try {
        exitValue();
        return false;
    } catch(IllegalThreadStateException e) {
        return true;
    }
}
```

## 1.7 destroy
```java
// 杀死子进程。但是否强制终止取决于实现。
public abstract void destroy();

/*
强制终止子进程。此方法的默认实现调用 destroy，因此可能不会强行终止该进程。强烈建议此类的具体实现用兼容的实现重写此方法。
在由 ProcessBuilder.start 和 Runtime.exec 返回的 Process 对象上调用此方法将强制终止该进程。

注意：子进程可能不会立即终止。即 isAlive() 可能在调用 destroyForcibly() 之后的短时间内返回 true。
如果需要，可以将此方法链接到 waitFor()。
*/
public Process destroyForcibly() {
    destroy();
    return this;
}
```


[pt]: 进程和线程.md