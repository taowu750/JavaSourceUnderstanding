# 1. 定义

Java 并发的核心机制是 `Thread` 类，当你创建一个 `Thread` 时，JVM 将分配一块内存作为此线程的私有内存，
用于提供运行任务时所需的一切，包括：
 - 程序计数器：指明要执行的下一个 JVM 字节码指令。
 - 虚拟机栈：用于支持 Java 代码执行的栈，存放有局部变量表、操作数栈、动态连接、方法出口等信息。
 - 本地方法栈：为虚拟机使用到的本地（Native）方法服务。
 - 线程本地变量（thread-local）的存储区域。
 - 用于控制线程的状态管理变量。

除此以外，线程必须绑定到操作系统，这样它就可以在某个时候连接到处理器。这是作为线程构建过程的一部分为你管理的。
Java 使用底层操作系统中的机制来管理线程的执行。

线程创建后可以调用 `Thread.start()` 方法启动线程。

# 2. 上下文切换

多线程编程中一般线程的个数都大于 CPU 核心的个数，而一个 CPU 核心在任意时刻只能被一个线程使用，为了让这些线程都能得到有效执行，
CPU 采取的策略是为每个线程分配时间片并轮转的形式。当一个线程的时间片用完的时候就会重新处于就绪状态让给其他线程使用，
这个过程就属于一次上下文切换。

概括来说就是：当前任务在执行完 CPU 时间片切换到另一个任务之前会先保存自己的状态，以便下次再切换回这个任务时，
可以再加载这个任务的状态。**任务从保存到再加载的过程就是一次上下文切换**。

上下文切换通常是计算密集型的。也就是说，它需要相当可观的处理器时间，在每秒几十上百次的切换中，每次切换都需要纳秒量级的时间。
所以，上下文切换对系统来说意味着消耗大量的 CPU 时间，事实上，可能是操作系统中时间消耗最大的操作。

Linux 相比与其他操作系统（包括其他类 Unix 系统）有很多的优点，其中有一项就是，其上下文切换和模式切换的时间消耗非常少。

# 3. 线程的使用方式

有三种使用线程的方法:
 - 实现 `Runnable` 接口；
 - 实现 `Callable` 接口；
 - 继承 `Thread` 类。

实现 `Runnable` 和 `Callable` 接口的类只能当做一个可以在线程中运行的任务，不是真正意义上的线程，因此最后还需要通过 `Thread` 来调用。
可以说任务是通过线程驱动从而执行的。

其中，`Runnable` 没有返回值，也不能抛出受检查的异常；而 `Callable` 可以有返回值，也能够抛出受检查的异常，
返回值通过 `FutureTask` 进行封装。

因为 `Thread` 类也实现了 `Runnable` 接口，所以可以通过继承 `Thread` 类来使用线程。但实现接口会更好一些，因为:
 - Java 不支持多重继承，因此继承了 `Thread` 类就无法继承其它类，但是可以实现多个接口；
 - 任务可能只要求可执行就行，继承整个 Thread 类开销过大。
 
# 4. 基础线程机制

## 4.1 Executor

`Executor` 是线程池，它管理多个异步任务的执行，而无需程序员显式地管理线程的生命周期。这里的异步是指多个任务的执行互不干扰，
不需要进行同步操作。主要有三种 `Executor`:
 - `CachedThreadPool`: 一个任务创建一个线程；
 - `FixedThreadPool`: 线程池大小是固定的；
 - `SingleThreadExecutor`: 相当于大小为 1 的 `FixedThreadPool`。
 - `ScheduledExecutorService`：创建一个支持定时及周期性的任务执行的线程池，多数情况下可用来替代 `Timer` 类。

下面是一个示例：
```java
import java.util.concurrent.*;
import java.util.stream.*;

public class SingleThreadExecutor {

    public static void main(String[] args) {
        ExecutorService exec =
            Executors.newSingleThreadExecutor();
        IntStream.range(0, 10)
            .mapToObj(NapTask::new)
            .forEach(exec::execute);
        System.out.println("All tasks submitted");
        exec.shutdown();
        while(!exec.isTerminated()) {
            System.out.println(
            Thread.currentThread().getName()+
            " awaiting termination");
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }
}
```

