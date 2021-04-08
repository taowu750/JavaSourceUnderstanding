`java.util.concurrent.locks.ReentrantReadWriteLock` 类的声明如下：
```java
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable
```

# 0. 介绍

## 0.1 简介

读写锁（`ReadWriteLock`）的实现，支持类似于 `ReentrantLock` 的语义。参见 [Lock][lock] 接口。

该类有以下属性。

### 0.1.1 获取顺序

这个类并没有为锁的访问强加一个读者或写者优先的顺序。然而，它确实支持一个可选的公平性政策。

1. 非公平模式（默认）  
当构造为非公平锁时，进入读写锁的顺序是不指定的。一个持续争用的非公平锁可能会无限期地推迟一个或多个读写线程，
但通常会比公平锁有更高的吞吐量。
2. 公平模式  
当构造为公平时，线程根据到达顺序进入。当前持有的锁被释放时，等待时间最长的单个写者线程将被分配到写锁，
或者如果有一组读者线程的等待时间长于所有等待的写者线程，该组线程将被分配到读锁。

    如果写锁被持有，或者有一个等待的写者线程，那么试图非重入（第一次获取）获取公平的读锁的线程将阻塞。
    该线程在当前最老的等待写入者线程获取并释放写锁后，才会获取读锁。当然，如果一个等待的写者线程放弃了它的等待，
    留下一个或多个读者线程作为队列中等待时间最长的线程，而写锁是空闲的，那么这些读者将被分配到读锁。
    除非读锁和写锁都是空闲的（这意味着没有等待的线程），否则一个试图获得公平写锁（非重入）的线程将阻塞。

    注意，非阻塞的 `ReentrantReadWriteLock.ReadLock.tryLock()` 和 `ReentrantReadWriteLock.WriteLock.WriteLock.tryLock()`
    方法不使用公平锁的设置，如果可能的话，会立即获取锁，而不管是否有等待的线程。

### 0.1.2 可重入性

这个锁允许读者和写者以 `ReentrantLock` 的风格重新获取读锁或写锁。在写线程所持有的所有写锁被释放之前，
其它线程不能获取写锁或读锁。如果是在公平锁模式下，这个写线程只能重入已经持有的读锁，非重入型（第一次获取）的读锁是不被允许的。

此外，写者可以获取读锁，但不能反过来。在其他应用中，当写锁在调用或回调到在读锁下执行读的方法期间被持有时，重入性是很有用的。
如果一个读者试图获取写锁，它将永远不会成功。

### 0.1.3 锁降级

可重入性也允许从写锁降级为读锁，方法是先获得写锁，再获得读锁，然后释放写锁。但是，从读锁升级到写锁是不可能的。

### 0.1.4 中断锁的获取

在锁的获取过程中，读锁和写锁都支持中断。

### 0.1.5 Condition 支持

写锁提供了一个 `Condition` 实现，对于写锁来说，它的行为和 `ReentrantLock.newCondition` 提供的 `Condition` 实现对于 `ReentrantLock` 的行为是一样的。
当然，这个 `Condition` 只能用于写锁。

读锁不支持 `Condition`，`readLock().newCondition()` 会抛出 `UnsupportedOperationException`。

### 0.1.6 其它工具

该类支持确定锁是否被持有或争夺的方法。这些方法是为了监控系统状态而设计的，而不是为了同步控制。

### 0.1.7 序列化

该类的序列化行为与内置锁的行为相同：反序列化的锁处于解锁状态，而不管其序列化时的状态如何。

### 0.1.8 实现要点

这个锁最多支持 65535 个递归写锁和 65535 个读锁。试图超过这些限制会导致锁方法抛出 `Error`。

## 0.2 例子

