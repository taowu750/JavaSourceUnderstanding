`java.lang.Runtime`类的声明如下：
```java
public class Runtime
```
每个`Java`应用程序都有一个`Runtime`类单例，该实例允许应用程序与运行环境进行交互。`Runtime`使用`getRuntime`方法获得。

# 1. 单例
```java
private static Runtime currentRuntime = new Runtime();

public static Runtime getRuntime() {
    return currentRuntime;
}

private Runtime() {}
```

# 2. 方法

## 2.1 addShutdownHook
```java
public void addShutdownHook(Thread hook) {
    // 进行安全检查
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("shutdownHooks"));
    }
    // 将 hook 添加到应用程序钩子中
    ApplicationShutdownHooks.add(hook);
}
```
注册一个新的虚拟机关闭挂钩。

`Java`虚拟机响应以下两种事件而关闭：
 - 程序退出：通常，当最后一个非守护线程退出时，或者当`Runtime.exit`或`System.exit`方法被调用
 - 响应于用户中断（例如键入<kbd>Ctrl</kbd>-<kbd>C</kbd>）或系统范围的事件（例如用户注销或系统关闭）来终止虚拟机。
 
关闭钩子只是一个初始化但未启动的线程。当虚拟机开始其关闭序列时，它将以未指定的顺序启动所有已注册的关闭钩子，
并使其同时运行。如果在运行关闭序列之前调用了方法`runFinalizersOnExit`，当所有钩子运行结束后，则它将运行所有未调用的`finalizer`
（`finalizer`概念参见[Object.md][object] 1.6 节 finalize。）。
最后，虚拟机将停止。请注意，如果通过调用`exit`方法启动了关闭操作，则守护线程将在关闭序列期间继续运行，
非守护程序线程也将继续运行。

一旦关闭序列开始，就只能通过调用`halt`方法来停止它，该方法将强制终止虚拟机。
一旦关闭序列开始，就无法注册新的关闭钩子或取消注册先前注册的钩子。尝试执行任何这些操作都将引发`IllegalStateException`。

关闭钩子在虚拟机的生命周期中的某个微妙时间运行，因此应进行防御性编码。特别是，应将它们编写为线程安全的，并尽可能避免死锁。
它们也不应依赖那些可能已经注册了自己的关闭钩子的服务，因为这些服务可能处于关闭过程中。尝试使用其他基于线程的服务
（例如`AWT`事件调度线程）可能会导致死锁。

关闭钩子也应迅速完成工作。当程序调用`exit`，期望虚拟机将立即关闭并退出。当虚拟机由于用户注销或系统关闭而终止时，
底层操作系统可能只允许在固定的时间内关闭和退出。因此，不建议尝试任何用户交互或在关闭钩子中执行长时间运行的计算。

通过调用线程的`ThreadGroup.uncaughtException`方法，可以像其他线程一样在关闭钩子中处理未捕获的异常。
此方法的默认实现将异常的堆栈跟踪信息打印到`System.err`并终止线程。它不会导致虚拟机退出或停止。

在极少数情况下，虚拟机可能会中止，即在不完全关闭的情况下停止运行。当虚拟机在外部终止时会发生这种情况，
例如在`Unix`上使用`SIGKILL`信号或在`Microsoft Windows`上使用`TerminateProcess`调用。
如果`native`方法出错（例如，破坏内部数据结构或尝试访问不存在的内存），则虚拟机也可能中止。
如果虚拟机中止，则无法保证是否运行任何关闭钩子。

另请参见[ApplicationShutdownHooks.md][application]和[Shutdown.md][shutdown]。

## 2.2 removeShutdownHook
```java
// 注销先前注册的虚拟机关闭挂钩。
public boolean removeShutdownHook(Thread hook) {
    // 进行安全检查
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("shutdownHooks"));
    }
    // 将 hook 从应用程序钩子中注销
    return ApplicationShutdownHooks.remove(hook);
}
```

