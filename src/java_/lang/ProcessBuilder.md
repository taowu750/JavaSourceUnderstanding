`java.lang.ProcessBuilder`类的声明如下：
```java
public final class ProcessBuilder
```
此类用于创建操作系统进程。每个`ProcessBuilder`实例管理一系列进程属性。`start()`方法使用这些属性创建一个新的`Process`实例。
可以从同一实例重复调用`start()`方法，以创建具有相同或相关属性的新子进程。参见[Process.md][process]

每个`ProcessBuilder`都管理以下进程属性：
1. **命令**。一个字符串列表，表示要调用的外部程序文件及其参数（如果有）。哪个字符串列表表示有效的操作系统命令取决于系统。
例如，通常每个概念性参数都是该列表中的一个元素，但是在某些操作系统中，程序本身应该对命令行字符串进行解析分割。
在这种系统上，`Java`实现可能要求命令恰好包含两个元素。
2. **环境**。依赖于系统的从变量到值的映射。初始值是当前进程环境的副本（请参见`System.getenv()`）。
3. **工作目录**。默认值是当前进程的当前工作目录，通常是系统属性`user.dir`命名的目录。
4. **标准输入的来源**。默认情况下，子进程从管道读取输入。`Java`代码可以通过`Process.getOutputStream()`返回的输出流访问此管道。
然而，标准的输入可使用`redirectInput`方法被重定向到另一来源。在这种情况下，`Process.getOutputStream()`将返回一个空输出流，
它的
    - `write`方法总是抛出`IOException`。
    - `close`方法不做任何事。
5. **标准输出和标准错误的目的地**。默认情况下，子进程将标准输出和标准错误写入管道。
`Java`代码可以通过`Process.getInputStream()`和`Process.getErrorStream()`返回的输入流访问这些管道。
但是，可以使用`redirectOutput`和`redirectError`方法将标准输出和标准错误重定向到其他目的地。在这种情况下，
`Process.getInputStream()`和/或`Process.getErrorStream()`将返回空输入流，它的
    - `read`方法始终返回 -1。
    - `available`方法总是返回 0。
    - `close`方法不做任何事。
6. **redirectErrorStream 属性**。最初，此属性为`false`，这意味着子进程的标准输出和错误输出将发送到两个单独的流，
可以使用`Process.getInputStream()`和`Process.getErrorStream()`方法对其进行访问。如果该值设置为`true`，则：
    - 标准错误与标准输出合并，并始终发送到同一目的地（这使将错误消息与相应的输出关联起来更加容易）。
    - 标准错误和标准输出的共同目的地可以使用`redirectOutput`重定向。
    - 创建子流程时，忽略`redirectError`方法设置的任何重定向。
    - 从`Process.getErrorStream()`返回的流将始终为空输入流。

修改`ProcessBuilder`的属性将影响随后由该对象的`start()`方法启动的进程，但绝不会影响先前启动的进程或`Java`进程本身。

大多数错误检查都是通过`start()`方法执行的。修改对象的状态可能导致`start()`失败。例如，将命令属性设置为空列表将不会引发异常，
除非调用`start()`。

请注意，此类未同步。如果多个线程同时访问`ProcessBuilder`实例，并且其中至少一个线程在结构上修改了其中一个属性，
则必须在外部对其进行同步。

启动使用默认工作目录和环境的新进程很容易：
```java
Process p = new ProcessBuilder("myCommand", "myArg").start();
```  

下面是一个示例，该示例使用修改后的工作目录和环境启动进程，然后重定向标准输出和标准错误到日志文件：
```java
ProcessBuilder pb = new ProcessBuilder("myCommand", "myArg1", "myArg2");

Map<String, String> env = pb.environment();
env.put("VAR1", "myValue");
env.remove("OTHERVAR");
env.put("VAR2", env.get("VAR1") + "suffix");

pb.directory(new File("myDir"));

File log = new File("log");
pb.redirectErrorStream(true);
pb.redirectOutput(Redirect.appendTo(log));

Process p = pb.start();
assert pb.redirectInput() == Redirect.PIPE;
assert pb.redirectOutput().file() == log;
assert p.getInputStream().read() == -1;
```
如果要使用一组明确的环境变量启动进程，请先调用`Map.clear()`然后再添加环境变量。

`ProcessBuilder`代码中比较值得注意的有：
 - 2.2 Redirect: 使用`Redirect`类封装流类型和流行为类型的操作。
 - 4.9 start: 使用安全管理器，发生异常时不要暴露底层错误细节

