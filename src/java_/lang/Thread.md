`java.lang.Thread`类的声明如下：
```java
public class Thread implements Runnable
```
线程是程序中的执行线程。`JVM`允许应用程序具有多个并发运行的执行线程。
每个线程都有一个优先级。具有较高优先级的线程优先于具有较低优先级的线程执行。每个线程可能会也可能不会被标记为**守护(daemon)线程**。

当在某个线程中运行的代码创建新的`Thread`对象时，新线程的优先级最初设置为与创建线程的优先级相等，
并且当且仅当创建线程是守护程序时，该线程才是守护程序线程。

当`JVM`启动时，通常只有一个非守护线程（通常调用某些指定类中名为`main`的方法）。`Java`虚拟机将继续执行线程，直到发生以下任何一种情况：
 - 调用类`Runtime.exit`方法，并且安全管理器已允许进行退出操作。
 - 不是守护程序线程的所有线程都已终结，或者从`run`方法返回，或者抛出一个传播到`run`方法之外的异常。
 
有两种方法可以创建新的执行线程：
 - 一种是将一个类声明为`Thread`的子类。该子类应重写`Thread`类的`run`方法。然后可以创建并启动子类的实例。
 - 另一种方法是声明一个实现`Runnable`接口的类，该类实现`run`方法。然后可以分配该类的实例，
 在创建`Thread`时将其作为参数传递并启动。 
 
每个线程都有一个名称供识别。多个线程可能具有相同的名称。如果在创建线程时未指定名称，则会为其生成一个新名称。

有关线程的更多用法和细节，参见[Java并发-线程基础][thread]。

# 1. 内部类

## 1.1 State
```java
// 表示线程状态
public enum State {
    // 新建状态。即线程创建，还未调用 start 方法
    NEW,

    // 运行状态。就绪状态（调用 start，等待调度） + 正在运行
    RUNNABLE,

    // 阻塞状态。等待监视器锁时，陷入阻塞状态
    BLOCKED,

    // 等待状态。正在等待另一线程执行特定的操作（如 notify）
    WAITING,

    // 超时等待状态。具有指定等待时间的等待状态
    TIMED_WAITING,

    // 终止状态。线程完成执行
    TERMINATED;
}
```
详细内容参见 [Java并发-线程基础][thread] 第 5 节-线程状态转换。

## 1.2 UncaughtExceptionHandler
```java
/*
当线程由于未捕获的异常突然终止时调用的处理程序的接口。

当线程由于未捕获的异常而将要终止时，Java 虚拟机将使用 getUncaughtExceptionHandler 查询线程的
UncaughtExceptionHandler 并将调用它的 uncaughtException 方法，并将线程和异常作为参数传递。

如果未显式设置线程的 UncaughtExceptionHandler，则使用它的 ThreadGroup 的 UncaughtExceptionHandler。
如果 ThreadGroup 也没有，则可以将调用转发到默认的 UncaughtExceptionHandler（getDefaultUncaughtExceptionHandler()）。
*/
@FunctionalInterface
public interface UncaughtExceptionHandler {
   
    void uncaughtException(Thread t, Throwable e);
}
```

## 1.3 WeakClassKey
```java
// 用于 Class 对象的弱引用
static class WeakClassKey extends WeakReference<Class<?>> {
    
    private final int hash;

    WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
        super(cl, refQueue);
        hash = System.identityHashCode(cl);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof WeakClassKey) {
            Object referent = get();
            return (referent != null) &&
                   (referent == ((WeakClassKey) obj).get());
        } else {
            return false;
        }
    }
}
```

## 1.4 Caches
```java
// 线程子类安全审核结果的缓存。也就是子类有没有覆盖 getContextClassLoader() 或 setContextClassLoader() 方法。
// 如果在未来的版本中出现的话，就用 ConcurrentReferenceHashMap 来代替。
private static class Caches {
    // 线程子类安全审核结果的缓存
    static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits =
        new ConcurrentHashMap<>();

    // 已审计子类的弱引用队列
    static final ReferenceQueue<Class<?>> subclassAuditsQueue =
        new ReferenceQueue<>();
}
```

# 2. 成员字段

## 2.1 常量
```java
// Java 线程的最小优先级
public final static int MIN_PRIORITY = 1;

// Java 线程的默认优先级
public final static int NORM_PRIORITY = 5;

// Java 线程的最大优先级
public final static int MAX_PRIORITY = 10;

private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

/*
enableContextClassLoaderOverride: 线程上下文类加载器方法的子类实现。

在需要查找可能不存在于系统类加载器中的资源时，系统代码和扩展部分会使用上下文类加载器。
授予 enableContextClassLoaderOverride 权限将允许线程的子类重写某些方法，
这些方法用于得到或设置特定线程的上下文类加载器。
*/
private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
                new RuntimePermission("enableContextClassLoaderOverride");
```

