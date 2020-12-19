`java.lang.ApplicationShutdownHooks`类的声明如下：
```java
class ApplicationShutdownHooks
```
`ApplicationShutdownHooks`是一个静态工具类。它是用来跟踪和运行通过`Runtime.addShutdownHook`方法注册的用户级关闭钩子。

# 1. 成员字段
```java
// 存储钩子的集合，此处键和值相等，用 IdentityHashMap 的目的是利用它使用"=="比较对象的特性。 
private static IdentityHashMap<Thread, Thread> hooks;
```

# 2. 构造方法/块
```java
static {
    try {
        // 使用 Shutdown 注册 ApplicationShutdownHooks
        Shutdown.add(1 /* shutdown hook invocation order */,
            false /* 如果正在关闭，则不进行注册 */,
            // 关闭时的回调
            new Runnable() {
                public void run() {
                    runHooks();
                }
            }
        );
        hooks = new IdentityHashMap<>();
    } catch (IllegalStateException e) {
        // Shutdown 如果不在 RUNNING 状态注册 ApplicationShutdownHooks 将会抛出 IllegalStateException 异常。
        hooks = null;
    }
}

private ApplicationShutdownHooks() {}
```
参见[Shutdown.md][shutdown]。

# 3. 方法

## 3.1 add
```java
// 添加新的关闭挂钩。检查关闭状态和钩子本身，但不执行任何安全检查。
static synchronized void add(Thread hook) {
    // hooks 要在静态初始化块里构造完才能使用
    if(hooks == null)
        throw new IllegalStateException("Shutdown in progress");

    if (hook.isAlive())
        throw new IllegalArgumentException("Hook already running");

    if (hooks.containsKey(hook))
        throw new IllegalArgumentException("Hook previously registered");

    hooks.put(hook, hook);
}
```

## 3.2 remove
```java
// 移除先前注册的钩子。与 add 方法类似，此方法不执行任何安全检查。
static synchronized boolean remove(Thread hook) {
    // hooks 要在静态初始化块里构造完才能使用
    if(hooks == null)
        throw new IllegalStateException("Shutdown in progress");

    if (hook == null)
        throw new NullPointerException();

    return hooks.remove(hook) != null;
}
```

## 3.3 runHooks
```java
// 运行所有应用程序钩子。钩子是并发运行的，这个方法等待它们完成。
static void runHooks() {
    Collection<Thread> threads;
    synchronized(ApplicationShutdownHooks.class) {
        threads = hooks.keySet();
        hooks = null;
    }

    // 开启所有钩子
    for (Thread hook : threads) {
        hook.start();
    }
    // 等待所有钩子运行结束
    for (Thread hook : threads) {
        while (true) {
            try {
                hook.join();
                break;
            } catch (InterruptedException ignored) {
            }
        }
    }
}
```


[shutdown]: Shutdown.md