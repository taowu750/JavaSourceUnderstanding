`java.lang.Throwable`类的声明如下：
```java
public class Throwable implements Serializable
```
`Throwable`类是`Java`语言中所有错误和异常的超类。`Java`虚拟机或者`Java throw`语句仅抛出属于此类（或其子类之一）实例的对象。
类似地，在`catch`子句中，只有此类或其子类之一才能成为参数类型。

通常使用子类`Error`、`Exception`和`RuntimeException`（和它们的子类）来指示发生了异常情况。这些实例是在特殊情况下重新创建的，
以包括相关信息（例如堆栈跟踪数据）。

一个`Throwable`包含它创建时线程执行堆栈的快照。它还可以包含一个消息字符串，该消息字符串提供有关错误的更多信息。
一个`Throwable`可以使用`Throwable.addSuppressed`抑制其他`Throwable`的传播。`Throwable`还可以包含一个 cause：
另一个导致该`Throwable`的`Throwable`。此因果信息的记录称为**异常链**，因为 cause 本身可以有 cause，依此类推，每个异常都是由另一个引起的。

`Throwable`代码中比较值得注意的有：
 - 1.2 `stackTrace`: 使用哨兵对象表达状态，实现不可变对象。
 - 2.2 `Wrapper`: 使用装饰器适配不同对象。
 - 3.6 `printStackTrace`: `Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>())`
 创建使用`==`比较的`Set`。

被抑制的异常和`try-with-resource`有关，参见[try-with-resources及异常抑制.md][try-with-resources]。

# 1. 成员字段

## 1.1 detailMessage
```java
// 有关 Throwable 的特定详细信息。例如，对于 FileNotFoundException，它包含找不到的文件的名称。
private String detailMessage;
```

## 1.2 stackTrace
```java
// native 代码在该插槽中保存了一些有关堆栈回溯的指示。
private transient Object backtrace;

/*
为了使 Throwable 可以用作不可变对象，并由 JVM 安全地重用，例如 OutOfMemoryErrors，Throwable 可写入字段
（响应用户操作、cause、stackTrace 和 suppressedExceptions）遵循以下协议：
1） 字段初始化为 non-null 的哨兵（sentinel）值，该值表示逻辑上未设置该值。
2） 向字段写入 null 表示禁止进一步写入
3） 哨兵值可以替换其他 non-null 值。

例如，HotSpot Jvm 的实现已经预先分配了 OutOfMemoryError 对象，以便更好地诊断这种情况。
这些对象是在不调用该类的构造函数的情况下创建的，相关字段被初始化为 null。为了支持此特性，
添加到 Throwable 的任何新字段需要初始化为非空值，都需要协调 JVM 更改。
*/

// 空堆栈的共享值。
private static final StackTraceElement[] UNASSIGNED_STACK = new StackTraceElement[0];

// 导致引发此 Throwable 的 Throwable；如果此 Throwable 不是由另一个 Throwable 引起的，或者引发 Throwable 未知，
// 则为 null。如果此字段等于此 Throwable 本身，则表明此 Throwable 的 cause 尚未初始化。
private Throwable cause = this;

// 堆栈跟踪，由 getStackTrace() 返回。该字段被初始化为零长度数组。此字段为 null 表示对
// setStackTrace(StackTraceElement[]) 和 fillInStackTrace() 的后续调用将为 no-ops。
private StackTraceElement[] stackTrace = UNASSIGNED_STACK;

// cause 异常堆栈跟踪的开头
private static final String CAUSE_CAPTION = "Caused by: ";
```