## 2.2 基本属性
```java
// 线程名称
private volatile String name;

// 线程优先级
private int priority;

// 是否是后台线程
private boolean daemon = false;

// 线程 id
private long tid;
// 用来产生线程 id
private static long threadSeqNumber;
private static synchronized long nextThreadID() {
    return ++threadSeqNumber;
}

// Java 线程状态，与 State 中的枚举整数值对应。初始化后表示线程处于 "NEW" 状态。
private volatile int threadStatus = 0;

// 用于自动编号匿名线程（即没有在构造器中指定名称的线程）
private static int threadInitNumber;
private static synchronized int nextThreadNum() {
    return threadInitNumber++;
}

/*
此线程请求的堆栈大小，如果创建者没有指定堆栈大小，则为 0。
这个数字由虚拟机自己决定，有些虚拟机会忽略它。
*/
private long stackSize;

// 将要运行的任务
private Runnable target;

// 所属的线程组
private ThreadGroup group;

// 上下文 ClassLoader
private ClassLoader contextClassLoader;

// 继承的 AccessControlContext
private AccessControlContext inheritedAccessControlContext;
```

## 2.3 线程异常处理器
```java
// 当前线程的异常处理器，默认为 null
private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

// 所以线程的默认异常处理器，默认为 null
private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
```

## 2.4 线程本地变量
```java
// 当前线程的本地变量
ThreadLocal.ThreadLocalMap threadLocals = null;

// 从父线程（创建当前线程的线程）继承得到的本地变量
ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
```

## 2.5 中断属性
```java
/*
当前线程在可中断 I/O 操作中被阻塞的对象（如果有的话）。
在设置本线程的中断状态后，应调用 Interruptible.interrupt(Thread) 方法。
中断状态是底层变量，Thread 中没有
*/
private volatile Interruptible blocker;
private final Object blockerLock = new Object();

// 设置 blocker 域，被 java.nio 中的代码通过 sun.misc.SharedSecrets 调用
void blockedOn(Interruptible b) {
    synchronized (blockerLock) {
        blocker = b;
    }
}
```

## 2.6 ThreadLocalRandom
```java
// ThreadLocalRandom 当前的种子
@sun.misc.Contended("tlr")
long threadLocalRandomSeed;

// 探测哈希值。如果 threadLocalRandomSeed 已初始化，则为非零
@sun.misc.Contended("tlr")
int threadLocalRandomProbe;

// 从公共 ThreadLocalRandom 序列中分离出的次要种子
@sun.misc.Contended("tlr")
int threadLocalRandomSecondarySeed;
```

## 2.7 其他属性
```java
private Thread         threadQ;

private long           eetop;

// 是否单步执行此线程
private boolean     single_step;

// JVM 状态
private boolean     stillborn = false;

// native 线程终止后持续存在的 JVM 私有状态。
private long nativeParkEventPointer;

/*
提供给 java.util.concurrent.locks.LockSupport.park 的参数。

由（私有）java.util.concurrent.locks.LockSupport.setBlocker 方法设置，
java.util.concurrent.locks.LockSupport.getBlocker 获取。
*/
volatile Object parkBlocker;
```

# 3. 构造器/块

## 3.1 静态构造块
```java
// 确保 registerNatives 是 <clinit> 要做的第一件事。
private static native void registerNatives();
static {
    registerNatives();
}
```

