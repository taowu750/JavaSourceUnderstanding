`java.util.concurrent.FutureTask` 类的声明如下：
```java
public class FutureTask<V> implements RunnableFuture<V>
```
一个可取消的异步任务。这个类提供了 `Future` 的基础实现，它有启动和取消任务、查询任务是否完成以及返回任务结果的方法。
只有当任务完成后才能返回结果；如果任务还没有完成，`get` 方法将阻塞。一旦任务完成，任务就不能被重启或取消（除非任务是用 `runAndReset` 调用的）。

`FutureTask` 可以用来包装一个 `Callable` 或 `Runnable` 对象。因为 `FutureTask` 实现了 `Runnable`，
所以 `FutureTask` 可以提交给一个 `Executor` 执行。

除了作为一个独立的类，这个类还提供了 `protected` 方法，这些方法在创建自定义任务类时可能很有用。

修订说明：这与该类以前的版本不同，以前的版本依赖于 `AbstractQueuedSynchronizer`，主要是为了避免在取消竞争时保留中断状态。
在当前的设计中，同步控制依赖于一个通过 CAS 更新的 `state` 字段来跟踪完成情况，以及一个简单的 `Treiber` 栈来保存等待的线程。

参见 [Future][future] 接口。

# 1. 内部类
```java
// 简单的链接列表节点，用于记录 Treiber 栈中的等待线程。更详细的解释请参考其他类，如 Phaser 和 SynchronousQueue。
static final class WaitNode {
    volatile Thread thread;
    volatile WaitNode next;
    
    // 记录当前线程
    WaitNode() { thread = Thread.currentThread(); }
}
```

# 2. 成员字段

## 2.1 状态
```java
/**
 * 这个任务的运行状态，最初是 NEW。只有在方法 set、setException 和 cancel 中，运行状态才会过渡到终止状态。
 * 在完成时，状态可能暂时会是 COMPLETING（在结果被设置的时候）或 INTERRUPTING（仅在运行 cancel(true) 时）。
 * 
 * 从这些中间状态到最终状态的转换使用高效的的 Unsafe.putOrdered，因为最终状态的转移路径是唯一的，不能再进一步修改。
 * 
 * 下面是可能的状态转换：
 * - NEW -> COMPLETING -> NORMAL
 * - NEW -> COMPLETING -> EXCEPTIONAL
 * - NEW -> CANCELLED 
 * - NEW -> INTERRUPTING -> INTERRUPTED
 */
private volatile int state;
private static final int NEW          = 0;
private static final int COMPLETING   = 1;
private static final int NORMAL       = 2;
private static final int EXCEPTIONAL  = 3;
private static final int CANCELLED    = 4;
private static final int INTERRUPTING = 5;
private static final int INTERRUPTED  = 6;
```

## 2.2 其他属性
```java
// 底层的 Callable；运行后被设为 null
private Callable<V> callable;

// get() 返回的结果或抛出的异常
private Object outcome;

// 运行 callable 的线程；在 run() 期间会被 CAS
private volatile Thread runner;

// 等待线程的 Treiber 堆栈
private volatile WaitNode waiters;
```

## 2.3 Unsafe
```java
// Unsafe 对象
private static final sun.misc.Unsafe UNSAFE;
// state 字段偏移量
private static final long stateOffset;
// runner 字段偏移量
private static final long runnerOffset;
// waiters 字段偏移量
private static final long waitersOffset;

static {
    try {
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> k = FutureTask.class;
        stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
        runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
        waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

# 3. 构造器
```java
public FutureTask(Callable<V> callable) {
    if (callable == null)
        throw new NullPointerException();
    this.callable = callable;
    this.state = NEW;       // ensure visibility of callable
}

public FutureTask(Runnable runnable, V result) {
    this.callable = Executors.callable(runnable, result);
    this.state = NEW;       // ensure visibility of callable
}
```

# 4. 方法

## 4.1 get
```java
public V get() throws InterruptedException, ExecutionException {
    int s = state;
    // 任务还未完成，等待任务完成
    if (s <= COMPLETING)
        s = awaitDone(false, 0L);
    return report(s);
}

public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (unit == null)
        throw new NullPointerException();
    int s = state;
    // 任务还未完成，则先等待任务完成。如果超时时间内还未完成，则抛出 TimeoutException
    if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
        throw new TimeoutException();
    return report(s);
}

