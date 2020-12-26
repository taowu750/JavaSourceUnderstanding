`java.lang.ProcessImpl`类的声明如下：
```java
final class ProcessImpl extends Process
```
`ProcessImpl`是`Process`的实现类，此类被`ProcessBuilder.start()`独占使用由于创建新进程。
由于参考的是 Windows 版本的`Java`源码，此类与 Windows 高度相关。

Microsoft `C/C++`代码所使用的命令行解析规则是 Microsoft 特有的。在解释操作系统命令行上给定的参数时使用以下规则：
 - 参数由空格分隔，空格可以是空格或制表符。
 - 对第一个参数（`argv[0]`）进行特殊处理。它代表程序名。因为它必须是有效的路径名，所以允许用双引号（"）括起来。
 双引号包围的部分阻止将空格或制表符解析为参数的结尾。此时双引号作为字符串分隔符。
 - 被双引号包围的字符串被解析为单个参数，其中可能包含空格字符。如果命令行在找到右双引号之前结束，
 那么到目前为止读取的所有字符都将作为最后一个参数输出。
 - 前接反斜杠（\"）的双引号被解析为字面量双引号（"）。
 - 如果偶数个反斜杠后跟双引号，则在`argv`数组中将每对反斜杠（\\）解析为一个反斜杠（\），而双引号被解析为字符串分隔符。
 - 如果奇数个反斜杠后跟双引号，则在`argv`数组中将每对反斜杠（\\）解析为一个反斜杠（\）。双引号被剩余的反斜杠解析为字面量双引号。
 
下表中是一些命令行参数例子：

| Command-line input | argv\[1\] | argv\[2\] | argv\[3\] |
| ------------------ | --------- | --------- | --------- |
| `"abc" d e` | `abc` | `d` | `e` |
| `a\\b d"e f"g h` | `a\\b` | `de fg` | `h` |
| `a\\\"b c d` | `a\"b` | `c` | `d` |
| `a\\\\"b c" d e` | `a\\b c` | `d` | `e` |
| `a"b"" c d` | `ab" c d` |  |  |

有关 Microsoft 命令行参数的解析规则参见 [main function and command-line arguments][main]。

有关进程的更多消息参见 [Process.md][process] 和 [进程和线程.md][pt]。

# 1. 成员字段

## 1.1 VERIFICATION
```java
// Windows 下命令执行的参数验证模式

// Windows 命令行脚本（.cmd 和 .bat 文件）。此模式下不允许命令参数内部有双引号
private static final int VERIFICATION_CMD_BAT = 0;
// 普通命令行
private static final int VERIFICATION_WIN32 = 1;
// 安全模式。如果命令参数被双引号包围，此模式下不允许命令内部有双引号
private static final int VERIFICATION_WIN32_SAFE = 2;
// 遗留模式
private static final int VERIFICATION_LEGACY = 3;
```

## 1.2 ESCAPE_VERIFICATION
```java
// 为每种不同的 VERIFICATION 模式定义了需要转义的字符
private static final char ESCAPE_VERIFICATION[][] = {
    {' ', '\t', '<', '>', '&', '|', '^'},
    {' ', '\t', '<', '>'},
    {' ', '\t', '<', '>'},
    {' ', '\t'}
};
```
有关特殊字符的文档，请参见[Command shell 概述][escape]。

## 1.3 双引号和反斜杠
```java
// 双引号
private static final char DOUBLEQUOTE = '\"';
// 反斜杠
private static final char BACKSLASH = '\\';
```

## 1.4 fdAccess
```java
private static final sun.misc.JavaIOFileDescriptorAccess fdAccess
    = sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess();
```

## 1.5 标准流
```java
// 子进程句柄
private long handle = 0;
private OutputStream stdin_stream;
private InputStream stdout_stream;
private InputStream stderr_stream;
```
在 Windows 程序中，有各种各样的资源（窗口、图标、光标等），系统在创建这些资源时会为他们分配内存，并返回标示这些资源的标示号，
即**句柄**。需要注意，句柄指的是一个核心对象在某一个进程中的唯一索引，但它不是指针。