## 3.2 公共构造器
```java
/*
创建一个新的 Thread 对象，它使用 target 作为任务，具有指定的 name 作为其名称，并属于 group 引用的线程组。

如果存在安全管理器，则将其 ThreadGroup 作为其参数来调用其 checkAccess 方法。

另外，当子类覆盖构造方法，并直接或间接调用 getContextClassLoader 或 setContextClassLoader 方法时，
将使用 RuntimePermission("enableContextClassLoaderOverride") 权限调用 checkPermission 方法。

新创建的线程的优先级设置为等于创建它的线程的优先级，即当前正在运行的线程。 setPriority 方法可用于将优先级更改为新值。

当且仅当创建它的线程当前被标记为守护程序线程时，才将新创建的线程最初标记为守护程序线程。
setDaemon 方法可用于更改线程是否是守护程序。
*/
public Thread(ThreadGroup group, Runnable target, String name) {
    init(group, target, name, 0);
}

// 创建一个新的 Thread 对象。此构造函数与 Thread (null, target, gname) 具有相同的作用，其中 gname 是线程名称。
// 自动生成的名称的形式为 "Thread-"+n，其中 n 为整数。
public Thread() {
    init(null, null, "Thread-" + nextThreadNum(), 0);
}

public Thread(Runnable target) {
    init(null, target, "Thread-" + nextThreadNum(), 0);
}

public Thread(ThreadGroup group, Runnable target) {
    init(group, target, "Thread-" + nextThreadNum(), 0);
}

public Thread(String name) {
    init(null, null, name, 0);
}

public Thread(ThreadGroup group, String name) {
    init(group, null, name, 0);
}

public Thread(Runnable target, String name) {
    init(null, target, name, 0);
}

/*
创建一个新的 Thread 对象，它使用 target 作为任务，具有指定的 name 作为其名称，并属于 group 引用的线程组。
并且具有指定的堆栈大小。

该构造函数与 Thread(ThreadGroup, Runnable, String) 相同，不同之处在于它允许指定线程堆栈的大小。
堆栈大小是虚拟机要为此线程的堆栈分配的地址空间的大概字节数。stackSize 参数（如果有）的效果高度依赖于平台。

在某些平台上，为 stackSize 参数指定更高的值可能会允许线程在 StackOverflowError 之前获得更大的递归深度。
类似地，指定一个较低的值可能允许并发存在更多数量的线程，而不会引发 OutOfMemoryError （或其他内部错误）。

stackSize 参数的值与最大递归深度和并发级别之间的关系的详细信息取决于平台。在某些平台上，stackSize 参数的值可能没有任何作用。

虚拟机可以随意将 stackSize 参数作为建议。如果指定值过低，则虚拟机可能会使用某些平台特定的最小值；
如果指定的值过高，则虚拟机可能会使用特定于平台的最大值。同样，虚拟机可以根据需要随意向上或向下舍入指定的值（或完全忽略它）。

为 stackSize 参数指定为 0 将导致此构造函数的行为与 Thread(ThreadGroup, Runnable, String) 构造函数完全相同。

由于此构造函数的行为依赖于平台，因此在使用时应格外小心。 从一个 JRE 实现到另一个 JRE 实现，
执行给定计算所需的线程堆栈大小可能会有所不同。鉴于这种变化，可能需要仔细调整堆栈大小参数，
并且可能需要针对要在其上运行应用程序的每个JRE实现重复进行该调整。

实现说明：鼓励 Java 平台实现者针对 stackSize 参数记录其所定义的行为。
*/
public Thread(ThreadGroup group, Runnable target, String name,
              long stackSize) {
    init(group, target, name, stackSize);
}
```
在公共构造器中，可以指定线程组、任务、线程名称、堆栈大小。并且默认从父线程继承本地变量。
线程优先级、是否是后台线程等，都需要使用对应的 set 方法设置。

关于本地变量和从父线程继承本地变量参见 [ThreadLocal.md][local] 和 [InheritableThreadLocal.md][inherit-local]。

## 3.3 非公共构造器
```java
// 创建一个新线程，该线程继承给定的 AccessControlContext。并且不继承父线程的本地变量。
Thread(Runnable target, AccessControlContext acc) {
    init(null, target, "Thread-" + nextThreadNum(), 0, acc, false);
}
```