// 等待任务完成，或在中断、超时时中止。
// 参数 timed 表示是否使用超时时间
private int awaitDone(boolean timed, long nanos) throws InterruptedException {
    final long deadline = timed ? System.nanoTime() + nanos : 0L;
    WaitNode q = null;
    boolean queued = false;
    for (;;) {
        // 如果当前线程（调用此方法的线程）被中断，则移除等待节点 q 并抛出异常
        if (Thread.interrupted()) {
            removeWaiter(q);
            throw new InterruptedException();
        }

        int s = state;
        // 如果任务已经完成、出现异常、被取消或被中断
        if (s > COMPLETING) {
            // 如果 q 不为 null，解除和线程的链接，避免垃圾保留
            if (q != null)
                q.thread = null;
            // 返回当前状态
            return s;
        }
        // 完成中，则当前线程先不允许，尝试让状态继续转移
        else if (s == COMPLETING) // cannot time out yet
            Thread.yield();
        // 为当前线程新建等待节点 q
        else if (q == null)
            q = new WaitNode();
        // 如果 q 还未入栈，将其放到栈顶
        else if (!queued)
            queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
        // 如果设定了超时时间
        else if (timed) {
            nanos = deadline - System.nanoTime();
            // 如果超时时间已过，移除节点，返回当前状态
            if (nanos <= 0L) {
                removeWaiter(q);
                return state;
            }
            // 否则阻塞当前线程一段时间
            LockSupport.parkNanos(this, nanos);
        }
        // 否则没有设定超时时间，则一直阻塞当前线程
        else
            LockSupport.park(this);
    }
}

// 任务已经完成，根据运行状态返回结果或抛出异常
@SuppressWarnings("unchecked")
private V report(int s) throws ExecutionException {
    Object x = outcome;
    // 任务正常完成，返回结果
    if (s == NORMAL)
        return (V)x;
    // 任务被取消或被中断，抛出 CancellationException
    if (s >= CANCELLED)
        throw new CancellationException();
    // 任务运行过程中发生异常，抛出 ExecutionException，此时 outcome 表示一个异常对象
    throw new ExecutionException((Throwable)x);
}

/**
 * 尝试删除超时或中断的等待节点 node，以避免保留垃圾。内部节点在没有 CAS 的情况下被简单地解除连接，
 * 因为如果释放者无论如何都要遍历它们，这是无害的。
 * 
 * 为了避免从已删除的节点解除连接的影响，在出现明显的竞争时，会重新进行遍历。当有很多节点时，这是很慢的，
 * 但我们并不希望列表长到足以超过更高开销的方案。
 */
private void removeWaiter(WaitNode node) {
    if (node != null) {
        // 准备移除 node
        node.thread = null;
        retry:
        for (;;) {          // restart on removeWaiter race
            // 循环遍历，找到并移除 node 节点。过程中如果出现线程竞争，则重新进行遍历
            for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                s = q.next;
                if (q.thread != null)
                    pred = q;
                else if (pred != null) {
                    pred.next = s;
                    // pred 节点 thread 被其他线程设为 null
                    if (pred.thread == null) // check for race
                        continue retry;
                }
                // CAS 失败，出现线程竞争
                else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s))
                    continue retry;
            }
            break;
        }
    }
}
```

## 4.3 状态查询
```java
public boolean isCancelled() {
    return state >= CANCELLED;
}

public boolean isDone() {
    return state != NEW;
}
```

## 4.4 finishCompletion
```java
// 删除并唤醒所有等待的线程，然后调用 done()，最后将 callable 设为 null。
private void finishCompletion() {
    // assert state > COMPLETING;
    // 当 waiters 不为 null 时，进入循环
    for (WaitNode q; (q = waiters) != null;) {
        // 尝试将 waiters CAS 为 null，这样只会有一个线程进行唤醒操作
        if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
            for (;;) {
                Thread t = q.thread;
                // 等待节点 q 的 thread 存在，则将其唤醒并设为 null
                if (t != null) {
                    q.thread = null;
                    LockSupport.unpark(t);
                }
                // 遍历下一个节点
                WaitNode next = q.next;
                if (next == null)
                    break;
                // 删除链接
                q.next = null; // unlink to help gc
                q = next;
            }
            // 完成后结束循环
            break;
        }
    }

    // 调用回调函数
    done();

    // 删除 callable
    callable = null;        // to reduce footprint
}

/**
 * 当此任务转换为状态 isDone 时调用的 protected 方法（无论正常完成还是通过取消）。默认实现不执行任何操作。
 * 
 * 子类可以重写此方法以调用完成回调或进行记录。请注意，您可以在此方法的实现内部查询状态，以确定此任务是否已取消。
 */