## 1.3 suppressed exception
```java
// 设置此静态字段引入了对几个 java.util 类（Collections、List、ArrayList）的可接受的初始化依赖关系。
private static final List<Throwable> SUPPRESSED_SENTINEL =
    Collections.unmodifiableList(new ArrayList<Throwable>(0));

// 由 getSuppressed() 返回的抑制异常列表。该列表被初始化为不可修改的空哨兵列表。读入序列化 Throwable 时，
// 如果 suppressedExceptions 字段指向一个零元素列表，则该字段将重置为哨兵值。
private List<Throwable> suppressedExceptions = SUPPRESSED_SENTINEL;

// 尝试抑制 NullPointException 的消息。
private static final String NULL_CAUSE_MESSAGE = "Cannot suppress a null exception.";

// 试图抑制自身的讯息。
private static final String SELF_SUPPRESSION_MESSAGE = "Self-suppression not permitted";

// 用于标记抑制的异常堆栈跟踪的标题
private static final String SUPPRESSED_CAPTION = "Suppressed: ";

// 空的 Throwable 数组，被 getSuppressed 方法使用
private static final Throwable[] EMPTY_THROWABLE_ARRAY = new Throwable[0];
```

# 2. 内部类

## 2.1 SentinelHolder
```java
// Holder 类用于推迟仅用于序列化的哨兵对象的初始化。
private static class SentinelHolder {

    // 用于表示不可变的堆栈跟踪。在序列化反序列化时如果等于此字段，那么 stackTrace 将被设置为 null
    public static final StackTraceElement STACK_TRACE_ELEMENT_SENTINEL =
        new StackTraceElement("", "", null, Integer.MIN_VALUE);

    // setStackTrace(StackTraceElement[]) 方法将堆栈跟踪设置为包含此单元素数组的哨兵值，等同于 null 效果，
    // 表示将来试图修改堆栈跟踪的调用将被忽略。
    public static final StackTraceElement[] STACK_TRACE_SENTINEL =
        new StackTraceElement[] {STACK_TRACE_ELEMENT_SENTINEL};
}
```

## 2.2 Wrapper
```java
// 用于 PrintStream 和 PrintWriter 的装饰器类。这样 printStackTrace 只需要一个实现即可
private abstract static class PrintStreamOrWriter {
    // 返回使用此 StreamOrWriter 时要加锁的对象
    abstract Object lock();

    // 使用此 StreamOrWriter 打印一行字符串
    abstract void println(Object o);
}

// 用来包装 PrintStream
private static class WrappedPrintStream extends PrintStreamOrWriter {
    private final PrintStream printStream;

    WrappedPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    Object lock() {
        return printStream;
    }

    void println(Object o) {
        printStream.println(o);
    }
}

// 用来包装 PrintWriter
private static class WrappedPrintWriter extends PrintStreamOrWriter {
    private final PrintWriter printWriter;

    WrappedPrintWriter(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    Object lock() {
        return printWriter;
    }

    void println(Object o) {
        printWriter.println(o);
    }
}
```

# 3. 方法

## 3.1 message
```java
// 返回此 Throwable 的详细消息字符串，可以为 null
public String getMessage() {
    return detailMessage;
}

// 返回此 Throwable 的本地化描述。子类可以重写此方法，以生成特定于语言环境的消息。对于不覆盖此方法的子类，
// 默认实现返回与 getMessage() 相同的结果。
public String getLocalizedMessage() {
    return getMessage();
}
```