## 3.4 init
```java
// 初始化线程
private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
    if (name == null) {
        throw new NullPointerException("name cannot be null");
    }

    this.name = name;

    // 创建当前线程的线程即为父线程
    Thread parent = currentThread();
    SecurityManager security = System.getSecurityManager();
    if (g == null) {
        /* 确认是否是 Java Applet */

        // 如果安全管理器存在，向安全管理器请求线程组
        if (security != null) {
            g = security.getThreadGroup();
        }

        // 如果安全管理器没有意见，则使用父线程的线程组
        if (g == null) {
            g = parent.getThreadGroup();
        }
    }

    // 无论是否显式传入线程组，都要检查访问权限
    g.checkAccess();

    // 检查是否有所需的权限
    if (security != null) {
        // 如果子类覆盖了 getContextClassLoader() 或 setContextClassLoader() 方法
        if (isCCLOverridden(getClass())) {
            // 检查它是否有这个权限
            security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
    }

    // 增加线程组中未启动线程的数量。
    g.addUnstarted();

    this.group = g;
    // 默认和父线程的后台状态和优先级相同
    this.daemon = parent.isDaemon();
    this.priority = parent.getPriority();
    // 从父线程获取上下文类加载器
    if (security == null || isCCLOverridden(parent.getClass()))
        this.contextClassLoader = parent.getContextClassLoader();
    else
        this.contextClassLoader = parent.contextClassLoader;
    // acc 是当前线程要继承的 AccessControlContext，为 null 的话就使用 AccessController.getContext()
    this.inheritedAccessControlContext =
            acc != null ? acc : AccessController.getContext();
    // 设置任务
    this.target = target;
    // 检查优先级
    setPriority(priority);
    // 如果可以的话，则继承父类的 inheritableThreadLocals
    if (inheritThreadLocals && parent.inheritableThreadLocals != null)
        this.inheritableThreadLocals =
            ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
    // 指定的堆栈大小
    this.stackSize = stackSize;

    // 生成线程 id
    tid = nextThreadID();
}

// 使用当前的 AccessControlContext 初始化线程。并且默认从父线程继承本地变量。
private void init(ThreadGroup g, Runnable target, String name,
                  long stackSize) {
    init(g, target, name, stackSize, null, true);
}

/*
验证是否可以在不违反安全约束的情况下构造此（可能是子类）实例：该子类想要覆盖对安全性敏感的非 final 方法，
就将检查“enableContextClassLoaderOverride” RuntimePermission。

安全性敏感的非 final 方法指 getContextClassLoader() 和 setContextClassLoader()。
*/
private static boolean isCCLOverridden(Class<?> cl) {
    if (cl == Thread.class)
        return false;
    
    // Caches 包含了子类安全审核结果的缓存。
    // 首先删除缓存中已被 gc 到引用队列的键（WeakClassKey）
    processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
    WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
    // 查找缓存
    Boolean result = Caches.subclassAudits.get(key);
    if (result == null) {
        // 没有的话进行检查，检查结果放入缓存中
        result = Boolean.valueOf(auditSubclass(cl));
        Caches.subclassAudits.putIfAbsent(key, result);
    }

    return result.booleanValue();
}

// 从指定 map 中删除已在指定引用队列上入队的所有引用。
static void processQueue(ReferenceQueue<Class<?>> queue,
                         ConcurrentMap<? extends WeakReference<Class<?>>, ?> map)
{
    Reference<? extends Class<?>> ref;
    while((ref = queue.poll()) != null) {
        map.remove(ref);
    }
}

/*
对给定的子类执行反射检查，以验证它有没有覆盖对安全性敏感的非 final 方法。
也就是 getContextClassLoader() 和 setContextClassLoader()。

如果子类覆盖任何方法，则返回 true，否则返回 false。
*/
private static boolean auditSubclass(final Class<?> subcl) {
    Boolean result = AccessController.doPrivileged(
        new PrivilegedAction<Boolean>() {
            public Boolean run() {
                for (Class<?> cl = subcl;
                     cl != Thread.class;
                     cl = cl.getSuperclass())
                {
                    try {
                        cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                        return Boolean.TRUE;
                    } catch (NoSuchMethodException ex) {
                    }
                    try {
                        Class<?>[] params = {ClassLoader.class};
                        cl.getDeclaredMethod("setContextClassLoader", params);
                        return Boolean.TRUE;
                    } catch (NoSuchMethodException ex) {
                    }
                }
                return Boolean.FALSE;
            }
        }
    );
    return result.booleanValue();
}
```
安全管理器参见 [安全管理器][security] 和 [SecurityManager][security-manager]。

# 4. 方法

## 4.1 checkAccess
```java
/*
确定当前正在运行的线程是否有权修改此线程。

如果有安全管理器，则以该线程为参数调用其 checkAccess 方法。这可能导致抛出 SecurityException 。
*/
public final void checkAccess() {
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkAccess(this);
    }
}
```

## 4.2 toString
```java
// 返回此线程的字符串表示形式，包括线程的名称，优先级和线程组。
public String toString() {
    ThreadGroup group = getThreadGroup();
    if (group != null) {
        return "Thread[" + getName() + "," + getPriority() + "," +
                       group.getName() + "]";
    } else {
        return "Thread[" + getName() + "," + getPriority() + "," +
                        "" + "]";
    }
}
```

## 4.3 clone
```java
// 抛出 CloneNotSupportedException，因为无法有意义地克隆线程。
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
}
```

## 4.4 run
```java
// 如果此线程是使用单独的 Runnable 构造的，则将调用该 Runnable 对象的 run 方法；否则，此方法不执行任何操作并返回。
// Thread 子类应重写此方法。
@Override
public void run() {
    if (target != null) {
        target.run();
    }
}
```

## 4.5 currentThread
```java
// 返回对当前正在执行的线程对象的引用。
public static native Thread currentThread();
```

## 4.6 start
```java
/*
使该线程开始执行； Java虚拟机将调用此线程的 run 方法。

此方法不能调用超过 1 次。特别是，线程一旦完成执行就可能不会重新启动。
*/
public synchronized void start() {
    /*
    本方法不为虚拟机创建/设置的 main 线程或"系统"组线程所调用。
    未来添加到本方法的任何新功能可能也必须添加到虚拟机中。
    
    状态值为零时，对应状态为 "NEW"。
    */
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

    // 通知线程组这个线程即将启动，这样就可以把它加入到线程组的线程列表中，并可以减少线程组的未启动线程次数。
    group.add(this);

    boolean started = false;
    try {
        start0();
        started = true;
    } finally {
        try {
            if (!started) {
                group.threadStartFailed(this);
            }
        } catch (Throwable ignore) {
            // 不做任何事情。如果 start0 抛出了一个 Throwable，那么它将会被传递到调用堆栈上
        }
    }
}

private native void start0();
```