protected void done() { }
```

## 4.4 cancel
```java
public boolean cancel(boolean mayInterruptIfRunning) {
    // 如果状态是 NEW，则根据 mayInterruptIfRunning 决定状态转移到 INTERRUPTING 还是 CANCELLED。
    // 否则如果状态不是 NEW，或者 CAS 失败，则返回 false，取消操作失败。
    if (!(state == NEW &&
        UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
        mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
        return false;
    
    try {    // in case call to interrupt throws exception
        if (mayInterruptIfRunning) {
            // 尝试中断任务运行的线程
            try {
                Thread t = runner;
                if (t != null)
                    t.interrupt();
            } finally { // final state
                // 任务状态最终转移到 INTERRUPTED
                UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
            }
        }
    } finally {
        // 最后调用 finishCompletion
        finishCompletion();
    }
    return true;
}
```

## 4.5 set
```java
/**
 * 除非已经设置或取消了此 Future，否则将此 Future 的结果设置为给定值。
 * 
 * 成功完成计算后，run 方法会在内部调用此方法。
 */
protected void set(V v) {
    // CAS 状态从 NEW 转移为 COMPLETING
    if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
        // 结果设为 v
        outcome = v;
        // 到达最终状态 NORMAL
        UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
        // 调用 finishCompletion
        finishCompletion();
    }
}

/**
 * setException 使此 future 最终会抛出 ExecutionException（当调用 get 时），并以给定 throwable 作为其原因，
 * 除非已经设置了结果或取消了此 future。
 * 
 * 计算失败时，run 方法会在内部调用此方法。
 */
protected void setException(Throwable t) {
        // CAS 状态从 NEW 转移为 COMPLETING
    if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
        // 结果设为异常 t
        outcome = t;
        // 到达最终状态 NORMAL
        UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
        // 调用 finishCompletion
        finishCompletion();
    }
}
```

## 4.6 run
```java
// 执行任务
public void run() {
    // 如果状态不为 NEW，或者 CAS 当前线程为运行任务的线程失败，则直接返回
    if (state != NEW ||
        !UNSAFE.compareAndSwapObject(this, runnerOffset,
        null, Thread.currentThread()))
        return;
    try {
        Callable<V> c = callable;
        // 检查条件
        if (c != null && state == NEW) {
            V result;
            boolean ran;
            try {
                // 执行给定任务，并等待结果
                result = c.call();
                ran = true;
            } catch (Throwable ex) {
                // 抛出异常，则 setException
                result = null;
                ran = false;
                setException(ex);
            }
            // 正常完成，则 set
            if (ran)
                set(result);
        }
    } finally {
        // runner 必须为非 null，直到最终完成为止，以防止并发调用 run()
        runner = null;
        // 清除 runner 后必须重新读取状态，以防止遗漏中断（cancel 方法中设置）
        int s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
}

private void handlePossibleCancellationInterrupt(int s) {
    // 等待状态转移（到 INTERRUPTED）完成
    if (s == INTERRUPTING)
        while (state == INTERRUPTING)
            Thread.yield();
}
```

## 4.7 runAndReset
```java
/**
 * 在不设置计算结果的情况下执行计算，然后将此 future 状态重置为初始状态，如果计算遇到异常或被取消，则无法执行此操作。
 * 它设计用于与内部执行多次的任务一起使用。
 */
protected boolean runAndReset() {
        // 如果状态不为 NEW，或者 CAS 当前线程为运行任务的线程失败，则直接返回
    if (state != NEW ||
        !UNSAFE.compareAndSwapObject(this, runnerOffset,
        null, Thread.currentThread()))
        return false;
    boolean ran = false;
    int s = state;
    try {
        Callable<V> c = callable;
        if (c != null && s == NEW) {
            try {
                // 执行给定任务，但不设置结果
                c.call(); // don't set result
                ran = true;
            } catch (Throwable ex) {
                // 出现异常，则设置异常
                setException(ex);
            }
        }
    } finally {
        // runner 必须为非 null，直到最终完成为止，以防止并发调用 run()
        runner = null;
        // 清除 runner 后必须重新读取状态，以防止遗漏中断（cancel 方法中设置）
        s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
    // 正常完成，过程中没有被取消，则返回 true，表示下次还可以再次运行
    return ran && s == NEW;
}
```


[future]: Future.md