由于地址空间的限制，句柄所标识的内容对进程是不可见的，只能由操作系统通过进程句柄列表来进行维护。
句柄列表：每个进程都要创建一个句柄列表，这些句柄指向各种系统资源，比如信号量，线程，和文件等，
进程中的所有线程都可以访问这些资源。

## 1.6 STILL_ACTIVE
```java
// 表示进程是否活动的标志常量
private static final int STILL_ACTIVE = getStillActive();
private static native int getStillActive();
```

# 2. 内部类
```java
// 懒加载 Pattern
private static class LazyPattern {
    // Escape-support version:
    //    "(\")((?:\\\\\\1|.)+?)\\1|([^\\s\"]+)";
    // 匹配：不包含空格类字符和双引号的一个或多个字符；或由双引号 " 括起来的没有内部 " 的任意长度字符序列
    private static final Pattern PATTERN = Pattern.compile("[^\\s\"]+|\"[^\"]*\"");
};
```

# 3. 构造器
```java
/*
构造一个 ProcessImpl 对象，仅用在 start 方法中。

@param cmd 一组命令字符串
@param envblock 环境变量字符串，包含所有环境变量。环境变量之间使用 '\0' 分隔，以两个 '\0' 结束
@param path 工作目录路径
@param stdHandlers Windows 句柄（参见 1.5 节标准流）数组。索引 0、1 和 2分别对应于标准输入，标准输出和标准错误。
其中使用 -1 值表示管道。
@param redirectErrorStream 是否合并标准输出和标准错误
*/
private ProcessImpl(String cmd[],
                    final String envblock,
                    final String path,
                    final long[] stdHandles,
                    final boolean redirectErrorStream)
    throws IOException
{
    String cmdstr;
    final SecurityManager security = System.getSecurityManager();
    final String value = GetPropertyAction.privilegedGetProperty("jdk.lang.Process.allowAmbiguousCommands",
                    (security == null ? "true" : "false"));
    // 是否允许有歧义的命令
    final boolean allowAmbiguousCommands = !"false".equalsIgnoreCase(value);

    if (allowAmbiguousCommands && security == null) {
        // 当允许有歧义的命令，并且没有设置安全管理器时，进入 Legacy（遗留）模式

        // 如果可能的话，规范化路径（File 构造函数中会对参数路径进行规范化）
        String executablePath = new File(cmd[0]).getPath();

        // 遗留模式不用管内部 ["] 或未配对的 ["]、重定向/管道字符。
        // 参见 1.2 节 ESCAPE_VERIFICATION

        // 如果 executablePath 存在需要转义的 ' ', '\t' 字符
        if (needsEscaping(VERIFICATION_LEGACY, executablePath) )
            // 为 executablePath 添加双引号
            executablePath = quoteString(executablePath);

        // 将参数组合成命令行字符串
        cmdstr = createCommandLine(
           VERIFICATION_LEGACY,
           executablePath,
           cmd);
    } else {
        String executablePath;
        try {
            // 规范化的可执行程序路径。getExecutablePath 不允许内部有双引号
            executablePath = getExecutablePath(cmd[0]);
        } catch (IllegalArgumentException e) {
            // 抛出了异常，如果命令像这样：Runtime.getRuntime().exec("\"C:\\Program Files\\foo\" bar")
            // 那么我们需要让它能够正常执行

            // 除非传入参数的时候就小心避免，否则不能避免 CMD/BAT 注入。
            // 因为在使用了内部 ["] 和转义序列的 Runtime.getRuntime().exec(String[] cmd [, ...])
            // 调用中，有太多的边缘情况需要处理。

            // 将原来的命令参数用空格连接
            StringBuilder join = new StringBuilder();
            for (String s : cmd)
                join.append(s).append(' ');

            // 再次解析连接后的命令行字符串，使用空格类字符或不配对的双引号作为分隔符。
            // 成对双引号内的字符串的分隔符和转义字符被视为普通字符。
            cmd = getTokensFromCommand(join.toString());
            executablePath = getExecutablePath(cmd[0]);

            // 再次检查新的 executablePath 的安全性
            if (security != null)
                security.checkExec(executablePath);
        }

        // executablePath 代表的是不是一个命令行脚本文件
        boolean isShell = allowAmbiguousCommands ? isShellFile(executablePath)
                : !isExe(executablePath);
        cmdstr = createCommandLine(
                // 针对不同的情况使用不同的 VERIFICATION 模式
                isShell ? VERIFICATION_CMD_BAT
                        : (allowAmbiguousCommands ? VERIFICATION_WIN32 : VERIFICATION_WIN32_SAFE),
                quoteString(executablePath),
                cmd);
    }

    // 创建子进程并返回子进程的句柄。
    // 千万要注意！
    // stdHandles 输入 create 时，里面的 -1 值表示管道方式；而当 create 运行结束，它会修改 stdHandles，
    // 为管道分配具体的句柄值。而对于文件方式，将句柄值设置为 -1。所以 stdHandles 输入输出的 -1 值表示不同的
    // 概念（太容易混淆，要不是调试了一下真的就蒙圈了）。
    handle = create(cmdstr, envblock, path, stdHandles, redirectErrorStream);

    java.security.AccessController.doPrivileged(
        new java.security.PrivilegedAction<Void>() {
            public Void run() {
                // -1 此时表示不能和子进程进行数据传输
                if (stdHandles[0] == -1L)
                    // 使用空的输入流
                    stdin_stream = ProcessBuilder.NullOutputStream.INSTANCE;
                else {
                    FileDescriptor stdin_fd = new FileDescriptor();
                    fdAccess.setHandle(stdin_fd, stdHandles[0]);
                    stdin_stream = new BufferedOutputStream(new FileOutputStream(stdin_fd));
                }

                if (stdHandles[1] == -1L)
                    // 使用空的输出流
                    stdout_stream = ProcessBuilder.NullInputStream.INSTANCE;
                else {
                    FileDescriptor stdout_fd = new FileDescriptor();
                    fdAccess.setHandle(stdout_fd, stdHandles[1]);
                    stdout_stream = new BufferedInputStream(new FileInputStream(stdout_fd));
                }

                if (stdHandles[2] == -1L)
                    // 使用空的错误流
                    stderr_stream = ProcessBuilder.NullInputStream.INSTANCE;
                else {
                    FileDescriptor stderr_fd = new FileDescriptor();
                    fdAccess.setHandle(stderr_fd, stdHandles[2]);
                    stderr_stream = new FileInputStream(stderr_fd);
                }

                return null; 
            }
        });
}
```