## 3.2 cause
```java
/*
返回此 Throwable 的 cause，如果 cause 不存在或未知，则返回 null。cause 是导致该 Throwable 被抛出的 Throwable。

cause 可以通过 Throwable 构造器和 initCause 方法提供。Throwable 只有三个子类（Error、Exception、RuntimeException）
支持使用构造器设置 cause，其他异常类必须使用 initCause 方法设置 cause。

虽然通常不必重写此方法，但是子类可以重写它以返回通过其他方式产生的 cause 集。
这适用于在将链式异常添加到 Throwable 之前实现链式异常。请注意，此时没有必要重写任何 PrintStackTrace 方法，
所有这些方法都调用了 getCause 方法来确定 Throwable 的 cause。
*/
public synchronized Throwable getCause() {
    return (cause==this ? null : cause);
}

/*
将此 Throwable 的 cause 初始化为指定的值。此方法最多只能调用一次，通常在构造函数内部或在创建 Throwable 之后立即调用它。
如果以及调用了 Throwable(Throwable) 或 Throwable(String, Throwable) 初始化原因，则不能调用此方法。

下面是一个此方法的使用示例：
       try {
           lowLevelOp();
       } catch (LowLevelException le) {
           throw (HighLevelException) new HighLevelException().initCause(le); // Legacy constructor
       }
*/
public synchronized Throwable initCause(Throwable cause) {
    if (this.cause != this)
        throw new IllegalStateException("Can't overwrite cause with " +
                                        Objects.toString(cause, "a null"), this);
    if (cause == this)
        throw new IllegalArgumentException("Self-causation not permitted", this);
    this.cause = cause;
    return this;
}
```
注意到`cause`方法都是`synchronized`，而且`cause`只能设置一次。这是为了保证`Throwable`是不可变对象。

## 3.3 toString
```java
// 返回此 Throwable 的简短描述。
public String toString() {
    String s = getClass().getName();
    String message = getLocalizedMessage();
    // Throwable 全限定名称和 message 的组合
    return (message != null) ? (s + ": " + message) : s;
}
```

## 3.4 fillStaceTrace
```java
// 初始化方法，填充线程调用堆栈跟踪信息。此方法在 Throwable 对象中记录有关当前线程的堆栈帧的当前状态的信息。
// 如果此 Throwable 的堆栈跟踪信息不可写，则调用此方法无效。
public synchronized Throwable fillInStackTrace() {
    // 根据 1.2 节所述，只有当 stackTrace 或 backtrace 不为 null 时，才会初始化堆栈跟踪
    if (stackTrace != null ||
        backtrace != null /* Out of protocol state */ ) {
        fillInStackTrace(0);
        // 延迟初始化 stackTrace，只有在调用 getStackTrace 或 printStackTrace 方法时才会对 stackTrace 初始化
        stackTrace = UNASSIGNED_STACK;
    }
    return this;
}

private native Throwable fillInStackTrace(int dummy);
```

## 3.5 getStackTrace
```java
/*
返回一个堆栈跟踪元素数组，每个元素代表一个堆栈帧。数组的第零个元素（假设数组的长度为非零）表示堆栈的顶部，
这是序列中的最后一个方法调用。通常，这是创建和抛出该 Throwable 的地方。数组的最后一个元素（假设数组的长度为非零）
表示堆栈的底部，这是序列中的第一个方法调用。
想想看，方法调用是压栈方式实现的，序列中第一个方法调用会被压到栈底。

在某些情况下，某些虚拟机可能会从堆栈跟踪中忽略一个或多个堆栈帧。在极端情况下，
允许没有有关此 Throwable 的堆栈跟踪信息将会从此方法返回零长度数组。

一般来说，此方法返回的数组中的每个元素表示一帧。这个数组将由 printStackTrace 方法打印。

在返回的数组中写入不会影响以后对该方法的调用。
*/
public StackTraceElement[] getStackTrace() {
    return getOurStackTrace().clone();
}

private synchronized StackTraceElement[] getOurStackTrace() {
    // 如果这是第一次调用此方法，则使用 backtrace 中的信息初始化 stackTrace
    if (stackTrace == UNASSIGNED_STACK ||
        (stackTrace == null && backtrace != null) /* Out of protocol state */) {
        int depth = getStackTraceDepth();
        stackTrace = new StackTraceElement[depth];
        for (int i=0; i < depth; i++)
            stackTrace[i] = getStackTraceElement(i);
    } else if (stackTrace == null) {
        return UNASSIGNED_STACK;
    }
    return stackTrace;
}

// 返回堆栈跟踪中的元素数（如果堆栈跟踪不可用，则返回0）
native int getStackTraceDepth();

// 返回指定下标的堆栈跟踪元素
native StackTraceElement getStackTraceElement(int index);
```