## 4.7 isAlive
```java
// 测试这个线程是否还在活动。如果一个线程已经启动并且还没有死亡，那么这个线程就是活动的。
public final native boolean isAlive();
```

## 4.8 getId
```java
// 返回线程 ID
public long getId() {
    return tid;
}
```

## 4.9 getState
```java
// 返回该线程的状态。本方法设计用于监视系统状态，而不是用于同步控制。
public State getState() {
    // get current thread state
    return sun.misc.VM.toThreadState(threadStatus);
}
```

## 4.10 名称
```java
/*
将这个线程的名称改为与参数名相等。

首先调用这个线程的 checkAccess 方法，这可能导致抛出一个 SecurityException。
*/
public final synchronized void setName(String name) {
    checkAccess();
    if (name == null) {
        throw new NullPointerException("name cannot be null");
    }

    this.name = name;
    // 但线程已经启动时，设置本地线程名称
    if (threadStatus != 0) {
        setNativeName(name);
    }
}

// 获取线程名称
public final String getName() {
    return name;
}

private native void setNativeName(String name);
```

## 4.11 优先级
```java
/*
改变这个线程的优先级。

首先调用这个线程的 checkAccess 方法。这可能导致抛出一个 SecurityException。
否则，这个线程的优先级将被设置为指定的 newPriority 和该线程的线程组的最大允许优先级中较小的一个。
*/
public final void setPriority(int newPriority) {
    ThreadGroup g;
    checkAccess();
    // 线程优先级要在 Java 给定的范围内
    if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
        throw new IllegalArgumentException();
    }
    if((g = getThreadGroup()) != null) {
        // 线程优先级不能超过所属线程组的最大优先级
        if (newPriority > g.getMaxPriority()) {
            newPriority = g.getMaxPriority();
        }
        setPriority0(priority = newPriority);
    }
}

// 获取线程优先级
public final int getPriority() {
    return priority;
}

private native void setPriority0(int newPriority);
```

## 4.12 后台线程
```java
/*
将此线程标记为守护进程线程或用户线程。当守护者线程在运行时，Java 虚拟机就会退出。

该方法必须在线程启动前被调用。
*/
public final void setDaemon(boolean on) {
    checkAccess();
    // 如果线程已经启动还调用此方法，则抛出异常
    if (isAlive()) {
        throw new IllegalThreadStateException();
    }
    daemon = on;
}

// 返回此线程是否是守护线程
public final boolean isDaemon() {
    return daemon;
}
```

## 4.13 getThreadGroup
```java
// 返回线程所属的线程组。如果线程已经终止，返回 null。
public final ThreadGroup getThreadGroup() {
    return group;
}
```

## 4.14 异常处理器
```java
/*
设置当一个线程由于未捕获异常而突然终止，且没有为该线程定义其他处理程序时，调用的默认处理程序。

未捕获异常处理首先由线程控制，然后由线程的 ThreadGroup 对象控制，最后由默认的未捕获异常处理程序控制。
如果线程没有设置显式的未捕获异常处理程序，并且线程的线程组（包括父线程组）也没有，
那么将调用默认处理程序的 uncaughtException 方法。

通过设置默认的异常处理程序，应用程序可以为那些已经接受系统提供的任何 "默认" 行为的线程改变处理
未捕获异常的方式（例如记录到特定的设备、或文件）。

需要注意的是，默认的未捕获异常处理程序通常不应该服从于线程的 ThreadGroup 对象，因为这可能会导致无限递归。
*/
public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        sm.checkPermission(
            new RuntimePermission("setDefaultUncaughtExceptionHandler")
        );
    }

    defaultUncaughtExceptionHandler = eh;
}

// 返回默认的异常处理器
public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
    return defaultUncaughtExceptionHandler;
}

/*
返回当这个线程的异常处理器，如果这个线程没有明确设置未捕获异常处理器，那么将返回这个线程的 ThreadGroup 对象。
ThreadGroup 实现了 UncaughtExceptionHandler 接口。

如果这个线程已经终止，将返回 null。
*/
public UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler != null ?
        uncaughtExceptionHandler : group;
}

/*
设置当该线程的异常处理器。

一个线程可以通过显式设置它的的异常处理器来完全控制它如何响应未捕获异常。如果没有设置这样的处理程序，
那么线程的 ThreadGroup 对象将作为它的异常处理器。
*/
public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
    checkAccess();
    uncaughtExceptionHandler = eh;
}

/*
将线程中发生的异常传递给异常处理器。
这个方法会被 JVM 隐式地调用。
*/
private void dispatchUncaughtException(Throwable e) {
    getUncaughtExceptionHandler().uncaughtException(this, e);
}
```