# 4. 方法

## 4.1 quote
```java
// 将参数用双引号包围
private String quoteString(String arg) {
    StringBuilder argbuf = new StringBuilder(arg.length() + 2);
    return argbuf.append('"').append(arg).append('"').toString();
}

// 如果 str 被双引号包围，去掉双引号
private static String unQuote(String str) {
    int len = str.length();
    return (len >= 2 && str.charAt(0) == DOUBLEQUOTE && str.charAt(len - 1) == DOUBLEQUOTE)
            ? str.substring(1, len - 1)  // 如果被双引号包围，去掉双引号并返回
            : str;  // 其他情况（双引号未配对、没有被双引号包围）直接返回
}
```

## 4.2 countLeadingBackslash
```java
// 计算字符序列 input 从 start - 1 开始往前有几个连续的反斜杠。
// 对于 VERIFICATION_CMD_BAT 模式始终返回 0。
// verificationType 参见 1.1 节 VERIFICATION
private static int countLeadingBackslash(int verificationType,
                                         CharSequence input, int start) {
    if (verificationType == VERIFICATION_CMD_BAT)
        return 0;
    int j;
    for (j = start - 1; j >= 0 && input.charAt(j) == BACKSLASH; j--) {
    }
    return (start - 1) - j;
}
```

## 4.3 needsEscaping
```java
// 判断 arg 是否包含转义字符。参数 verificationType 在 1.1 节 VERIFICATION 定义
// 
private static boolean needsEscaping(int verificationType, String arg) {
    // 如果需要使用内部 ["]，请使用显式 [cmd.exe] 调用。
    // 示例："cmd.exe", "/C", "Extended_MS_Syntax"

    // 对于 [.exe] 或 [.com] 文件，参数内部出现未配对或内部 ["] 没有关系
    String unquotedArg = unQuote(arg);  // 如果 arg 被双引号包围，去掉双引号
    // arg 是否被双引号包围
    boolean argIsQuoted = !arg.equals(unquotedArg);
    // arg 内部是否有双引号
    boolean embeddedQuote = unquotedArg.indexOf(DOUBLEQUOTE) >= 0;

    switch (verificationType) {
        case VERIFICATION_CMD_BAT:
            // Windows 命令行脚本模式不允许命令内部有双引号
            if (embeddedQuote) {
                throw new IllegalArgumentException("Argument has embedded quote, " +
                        "use the explicit CMD.EXE call.");
            }
            break;
        case VERIFICATION_WIN32_SAFE:
            // 如果命令参数被双引号包围，则 Windows 命令行安全模式不允许命令内部有双引号
            if (argIsQuoted && embeddedQuote)  {
                throw new IllegalArgumentException("Malformed argument has embedded quote: "
                        + unquotedArg);
            }
            break;
        default:
            break;
    }

    // 如果参数没有被双引号包围
    if (!argIsQuoted) {
        // 针对不同的 VERIFICATION 模式，获取对应的转义字符 
        char testEscape[] = ESCAPE_VERIFICATION[verificationType];
        // 判断命令中是否有转义字符
        for (int i = 0; i < testEscape.length; ++i) {
            if (arg.indexOf(testEscape[i]) >= 0) {
                return true;
            }
        }
    }
    return false;
}
```

