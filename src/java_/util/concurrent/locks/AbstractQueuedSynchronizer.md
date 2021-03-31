`java.util.concurrent.locks.AbstractQueuedSynchronizer`抽象类的声明如下：
```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable
```

# 0. 介绍

## 0.1 底层实现

AQS 提供了一个实现阻塞锁和相关同步器（`ReentrantLock`、`Semaphore`等）的框架。它的核心思想是，如果被请求的共享资源空闲，
则将当前请求资源的线程设置为有效的工作线程，并且将共享资源设置为锁定状态。如果被请求的共享资源被占用，
那么就需要一套线程阻塞等待以及被唤醒时锁分配的机制，这个机制 AQS 是用 **CLH 队列锁**实现的，
即将暂时获取不到锁的线程加入到队列中。

CLH(Craig,Landin,and Hagersten)队列是一个虚拟的双向队列（虚拟的双向队列即不存在队列实例，仅存在结点之间的关联关系）。
AQS 是将每条请求共享资源的线程封装成一个 CLH 锁队列的一个结点（Node）来实现锁的分配。

AQS 使用一个 `int` 值来表示状态。子类必须定义改变这个状态的 `protected` 方法，
这些方法定义了这个状态对这个对象的 acquire 或 release 意味着什么。通过这些，这个类中的其他方法就会执行所有的排队和阻塞机制。
子类可以维护其他的状态字段，但只有使用方法 `getState`、`setState` 和 `compareAndSetState` 方法原子更新的 `int` 值才会被跟踪与同步。

`AbstractQueuedSynchronizer` 子类应该被定义为非 `public` 的内部帮助类，用于实现其外部类的同步属性。
`AbstractQueuedSynchronizer` 没有实现任何同步接口，相反，它定义了诸如 `acquireInterruptibly` 这样的方法，
可以被具体的子类锁和同步器酌情调用，以实现它们的公共方法。

该类支持默认的独占模式和共享模式中的一种或两种（分别提供了不同的方法）。
 - **独占模式**：只有一个线程能执行。
    - **公平锁**：按照线程在队列中的排队顺序，先到者先拿到锁
    - **非公平锁**：当线程要获取锁时，先通过两次 CAS 操作去抢锁，如果没抢到，当前线程再加入到队列中等待唤醒。
 - **共享模式**：多个线程可同时执行。

相对来说，非公平锁会有更好的性能，因为它的吞吐量比较大。当然，非公平锁让获取锁的时间变得更加不确定，
可能会导致在阻塞队列中的线程长期处于饥饿状态。

AQS 并不"理解"独占模式和共享模式的差异。当共享模式 acquire 成功时，下一个等待的线程（如果存在的话）必须确定它是否也能 acquire。
在不同模式下等待的线程共享同一个 CLH 队列。通常情况下，实现子类只支持其中一种模式，但两种模式都可以发挥作用，
例如在读写锁中。只支持独占或只支持共享模式的子类不需要定义未使用的模式的方法。

AQS 定义了一个嵌套的 `AbstractQueuedSynchronizer.ConditionObject` 类，可以被支持独占模式的子类用作 `Condition` 实现。
`ConditionObject` 使用了 AQS 的各种独占模式方法。其中方法 `isHeldExclusively` 返回当前线程（调用此方法的线程）是否正在独占资源；
方法 `fullyRelease` 释放当前线程持有的锁；方法 `acquireQueued`，不断尝试获取锁。

`AbstractQueuedSynchronizer` 中的所有方法几乎都没有使用这个 `ConditionObject`，除了一些监控方法。
如果不准备支持独占模式，就不要使用它。`ConditionObject` 的行为取决于其同步器实现的语义。

这个类为内部队列提供了各种检查、工具和监控方法，这些方法可以根据需要导出到使用 `AbstractQueuedSynchronizer` 的类中，以实现其同步机制。

该类的序列化只存储底层原子整数，所以反序列化对象的同步队列是空的。需要序列化的子类需要定义一个 `readObject` 方法，
在反序列化时将其恢复到已知的初始状态。

## 0.2 用法

### 0.2.1 使用惯例

要使用该类作为同步器的基础，请使用 `getState`、`setState` 和 `compareAndSetState` 检查或修改同步状态，
酌情重写以下方法：
 - `isHeldExclusively()`：当前线程是否正在独占资源。只有用到 `Condition` 才需要去实现它。
 - `tryAcquire(int acquires)`：独占方式。尝试获取资源，成功则返回 `true`，失败则返回 `false`。
 - `tryRelease(int releases)`：独占方式。尝试释放资源，成功则返回 `true`，失败则返回 `false`。
 - `tryAcquireShared(int acquires)`：共享方式。尝试获取资源。负数表示失败；0 表示成功，但没有剩余可用资源；正数表示成功，且有剩余资源。
 - `tryReleaseShared(int releases)`：共享方式。尝试释放资源，成功则返回 `true`，失败则返回 `false`。

这些方法默认都会抛出 `UnsupportedOperationException`。这些方法的实现必须是内部线程安全的，并且应该是快速的。
所有其他方法都被声明为 `final`，因为它们不能独立变化。

你可能还会发现从 `AbstractOwnableSynchronizer` 继承的方法对于跟踪拥有独占同步器的线程很有用。
鼓励你使用它们——这将启用监视和诊断工具，以帮助用户确定哪些线程持有锁。

即使这个类是基于内部的 CLH 队列，它也不会自动执行 FIFO 获取策略。独占模式同步获取的核心形式是：
```java
Acquire:
   while (!tryAcquire(arg)) {
       // 如果线程没有入队，将其添加到等待队列中。可能会阻塞当前线程
   }
  
Release:
   if (tryRelease(arg))
       // 解除对第一个入队线程的阻塞
```
共享模式与上面类似，但可能涉及信号传播。

因为在 `acquire` 中的检查是在入队之前调用的，所以一个新的请求线程可能会在其他被阻塞或排队的线程之前插入，不排队获取锁。
然而，你可以根据需要定义 `tryAcquire` 或 `tryAcquireShared`，通过内部调用一个或多个检查方法来禁止插入，
从而提供一个公平的 FIFO 获取顺序（也就是公平锁）。
特别的，如果 `hasQueuedPredecessors`（一种专门设计用于公平模式同步器的方法）返回 `true`，
则大多数公平模式同步器都可以让 `tryAcquire` 返回 `false`。

非公平策略吞吐量和可扩展性最高，但可能导致线程饥饿。它的每次锁竞争对传入的线程都是毫无偏向。

这个类为同步提供了一个高效和可扩展的基础。如果这还不够，你可以使用原子类、自己定制的 `java.util.Queue` 类和 `LockSupport` 阻塞支持，
从较低的层次构建同步器。

### 0.2.2 例子

下面是一个不可重入的互斥锁，它使用值 0 表示解锁状态，使用值 1 表示锁定状态。尽管不可重入锁并不严格要求记录当前所有者线程，
但无论如何，此类会这样做，让使用情况更易于监控。它还支持 `Condition` 并公开一种检测方法：
```java
class Mutex implements Lock, java.io.Serializable {

    // 继承了 AbstractQueuedSynchronizer 的内部帮助类
    private static class Sync extends AbstractQueuedSynchronizer {
        // 报告是否处于锁定状态
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // 如果状态为零，则获取锁
        public boolean tryAcquire(int acquires) {
            assert acquires == 1; // 否则无效
            if (compareAndSetState(0, 1)) {
                // 记录当前持有锁的线程
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 通过将状态设置为零来释放锁
        protected boolean tryRelease(int releases) {
            assert releases == 1; // 否则无效
            if (getState() == 0) throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // 提供一个 Condition
        Condition newCondition() { return new ConditionObject(); }

        // 反序列化属性
        private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // 重置到未锁定状态
        }
    }

    // Sync 对象完成所有的同步工作，外部类只是将请求转发给它
    private final Sync sync = new Sync();

    public void lock()                { sync.acquire(1); }
    public boolean tryLock()          { return sync.tryAcquire(1); }
    public void unlock()              { sync.release(1); }
    public Condition newCondition()   { return sync.newCondition(); }
    public boolean isLocked()         { return sync.isHeldExclusively(); }
    public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }
    public boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}
```

# 1. 内部类

## 1.1 Node
```java
static final class Node
```
同步队列节点类。

同步队列是“CLH”（Craig，Landin 和 Hagersten）队列锁的变体。CLH 锁通常用于自旋锁。我们将它们用于阻塞同步器，
但使用相同的基本策略，**将有关线程的某些控制信息保存在其节点的前驱节点中（主要是 SIGNAL 和 PROPAGATE）**。
CLH 队列中保存的是暂时获取不到锁的线程。

每个节点中的 `waitStatus` 字段将跟踪线程是否应阻塞。`waitStatus` 字段不控制是否授予线程锁等。如果线程在队头，
它可能会尝试 acquire。但是在队头并不能保证成功，它只赋予了争夺的权利。因此，当前 release 的竞争线程可能需要重新等待。

要加入 CLH 锁队列，你可以将其作为新的 `tail`；要出队，只需设置 `head` 字段：
```
            +------+  prev  +-----+        +-----+
       head |      | <----> |     | <----> |     |  tail
            +------+  next  +-----+        +-----+
```

队列的每个节点都是由前一个结点唤醒。当结点发现前驱结点是 head 并且 `tryAcquire` 成功，则会轮到该线程运行。

插入到 CLH 队列中只需要对 `tail` 执行一次原子操作，因此存在一个简单的原子分界点，即未入队到入队。
类似地，出队仅涉及更新 `head`。