此类的测试参见 [ProcessTest.java][test]。

# 1. 成员字段
```java
// 命令列表
private List<String> command;
// 工作目录
private File directory;
// 环境变量
private Map<String,String> environment;
// 是否合并标准错误和标准输出
private boolean redirectErrorStream;
// 标准输入、标准输出、标准错误的重定向
private Redirect[] redirects;
```

# 2. 内部类

## 2.1 空流
```java
// 空的输入流
static class NullInputStream extends InputStream {
    static final NullInputStream INSTANCE = new NullInputStream();
    private NullInputStream() {}
    public int read()      { return -1; }
    public int available() { return 0; }
}

// 空的输出流
static class NullOutputStream extends OutputStream {
    static final NullOutputStream INSTANCE = new NullOutputStream();
    private NullOutputStream() {}
    public void write(int b) throws IOException {
        throw new IOException("Stream closed");
    }
}
```

## 2.2 Redirect
```java
public static abstract class Redirect
```
表示子进程输入的源或子进程输出的目的地。 每个`Redirect`实例都是以下之一：
 - 特殊值`Redirect.PIPE`
 - 特殊值`Redirect.INHERIT`
 - 通过调用`Redirect.from(File)`创建的从文件读取的流
 - 通过调用`Redirect.to(File)`创建的写入文件的流
 - 通过调用`Redirect.appendTo(File)`创建的追加到文件的流

以上每个类别都有一个关联的唯一`Type`。

### 2.2.1 Type
```java
public enum Type {
    PIPE,  // 管道
    INHERIT,  // 继承自父进程（大多数情况下等同于管道）
    READ,  // 从文件读
    WRITE,  // 写入文件
    APPEND  // 追加到文件
};
```

### 2.2.2 成员字段
```java
// 管道 Redirect
public static final Redirect PIPE = new Redirect() {
    public Type type() { return Type.PIPE; }
    public String toString() { return type().toString(); }
};

// 继承 Redirect
public static final Redirect INHERIT = new Redirect() {
    public Type type() { return Type.INHERIT; }
    public String toString() { return type().toString(); }
};
```

### 2.2.3 构造器
```java
// 没有公有构造器。必须使用 Redirect 的静态工厂方法构造 Redirect 对象
private Redirect() {}
```

### 2.2.4 方法
```java
// 返回当前 Redirect 的类型
public abstract Type type();

// 返回与此 Redirect 关联的 File 源或目标；如果没有此类文件，则返回 null。
public File file() { return null; }

public boolean equals(Object obj) {
    if (obj == this)
        return true;
    if (! (obj instanceof Redirect))
        return false;
    Redirect r = (Redirect) obj;
    if (r.type() != this.type())
        return false;
    assert this.file() != null;
    return this.file().equals(r.file());
}

public int hashCode() {
    File file = file();
    if (file == null)
        return super.hashCode();
    else
        return file.hashCode();
}

// 当重定向到目标文件时，指示是否将输出写入文件的末尾。
boolean append() {
    throw new UnsupportedOperationException();
}

// 返回从指定文件读取的 Redirect。
public static Redirect from(final File file) {
    if (file == null)
        throw new NullPointerException();
    return new Redirect() {
        public Type type() { return Type.READ; }
        public File file() { return file; }
        public String toString() {
            return "redirect to read from file \"" + file + "\"";
        }
    };
}

// 返回指定文件写入的 Redirect。如果在子进程启动时指定的文件存在，则此文件先前的内容将被删除。
public static Redirect to(final File file) {
    if (file == null)
        throw new NullPointerException();
    return new Redirect() {
        public Type type() { return Type.WRITE; }
        public File file() { return file; }
        public String toString() {
            return "redirect to write to file \"" + file + "\"";
        }
        boolean append() { return false; }
    };
}

// 返回追加到指定文件 Redirect。每个写入操作首先将位置前进到文件末尾，然后写入请求的数据。
// 位置的改变和数据的写入是否在单个原子操作中完成取决于系统，因此未指定。
public static Redirect appendTo(final File file) {
    if (file == null)
        throw new NullPointerException();
    return new Redirect() {
        public Type type() { return Type.APPEND; }
        public File file() { return file; }
        public String toString() {
            return "redirect to append to file \"" + file + "\"";
        }
        boolean append() { return true; }
    };
}
```

# 3. 构造器
```java
public ProcessBuilder(List<String> command) {
    if (command == null)
        throw new NullPointerException();
    this.command = command;
}

public ProcessBuilder(String... command) {
    this.command = new ArrayList<>(command.length);
    for (String arg : command)
        this.command.add(arg);
}
```

