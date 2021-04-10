`java.util.concurrent.AbstractExecutorService` 抽象类的声明如下：
```java
public abstract class AbstractExecutorService implements ExecutorService
```
提供 `ExecutorService` 的默认实现。该类使用 `newTaskFor` 返回的 `RunnableFuture` 对象来实现 `submit`、`invokeAny` 和 `invokeAll` 方法，
这个对象默认为本包中提供的 `FutureTask` 类。例如，`submit(Runnable)`的实现创建了一个相关联的 `RunnableFuture`，该方法被执行并返回。
子类可以重写 `newTaskFor` 方法来返回除 `FutureTask` 以外的 `RunnableFuture` 实现。

下面是一个扩展示例，使用 `CustomTask` 类而不是默认的 `FutureTask`：
```java
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {

    static class CustomTask<V> implements RunnableFuture<V> {...}

    protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
        return new CustomTask<V>(c);
    }
   
    protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
        return new CustomTask<V>(r, v);
    }
    // ... add constructors, etc.
 }
```

参见 [ExecutorService][service] 接口。

# 1. 方法

## 1.1 newTaskFor
```java
// 返回给定 Runnable 和默认返回值的 RunnableFuture。
protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new FutureTask<T>(runnable, value);
}

// 返回给定 Callable 的 RunnableFuture。
protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new FutureTask<T>(callable);
}
```
参见 [FutureTask][future-task]。

## 1.2 submit
```java
public Future<?> submit(Runnable task) {
    if (task == null) throw new NullPointerException();
    
    // 将 Runnable 包装成 RunnableFuture
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
}

public <T> Future<T> submit(Runnable task, T result) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task, result);
    execute(ftask);
    return ftask;
}

public <T> Future<T> submit(Callable<T> task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task);
    execute(ftask);
    return ftask;
}
```

## 1.3 invokeAll
```java
public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    if (tasks == null)
        throw new NullPointerException();
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
        // 将所有 Callable 包装成 RunnableFuture，然后依次调用 execute 执行
        for (Callable<T> t : tasks) {
            RunnableFuture<T> f = newTaskFor(t);
            futures.add(f);
            execute(f);
        }
        // 对每个 future 依次调用 get，等待它们完成，并忽略抛出的异常
        for (int i = 0, size = futures.size(); i < size; i++) {
            Future<T> f = futures.get(i);
            if (!f.isDone()) {
                try {
                    f.get();
                } catch (CancellationException ignore) {
                } catch (ExecutionException ignore) {
                }
            }
        }
        // 所有任务完成后，返回 futures
        done = true;
        return futures;
    } finally {
        // 如果有任务还未完成，则取消这些未完成的任务
        if (!done)
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
    }
}

public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
    if (tasks == null)
        throw new NullPointerException();
    long nanos = unit.toNanos(timeout);
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
        // 先将所有 Callable 包装成 RunnableFuture
        for (Callable<T> t : tasks)
            futures.add(newTaskFor(t));

        final long deadline = System.nanoTime() + nanos;
        final int size = futures.size();

        // 交叉进行时间检查和 execute，以防执行程序没有任何并行性或并行性太高。
        for (int i = 0; i < size; i++) {
            execute((Runnable)futures.get(i));
            nanos = deadline - System.nanoTime();
            // 如果超时时间已到，返回 futures（此时一些 future 可能来不及执行）
            if (nanos <= 0L)
                return futures;
        }

        // 此时所有任务都被调用了 execute。在他们上面循环调用 get
        for (int i = 0; i < size; i++) {
            Future<T> f = futures.get(i);
            if (!f.isDone()) {
                // 如果超时时间已到，返回 futures（此时一些 future 可能没有完成）
                if (nanos <= 0L)
                    return futures;
                // 调用 get，并且忽略异常
                try {
                    // 任务的超时时间设为当前剩余时间
                    f.get(nanos, TimeUnit.NANOSECONDS);
                } catch (CancellationException ignore) {
                } catch (ExecutionException ignore) {
                } catch (TimeoutException toe) {
                    return futures;
                }
                // 更新剩余时间
                nanos = deadline - System.nanoTime();
            }
        }
        done = true;
        return futures;
    } finally {
        // 如果有任务还未完成（超时时间内未执行），则取消这些未完成的任务
        if (!done)
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
    }
}
```

## 1.4 invokeAny
```java
public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
    try {
        return doInvokeAny(tasks, false, 0);
    } catch (TimeoutException cannotHappen) {
        assert false;
        return null;
    }
}

public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
    return doInvokeAny(tasks, true, unit.toNanos(timeout));
}

private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
        throws InterruptedException, ExecutionException, TimeoutException {
    if (tasks == null)
        throw new NullPointerException();
    int ntasks = tasks.size();
    if (ntasks == 0)
        throw new IllegalArgumentException();
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
    ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);
    
    /*
    为了提高效率（尤其是在并行性有限的执行器中），请在提交更多任务之前检查是否完成了先前提交的任务。
    这种交织加上异常机制解决了主循环的混乱问题。
     */

    try {
        // 记录异常，以便如果我们无法获得任何结果，则可以抛出所得到的最后一个异常。
        ExecutionException ee = null;
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Iterator<? extends Callable<T>> it = tasks.iterator();

        // 确保开始一项任务；其余的递增
        futures.add(ecs.submit(it.next()));
        --ntasks;
        int active = 1;

        for (;;) {
            Future<T> f = ecs.poll();
            if (f == null) {
                if (ntasks > 0) {
                    --ntasks;
                    futures.add(ecs.submit(it.next()));
                    ++active;
                }
            else if (active == 0)
                break;
            else if (timed) {
                f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                if (f == null)
                    throw new TimeoutException();
                nanos = deadline - System.nanoTime();
            }
            else
                f = ecs.take();
            }
            
            if (f != null) {
                --active;
                try {
                    return f.get();
                } catch (ExecutionException eex) {
                    ee = eex;
                } catch (RuntimeException rex) {
                    ee = new ExecutionException(rex);
                }
            }
        }

        if (ee == null)
            ee = new ExecutionException();
        throw ee;

    } finally {
        for (int i = 0, size = futures.size(); i < size; i++)
            futures.get(i).cancel(true);
    }
}
```


[service]: ExecutorService.md
[future-task]: FutureTask.md