这里是一个示例，展示了如何在更新缓存后执行锁降级（当以非嵌套方式处理多个锁时，异常处理特别棘手）：
```java
 class CachedData {
   Object data;
   volatile boolean cacheValid;
   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

   void processCachedData() {
     rwl.readLock().lock();
     if (!cacheValid) {
       // 必须在获取写锁之前释放读锁
       rwl.readLock().unlock();
       rwl.writeLock().lock();
       try {
         // 重新检查状态，因为另一个线程可能在我们之前获得了写锁并改变了状态。
         if (!cacheValid) {
           data = ...
           cacheValid = true;
         }
         // 在释放写锁之前，通过获取读锁进行降级。
         rwl.readLock().lock();
       } finally {
         rwl.writeLock().unlock(); // 释放写锁，此时仍持有读锁
       }
     }

     try {
       use(data);
     } finally {
       rwl.readLock().unlock();
     }
   }
 }
```

`ReentrantReadWriteLock` 可以用来提高某些类型 `Collection` 的某些用途的并发性。通常只有当集合预期很大，
被更多的读者线程而不是写者线程访问，并且需要进行开销大于同步开销的操作时，才值得这样做。例如，这里有一个使用 `TreeMap` 的类，
它的并发访问预期是很大的：
```java
 class RWDictionary {
   private final Map<String, Data> m = new TreeMap<String, Data>();
   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
   private final Lock r = rwl.readLock();
   private final Lock w = rwl.writeLock();

   public Data get(String key) {
     r.lock();
     try { return m.get(key); }
     finally { r.unlock(); }
   }
   public String[] allKeys() {
     r.lock();
     try { return m.keySet().toArray(); }
     finally { r.unlock(); }
   }
   public Data put(String key, Data value) {
     w.lock();
     try { return m.put(key, value); }
     finally { w.unlock(); }
   }
   public void clear() {
     w.lock();
     try { m.clear(); }
     finally { w.unlock(); }
   }
 }
```

# 1. 内部类

## 1.1 Sync
```java
abstract static class Sync extends AbstractQueuedSynchronizer
```
`ReentrantReadWriteLock` 的底层同步控制实现，有公平和非公平模式的子类。

底层实现参见 [AbstractQueuedSynchronizer][aqs] 抽象类。

### 1.1.1 内部类
```java
// 每个读者线程的计数器。以 ThreadLocal 的形式维护；缓存在 cachedHoldCounter 中。
static final class HoldCounter {
    int count = 0;
    // 使用线程 id，而不是线程引用，避免线程不能被回收
    final long tid = getThreadId(Thread.currentThread());
}

static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
    public HoldCounter initialValue() {
        return new HoldCounter();
    }
}
```

### 1.1.2 成员字段
```java
// 当前线程持有的可重入读锁的数量。仅在构造函数和 readObject 中初始化。当一个线程的读锁数降到 0 时删除。
private transient ThreadLocalHoldCounter readHolds;

/*
最后一个成功获取读锁的线程的持有数。在常见的情况下，下一个要释放的线程是最后一个获取的线程，
这可以节省 ThreadLocal 的查找。

它不是 volatile 的，因为它只是作为一个启发式的方法，对于线程来说，这将是很好的缓存。
*/
private transient HoldCounter cachedHoldCounter;

/*
firstReader 是第一个获得读锁的线程，firstReaderHoldCount 是 firstReader 的持有数。

更准确地说，firstReader 是最后一次将共享计数从 0 改为 1，并且在那之后没有释放读锁的唯一线程；
如果没有这样的线程，则为空。

除非线程终止时没有放弃读锁，否则不会导致垃圾保留，因为 tryReleaseShared 将其设置为 null。

这两个字段使得对无竞争读锁的读保持的跟踪非常高效。
*/
private transient Thread firstReader = null;
private transient int firstReaderHoldCount;
```

### 1.1.3 读写计数
```java
/*
下面是读数与写数的提取常量和函数。锁定状态(int)在逻辑上分为两个无符号 short。
低位代表独占的(写者)锁重入次数，高位的代表共享的(读者)锁重入次数。
*/

static final int SHARED_SHIFT   = 16;
static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

// 返回 c 中的读锁计数
static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }

// 返回 c 中的写锁计数
static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
```