# 4. 方法

## 4.1 基本属性
```java
// 返回此 ProcessBuilder 的操作系统命令和参数。返回的列表不是副本。列表的后续更新将反映在此 ProcessBuilder 的状态中。
public List<String> command() {
    return command;
}

/*
返回此 ProcessBuilder 的环境变量 Map。每当创建 ProcessBuilder 时，环境都会初始化为当前进程环境的副本
（请参见System.getenv()）。随后由该对象的 start() 方法启动的子进程将使用此 Map 作为其环境。

可以使用常规 Map 操作修改返回的对象。这些修改对 start() 方法启动的子进程可见。
两个不同的 ProcessBuilder 实例始终包含独立的进程环境，因此对返回的 Map 所做的更改将永远不会反映在
任何其他 ProcessBuilder 实例或 System.getenv 返回的值中。

如果系统不支持环境变量，则返回一个空 Map。
返回的 Map 的行为取决于系统。系统可能不允许修改环境变量，也可能禁止某些变量名称或值。因此，
如果操作系统不允许修改，则尝试修改 Map 可能会失败，并显示 UnsupportedOperationException 或 IllegalArgumentException。

由于环境变量名称和值的外部格式取决于系统，因此它们与 Java 的 Unicode 字符串之间可能没有一对一的映射。
尽管如此，未由 Java 代码修改的环境变量将在子进程中具有未修改的本地表示形式。

返回的 Map 不允许插入空键或空值。尝试插入或查询空键或空值的存在将抛出 NullPointerException。
返回的 Map 及其集合视图可能不遵循 Object.equals 和 Object.hashCode 方法的约定。
返回的 Map 在所有平台上通常区分大小写。

如果存在安全管理器，则使用 RuntimePermission ("getenv.*") 权限调用其 checkPermission 方法。
这可能会导致引发 SecurityException 。

将信息传递给 Java 子进程时，使用系统属性（参见 System.getenv(String name)）通常优于使用环境变量。
*/
public Map<String,String> environment() {
    SecurityManager security = System.getSecurityManager();
    if (security != null)
        // 检查是否有获取系统环境变量的权限
        security.checkPermission(new RuntimePermission("getenv.*"));

    // 如果当前 environment 为空，则将当前进程环境变量的副本赋值给 environment
    if (environment == null)
        environment = ProcessEnvironment.environment();

    assert environment != null;

    return environment;
}

/*
返回此 ProcessBuilder 的工作目录。由该对象的 start() 方法启动的子进程将使用此方法的返回值作为其工作目录。

返回的值可以为 null。这意味着将当前 Java 进程的工作目录（通常由系统属性 user.dir 命名的目录）用作子进程的工作目录。
*/
public File directory() {
    return directory;
}

// 返回此 ProcessBuilder 是否合并标准错误和标准输出。初始值为 false
public boolean redirectErrorStream() {
    return redirectErrorStream;
}
```
有关环境变量，参见 [ProcessEnvironment.md][env]。

## 4.2 Redirect 属性
```java
// 返回 Redirect 数组。其中的三个值分别代表标准输入、标准输出、标准错误
private Redirect[] redirects() {
    if (redirects == null)
        redirects = new Redirect[] {
            Redirect.PIPE, Redirect.PIPE, Redirect.PIPE
    };
    return redirects;
}

// 返回此 ProcessBuilder 的标准输入源。由该对象的 start() 方法启动的子进程将使用此方法的返回值作为标准输入。
// 初始值为 Redirect.PIPE。
public Redirect redirectInput() {
    return (redirects == null) ? Redirect.PIPE : redirects[0];
}

// 返回此 ProcessBuilder 的标准输出源。由该对象的 start() 方法启动的子进程将使用此方法的返回值作为标准输出。
// 初始值为 Redirect.PIPE。
public Redirect redirectOutput() {
    return (redirects == null) ? Redirect.PIPE : redirects[1];
}

// 返回此 ProcessBuilder 的标准错误源。由该对象的 start() 方法启动的子进程将使用此方法的返回值作为标准错误。
// 初始值为 Redirect.PIPE。
public Redirect redirectError() {
    return (redirects == null) ? Redirect.PIPE : redirects[2];
}
```

