`java.util.concurrent.locks.ReentrantLock`类的声明如下：
```java
public class ReentrantLock implements Lock, java.io.Serializable
```
一个重入式互斥锁，其基本行为和语义与使用同步方法和语句（`synchronized`）访问的隐式监控锁相同，但具有扩展功能。

一个 `ReentrantLock` 由最后成功锁定但尚未解锁的线程拥有。当锁没有被其他线程拥有时，调用 `lock` 方法的线程将返回，成功获取锁。
如果当前线程已经拥有锁，该方法将立即返回。这可以使用方法 `isHeldByCurrentThread` 和 `getHoldCount` 方法来检查。

该类的构造函数接受一个可选的公平性参数。当设置为 `true` 时，在发生锁竞争的情况下，锁倾向于授予等待时间最长的线程。
否则这个锁不保证任何特定的访问顺序。与使用默认设置的程序相比，使用由许多线程访问的公平锁的程序可能会显示出较低的总体吞吐量（即较慢；通常要慢得多），
但在获取锁的时间上有较小的差异，并保证不出现饥饿现象。

但请注意，锁的公平性并不能保证线程调度的公平性。因此，在使用公平锁的众多线程中，其中一个线程可能会连续多次获得锁。
另外要注意的是，不带超时参数的 `tryLock()` 方法会忽略公平性设置。如果锁是可用的，即使其他线程在等待，它也会成功。

建议的做法是，总是在调用 `lock` 方法之后立即使用 `try/finally` 块：
```java
class X { 
    private final ReentrantLock lock = new ReentrantLock();
    // ...

    public void m() {
        lock.lock();  // block until condition holds
        try {
            // ... method body
        } finally {
            lock.unlock()
        }
    }
}
```

除了实现 `Lock` 接口外，这个类还定义了许多用于检查锁的状态的 `public` 和 `protected` 方法。这些方法中的一些只对监控有用。

该类的序列化行为与内置锁相同：反序列化的锁处于未锁定状态，而不管其序列化时的状态如何。

该锁最多支持同一个线程进行 2147483647(`int` 的最大值) 次递归锁。试图超过这个限制会导致 `lock` 方法抛出 `Error`。

更多信息参见 [Lock][lock] 接口和 [AbstractQueuedSynchronizer][aqs] 抽象类。

# 1. 内部类

## 1.1 Sync
```java
// ReentrantLock 的同步控制基础。有公平版的子类 FairSync 和非公平版的子类 NonFairSync。
// 使用 AQS 的状态字段 state 来表示锁的持有数量。
abstract static class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = -5179523762034025860L;

    // 实现 Lock.lock。
    abstract void lock();

    // 实现非公平的 tryLock。tryAcquire 在子类中实现，但都需要非公平的 tryLock 方法。
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        // state 为 0，表示还没有其他线程获取锁
        if (c == 0) {
            // CAS 成功，则成功获取到了锁
            if (compareAndSetState(0, acquires)) {
                // 设置当前线程为持有锁的线程
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 如果当前线程是持有锁的线程
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            // 数字溢出，则抛出溢出
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            // 重入
            setState(nextc);
            return true;
        }
        return false;
    }

    // 尝试释放锁
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        // 如果当前线程未持有锁，则抛出异常
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        // 如果 state 变为 0，则锁被完全释放
        if (c == 0) {
            free = true;
            // 锁已完全释放，当前线程不在持有锁
            setExclusiveOwnerThread(null);
        }
        // 更新状态
        setState(c);
        return free;
    }

    // 当前线程是否持有锁
    protected final boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    final ConditionObject newCondition() {
        return new ConditionObject();
    }

    // 获取拥有锁的线程
    final Thread getOwner() {
        return getState() == 0 ? null : getExclusiveOwnerThread();
    }

    // 如果当前线程持有锁，返回它重入的次数，否则返回 0
    final int getHoldCount() {
        return isHeldExclusively() ? getState() : 0;
    }

    // 是否有现成持有这个锁
    final boolean isLocked() {
        return getState() != 0;
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        setState(0); // reset to unlocked state
    }
}
```

## 1.2 NonfairSync
```java
// 非公平锁
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = 7316153563782823691L;

    // 实现 lock 方法
    final void lock() {
        // 先使用 CAS 快速地尝试
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            // 失败后使用 acquire 方法
            acquire(1);
    }

    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
}
```

## 1.3 FairSync
```java
static final class FairSync extends Sync {
    private static final long serialVersionUID = -3000897897090466540L;

    final void lock() {
        acquire(1);
    }

    // tryAcquire 的公平版本。
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        // 如果没有线程持有锁
        if (c == 0) {
            // 如果同步队列中没有等待的线程，并且 CAS 成功
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                // 设置当前线程为持有锁的线程
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 如果当前线程已经持有了这个锁
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            // 数字溢出，则抛出溢出
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            // 重入
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

# 2. 成员字段
```java
private static final long serialVersionUID = 7373984872572414699L;

// 所以实际的同步操作都由这个对象完成
private final Sync sync;
```

# 3. 构造器
```java
// 默认为非公平模式
public ReentrantLock() {
    sync = new NonfairSync();
}

// 根据 fair 字段选择是公平模式(true)还是非公平模式(false)
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

# 4. 方法