### 1.1.4 构造器
```java
Sync() {
    readHolds = new ThreadLocalHoldCounter();
    setState(getState()); // 确保 readHolds 的可见性
}
```

### 1.1.5 方法

#### 1.1.5.1 抽象方法
```java
// 当前线程试图获取读锁时，如果应该因为超过其他等待线程的策略而阻塞，返回 true。
// 子类覆盖此方法，以实现公平或非公平策略
abstract boolean readerShouldBlock();

// 当前线程试图获取写锁时，如果应该因为超过其他等待线程的策略而阻塞，返回 true。
// 子类覆盖此方法，以实现公平或非公平策略
abstract boolean writerShouldBlock();
```

#### 1.1.5.2 写锁(独占锁)获取释放
```java
protected final boolean isHeldExclusively() {
    return getExclusiveOwnerThread() == Thread.currentThread();
}

/*
注意，tryRelease 和 tryAcquire 可以被 Condition 调用。所以它们的参数可能包含了读和写的持有计数，
这些持有计数在 Condition 等待期间被全部释放，然后在 tryAcquire 中重新建立。
*/

protected final boolean tryAcquire(int acquires) {
    /*
    执行流程：
    1. 如果读计数或写计数非 0，并且持有线程是其它线程，获取失败。
    2. 如果计数达到最大值，获取失败。
    3. 否则，如果该线程是重入获取或者队列策略允许，则该线程有资格获取锁，更新状态并设置所有者。
    */

    Thread current = Thread.currentThread();
    // 使用 state 作为写锁和读锁的计数
    int c = getState();
    int w = exclusiveCount(c);
    if (c != 0) {
        // (注意: if c != 0 and w == 0 then shared count != 0)
        // 如果读锁或写锁被其他线程持有，返回 false
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        // 已达到可重入最大上限，抛出异常
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // 重入获取
        setState(c + acquires);
        return true;
    }
    // 此时锁还没有被任何线程持有

    // 如果应该阻塞当前线程使用 tryAcquire 获取写锁，或者 CAS 获取写锁失败，返回 false
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    // 获取写锁成功，设置当前线程为独占线程
    setExclusiveOwnerThread(current);
    return true;
}

protected final boolean tryRelease(int releases) {
    // 确保当前线程是写锁的持有者
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // 减去 releases，减少重入计数
    int nextc = getState() - releases;
    boolean free = exclusiveCount(nextc) == 0;
    // 如果减到 0，则释放写锁
    if (free)
        setExclusiveOwnerThread(null);
    setState(nextc);
    return free;
}
```