当向线程池提交 `Callable` 时，使用 `submit()` 方法并返回一个 `Future` 对象。也可以使用 `invokeAll()` 方法，
启动集合中的每个 `Callable`，并返回 `Future` 集合。

## 4.2 sleep

`Thread.sleep(millisec)` 方法会休眠当前正在执行的线程，`millisec` 单位为毫秒。 `sleep()` 被中断会抛出 `InterruptedException`。

Java SE5 中引入了 `TimeUnit` 类，它也有 `sleep()` 方法，并且可以显式地指定 `sleep()` 延迟的时间单元，
因此可以提供更好的可阅读性。`TimeUnit` 还可以被用来执行时间转换。

## 4.3 yield

对静态方法 `Thread.yield()` 的调用声明了当前线程已经完成了生命周期中最重要的部分，可以切换给其它线程来执行。
该方法只是对线程调度器的一个建议，而且也只是建议具有相同优先级的其它线程可以运行，这完全是可选并且随机的。

## 4.4 优先级

线程的优先级将该线程的重要性传递给了调度器。尽管 CPU 处理现有线程集的顺序是不确定的，但是调度器将倾向于让优先权最高的线程先执行。
然而，这并不是意味着优先权较低的线程将得不到执行（也就是说，优先权不会导致死锁）。优先级较低的线程仅仅是执行的频率较低。

在绝大多数时间里，所有线程都应该以默认的优先级运行。**试图操纵线程优先级通常是一种错误**。

你可以用 `Thread.getPriority()` 来读取现有线程的优先级，并且在任何时刻都可以通过 `Thread.setPriority()` 来修改它。

Java 提供了 10 个优先级，但它与多数操作系统都不能映射得很好。比如，Windows 有 7 个优先级且不是固定的，
所以这种映射关系也是不确定的。Sun 的 Solaris 有 2<sup>31</sup> 个优先级。唯一可移植的方法是当调整优先级的时候，
只使用 `MAX_PRIORITY`、`NORM_PRIORITY` 和 `MIN_PRIORITY` 三种级别。

## 4.5 后台线程

所谓后台（daemon）线程（或叫守护线程），是指在程序运行的时候在后台提供一种通用服务的线程，并且这种线程并不属于程序中不可或缺的部分。
因此，当所有的非后台线程结束时，程序也就终止了，同时会杀死进程中的所有后台线程。反过来说，只要有任何非后台线程还在运行，程序就不会终止。

比如，执行 `main()` 的就是一个非后台线程，而 GC 回收的线程就是一个后台线程。

可以使用 `Thread.setDaemon()` 方法将一个线程设置为后台线程，注意要在 `start()` 方法之前使用才能把它设置为后台线程。
可以通过调用 `Thread.isDaemon()` 方法来确定线程是否是一个后台线程。如果是一个后台线程，那么它创建的任何线程将被自动设置成后台线程。

如果需要使用 `Executor` 类创建后台线程，则需要实现 `ThreadFacttory` 接口。下面是一个例子：
```java
public class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);

        return t;
    }
}

public class DaemonFromFactory {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService exec = Executors.newCachedThreadPool(new DaemonThreadFactory());
        for (int i = 0; i < 10; i++) {
            exec.execute(new SimpleDaemons());
        }
        System.out.println("All daemon started");
        TimeUnit.MILLISECONDS.sleep(175);
    }
}
```

## 4.6 异常捕获

在 `Thread.run()` 方法中未捕获的运行时异常会“逃逸”到 `main()` 方法中。即使在 `main()` 方法体内包裹 `try-catch` 代码块来捕获异常也不成功。

