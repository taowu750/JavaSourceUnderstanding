`java.util.concurrent.Executor` 接口代码如下所示：
```java
public interface Executor {
    
    // 在未来的某个时间执行给定的 command。该 command 可能在新的线程、池线程或调用线程中执行，由 Executor 实现决定。
    void execute(Runnable command);
}
```
一个执行提交的 `Runnable` 任务的对象。这个接口提供了一种方法，将任务提交与每个任务如何运行的机制解耦，包括线程使用、调度等细节。

通常使用 `Executor` 来代替显式创建线程。例如，您可以使用以下方法，而不是为一组任务中的每个任务调用 `new Thread(new(RunnableTask())).start()`：
```java
Executor executor = anExecutor;
executor.execute(new RunnableTask1());
executor.execute(new RunnableTask2());
...
```

然而，`Executor` 接口并不严格要求执行是异步的。在最简单的情况下，执行器可以在调用者的线程中立即运行提交的任务：
```java
class DirectExecutor implements Executor {
    public void execute(Runnable r) {
        r.run();
    }
}
```

更典型的是，任务是在调用者线程之外的某个线程中执行的。下面的执行器为每个任务生成一个新的线程：
```java
class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}
```

许多 `Executor` 实现都对任务的调度方式和时间进行了某种限制。下面的执行器将任务的提交序列化到第二个执行器，构造了一个复合执行器：
```java
class SerialExecutor implements Executor {
    
    final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
    final Executor executor;
    Runnable active;

    SerialExecutor(Executor executor) {
        this.executor = executor;
    }

    public synchronized void execute(final Runnable r) {
        tasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }
    
    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            executor.execute(active);
        }
    }
}
```

本包中提供的 `Executor` 实现实现了 `ExecutorService`，这是一个更广泛的接口。`ThreadPoolExecutor` 类提供了一个可扩展的线程池实现。
`Executors` 类为这些 `Executor` 提供了方便的工厂方法。

内存一致性效果：线程在提交 `Runnable` 对象给 `Executor` 之前的操作 happens-before 在它的执行开始之前。