## 4.4 文件类型判断
```java
// 可执行文件是指扩展名为 EXE 或扩展名的程序。Windows CreateProcess 函数将在其中查找 .exe。
// 根据名称比较不区分大小写。
private boolean isExe(String executablePath) {
    File file = new File(executablePath);
    String upName = file.getName().toUpperCase(Locale.ROOT);
    //  如果执行路径以 .EXE 结尾或没有扩展名
    return (upName.endsWith(".EXE") || upName.indexOf('.') < 0);
}

// 判断路径是否表示一个命令行脚本
private boolean isShellFile(String executablePath) {
    String upPath = executablePath.toUpperCase();
    // Windows 命令行脚本以 .CMD 和 .BAT 结尾
    return (upPath.endsWith(".CMD") || upPath.endsWith(".BAT"));
}
```

## 4.5 getExecutablePath
```java
// 获取可执行程序路径，不允许路径内部有双引号
private static String getExecutablePath(String path) throws IOException {
    // 如果 path 被双引号包围，去掉双引号
    String name = unQuote(path);
    // 执行程序路径内部不能包含双引号
    if (name.indexOf(DOUBLEQUOTE) >= 0) {
        throw new IllegalArgumentException("Executable name has embedded quote, " +
                "split the arguments: " + name);
    }
    // Win32 CreateProcess 程序要求路径被规范化。File 构造函数中会对参数路径进行规范化
    File fileToRun = new File(name);
    
    /*
    以下注释来自 CreateProcess 函数注释：
    "如果文件名不包含扩展名，则添加 .exe。如果文件扩展名是 .com，此参数必须包含 .com 扩展名。
    如果文件名以没有扩展名的 . 结尾，或者文件名包含路径，则不添加 .exe。"

    实际上，任何不存在的路径会被 CreateProcess 函数添加 .exe 扩展名，除了路径以 . 结尾的情况。
     */

    return fileToRun.getPath();
}
```

## 4.6 getTokensFromCommand
```java
/*
j将命令行字符串解析为命令参数。

命令行字符串被分解为 token。token 分隔符是空格类字符或双引号字符。成对双引号内的字符串的 token 分隔符
和转义字符被视为普通字符。
*/
private static String[] getTokensFromCommand(String command) {
    ArrayList<String> matchList = new ArrayList<>(8);
    // PATTERN 匹配：不包含空格类字符和双引号的字符序列；或由双引号 " 括起来的没有内部 " 的任意长度字符序列
    Matcher regexMatcher = LazyPattern.PATTERN.matcher(command);
    while (regexMatcher.find())
        matchList.add(regexMatcher.group());
    return matchList.toArray(new String[matchList.size()]);
}
```