为解决这个问题，需要使用 `Thread.UncaughtExceptionHandler` 接口。它是一个添加给每个 `Thread` 对象，用于进行异常处理的接口。
当该线程即将死于未捕获的异常时，将自动调用它的 `uncaughtException()` 方法。

为了调用该方法，我们创建实现 `ThreadFactory` 接口来让 `Thread.UncaughtExceptionHandler` 对象附加到每个它所新创建的 `Thread`（线程）对象上。
然后将该工厂传递给 `Executor`。下面是一个例子：
```java
class ExceptionThread implements Runnable {

  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("run() by " + t.getName());
    System.out.println(
      "eh = " + t.getUncaughtExceptionHandler());
    throw new RuntimeException();
  }
}

class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    System.out.println("caught " + e);
  }
}

public class CaptureUncaughtException {

  public static void main(String[] args) {
    ExecutorService exec = Executors.newCachedThreadPool(new HandlerThreadFactory());
    exec.execute(new ExceptionThread());
    exec.shutdown();
  }
}
```

也可以使用 `Thread.setDefaultUncaughtExceptionHandler` 方法为线程类提供一个默认的异常处理器。
只有在每个线程没有设置异常处理器时候，默认处理器才会被调用。系统会检查线程专有的版本，如果没有，
则检查是否线程组中有专有的 `uncaughtException()` 方法；如果都没有，就会调用 `defaultUncaughtExceptionHandler` 方法。

## 4.7 线程中断

可以通过调用一个线程的 `interrupt()` 来中断该线程，如果该线程处于阻塞、限期等待或者无限期等待状态，
那么就会抛出 `InterruptedException`，从而提前结束该线程。但是不能中断 I/O 阻塞和 `synchronized` 锁阻塞。

需要注意，`interrupt()` 不能中断在运行中的线程，它只能改变中断状态而已。
当抛出 `InterruptedException` 或者调用 `Thread.interrupted()` 检查中断状态时，中断状态将被复位。

调用 `Executor` 的 `shutdown()` 方法会等待线程都执行完毕之后再关闭线程池，不再接受新的任务；
但是如果调用的是 `shutdownNow()` 方法，则相当于调用每个线程的 `interrupt()` 方法。

如果只想中断 `Executor` 中的一个线程，可以通过使用 `submit()` 方法来提交一个线程，它会返回一个 `Future<?>` 对象，
通过调用该对象的 `cancel(true)` 方法就可以中断线程。

# 5. 线程状态转换

Java 线程总共有 6 种状态，这六种状态在 `java.lang.Thread.State` 枚举类中定义，
可以调用线程 `Thread.getState()` 方法获取当前线程的状态。

| 线程状态 | 解释说明 |
| ------- | -------- |
| NEW | 尚未启动的线程状态，即线程创建，还未调用 start 方法 |
| RUNNABLE | 就绪状态（调用start，等待调度） + 正在运行 |
| BLOCKED | 等待监视器锁时，陷入阻塞状态 |
| WAITING | WAITING 状态的线程正在等待另一线程执行特定的操作（如notify） |
| TIMED_WAITING | 具有指定等待时间的等待状态 |
| TERMINATED | 线程完成执行，终止状态 |

下图说明了线程状态之间的转换：

![线程状态转换][thread-state]

## 5.1 新建状态(NEW)

即用 `new` 关键字新建一个线程，这个线程就处于新建状态。

## 5.2 运行状态(RUNNABLE)

操作系统中的就绪(READY)和运行(RUNNING)两种状态，在 Java 中统称为 RUNNABLE。处于 RUNNABLE 状态下的线程正在 Java 虚拟机中执行，
但它可能正在等待来自于操作系统的其它资源，比如处理器。

### 5.2.1 就绪状态(READY)

当线程对象调用了 `start()` 方法之后，线程处于就绪状态，就绪意味着该线程可以执行，但具体啥时候执行将取决于 JVM 里线程调度器的调度。
此外，不允许对一个线程多次使用 `start`；线程执行完成之后，不能试图用 `start` 将其唤醒。