## 3.6 printStackTrace
```java
/*
将这个 Throwable 及其栈帧打印到标准错误流。此方法在标准错误输出流 System.err 上打印此 Throwable 对象的堆栈跟踪。
输出的第一行包含此对象的 toString() 方法的结果。剩余的行表示先前由 fillInStackTrace() 方法记录的数据。
此信息的格式取决于实现，但是以下示例可以视为典型示例：
       class MyClass {
           public static void main(String[] args) {
               crunch(null);
           }
           static void crunch(int[] a) {
               mash(a);
           }
           static void mash(int[] b) {
               System.out.println(b[0]);
           }
       }

       java.lang.NullPointerException
               at MyClass.mash(MyClass.java:9)
               at MyClass.crunch(MyClass.java:6)
               at MyClass.main(MyClass.java:3)


具有 non-null cause 的 Throwable 的异常跟踪通常应包括该 cause 的跟踪。此信息的格式取决于实现，但是以下示例可以视为典型示例：
       class HighLevelException extends Exception {
           HighLevelException(Throwable cause) { super(cause); }
       }
       class MidLevelException extends Exception {
           MidLevelException(Throwable cause)  { super(cause); }
       }
       class LowLevelException extends Exception {
       }

       public class Junk {
           public static void main(String args[]) {
               try {
                   a();
               } catch(HighLevelException e) {
                   e.printStackTrace();
               }
           }
           static void a() throws HighLevelException {
               try {
                   b();
               } catch(MidLevelException e) {
                   throw new HighLevelException(e);
               }
           }
           static void b() throws MidLevelException {
               c();
           }
           static void c() throws MidLevelException {
               try {
                   d();
               } catch(LowLevelException e) {
                   throw new MidLevelException(e);
               }
           }
           static void d() throws LowLevelException {
              e();
           }
           static void e() throws LowLevelException {
               throw new LowLevelException();
           }
       }

       HighLevelException: MidLevelException: LowLevelException
               at Junk.a(Junk.java:13)
               at Junk.main(Junk.java:4)
       Caused by: MidLevelException: LowLevelException
               at Junk.c(Junk.java:23)
               at Junk.b(Junk.java:17)
               at Junk.a(Junk.java:11)
               ... 1 more
       Caused by: LowLevelException
               at Junk.e(Junk.java:30)
               at Junk.d(Junk.java:27)
               at Junk.c(Junk.java:21)
               ... 3 more

注意结尾包含折叠简写“... n more” 。n 表示此异常的堆栈跟踪的其余帧数，这些帧与由该异常引起的异常的堆栈跟踪底部的帧相同。
这种简写可以极大地减少输出的长度。
例如上面的“... 1 more”和“at Junk.main(Junk.java:4)”匹配，“... 3 more”和
“at Junk.b(Junk.java:17)
at Junk.a(Junk.java:11)
at Junk.main(Junk.java:4)”匹配。
可以看到每个异常显示的栈帧都包含了抛出此异常到捕获此异常的调用方法及位置。
需要注意“at Junk.a(Junk.java:13)”和“at Junk.a(Junk.java:11)”是不同的帧，分别表示 HighLevelException 被捕获的位置
和 MidLevelException 被抛出的位置。


从 Java7 开始，支持抑制异常的概念（与 try-with-resources 语句结合使用）。
为了传递异常而被抑制的所有异常都在堆栈跟踪下方打印出来。 此信息的格式取决于实现，但是以下示例可以视为典型示例：
       Exception in thread "main" java.lang.Exception: Something happened
        at Foo.bar(Foo.java:10)
        at Foo.main(Foo.java:5)
        Suppressed: Resource$CloseFailException: Resource ID = 0
                at Resource.close(Resource.java:26)
                at Foo.bar(Foo.java:9)
                ... 1 more

请注意，“ ... n more”简写用在抑制的异常上，仅当它是 cause 时。


异常既可以有 cause，也可以具有一个或多个抑制的异常：
       Exception in thread "main" java.lang.Exception: Main block
        at Foo3.main(Foo3.java:7)
        Suppressed: Resource$CloseFailException: Resource ID = 2
                at Resource.close(Resource.java:26)
                at Foo3.main(Foo3.java:5)
        Suppressed: Resource$CloseFailException: Resource ID = 1
                at Resource.close(Resource.java:26)
                at Foo3.main(Foo3.java:5)
       Caused by: java.lang.Exception: I did it
        at Foo3.main(Foo3.java:8)
       

同样，受抑制的异常可能有 cause：
       Exception in thread "main" java.lang.Exception: Main block
        at Foo4.main(Foo4.java:6)
        Suppressed: Resource2$CloseFailException: Resource ID = 1
                at Resource2.close(Resource2.java:20)
                at Foo4.main(Foo4.java:5)
        Caused by: java.lang.Exception: Rats, you caught me
                at Resource2$CloseFailException.<init>(Resource2.java:45)
                ... 2 more
*/
public void printStackTrace() {
    printStackTrace(System.err);
}

// 将这个 Throwable 及其调用跟踪打印到指定的 PrintStream。格式参见 printStackTrace() 方法
public void printStackTrace(PrintStream s) {
    printStackTrace(new WrappedPrintStream(s));
}

// 将这个 Throwable 及其调用跟踪打印到指定的 PrintWriter。格式参见 printStackTrace() 方法
public void printStackTrace(PrintWriter s) {
    printStackTrace(new WrappedPrintWriter(s));
}

private void printStackTrace(PrintStreamOrWriter s) {
    // 为了防止覆盖了 Throwable.equals 方法，使用 IdentityHashMap。
    // IdentityHashMap 比较键（和值）时使用引用相等性代替对象相等性，也就是说使用 == 而不是使用 equals。
    // Collections.newSetFromMap 返回由指定 Map 支持的 Set。此 Set 将和指定 Map 具有相同的排序，并发和性能特征。 
    Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    dejaVu.add(this);

    // 锁定输出对象
    synchronized (s.lock()) {
        // 首先打印异常信息
        s.println(this);
        // 获取堆栈栈帧信息
        StackTraceElement[] trace = getOurStackTrace();
        // 打印堆栈栈帧信息
        for (StackTraceElement traceElement : trace)
            s.println("\tat " + traceElement);

        // 如果有被抑制的异常，打印它的信息
        for (Throwable se : getSuppressed())
            se.printEnclosedStackTrace(s, trace, SUPPRESSED_CAPTION, "\t", dejaVu);

        // 如果有 cause 异常，打印它的信息
        Throwable ourCause = getCause();
        if (ourCause != null)
            ourCause.printEnclosedStackTrace(s, trace, CAUSE_CAPTION, "", dejaVu);
    }
}

/*
利用上游异常信息打印当前下游异常。

@param s: 上游异常输出流
@param enclosingTrace: 上游异常栈帧
@param caption: 当前异常前缀
@param prefix: 打印的每行前缀
@param dejaVu: 所有上游异常对象
*/
private void printEnclosedStackTrace(PrintStreamOrWriter s,
                                     StackTraceElement[] enclosingTrace,
                                     String caption,
                                     String prefix,
                                     Set<Throwable> dejaVu) {
    // 保证当前线程持有输出流上的锁
    assert Thread.holdsLock(s.lock());
    if (dejaVu.contains(this)) {
        // 如果上游异常包含当前异常，打印 CIRCULAR REFERENCE
        s.println("\t[CIRCULAR REFERENCE:" + this + "]");
    } else {
        // 将当前异常添加到上游异常中
        dejaVu.add(this);
        // 计算此异常栈帧和之前共有的帧数
        StackTraceElement[] trace = getOurStackTrace();
        int m = trace.length - 1;
        int n = enclosingTrace.length - 1;
        // 从栈底开始计算
        while (m >= 0 && n >=0 && trace[m].equals(enclosingTrace[n])) {
            m--; n--;
        }
        // 相同栈帧数
        int framesInCommon = trace.length - 1 - m;

        // 打印当前异常栈帧
        s.println(prefix + caption + this);
        for (int i = 0; i <= m; i++)
            s.println(prefix + "\tat " + trace[i]);
        if (framesInCommon != 0)
            s.println(prefix + "\t... " + framesInCommon + " more");

        // 递归打印被抑制异常信息
        for (Throwable se : getSuppressed())
            se.printEnclosedStackTrace(s, trace, SUPPRESSED_CAPTION, prefix +"\t", dejaVu);

        // 递归打印 cause 异常信息
        Throwable ourCause = getCause();
        if (ourCause != null)
            ourCause.printEnclosedStackTrace(s, trace, CAUSE_CAPTION, prefix, dejaVu);
    }
}
```