## 4.7 createCommandLine
```java
/*
创建命令行字符串。有关双引号和反斜杠的解析规则参见 Markdown 开头声明处。

@param verificationType 参见 1.1 节 VERIFICATION
@param executablePath 可执行程序路径
@param cmd 命令参数
*/
private static String createCommandLine(int verificationType,
                                        final String executablePath,
                                        final String cmd[])
{
    StringBuilder cmdbuf = new StringBuilder(80);

    cmdbuf.append(executablePath);

    // cmd[] 第一个参数是执行程序。已经有了 executablePath，不需要再次添加
    for (int i = 1; i < cmd.length; ++i) {
        cmdbuf.append(' ');
        String s = cmd[i];
        // 如果命令参数包含转义字符
        if (needsEscaping(verificationType, s)) {
            // 将命令参数用双引号包围。需要注意，如果命令行参数已经被双引号包围，这里还是会加上双引号

            // 添加左双引号
            cmdbuf.append('"');

            if (verificationType == VERIFICATION_WIN32_SAFE) {
                // 在 VERIFICATION_WIN32_SAFE 模式下，如果命令参数内部有双引号，则添加反斜杠进行转义
                int length = s.length();
                for (int j = 0; j < length; j++) {
                    char c = s.charAt(j);
                    // 如果 j 处字符是双引号
                    if (c == DOUBLEQUOTE) {
                        // j 之前有几个连续的反斜杠
                        int count = countLeadingBackslash(verificationType, s, j);
                        // 添加反斜杠，保证反斜杠数目是偶数
                        while (count-- > 0) {
                            cmdbuf.append(BACKSLASH);
                        }
                        // 最后添加一个反斜杠
                        cmdbuf.append(BACKSLASH);
                    }
                    cmdbuf.append(c);
                }
            } else {
                cmdbuf.append(s);
            }

            // 计算命令参数尾部有几个连续的反斜杠
            int count = countLeadingBackslash(verificationType, s, s.length());
            // 添加反斜杠，保证反斜杠数目是偶数
            while (count-- > 0) {
                cmdbuf.append(BACKSLASH);
            }
            // 添加右双引号
            cmdbuf.append('"');
        } else {
            // 命令参数不包含转义字符，可以直接添加
            cmdbuf.append(s);
        }
    }
    return cmdbuf.toString();
}
```

## 4.8 create
```java
/*
使用 Win32 函数 CreateProcess 创建一个进程。由于 MS kb315939 问题，该方法已同步。

@param cmdstr Windows 命令行
@param envblock 环境变量字符串，包含所有环境变量。环境变量之间使用 '\0' 分隔，以两个 '\0' 结束
@param dir 进程的工作目录；如果从父进程继承当前目录，则为 null
@param stdHandles – Windows 句柄数组。索引 0、1 和 2分别对应于标准输入，标准输出和标准错误。
stdHandles 输入 create 时，里面的 -1 值表示管道方式；而当 create 运行结束，它会修改 stdHandles，
为管道分配具体的句柄值。而对于文件方式，将句柄值设置为 -1。所以 stdHandles 输入输出的 -1 值表示不同的
概念（太容易混淆，要不是调试了一下真的就蒙圈了）。
@param redirectErrorStream 是否合并输出流和错误流

@return 本地子进程的句柄 
*/
private static synchronized native long create(String cmdstr,
                                  String envblock,
                                  String dir,
                                  long[] stdHandles,
                                  boolean redirectErrorStream)
    throws IOException;
```