## 4.3 命令属性设置
```java
// 设置此 ProcessBuilder 的操作系统命令和参数。这种方法不会复制 command。列表的后续更新将反映在流程构建器的状态中。
// 不检查 command 的命令是否有效。
public ProcessBuilder command(List<String> command) {
    if (command == null)
        throw new NullPointerException();
    this.command = command;
    return this;
}

public ProcessBuilder command(String... command) {
    this.command = new ArrayList<>(command.length);
    for (String arg : command)
        this.command.add(arg);
    return this;
}
```

## 4.4 环境变量设置
```java
// 此方法用于设置环境变量。envp 中每个字符串都是 name=value 的键值对形式。
// 此方法仅用在 Runtime.exec(envp) 方法中
ProcessBuilder environment(String[] envp) {
    assert environment == null;
    if (envp != null) {
        environment = ProcessEnvironment.emptyEnvironment(envp.length);
        assert environment != null;

        for (String envstring : envp) {
            // 在 1.5 之前，我们盲目地将无效的 env 字符串传递给子进程。
            // 为了与旧代码兼容，我们不能引发异常。

            // 环境变量字符串中不能包含 '\0'，但为了兼容性，这里只是替换它
            if (envstring.indexOf((int) '\u0000') != -1)
                envstring = envstring.replaceFirst("\u0000.*", "");

            // 找到分隔符 = 的位置。需要注意，Windows 允许将 = 作为环境变量名的第一个字符，
            // 表示一个 magic 环境变量。因此，我们需要从第二个字符开始查找。
            int eqlsign = envstring.indexOf('=', ProcessEnvironment.MIN_NAME_LENGTH);
            if (eqlsign != -1)
                // 将环境变量添加到 Map 中
                environment.put(envstring.substring(0,eqlsign),
                                envstring.substring(eqlsign+1));
            // 忽视没有 = 的非法字符串
        }
    }
    return this;
}
```

## 4.5 工作目录设置
```java
/*
设置此 ProcessBuilder 的工作目录。由该对象的 start() 方法启动的子进程将使用参数 directory 作为其工作目录。

directory 的值可以为 null。这意味着将当前 Java 进程的工作目录（通常由系统属性 user.dir 命名的目录）用作子进程的工作目录。
*/
public ProcessBuilder directory(File directory) {
    this.directory = directory;
    return this;
}
```

## 4.6 redirectErrorStream 设置
```java
// 设置此 ProcessBuilder 的 redirectErrorStream 属性。此属性表示是否合并标准输出和标准错误
public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
    this.redirectErrorStream = redirectErrorStream;
    return this;
}
```

## 4.7 Redirect 设置
```java
/*
设置此 ProcessBuilder 的标准输入源。由该对象的 start() 方法启动的子进程将使用此源获取其标准输入。

如果源是 Redirect.PIPE（初始值），则可以使用 Process.getOutputStream() 返回的输出流来写入子进程的标准输入。
如果将源设置为任何其他值，则 Process.getOutputStream() 将返回空输出流。

Redirect 对象使用 Redirect 的工厂方法构造。
*/
public ProcessBuilder redirectInput(Redirect source) {
    if (source.type() == Redirect.Type.WRITE ||
        source.type() == Redirect.Type.APPEND)
        throw new IllegalArgumentException(
            "Redirect invalid for reading: " + source);
    redirects()[0] = source;
    return this;
}

/*
设置此 ProcessBuilder 的标准输出源。由该对象的 start() 方法启动的子进程将使用此源获取其标准输出。

如果源是 Redirect.PIPE（初始值），则可以使用 Process.getInputStream() 返回的输入流来读取子进程的标准输出。
如果将源设置为任何其他值，则 Process.getInputStream() 将返回空输入流。

Redirect 对象使用 Redirect 的工厂方法构造。
*/
public ProcessBuilder redirectOutput(Redirect destination) {
    if (destination.type() == Redirect.Type.READ)
        throw new IllegalArgumentException(
            "Redirect invalid for writing: " + destination);
    redirects()[1] = destination;
    return this;
}

/*
设置此 ProcessBuilder 的标准错误源。由该对象的 start() 方法启动的子进程将使用此源获取其标准错误。

如果源是 Redirect.PIPE（初始值），则可以使用 Process.getErrorStream() 返回的输入流来读取子进程的标准错误。
如果将源设置为任何其他值，则 Process.getErrorStream() 将返回空输入流。

Redirect 对象使用 Redirect 的工厂方法构造。
*/
public ProcessBuilder redirectError(Redirect destination) {
    if (destination.type() == Redirect.Type.READ)
        throw new IllegalArgumentException(
            "Redirect invalid for writing: " + destination);
    redirects()[2] = destination;
    return this;
}

/*
将此 ProcessBuilder 的标准输入源设置为文件。
这是一种方便的方法。它与调用 redirectInput(Redirect.from(file)) 行为完全相同。
*/
public ProcessBuilder redirectInput(File file) {
    return redirectInput(Redirect.from(file));
}

/*
将此 ProcessBuilder 的标准输出源设置为文件，并且覆写此文件。
这是一种方便的方法。它与调用 redirectOutput(Redirect.to(file)) 行为完全相同。
*/
public ProcessBuilder redirectOutput(File file) {
    return redirectOutput(Redirect.to(file));
}

/*
将此 ProcessBuilder 的标准错误源设置为文件，并且覆写此文件。
这是一种方便的方法。它与调用 redirectError(Redirect.to(file)) 行为完全相同。
*/
public ProcessBuilder redirectError(File file) {
    return redirectError(Redirect.to(file));
}
```