### 5.2.2 其他状态->就绪

 - 线程调用 `start()`，新建状态转化为就绪状态。
 - 线程 `sleep(long)` 时间到，等待状态转化为就绪状态。
 - 进行阻塞式 IO 操作，线程变为就绪状态。
 - 其他线程调用 `join()` 方法，结束之后转化为就绪状态。
 - 线程对象拿到对象锁之后，也会进入就绪状态。
 
### 5.2.3 运行状态(RUNNING)

处于就绪状态的线程获得了 CPU 之后，真正开始执行 `run()` 方法的线程执行体时，意味着该线程就已经处于运行状态。
需要注意的是，对于单处理器，一个时刻只能有一个线程处于运行状态。

对于抢占式策略的系统来说，系统会给每个线程一小段时间处理各自的任务。时间用完之后，系统负责夺回线程占用的资源。下一段时间里，
系统会根据一定规则，再次进行调度。

运行状态转变为就绪状态的情形：
 - 线程失去处理器资源。线程不一定完整执行的，执行到一半，说不定就被别的线程抢走了。
 - 调用 `yield()` 静态方法，暂时暂停当前线程，让系统的线程调度器重新调度一次，它自己完全有可能再次运行。
 - 进行阻塞式 IO 操作，线程变为就绪状态。
 
### 5.2.4 为什么要合并 READY 和 RUNNING

有人常觉得 Java 线程状态中还少了个 RUNNING 状态，这其实是把两个不同层面的状态混淆了。我们可能会问，为何 JVM 中没有去区分这两种状态呢？

因为时间分片通常是很小的，一个线程一次最多只能在 cpu 上运行比如 10-20ms 的时间（此时处于 RUNNING 状态），
也即大概只有 0.01 秒这一量级，时间片用后就要被切换下来放入调度队列的末尾等待再次调度。（也即回到 READY 状态）。
如果期间进行了 I/O 的操作还会导致提前释放时间分片，并进入等待队列。又或者是时间分片没有用完就被抢占，这时也是回到 READY 状态。

通常，**Java 的线程状态是服务于监控的**，如果线程切换得是如此之快，那么区分 READY 与 RUNNING 就没什么太大意义了。
现今主流的 JVM 实现都把 Java 线程一一映射到操作系统底层的线程上，把调度委托给了操作系统，
我们在虚拟机层面看到的状态实质是对底层状态的映射及包装。JVM 本身没有做什么实质的调度，
把底层的 READY 及 RUNNING 状态映射上来也没多大意义，因此，统一成为 RUNNABLE 状态是不错的选择。

## 5.3 阻塞状态(BLOCKED)

阻塞状态表示线程正等待监视器锁，而陷入的状态。以下场景线程将会阻塞：
 - 线程等待进入 `synchronized` 同步方法。
 - 线程等待进入 `synchronized` 同步代码块。
 - 进入锁对象的同步区。

线程取得锁，就会从阻塞状态转变为就绪状态。

## 5.4 等待状态(WAITING)

进入该状态表示当前线程需要等待其他线程做出一些的特定的动作（通知或中断）。

### 5.4.1 运行->等待

 - 当前线程运行过程中，其他线程调用 `Thread.join()` 方法，当前线程将会进入等待状态。
 - 当前线程对象调用 `Object.wait()` 方法。
 - 对线程调用 `LockSupport.park(Thread)` 方法，会出于线程调度的目的禁用当前线程。
 
### 5.4.2 等待->就绪

 - 等待的线程被其他线程对象唤醒，`Object.notify()` 和 `notifyAll()`。
 - `LockSupport.unpark(Thread)`，与上面 `park` 方法对应，给出许可证，解除等待状态。
 
## 5.5 超时等待状态(TIMED_WAITING)

区别于 `WAITING`，它可以在指定的时间自行返回。

