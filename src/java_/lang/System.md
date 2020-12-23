`java.lang.System`类的声明如下：
```java
public final class System
```
`System`类包含几个有用的类字段和方法。它是个工具类，无法实例化。
`System`类提供的功能包括标准输入，标准输出和标准错误输出流；用于快速复制数组的实用方法；
访问外部定义的属性和环境变量；加载文件和库的方法；等等。

`System`中包含了系统初始化的操作，参见`initializeSystemClass`方法。

# 1. 初始化
```java
/*
通过静态初始化器注册 native 代码（例如标准输入输出流）。
VM 将调用 initializeSystemClass 方法来完成和类初始化方法 clinit 分离的初始化操作。请注意，若要使用 VM 设置的属性，
请参阅 initializeSystemClass 方法中描述的约束。
*/
private static native void registerNatives();
static {
    registerNatives();
}

// 不能实例化 System
private System() {
}
```

# 2. 成员字段

## 2.1 标准流
```java
// “标准”输入流。该流已经打开，可以读取输入。通常，此流对应于键盘输入或主机环境、用户指定的另一个输入源
public final static InputStream in = null;

// “标准”输出流。该流已经打开，并准备接受输出数据。通常，此流对应于显示输出（如控制台）或主机环境、用户指定的或另一个输出目标
public final static PrintStream out = null;

// “标准”错误输出流。该流已经打开，并准备接受输出数据。通常，此流对应于显示输出（如控制台）或主机环境、用户指定的或另一个输出目标。
// 按照约定，即使主要输出流（变量 out 的值）已重定向到文件或其他目标位置，该输出流也用于显示错误消息或其他应引起用户的注意的信息
public final static PrintStream err = null;
```

## 2.2 security
```java
// 默认安全管理器
private static volatile SecurityManager security = null;
```
参见 [SecurityManager.md][security-manager] 和 [安全管理器.md][sm]。

## 2.3 console
```java
// 系统控制台
private static volatile Console cons = null;
```

## 2.4 props
```java
// 系统属性
private static Properties props;
```
下列系统属性保证会被定义：

| 系统属性名称 | 说明 |
| ----------- | ---- |
| java.version | Java 版本号 |
| java.vendor | Java 供应商名称字符串 |
| java.vendor.url | Java 供应商 URL |
| java.home | Java 安装路径 |
| java.class.version | Java 类版本号 |
| java.class.path | Java 类路径 |
| os.name | 操作系统名称 |
| os.arch | 操作系统架构 |
| os.version | 操作系统版本 |
| file.separator | 文件分隔符（Unix 系统上是 "/"） |
| path.separator | 路径分隔符（Unix 系统上是 ":"） |
| line.separator | 行分隔符（Unix 系统上是 "\n"） |
| user.name | 用户账号名称 |
| user.home | 用户主目录 |
| user.dir | 用户当前工作目录 |

这些属性的测试参见[SystemTest.java][test]。

## 2.5 lineSeparator
```java
// 与系统有关的行分隔符字符串，和 line.separator 一样
// 在 UNIX 系统上，它返回 "\n"; 在 Microsoft Windows 系统上，它返回 "\r\n"。
private static String lineSeparator;
```

# 3. 方法