#### 1.1.5.3 读锁(共享锁)获取释放
```java
protected final int tryAcquireShared(int unused) {
    /*
    执行流程：
    1. 如果有其他线程持有写锁，获取失败
    2. 否则，这个线程是符合锁状态的，检查它是否应该因为队列策略而阻塞。如果不是，则尝试通过 CAS 状态和更新计数来授予读锁。
       注意，该步骤不检查是否有重入的获取，这一点推迟到 fullTryAcquireShared 方法，以避免在非重入情况下必须检查持有计数。
    3. 如果第 2 步失败，要么是因为线程不符合条件，要么是因为 CAS 失败，要么是因为计数达到最大值，
       使用 fullTryAcquireShared 方法进行进一步操作。
    */

    Thread current = Thread.currentThread();
    int c = getState();
    // 如果其它线程持有写锁，返回 -1
    if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
        return -1;
    int r = sharedCount(c);

    // 如果不阻塞当前线程获取读锁，并且读计数未达到最大值，且 CAS 读取计数成功
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        // 如果当前线程是第一个获得读锁的线程，将其进行记录
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        }
        // 如果当前线程是第一个获得读锁的线程并且重入，更新计数
        else if (firstReader == current) {
            firstReaderHoldCount++;
        } 
        // 否则更新当前线程的 ThreadLocal 计数
        else {
            HoldCounter rh = cachedHoldCounter;
            // 如果缓存是 null，或者缓存线程不等于当前线程
            if (rh == null || rh.tid != getThreadId(current))
                // 更新缓存为当前线程的 HoldCounter
                cachedHoldCounter = rh = readHolds.get();
            // 缓存和当前线程匹配。
            // 如果缓存计数为 0，则将其设置到线程本地变量中。因为之前计数变为 0 的时候缓存会被删除，
            // 所以在这里需要重新设置
            else if (rh.count == 0)
                readHolds.set(rh);
            // 增加计数
            rh.count++;
        }
        // 获取读锁成功，返回 1
        return 1;
    }

    // 线程不符合条件、或 CAS 失败、或计数达到最大值，使用 fullTryAcquireShared 方法进行处理
    return fullTryAcquireShared(current);
}

// 获取读锁的完整版本，处理了 tryAcquireShared 中没有处理的「线程不符合条件、CAS 失败、计数达到最大值」情况。
final int fullTryAcquireShared(Thread current) {
    HoldCounter rh = null;
    // 循环 CAS
    for (;;) {
        int c = getState();
        // 如果有线程获取了独占锁
        if (exclusiveCount(c) != 0) {
            // 这个线程不是当前线程，返回 -1
            if (getExclusiveOwnerThread() != current)
                return -1;
            // 此时当前线程持有独占锁，在这里阻塞会造成死锁。
        } 
        // 如果应该阻塞当前线程获取读锁。准确来说是应该阻塞当前线程获取非重入读锁
        else if (readerShouldBlock()) {
            // 确保我们没有重新获取读取锁。

            // 当前线程是第一个获取读锁的线程，则此时是重入读锁，因此不需要做任何限制
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
            } 
            // 如果当前线程不是第一个获取读锁的线程
            else {
                // 更新线程本地变量的计数
                if (rh == null) {
                    rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current)) {
                        rh = readHolds.get();
                        // 如果计数为 0，表示第一次获取读锁。而 ThreadLocal.get 方法会在值不存在的情况下创建一个默认的变量。
                        // 所以我们需要删除这个线程本地变量
                        if (rh.count == 0)
                            readHolds.remove();
                    }
                }
                // 如果读取计数为 0，表示是第一次获取读锁，即非重入获取，因此返回 -1
                if (rh.count == 0)
                    return -1;
            }
        }
        // 此时当前线程有获取读锁的资格

        // 如果读取计数达到最大值，抛出异常
        if (sharedCount(c) == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");

        // CAS 读锁计数
        if (compareAndSetState(c, c + SHARED_UNIT)) {
            // 如果当前线程是第一个获得读锁的线程，将其进行记录
            if (sharedCount(c) == 0) {
                firstReader = current;
                firstReaderHoldCount = 1;
            } 
            // 如果当前线程是第一个获得读锁的线程并且重入，更新计数
            else if (firstReader == current) {
                firstReaderHoldCount++;
            }
            // 否则更新当前线程的 ThreadLocal 计数 
            else {
                if (rh == null)
                    rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
                cachedHoldCounter = rh; // cache for release
            }
            return 1;
        }
    }
}

protected final boolean tryReleaseShared(int unused) {
    Thread current = Thread.currentThread();
    // 如果当前线程是第一个获得读锁的线程
    if (firstReader == current) {
        // assert firstReaderHoldCount > 0;
        // 如果 firstReaderHoldCount 等于 1，表示完全释放
        if (firstReaderHoldCount == 1)
            // firstReader 设为 null
            firstReader = null;
        else
            // 否则重入计数减 1
            firstReaderHoldCount--;
    } else {
        // 否则获取当前线程的计数
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current))
            rh = readHolds.get();
        int count = rh.count;
        if (count <= 1) {
            // 如果计数小于等于 1，表示完全释放，则删除这个线程本地变量
            readHolds.remove();
            // 如果计数小于等于 0，表示当前线程没持有读锁，抛出异常
            if (count <= 0)
                throw unmatchedUnlockException();
        }
        // 重入计数减 1
        --rh.count;
    }
    
    // 循环 CAS 直到成功
    for (;;) {
        int c = getState();
        int nextc = c - SHARED_UNIT;
        // 释放读锁对读者没有任何影响，但如果现在读锁和写锁都是空闲的，则可能允许等待的写者获取写锁。
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

#### 1.1.5.4 tryLock
```java
// 执行写锁的 tryLock。除了没有调用 writerShouldBlock 之外，它和 tryAcquire 的效果是一样的。
// 也就是此方法会忽略公平性设置。
final boolean tryWriteLock() {
    Thread current = Thread.currentThread();
    int c = getState();
    if (c != 0) {
        int w = exclusiveCount(c);
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
    }
    if (!compareAndSetState(c, c + 1))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}

