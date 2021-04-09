`java.util.concurrent.ExecutorService` 接口的声明如下：
```java
public interface ExecutorService extends Executor
```
一个 `Executor`，它提供了管理终止的方法和可以产生一个 `Future` 的方法，用于跟踪一个或多个异步任务的进度。

一个 `ExecutorService` 可以被关闭，这将导致它拒绝新的任务。为关闭一个 `ExecutorService` 提供了两种不同的方法。
`shutdown` 方法将允许之前提交的任务在终止之前执行，而 `shutdownNow` 方法则会阻止等待的任务启动，并试图停止当前正在执行的任务。
终止后，一个执行器没有正在执行的任务，没有等待执行的任务，也不能提交新的任务。一个不被使用的 `ExecutorService` 应该被关闭，
以便回收其资源。

方法 `submit` 扩展了父类方法 `Executor.execute(Runnable)`，创建并返回一个 `Future`，该 `Future` 可用于取消执行和/或等待完成。
方法 `invokeAny` 和 `invokeAll` 执行最常用的批量执行形式，执行一个任务集合，然后等待至少一个或全部任务完成。
类 `ExecutorCompletionService` 可以用来编写这些方法的自定义变体。`Executors` 类为本包中提供的执行器服务提供了工厂方法。

下面是一个网络服务的例子，线程池中的线程为传入的请求提供服务。它使用了预先配置的 `Executors.newFixedThreadPool` 工厂方法：
```java
class NetworkService implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;

    public NetworkService(int port, int poolSize) throws IOException {
        serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(poolSize);
    }

    public void run() { // run the service
        try {
            for (;;) {
                pool.execute(new Handler(serverSocket.accept()));
            }
        } catch (IOException ex) {
            pool.shutdown();
        }
    }
}

class Handler implements Runnable {
    private final Socket socket;
    
    Handler(Socket socket) { this.socket = socket; }
    
    public void run() {
        // read and service request on socket
    }
}
```

下面的方法分两个阶段关闭一个 `ExecutorService`，首先调用 `shutdown` 来拒绝传入的任务，然后在必要时调用 `shutdownNow` 来取消任何滞留的任务。：
```java
void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown(); // 拒绝新的任务
    try {
        // 等待 60 秒让现有的任务完成
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            pool.shutdownNow(); // 取消仍在执行的任务
            // 等待一段时间后，任务才会响应取消操作。
            if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                System.err.println("Pool did not terminate");
        }
    } catch (InterruptedException ie) {
        // 如果当前线程也被中断，则再次进行取消操作
        pool.shutdownNow();
        // 保存中断状态
        Thread.currentThread().interrupt();
    }
}
```

内存一致性：在一个线程中，在将一个 `Runnable` 或 `Callable` 任务提交给一个 `ExecutorService` 之前，
该线程中的行为 happens-before 在该任务所做的任何行为之前，而这些行为又 happens-before 通过 `Future.get()` 检索结果。

# 1. 方法

## 1.1 submit
```java
/**
 * 提交一个有返回值的任务供执行，并返回一个 Future，代表任务的待处理结果。Future 的 get 方法将在成功完成后返回任务的结果。
 * 
 * 如果你想立即阻塞以等待任务完成，你可以使用 result = exec.submit(aCallable).get() 这种形式的构造。
 * 
 * 注意：Executors 类包括一组方法，可以将其他一些常见的类似闭包的对象，例如 java.security.PrivilegedAction 转换为 Callable 形式，
 * 这样就可以被 submit 方法使用。
 */
<T> Future<T> submit(Callable<T> task);

// 提交一个 Runnable 任务供执行，并返回一个代表该任务的 Future。Future 的 get 方法将在成功完成后返回给定的 result。
<T> Future<T> submit(Runnable task, T result);

// 提交一个 Runnable 任务供执行，并返回一个代表该任务的 Future。Future 的 get 方法将在成功完成后返回 null。
Future<?> submit(Runnable task);
```

## 1.2 invokeAll
```java
/**
 * 执行给定的任务，当所有任务完成时，返回一个持有其状态和结果的 Future 列表。Future.isDone 对返回列表中的每个元素都为真。
 * 
 * 请注意，一个已完成的任务可以正常终止，也可以通过抛出异常终止。如果给定的集合在此操作进行时被修改，则此方法的结果是未定义的。
 */
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

/**
 * 执行给定的任务，返回一个持有其状态的 Futures 列表，并在全部完成或超时后返回结果，以先发生者为准。
 * Future.isDone 对返回列表中的每个元素都是 true。返回后，未完成的任务将被取消。
 * 
 * 请注意，一个已完成的任务可以正常终止，也可以通过抛出异常终止。如果给定的集合在此操作进行时被修改，则此方法的结果是未定义的。
 */
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit)
        throws InterruptedException;
```

## 1.3 invokeAny
```java
/**
 * 执行给定的任务，如果有任务成功完成（即没有抛出异常），则返回其中一个任务的结果。正常或异常返回后，未完成的任务将被取消。
 * 
 * 如果给定的集合在此操作进行时被修改，则此方法的结果未被定义。
 */
<T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

/**
 * 执行给定的任务，如果在给定的超时时间之前有任何任务成功完成（即没有抛出异常），则返回其中一个任务的结果。
 * 正常或异常返回后，未完成的任务将被取消。
 * 
 * 如果给定的集合在此操作进行时被修改，则此方法的结果未被定义。
 */
<T> T invokeAny(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
```

## 1.4 shutdown
```java
// 启动有序关闭，在此过程中，先前提交的任务将被执行，但不会接受新任务。如果已经关闭，调用不会产生额外的效果。
void shutdown();

/**
 * 尝试停止所有执行的任务，和等待的任务，并返回等待执行的任务列表。
 * 本方法不会等待执行的任务终止。可以使用 awaitTermination 来做到这一点。
 * 
 * 此方法会尽力尝试停止执行的任务。例如，典型的实现会通过 Thread.interrupt 中断，
 * 因此任何未能响应中断的任务可能永远不会终止。
 */
List<Runnable> shutdownNow();
```

## 1.5 awaitTermination
```java
// 在进行关闭后，阻塞直到所有任务完成执行、或发生超时、或当前线程被中断，以先发生者为准。
boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
```

## 1.6 状态查询
```java
// 如果 Executor 已经关闭，则返回 true
boolean isShutdown();

// 如果关闭后所有任务都已完成，则返回 true。请注意，除非先调用 shutdown 或 shutdownNow，否则 isTerminated 永远不会为真。
boolean isTerminated();
```