## 3.1 initializeSystemClass
```java
// 初始化 System。线程初始化后调用。
private static void initializeSystemClass() {
    /*
    VM 可能会调用 JNU_NewStringPlatform() 来设置一些编码敏感类属性（user.home、user.name、boot.class.path 等），
    在初始化初期，它可能需要通过 System.getProperty() 访问初始化（放入"props"）相关的系统属性。
    因此，请确保在初始化开始时提供"props"，并直接将所有系统属性放入其中。
     */
    props = new Properties();
    initProperties(props);  // 由 VM 进行初始化

    /*
    VM 选项（java -xx）可能控制某些系统配置，例如 Integer 缓存池大小。通常，库将从 VM 设置的属性获取这些值。
    如果属性仅供内部实现使用，则应从系统属性中删除这些属性。
    java.lang.Integer.IntegerCache 和 sun.misc.VM.saveAndRemoveProperties() 是一些例子。
    
    保存系统属性对象的私有副本，该对象只能由内部实现访问。删除不适合公共访问的某些系统属性。
     */
    sun.misc.VM.saveAndRemoveProperties(props);

    lineSeparator = props.getProperty("line.separator");
    sun.misc.Version.init();

    // 设置标准流，可以看到它们默认都是 Buffer 形式的
    FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
    FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
    FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
    setIn0(new BufferedInputStream(fdIn));
    // 使用指定编码创建 stdout/stderr 流
    setOut0(newPrintStream(fdOut, props.getProperty("sun.stdout.encoding")));
    setErr0(newPrintStream(fdErr, props.getProperty("sun.stderr.encoding")));

    // 现在加载 zip 库，以保证 java.util.zip.ZipFile 以后不会尝试使用自身加载此库。
    loadLibrary("zip");

    // 为 HUP、TERM 和 INT 设置 Java 信号处理程序（如果可用）。
    Terminator.setup();

    /*
    初始化需要为类库设置其他操作系统设置。目前，除了需要为 Windows 在 java.io 类使用之前设置
    process-wide 错误模式外，没有进行其他操作。
     */
    sun.misc.VM.initializeOSEnvironment();

    // 主线程不会以与其他线程相同的方式添加到其线程组，我们必须在这里自己做。
    Thread current = Thread.currentThread();
    current.getThreadGroup().add(current);

    // 注册 Java lang 包共享给外部的操作
    setJavaLangAccess();

    // 初始化期间调用的子系统需要调用 sun.misc.VM.isBooted() 以避免执行应该在应用加载程序设置之前的操作。
    // 确保这是最后的初始化操作！
    sun.misc.VM.booted();
}

private static native Properties initProperties(Properties props);

// 根据编码为 stdout/err 创建 PrintStream。
private static PrintStream newPrintStream(FileOutputStream fos, String enc) {
    if (enc != null) {
        try {
            return new PrintStream(new BufferedOutputStream(fos, 128), true, enc);
        } catch (UnsupportedEncodingException uee) {}
    }
    return new PrintStream(new BufferedOutputStream(fos, 128), true);
}

private static void setJavaLangAccess() {
    // 允许 java.lang 之外的特权类
    sun.misc.SharedSecrets.setJavaLangAccess(new sun.misc.JavaLangAccess(){
        public sun.reflect.ConstantPool getConstantPool(Class<?> klass) {
            return klass.getConstantPool();
        }
        public boolean casAnnotationType(Class<?> klass, AnnotationType oldType, AnnotationType newType) {
            return klass.casAnnotationType(oldType, newType);
        }
        public AnnotationType getAnnotationType(Class<?> klass) {
            return klass.getAnnotationType();
        }
        public Map<Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap(Class<?> klass) {
            return klass.getDeclaredAnnotationMap();
        }
        public byte[] getRawClassAnnotations(Class<?> klass) {
            return klass.getRawAnnotations();
        }
        public byte[] getRawClassTypeAnnotations(Class<?> klass) {
            return klass.getRawTypeAnnotations();
        }
        public byte[] getRawExecutableTypeAnnotations(Executable executable) {
            return Class.getExecutableTypeAnnotationBytes(executable);
        }
        public <E extends Enum<E>> E[] getEnumConstantsShared(Class<E> klass) {
            return klass.getEnumConstantsShared();
        }
        public void blockedOn(Thread t, Interruptible b) {
            t.blockedOn(b);
        }
        public void registerShutdownHook(int slot, boolean registerShutdownInProgress, Runnable hook) {
            Shutdown.add(slot, registerShutdownInProgress, hook);
        }
        public int getStackTraceDepth(Throwable t) {
            return t.getStackTraceDepth();
        }
        public StackTraceElement getStackTraceElement(Throwable t, int i) {
            return t.getStackTraceElement(i);
        }
        public String newStringUnsafe(char[] chars) {
            return new String(chars, true);
        }
        public Thread newThreadWithAcc(Runnable target, AccessControlContext acc) {
            return new Thread(target, acc);
        }
        public void invokeFinalize(Object o) throws Throwable {
            o.finalize();
        }
    });
}
```

