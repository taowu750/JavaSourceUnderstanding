`java.util.concurrent.locks.LockSupport`类的声明如下：
```java
public class LockSupport
```

# 0. 介绍

## 0.1 基本原理

用于创建锁和其他同步类的锁工具类。

**该类为每个使用它的线程关联一个许可证**（类似于 `Semaphore` 类）。如果许可证可用，对 `park` 的调用将立即返回，
并在过程中消耗它；否则可能会等待（线程会进入 `WAITING` 或 `TIMED_WAITING` 状态）。
如果许可证不可用的话，`unpark` 会使许可证可用。与 `Semaphores` 不同的是，许可证最多只有一个，且线程初始时没有许可。

方法 `park` 和 `unpark` 提供了暂停和解除线程等待的有效方法，这些方法不会遇到被废弃的方法 `Thread.suspend` 和
`Thread.resume` 的问题。

此外，如果调用者的线程被中断，`park` 将返回。并且 `park` 方法有超时版本。`park` 方法也可能在其他任何时间以"无理由"的方式返回，
所以一般情况下 `park` 必须在一个循环内调用，返回后重新检查条件。从这个意义上说，`park` 作为"忙等待"的优化，
不会浪费那么多时间去旋转，但必须搭配 `unpark` 才有效。

`park` 的三种形式还各自支持一个阻塞对象参数。这个对象在线程被暂停时被记录下来，以允许监控和诊断工具识别线程被暂停的原因
（这类工具可以使用方法 `getBlocker(Thread)` 访问阻塞者）。强烈鼓励使用这些形式而不是没有这个参数的原始形式。

这些方法被设计为用于创建更高级别的同步实用程序的工具，其本身对大多数并发控制应用并不有用。
`park` 方法被设计成只用于如下形式的构造中：
```java
while (!canProceed()) { 
    ... 
    LockSupport.park(this);
}
```
其中 `canProceed` 和在调用 `park` 之前的任何其他操作都不应该导致加锁或阻塞。因为每个线程只关联一个许可，
所以任何对 `park` 的中间使用都可能干扰其预期效果。

## 0.2 park 和 wait 的区别

`park` 和 `wait` 有以下区别：
 - `wait` 和 `notify` 都是 `Object` 中的方法，在调用这两个方法前必须先获得锁对象，
 但是 `park` 不需要获取某个对象的锁就可以暂停线程。
 - `notify` 只能随机选择一个线程唤醒，无法唤醒指定的线程，`unpark` 却可以唤醒一个指定的线程。
 - `park` 带有一个许可证，这使得 `unpark` 先于 `park` 调用也不会错失信号，导致死锁。
 而 `notify` 先于 `wait` 调用会导致死锁。

`LockSupport` 的测试参见 [LockSupportTest][test]。

## 0.3 实例

下面是一个例子，展示了用 `LockSupport` 实现一个先进先出的非重入式锁：
```java
class FIFOMutex {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

    public void lock() {
        boolean wasInterrupted = false;
        Thread current = Thread.currentThread();
        waiters.add(current);

        // 当前线程在队列中不是第一个或不能获得锁的情况下进行暂停。
        while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
            LockSupport.park(this);
            if (Thread.interrupted()) // 在等待的时候忽视中断
                wasInterrupted = true;
        }

        waiters.remove();
        if (wasInterrupted)          // 退出时重新设置中断状态
            current.interrupt();
    }

    public void unlock() {
        locked.set(false);
        LockSupport.unpark(waiters.peek());
    }
}
```

# 1. 成员字段
```java
private static final sun.misc.Unsafe UNSAFE;
private static final long parkBlockerOffset;
private static final long SEED;
private static final long PROBE;
private static final long SECONDARY;

static {
    try {
        // 获取 Unsafe 对象
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> tk = Thread.class;
        // 获取 Thread 中的 parkBlocker、threadLocalRandomSeed、threadLocalRandomSeed、
        // threadLocalRandomProbe、threadLocalRandomSecondarySeed 字段的偏移地址
        parkBlockerOffset = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("parkBlocker"));
        SEED = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomSeed"));
        PROBE = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomProbe"));
        SECONDARY = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
    } catch (Exception ex) { throw new Error(ex); }
}
```
参见 [Unsafe 类][unsafe] 和 [Thread 类][thread]。

# 2. 方法

## 2.1 setBlocker
```java
// 设置 Thread 的 parkBlocker 字段
private static void setBlocker(Thread t, Object arg) {
    // 即使 Thread 的 parkBlocker 字段使用了 volatile，HotSpot 在这里也不需要使用写屏障。
    UNSAFE.putObject(t, parkBlockerOffset, arg);
}
```