## 3.7 setStackTrace
```java
/*
设置将由 getStackTrace() 返回并由 printStackTrace() 和相关方法打印的堆栈跟踪元素。
此方法旨在供 RPC 框架和其他高级系统使用，它允许客户端覆盖在构建 Throwable 时由 fillInStackTrace() 生成
或在从序列化流中读取 Throwable 时反序列化的默认堆栈跟踪。

如果此 Throwable 的堆栈跟踪是不可写的，则调用此方法除了验证其参数外没有其他作用。
*/
public void setStackTrace(StackTraceElement[] stackTrace) {
    // 验证参数，防止有为 null 的栈帧
    StackTraceElement[] defensiveCopy = stackTrace.clone();
    for (int i = 0; i < defensiveCopy.length; i++) {
        if (defensiveCopy[i] == null)
            throw new NullPointerException("stackTrace[" + i + "]");
    }

    synchronized (this) {
        // 根据 1.2 节所述，当 stackTrace 或 backtrace 为 null 时，表示对象不可写
        if (this.stackTrace == null && // Immutable stack
            backtrace == null) // Test for out of protocol state
            return;
        this.stackTrace = defensiveCopy;
    }
}
```

## 3.8 suppress
```java
/*
将被抑制的异常附加到当前异常上。此方法是线程安全的，通常由 try-with-resources 语句（自动和隐式）调用。

除非通过构造函数禁用抑制行为，默认将启用抑制行为。禁用抑制后，此方法除了验证其参数外不执行其他操作。

请注意，当一个异常导致另一个异常时，通常会捕获第一个异常，然后作为响应抛出第二个异常。换句话说，两个异常之间存在因果关系。
在某些情况下，可能在同级代码块中引发两个独立的异常，例如在 try-with-resources 语句的 try 块中引发处理异常，
和编译器生成的 finally 块中关闭资源会引发关闭异常。在这些情况下，只能传播引发的异常之一。

在 try-with-resources 语句中，当有两个这样的异常时，将传播来自 try 块的异常，并将 finally 块的异常添加到
由 try 块异常的抑制异常列表中。它可以累积多个抑制的异常。

请注意，在有多个同级异常并且只能传播一个的情况下，程序员编写的代码也可以利用此方法。
*/
public final synchronized void addSuppressed(Throwable exception) {
    // 被抑制的异常不能为自身和 null
    if (exception == this)
        throw new IllegalArgumentException(SELF_SUPPRESSION_MESSAGE, exception);

    if (exception == null)
        throw new NullPointerException(NULL_CAUSE_MESSAGE);

    // suppressedExceptions 为 null，则不能写入
    if (suppressedExceptions == null)
        return;

    // suppressedExceptions 等于哨兵值，则进行初始化
    if (suppressedExceptions == SUPPRESSED_SENTINEL)
        suppressedExceptions = new ArrayList<>(1);

    suppressedExceptions.add(exception);
}

/*
返回一个数组，该数组包含通常被 try-with-resources 语句抑制的所有异常。
如果没有抑制异常或禁用抑制，则返回一个空数组。

此方法是线程安全的。向返回的数组写入不会影响以后对该方法的调用。
*/
public final synchronized Throwable[] getSuppressed() {
    if (suppressedExceptions == SUPPRESSED_SENTINEL ||
        suppressedExceptions == null)
        return EMPTY_THROWABLE_ARRAY;
    else
        // toArray(new Object [0]) 在功能上与 toArray() 相同。
        return suppressedExceptions.toArray(EMPTY_THROWABLE_ARRAY);
}
```