## 4.9 newFileOutputStream
```java
// 打开文件进行写入。如果 append 为 true 则使用原子附加方式打开文件，并使用结果句柄构造 FileOutputStream。
// 之所以这样做是因为直接创建附加到文件的 FileOutputStream 不会保证子进程的写入是原子的方式打开文件。
private static FileOutputStream newFileOutputStream(File f, boolean append) throws IOException
{
    // 如果是追加写方式
    if (append) {
        String path = f.getPath();
        // 检查安全性
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkWrite(path);
        // 以原子方式打开文件并返回句柄
        long handle = openForAtomicAppend(path);
        final FileDescriptor fd = new FileDescriptor();
        fdAccess.setHandle(fd, handle);
        return AccessController.doPrivileged(
            new PrivilegedAction<FileOutputStream>() {
                public FileOutputStream run() {
                    return new FileOutputStream(fd);
                }
            }
        );
    } else {
        // 覆盖写直接使用 FileOutputStream
        return new FileOutputStream(f);
    }
}

// 打开一个文件进行原子附加。如果文件不存在，则创建该文件。返回文件句柄。
private static native long openForAtomicAppend(String path) throws IOException;
```

## 4.10 start
```java
/*
创建一个子进程，此进程的实现依赖于操作系统。此方法仅被 ProcessBuilder.start 方法调用

@param cmdarray 命令行参数
@param environment 环境变量 Map
@param dir 工作目录
@param redirects 表示子进程输入的源或子进程输出的目的地。索引 0、1 和 2分别对应于标准输入，标准输出和标准错误。
参见 ProcessBuilder.md 第 2.2 节 Redirect
@param redirectErrorStream 是否合并输出流和错误流
*/
static Process start(String cmdarray[],
                     java.util.Map<String,String> environment,
                     String dir,
                     ProcessBuilder.Redirect[] redirects,
                     boolean redirectErrorStream)
    throws IOException
{
    // 将 environment 变成一个字符串。环境变量之间使用 '\0' 分隔，以两个 '\0' 结束
    String envblock = ProcessEnvironment.toEnvironmentBlock(environment);

    // 标准输入，标准输出和标准错误
    FileInputStream  f0 = null;
    FileOutputStream f1 = null;
    FileOutputStream f2 = null;

    try {
        // 对应于标准输入，标准输出和标准错误的句柄
        long[] stdHandles;
        if (redirects == null) {
            // 如果 redirects 为 null，则输入输出全部使用管道句柄，管道句柄用 -1 表示
            stdHandles = new long[] { -1L, -1L, -1L };
        } else {
            stdHandles = new long[3];

            if (redirects[0] == Redirect.PIPE)
                // 使用管道句柄
                stdHandles[0] = -1L;
            else if (redirects[0] == Redirect.INHERIT)
                // 继承父进程的标准输入，此时父进程可以向子进程写入
                stdHandles[0] = fdAccess.getHandle(FileDescriptor.in);
            else {
                // 使用文件句柄
                f0 = new FileInputStream(redirects[0].file());
                stdHandles[0] = fdAccess.getHandle(f0.getFD());
            }

            if (redirects[1] == Redirect.PIPE)
                stdHandles[1] = -1L;
            else if (redirects[1] == Redirect.INHERIT)
                stdHandles[1] = fdAccess.getHandle(FileDescriptor.out);
            else {
                // 构造输出流
                f1 = newFileOutputStream(redirects[1].file(),
                                         redirects[1].append());
                stdHandles[1] = fdAccess.getHandle(f1.getFD());
            }

            if (redirects[2] == Redirect.PIPE)
                stdHandles[2] = -1L;
            else if (redirects[2] == Redirect.INHERIT)
                stdHandles[2] = fdAccess.getHandle(FileDescriptor.err);
            else {
                f2 = newFileOutputStream(redirects[2].file(),
                                         redirects[2].append());
                stdHandles[2] = fdAccess.getHandle(f2.getFD());
            }
        }

        // 创建 ProcessImpl 对象
        return new ProcessImpl(cmdarray, envblock, dir,
                               stdHandles, redirectErrorStream);
    } finally {
        // 理论上，close() 可能抛出 IO 异常，但在这里不太可能发生
        try { if (f0 != null) f0.close(); }
        finally {
            try { if (f1 != null) f1.close(); }
            finally { if (f2 != null) f2.close(); }
        }
    }
}
```

