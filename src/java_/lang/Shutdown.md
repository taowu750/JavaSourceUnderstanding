`java.lang.Shutdown`类的声明如下：
```java
class Shutdown
```
包私有工具类，包含控制虚拟机关闭序列的数据结构和逻辑。参见[Runtime.md][runtime] 2.1 节 addShutdownHook。


# 1. 成员字段

## 1.1 关闭状态
```java
// RUNNING 状态：此时可以向关闭序列中添加钩子
private static final int RUNNING = 0;
// HOOKS 状态：由 RUNNING 转换而来，表示开启关闭序列
private static final int HOOKS = 1;
// FINALIZERS 状态：由 HOOKS 转换而来，表示关闭序列全部启动
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
系统关闭钩子被注册到一个预定义的数组中，关闭时将会依次运行。关闭钩子在数组中的分布如下：
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
@params registerShutdownInProgress 如果为 true，则即使正在关闭，也允许注册钩子。
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
实际停机序列在这里定义。sequence 只会在 HOOKS 状态下运行。如果已经是 FINALIZERS 状态，则不会运行第二遍。
此方法会先依次运行注册的关闭钩子，然后如果启用了 runFinalizersOnExit，就运行所有未调用的 finalizer
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
        // 将状态由 HOOKS 变为 FINALIZERS，表示关闭序列运行完毕
        state = FINALIZERS;
        rfoe = runFinalizersOnExit;
    }
    // 如果启用 runFinalizersOnExit，则运行所有未调用的 finalizer
    if (rfoe) runAllFinalizers();
}

// 调用 java.lang.ref.Finalizer.runAllFinalizers 方法
private static native void runAllFinalizers();
```

## 2.6 shutdown
```java
// 此方法在最后一个非守护进程线程完成时由 JNI DestroyJavaVM 过程调用。
// 与 exit 方法不同，此方法实际上不会停止 VM，只会运行一遍关闭序列。
static void shutdown() {
    synchronized (lock) {
        switch (state) {
            case RUNNING:       /* 从 RUNNING 到 HOOKS 状态，启动关闭序列 */
                state = HOOKS;
                break;
            case HOOKS:
            case FINALIZERS:
                break;
        }
    }
    /* 在类对象上同步，使任何其他试图再次启动关闭的线程无限期地暂停 */
    synchronized (Shutdown.class) {
        sequence();
    }
}
```

## 2.7 exit
```java
/*
status 是状态码。按照惯例，非零状态代码表示异常终止。

此方法由 Runtime.exit 调用，它会执行所有安全检查。此方法也由系统提供的终止事件的处理程序调用，该事件传递非零状态码。
*/
static void exit(int status) {
    boolean runMoreFinalizers = false;
    synchronized (lock) {
        if (status != 0) runFinalizersOnExit = false;
        switch (state) {
            case RUNNING:       /* 从 RUNNING 到 HOOKS 状态，启动 Shutdown */
                state = HOOKS;
                break;
            case HOOKS:
                break;
            case FINALIZERS:
                if (status != 0) {
                    /* 非零状态表示异常终止，此时应该立即停止程序 */
                    halt(status);
                } else {
                    /* 与旧行为兼容：运行 finalizer，然后停止程序 */
                    runMoreFinalizers = runFinalizersOnExit;
                }
                break;
        }
    }
    // 如果已经是 FINALIZERS 状态，status 为 0 且启用 runFinalizersOnExit，
    // 则运行所有 finalizer 后停止程序
    if (runMoreFinalizers) {
        runAllFinalizers();
        halt(status);
    }
    synchronized (Shutdown.class) {
        // 如果从 RUNNING 到 HOOKS 状态，或已经是 HOOKS 状态
        /* 在类对象上同步，使任何其他试图再次启动关闭的线程无限期地暂停 */
        sequence();
        halt(status);
    }
}
```
`exit`方法运行流程如下：
1. 首先，如果`status`非 0，`runFinalizersOnExit`会被设为`false`，不会运行任何`finalizer`。
2. 如果当前状态是`RUNNING`，`sequence`方法此时还未被调用，则转移到`HOOKS`状态：
    - 如果`status`非 0，先执行`sequence` 方法，运行关闭序列，不会运行任何`finalizer`，
    状态转移至`FINALIZERS`，最后停止程序。
    - 如果`status`为 0，先执行`sequence`方法（`sequence`方法内部根据`runFinalizersOnExit`决定是否运行所有未调用的`finalizer`），
    状态转移至`FINALIZERS`，然后停止程序。
3. 如果当前状态为`HOOKS`，则会被无限期阻塞。
4. 如果当前状态是`FINALIZERS`，`sequence`方法此时已经运行过一次：
    - 如果`status`非 0，则会立即停止程序。
    - 如果`status`为 0，且`runFinalizersOnExit`为`true`，则会先运行所有未调用的`finalizer`，然后停止程序。

总结下来，如果关闭序列未运行，则会运行一遍（也只会运行一遍）。
而`finalizer`只会在`status`为 0 且`runFinalizersOnExit`为`true`的状态下运行，而且在关闭序列运行完一遍后仍然可以执行。


[object]: Object.md
[runtime]: Runtime.md
[hook]: ApplicationShutdownHooks.md