### 5.5.1 运行->超时等待

 - 调用静态方法，`Thread.sleep(long)`
 - 线程对象调用 `Object.wait(long)` 方法
 - 其他线程调用指定时间的 `Thread.join(long)`。
 - `LockSupport.parkNanos()`。
 - `LockSupport.parkUntil()`。

注意，`sleep` 方法和 `yield` 方法不会释放锁，而 `wait` 方法会释放锁。`wait()` 通常被用于线程间交互/通信，`sleep()` 通常被用于暂停执行。

### 5.5.2 超时等待->就绪

 - 等待时间结束。
 - 同样的，等待的线程被其他线程对象唤醒，`notify()` 和 `notifyAll()`。
 - `LockSupport.unpark(Thread)`。

## 5.6 消亡状态

即线程的终止，表示线程已经执行完毕。前面已经说了，已经消亡的线程不能通过 `start` 再次唤醒。
 - `run()` 和 `call()` 线程执行体中顺利执行完毕，线程正常终止。
 - 线程抛出一个没有捕获的 `Exception` 或 `Error`。

需要注意的是：主线程和子线程互不影响，子线程并不会因为主线程结束就结束。

# 6. 线程互斥同步

Java 提供了两种锁机制来控制多个线程对共享资源的互斥访问，第一个是 JVM 实现的 `synchronized`，
而另一个是 JDK 实现的 `ReentrantLock`。

## 6.1 synchronized

### 6.1.1 同步方法
```java
// 获取当前对象的锁
public synchronized void func () {
    // ...
}

// 获取当前类的锁
public synchronized static void func () {
    // ...
}
```

### 6.1.2 同步代码块
```java

public void func () {
    // 获取当前对象的锁
    synchronized(this) {
        // ...
    }
}

public void func () {
    // 获取其它对象的锁
    Object obj = new Object();
    synchronized(obj) {
        // ...
    }
}


public synchronized static void func () {
    // 获取类的锁
    synchronized(SynchronizedExample.class) {
        // ...
    }
}
```

## 6.2 ReentrantLock

`ReentrantLock` 是 `java.util.concurrent`(J.U.C)包中的锁。下面是一个示例：
```java
public class LockExample {

    private Lock lock = new ReentrantLock();

    public void func() {
        lock.lock();
        try {
            for (int i = 0; i < 10; i++) {
                System.out.print(i + " ");
            }
        } finally {
            lock.unlock(); // 确保释放锁，从而避免发生死锁。
        }
    }
}
```

## 6.3 比较

### 6.3.1 两者都是可重入锁

“可重入锁” 指的是自己可以再次获取自己的内部锁。比如一个线程获得了某个对象的锁，此时这个对象锁还没有释放，
当其再次想要获取这个对象的锁的时候还是可以获取的，如果不可锁重入的话，就会造成死锁。同一个线程每次获取锁，
锁的计数器都自增 1，所以要等到锁的计数器下降为 0 时才能释放锁。

### 6.3.2 synchronized 依赖于 JVM 而 ReentrantLock 依赖于 API

`synchronized` 是依赖于 JVM 实现的，前面我们也讲到了 虚拟机团队在 JDK1.6 为 `synchronized` 关键字进行了很多优化，
但是这些优化都是在虚拟机层面实现的，并没有直接暴露给我们。`ReentrantLock` 是 JDK 层面实现的
（也就是 API 层面，需要 `lock()` 和 `unlock()` 方法配合 `try/finally` 语句块来完成），所以我们可以通过查看它的源代码，来看它是如何实现的。