## 4.8 inheritIO
```java
/*
将子进程标准 I/O 源设置为与当前 Java 进程相同。

这是一种方便的方法。它与下面的调用完全相同：
 pb.redirectInput(Redirect.INHERIT)
   .redirectOutput(Redirect.INHERIT)
   .redirectError(Redirect.INHERIT)
 
这提供了与大多数操作系统命令解释器或标准 C 库函数 system() 等效的行为。
*/
public ProcessBuilder inheritIO() {
    Arrays.fill(redirects(), Redirect.INHERIT);
    return this;
}
```

## 4.9 start
```java
/*
使用此 ProcessBuilder 的属性启动新进程。
新进程将在由 directory() 给定的工作目录中调用由 command() 给定的命令和参数，并具有由 environment() 给定的进程环境。

此方法检查命令是否为有效的操作系统命令。哪些命令有效取决于系统，但至少该命令必须是非空字符串的非空列表。

在某些操作系统上启动进程可能需要最少的一组与系统相关的环境变量。因此，子进程可能会继承 ProcessBuilder 的
environment() 中的设置之外的其他环境变量设置。

如果有安全管理器，则以该对象的 command 数组的第一个组件作为参数来调用其 checkExec 方法。这可能会导致引发 SecurityException 。

启动操作系统进程与系统高度相关。可能出错的许多事情包括：
 - 找不到操作系统程序文件。
 - 对该程序文件的访问被拒绝。
 - 工作目录不存在。
在这种情况下，将引发异常。异常的确切性质取决于系统，但是它将始终是 IOException 的子类。

对该流程生成器的后续修改不会影响返回的 Process。
*/
public Process start() throws IOException {
    // 必须首先转换为数组 -- 恶意用户提供的 list 可能会试图规避安全检查。
    String[] cmdarray = command.toArray(new String[command.size()]);
    cmdarray = cmdarray.clone();

    // 命令不能有 null
    for (String arg : cmdarray)
        if (arg == null)
            throw new NullPointerException();
    // 如果命令是空的抛出 IndexOutOfBoundsException
    String prog = cmdarray[0];

    // 对第一个命令参数进行安全检查
    SecurityManager security = System.getSecurityManager();
    if (security != null)
        security.checkExec(prog);

    String dir = directory == null ? null : directory.toString();

    // 确保命令中没有 '\0' 字符
    for (int i = 1; i < cmdarray.length; i++) {
        if (cmdarray[i].indexOf('\u0000') >= 0) {
            throw new IOException("invalid null character in command");
        }
    }

    try {
        // 启动 Process 的具体实现 ProcessImpl
        return ProcessImpl.start(cmdarray,
                                 environment,
                                 dir,
                                 redirects,
                                 redirectErrorStream);
    } catch (IOException | IllegalArgumentException e) {
        String exceptionInfo = ": " + e.getMessage();
        Throwable cause = e;
        if ((e instanceof IOException) && security != null) {
            // 如果配置了安全管理器，并且文件是个读取保护文件，为了安全考虑，我们不能显示错误原因
            try {
                security.checkRead(prog);
            } catch (SecurityException se) {
                // 清空错误原因
                exceptionInfo = "";
                cause = se;
            }
        }
        // 与低等级的 C 代码相比，我们更容易创建高质量的错误消息
        throw new IOException(
            "Cannot run program \"" + prog + "\""
            + (dir == null ? "" : " (in directory \"" + dir + "\")")
            + exceptionInfo,
            cause);
    }
}
```


[process]: Process.md
[env]: ProcessEnvironment.md