## 3.9 序列化
```java
private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();     // read in all fields

    // 将抑制的异常和堆栈跟踪元素字段设置为哨兵值。先要对序列化流中的内容进行验证。
    List<Throwable> candidateSuppressedExceptions = suppressedExceptions;
    suppressedExceptions = SUPPRESSED_SENTINEL;

    StackTraceElement[] candidateStackTrace = stackTrace;
    // 如果 stackTrace 的值是 UNASSIGNED_STACK，getOurStackTrace 方法将会根据 backtrace 中的信息构造 stackTrace，
    // 而这是从序列化中得到的异常，我们不想再次构造它。
    // 所以将 UNASSIGNED_STACK 的克隆赋值给 stackTrace，这样在 getOurStackTrace 中和 UNASSIGNED_STACK 
    // 进行 == 比较时，就不会使用到 backtrace。
    stackTrace = UNASSIGNED_STACK.clone();

    if (candidateSuppressedExceptions != null) {
        int suppressedSize = validateSuppressedExceptionsList(candidateSuppressedExceptions);
        if (suppressedSize > 0) { // Copy valid Throwables to new list
            List<Throwable> suppList  = new ArrayList<>(Math.min(100, suppressedSize));

            for (Throwable t : candidateSuppressedExceptions) {
                // 被抑制的异常不能是 null 或者自身
                if (t == null)
                    throw new NullPointerException(NULL_CAUSE_MESSAGE);
                if (t == this)
                    throw new IllegalArgumentException(SELF_SUPPRESSION_MESSAGE);
                suppList.add(t);
            }
            // 验证通过，赋值给抑制的异常字段
            suppressedExceptions = suppList;
        }
    } else {
        suppressedExceptions = null;
    }

    if (candidateStackTrace != null) {
        // 使用 candidateStackTrace 的克隆来确保检查的一致性。
        candidateStackTrace = candidateStackTrace.clone();
        if (candidateStackTrace.length >= 1) {
            if (candidateStackTrace.length == 1 &&
                    SentinelHolder.STACK_TRACE_ELEMENT_SENTINEL.equals(candidateStackTrace[0])) {
                // 如果 candidateStackTrace 是一个表示不可变的哨兵堆栈，那么将 stackTrace 设置为 null 表示不可变
                stackTrace = null;
            } else {
                // 验证栈帧元素非 null
                for (StackTraceElement ste : candidateStackTrace) {
                    if (ste == null)
                        throw new NullPointerException("null StackTraceElement in serial stream.");
                }
                stackTrace = candidateStackTrace;
            }
        }
    }
    // 序列化得到的 null stackTrace 字段可能是由于在较旧的 JDK 版本中序列化没有该字段的异常产生的；
    // 通过将 stackTrace 复制为 UNASSIGNED_STACK 的克隆，将此类异常视为具有空堆栈跟踪。
}

// 验证抑制异常列表
private int validateSuppressedExceptionsList(List<Throwable> deserSuppressedExceptions) throws IOException {
    // 验证抑制异常列表是否是被 BootstrapClassLoader 加载
    boolean isBootstrapClassLoader;
    try {
        ClassLoader cl = deserSuppressedExceptions.getClass().getClassLoader();
        isBootstrapClassLoader = (cl == null);
    } catch (SecurityException exc) {
        isBootstrapClassLoader = false;
    }

    if (!isBootstrapClassLoader) {
        throw new StreamCorruptedException("List implementation class was not loaded" +
                                           " by bootstrap class loader.");
    } else {
        // 验证大小是否非负
        int size = deserSuppressedExceptions.size();
        if (size < 0) {
            throw new StreamCorruptedException("Negative list size reported.");
        }
        return size;
    }
}

private synchronized void writeObject(ObjectOutputStream s) throws IOException {
    // 确保 stackTrace 字段初始化为非空值。从 JDK7 开始，null 堆栈跟踪字段是一个有效值，指示不应再次更改堆栈跟踪。
    getOurStackTrace();

    StackTraceElement[] oldStackTrace = stackTrace;
    try {
        if (stackTrace == null)
            // 如果 stackTrace 等于 null，就将表示不可变的哨兵对象写入序列化流中
            stackTrace = SentinelHolder.STACK_TRACE_SENTINEL;
        s.defaultWriteObject();
    } finally {
        stackTrace = oldStackTrace;
    }
}
```