## 3.2 重定向标准流
```java
/*
重定向“标准”输入流。如果有安全管理器，则使用 RuntimePermission("setIO") 权限调用其 checkPermission 方法，
以查看是否可以重定向“标准”输入流。
*/
public static void setIn(InputStream in) {
    checkIO();
    setIn0(in);
}

/*
重定向“标准”输出流。如果有安全管理器，则使用 RuntimePermission("setIO") 权限调用其 checkPermission 方法，
以查看是否可以重定向“标准”输出流。
*/
public static void setOut(PrintStream out) {
    checkIO();
    setOut0(out);
}

/*
重定向“标准”错误输出流。如果有安全管理器，则使用 RuntimePermission("setIO") 权限调用其 checkPermission 方法，
以查看是否可以重定向“标准”错误输出流。
*/
public static void setErr(PrintStream err) {
    checkIO();
    setErr0(err);
}

// 测试是否具有 setIO 权限
private static void checkIO() {
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("setIO"));
    }
}

private static native void setIn0(InputStream in);
private static native void setOut0(PrintStream out);
private static native void setErr0(PrintStream err);
```

## 3.3 securityManager
```java
/*
设置 SecurityManager。如果已经设置了 SecurityManager，则此方法首先使用 RuntimePermission("setSecurityManager")
权限调用 SecurityManager 的 checkPermission 方法，以确保可以替换现有的 SecurityManager。这可能导致抛出 SecurityException。
否则，将参数设置为当前 SecurityManager。

如果参数为 null 且未建立 SecurityManager，则不执行任何操作。
*/
public static void setSecurityManager(final SecurityManager s) {
    try {
        s.checkPackageAccess("java.lang");
    } catch (Exception e) {
        // no-op
    }
    setSecurityManager0(s);
}

private static synchronized void setSecurityManager0(final SecurityManager s) {
    // 判断是否有更换安全管理器的权限
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("setSecurityManager"));
    }

    // 新的安全管理器类不为 null 或不在 bootstrap 类路径上
    if ((s != null) && (s.getClass().getClassLoader() != null)) {
        /*
        我们需要确保新的安全管理器类不在 bootstrap 类路径上，这会使安全策略在设置新安全管理器之前得到初始化，
        以防止在尝试初始化策略时出现无限循环（这通常涉及访问某些安全和/或系统属性，它们会反过来调用已安装的安全管理器的
        checkPermission 方法，如果堆栈上存在非系统类（例如新安全管理器类），该方法将无限循环）。
        */
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                s.getClass().getProtectionDomain().implies(SecurityConstants.ALL_PERMISSION);
                return null;
            }
        });
    }

    security = s;
}

// 如果已经为当前应用程序建立了安全管理器，则返回该安全管理器；否则，返回 null。
// 默认情况返回 null。
public static SecurityManager getSecurityManager() {
    return security;
}
```

## 3.4 console
```java
// 返回与当前 Java 虚拟机关联的唯一 Console 对象（如果有）。
public static Console console() {
    if (cons == null) {
        synchronized (System.class) {
            cons = sun.misc.SharedSecrets.getJavaIOAccess().console();
        }
    }
    return cons;
}
```

## 3.5 inheritedChannel
```java
/*
返回从创建此 Java 虚拟机的实体继承的 Channel。

此方法返回通过调用系统默认的 SelectorProvider 对象的 inheritedChannel 方法获得的通道。
除了在 SelectorProvider.inheritedChannel 方法所描述的面向网络的信道 inheritedChannel，
此方法可能会在将来返回其他信道。
*/
public static Channel inheritedChannel() throws IOException {
    return SelectorProvider.provider().inheritedChannel();
}
```

## 3.6 time
```java
/*
返回当前时间（以毫秒为单位）。请注意，虽然返回值的时间单位为毫秒，但该值的粒度取决于基础操作系统。
例如，许多操作系统以几十毫秒为单位测量时间。

有关“计算机时间”与 UTC 之间可能出现的细微差异的讨论，请参见 Date 类的描述。
*/
public static native long currentTimeMillis();

/*
返回正在运行的 Java 虚拟机的高分辨率时间源的当前值，以纳秒为单位。

此方法只能用于测量经过时间，并且与系统或 wall-clock 的任何其他概念无关。返回的值表示自某个固定但任意的起始时间以来的纳秒
（也许是将来的时间，因此值可能为负）。在 Java 虚拟机的实例中，此方法的所有调用都使用相同的源。其他虚拟机实例可能使用其他来源。

此方法不一定提供纳秒级精度.除了分辨率至少与 currentTimeMillis() 一样好以外，不做任何保证。

由于数值上溢，跨越大约 292 年（2^63纳秒）的连续调用将无法正确计算经过时间。
仅当计算在 Java 虚拟机的同一实例中获得的两个此类值之间的差时，此方法返回的值才有意义。
*/
public static native long nanoTime();
```
在一些系统调用中需要指定时间是用`CLOCK_MONOTONIC`还是`CLOCK_REALTIME`（`linux`中的概念）。
`CLOCK_MONOTONIC`是`monotonic time`，而`CLOCK_REALTIME`是`wall time`。

