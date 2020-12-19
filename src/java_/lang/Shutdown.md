`java.lang.Shutdown`类的声明如下：
```java
class Shutdown
```
包私有工具类，包含控制虚拟机关闭序列的数据结构和逻辑。

`Java`虚拟机响应以下两种事件而关闭：
 - 程序退出：通常，当最后一个非守护线程退出时，或者当`Runtime.exit`或`System.exit`方法被调用
 - 响应于用户中断（例如键入<kbd>Ctrl</kbd>-<kbd>C</kbd>）或系统范围的事件（例如用户注销或系统关闭）来终止虚拟机。
 
关闭钩子只是一个初始化但未启动的线程。当虚拟机开始其关闭序列时，它将以未指定的顺序启动所有已注册的关闭钩子，
并使其同时运行。当所有钩子完成后，如果启用了`runFinalizersOnExit`，则它将运行所有未调用的`finalizer`。最后，虚拟机将停止。
请注意，如果通过调用`exit`方法启动了关闭操作，则守护程序线程将在关闭序列期间继续运行，非守护程序线程也将继续运行。

一旦关闭序列开始，就只能通过调用`halt`方法来停止它，该方法将强制终止虚拟机。
一旦关闭序列开始，就无法注册新的关闭钩子或取消注册先前注册的钩子。尝试执行任何这些操作都将引发`IllegalStateException`。

关闭钩子在虚拟机的生命周期中的某个微妙时间运行，因此应进行防御性编码。特别是，应将它们编写为线程安全的，并尽可能避免死锁。
它们也不应盲目依赖可能已经注册了自己的关闭钩子的服务，因此可能自己处于关闭过程中。尝试使用其他基于线程的服务
（例如`AWT`事件调度线程）可能会导致死锁。

关闭钩子也应迅速完成工作。当程序调用`exit`，期望虚拟机将立即关闭并退出。当虚拟机由于用户注销或系统关闭而终止时，
底层操作系统可能只允许在固定的时间内关闭和退出。因此，不建议尝试任何用户交互或在关闭钩子中执行长时间运行的计算。

通过调用线程的`ThreadGroup.uncaughtException`方法，可以像其他线程一样在关闭钩子中处理未捕获的异常。
此方法的默认实现将异常的堆栈跟踪信息打印到`System.err`并终止线程。它不会导致虚拟机退出或停止。

在极少数情况下，虚拟机可能会中止，即在不完全关闭的情况下停止运行。当虚拟机在外部终止时会发生这种情况，
例如在`Unix`上使用`SIGKILL`信号或在`Microsoft Windows`上使用`TerminateProcess`调用。
如果`native`方法出错（例如，破坏内部数据结构或尝试访问不存在的内存），则虚拟机也可能中止。
如果虚拟机中止，则无法保证是否将运行任何关闭钩子。

# 1. 成员字段

## 1.1 关闭状态
```java
private static final int RUNNING = 0;
private static final int HOOKS = 1;
private static final int FINALIZERS = 2;

// 关闭状态初始为 RUNNING
private static int state = RUNNING;
```

## 1.2 runFinalizersOnExit
```java
// 是否应该在退出时运行所有 finalizer
private static boolean runFinalizersOnExit = false;
```
`finalizer`概念参见[Object.md][object] 1.6 节 finalize。

## 1.3 hook
```java
/*
系统关闭钩子被注册到一个预定义的数组中。关闭钩子列表如下：
(0) Console restore hook
(1) Application hooks
(2) DeleteOnExit hook
*/
private static final int MAX_SYSTEM_HOOKS = 10;
private static final Runnable[] hooks = new Runnable[MAX_SYSTEM_HOOKS];

// 当前正在运行的关闭钩子在 hooks 数组中的索引
private static int currentRunningHook = 0;
```

## 1.4 lock
```java
// 1.4 节之前定义的静态字段受此锁保护
private static class Lock { };
private static Object lock = new Lock();

// native halt 方法的锁对象
private static Object haltLock = new Lock();
```

# 2. 方法

## 2.1 setRunFinalizersOnExit
```java
// 被 Runtime.runFinalizersOnExit 方法调用
static void setRunFinalizersOnExit(boolean run) {
    synchronized (lock) {
        runFinalizersOnExit = run;
    }
}
```

