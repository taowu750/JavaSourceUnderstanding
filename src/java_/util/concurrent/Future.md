`java.util.concurrent.Future` 接口声明如下：
```java
public interface Future<V>
```
`Future` 表示异步计算的结果。我们提供了一些方法来检查计算是否完成，等待它的完成，以及检索计算的结果。只有当计算完成后，
才能使用方法 `get` 检索结果，必要时会阻塞直到计算准备好。

取消由 `cancel` 方法执行。我们提供了额外的方法来确定任务是正常完成还是被取消。一旦计算完成，计算就不能被取消。
如果你想为了可取消性而使用 `Future`，但不提供可用的结果，你可以声明 `Future<?>` 形式的类型，这样结果将返回 `null`。

# 1. 方法

## 1.1 get
```java
// 必要时等待计算完成，然后返回结果。
V get() throws InterruptedException, ExecutionException;

// 必要时最多等待给定的时间完成计算，然后返回结果（如果有）。
V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
```

## 1.2 cancel
```java
/**
 * 尝试取消该任务的执行。如果任务已经完成，已经被取消，或者由于其他原因无法取消，则该尝试将失败。
 * 
 * 如果成功，并且在调用 cancel 时这个任务还没有启动，那么这个任务就不会被运行。
 * 如果任务已经启动，那么 mayInterruptIfRunning 参数决定是否应该中断执行这个任务的线程，以尝试停止任务。
 * 
 * 该方法返回后，后续对 isDone 的调用将始终返回 true。如果此方法返回 true，后续对 isCancelled 的调用将始终返回 true。
 */
boolean cancel(boolean mayInterruptIfRunning);
```

## 1.3 状态查询
```java
// 如果该任务在正常完成前被取消，则返回true。
boolean isCancelled();

// 如果这个任务完成，返回 true。完成的原因可能是正常终止、异常或取消--在所有这些情况下，本方法将返回 true。
boolean isDone();
```