`monotonic time`字面意思是单调时间，实际上它指的是系统启动以后流逝的时间，这是由变量`jiffies`来记录的。
系统每次启动时`jiffies`初始化为 0，每来一个时钟中断，`jiffies`加 1，也就是说它代表系统启动后流逝的`tick`数。
`jiffies`一定是单调递增的，因为时间不可逆。

`wall time`字面意思是挂钟时间，实际上就是指的是现实的时间，这是由变量`xtime`来记录的。
系统每次启动时将`CMOS`上的`RTC`时间读入`xtime`，这个值是自 1970-01-01 起经历的秒数、本秒中经历的纳秒数，
每来一个时钟中断，也需要去更新`xtime`。

需要注意，`wall time`不一定是单调递增的。因为`wall time`是指现实中的实际时间，如果系统要与网络中某个节点时间同步、
或者由系统管理员觉得这个`wall time`与现实时间不一致，有可能任意的改变这个`wall time`。最简单的例子是，
我们用户可以去任意修改系统时间，这个被修改的时间就是`wall time`，即`xtime`，它甚至可以被写入`RTC`而永久保存。
一些应用软件可能就是用到了这个`wall time`，比如以前的 VMWare Workstation，一启动提示试用期已过，
但是只要把系统时间调整一下提前一年，再启动就不会有提示了，这很可能就是因为它启动时用`gettimeofday`去读`wall time`，
然后判断是否过期，只要将`wall time`改一下，就可以欺骗过去了。

## 3.7 arraycopy
```java
// 从指定的源数组（从指定位置开始）复制数据到目标数组的指定位置。复制的元素数等于 length 参数。
public static native void arraycopy(Object src,  int  srcPos,
                                    Object dest, int destPos,
                                    int length);
```

## 3.8 identityHashCode
```java
// 无论给定对象的类是否覆盖 hashCode()，都为给定对象返回与默认 hashCode() 相同的哈希码。空引用的哈希码为零。
public static native int identityHashCode(Object x);
```

## 3.9 properties
```java
/*
获取当前系统属性。如果有安全管理器，则不带任何参数调用其 checkPropertiesAccess 方法。这可能会导致 SecurityException。

如果没有系统属性，则首先创建并初始化一组系统属性。这套系统属性始终包含 2.4 节 props 中声明的属性。
系统属性值中的多个路径由平台的路径分隔符分隔。

请注意，即使安全管理器不允许 getProperties 操作，它也可以选择允许 getProperty(String) 操作。
*/
public static Properties getProperties() {
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPropertiesAccess();
    }

    return props;
}

/*
将系统属性设置为 Properties 参数。如果有安全管理器，则不带任何参数调用其 checkPropertiesAccess 方法。
这可能会导致  SecurityException。

该参数会成为 getProperty(String) 方法使用的当前系统属性集。如果参数为 null，则将当前的系统属性集也设为 null。
*/
public static void setProperties(Properties props) {
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPropertiesAccess();
    }
    if (props == null) {
        props = new Properties();
        initProperties(props);
    }
    System.props = props;
}

/*
获取指定键的系统属性。如果有安全管理器，则将 key 作为参数调用其 checkPropertyAccess 方法。这可能会导致 SecurityException。
如果没有当前的系统属性集，则首先以与 getProperties 方法相同的方式创建和初始化一组系统属性。
*/
public static String getProperty(String key) {
    checkKey(key);
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPropertyAccess(key);
    }

    return props.getProperty(key);
}

/*
获取指定键的系统属性。如果有安全管理器，则将 key 作为参数调用其 checkPropertyAccess 方法。这可能会导致 SecurityException。
如果没有当前的系统属性集，则首先以与 getProperties 方法相同的方式创建和初始化一组系统属性。

如果系统属性不存在，返回给定的默认值 def。
*/
public static String getProperty(String key, String def) {
    checkKey(key);
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPropertyAccess(key);
    }

    return props.getProperty(key, def);
}

/*
设置由指定键和它关联的系统属性。如果存在安全管理器，则使用 PropertyPermission(key, "write") 权限
调用其 checkPermission 方法。这可能会导致 SecurityException。
如果没有抛出异常，则将指定的属性设置为给定值。
*/
public static String setProperty(String key, String value) {
    checkKey(key);
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new PropertyPermission(key, SecurityConstants.PROPERTY_WRITE_ACTION));
    }

    return (String) props.setProperty(key, value);
}

/*
删除由指定键的系统属性。如果存在安全管理器，则使用 PropertyPermission(key, "write") 权限调用其
checkPermission 方法。这可能会导致 SecurityException。如果没有抛出异常，则删除指定的属性。
*/
public static String clearProperty(String key) {
    checkKey(key);
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new PropertyPermission(key, "write"));
    }

    return (String) props.remove(key);
}

// 检查属性键
private static void checkKey(String key) {
    if (key == null) {
        throw new NullPointerException("key can't be null");
    }
    if (key.equals("")) {
        throw new IllegalArgumentException("key can't be empty");
    }
}
```