# 4. 构造器

## 4.1 默认构造器
```java
// 构造一个新的 Throwable，它的 detailMessage 是 null。 cause 未初始化，并且随后可以通过调用 initCause 来初始化。
// 通过调用 fillInStackTrace() 方法以初始化新创建的 Throwable 中的堆栈跟踪数据。
public Throwable() {
    fillInStackTrace();
}
```

## 4.2 Throwable(String)
```java
// 构造一个新的 Throwable，它的 detailMessage 是 message。 cause 未初始化，并且随后可以通过调用 initCause 来初始化。
// 通过调用 fillInStackTrace() 方法以初始化新创建的 Throwable 中的堆栈跟踪数据。
public Throwable(String message) {
    fillInStackTrace();
    detailMessage = message;
}
```

## 4.3 Throwable(String, Throwable)
```java
// 构造一个新的 Throwable，它的 detailMessage 是 message。cause 是给定的 cause。
// 通过调用 fillInStackTrace() 方法以初始化新创建的 Throwable 中的堆栈跟踪数据。
public Throwable(String message, Throwable cause) {
    fillInStackTrace();
    detailMessage = message;
    this.cause = cause;
}
```

## 4.4 Throwable(Throwable)
```java
// 构造一个新的 Throwable，如果 cause 不为 null，则它的 detailMessage 是 cause.toString()。
// 通过调用 fillInStackTrace() 方法以初始化新创建的 Throwable 中的堆栈跟踪数据。
public Throwable(Throwable cause) {
    fillInStackTrace();
    detailMessage = (cause==null ? null : cause.toString());
    this.cause = cause;
}
```