## 2.2 add
```java
/*
添加一个新的关闭钩子。检查关闭状态和钩子本身，但不执行任何安全检查。除了注册 DeleteOnExitHook 之外，
registerShutdownInProgress 参数应该为 false，因为第一个文件可能会被 ApplicationShutdownHooks
添加到 DeleteOnExit 列表中。

@params slot  关闭钩子数组中的插槽，其元素将在关闭期间按顺序调用
@params registerShutdownInProgress 如果为true，则即使正在关闭，也允许注册钩子。
@params hook  要注册的钩子

@throw IllegalStateException 
       如果 registerShutdownProgress 为 false 并且正在关闭；或者如果 registerShutdownProgress 为 true，
       并且关闭进程已通过给定的插槽，抛出此异常。
*/
static void add(int slot, boolean registerShutdownInProgress, Runnable hook) {
    synchronized (lock) {
        if (hooks[slot] != null)
            throw new InternalError("Shutdown hook at slot " + slot + " already registered");

        if (!registerShutdownInProgress) {
            if (state > RUNNING)
                throw new IllegalStateException("Shutdown in progress");
        } else {
            if (state > HOOKS || (state == HOOKS && slot <= currentRunningHook))
                throw new IllegalStateException("Shutdown in progress");
        }

        hooks[slot] = hook;
    }
}
```

## 2.3 runHooks
```java
// 运行全部注册的关闭钩子
private static void runHooks() {
    for (int i=0; i < MAX_SYSTEM_HOOKS; i++) {
        try {
            Runnable hook;
            synchronized (lock) {
                // 获取锁以确保在关闭期间注册的钩子在这里可见。
                currentRunningHook = i;
                hook = hooks[i];
            }
            if (hook != null) hook.run();
        } catch(Throwable t) {
            if (t instanceof ThreadDeath) {
                ThreadDeath td = (ThreadDeath)t;
                throw td;
            }
        }
    }
}
```

## 2.4 halt
```java
// halt 方法在 halt 锁上同步，以避免 delete-on-shutdown 文件列表损坏。它调用真正的 native halt 方法。
static void halt(int status) {
    synchronized (haltLock) {
        halt0(status);
    }
}

static native void halt0(int status);
```

## 2.5 sequence
```java
/*
实际停机序列在这里定义。

如果不是 runFinalizersOnExit，这将很简单——我们只需运行钩子，然后停止。
相反，我们需要跟踪是在运行钩子还是 finalizer。在后一种情况下，finalizer 可以调用 exit(1) 以立即终止，
而在前一种情况下，对于任何 n，调用 exit(n) 都只是暂停。

请注意，如果启用了 on-exit finalizer，那么当关闭是由一个 exit(0) 启动时，它们就会运行；
对于，它们永远不会运行在 exit(n)（n != 0）或响应 SIGINT、SIGTERM 等。
*/
private static void sequence() {
    synchronized (lock) {
        /* 防止后台线程在 DestroyJavaVM 启动关闭序列后调用 exit 的可能性
         */
        if (state != HOOKS) return;
    }
    // 运行全部注册的关闭钩子
    runHooks();
    boolean rfoe;
    synchronized (lock) {
        // 将状态变为 FINALIZERS
        state = FINALIZERS;
        rfoe = runFinalizersOnExit;
    }
    if (rfoe) runAllFinalizers();
}

// 调用 java.lang.ref.Finalizer.runAllFinalizers 方法
private static native void runAllFinalizers();
```

## 2.6 shutdown
```java
// 在最后一个非守护进程线程完成时由 JNI DestroyJavaVM 过程调用。与 exit 方法不同，此方法实际上不会停止 VM。
static void shutdown() {
    synchronized (lock) {
        switch (state) {
            case RUNNING:       /* 启动 Shutdown */
                state = HOOKS;
                break;
            case HOOKS:         /* 停止 */
            case FINALIZERS:
                break;
        }
    }
    synchronized (Shutdown.class) {
        sequence();
    }
}
```

## 2.7 exit
```java
// 由 Runtime.exit 调用，它执行所有安全检查。也由系统提供的终止事件的处理程序调用，该事件应传递非零状态代码。
static void exit(int status) {
    boolean runMoreFinalizers = false;
    synchronized (lock) {
        // 系统提供的终止事件的处理程序 status 非 0
        if (status != 0) runFinalizersOnExit = false;
        switch (state) {
            case RUNNING:       /* 启动 Shutdown */
                state = HOOKS;
                break;
            case HOOKS:         /* 停止 */
                break;
            case FINALIZERS:
                if (status != 0) {
                    /* 非零状态下立即停止 */
                    halt(status);
                } else {
                    /* 与旧行为兼容：运行 finalizer，然后停止 */
                    runMoreFinalizers = runFinalizersOnExit;
                }
                break;
        }
    }
    if (runMoreFinalizers) {
        runAllFinalizers();
        halt(status);
    }
    synchronized (Shutdown.class) {
        /* 在类对象上同步，使任何其他试图启动关闭的线程无限期地暂停 */
        sequence();
        halt(status);
    }
}
```


[object]: Object.md
[hook]: ApplicationShutdownHooks.md