但是，节点需要花更多的精力来确定其后继节点是谁，部分原因是要处理由于超时、中断等可能导致的取消。
**`prev` 链接（在原始 CLH 锁中不使用）主要用于处理取消**。如果取消某个节点，则其后继节点（通常）会重新链接到未取消的前驱节点。
有关自旋锁情况下类似机制的说明，请参见 [Scott 和 Scherer 的论文][clh]。

注意**队列中的一个节点只有在成功获得锁后才会成为头节点，并且头节点永远不会被取消**。

**我们还使用 `next` 链接来实现阻塞和唤醒机制**。每个节点保存自己的线程，因此前驱节点通过 `next` 链接以确定下一个节点是哪个线程，
并通知其唤醒。新入队节点会设置其前驱结点的 `next` 字段，因此使用 `next` 链接需要避免与新入队的节点竞争。必要时，
当一个节点的后继结点为 `null` 时，通过从 `tail` 开始向前检查来确定下一个节点。（换句话说，`next` 链接是一种优化，
因此我们通常不需要向后扫描。）

**CLH 队列需要一个虚拟头节点（不包含线程的节点）**。但是，我们不会在构造过程中创建它，因为如果永远没有竞争，那会浪费时间。
相反，我们会在第一次竞争时创建节点并设置头尾指针。

**等待 `Condition` 的线程使用相同的节点，但是会用到额外的链接 `nextWaiter`**。`Condition` 只需要在一个简单（非并发）的单链表队列中链接节点，
因为它们只在独占模式下被访问。`await` 时，将节点插入 `Condition` 队列。`signal` 后，该节点将转移到同步队列中。
`waitStatus` 字段的 `CONDITION` 值用于标记一个节点在 `Condition` 队列上。

### 1.1.1 成员字段
```java
// 表示一个节点在共享模式下等待的标志，用在 nextWaiter 字段中。
static final Node SHARED = new Node();

// 表示一个节点在独占模式下等待的标志，用在 nextWaiter 字段中。
static final Node EXCLUSIVE = null;

// waitStatus 值：表示线程已经取消（超时、中断等），不再被调度。
static final int CANCELLED =  1;

// waitStatus 值：表示后继结点在等待当前结点唤醒子集。后继结点入队时，会将前继结点的状态更新为 SIGNAL。
static final int SIGNAL    = -1;

// waitStatus 值：表示线程正在等待 Condition，也就是在 Condition 队列中。
static final int CONDITION = -2;

// waitStatus 值：表示共享锁可能可以被后面在共享模式下阻塞的节点获取（传播）。
static final int PROPAGATE = -3;

/*
状态字段，只取以下值：
 - SIGNAL：
    表示此节点的后继结点在等待当前结点唤醒自己。后继结点入队时，会将前驱结点的状态更新为 SIGNAL。
    此节点的后继节点已经（或即将）被阻塞（通过 park），所以此节点在 release 时必须 unpark 后继结点。
 - CANCELLED：
    这个节点由于超时、中断或 fullyRelease 锁失败而被取消。节点永远不会离开这个状态，被取消的节点将会在以后被移除。
    特别的，一个被取消的节点的线程再也不会被阻塞。
 - CONDITION：
    该节点当前处于 Condition 队列中。在被转移到同步队列之前，它不会被用作同步队列节点；转移到同步队列之后，状态字段将被设置为 0。
 - PROPAGATE：
    表示共享锁可能可以被后面在共享模式下阻塞的节点获取（传播）。也就是有线程调用了 releaseShared 释放了锁。
    在 doReleaseShared 中只对 head 设置这个值，以确保传播继续，即使其他操作已经介入。
    共享锁的传播性目的是尽快唤醒同步队列中等待的线程，使其尽快获取资源（锁），但是也有一定的副作用，可能会造成不必要的唤醒。
 - 0：
    中间状态，表示当前节点在同步队列中，还没有阻塞，并准备尝试获取锁。

负值表示结点处于有效等待状态，而正值表示结点已被取消。所以源码中很多地方用 >0、<0 来判断结点的状态是否正常。

该字段对于普通的同步节点初始化为 0，对于 Condition 节点初始化为 CONDITION。
它的修改使用 CAS（或者在可能的情况下，使用无条件的 volatile 写入）。
*/
volatile int waitStatus;

/*
前驱节点的链接，当前节点要检查 prev 的 waitStatus。在入队时设置，出队时被清除。

当前驱节点被取消时，需要再寻找一个未取消的前驱节点，这个节点肯定存在，
因为头节点永远不会被取消：一个节点只有在成功 acquire 后才会成为头节点。

被取消的线程将永远不能成功地进行 acquire 操作，并且一个线程只会取消自己，而不会取消任何其他节点。
*/
volatile Node prev;

/*
后继节点的链接，当前节点在 release 时需要 unpark next 节点线程。在入队时设置，在跳过被取消的前驱节点时调整，出队时清除。

真正入队后才会分配前驱节点的 next 字段，所以一个为 null 的 next 字段并不一定意味着该节点处于队列的末端。
但是，如果一个 next 字段为 null，我们可以从 tail 向前扫描来再次检查。

被取消的节点的 next 字段被设置为指向节点本身，而不是 null，以方便 isOnSyncQueue 的操作。
*/
volatile Node next;

// 这个节点入队时的线程。构造时初始化，使用后设为 null。
volatile Thread thread;

/*
链接到下一个等待 Condition 的节点，或者特殊节点 SHARED。因为 Condition 队列只有在独占模式下才可以被访问，
所以我们只需要一个简单的链接队列，持有等待 Condition 的节点。在 signal 时将它们转移到同步队列中。

因为 Condition 只能用在独占模式下，所以我们用特殊节点 SHARED 来表示共享模式。
*/
Node nextWaiter;
```

### 1.1.2 构造器
```java
// 用于创建初始的头节点或 SHARED 标记节点。
Node() {
}

// 被 addWaiter 方法使用。参数 mode 有两种值：Node.EXCLUSIVE 和 Node.SHARED。
// 它用来添加同步队列节点
Node(Thread thread, Node mode) {
    this.nextWaiter = mode;
    this.thread = thread;
}

// 被 addConditionWaiter 方法使用，参数 waitStatus 在源码中只有 CONDITION 一种值。
// 它用来添加 Condition 等待节点
Node(Thread thread, int waitStatus) {
    this.waitStatus = waitStatus;
    this.thread = thread;
}
```

### 1.1.3 方法
```java
// 如果节点在共享模式下等待，则返回 true。
final boolean isShared() {
    // 此时 nextWaiter 用于表示共享模式
    return nextWaiter == SHARED;
}

/*
返回前驱节点。如果为 null，则抛出 NullPointerException。此方法在保证前驱节点不为 null 的情况下使用。
null 检查可以省略，存在是为了帮助 VM。
*/
final Node predecessor() throws NullPointerException {
    Node p = prev;
    if (p == null)
        throw new NullPointerException();
    else
        return p;
}
```

## 1.2 ConditionObject
```java
public class ConditionObject implements Condition, java.io.Serializable
```
作为 `Lock` 实现基础的 `AbstractQueuedSynchronizer` 的 `Condition` 实现。
用于独占模式的 Condition。

这个类是 `Serializable` 的，但是所有的字段都是 `transient` 的，所以反序列化的 `Condition` 没有等待者。

参见 [Condition][condition] 接口。

### 1.2.1 成员字段
```java
private static final long serialVersionUID = 1173984872572414699L;

// 表示在退出等待时需要重新中断的模式。
private static final int REINTERRUPT =  1;

// 表示在退出等待时需要抛出 InterruptedException 的模式。
private static final int THROW_IE    = -1;

// Condition 队列中第一个节点
private transient Node firstWaiter;

// Condition 队列中最后一个节点
private transient Node lastWaiter;
```

### 1.2.2 构造器
```java
public ConditionObject() { }
```

### 1.2.3 方法

#### 1.2.3. unlinkCancelledWaiters
```java
/*
一个清理方法，从 Condition 队列中解除已取消的等待节点的链接。仅在持有锁时才能调用。

当节点在多种 await 方法调用期间被取消，或在 addConditionWaiter 方法插入一个新的等待节点期间看到 lastWaiter 被取消时，
才会调用这个方法。

这个方法是为了在没有调用 signal 的情况下避免保留垃圾，导致内存泄露（也就是被取消的节点）。
因此，即使它可能需要进行一次完全遍历，但只有在没有 signal 的情况下发生超时或取消时，它才会发挥作用。

它遍历所有节点（而不是停留在某个特定的目标上），以解开所有指向垃圾节点的指针，而不需要在“取消风暴”中进行多次重新遍历。
*/
private void unlinkCancelledWaiters() {
    Node t = firstWaiter;
    Node trail = null;
    // 从头节点开始，遍历等待队列
    while (t != null) {
        Node next = t.nextWaiter;
        // 如果节点 t 不是处于 CONDITION 状态了，则需要将其从等待队列中移除
        if (t.waitStatus != Node.CONDITION) {
            t.nextWaiter = null;
            if (trail == null)
                firstWaiter = next;
            else
                trail.nextWaiter = next;
            if (next == null)
                lastWaiter = trail;
        }
        else
            trail = t;
        t = next;
    }
}
```

#### 1.2.3. addConditionWaiter
```java
// 此方法用在多种 await 方法中。添加一个新的节点到等待队列中。
private Node addConditionWaiter() {
    Node t = lastWaiter;
    // 如果 lastWaiter 被取消, 进行一次清理。
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
    }
    // 将当前线程（调用此方法的线程）添加到等待队列末尾
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    if (t == null)
        firstWaiter = node;
    else
        t.nextWaiter = node;
    // 让 lastWaiter 指向新加入的节点
    lastWaiter = node;
    return node;
}
```