// 执行读锁的 tryLock。除了没有调用 writerShouldBlock 之外，它和 tryAcquireShared 的效果是一样的。
// 也就是此方法会忽略公平性设置。
final boolean tryReadLock() {
    Thread current = Thread.currentThread();
    for (;;) {
        int c = getState();
        if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != current)
            return false;
        int r = sharedCount(c);
        if (r == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        if (compareAndSetState(c, c + SHARED_UNIT)) {
            if (r == 0) {
                firstReader = current;
                firstReaderHoldCount = 1;
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    cachedHoldCounter = rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
            }
            return true;
        }
    }
}
```

#### 1.1.5.5 newCondition
```java
final ConditionObject newCondition() {
    // 使用 AbstractQueuedSynchronizer.ConditionObject
    return new ConditionObject();
}
```

#### 1.1.5.6 监控和状态方法
```java
final Thread getOwner() {
    return ((exclusiveCount(getState()) == 0) ?
            null :
            getExclusiveOwnerThread());
}

// 获取所有读者线程的重入次数
final int getReadLockCount() {
    return sharedCount(getState());
}

final boolean isWriteLocked() {
    return exclusiveCount(getState()) != 0;
}

// 获取当前线程在此写锁上面的重入次数
final int getWriteHoldCount() {
    return isHeldExclusively() ? exclusiveCount(getState()) : 0;
}

// 获取当前线程在此读锁上面的重入次数。
final int getReadHoldCount() {
    if (getReadLockCount() == 0)
        return 0;

    // 先查询缓存，缓存中没有再查询 ThreadLocal

    Thread current = Thread.currentThread();
    if (firstReader == current)
        return firstReaderHoldCount;

    HoldCounter rh = cachedHoldCounter;
    if (rh != null && rh.tid == getThreadId(current))
        return rh.count;

    int count = readHolds.get().count;
    // ThreadLocal.get 方法会在值不存在的情况下创建一个默认的变量。因此这里需要删除它
    if (count == 0) readHolds.remove();
    return count;
}

final int getCount() { return getState(); }
```

#### 1.1.5.7 序列化
```java
// 反序列化相当于调用了构造器，恢复到未锁定状态
private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    readHolds = new ThreadLocalHoldCounter();
    setState(0); // reset to unlocked state
}
```

## 1.2 NonfairSync
```java
// 非公平锁实现
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -8159625535654395037L;
    
    final boolean writerShouldBlock() {
        return false; // writers can always barge
    }
    
    final boolean readerShouldBlock() {
        /*
        apparentlyFirstQueuedIsExclusive：如果第一个排队的线程以独占模式等待，则返回 true。

        作为避免无限期写者饥饿的启发式方法，如果此时队列头部是一个等待的写者，则需要阻塞读者线程。
        这只是一个概率效应，因为如果队列前面有其它等待的读者，它们后面有一个等待的写者，那么这个新的读者就不会被阻塞。
        */
        return apparentlyFirstQueuedIsExclusive();
    }
}
```

## 1.3 FairSync
```java
// 公平锁实现
static final class FairSync extends Sync {
    private static final long serialVersionUID = -2274990926593161451L;
    