### 6.3.3 ReentrantLock 比 synchronized 增加了一些高级功能

 - **等待可中断**: `ReentrantLock` 提供了一种能够中断等待锁的线程的机制，通过 `lock.lockInterruptibly()` 来实现这个机制。
 也就是说正在等待的线程可以选择放弃等待，改为处理其他事情。
 - **可实现公平锁**: `ReentrantLock` 可以指定是公平锁还是非公平锁。而 `synchronized` 只能是非公平锁。
 所谓的公平锁就是先等待的线程先获得锁。`ReentrantLock` 默认情况是非公平的，可以通过 `ReentrantLock(boolean fair)` 构造方法来指定是否是公平的。
 - **可实现选择性通知（锁可以绑定多个条件）**: `synchronized` 关键字与 `wait()` 和 `notify()/notifyAll()` 方法相结合可以实现等待/通知机制。
 `ReentrantLock` 类当然也可以实现，但是需要借助于 `Condition` 接口与 `ReentrantLock.newCondition()` 方法。
    - `Condition` 是 JDK1.5 之后才有的，它具有很好的灵活性，比如可以实现**多路通知功能**也就是在一个 `Lock` 对象中可以创建多个 `Condition` 实例，
    线程对象可以注册在指定的 `Condition` 中，从而可以有选择性的进行线程通知，在调度线程上更加灵活。
    在使用 `notify()/notifyAll()` 方法进行通知时，被通知的线程是由 JVM 选择的，用 `ReentrantLock` 类结合 `Condition` 实例可以实现“选择性通知” 。
    - `synchronized` 关键字就相当于整个 `Lock` 对象中只有一个 `Condition` 实例，所有的线程都注册在它一个身上。
    如果执行 `notifyAll()` 方法的话就会通知所有处于等待状态的线程这样会造成很大的效率问题，
    而 `Condition` 实例的 `signalAll()` 方法只会唤醒注册在该 `Condition` 实例中的所有等待线程。

# 7. 线程协作

当多个线程可以一起工作去解决某个问题时，如果某些部分必须在其它部分之前完成，那么就需要对线程进行协调。

## 7.1 join

在线程中调用另一个目标线程的 `join()` 方法，会将当前线程挂起，而不是忙等待，直到目标线程结束。

## 7.2 wait()、notify() 和 notifyAll()

### 7.2.1 基本使用

`wait()` 使你可以等待某个条件发生变化，通常，这种条件将由另一个任务来改变。你肯定不想在你的任务测试这个条件的同时，
不断地进行空循环，这被称为忙等待，通常是一种不良的 CPU 周期使用方式。因此 `wait()` 会在等待条件产生变化的时候将任务挂起，
并且只有在 `notify()` 或 `notifyAll()` 发生时，这个任务才会被唤醒并去检查所产生的变化。

下面看一个简单的示例，WaxOMatic.java 有两个过程：一个是将蜡涂到 `Car` 上，一个是抛光它。抛光任务在涂蜡任务完成之前，
是不能执行其工作的，而涂蜡任务在涂另一层蜡之前，必须等待抛光任务完成。`WaxOn` 和 `WaxOff` 都使用了 `Car` 对象，
该对象在这些任务等待条件变化的时候，使用 `wait()` 和 `notifyAll()` 来挂起和重新启动这些任务：
```java
public class WaxOMatic {

    public static void main(String[] args) throws InterruptedException {
        Car car = new Car();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Wax(car));
        exec.execute(new Buffer(car));

        TimeUnit.SECONDS.sleep(3);
        exec.shutdownNow();
    }
}

class Car {
    private boolean waxOn = false;

    public synchronized void waxed() {
        waxOn = true;  // Ready to buff
        notifyAll();
    }

    public synchronized void buffed() {
        waxOn = false;  // Ready for another coat of wax
        notifyAll();
    }

    public synchronized void waitForWaxing() throws InterruptedException {
        while (!waxOn)
            wait();
    }

    public synchronized void waitForBuffing() throws InterruptedException {
        while (waxOn)
            wait();
    }
}

class Wax implements Runnable {

    private Car car;

    public Wax(Car car) {
        this.car = car;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                System.out.print("Wax! ");
                TimeUnit.MILLISECONDS.sleep(200);
                car.waxed();
                car.waitForBuffing();
            }
        } catch (InterruptedException e) {
            System.out.println("Exiting via interrupt");
        }
        System.out.println("Ending Wax task");
    }
}

class Buffer implements Runnable {
    private Car car;

    public Buffer(Car car) {
        this.car = car;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                System.out.print("Buffer! ");
                TimeUnit.MILLISECONDS.sleep(200);
                car.buffed();
                car.waitForWaxing();
            }
        } catch (InterruptedException e) {
            System.out.println("Exiting via interrupt");
        }
        System.out.println("Ending Buffer task");
    }
}
```