## 4.1 toString
```java
public String toString() {
    Thread o = sync.getOwner();
    return super.toString() + ((o == null) ?
                               "[Unlocked]" :
                               "[Locked by thread " + o.getName() + "]");
}
```

## 4.2 lock
```java
public void lock() {
    sync.lock();
}

public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}
```

## 4.3 tryLock
```java
public boolean tryLock() {
    // 非公平获取
    return sync.nonfairTryAcquire(1);
}

public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
```

## 4.4 unlock
```java
public void unlock() {
    sync.release(1);
}
```

## 4.5 newCondition
```java
public Condition newCondition() {
    return sync.newCondition();
}
```

## 4.6 getHoldCount
```java
/*
查询当前线程对该锁的持有数量。一个线程的每一次锁操作都会增加一个锁的持有量。

持有数信息通常只用于测试和调试目的。例如，如果某段代码不应该在已经持有锁的情况下进入，那么我们就可以断言这个情况：
 class X {
   ReentrantLock lock = new ReentrantLock();
   // ...
   public void m() {
     // 断言
     assert lock.getHoldCount() == 0;
     lock.lock();
     try {
       // ... method body
     } finally {
       lock.unlock();
     }
   }
 }
*/
public int getHoldCount() {
    return sync.getHoldCount();
}
```

## 4.7 isHeldByCurrentThread
```java
/*
查询该锁是否被当前线程持有。

类似于内置监控锁的 Thread.holdsLock(Object) 方法，这个方法通常用于调试和测试。
例如，一个只应该在锁被持有时调用的方法可以如下断言：
 class X {
   ReentrantLock lock = new ReentrantLock();
   // ...

   public void m() {
       assert lock.isHeldByCurrentThread();
       // ... method body
   }
 }

它也可以用来确保重入式锁的使用，例如：非重入式锁。
 class X {
   ReentrantLock lock = new ReentrantLock();
   // ...

   public void m() {
       assert !lock.isHeldByCurrentThread();
       lock.lock();
       try {
           // ... method body
       } finally {
           lock.unlock();
       }
   }
 }
*/
public boolean isHeldByCurrentThread() {
    return sync.isHeldExclusively();
}
```

## 4.8 isLocked
```java
// 查询该锁是否被任何线程持有。此方法是用于监视系统状态，而不是用于同步控制。
public boolean isLocked() {
    return sync.isLocked();
}
```

## 4.9 isFair
```java
// 返回是否是公平模式
public final boolean isFair() {
    return sync instanceof FairSync;
}
```

## 4.10 getOwner
```java
/*
返回当前拥有此锁的线程，如果没有则返回 null。

当这个方法被一个不是所有者的线程调用时，返回值只反映当前时刻的状态。例如，即使有线程已经快要获取该锁但还没有完成操作，
所有者也可能暂时为 null。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Thread getOwner() {
    return sync.getOwner();
}
```

## 4.11 hasQueuedThreads
```java
// 查询是否有线程在等待获取这个锁。请注意，由于取消锁的情况可能在任何时候发生，所以返回 true 并不能保证有其他线程获得这个锁。
// 这个方法主要是设计用来监控系统状态的。
public final boolean hasQueuedThreads() {
    return sync.hasQueuedThreads();
}
```

## 4.12 hasQueuedThread
```java
// 查询给定线程是否在等待获取这个锁。请注意，由于取消锁的情况可能随时发生，所以返回 true 并不能保证这个线程一定会获得这个锁。
// 这个方法主要是设计用来监控系统状态的。
public final boolean hasQueuedThread(Thread thread) {
    return sync.isQueued(thread);
}
```

## 4.13 getQueueLength
```java
// 返回等待获取此锁的线程数的估计值。这个值只是一个估计值，因为在本方法遍历内部数据结构时，线程数可能会动态变化。
// 本方法设计用于监视系统状态，而不是用于同步控制。
public final int getQueueLength() {
    return sync.getQueueLength();
}
```

## 4.14 getQueuedThreads
```java
/*
返回一个包含可能正在等待获取这个锁的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Collection<Thread> getQueuedThreads() {
    return sync.getQueuedThreads();
}
```

## 4.15 hasWaiters
```java
/*
查询是否有线程在等待与此锁相关的 Condition。注意，由于超时和中断可能随时发生，
所以返回 true 并不能保证未来的 signal 会唤醒任何线程。

这个方法主要是设计用来监控系统状态的。
*/
public boolean hasWaiters(Condition condition) {
    if (condition == null)
        throw new NullPointerException();
    // condition 必须是 ConditionObject 的实例
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
        throw new IllegalArgumentException("not owner");
    return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```

## 4.16 getWaitQueueLength
```java
/*
返回与此锁相关联的 Condition 上等待的线程数的估计值。请注意，由于超时和中断可能随时发生，所以估计值只作为实际等待者数量的上限。

此方法设计用于监视系统状态，而不是用于同步控制。
*/
public int getWaitQueueLength(Condition condition) {
    if (condition == null)
        throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
        throw new IllegalArgumentException("not owner");
    return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```

## 4.17 getWaitingThreads
```java
/*
返回一个集合，其中包含那些可能正在等待与这个锁相关联的 Condition 的线程。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Collection<Thread> getWaitingThreads(Condition condition) {
    if (condition == null)
        throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
        throw new IllegalArgumentException("not owner");
    return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```


[lock]: Lock.md
[aqs]: AbstractQueuedSynchronizer.md