## 4.11 finalize
```java
// 回收时调用 closeHandle 关闭子进程，确保资源即时被清理
protected void finalize() {
    closeHandle(handle);
}

private static native boolean closeHandle(long handle);
```

## 4.12 获取流
```java
public InputStream getInputStream() {
    return stdout_stream;
}

public OutputStream getOutputStream() {
    return stdin_stream;
}

public InputStream getErrorStream() {
    return stderr_stream;
}
```

## 4.13 exitValue
```java
/*
返回子进程的退出值。按照惯例，值 0 表示正常终止。

如果子进程未退出，抛出 IllegalThreadStateException。
*/
public int exitValue() {
    // 获取子进程退出码
    int exitCode = getExitCodeProcess(handle);
    // 子进程仍然存活则抛出异常
    if (exitCode == STILL_ACTIVE)
        throw new IllegalThreadStateException("process has not exited");
    return exitCode;
}

private static native int getExitCodeProcess(long handle);
```

## 4.14 waitFor
```java
/*
如有必要，使当前线程等待，直到此 Process 对象表示的进程终止。如果子进程已经终止，则此方法立即返回。
如果子进程尚未终止，则调用线程将被阻塞，直到子进程退出。

@return 此 Process 对象表示的子进程的退出值。按照惯例，值 0 表示正常终止。
*/
public int waitFor() throws InterruptedException {
    waitForInterruptibly(handle);
    if (Thread.interrupted())
        throw new InterruptedException();
    return exitValue();
}

private static native void waitForInterruptibly(long handle);

/*
如有必要，使当前线程等待，直到此 Process 对象表示的进程终止或经过指定的等待时间为止。
如果子进程已经终止，则此方法立即返回 true 值。如果进程尚未终止，并且超时值小于或等于零，则此方法立即返回 false 值。
*/
@Override
public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    // 转成纳秒
    long remainingNanos = unit.toNanos(timeout);
    // 子进程已终止直接返回 true
    if (getExitCodeProcess(handle) != STILL_ACTIVE) return true;
    if (timeout <= 0) return false;

    // 等待终止时间
    long deadline = System.nanoTime() + remainingNanos;
    do {
        // 舍入到下一毫秒
        long msTimeout = TimeUnit.NANOSECONDS.toMillis(remainingNanos + 999_999L);
        if (msTimeout < 0) {
            // 如果输入时间小于 0，则无限期等待，此时行为等同于 waitFor
            msTimeout = Integer.MAX_VALUE;
        }
        waitForTimeoutInterruptibly(handle, msTimeout);
        // 响应线程中断事件
        if (Thread.interrupted())
            throw new InterruptedException();
        if (getExitCodeProcess(handle) != STILL_ACTIVE) {
            return true;
        }
        remainingNanos = deadline - System.nanoTime();
    } while (remainingNanos > 0);

    return (getExitCodeProcess(handle) != STILL_ACTIVE);
}

private static native void waitForTimeoutInterruptibly(long handle, long timeoutMillis);
```

## 4.15 isAlive
```java
// 测试此 Process 代表的子进程是否处于活动状态。
@Override
public boolean isAlive() {
    return isProcessAlive(handle);
}

private static native boolean isProcessAlive(long handle);
```

## 4.16 destroy
```java
// 杀死子进程。但是否强制终止取决于实现。
public void destroy() { terminateProcess(handle); }

/*
强制终止子进程。此方法的默认实现调用 destroy，因此可能不会强行终止该进程。

注意：子进程可能不会立即终止。即 isAlive() 可能在调用 destroyForcibly() 之后的短时间内返回 true。
如果需要，可以将此方法链接到 waitFor()。
*/
@Override
public Process destroyForcibly() {
    destroy();
    return this;
}

private static native void terminateProcess(long handle);
```


[process]: Process.md
[pt]: 进程和线程.md
[main]: https://docs.microsoft.com/en-us/cpp/cpp/main-function-command-line-args?redirectedfrom=MSDN&view=msvc-160
[escape]: https://docs.microsoft.com/en-us/previous-versions/windows/it-pro/windows-xp/bb490954(v=technet.10)