## 3.10 lineSeparator
```java
public static String lineSeparator() {
    return lineSeparator;
}
```

## 3.11 getenv
```java
/*
获取指定环境变量的值。环境变量是与系统有关的外部命名值。环境变量名忽略大小写。

如果存在安全管理器，则会使用 RuntimePermission("getenv."+name) 权限来调用其 checkPermission方法。
这可能会导致引发 SecurityException。如果没有抛出异常，则返回变量 name 的值。
*/
public static String getenv(String name) {
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("getenv."+name));
    }

    return ProcessEnvironment.getenv(name);
}

// 返回当前系统环境的所有环境变量，返回的 Map 不可更改。环境变量从父进程传递到子进程。
// 如果系统不支持环境变量，则返回一个空映射。
// 此方法返回的 Map 对大小写敏感
public static java.util.Map<String,String> getenv() {
    SecurityManager sm = getSecurityManager();
    if (sm != null) {
        sm.checkPermission(new RuntimePermission("getenv.*"));
    }

    return ProcessEnvironment.getenv();
}
```
关于`getenv(String)`和`getenv()`方法之间的差异，参见[ProcessEnvironment.md][pe]。

## 3.12 exit
```java
// 终止当前正在运行的 Java 虚拟机。参数用作状态码；按照惯例，非零状态代码表示异常终止。
// 此方法调用 Runtime.exit 方法。
public static void exit(int status) {
    Runtime.getRuntime().exit(status);
}
```

## 3.13 gc
```java
/*
运行垃圾收集器，Java 虚拟机将花费更多精力来回收未使用的对象，以使它们当前占用的内存可用于快速重用。
当从方法调用返回时，Java 虚拟机已尽最大努力从所有丢弃的对象中回收空间。
*/
public static void gc() {
    Runtime.getRuntime().gc();
}
```

## 3.14 runFinalization
```java
/*
运行不可达且没有运行 finalize 的对象的 finalize 方法。Java 虚拟机会花更多精力来运行已发现被丢弃但尚未运行其
finalize 方法的对象的 finalize 方法。当从方法调用返回时，Java 虚拟机将尽最大努力完成所有终结工作。
*/
public static void runFinalization() {
    Runtime.getRuntime().runFinalization();
}

// 已废弃，参见 Runtime.runFinalizersOnExit()
@Deprecated
public static void runFinalizersOnExit(boolean value) {
    Runtime.runFinalizersOnExit(value);
}
```

## 3.15 load
```java
/*
加载由 filename 参数指定的本机库。filename 参数必须是绝对路径名。
更多说明参见 Runtime.load() 方法。
*/
// 注解 @CallerSensitive 的方法对其调用类敏感
@CallerSensitive
public static void load(String filename) {
    Runtime.getRuntime().load0(Reflection.getCallerClass(), filename);
}

// 加载由 libname 参数指定的本地库。libname 参数不得包含任何平台特定的前缀，文件扩展名或路径。
// 更多说明参见 Runtime.loadLibrary() 方法。
@CallerSensitive
public static void loadLibrary(String libname) {
    Runtime.getRuntime().loadLibrary0(Reflection.getCallerClass(), libname);
}
```

## 3.16 mapLibraryName
```java
// 将库名称映射为表示特定于平台的本地库字符串中。
public static native String mapLibraryName(String libname);
```


[security-manager]: SecurityManager.md
[sm]: 安全管理器.md
[test]: ../../../test/java_/lang/SystemTest.java
[pe]: ProcessEnvironment.md