## 4.15 yield
```java
/*
给调度程序的提示：当前线程愿意放弃当前使用的处理器。调度程序可以随意忽略此提示。

yield 是一种启发式尝试，旨在提高线程之间的相对进度，防止过度利用 CPU。它的使用应与详细的性能分析和基准测试结合使用，
以确保它实际上具有所需的效果。

很少适合使用此方法。它可能对调试或测试有用，因为它可能有助于重现由于竞争条件而产生的错误。
当设计诸如 java.util.concurrent.locks 包中的并发控制结构时，它也可能很有用。
*/
public static native void yield();
```

## 4.16 sleep
```java
/*
根据系统计时器和调度程序的精度和准确性，使当前正在执行的线程进入休眠状态（暂时停止执行）达指定的毫秒数。
该线程不会释放任何持有的锁。
*/
public static native void sleep(long millis) throws InterruptedException;

// 虽然好像可以指定纳秒的休眠时间，实际上会被舍入到毫秒中
public static void sleep(long millis, int nanos) throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    sleep(millis);
}
```

## 4.17 join
```java
/*
在其他线程中调用此线程的 join() 方法，会将其他线程挂起最多 millis 毫秒，或者此线程结束。
millis 为 0 意味着一直等待知道此线程结束。

本方法将此线程作为锁对象，使用 isAlive() 作为条件。当未超时并且此线程未终止时，使用 wait() 方法
让调用 join() 方法的其他线程等待。
当一个线程终止时，会自动调用 notifyAll() 方法，从而不会发生死锁。

建议应用程序不要在 Thread 实例上使用 wait、notify 或 notifyAll。
*/
public final synchronized void join(long millis) throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    // 如果 millis 为 0，则一直等待
    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}

// 虽然好像可以指定纳秒的休眠时间，实际上会被舍入到毫秒中
public final synchronized void join(long millis, int nanos) throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    join(millis);
}

// 等同于 join(0)
public final void join() throws InterruptedException {
    join(0);
}
```

## 4.18 interrupt
```java
public void interrupt() {
    // 如果此线程不等于当前线程，则检查当前线程是否有权限修改此线程的状态
    if (this != Thread.currentThread())
        checkAccess();

    synchronized (blockerLock) {
        Interruptible b = blocker;
        if (b != null) {
            // 只设置中断状态
            interrupt0();
            b.interrupt(this);
            return;
        }
    }
    interrupt0();
}

private native void interrupt0();
```
参见 [Interruptible][interruptible]。

## 4.19 中断状态
```java
/*
测试该线程是否被中断。线程的中断状态不受本方法的影响。

如果线程在中断时已不在活动，则线程中断将被忽略的，本方法将返回 false。
*/
public boolean isInterrupted() {
    return isInterrupted(false);
}

/*
测试当前线程是否被中断。线程的中断状态会被本方法清除。换句话说，如果这个方法连续被调用两次，
第二次调用将返回 false（除非当前线程在第一次调用清除其中断状态后，第二次调用检查之前再次被中断）。

如果线程在中断时已不在活动，则线程中断将被忽略的，本方法将返回 false。
*/
public static boolean interrupted() {
    return currentThread().isInterrupted(true);
}

// 测试某个 Thread 是否被中断。ClearInterrupted 为 true，则重置中断状态。
private native boolean isInterrupted(boolean ClearInterrupted);
```

## 4.20 上下文类加载器
```java
/*
返回该线程的上下文 ClassLoader。该上下文 ClassLoader 由线程的创建者提供，供运行在该线程中的代码在加载类和资源时使用。
如果没有设置，默认为父线程的 ClassLoader 上下文。原始线程的上下文 ClassLoader 通常设置为用于加载应用程序的类加载器。

如果存在安全管理器，调用者的类加载器不是 null，并且与上下文类加载器不是同一个或其祖先，
那么这个方法就会调用安全管理器的 checkPermission 方法，用 RuntimePermission("getClassLoader") 权限
来验证上下文类加载器的检索是否被允许。
*/
@CallerSensitive
public ClassLoader getContextClassLoader() {
    if (contextClassLoader == null)
        return null;
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        ClassLoader.checkClassLoaderPermission(contextClassLoader,
                                               Reflection.getCallerClass());
    }
    return contextClassLoader;
}

/*
设置该线程的上下文 ClassLoader。在创建线程时可以设置上下文 ClassLoader，允许线程的创建者在加载类和资源时，
通过 getContextClassLoader 为线程中运行的代码提供相应的类加载器。

如果存在安全管理器，其 checkPermission 方法会检查 RuntimePermission("setContextClassLoader") 权限，
查看是否允许设置上下文 ClassLoader。
*/
public void setContextClassLoader(ClassLoader cl) {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("setContextClassLoader"));
    }
    contextClassLoader = cl;
}
```