#### 1.2.3. hasWaiters
```java
// 查询是否有线程在等待这个 Condition。
protected final boolean hasWaiters() {
    // 如果当前线程（调用此方法的线程）不是正在独占资源，抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // Condition 队列中有任意一个节点是 CONDITION 的，则返回 true；否则返回 false
    for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION)
            return true;
    }
    return false;
}
```

#### 1.2.3. getWaitQueueLength
```java
// 返回等待此 Condition 的线程数的估计值。
protected final int getWaitQueueLength() {
    // 如果当前线程不是正在独占资源，抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    int n = 0;
    for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION)
            ++n;
    }
    return n;
}
```

#### 1.2.3. getWaitingThreads
```java
// 返回一个包含可能正在等待该条件的线程的集合。
protected final Collection<Thread> getWaitingThreads() {
    // 如果当前线程不是正在独占资源，抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION) {
            Thread t = w.thread;
            if (t != null)
                list.add(t);
        }
    }
    return list;
}
```

#### 1.2.3. checkInterruptWhileWaiting
```java
/*
此方法用在多种 await 方法中。

检测是否发生了中断。
 - 如果没有中断，则返回 0
 - 如果发生了中断，则将 node 转移到同步队列中。
    - 如果在收到 signal 前中断，返回 THROW_IE。这表示在 await 过程中发生了中断，需要抛出异常；
    - 如果在收到 signal 后中断，返回 REINTERRUPT。这表示节点状态已为 SIGNAL，需要重新中断将其取消。
*/
private int checkInterruptWhileWaiting(Node node) {
    // 如果当前线程被中断，将 node 转移到同步队列中。成功转移返回 THROW_IE，失败返回 REINTERRUPT。
    // 没有被中断返回 0。
    return Thread.interrupted() ?
        (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
        0;
}
```

#### 1.2.3. reportInterruptAfterWait
```java
/*
此方法用在多种 await 方法中。

根据 interruptMode 的值有不同的行为：
 - THROW_IE: 抛出中断异常（InterruptedException）
 - REINTERRUPT: 重新中断当前线程
其他值则什么都不做。
*/
private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
    if (interruptMode == THROW_IE)
        throw new InterruptedException();
    else if (interruptMode == REINTERRUPT)
        // 重新中断当前线程。因为 checkInterruptWhileWaiting 会将线程中断状态重置，为了防止中断状态丢失，需要再次中断
        selfInterrupt();
}
```

#### 1.2.3. await
```java
/*
实现可中断的条件等待。
1. 如果当前线程（调用此方法的线程）已被中断，抛出 InterruptedException。
2. 保存由 getState() 返回的锁状态。
3. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
4. 阻塞线程，直到收到信号或被中断。
5. 以保存的状态为参数，调用 acquireQueued 方法重新 acquire。
6. 如果在步骤 4 中阻塞时发生中断，则抛出 InterruptedException。
*/
public final void await() throws InterruptedException {
    // 1. 如果当前线程已被中断，抛出 InterruptedException
    if (Thread.interrupted())
        throw new InterruptedException();
    // 添加一个新的节点（包含当前线程）到 Condition 队列中
    Node node = addConditionWaiter();
    // 2-3. 以 getState() 返回值作为参数调用 release，释放锁。
    // 如果失败（可能没有先获取锁）则 node 被取消，并且抛出 IllegalMonitorStateException
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 4. 阻塞线程，直到收到信号（节点会被转移到同步队列）或被中断
    while (!isOnSyncQueue(node)) {
        // park 会在 signal 或 release 中被 unpark。
        // park 也会被中断，从而唤醒线程。
        LockSupport.park(this);
        // 检查节点线程的中断状态。被中断则将节点转移到同步队列
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // 5. 被唤醒后需要继续尝试 acquire。以保存的状态为参数，调用 acquireQueued 方法重新尝试 acquire。
    // 如果 acquire 过程中发生了中断，并且步骤 4 中没有发生 THROW_IE 类型的中断，将 interruptMode 更新为 REINTERRUPT
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 清理 Condition 队列中被取消节点
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    // 6. 发生了中断，则报告中断状态
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

#### 1.2.3. awaitUninterruptibly
```java
/*
实现不中断的条件等待。

1. 保存由 getState() 返回的锁状态。
2. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
3. 阻塞线程，直到收到信号。
4. 以保存的状态为参数，调用 acquireQueued 方法重新 acquire。
*/
public final void awaitUninterruptibly() {
    // 添加一个新的节点（包含当前线程）到 Condition 队列中
    Node node = addConditionWaiter();
    // 1-2. 以保存的状态作为参数调用 release，如果失败（可能没有先获取锁）则抛出 IllegalMonitorStateException。
    int savedState = fullyRelease(node);
    boolean interrupted = false;
    // 3. 阻塞线程，直到收到信号，此时 node 已在同步队列中
    while (!isOnSyncQueue(node)) {
        // park 会在 signal 或 release 中被 unpark。
        // park 也会被中断，从而唤醒线程
        LockSupport.park(this);
        if (Thread.interrupted())
            interrupted = true;
    }
    // 4. 被唤醒后需要继续尝试 acquire。以保存的状态为参数，调用 acquireQueued 方法重新尝试 acquire。
    // 如果 acquire 过程中发生了中断，或者之前等待的过程中发生了中断，则重新中断线程。
    // 因为 acquireQueued 会将线程中断状态重置，为了防止中断状态丢失，需要再次中断
    if (acquireQueued(node, savedState) || interrupted)
        selfInterrupt();
}
```

#### 1.2.3. awaitNanos
```java
/*
实现定时条件等待。

1. 如果当前线程中断，抛出 InterruptedException。
2. 保存由 getState() 返回的锁状态，如果失败则抛出 IllegalMonitorStateException。
3. 用保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
4. 阻塞线程，直到收到信号、中断或超时。
5. 以保存的状态为参数，调用 acquireQueued 方法重新 acquire。
6. 如果在步骤 4 中阻塞时发生中断，则抛出 InterruptedException。

@return nanosTimeout 的估计值减去从本方法返回时的等待时间。如果大于 0 可以作为后续调用本方法的参数，
以完成所需时间的等待；小于等于零的值表示没有剩余时间。
*/
public final long awaitNanos(long nanosTimeout) throws InterruptedException {
    // 1. 如果当前线程已被中断，抛出 InterruptedException
    if (Thread.interrupted())
        throw new InterruptedException();
    // 添加一个新的节点（包含当前线程）到 Condition 队列中
    Node node = addConditionWaiter();
    // 2-3. 以 getState() 返回值作为参数调用 release，释放锁。
    // 如果失败（可能没有先获取锁）则 node 被取消，并且抛出 IllegalMonitorStateException
    int savedState = fullyRelease(node);
    // 计算等待的截止时间
    final long deadline = System.nanoTime() + nanosTimeout;
    int interruptMode = 0;
    // 4. 阻塞线程，直到收到信号（节点会被转移到同步队列）、发生中断或超时
    while (!isOnSyncQueue(node)) {
        // 超时则主动将节点移动到同步队列并跳出循环
        if (nanosTimeout <= 0L) {
            transferAfterCancelledWait(node);
            break;
        }
        // 当超时时间小于 spinForTimeoutThreshold 时，旋转比 park 更快。
        // 否则阻塞线程
        if (nanosTimeout >= spinForTimeoutThreshold)
            // park 会在 signal 或 release 中被 unpark。
            // park 也会被中断，从而唤醒线程
            LockSupport.parkNanos(this, nanosTimeout);
        // 检查节点线程的中断状态。被中断则将节点转移到同步队列
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
        nanosTimeout = deadline - System.nanoTime();
    }
    // 5. 被唤醒后需要继续尝试 acquire。以保存的状态为参数，调用 acquireQueued 方法重新尝试 acquire。
    // 如果 acquire 过程中发生了中断，并且步骤 4 中没有发生 THROW_IE 类型的中断，将 interruptMode 更新为 REINTERRUPT
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 清理 Condition 队列中被取消节点
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    // 6. 发生了中断，则报告中断状态
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    return deadline - System.nanoTime();
}