## 4.5 状态构造器
```java
/*
构造一个新的 Throwable，它的 detailMessage 是 message。

enableSuppression 启用或禁用抑制， writableStackTrace 表示堆栈跟踪是否可写。

如果禁用了抑制， getSuppressed 方法将返回零长度的数组，并且对 addSuppressed 调用将无效。

如果 writableStackTrace 为 false，则此构造方法将不会调用 fillInStackTrace()，并将 null 写入 stackTrace 字段，
并且随后对 fillInStackTrace 和 setStackTrace(StackTraceElement[]) 的调用不会写入堆栈跟踪。
getStackTrace将返回长度为零的数组。

请注意，Throwable 的其他构造函数将抑制视为已启用，并将堆栈跟踪视为可写。Throwable 子类应记录禁用抑制的任何情况，
以及记录堆栈跟踪不可写的情况。仅在特殊情况下（例如，虚拟机在低内存情况下重用异常对象），才可以禁用抑制。
另一种情况是反复捕获和重新抛出给定异常对象（例如在两个子系统之间实现控制流），此时它可以为不可变对象。
*/
protected Throwable(String message, Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
    if (writableStackTrace) {
        fillInStackTrace();
    } else {
        stackTrace = null;
    }
    detailMessage = message;
    this.cause = cause;
    if (!enableSuppression)
        suppressedExceptions = null;
}
```


[try-with-resources]: try-with-resources及异常抑制.md