## 4.21 holdsLock
```java
/*
如果且仅当当前线程持有指定对象的监控锁时，返回 true。

这个方法被设计为允许程序断言当前线程已经持有指定的锁。
           assert Thread.holdLock(obj);
*/
public static native boolean holdsLock(Object obj);
```

## 4.22 activeCount
```java
/*
返回当前线程的线程组及其子组中活跃线程的估计数量。递归地遍历当前线程的线程组中的所有子组。

返回的值只是一个估计值，因为在本方法遍历内部数据结构时，线程数可能会动态变化，可能会受到某些系统线程存在的影响。
本方法主要用于调试和监控。
*/
public static int activeCount() {
    return currentThread().getThreadGroup().activeCount();
}
```

## 4.23 enumerate
```java
/*
将当前线程的线程组及其子组中的每个活动线程复制到指定数组中。这个方法只是调用了当前线程的线程组的
ThreadGroup.enumerate(Thread[]) 方法。

一个应用程序可能会使用 activeCount 方法来获得一个估计数组应该有多大，然而如果数组太短，无法容纳所有的线程，
那么多余的线程就会被默默忽略。
如果获得当前线程的线程组及其子组中的每一个活动线程是至关重要的，那么调用者应该验证返回的 int 值是否严格小于 tarray 的长度。

由于该方法存在固有的竞争条件，建议该方法仅用于调试和监控目的。
*/
public static int enumerate(Thread tarray[]) {
    return currentThread().getThreadGroup().enumerate(tarray);
}
```

## 4.24 线程堆栈
```java
// 将当前线程的堆栈跟踪信息打印到标准错误流中。此方法仅用于调试。
public static void dumpStack() {
    // 利用了异常对象会记录堆栈调用的特性
    new Exception("Stack trace").printStackTrace();
}

/*
返回一个堆栈跟踪元素的数组，代表这个线程的堆栈转储。如果这个线程还没有启动，已经启动但还没有被系统安排运行，
或者已经终止，那么这个方法将返回一个零长度的数组。

如果返回的数组长度为非零，那么数组的第一个元素代表堆栈的顶部，是方法调用序列中的最后一个方法调用，也就是此方法。
数组的最后一个元素代表堆栈的底部，也就是方法调用序列中第一个方法调用（一般是 main 方法）。

如果有一个安全管理器，而这个线程不是当前线程，那么就会调用安全管理器的 checkPermission 方法，
用 RuntimePermission("getStackTrace") 权限来查看是否可以获取栈跟踪。

在某些情况下，一些虚拟机可能会从堆栈跟踪中省略一个或多个堆栈帧。在极端的情况下，
如果一个虚拟机没有关于这个线程的堆栈跟踪信息，则允许从这个方法中返回一个零长度的数组。
*/
public StackTraceElement[] getStackTrace() {
    // 如果此线程不是当前线程
    if (this != Thread.currentThread()) {
        // 检查 getStackTrace 权限
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                SecurityConstants.GET_STACK_TRACE_PERMISSION);
        }
        // 此线程未启动或已结束，则返回 0 长度栈帧。
        if (!isAlive()) {
            return EMPTY_STACK_TRACE;
        }
        // 获取线程栈帧
        StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
        StackTraceElement[] stackTrace = stackTraceArray[0];
        // 在上一个 isAlive 调用期间还活着的线程可能已经终止了，可能没有堆栈跟踪。
        if (stackTrace == null) {
            stackTrace = EMPTY_STACK_TRACE;
        }
        return stackTrace;
    } else {
        // 使用异常对象获取线程栈帧
        return (new Exception()).getStackTrace();
    }
}

/*
返回所有活动线程的堆栈踪迹 Map。键是线程，值是一个 StackTraceElement 数组，代表相应线程的堆栈转储。
返回的堆栈记录是以 getStackTrace() 方法指定的格式。

在调用该方法时，线程可能正在执行。每个线程的堆栈跟踪只代表一个快照，每个堆栈跟踪可能在不同的时间获得。
如果虚拟机没有某个线程的堆栈跟踪信息，那么映射值中会返回一个零长度的数组。

如果有安全管理器，则调用安全管理器的 checkPermission 方法，同时检查 RuntimePermission("getStackTrace")
权限以及 RuntimePermission("modifyThreadGroup") 权限，看是否可以获取所有线程的堆栈跟踪。
*/
public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
    // 检查 getStackTrace 权限和 modifyThreadGroup 权限
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkPermission(
            SecurityConstants.GET_STACK_TRACE_PERMISSION);
        security.checkPermission(
            SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
    }

    // 获取所有活动线程的快照
    Thread[] threads = getThreads();
    // 生成线程堆栈记录
    StackTraceElement[][] traces = dumpThreads(threads);
    Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
    for (int i = 0; i < threads.length; i++) {
        StackTraceElement[] stackTrace = traces[i];
        if (stackTrace != null) {
            m.put(threads[i], stackTrace);
        }
        // else terminated so we don't put it in the map
    }
    return m;
}

private native static StackTraceElement[][] dumpThreads(Thread[] threads);

private native static Thread[] getThreads();
```