## 2.3 runFinalizersOnExit
```java
/*
启用或禁用退出时的 finalize 处理；启用时尚未调用的 finalizer 将在 Java 程序退出之前运行。默认情况下，
退出时的 finalize 处理是禁用的。如果有 SecurityManager，则首先使用 0 作为其参数来调用其 checkExit 方法，以确保允许退出。
这可能会导致 SecurityException。

@deprecated: 此方法使对象即使处于可达状态，JVM 仍对其执行 finalize 方法。这种方法本质上是不安全的，
因为其他线程可能正在操纵这些对象，从而导致行为不稳定或死锁。
*/
@Deprecated
public static void runFinalizersOnExit(boolean value) {
    // 进行安全检查
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        try {
            security.checkExit(0);
        } catch (SecurityException e) {
            throw new SecurityException("runFinalizersOnExit");
        }
    }
    // 设置 runFinalizersOnExit 状态
    Shutdown.setRunFinalizersOnExit(value);
}
```

## 2.4 runFinalization
```java
/*
运行任何未完成的对象的终结方法。调用此方法，Java 虚拟机将花更多的精力来运行不可达但尚未运行其 finalize 方法
的对象的 finalize 方法。当从 runFinalization 方法调用中返回时，虚拟机将尽最大努力完成所有未完成的终结工作。

如果未显式调用 runFinalization 方法，则虚拟机将根据需要在单独的线程中自动执行完成过程。
System.runFinalization() 方法是调用此方法的常规且方便的方法。
*/
public void runFinalization() {
    runFinalization0();
}

// 调用 java.lang.ref.Finalizer.runAllFinalizers 方法
private static native void runFinalization0();
```

## 2.5 exit
```java
/*
通过启动关闭序列来终止当前正在运行的 Java 虚拟机。此方法永远不会正常返回。参数 status 用作状态码，
按照惯例，非零状态代码表示异常终止。

虚拟机的关闭序列包括两个阶段。在第一阶段，所有已注册的 application shutdown hooks（如果有）以某种未指定的顺序启动，
并允许它们并发运行直到全部完成。在第二阶段，如果已经调用了方法 runFinalizersOnExit，则所有未调用的 finalizer 都将运行。
完成此操作后，虚拟机将终结。

如果在虚拟机开始其关闭序列后调用此方法，且正在运行关闭挂钩，则此方法将无限期地阻塞。
如果关机挂钩运行结束，并且已经调用了方法 runFinalizersOnExit，此时如果 status 为非零，
则此方法将使用给定的状态代码来停止虚拟机；否则，它将无限期地阻塞。

System.exit 方法是调用此方法的常规且便捷的方法。
*/
public void exit(int status) {
    // 进行安全检查
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkExit(status);
    }
    // 调用 Shutdown 退出
    Shutdown.exit(status);
}
```
参见[Shutdown.md][shutdown] 2.7 节 exit。

## 2.6 halt
```java
/*
强制终止当前正在运行的 Java 虚拟机。此方法永远不会正常返回。

使用此方法时应格外小心。与 exit 方法不同，此方法不会启动关闭挂钩。即使已经调用了方法 runFinalizersOnExit，
也不会运行未调用的终结器。如果关闭序列已经启动，则此方法不会等待任何正在运行的关闭挂钩或 finalizer 完成其工作。
*/
public void halt(int status) {
    // 进行安全检查
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        sm.checkExit(status);
    }
    // 调用 Shutdown 终止
    Shutdown.halt(status);
}
```

## 2.7 gc
```java
/*
运行垃圾收集器。调用此方法 Java 虚拟机将花费大量精力来回收未使用的对象，以使它们当前占用的内存可用于快速重用。
当控制从方法调用返回时，虚拟机将尽最大努力回收所有丢弃的对象。

gc 代表“垃圾收集器”。即使未显式调用 gc 方法，虚拟机也会根据需要在单独的线程中自动执行此回收过程。
System.gc() 方法是调用此方法的常规且方便的方法。
*/
public native void gc();
```