### 7.2.2 while 循环检查条件

需要注意的是，你必须用一个检查条件的 `while` 循环包围 `wait()`。这很重要，因为：
 - 在这个任务从其 `wait()` 中被唤醒时，有可能会有某个其他的任务已经改变了条件，从而使得这个任务在此时不能执行，
 或者执行其操作已显得无关紧要。此时，应该通过再次调用 `wait()` 来将其重新挂起。
 - 也有可能某些任务根据不同的条件在等待你的对象上的锁（在这种情况下必须使用 `notifyAll()`）。在这种情况下，
 你需要检查是否已经由正确的原因唤醒，如果不是，就再次调用 `wait()`。
 
## 7.3 Condition

`java.util.concurrent` 类库中提供了 `Condition` 类来实现线程之间的协调，可以在 `Condition` 上调用 `await()` 方法使线程等待，
其它线程调用 `signal()` 或 `signalAll()` 方法唤醒等待的线程。相比于 `wait()` 这种等待方式，`await()` 可以指定等待的条件，
因此更加灵活。

下面是一个例子：
```java
public class AwaitSignalExample {
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public void before() {
        lock.lock();
        try {
            System.out.println("before");
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void after() {
        lock.lock();
        try {
            condition.await();
            System.out.println("after");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
```

# 8. 线程死锁

## 8.1 什么是线程死锁

线程死锁描述的是这样一种情况：多个线程同时被阻塞，它们中的一个或者全部都在等待某个资源被释放。由于线程被无限期地阻塞，
因此程序不可能正常终止。

下面通过一个例子来说明线程死锁,代码模拟了上图的死锁的情况 (代码来源于《并发编程之美》)：
```java
public class DeadLockDemo {
    private static Object resource1 = new Object();//资源 1
    private static Object resource2 = new Object();//资源 2

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (resource1) {
                System.out.println(Thread.currentThread() + "get resource1");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + "waiting get resource2");
                synchronized (resource2) {
                    System.out.println(Thread.currentThread() + "get resource2");
                }
            }
        }, "线程 1").start();

        new Thread(() -> {
            synchronized (resource2) {
                System.out.println(Thread.currentThread() + "get resource2");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + "waiting get resource1");
                synchronized (resource1) {
                    System.out.println(Thread.currentThread() + "get resource1");
                }
            }
        }, "线程 2").start();
    }
}
```

线程 A 通过 `synchronized(resource1)` 获得 `resource1` 的监视器锁，然后通过 `Thread.sleep(1000)` 让线程 A 休眠 1s，
为的是让线程 B 得到执行然后获取到 `resource2` 的监视器锁。线程 A 和线程 B 休眠结束了都开始企图请求获取对方的资源，
然后这两个线程就会陷入互相等待的状态，这也就产生了死锁。

## 8.2 死锁的四个必要条件

学过操作系统的朋友都知道产生死锁必须具备以下四个条件：
 - **互斥条件**：该资源任意一个时刻只由一个线程占用。
 - **请求与保持条件**：一个进程因请求资源而阻塞时，对已获得的资源保持不放。
 - **不剥夺条件**: 线程已获得的资源在未使用完之前不能被其他线程强行剥夺，只有自己使用完毕后才释放资源。
 - **循环等待条件**: 若干进程之间形成一种头尾相接的循环等待资源关系。

## 8.3 如何避免线程死锁