    final boolean writerShouldBlock() {
        // hasQueuedPredecessors 方法返回队列中是否有其它等待的线程
        return hasQueuedPredecessors();
    }
    
    final boolean readerShouldBlock() {
        return hasQueuedPredecessors();
    }
}
```

## 1.4 ReadLock
```java
// 读锁
public static class ReadLock implements Lock, java.io.Serializable {
    
    private static final long serialVersionUID = -5992448646407690164L;
    private final Sync sync;

    protected ReadLock(ReentrantReadWriteLock lock) {
        sync = lock.sync;
    }

    public void lock() {
        sync.acquireShared(1);
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean tryLock() {
        return sync.tryReadLock();
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void unlock() {
        sync.releaseShared(1);
    }

    // 读锁不支持 Condition
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        int r = sync.getReadLockCount();
        return super.toString() +
                "[Read locks = " + r + "]";
    }
}
```

## 1.5 WriteLock
```java
// 写锁
public static class WriteLock implements Lock, java.io.Serializable {
    
    private static final long serialVersionUID = -4992448646407690164L;
    private final Sync sync;

    protected WriteLock(ReentrantReadWriteLock lock) {
        sync = lock.sync;
    }

    public void lock() {
        sync.acquire(1);
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public boolean tryLock( ) {
        return sync.tryWriteLock();
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    public void unlock() {
        sync.release(1);
    }

    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public int getHoldCount() {
        return sync.getWriteHoldCount();
    }

    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
```

# 2. 成员字段
```java
private static final long serialVersionUID = -6992448646407690164L;

// 读锁
private final ReentrantReadWriteLock.ReadLock readerLock;
// 写锁
private final ReentrantReadWriteLock.WriteLock writerLock;
// 执行所有的同步策略
final Sync sync;

// Unsafe 实例
private static final sun.misc.Unsafe UNSAFE;
// 线程 id 字段偏移量
private static final long TID_OFFSET;
static {
    try {
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> tk = Thread.class;
        TID_OFFSET = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("tid"));
    } catch (Exception e) {
        throw new Error(e);
    }
}
```
参见 [Unsafe][unsafe] 类。

# 3. 构造器
```java
// 默认创建一个非公平读写锁
public ReentrantReadWriteLock() {
    this(false);
}

// 参数 fair 为 true，表示公平模式；false 表示非公平模式
public ReentrantReadWriteLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
    readerLock = new ReadLock(this);
    writerLock = new WriteLock(this);
}
```

# 4. 方法

## 4.1 读写锁获取
```java
// 返回构造器中已经创建好的读锁或写锁。重复调用此方法也只会返回同一个读锁或写锁对象。

public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }

public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }
```

## 4.2 toString
```java
public String toString() {
    int c = sync.getCount();
    int w = Sync.exclusiveCount(c);
    int r = Sync.sharedCount(c);

    return super.toString() +
        "[Write locks = " + w + ", Read locks = " + r + "]";
}
```

## 4.3 getThreadId
```java
// 之所以使用这个方法而不是 Thread.getID 方法，是因为 Thread.getID 是非 final 的，有被覆盖的可能。
static final long getThreadId(Thread thread) {
    return UNSAFE.getLongVolatile(thread, TID_OFFSET);
}
```

## 4.4 isFair
```java
// 该锁是不是公平锁
public final boolean isFair() {
    return sync instanceof FairSync;
}
```

## 4.5 getOwner
```java
/*
返回当前拥有写锁的线程，如果没有则返回 null。

当这个方法被一个不是所有者的线程调用时，返回值只反映当前时刻的状态。例如，即使有线程已经快要获取该锁但还没有完成操作，
所有者也可能暂时为 null。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Thread getOwner() {
    return sync.getOwner();
}
```

## 4.6 isWriteLocked
```java
// 查询是否有线程持有写锁。该方法用于监控系统状态，而不是用于同步控制。
public boolean isWriteLocked() {
    return sync.isWriteLocked();
}
```

## 4.7 isWriteLockedByCurrentThread
```java
// 查询当前线程是否持有读锁
public boolean isWriteLockedByCurrentThread() {
    return sync.isHeldExclusively();
}
```

## 4.8 holdCount
```java
// 查询该锁所持有的读锁数量（准确的说是读锁总的重入次数）。该方法用于监控系统状态，而不是用于同步控制。
public int getReadLockCount() {
    return sync.getReadLockCount();
}

// 查询当前线程在写锁上总的重入次数，当前线程没有持有写锁则返回 0。该方法用于监控系统状态，而不是用于同步控制。
public int getWriteHoldCount() {
    return sync.getWriteHoldCount();
}

// 查询当前线程在读锁上的重入次数，当前线程没有持有读锁则返回 0。该方法用于监控系统状态，而不是用于同步控制。
public int getReadHoldCount() {
    return sync.getReadHoldCount();
}
```

## 4.9 queuedThreads
```java
/*
返回一个包含可能正在等待获取写锁的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Collection<Thread> getQueuedWriterThreads() {
    return sync.getExclusiveQueuedThreads();
}

/*
返回一个包含可能正在等待获取读锁的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Collection<Thread> getQueuedReaderThreads() {
    return sync.getSharedQueuedThreads();
}

// 查询是否有线程在等待获取读锁或写锁。请注意，由于取消锁的情况可能在任何时候发生，所以返回 true 并不能保证有其他线程获得这个锁。
// 这个方法主要是设计用来监控系统状态的。
public final boolean hasQueuedThreads() {
    return sync.hasQueuedThreads();
}

// 查询给定线程是否在等待获取读锁或写锁。请注意，由于取消锁的情况可能随时发生，所以返回 true 并不能保证这个线程一定会获得这个锁。
// 这个方法主要是设计用来监控系统状态的。
public final boolean hasQueuedThread(Thread thread) {
    return sync.isQueued(thread);
}

// 返回等待获取读锁或写锁的线程数的估计值。这个值只是一个估计值，因为在本方法遍历内部数据结构时，线程数可能会动态变化。
// 本方法设计用于监视系统状态，而不是用于同步控制。
public final int getQueueLength() {
    return sync.getQueueLength();
}

/*
返回一个包含可能正在等待获取读锁或写锁的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便实现具有更广泛锁监控功能的子类。
*/
protected Collection<Thread> getQueuedThreads() {
    return sync.getQueuedThreads();
}
```

## 4.10 Condition 监控方法
```java
/*
查询是否有线程在等待与写锁相关的 Condition。注意，由于超时和中断可能随时发生，
所以返回 true 并不能保证未来的 signal 会唤醒任何线程。

这个方法主要是设计用来监控系统状态的。
*/
public boolean hasWaiters(Condition condition) {
    if (condition == null)
        throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
        throw new IllegalArgumentException("not owner");
    return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
}

/*
返回与写锁相关联的 Condition 上等待的线程数的估计值。请注意，由于超时和中断可能随时发生，
所以估计值只作为实际等待者数量的上限。

此方法设计用于监视系统状态，而不是用于同步控制。
*/
public int getWaitQueueLength(Condition condition) {
    if (condition == null)
        throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
        throw new IllegalArgumentException("not owner");
    return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
}

/*
返回一个集合，其中包含那些可能正在等待与写锁相关联的 Condition 的线程。因为在构造这个结果时，实际的线程集可能会动态变化，
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
[unsafe]: ../../../../sun_/misc/Unsafe.md