## 4.25 exit
```java
// 系统将会调用此方法，以使线程有机会在实际退出之前进行清理。
private void exit() {
    if (group != null) {
        group.threadTerminated(this);
        group = null;
    }
    // 积极清空所有参考字段：请参见 bug 4006245
    target = null;
    // 加快释放其中一些资源
    threadLocals = null;
    inheritableThreadLocals = null;
    inheritedAccessControlContext = null;
    blocker = null;
    uncaughtExceptionHandler = null;
}
```

## 4.26 suspend
```java
/*
暂停这个线程。

首先，这个线程的 checkAccess 方法被调用，这可能导致抛出一个 SecurityException（在当前线程中）。
如果该线程还活着，它就会被暂停，直到它被恢复。

@deprecated: 这个方法已经被废弃，因为它本身就容易产生死锁。如果目标线程在暂停时持有保护关键系统资源的
监视器的锁，那么在目标线程恢复之前，任何线程都不能访问这个资源。如果用来恢复目标线程的线程在调用 resume 之前
试图获取这个监视器，就会产生死锁。这种死锁通常表现为 "冻结 "进程。
*/
@Deprecated
public final void suspend() {
    checkAccess();
    suspend0();
}

private native void suspend0();
```

## 4.27 resume
```java
/*
恢复一个暂停的线程。

首先，这个线程的 checkAccess 方法被调用，这可能导致抛出一个 SecurityException（在当前线程中）。

如果该线程还活着但被暂停，则恢复该线程并允许其继续执行。

@deprecated: 这个方法只与 suspend 一起使用，而 suspend 由于容易产生死锁，已经被废弃。
*/
@Deprecated
public final void resume() {
    checkAccess();
    resume0();
}

private native void resume0();
```

## 4.28 stop
```java
/*
强制线程停止执行。线程被终止时会抛出 ThreadDeath 异常。

如果安全管理器存在，那么就会调用 checkAccess 方法。这可能会导致一个 SecurityException（在当前线程中）。

如果这个线程与当前线程不同（也就是说，当前线程正试图停止除自身以外的其他线程），
那么安全管理器的 checkPermission 方法（以RuntimePermission("stopThread")为参数）会被另外调用。
同样，这可能会导致抛出一个 SecurityException。

@deprecated 这个方法本质上是不安全的。用 Thread.stop 停止一个线程会导致它解锁所有被锁定的监控器
（因为未被捕获的 ThreadDeath 异常在堆栈上传播会导致这种情况发生）。如果之前被这些监控器保护的对象中
有任何一个处于不一致的状态，那么受损的对象就会对其他线程可见，有可能导致任意行为。
stop 的许多用法都应该被状态变量所取代，以表明目标线程应该停止运行。目标线程应该定期检查这个变量，
如果变量指示它要停止运行，就应该有序地从它的 run 方法中返回。如果目标线程长时间等待，
应该使用中断方法来中断等待。
*/
@Deprecated
public final void stop() {
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        checkAccess();
        if (this != Thread.currentThread()) {
            security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
        }
    }
    // A zero status value corresponds to "NEW", it can't change to
    // not-NEW because we hold the lock.
    if (threadStatus != 0) {
        resume(); // Wake up thread if it was suspended; no-op otherwise
    }

    // The VM can handle all thread states
    stop0(new ThreadDeath());
}

@Deprecated
public final synchronized void stop(Throwable obj) {
    throw new UnsupportedOperationException();
}

private native void stop0(Object o);
```

## 4.29 destroy
```java
@Deprecated
public void destroy() {
    throw new NoSuchMethodError();
}
```

## 4.30 countStackFrames
```java
/*
计算该线程的堆栈帧数。该线程必须暂停。

@deprecated: 这个调用的定义依赖于 suspend，而 suspend 已经被废弃。此外，这个调用的结果从来没有被很好地定义过。
*/
@Deprecated
public native int countStackFrames();
```


[thread]: ../util/concurrent/Java并发-线程基础.md
[local]: ThreadLocal.md
[inherit-local]: InheritableThreadLocal.md
[security]: 安全管理器.md
[security-manager]: SecurityManager.md
[interruptible]: ../../sun_/nio/ch/Interruptible.md