public final boolean await(long time, TimeUnit unit) throws InterruptedException {
    long nanosTimeout = unit.toNanos(time);
    if (Thread.interrupted())
        throw new InterruptedException();
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    final long deadline = System.nanoTime() + nanosTimeout;
    boolean timedout = false;
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {
        if (nanosTimeout <= 0L) {
            timedout = transferAfterCancelledWait(node);
            break;
        }
        if (nanosTimeout >= spinForTimeoutThreshold)
            LockSupport.parkNanos(this, nanosTimeout);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
        nanosTimeout = deadline - System.nanoTime();
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    return !timedout;
}
```

#### 1.2.3. awaitUntil
```java
/*
实现截止时间条件等待。

1. 如果当前线程中断，抛出 InterruptedException。
2. 保存由 getState() 返回的锁状态，如果失败则抛出 IllegalMonitorStateException。
3. 用保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
4. 阻塞线程，直到收到信号、中断或超时。
5. 以保存的状态为参数，调用 acquireQueued 方法重新 acquire。
6. 如果在步骤 4 中阻塞时发生中断，则抛出 InterruptedException。

@return 如果最后期限已过，则为 false，否则为 true。
*/
public final boolean awaitUntil(Date deadline) throws InterruptedException {
    long abstime = deadline.getTime();
    // 1. 如果当前线程已被中断，抛出 InterruptedException
    if (Thread.interrupted())
        throw new InterruptedException();
    // 添加一个新的节点（包含当前线程）到 Condition 队列中
    Node node = addConditionWaiter();
    // 2-3. 以 getState() 返回值作为参数调用 release，释放锁。
    // 如果失败（可能没有先获取锁）则 node 被取消，并且抛出 IllegalMonitorStateException
    int savedState = fullyRelease(node);
    boolean timedout = false;
    int interruptMode = 0;
    // 4. 阻塞线程，直到收到信号（节点会被转移到同步队列）、发生中断或超时
    while (!isOnSyncQueue(node)) {
        // 超时则主动将节点状态到同步队列并跳出循环
        if (System.currentTimeMillis() > abstime) {
            timedout = transferAfterCancelledWait(node);
            break;
        }
        // park 会在 signal 或 release 中被 unpark。
        // park 也会被中断，从而唤醒线程
        LockSupport.parkUntil(this, abstime);
        // 检查节点线程的中断状态。被中断则将节点转移到同步队列
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // 5. 被唤醒后需要继续尝试 acquire。以保存的状态为参数，调用 acquireQueued 方法重新尝试 acquire。
    // 如果 acquire 过程中发生了中断，并且步骤 4 中没有发生 THROW_IE 类型的中断，将 interruptMode 更新为 REINTERRUPT
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 清理 Condition 队列中被取消节点
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    // 6. 发生了中断，则报告中断状态
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    return !timedout;
}
```

#### 1.2.3. doSignal
```java
/*
从 first 开始，将一个未取消的结点从等待队列移到同步队列。

从 signal 方法中拆分出来，部分原因是为了鼓励编译器在没有等待者的情况下内联。
*/
private void doSignal(Node first) {
    // 从 first 开始，找到一个未取消的节点，将其移到同步队列
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
        // transferForSignal 将 first 从条件队列转移到同步队列。
        // 如果成功则返回 true；结点被取消返回 false。
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```

#### 1.2.3. doSignalAll
```java
// 将等待队列队列中的所有未取消的节点移到同步队列。
private void doSignalAll(Node first) {
    // 从 first 开始，将后面未取消的的结点都移到同步队列中。
    lastWaiter = firstWaiter = null;
    do {
        Node next = first.nextWaiter;
        first.nextWaiter = null;
        transferForSignal(first);
        first = next;
    } while (first != null);
}
```

#### 1.2.3. signal
```java
// 将队头未取消的线程——也是等待时间最长的线程（如果存在）从这个 Condition 的等待队列中移到同步队列中。
public final void signal() {
    // 如果当前线程不是正在独占资源，抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}
```

#### 1.2.3. signalAll
```java
// 将所有未取消的等待的线程（如果存在）从这个 Condition 的等待队列中移到同步队列中。
public final void signalAll() {
    // 如果同步器不是独占的，抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignalAll(first);
}
```

#### 1.2.3. isOwnedBy
```java
// 如果该 Condition 是由给定的同步器对象创建的，则返回 true。
final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
    return sync == AbstractQueuedSynchronizer.this;
}
```

# 2. 成员字段

## 2.1 同步队列
```java
// 等待队列的头，lazy 初始化。除初始化外，只能通过方法 setHead 修改。
// 注意：如果 head 存在，保证其 waitStatus 不为 CANCELLED。
private transient volatile Node head;

// 等待队列的尾，lazy 初始化。除初始化外，只能通过方法 enq 修改。
private transient volatile Node tail;
```

## 2.2 同步状态
```java
// 同步状态
private volatile int state;
```

## 2.3 spinForTimeoutThreshold
```java
/*
在多少纳秒的时间内，旋转比使用定时 park 更快。
粗略估计一下，就可以在超时时间很短的情况下提高响应速度。
*/
static final long spinForTimeoutThreshold = 1000L;
```

## 2.4 Unsafe 和字段偏移量
```java
// 获取 Unsafe 单例
private static final Unsafe unsafe = Unsafe.getUnsafe();

// state 属性的偏移量
private static final long stateOffset;

// head 属性的偏移量
private static final long headOffset;

// tail 属性的偏移量
private static final long tailOffset;

// Node.waitStatus 的偏移量
private static final long waitStatusOffset;

// Node.next 的偏移量
private static final long nextOffset;

// 静态初始化偏移量
static {
    try {
        stateOffset = unsafe.objectFieldOffset
            (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
        headOffset = unsafe.objectFieldOffset
            (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
        tailOffset = unsafe.objectFieldOffset
            (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
        waitStatusOffset = unsafe.objectFieldOffset
            (Node.class.getDeclaredField("waitStatus"));
        nextOffset = unsafe.objectFieldOffset
            (Node.class.getDeclaredField("next"));

    } catch (Exception ex) { throw new Error(ex); }
}
```

# 3. 构造器
```java
protected AbstractQueuedSynchronizer() { }
```

# 4. 方法

## 4.1 同步模板方法

### 4.1.1 isHeldExclusively
```java
/*
返回当前线程（调用此方法的线程）是否正在独占资源。

这个方法在每次调用一个非等待的 AbstractQueuedSynchronizer.ConditionObject 方法时被调用。
等待的方法反而会调用 release。

默认实现会抛出 UnsupportedOperationException。该方法仅在 AbstractQueuedSynchronizer.ConditionObject 方法中被内部调用，
因此如果不使用 Condition，则不需要定义该方法。
*/
protected boolean isHeldExclusively() {
    throw new UnsupportedOperationException();
}
```

### 4.1.2 tryAcquire
```java
/*
使用 arg 设置 state（这是自定义的部分），来尝试在独占模式下获取。本方法应该查询对象的状态是否允许以独占模式获取，如果允许则获取。

这个方法总是由执行获取的线程调用。如果该方法返回失败（false），且当前线程（调用此方法的线程）还没有被排队，
那么 acquire 方法可能会将其排队，直到其他线程发出释放信号。这可以用来实现方法 Lock.tryLock()。

默认的实现会抛出UnsupportedOperationException。
*/
protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
}
```

### 4.1.3 tryRelease
```java
/*
使用 arg 设置 state（这是自定义的部分），来进行独占模式下的释放操作。该方法总是由执行释放的线程调用。

默认实现会抛出 UnsupportedOperationException。

如果此对象现在处于完全释放的状态，则返回 true，这样任何等待的线程都可以尝试 acquire；否则返回 false。
*/
protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
}
```

### 4.1.4 tryAcquireShared
```java
/*
使用 arg 设置 state（这是自定义的部分），来常数以共享模式获取。本方法应该查询对象的状态是否允许在共享模式下获取，
如果允许则获取。

这个方法总是由执行获取的线程调用。如果该方法返回失败，并且如果当前线程还没有被排队，那么 acquire 方法可能会将其排队，
直到其他线程发出释放信号。

默认实现会抛出UnsupportedOperationException。

@return 资源数量。失败时返回负值；如果共享模式下的获取成功，但后续的获取不能成功，则返回零；
如果共享模式下的获取成功，后续共享模式下的获取也可能成功，则返回正值，在这种情况下，后续的等待线程必须检查可用性。
*/
protected int tryAcquireShared(int arg) {
    throw new UnsupportedOperationException();
}
```

### 4.1.5 tryReleaseShared
```java
/*
使用 arg 设置 state（这是自定义的部分），来共享模式下的释放。
该方法总是由执行释放的线程调用。

默认实现会抛出 UnsupportedOperationException。
*/
protected boolean tryReleaseShared(int arg) {
    throw new UnsupportedOperationException();
}
```

## 4.2 同步状态访问

### 4.2.1 getState
```java
// 返回同步状态的当前值。此操作的内存语义为 volatile。
protected final int getState() {
    return state;
}
```

### 4.2.2 setState
```java
// 设置同步状态的值。此操作的内存语义为 volatile。
protected final void setState(int newState) {
    state = newState;
}
```

### 4.2.3 compareAndSetState
```java
// CAS 更新 state
protected final boolean compareAndSetState(int expect, int update) {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

## 4.3 CLH 队列工具方法

### 4.3.1 CAS CLH
```java
// CAS CLH 队列的头
private final boolean compareAndSetHead(Node update) {
    return unsafe.compareAndSwapObject(this, headOffset, null, update);
}

// CAS CLH 队列的尾
private final boolean compareAndSetTail(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
}

// CAS CLH Node.next
private static final boolean compareAndSetNext(Node node,
                                               Node expect,
                                               Node update) {
    return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
}

// CAS 节点的 waitStatus
private static final boolean compareAndSetWaitStatus(Node node,
                                                     int expect,
                                                     int update) {
    return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                    expect, update);
}
```

### 4.3.2 setHead
```java
/*
将队列的头部设置为 node，也相当于此 node 出队，变成了虚拟头节点（不包含线程）。

仅由多种 acquire 方法调用。同时为了 GC、抑制不必要的 signal 和遍历，将不再使用的字段设为 null。
*/
private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}
```

### 4.3.3 enq
```java
// 将节点插入 CLH 同步队列中，必要时进行初始化。
// 返回 node 的前驱节点
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        // 同步队列为空，则进行初始化，新建一个头结点
        if (t == null) {
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            // 循环 CAS，将 node 作为新的 tail
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                // 返回 node 的前驱节点
                return t;
            }
        }
    }
}
```

### 4.3.4 addWaiter
```java
// 以当前线程（调用此方法的线程）和给定模式创建节点并将其入队。
// 返回创建的节点。
private Node addWaiter(Node mode) {
    // 以当前线程（调用此方法的线程）和给定模式创建节点
    Node node = new Node(Thread.currentThread(), mode);
    // 尝试快速地将节点入队，失败则使用 enq()
    Node pred = tail;
    // 如果队列非空
    if (pred != null) {
        node.prev = pred;
        // 将 node CAS 为新的 tail 成功
        if (compareAndSetTail(pred, node)) {
            // 连接两个节点，返回 node
            pred.next = node;
            return node;
        }
    }
    // 否则使用 enq()
    enq(node);
    return node;
}
```

### 4.3.5 isOnSyncQueue
```java
/*
如果一个节点（最初是放在 Condition 队列上的节点）现在已经在同步队列上等待重新 acquire，则返回 true。
*/
final boolean isOnSyncQueue(Node node) {
    // 如果 node 的 waitStatus 是 CONDITION 或者没有前驱节点，它肯定不在同步队列中
    if (node.waitStatus == Node.CONDITION || node.prev == null)
        return false;
    // 如果 node 有后继节点，它肯定在同步队列中（被取消的节点 next 指向自己，所以也认为被取消的节点在同步队列中）
    if (node.next != null)
        return true;

    /*
    node.prev 可以是非 null，但还没有进入队列，因为将其放入队列的 CAS 可能会失败。所以我们必须从 tail 向前遍历，以确保它真的做到了。
    在对这个方法的调用中，它总是会在尾部附近（因为是使用 enq() 添加的），所以我们几乎不会遍历多长。
    */
    return findNodeFromTail(node);
}

// 从 tail 向前搜索，如果节点在同步队列上，返回 true。此方法仅被 isOnSyncQueue 调用。
private boolean findNodeFromTail(Node node) {
    Node t = tail;
    for (;;) {
        if (t == node)
            return true;
        if (t == null)
            return false;
        t = t.prev;
    }
}
```

### 4.3.6 selfInterrupt
```java
// 中断当前线程。
static void selfInterrupt() {
    Thread.currentThread().interrupt();
}
```

## 4.4 Condition 队列转移方法

### 4.4.1 transferAfterCancelledWait
```java
/*
此方法用在多种 Condition.await 方法中。

在 Condition 队列等待的节点被取消后（超时或中断），将节点转移到同步队列。
如果线程在收到 signal 之前被取消，则返回 true。
*/
final boolean transferAfterCancelledWait(Node node) {
    // state: CONDITION -> 0
    // 如果 node 还是 Condition 状态，则它还没收到 signal。此时可以成功地将 node 的 Condition 状态取消，
    // 并将其添加到同步队列中，然后返回 true
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        // 添加到同步队列
        enq(node);
        return true;
    }

    /*
    否则此时 node 状态已被其他线程设为 SIGNAL，那么在它进入同步队列之前我们不能继续其他操作。
    所以我们旋转等待入队完成。
    */
    while (!isOnSyncQueue(node))
        // 如果 node 不在同步队列中，则等待
        Thread.yield();
    return false;
}
```

### 4.4.2 transferForSignal
```java
/*
此方法被多种 Condition.signal 方法调用。

 - 如果节点收到 signal 之前被取消直接返回 false；
 - 否则将一个节点从 Condition 队列转移到同步队列。
    - 如果 node 的前驱节点未取消或设置为 SIGNAL 成功，则同步队列中还有其他等待的节点，node 会在 release 中被 unpark
    - 否则让 node 从 Condition.await 的 park 中被唤醒，随后 node 会尝试 acquire
*/
final boolean transferForSignal(Node node) {
    // state: CONDITION -> 0
    // 如果不能改变 waitStatus，则 node 已被取消，返回 false
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;

    // 将 node 加入到同步队列。p 是 node 的前驱节点
    Node p = enq(node);
    int ws = p.waitStatus;
    // 如果前驱节点被取消（CANCELED = 1），或者设置它的 waitStatus 属性为 SIGNAL 失败
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        // 则唤醒 node 的线程以让它重新进行同步。
        LockSupport.unpark(node.thread);
    return true;
}
```

## 4.5 同步控制方法

### 4.5.1 shouldParkAfterFailedAcquire
```java
/*
要求 pred == node.prev。

检查 acquire 失败的节点 node：
 - 如果它的前驱节点状态是 SIGNAL，则当前线程（调用此方法的线程）应该阻塞（它以后会被持有锁的线程唤醒），返回 true。
 - 如果它的前驱节点被取消，向前寻找新的未被取消的前驱节点，然后返回 false。
 - 如果它的前驱节点处于其他状态，尝试将其状态更新为 SIGNAL，返回 false。

也就是只有当 node 的前驱结点的状态为 SIGNAL 时，才可以对 node 的线程进行 park 操作。
否则，将不能进行 park 操作。

这是所有 acquire 循环中的主要 signal 控制方法。
*/
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    // ws 是 node 的前驱节点
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        // node 拿锁失败，前继节点的状态是 SIGNAL，node 节点可以放心的阻塞，因为下次会被唤醒
        return true;
    // CANCELLED = 1
    if (ws > 0) {
        // node 的前驱节点被取消了，则向前查找最近的未被取消的前驱节点
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
        state: 0/PROPAGATE -> SIGNAL
        waitStatus 此时等于 0 或 PROPAGATE，当前线程还不能 park。
        尝试将前驱的状态设置成 SIGNAL，告诉它 release 后通知自己一下。有可能失败，说不定刚刚释放完
        */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    // 否则不能判断当前线程会不会被唤醒，不能 park
    return false;
}
```

### 4.5.2 parkAndCheckInterrupt
```java
// park 当前线程，之后测试当前线程是否被中断并返回
private final boolean parkAndCheckInterrupt() {
    // 注意 park 也是可以被中断的
    LockSupport.park(this);
    // Thread.interrupted() 方法会重置中断状态
    return Thread.interrupted();
}
```

### 4.5.3 unparkSuccessor
```java
// 此方法用在 cancelAcquire()、release() 和 doReleaseShared() 中。
// 如果存在的话，唤醒 node 后面第一个没有被取消的结点，并尝试将 node 状态 CAS 为 0。
private void unparkSuccessor(Node node) {
    /*
    state: SIGNAL -> 0
    如果 node 的 waitStatus 为负值（即可能等于 SIGNAL），尝试重置状态，消耗掉这次资源。
    如果 CAS 失败（waitStatus 被其他线程改变），也没关系。
    */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
    使用 s 存放需要进行 unpark 的线程，一般情况下是后继节点。但如果后继节点被取消（CANCELLED = 1）或为 null，
    则从 tail 向前遍历，找到下一个没有被取消的结点。
    之所以从后往前遍历，是因为被取消的节点的 next 指针指向它自己。
    */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            // 找到最接近 node 的没有被取消的后继结点
            if (t.waitStatus <= 0)
                s = t;
    }
    // 后继结点存在，则唤醒它
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

### 4.5.4 cancelAcquire
```java
/*
取消 node 的 acquire 尝试，将 node 状态设为 CANCELLED。
然后准备下一个节点的 acquire（将其连接到 SIGNAL 前驱节点上或唤醒它），让同步过程继续。

 - 如果 node 存在未取消的前驱节点 pred
   且 pred 不是头节点（是头节点的话 node 后面的节点就可以直接被唤醒去获取锁）
   且 pred.waitStatus 是 SIGNAL 或设置为 SIGNAL 成功
    - 如果 node 的后继节点存在且未被取消，则将 pred 的 next 指针 CAS 为这个后继节点，
      这样它以后就可以在 release 中被唤醒
 - 否则如果存在的话，唤醒 node 后面第一个没有被取消的结点，让它继续 acquire
*/
private void cancelAcquire(Node node) {
    // node 为 null 则直接忽略
    if (node == null)
        return;

    node.thread = null;

    // 跳过被取消的前驱节点（CANCELLED = 1）
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    Node predNext = pred.next;
    // 这里可以用直接写代替 CAS。在这个原子步骤之后，其他 Node 可以跳过 node。
    node.waitStatus = Node.CANCELLED;

    // 如果 node 是尾节点，则将其移出队列
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null);
    } else {
        /*
        如果 node 的后继结点需要 signal：
            - 尽量设置 pred 的 next 链接，连接这个后继节点，让它有机会得到信号。
        否则唤醒 node 后面第一个没有被取消的节点。
        */
        int ws;
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);
        } else {
            // 如果存在的话，唤醒 node 后面第一个没有被取消的节点，让它继续 acquire
            unparkSuccessor(node);
        }

        // 让被取消的节点的 next 指针指向自己
        node.next = node; // help GC
    }
}
```

## 4.6 独占锁方法

### 4.6.1 acquireQueued
```java
/*
对已经在同步队列中的节点 node 以独占、不被中断的模式进行 acquire。
被 Condition.await 方法和 acquire 方法使用。

如果等待期间发生中断，返回 true。
*/
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        // 循环尝试获取锁
        for (;;) {
            // 获取 node 的前驱节点
            final Node p = node.predecessor();
            // 如果 node 是真实头节点（虚拟头节点的后继节点），并且 tryAcquire 成功
            if (p == head && tryAcquire(arg)) {
                // 将 node 设为虚拟头节点，node 出队
                setHead(node);
                p.next = null; // help GC
                failed = false;
                // 返回是否发生中断
                return interrupted;
            }
            /*
            之前尝试 acquire 失败了。
             - 如果前驱节点 p 是 SIGNAL 状态，尝试 park 当前线程（调用此方法的线程，也是 node 的线程）。
               因为获取锁失败后，就需要被阻塞。之后某个线程对本线程 unpark（release 中）或当前线程被中断后，再往下运行。
             - 否则如果前驱节点被取消了，向前查找并更新 node 的前驱节点。
             - 否则尝试更新 node 的前驱节点状态为 SIGNAL，继续下次循环 acquire 或 park。

            如果成功 park 了当前线程并且随后当前线程被中断，则 interrupted 设为 true
            */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        // 如果失败（可能是因为发生了异常？），取消 node 的 acquire 尝试，将 node 状态设为 CANCELLED，
        // 并准备下一个节点的 acquire
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.6.2 acquire
```java
/*
以独占模式获取，忽略中断。

实现方式是至少调用一次 tryAcquire，成功后就返回，然后可以运行同步块代码。
否则线程排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquire，直到成功。

这个方法可以用来实现 Lock.lock。
*/
public final void acquire(int arg) {
    /*
    1. 首先调用 tryAcquire 方法，尝试在独占模式下进行获取。成功则返回，接下来就可以继续运行同步块中的代码。
       这里体现了非公平锁，每个线程获取锁时会尝试直接抢占加塞一次，而 CLH 队列中可能还有别的线程在等待。
    2. 如果失败，则调用 addWaiter 方法将当前线程（调用此方法的线程）封装成节点 node 并放入同步队列。
    3. 然后调用 acquireQueued 方法，此方法让 node 不断尝试获取，直到成功。如果过程中发生中断返回 true，否则返回 false。
    4. 如果 acquireQueued 返回 true，则重新中断当前线程
       （因为 acquireQueued 会将线程中断状态重置，而 acquire 是不响应的。为了防止中断状态丢失，需要再次中断）。
    */
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

### 4.6.3 doAcquireInterruptibly
```java
/*
对已经在同步队列中的节点 node 以独占、可以被中断的模式进行 acquire。
被 Condition.await 方法和 acquire 方法使用。

此方法被 acquireInterruptibly 方法使用。

如果等待期间发生中断，抛出异常。
*/
private void doAcquireInterruptibly(int arg) throws InterruptedException {
    // 将节点以独占模式添加到队列中
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        // 循环尝试获取锁
        for (;;) {
            // 获取 node 的前驱节点
            final Node p = node.predecessor();
            // 如果 node 是真实头节点（虚拟头节点的后继节点），并且 tryAcquire 成功
            if (p == head && tryAcquire(arg)) {
                // 将 node 设为虚拟头节点，node 出队
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return;
            }
            /*
            之前尝试 acquire 失败了。
             - 如果前驱节点 p 是 SIGNAL 状态，尝试 park 当前线程（调用此方法的线程，也是 node 的线程）。
               因为获取锁失败后，就需要被阻塞。之后某个线程对本线程 unpark（release 中）或当前线程被中断后，再往下运行。
             - 否则如果前驱节点被取消了，向前查找并更新 node 的前驱节点。
             - 否则尝试更新 node 的前驱节点状态为 SIGNAL，继续下次循环 acquire 或 park。

            如果成功 park 了当前线程并且随后当前线程被中断，则抛出异常
            */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } finally {
        // 如果失败（被中断），取消 node 的 acquire 尝试，将 node 状态设为 CANCELLED，
        // 并准备下一个节点的 acquire
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.6.4 acquireInterruptibly
```java
/*
以独占模式获取，如果被中断则中止。

实现方式是先检查中断状态，然后至少调用一次 tryAcquire，成功后返回并执行同步代码。
否则线程会排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquire，直到成功或线程被中断。

这个方法可以用来实现方法 Lock.lockInterruptibly。
*/
public final void acquireInterruptibly(int arg) throws InterruptedException {
    // 当前线程已被中断，则抛出异常
    if (Thread.interrupted())
        throw new InterruptedException();
    /*
    1. 调用 tryAcquire 方法，尝试在独占模式下进行获取。成功则返回，接下来就可以继续运行同步块中的代码。
       这里体现了非公平锁，每个线程获取锁时会尝试直接抢占加塞一次，而 CLH 队列中可能还有别的线程在等待。
    2. 失败的话调用 doAcquireInterruptibly 方法，此方法让 node 不断尝试获取，直到成功或发生中断。
       如果过程中发生中断则抛出异常
    */
    if (!tryAcquire(arg))
        doAcquireInterruptibly(arg);
}
```

### 4.6.5 doAcquireNanos
```java
/*
对已经在同步队列中的节点 node 以独占、可以被中断的模式进行 acquire。
如果超时就取消 node 并返回 false。

此方法被 tryAcquireNanos 方法使用。

如果等待期间发生中断，抛出异常。
*/
private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    // 超时时间小于等于 0 则直接返回 false
    if (nanosTimeout <= 0L)
        return false;
    // 计算截至日期
    final long deadline = System.nanoTime() + nanosTimeout;
    // 将节点以独占模式添加到队列中
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        // 循环尝试获取锁
        for (;;) {
            // 获取 node 的前驱节点
            final Node p = node.predecessor();
            // 如果 node 是真实头节点（虚拟头节点的后继节点），并且 tryAcquire 成功
            if (p == head && tryAcquire(arg)) {
                // 将 node 设为虚拟头节点，node 出队
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return true;
            }
            // 超时返回 false
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L)
                return false;
            /*
            之前尝试 acquire 失败了。
             - 如果前驱节点 p 是 SIGNAL 状态，尝试 park 当前线程（调用此方法的线程，也是 node 的线程）。
               因为获取锁失败后，就需要被阻塞。
               如果超时时间小于阈值 spinForTimeoutThreshold，则使用旋转进行等待；否则使用 parkNanos 对线程进行阻塞。
               之后某个线程对本线程 unpark（release 中）或当前线程被中断后，再往下运行。
             - 否则如果前驱节点被取消了，向前查找并更新 node 的前驱节点。
             - 否则尝试更新 node 的前驱节点状态为 SIGNAL，继续下次循环 acquire 或 park。
            */
            if (shouldParkAfterFailedAcquire(p, node) &&
                nanosTimeout > spinForTimeoutThreshold)
                LockSupport.parkNanos(this, nanosTimeout);
            // 当前线程被中断，则抛出异常
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    } finally {
        // 如果失败（超时或被中断），取消 node 的 acquire 尝试，将 node 状态设为 CANCELLED，
        // 并准备下一个节点的 acquire
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.6.6 tryAcquireNanos
```java
/*
以独占模式获取，如果被中断或超时则中止。

实现方式是先检查中断状态，然后至少调用一次 tryAcquire，成功后返回并执行同步代码。
否则线程会排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquire，直到成功、线程被中断或超时。

这个方法可以用来实现方法 Lock.tryLock(long，TimeUnit)。

@return 成功获取了锁则返回 true；超时返回 false
*/
public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    // 当前线程已被中断，则抛出异常
    if (Thread.interrupted())
        throw new InterruptedException();
    /*
    1. 调用 tryAcquire 方法，尝试在独占模式下进行获取。成功则返回，接下来就可以继续运行同步块中的代码。
       这里体现了非公平锁，每个线程获取锁时会尝试直接抢占加塞一次，而 CLH 队列中可能还有别的线程在等待。
    2. 失败的话调用 doAcquireNanos 方法，此方法让 node 不断尝试获取，直到成功或发生中断。如果过程中发生中断则抛出异常
    */
    return tryAcquire(arg) ||
        doAcquireNanos(arg, nanosTimeout);
}
```

### 4.6.7 release
```java
/*
独占模式下的释放方法。从头节点开始，尝试解锁一个线程。

这个方法可以用来实现方法 Lock.unlock。

返回是否释放成功。
*/
public final boolean release(int arg) {
    // tryRelease 释放锁，成功返回 true，此时其他线程可以尝试 acquire。
    if (tryRelease(arg)) {
        Node h = head;
        /*
        当同步队列不为空，或者虚拟头节点 waitStatus 不等于 0 时，尝试唤醒 head 后面第一个没有被取消的结点。
        头节点状态值不会等于 CANCELED(1)。
        
        后面的节点会在 acquire 中将前面的节点设为 SIGNAL，所以在这里才能通过 if 检查
        */
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

### 4.6.8 fullyRelease
```java
/*
以当前状态值调用 release，返回保存的状态。释放失败则将 node 状态设为 CANCELLED 并抛出异常。

此方法用在各种 Condition.await 方法中，node 是 Condition 队列里面的节点。
*/
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        // 以当前 getState() 的值为参数调用 release。
        // 如果返回 false 表示释放失败，可能因为之前还没有任何线程尝试获取锁。
        int savedState = getState();
        if (release(savedState)) {
            failed = false;
            return savedState;
        } else {
            // 释放失败将抛出异常
            throw new IllegalMonitorStateException();
        }
    } finally {
        // 释放失败将 node 的状态设为 CANCELLED
        if (failed)
            node.waitStatus = Node.CANCELLED;
    }
}
```

## 4.7 共享锁方法

### 4.7.1 doReleaseShared
```java
/*
共享模式的释放操作 -- 释放头节点的后继结点，或者记录共享锁的释放（传播）。

共享锁的传播性目的是尽快唤醒同步队列中等待的线程，使其尽快获取资源（锁）。但是也有一定的副作用，可能会造成不必要的唤醒。

注意，同步队列是独占模式和共享模式公用的。
对于独占模式的节点，即如果 head 是 Node.SIGNAL，此方法相当于对 head 调用 unparkSuccessor 方法。
*/
private void doReleaseShared() {
    /*
    如果 head 的后继节点需要 signal，则按照通常的方式尝试 unpark 它。
    但如果不需要，则将状态设置为 PROPAGATE，以确保在释放时，传播继续。
    
    为了确保释放操作被记录，能够正确地传播，如果头节点被改变了，就继续循环。
    */
    for (;;) {
        Node h = head;
        // 如果循环队列不为空
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            // state: SIGNAL -> 0
            // 如果 head 状态是 SIGNAL，需要唤醒一个后序未取消的节点
            if (ws == Node.SIGNAL) {
                // 重置状态。可能有其他线程会先调用 unparkSuccessor 方法将状态设为 0
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // 失败时继续循环设置状态
                // unpark head 后面的节点。它可能会 acquire 成功并将自己设为头节点；
                // 也可能唤醒后因为没有共享锁可以获取而再次阻塞
                unparkSuccessor(h);
            }
            // state: 0 -> PROPAGATE。
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // 失败时继续循环设置状态
        }
        // 可能被唤醒的节点立刻获取了锁出队列，导致 head 变了，所以继续循环唤醒 head 后继节点。
        if (h == head)
            break;
    }
}
```

### 4.7.2 setHeadAndPropagate
```java
/*
将 node 设置为队头，并检查如果后继节点在共享模式下等待，并且 propagate > 0 或有共享锁被释放，则进行传播。

@param propagate tryAcquireShared 的返回值，剩余资源数量，大于 0 表示后面的线程也可能获得锁
*/
private void setHeadAndPropagate(Node node, int propagate) {
    // 保存老的头节点
    Node h = head;
    // 将 node 设为新的头节点，现在 head == node
    setHead(node);

    /*
    进行以下判断，决定是否传播：
    1. 首先 h == null 和 (h = head) == null 是防止 NPE 的情况，几乎不可能出现，在这里我们可以忽略。
    2. 如果 propagate > 0 成立的话，说明还有剩余资源可以获取
    3. 否则 propagate == 0，没有共享资源可用。按理说不需要唤醒后继的。也就是说，很多情况下，调用 doReleaseShared，
       会造成不必要的唤醒。之所以说不必要，是因为唤醒后因为没有共享锁可以获取而再次阻塞了。
    4. 如果 propagate > 0 不成立，而 h.waitStatus < 0 成立，这说明旧 head 的 status < 0。
       - 因为 head 的 status 为 0 代表一种中间状态（head 的后继节点已经唤醒，但它还没有获取到资源）；或者 head 等于 tail。
         旧 head == 0，只可能是因为 node 在 doAcquireShared 中将 head 设为 SIGNAL，然后 head 在 doReleaseShared 中
         唤醒了 node 并将自己设为 0。于是 node 才会进入 setHeadAndPropagate 方法中。
       - 所以旧 head 的 status < 0，只能是由于 doReleaseShared 里的 0 -> PROPAGATE 操作。
         而且由于当前执行 setHeadAndPropagate 的线程只会在最后一句才执行 doReleaseShared，所以出现这种情况，
         一定是因为有另一个线程在调用 doReleaseShared 才能造成。而这很可能是因为在中间状态时，又有人释放了共享锁。
         「propagate == 0 只能代表当时 tryAcquireShared 后没有资源剩余，但之后的时刻很可能又有共享锁释放出来了」。
    5. 如果之前的都不成立，第二个 h.waitStatus < 0 成立。这个条件成立，可能是因为：
       - 又有其他线程释放锁调用 doReleaseShared，把新 head 的 waitStatus 改为了 PROPAGATE。
         此时又有共享锁释放出来。
       - 新 head 的后继线程在 shouldParkAfterFailedAcquire 方法中把新 head 的 waitStatus 改为了 SIGNAL。
         这种情况会造成不必要的唤醒，因为没有共享锁释放，唤醒的后继线程很快又会被阻塞。不过这种情况是可以容忍的。
    */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}
```

### 4.7.3 doAcquireShared
```java
/*
对已经在同步队列中的节点 node 以共享、不被中断的模式进行 acquire。

此方法被 acquireShared 方法使用。

如果等待期间发生中断，则在成功获取锁后重新中断当前线程。
*/
private void doAcquireShared(int arg) {
    // 将节点以共享模式添加到队列中
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        // 循环尝试获取锁
        boolean interrupted = false;
        for (;;) {
            // 获取 node 的前驱节点
            final Node p = node.predecessor();
            // 如果 node 是真实头节点（虚拟头节点的后继节点）
            if (p == head) {
                int r = tryAcquireShared(arg);
                // 如果 tryAcquireShared 成功，返回剩余资源数量 r
                if (r >= 0) {
                    // 将 node 设为虚拟头节点，node 出队。
                    // 如果后继节点在共享模式下等待，并且 r > 0 或有共享锁被释放，则进行传播
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        // 因为 parkAndCheckInterrupt 会将线程中断状态重置，为了防止中断状态丢失，需要再次中断
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            /*
            之前尝试 acquire 失败了。
             - 如果前驱节点 p 是 SIGNAL 状态，尝试 park 当前线程（调用此方法的线程，也是 node 的线程）。
               因为获取锁失败后，就需要被阻塞。之后某个线程对本线程 unpark（release 中）或当前线程被中断后，再往下运行。
             - 如果前驱节点被取消了，向前查找并更新 node 的前驱节点。
             - 否则尝试更新 node 的前驱节点状态为 SIGNAL，继续下次循环 acquire 或 park。
             - 连接到 SIGNAL 前驱节点或将前驱节点设为 SIGNAL 很重要，这样 node 就可以在 releaseShared 中被 unpark

            如果成功 park 了当前线程并且随后当前线程被中断，则 interrupted 设为 true
            */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        // 如果失败（可能是因为发生了异常？），取消 node 的 acquire 尝试，将 node 状态设为 CANCELLED，
        // 并准备下一个节点的 acquire
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.7.4 acquireShared
```java
/*
以共享模式获取，忽略中断。

实现方式是先调用至少一次 tryAcquireShared，成功后返回并执行同步代码。
否则线程排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquireShared，直到成功。
*/
public final void acquireShared(int arg) {
    /*
    1. 首先调用 tryAcquireShared 方法，尝试在共享模式下进行获取。成功则返回，接下来就可以继续运行同步块中的代码。
    2. 失败后调用 doAcquireShared 方法：
       - 此方法首先创建一个共享模式的节点 node 入队，然后让 node 不断尝试获取，直到成功。
       - 如果过程中发生中断则在获取成功后调用 selfInterrupt，保证中断状态不丢失。
       - 获取成功后如果条件符合，还会尝试唤醒在共享模式下等待的后继节点。
    */
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

### 4.7.5 releaseShared
```java
// 在共享模式下释放。如果 tryReleaseShared 返回 true，则解锁一个或多个等待的线程。
public final boolean releaseShared(int arg) {
    // tryReleaseShared 尝试返还资源，成功后调用 doReleaseShared() 唤醒等待线程或记录资源的释放
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

### 4.7.6 工作原理说明

假设我们使用 `Semaphore`，它使用了 AQS 来完成资源的共享的分配和释放。首先 `Semaphore` 初始化 `state` 值为 0（模拟一个资源被分配完的场景），
然后 4 个线程分别运行 4 个任务。线程 `t1,t2` 获取锁，另外两个线程 `t3,t3` 释放锁，如下所示：
```java
public class TestSemaphore {

  // 这里将信号量设置成了 0
  private static Semaphore sem = new Semaphore(0);

  private static class Thread1 extends Thread {
    @Override
    public void run() {
      // 获取锁
      sem.acquireUninterruptibly();
    }
  }

  private static class Thread2 extends Thread {
    @Override
    public void run() {
      // 释放锁
      sem.release();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    for (int i = 0; i < 10000000; i++) {
      Thread t1 = new Thread1();
      Thread t2 = new Thread1();
      Thread t3 = new Thread2();
      Thread t4 = new Thread2();
      t1.start();
      t2.start();
      t3.start();
      t4.start();
      t1.join();
      t2.join();
      t3.join();
      t4.join();
      System.out.println(i);
    }
  }
}
```

根据上面的代码，我们将信号量设置为 0，所以 `t1,t2` 获取锁会失败。假设某次循环中队列中的情况如下：
```
head(SIGNAL) --> t1 --> t2(tail)
```

锁的释放由 `t3` 先释放，`t4` 后释放。在以下时刻有不同的情况：
1. **时刻 1**: 线程 `t3` 调用 `releaseShared()`，然后唤醒队列中节点(线程 `t1`)，此时 `head` 的状态从 `SIGNAL` 变成 0
（`releaseShared()` 循环中的第一个条件判断）。
2. **时刻 2**: 线程 `t1` 由于线程 `t3` 释放了锁，被 `t3` 唤醒，然后取得 `propagate` 值为 0。
3. **时刻 3:** 线程 `t4` 调用 `releaseShared()`，读到此时 `h.waitStatu` 为 0(假设此时 `head` 还未被替换，和时刻 1 中是同一个 `head`),
不满足条件，因此不唤醒后继节点。
4. **时刻 4**: 线程 `t1` 获取锁成功，它自己成为头节点，调用 `setHeadAndPropagate()`，此时不满足 `propagate > 0`(时刻 2 中 `propagate == 0`)。

如果没有 `PROPAGATE` 状态，那么时刻 3 中就没有 `releaseShared()` 循环中的第二个条件判断。那么 `h.waitStatu` 就为 0，
从而时刻 4 中就会导致线程 `t2` 不会被唤醒。就会出现释放两次，但有一个线程未被唤醒，一直阻塞。

那在引入了 `PROPAGATE` 之后又会是怎样的情况呢？此时时刻 3 中就会将 `h.waitStatu` 设为 `PROPAGATE(-3)`，
那么时刻 4 中虽然不满足 `propagate > 0`，但是 `h.waitStatu < 0`，这样线程 `t2` 就能够正常被唤醒。
**可以看到，引入了  `PROPAGATE` 之后，不会再错失“锁被释放这一事件**。

至此我们知道了 `PROPAGATE` 的作用，就是为了避免线程无法会唤醒的窘境。因为共享锁会有很多线程获取到锁或者释放锁，
所以有些方法是并发执行的，就会产生很多中间状态，而 `PROPAGATE` 就是为了让这些中间状态不影响程序的正常运行。

### 4.7.7 doAcquireSharedInterruptibly
```java
/*
对已经在同步队列中的节点 node 以共享、可被中断的模式进行 acquire。

此方法被 acquireSharedInterruptibly 方法使用。

如果等待期间发生中断，则抛出异常。
*/
private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }
            // 发生异常直接抛出
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.7.8 acquireSharedInterruptibly
```java
/*
以共享模式获取，响应中断抛出异常。

实现方式是先调用至少一次 tryAcquireShared，成功后返回并执行同步代码。
否则线程排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquireShared，直到成功或发生异常。
*/
public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```

### 4.7.9 doAcquireSharedNanos
```java
/*
对已经在同步队列中的节点 node 以共享、可以被中断的模式进行 acquire。
如果超时就取消 node 并返回 false。

此方法被 tryAcquireSharedNanos 方法使用。

如果等待期间发生中断，抛出异常。
*/
private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (nanosTimeout <= 0L)
        return false;
    final long deadline = System.nanoTime() + nanosTimeout;
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L)
                return false;
            if (shouldParkAfterFailedAcquire(p, node) &&
                nanosTimeout > spinForTimeoutThreshold)
                LockSupport.parkNanos(this, nanosTimeout);
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

### 4.7.10 tryAcquireSharedNanos
```java
/*
以共享模式获取，如果被中断或超时则中止。

实现方式是先检查中断状态，然后至少调用一次 tryAcquireShared，成功后返回并执行同步代码。
否则线程会排队，可能会反复阻塞和解除阻塞，并不断调用 tryAcquireShared，直到成功、线程被中断或超时。

@return 成功获取了锁则返回 true；超时返回 false
*/
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    return tryAcquireShared(arg) >= 0 ||
        doAcquireSharedNanos(arg, nanosTimeout);
}
```

## 4.8 检查、工具和监控方法

### 4.8.1 toString
```java
public String toString() {
    int s = getState();
    // 是否有等待的线程
    String q  = hasQueuedThreads() ? "non" : "";
    return super.toString() +
        "[State = " + s + ", " + q + "empty queue]";
}
```

### 4.8.2 hasQueuedThreads
```java
/*
查询是否有线程在同步队列中等待。请注意，由于中断和超时导致的取消操作可能随时发生，
所以返回 true 并不能保证有线程等待 acquire。

在本实现中，该操作以常数时间返回。
*/
public final boolean hasQueuedThreads() {
    return head != tail;
}
```

### 4.8.3 hasContended
```java
/*
查询是否有线程曾经争夺过这个同步器；也就是说，是否有一个 acquire 方法曾经阻塞过。
在本实现中，该操作以常数时间返回。
*/
public final boolean hasContended() {
    return head != null;
}
```

### 4.8.4 getFirstQueuedThread
```java
/*
返回队列中第一个（等待时间最长的）线程，如果当前没有线程排队，则返回 null。
在这个实现中，这个操作通常以常数时间返回；但如果其他线程正在并发修改队列，则可能会在争用时迭代。
*/
public final Thread getFirstQueuedThread() {
    return (head == tail) ? null : fullGetFirstQueuedThread();
}

private Thread fullGetFirstQueuedThread() {
    /*
    第一个节点通常是 head.next，尽量获取它的线程字段。
    如果线程字段被清空或者 s.prev 不再是 head，那么说明有其他线程就会在我们的一些读取操作之间同时执行 setHead。
    在使用遍历之前，我们先试两次。
    */
    Node h, s;
    Thread st;
    if (((h = head) != null && (s = h.next) != null &&
         s.prev == head && (st = s.thread) != null) ||
        ((h = head) != null && (s = h.next) != null &&
         s.prev == head && (st = s.thread) != null))
        return st;

    /*
    head 的 next 字段可能还没有被设置，或者在 setHead 之后被设为 null。
    所以我们必须检查 tail 是否真的是第一个节点。
    如果不是，我们就 tail 后向遍历到 head，找到第一个节点，获得它的线程。

    之所以反向遍历，是因为被取消的节点的 next 指针指向它自己。它在被从队列中清除之前会干扰正向遍历。
    */
    Node t = tail;
    Thread firstThread = null;
    while (t != null && t != head) {
        Thread tt = t.thread;
        if (tt != null)
            firstThread = tt;
        t = t.prev;
    }
    return firstThread;
}
```

### 4.8.5 apparentlyFirstQueuedIsExclusive
```java
/*
如果第一个排队的线程（如果存在的话）以独占模式等待，则返回 true。

如果这个方法返回 true，并且当前线程正试图以共享模式获取（也就是说调用了 tryAcquireShared），
那么保证当前线程不是第一个排队的线程。

此方法仅在 ReentrantReadWriteLock 中启发式地使用。
*/
final boolean apparentlyFirstQueuedIsExclusive() {
    Node h, s;
    return (h = head) != null &&
        (s = h.next)  != null &&
        !s.isShared()         &&
        s.thread != null;
}
```

### 4.8.6 isQueued
```java
// 查询给定线程是否存在于队列中
public final boolean isQueued(Thread thread) {
    // 确保 thread 不为 null
    if (thread == null)
        throw new NullPointerException();
    // 从后往前遍历，查询 thread 释放在同步队列中。
    // 之所以反向遍历，是因为被取消的节点的 next 指针指向它自己。它在被从队列中清除之前会干扰正向遍历。
    for (Node p = tail; p != null; p = p.prev)
        if (p.thread == thread)
            return true;
    return false;
}
```

### 4.8.7 hasQueuedPredecessors
```java
/*
查询是否有线程等待 acquire 的时间比当前线程长。这个方法的调用相当于（但可能更高效）：
    getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()

请注意，由于中断和超时导致的取消可能随时发生，所以返回 true 并不能保证其他一些线程会比当前线程先获取。
同样，在这个方法返回 false 之后，有可能因为队列是空的，另一个线程有可能比当前线程先获取。

这个方法被设计成由一个公平的同步器使用，以避免竞争。但此方法返回 true 时，这样的同步器的 tryAcquire 方法应该返回 false。
如果这个方法返回 true，它的 tryAcquireShared 方法应该返回一个负值（除非这是一个已获得锁的线程的重入式获取）。

例如，一个公平、可重入、独占模式的同步器的 tryAcquire 方法可能是这样的：
 protected boolean tryAcquire(int arg) {
   if (isHeldExclusively()) {
     // A reentrant acquire; increment hold count
     return true;
   } else if (hasQueuedPredecessors()) {
     return false;
   } else {
     // try to acquire normally
   }
 }
*/
public final boolean hasQueuedPredecessors() {
    /*
    以初始化的反向顺序读取字段。
    这一点的正确性取决于 head 在 tail 之前被初始化，以及如果当前线程是队列中的第一个线程，那么 head.next 是正确的。
    */
    Node t = tail;
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

### 4.8.8 getQueueLength
```java
// 返回队列中等待线程的估计值。此方法被用于监控系统状态，而不是用来做同步控制。
public final int getQueueLength() {
    int n = 0;
    for (Node p = tail; p != null; p = p.prev) {
        // 注意 head 不会被计算在内，因为 head.thread == null（在 setHead() 中被设置）。
        if (p.thread != null)
            ++n;
    }
    return n;
}
```

### 4.8.9 getQueuedThreads
```java
/*
返回一个包含可能正在等待获取的线程的集合。因为在构造这个结果时，实际的线程集可能会动态变化，
所以返回的集合只是一个尽力估计的结果。返回的集合中的元素没有特定的顺序。

这个方法的设计是为了方便构造提供更广泛监控设施的子类。
*/
public final Collection<Thread> getQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
        Thread t = p.thread;
        if (t != null)
            list.add(t);
    }
    return list;
}
```

### 4.8.10 getExclusiveQueuedThreads
```java
// 类似于 getQueuedThreads，但返回的集合中只包含在独占模式下等待的线程。
public final Collection<Thread> getExclusiveQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
        if (!p.isShared()) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
    }
    return list;
}
```

### 4.8.11 getSharedQueuedThreads
```java
// 类似于 getQueuedThreads，但返回的集合中只包含在共享模式下等待的线程。
public final Collection<Thread> getSharedQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
        if (p.isShared()) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
    }
    return list;
}
```

### 4.8.12 owns
```java
// 查询给定的 ConditionObject 对象是不是属于这个同步器
public final boolean owns(ConditionObject condition) {
    return condition.isOwnedBy(this);
}
```

### 4.8.13 hasWaiters
```java
// 查询给定的 ConditionObject 对象有没有等待的线程。此方法不是精确的。
public final boolean hasWaiters(ConditionObject condition) {
    if (!owns(condition))
        throw new IllegalArgumentException("Not owner");
    return condition.hasWaiters();
}
```

### 4.8.14 getWaitQueueLength
```java
// 查询给定的 ConditionObject 对象等待的线程的估计数量。
public final int getWaitQueueLength(ConditionObject condition) {
    if (!owns(condition))
        throw new IllegalArgumentException("Not owner");
    return condition.getWaitQueueLength();
}
```

### 4.8.15 getWaitingThreads
```java
// 查询给定的 ConditionObject 对象等待的线程的“快照”集合。
public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
    if (!owns(condition))
        throw new IllegalArgumentException("Not owner");
    return condition.getWaitingThreads();
}
```


[clh]: http://www.cs.rochester.edu/u/scott/synchronization/
[condition]: Condition.md