为了避免死锁，我们只要破坏产生死锁的四个条件中的其中一个就可以了。现在我们来挨个分析一下：
 - 破坏互斥条件：这个条件我们没有办法破坏，因为我们用锁本来就是想让他们互斥的（临界资源需要互斥访问）。
 - 破坏请求与保持条件：一次性申请所有的资源。
 - 破坏不剥夺条件：占用部分资源的线程进一步申请其他资源时，如果申请不到，可以主动释放它占有的资源。
 - 破坏循环等待条件：靠按序申请资源来预防。按某一顺序申请资源，释放资源则反序释放。破坏循环等待条件。

将上面的代码改成下面这样就不会死锁了：
```java
        new Thread(() -> {
            synchronized (resource1) {
                System.out.println(Thread.currentThread() + "get resource1");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + "waiting get resource2");
                synchronized (resource2) {
                    System.out.println(Thread.currentThread() + "get resource2");
                }
            }
        }, "线程 2").start();
```

# 9. Java程序启动时至少启动几个线程

要知道这个问题，我们需要调用 JMX 的 API：
```java
public class AllThreads {

    public static void main(String[] args) {
    	//虚拟机线程管理的接口
    	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    	ThreadInfo[] threadInfos = 
    	 threadMXBean.dumpAllThreads(false, false);
    	for(ThreadInfo threadInfo:threadInfos) {
    		System.out.println("["+threadInfo.getThreadId()+"]"+" "
    				+threadInfo.getThreadName());
    	}
    }
}
```
输出结果：
```
[6] Monitor Ctrl-Break
[5] Attach Listener
[4] Signal Dispatcher
[3] Finalizer
[2] Reference Handler
[1] main
```
可以看到，有 6 个线程。但需要注意的是，Monitor Ctrl-Break 线程是 IDEA 启动的线程，所以 JVM 启动的线程实际上只有 5 个。

## 9.1 Attach Listener

Attach Listener 线程负责接收外部的命令，对该命令进行执行并把结果返回给调用者。
通常我们会用一些命令去要求 JVM 给我们一些反馈信息，如：`java -version`、`jmap`、`jstack`等等。
如果该线程在 JVM 启动的时候没有初始化，那么，则会在用户第一次执行 JVM 命令时启动。

## 9.2 Signal Dispatcher

前面我们提到第一个 Attach Listener 线程的职责是接收外部 JVM 命令，当命令接收成功后，会交给 Signal Dispatcher 线程去进行分发到各个不同的模块处理命令，
并且返回处理结果。Signal Dispatcher 线程也是在第一次接收外部 JVM 命令时，进行初始化工作。

## 9.3 Finalizer

这个线程是在 main 线程之后创建的，主要用于在垃圾收集前，调用对象的 `finalize()` 方法。关于 Finalizer 线程有几个要点：
 - 只有当开始一轮垃圾收集时，才会开始调用 `finalize()` 方法；因此并不是所有对象的 `finalize()` 方法都会被执行；
 - 该线程也是后台线程，因此如果虚拟机中没有其他非后台线程，不管该线程有没有执行完 `finalize()` 方法，JVM 也会退出；
 - JVM 在垃圾收集时会将失去引用的对象包装成 `Finalizer` 对象（`Reference` 的实现），并放入 `ReferenceQueue`，
 由 `Finalizer` 线程来处理；最后将该 `Finalizer` 对象的引用置为 null，由垃圾收集器来回收；
 - JVM 为什么要单独用一个线程来执行 `finalize()` 方法呢？如果 JVM 的垃圾收集线程自己来做，
 很有可能由于在 `finalize()` 方法中误操作导致 GC 线程停止或不可控，这对 GC 线程来说是一种灾难。

## 9.4 Reference Handler

JVM 在创建 main 线程后就创建 Reference Handler 线程，其优先级最高，为 10。
它主要用于处理引用对象本身（软引用、弱引用、虚引用）的垃圾回收问题。

## 9.5 Monitor Ctrl-Break

Monitor Ctrl-Break 线程是在 IDEA 中才有的，而且还是要用 run 启动方式才会出现，debug 模式不会出现。


[thread-state]: ../../../../res/img/thread-state.png