## 2.8 memory
```java
// 返回 Java 虚拟机中的可用内存量，以字节为单位。调用 gc 方法可能会导致 freeMemory 返回的值增加。
public native long freeMemory();

// 返回 Java 虚拟机中的内存总量，以字节为单位。此方法返回的值可能会随时间变化，具体取决于主机环境。
// 请注意，保存任何给定类型的对象所需的内存量取决于实现。
public native long totalMemory();

// 返回 Java 虚拟机将尝试使用的最大内存量，以字节为单位。如果没有限制，则将返回 Long.MAX_VALUE 值。
public native long maxMemory();
```

## 2.9 availableProcessors
```java
// 返回可用于 Java 虚拟机的 CPU 核心数。
// 在虚拟机的特定调用期间，此值可能会更改。因此，对可用处理器数量敏感的应用程序应该偶尔轮询此属性并适当地调整其资源使用情况。
public native int availableProcessors();
```

## 2.10 exec
```java
/*
在具有指定环境和工作目录的单独进程中执行指定的字符串命令。相当于在命令行执行命令。

给定代表命令的字符串数组 cmdarray 和代表系统环境变量设置的字符串数组 envp，此方法将创建一个新进程来执行指定的命令。
此方法检查 cmdarray 是不是有效的操作系统命令。哪些命令有效取决于系统，但至少该命令必须是非空字符串的非空列表。

如果 envp 为 null，则子进程继承当前进程的环境设置。新子进程的工作目录由 dir 指定。如果 dir 为 null，
则子进程继承当前进程的当前工作目录。

如果存在安全管理器，则使用数组 cmdarray 的第一个值作为 SecurityManager.checkExec 方法的参数。
这可能会导致引发 SecurityException。

启动操作系统进程与系统高度相关。可能出错的情况包括：
 - 找不到操作系统程序文件。
 - 对该程序文件的访问被拒绝。
 - 工作目录不存在。
在这种情况下，将引发异常。异常的确切性质取决于系统，但是它将始终是 IOException 的子类。

@param cmdarray: 包含调用命令及其参数的数组。
@param envp: 字符串数组，其每个元素是 name=value 格式的环境变量设置。如果子进程应继承当前进程的环境，则应为 null。
@param dir: 子进程的工作目录。如果子进程应继承当前进程的工作目录，则应为 null。
*/
public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
    return new ProcessBuilder(cmdarray)
        .environment(envp)
        .directory(dir)
        .start();
}

/*
在具有指定环境和工作目录的单独进程中执行指定的字符串命令。相当于在命令行执行命令。
command 是包含调用命令及其参数的字符串。
*/
public Process exec(String command, String[] envp, File dir) throws IOException {
    if (command.length() == 0)
        throw new IllegalArgumentException("Empty command");

    // StringTokenizer 由于分隔字符串，默认的分隔符是空格("")、制表符(\t)、换行符(\n)、回车符(\r)
    StringTokenizer st = new StringTokenizer(command);
    String[] cmdarray = new String[st.countTokens()];
    for (int i = 0; st.hasMoreTokens(); i++)
        cmdarray[i] = st.nextToken();
    return exec(cmdarray, envp, dir);
}

public Process exec(String[] cmdarray, String[] envp) throws IOException {
    return exec(cmdarray, envp, null);
}

public Process exec(String cmdarray[]) throws IOException {
    return exec(cmdarray, null, null);
}

public Process exec(String command, String[] envp) throws IOException {
    return exec(command, envp, null);
}

public Process exec(String command) throws IOException {
    return exec(command, null, null);
}
```

## 2.11 traceInstructions
```java
/*
启用/禁用指令跟踪。如果参数 on 为 true ，则此方法建议 Java 虚拟机在执行时为虚拟机中的每个指令发出调试信息。
此信息的格式以及将其发送到的文件或其他输出流，取决于主机环境。如果虚拟机不支持此功能，则可以忽略该请求。
跟踪输出的目的地取决于系统。

如果参数为 false，则此方法导致虚拟机停止执行其正在执行的详细指令跟踪。
*/
public native void traceInstructions(boolean on);
```