## 2.2 getBlocker
```java
/*
返回提供给最近调用 park 方法且尚未取消等待的线程的 blocker 对象；如果线程没有被暂停，则返回 null。

返回的值只是一个瞬间的快照--线程可能已经在不同的 blocker 对象上解封或等待了。
*/
public static Object getBlocker(Thread t) {
    if (t == null)
        throw new NullPointerException();
    return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
}
```

## 2.3 park
```java
/*
如果许可证可用，那么它就会被消耗掉，调用立即返回；否则当前线程就会处于线程调度的目的而被禁用，并处于等待状态，
直到下面三种情况之一发生：
 - 其他线程以当前线程为参数调用 unpark；
 - 其他线程中断了当前线程；
 - 调用虚假地（即无缘无故）返回。

本方法不报告是哪种情况导致方法返回。调用者应该重新检查当初导致线程等待的条件。
调用者还可以通过返回时线程的中断状态确定。
*/
public static void park() {
    UNSAFE.park(false, 0L);
}

// 和 park 类似，只不过多了一个 blocker 参数作为负责暂停当前线程的同步对象
public static void park(Object blocker) {
    // 获取当前线程
    Thread t = Thread.currentThread();
    // 设置 Blocker
    setBlocker(t, blocker);
    // 获取许可
    UNSAFE.park(false, 0L);
    // 重新可运行后将 Blocker 设置为 null。没有这句话，调用 getBlocker 函数，
    // 得到的还是前一个 park(Object blocker) 设置的 Blocker，这样是不符合逻辑的。
    setBlocker(t, null);
}
```

## 2.4 parkNanos
```java
/*
如果许可证可用，则将其消耗掉，并立即返回；否则，出于线程调度的目的，当前线程将被禁用，并处于等待状态，
直到发生以下四种情况之一：
 - 其他一些线程以当前线程为目标调用 unpark；
 - 其他一些线程中断当前线程；
 - 经过指定的等待时间；
 - 虚假地（即无缘无故）调用返回。

本方法不报告是哪种情况导致方法返回。调用者应该重新检查当初导致线程等待的条件。
调用者还可以通过返回时线程的中断状态或返回时所经过的时间确定。
*/
public static void parkNanos(long nanos) {
    if (nanos > 0)
        UNSAFE.park(false, nanos);
}

// 和 parkNanos 类似，只不过多了一个 blocker 参数作为负责暂停当前线程的同步对象
public static void parkNanos(Object blocker, long nanos) {
    if (nanos > 0) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, nanos);
        setBlocker(t, null);
    }
}
```

## 2.5 parkUntil
```java
/*
如果许可证可用，则将其消耗掉，并立即返回；否则，出于线程调度的目的，当前线程将被禁用，并处于等待状态，
直到发生以下四种情况之一：
 - 其他一些线程以当前线程为目标调用 unpark；
 - 其他一些线程中断当前线程；
 - 指定的截止日期过去了；
 - 虚假地（即无缘无故）调用返回。
 - 其他一些线程以当前线程为目标调用 unpark； 或者

本方法不报告是哪种情况导致方法返回。调用者应该重新检查当初导致线程等待的条件。
调用者还可以通过返回时线程的中断状态或返回时的当前时间确定。
*/
public static void parkUntil(long deadline) {
    UNSAFE.park(true, deadline);
}

// 和 parkUntil 类似，只不过多了一个 blocker 参数作为负责暂停当前线程的同步对象
public static void parkUntil(Object blocker, long deadline) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    UNSAFE.park(true, deadline);
    setBlocker(t, null);
}
```

## 2.6 unpark
```java
/*
如果给定线程许可证不可用，则使它可用。

如果此线程被 park 暂停，则它将取消等待；否则，此线程不会被下一个 park 调用暂停。

如果给定线程尚未启动，则不能保证此操作完全没有影响。
*/
public static void unpark(Thread thread) {
    if (thread != null)
        UNSAFE.unpark(thread);
}
```

## 2.7 nextSecondarySeed
```java
/*
返回伪随机初始化或更新的辅助种子。由于包访问限制，从 ThreadLocalRandom 复制。

此方法被 StampedLock 类使用。
*/
static final int nextSecondarySeed() {
    int r;
    Thread t = Thread.currentThread();
    if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
        r ^= r << 13;   // xorshift
        r ^= r >>> 17;
        r ^= r << 5;
    }
    else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
        r = 1; // avoid zero
    UNSAFE.putInt(t, SECONDARY, r);
    return r;
}
```


[unsafe]: ../../../../sun_/misc/Unsafe.md
[thread]: ../../../lang/Thread.md
[test]: ../../../../../test/java_/util/concurrent/locks/LockSupportTest.java