## 2.12 traceMethodCalls
```java
/*
启用/禁用方法调用的跟踪。如果参数 on 为 true，则此方法建议 Java 虚拟机在调用虚拟机时为虚拟机中的每个方法发出调试信息。
此信息的格式以及将其发送到的文件或其他输出流，取决于主机环境。如果虚拟机不支持此功能，则可以忽略该请求。

如果参数为 false，则此方法虚导致拟机停止发出调用方法的调试信息。
*/
public native void traceMethodCalls(boolean on);
```

## 2.13 load
```java
/*
加载由 filename 参数指定的 native 库。filename 参数必须是绝对路径名。
（例如 Runtime.getRuntime().load("/home/avh/lib/libX11.so"); ）。 

如果从文件名参数中删除任何特定于平台的库前缀，路径和文件扩展名，例如为 L，
并且名为 L 的 native 库与 VM 静态链接，则将会调用用来导出库的 JNI_OnLoad_L 函数，而不是尝试加载动态库。
与参数匹配的文件名在文件系统中不必存在。有关更多详细信息，请参见 JNI 规范。否则，
文件名参数将以与实现相关的方式映射到 native 库映像。

如果有 SecurityManager，则以 filename 作为参数调用其 checkLink 方法。这可能会导致 SecurityException。

此方法类似于 loadLibrary(String) 方法，但是它接受常规文件名作为参数，而不仅仅是库名，从而允许加载任何 native 代码文件。

System.load(String) 方法是调用此方法的常规且方便的方法。
*/
// 注解 @CallerSensitive 的方法对其调用类敏感
@CallerSensitive
public void load(String filename) {
    load0(Reflection.getCallerClass(), filename);
}

synchronized void load0(Class<?> fromClass, String filename) {
    // 进行安全检查
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkLink(filename);
    }
    // 如果 filename 不是绝对路径，抛出异常
    if (!(new File(filename).isAbsolute())) {
        throw new UnsatisfiedLinkError(
            "Expecting an absolute path of the library: " + filename);
    }
    ClassLoader.loadLibrary(fromClass, filename, true);
}
```

## 2.14 loadLibrary
```java
/*
加载由 libname 参数指定的 native 库。libname 参数不得包含任何平台特定的前缀，文件扩展名或路径。
如果名为 libname native 库与 VM 静态链接，则将会调用用来导出库的 JNI_OnLoad_libname 函数。
有关更多详细信息，请参见JNI规范。否则，libname 参数将从系统库位置加载，并以与实现相关的方式映射到本机库映像。

如果有 SecurityManager，则以 libname 作为参数调用其 checkLink 方法。这可能会导致 SecurityException。

System.loadLibrary(String) 方法是调用此方法的常规且方便的方法。

如果要在类的实现中使用本机方法，则标准策略是将 native 代码写在库文件（例如称为 LibFile）中，然后放入静态初始化器：
    static { System.loadLibrary("LibFile"); }

在类声明中。当类被加载和初始化时，native 方法的必要 native 代码实现也将被加载。
如果使用相同的库名称多次调用此方法，则将忽略第二个及后续调用。
*/
@CallerSensitive
public void loadLibrary(String libname) {
    loadLibrary0(Reflection.getCallerClass(), libname);
}

synchronized void loadLibrary0(Class<?> fromClass, String libname) {
    // 进行安全检查
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkLink(libname);
    }
    // libname 不能含有任何路径
    if (libname.indexOf((int)File.separatorChar) != -1) {
        throw new UnsatisfiedLinkError("Directory separator should not appear in library name: " + libname);
    }
    ClassLoader.loadLibrary(fromClass, libname, false);
}
```
此方法的使用参见[native关键字.md][native] 第 4.1 节 Java 文件。


[object]: Object.md
[application]: ApplicationShutdownHooks.md
[shutdown]: Shutdown.md
[native]: native关键字.md