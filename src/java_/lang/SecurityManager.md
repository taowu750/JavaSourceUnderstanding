`java.lang.SecurityManager`类的声明如下：
```java
public class SecurityManager
```
安全管理器是允许应用程序实施安全策略的类。它允许应用程序在执行可能不安全或敏感的操作之前确定该操作是什么
以及是否在允许该操作的安全上下文中尝试执行该操作。应用程序可以允许或禁止该操作。

`SecurityManager`包含许多方法，其名称以单词`check`开头。在执行某些可能敏感的操作之前，这些方法会被`Java`库中的各种方法调用。
这种`check`方法的调用通常如下所示：
```java
SecurityManager security = System.getSecurityManager();
if (security != null) {
    security.checkXXX(argument, ...);
}
```

安全管理器会通过引发异常来阻止操作完成。如果允许该操作，则安全管理器仅返回，但是如果不允许该操作，则抛出`SecurityException`。
该约定的唯一例外是`checkTopLevelWindow`，它返回一个`boolean`值。

当前的安全管理器由`System`类中的`setSecurityManager`方法设置。当前的安全管理器通过`getSecurityManager`方法获得。
特殊方法`checkPermission(Permission)`确定是应授予还是拒绝请求指定权限的访问请求。默认实现调用
```java
AccessController.checkPermission(perm);
```
如果允许请求的访问，则`checkPermission`安静地返回。如果被拒绝，则抛出`SecurityException`。

从`Java2`开始，`SecurityManager`中每个其他`check`方法的默认实现是调用`SecurityManager.checkPermission`方法，
以确定调用线程是否有权执行所请求的操作。请注意，只有一个权限参数的`checkPermission`方法始终在当前正在执行的线程的上下文中执行安全检查。 
有时，需要在其他上下文中进行（例如，在工作线程中）安全检查。针对这种情况，
提供了`getSecurityContext`方法和包括上下文参数的`checkPermission`方法。`getSecurityContext`方法返回当前调用上下文的“快照”。
（默认实现返回一个`AccessControlContext`对象。）下面是一个示例调用：
```java
Object context = null;
SecurityManager sm = System.getSecurityManager();
if (sm != null)
    context = sm.getSecurityContext();
```
带有上下文对象的`checkPermission`方法根据该上下文而不是当前执行线程的上下文做出权限判断。因此，不同上下文中的代码可以调用该方法，
并传递权限和先前保存的上下文对象。以下示例接着上一个示例执行：
```java
if (sm != null)
    sm.checkPermission(permission, context);
```

权限分为以下几类：文件，套接字，网络，安全性，运行时，属性，`AWT`，反射和序列化。
管理这些各种权限类别的类是`java.io.FilePermission`，`java.net.SocketPermission`，`java.net.NetPermission`，
`java.security.SecurityPermission`，`java.lang.RuntimePermission`，`java.util.PropertyPermission`，
`java.awt.AWTPermission`，`java.lang.reflect.ReflectPermission`和`java.io.SerializablePermission`。
除了前两个（`FilePermission`和`SocketPermission`）之外，其他都是`java.security.BasicPermission`的子类，
而`java.security.BasicPermission`本身是顶级权限`java.security.Permission`的抽象子类。
`BasicPermission`定义了所有权限所需的功能，这些功能包含一个遵循层次属性命名约定的名称
（例如，`exitVM`，`setFactory`，`queuePrintJob`等）。在名称的末尾可能会出现一个星号，后面跟着一个`.`，或者单独出现一个星号，
表示通配符。例如：`a.*`或`*`有效，`*a`或`a*b`无效。

`FilePermission`和`SocketPermission`是权限的顶级类`java.security.Permission`的子类。
此类的名称语法要比`BasicPermission`子类使用的名称语法更复杂。例如，对于`java.io.FilePermission`对象，
权限名称是文件（或目录）的路径名。

一些权限类具有“操作”列表，该列表告知对象所允许的操作。例如，对于`java.io.FilePermission`对象，操作列表（例如“读，写”）
指定为指定文件（或指定目录中的文件）授予哪些操作。其他权限类用于"命名"权限 - 包含名称但不包含操作列表的权限类；你要么有命名权限，
要么没有。

注意：还有一个`java.security.AllPermission`权限暗含所有权限。它的存在是为了简化系统管理员的工作，
他们可能需要执行需要所有（或众多）权限的多个任务。

有关权限的更多信息，请参见[JDK Permissions][permissions]。本文档包括一个表，
该表列出了各种`SecurityManager.check`方法以及每个方法的默认实现所需的权限。它还包含一个表格，
列出了所有需要权限的 1.2 版方法，并且针对每个方法告诉它所需的权限。

有关`JDK`中对`SecurityManager`所做的更改以及有关移植 1.1 样式安全管理器的建议，请参阅[Java Security 文档][security]。

有关`SecurityManager`的配置和应用场景，参见[安全管理器.md][sm]。

# 1. 属性

## 1.1 inCheck
```java
/*
如果正在进行安全检查，则此字段为 true 否则为 false。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected boolean inCheck;
```

## 1.2 initialized
```java
// 是否已经初始化。它对于抵御 finalize 攻击很有效。
private boolean initialized = false;
```
`finalize`攻击参见 [finalize攻击.md][finalize-attack]。

## 1.3 rootGroup
```java
// 用于 checkAccess 方法的根 ThreadGroup。
private static ThreadGroup rootGroup = getRootGroup();

private static ThreadGroup getRootGroup() {
    ThreadGroup root =  Thread.currentThread().getThreadGroup();
    while (root.getParent() != null) {
        root = root.getParent();
    }
    return root;
}
```

## 1.4 package
```java
/*
packageAccessValid/packageDefinitionValid 类变量初始为 false，
它们用于判断缓存（packageAccess/packageDefinitionLock）是否有效。

如果使用 java.security.Security.setProperty() 方法更改基础属性，
Security 类在这个方法中将会使用反射来更改 packageAccessValid 或 packageDefinitionValid 变量为 false，从而使缓存无效。

使用 packageAccessLock/packageDefinitionLock 对象进行同步。它们只用于此类。
请注意，由于属性更改导致的缓存无效将不会使用这些锁，因此当一个线程更新属性与其他线程更新缓存之间可能会出现延迟。
*/

private static boolean packageAccessValid = false;
private static String[] packageAccess;
private static final Object packageAccessLock = new Object();

private static boolean packageDefinitionValid = false;
private static String[] packageDefinition;
private static final Object packageDefinitionLock = new Object();
```

# 2. 构造器
```java
public SecurityManager() {
    // 由于在同步块中，因此 initialized 无需是 volatile 的
    synchronized(SecurityManager.class) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // 询问当前的安全管理器，我们能否创建新的安全管理器。
            sm.checkPermission(new RuntimePermission
                               ("createSecurityManager"));
        }
        // 将标志变量设为 true
        initialized = true;
    }
}
```

# 3. 方法

## 3.1 getSecurityContext
```java
// 创建一个封装当前执行环境的对象。例如，三参数 checkConnect 方法和两参数 checkRead 方法使用此方法的结果。
// 需要这些方法，因为可以调用受信任的方法来读取文件或代表另一种方法打开套接字。
// 受信任的方法需要确定是否允许其他（可能不受信任的）方法执行操作。
// 此方法的默认实现是返回 AccessControlContext 对象。
public Object getSecurityContext() {
    return AccessController.getContext();
}
```

## 3.2 checkPermission
```java
// 如果基于当前有效的安全策略不允许给定权限 perm 指定的请求访问，则引发 SecurityException。
public void checkPermission(Permission perm) {
    // 调用 AccessController.checkPermission
    java.security.AccessController.checkPermission(perm);
}

/*
如果给定安全上下文被指定的权限拒绝访问，则引发 SecurityException。
该上下文必须是使用 getSecurityContext 调用返回的安全上下文，getSecurityContext 访问控制决策基于为该安全上下文配置的安全策略。

如果 context 是 AccessControlContext 的实例，则将使用指定的权限调用 AccessControlContext.checkPermission 方法。
如果 context 不是 AccessControlContext 的实例，则抛出 SecurityException。
*/
public void checkPermission(Permission perm, Object context) {
    if (context instanceof AccessControlContext) {
        ((AccessControlContext)context).checkPermission(perm);
    } else {
        throw new SecurityException();
    }
}
```

## 3.3 hasAllPermission
```java
// 如果当前上下文被授予了全部权限，返回 true
private boolean hasAllPermission() {
    try {
        checkPermission(SecurityConstants.ALL_PERMISSION);
        return true;
    } catch (SecurityException se) {
        return false;
    }
}
```

## 3.4 getInCheck
```java
/*
测试是否正在进行安全检查。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
public boolean getInCheck() {
    return inCheck;
}
```

## 3.5 getClassContext
```java
// 以类数组的形式返回当前执行堆栈。数组的长度是执行堆栈上方法的数量。索引 0 处的元素是当前正在执行的方法的类，
// 索引 1 处的元素是该方法的调用者的类，依此类推。
protected native Class[] getClassContext();
```

## 3.6 classLoader
```java
/*
返回最近执行方法的类的类加载器，这个类加载器需要是非系统类加载器。非系统类加载器被定义为不等于系统类加载器
（ClassLoader.getSystemClassLoader 的返回值）或其祖先之一的类加载器。

在以下三种情况，此方法将返回 null ：
 - 执行堆栈上的所有方法均来自使用系统类加载器或其祖先之一定义的类。
 - 执行堆栈上直到第一个“特权”调用者的所有方法（请参阅 AccessController.doPrivileged）都来自使用系统类加载器或其祖先定义的类。
 - hasAllPermission() 返回 true

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected ClassLoader currentClassLoader() {
    ClassLoader cl = currentClassLoader0();
    if ((cl != null) && hasAllPermission())
        cl = null;
    return cl;
}

private native ClassLoader currentClassLoader0();

/*
从使用非系统类加载器定义的类中返回最近执行的方法的堆栈深度。
如果 currentClassLoader 返回 null，此方法返回 -1。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected int classLoaderDepth() {
    int depth = classLoaderDepth0();
    if (depth != -1) {
        if (hasAllPermission())
            depth = -1;
        else
            depth--; // 确保我们不会包含自身
    }
    return depth;
}

private native int classLoaderDepth0();

/*
测试使用类加载器定义的类中的方法是否在执行堆栈上。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected boolean inClassLoader() {
    return currentClassLoader() != null;
}
```

## 3.7 class
```java
/*
从使用非系统类加载器定义的类中返回最近执行的方法的类。
如果 currentClassLoader 返回 null，此方法返回 null。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected Class<?> currentLoadedClass() {
    Class<?> c = currentLoadedClass0();
    if ((c != null) && hasAllPermission())
        c = null;
    return c;
}

private native Class<?> currentLoadedClass0();

/*
返回指定类的堆栈深度。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected native int classDepth(String name);

/*
测试具有指定名称的类中的方法是否在执行堆栈上。

@deprecated 不建议使用这种类型的安全检查。建议改用 checkPermission 调用。
*/
@Deprecated
protected boolean inClass(String name) {
    return classDepth(name) >= 0;
}
```

## 3.8 getThreadGroup
```java
// 返回被调用时实例化正在创建的任何新线程的线程组。默认情况下，它返回当前线程的线程组。
// 特定的安全管理器应重写此方法以返回适当的线程组。
public ThreadGroup getThreadGroup() {
    return Thread.currentThread().getThreadGroup();
}
```

## 3.9 checkCreateClassLoader
```java
/*
如果不允许调用线程创建新的类加载器，则抛出 SecurityException。

此方法使用 RuntimePermission("createClassLoader") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点调用 super.checkCreateClassLoader。
*/
public void checkCreateClassLoader() {
    checkPermission(SecurityConstants.CREATE_CLASSLOADER_PERMISSION);
}
```

## 3.10 checkAccess
```java
/*
如果不允许调用线程修改线程参数，则引发 SecurityException。

当前的安全管理器通过 Thread 类的 stop，suspend，resume，setPriority，setName 和 setDaemon 方法调用此方法。

如果 Thread 参数是系统线程（属于具有 null 父级的线程组），则此方法将调用具有 RuntimePermission("modifyThread")
权限的 checkPermission。
如果 Thread 参数不是系统线程，则此方法仅静默返回。

如果需要更严格策略的应用程序应重写此方法。如果重写此方法，则重写该方法的方法还应该检查调用线程是否具有
RuntimePermission("modifyThread") 权限，如果有，则静默返回。这是为了确保授予该权限的代码（例如 JDK 本身）被允许操纵任何线程。
如果此方法被覆盖，则覆盖方法中的第一条语句应该调用 super.checkAccess，或者应将等效的安全检查放置在覆盖方法中。
*/
public void checkAccess(Thread t) {
    if (t == null) {
        throw new NullPointerException("thread can't be null");
    }
    if (t.getThreadGroup() == rootGroup) {
        checkPermission(SecurityConstants.MODIFY_THREAD_PERMISSION);
    } else {
        // just return
    }
}
```

## 3.11 checkExit
```java
/*
如果不允许调用线程使 Java 虚拟机以指定的状态代码停止，则引发 SecurityException 。

Runtime 类的 exit 方法使用当前的安全管理器（System.getSecurityManager()）调用此方法。状态 0 表示成功退出；其他值表示各种错误。
此方法使用 RuntimePermission("exitVM."+status) 权限调用 checkPermission 。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkExit。
*/
public void checkExit(int status) {
    checkPermission(new RuntimePermission("exitVM."+status));
}
```

## 3.12 checkExec
```java
/*
如果不允许调用线程创建子进程，则引发 SecurityException。

Runtime 类的 exec 方法使用当前的安全管理器调用此方法。

如果 cmd 是绝对路径，此方法使用 FilePermission(cmd,"execute") 权限调用 checkPermission。
否则它调用 checkPermission 用 FilePermission("<<ALL FILES>>","execute")。

如果重写此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkExec。
*/
public void checkExec(String cmd) {
    File f = new File(cmd);
    if (f.isAbsolute()) {
        checkPermission(new FilePermission(cmd, SecurityConstants.FILE_EXECUTE_ACTION));
    } else {
        checkPermission(new FilePermission("<<ALL FILES>>",
            SecurityConstants.FILE_EXECUTE_ACTION));
    }
}
```

## 3.13 checkLink
```java
/*
如果不允许线程调用由字符串参数文件指定的库代码动态链接，则引发 SecurityException。参数是简单的库名或完整的文件名。

Runtime 类的 load 和 loadLibrary 方法使用当前的安全管理器调用此方法。
此方法使用 RuntimePermission("loadLibrary."+lib) 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkLink。
*/
public void checkLink(String lib) {
    if (lib == null) {
        throw new NullPointerException("library can't be null");
    }
    checkPermission(new RuntimePermission("loadLibrary."+lib));
}
```

## 3.14 checkRead
```java
/*
如果不允许调用线程从指定的文件描述符中读取，则引发 SecurityException 。

此方法使用 RuntimePermission("readFileDescriptor") 权限调用 checkPermission 。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkRead。
*/
public void checkRead(FileDescriptor fd) {
    if (fd == null) {
        throw new NullPointerException("file descriptor can't be null");
    }
    checkPermission(new RuntimePermission("readFileDescriptor"));
}

/*
如果不允许调用线程读取字符串参数指定的文件，则引发 SecurityException。

此方法使用 FilePermission(file,"read") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkRead。
*/
public void checkRead(String file) {
    checkPermission(new FilePermission(file, SecurityConstants.FILE_READ_ACTION));
}

/*
如果不允许指定的安全上下文读取字符串参数指定的文件，则引发 SecurityException 。
该上下文必须是先前对 getSecurityContext 调用返回的安全上下文。

如果 context 是 AccessControlContext 的实例，则将使用 FilePermission(file,"read") 权限
调用 AccessControlContext.checkPermission 方法。
如果 context 不是 AccessControlContext 的实例，则抛出 SecurityException。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkRead。
*/
public void checkRead(String file, Object context) {
    checkPermission(new FilePermission(file, SecurityConstants.FILE_READ_ACTION),
        context);
}
```

## 3.15 checkWrite
```java
/*
如果不允许调用线程写入指定的文件描述符，则引发 SecurityException。

此方法使用 RuntimePermission("writeFileDescriptor") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkWrite。
*/
public void checkWrite(FileDescriptor fd) {
    if (fd == null) {
        throw new NullPointerException("file descriptor can't be null");
    }
    checkPermission(new RuntimePermission("writeFileDescriptor"));
}

/*
如果不允许调用线程写入字符串参数指定的文件，则引发 SecurityException。

此方法使用 FilePermission(file,"write") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkWrite。
*/
public void checkWrite(String file) {
    checkPermission(new FilePermission(file, SecurityConstants.FILE_WRITE_ACTION));
}
```

## 3.16 checkDelete
```java
/*
如果不允许调用线程删除指定的文件，则引发 SecurityException。

File 类的 delete 方法通过当前的安全管理器调用此方法。
此方法使用 FilePermission(file,"delete") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkDelete。
*/
public void checkDelete(String file) {
    checkPermission(new FilePermission(file, SecurityConstants.FILE_DELETE_ACTION));
}
```

## 3.17 checkConnect
```java
/*
如果不允许调用线程打开与指定主机和端口号的套接字连接，则引发 SecurityException。

端口号为 -1 表示调用方法正在尝试确定指定主机名的 IP 地址。

如果端口不等于 -1，则此方法使用 SocketPermission(host+":"+port,"connect") 权限调用 checkPermission。
如果端口等于 -1，则它将使用 SocketPermission(host,"resolve") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkConnect。
*/
public void checkConnect(String host, int port) {
    if (host == null) {
        throw new NullPointerException("host can't be null");
    }
    if (!host.startsWith("[") && host.indexOf(':') != -1) {
        host = "[" + host + "]";
    }
    if (port == -1) {
        checkPermission(new SocketPermission(host, SecurityConstants.SOCKET_RESOLVE_ACTION));
    } else {
        checkPermission(new SocketPermission(host+":"+port, SecurityConstants.SOCKET_CONNECT_ACTION));
    }
}

/*
如果不允许指定的安全上下文打开与指定主机和端口号的套接字连接，则引发 SecurityException。
如果 context 不是 AccessControlContext 的实例，则抛出 SecurityException。

端口号为 -1 表示调用方法正在尝试确定指定主机名的 IP 地址。

如果端口不等于 -1，则此方法使用 SocketPermission(host+":"+port,"connect") 权限调用 checkPermission。
如果端口等于 -1，则它将使用 SocketPermission(host,"resolve") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkConnect。
*/
public void checkConnect(String host, int port, Object context) {
    if (host == null) {
        throw new NullPointerException("host can't be null");
    }
    if (!host.startsWith("[") && host.indexOf(':') != -1) {
        host = "[" + host + "]";
    }
    if (port == -1)
        checkPermission(new SocketPermission(host,
            SecurityConstants.SOCKET_RESOLVE_ACTION),
            context);
    else
        checkPermission(new SocketPermission(host+":"+port,
            SecurityConstants.SOCKET_CONNECT_ACTION),
            context);
}
```

## 3.18 checkListen
```java
/*
如果不允许调用线程在指定的本地端口号上等待连接请求，则引发 SecurityException。

此方法使用 SocketPermission("localhost:"+port,"listen") 调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用super.checkListen。
*/
public void checkListen(int port) {
    checkPermission(new SocketPermission("localhost:"+port, SecurityConstants.SOCKET_LISTEN_ACTION));
}
```

## 3.19 checkAccept
```java
/*
如果不允许调用线程接受来自指定主机和端口号的套接字连接，则引发 SecurityException。

ServerSocket 类的 accept 方法使用当前的安全管理器调用此方法。
此方法使用 SocketPermission(host+":"+port,"accept") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkAccept。
*/
public void checkAccept(String host, int port) {
    if (host == null) {
        throw new NullPointerException("host can't be null");
    }
    if (!host.startsWith("[") && host.indexOf(':') != -1) {
        host = "[" + host + "]";
    }
    checkPermission(new SocketPermission(host+":"+port, SecurityConstants.SOCKET_ACCEPT_ACTION));
}
```

## 3.20 checkMulticast
```java
/*
如果不允许调用线程使用（加入/离开/发送/接收）IP 多播，则抛出 SecurityException。

此方法使用 java.net.SocketPermission(maddr.getHostAddress(), "accept,connect") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkMulticast。
*/
public void checkMulticast(InetAddress maddr) {
    String host = maddr.getHostAddress();
    if (!host.startsWith("[") && host.indexOf(':') != -1) {
        host = "[" + host + "]";
    }
    checkPermission(new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
}

/*
如果不允许调用线程使用（加入/离开/发送/接收） IP 多播，则抛出 SecurityException。

此方法使用 java.net.SocketPermission(maddr.getHostAddress(), "accept,connect") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkMulticast。

@param ttl: 使用的值（如果是多播发送）。注意：此特定实现不使用 ttl 参数。

@deprecated 使用 checkPermission(java.security.Permission) 代替
*/
@Deprecated
public void checkMulticast(InetAddress maddr, byte ttl) {
    String host = maddr.getHostAddress();
    if (!host.startsWith("[") && host.indexOf(':') != -1) {
        host = "[" + host + "]";
    }
    checkPermission(new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
}
```

## 3.21 checkPropertiesAccess
```java
/*
如果不允许调用线程访问或修改系统属性，则引发 SecurityException。

System 类的 getProperties 和 setProperties 方法使用此方法。
此方法使用 PropertyPermission("*", "read,write") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkPropertiesAccess。
*/
public void checkPropertiesAccess() {
    checkPermission(new PropertyPermission("*", SecurityConstants.PROPERTY_RW_ACTION));
}

/*
如果不允许调用线程使用指定的 key 名访问系统属性，则引发 SecurityException。

System 类的 getProperty 方法使用此方法。
此方法使用 PropertyPermission(key, "read") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkPropertyAccess。
*/
public void checkPropertyAccess(String key) {
    checkPermission(new PropertyPermission(key, SecurityConstants.PROPERTY_READ_ACTION));
}
```

## 3.22 checkTopLevelWindow
```java
/*
如果不信任调用线程来调出 window 参数指示的顶级窗口，则返回 false 。在这种情况下，调用者仍可以决定显示该窗口，
但是该窗口应包括某种视觉警告。如果该方法返回 true，则可以显示该窗口而没有任何特殊限制。

有关受信任和不受信任的 Window 的更多信息，请参见类 Window。

此方法使用 AWTPermission("showWindowWithoutWarningBanner") 权限调用 checkPermission。

如果没有抛出 SecurityException 则返回 true，否则返回 false。如果 Java SE 的子集概要文件不包含 java.awt 包，
则调用 checkPermission 来检查权限 java.security.AllPermission。

如果重写此方法，则应在被重写的方法通常返回 false 的点上调用 super.checkTopLevelWindow，
并应返回 super.checkTopLevelWindow 的值。

@deprecated 对 AWTPermission 的依赖性阻碍了 Java 平台未来的模块化。相反，此方法的用户应直接调用 checkPermission。
在将来的发行版中，将更改此方法以检查权限 java.security.AllPermission。
*/
@Deprecated
public boolean checkTopLevelWindow(Object window) {
    if (window == null) {
        throw new NullPointerException("window can't be null");
    }
    Permission perm = SecurityConstants.AWT.TOPLEVEL_WINDOW_PERMISSION;
    if (perm == null) {
        perm = SecurityConstants.ALL_PERMISSION;
    }
    try {
        checkPermission(perm);
        return true;
    } catch (SecurityException se) {
        // just return false
    }
    return false;
}
```

## 3.23 checkPrintJobAccess
```java
/*
如果不允许调用线程发起打印任务请求，则引发 SecurityException。

此方法使用 RuntimePermission("queuePrintJob") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkPrintJobAccess。
*/
public void checkPrintJobAccess() {
    checkPermission(new RuntimePermission("queuePrintJob"));
}
```

## 3.24 checkSystemClipboardAccess
```java
/*
如果不允许调用线程访问系统剪贴板，则引发 SecurityException。

此方法使用 AWTPermission("accessClipboard") 权限调用 checkPermission。如果 Java SE 的子集概要文件不包含 java.awt 包，
则调用 checkPermission 来检查权限 java.security.AllPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkSystemClipboardAccess。

@deprecated 对 AWTPermission 的依赖性阻碍了 Java 平台未来的模块化。相反，此方法的用户应直接调用 checkPermission。
在将来的发行版中，将更改此方法以检查权限 java.security.AllPermission。
*/
@Deprecated
public void checkSystemClipboardAccess() {
    Permission perm = SecurityConstants.AWT.ACCESS_CLIPBOARD_PERMISSION;
    if (perm == null) {
        perm = SecurityConstants.ALL_PERMISSION;
    }
    checkPermission(perm);
}
```

## 3.25 checkAwtEventQueueAccess
```java
/*
如果不允许调用线程访问 AWT 事件队列，则引发 SecurityException。

此方法使用 AWTPermission("accessEventQueue") 权限调用 checkPermission。如果 Java SE 的子集概要文件不包含 java.awt 包，
则调用 checkPermission 来检查权限 java.security.AllPermission。

如果重写此方法，则应在被重写的方法通常会引发异常的点上调用 super.checkAwtEventQueueAccess。

@deprecated 对 AWTPermission 的依赖性阻碍了 Java 平台未来的模块化。相反，此方法的用户应直接调用 checkPermission。
在将来的发行版中，将更改此方法以检查权限 java.security.AllPermission。
*/
@Deprecated
public void checkAwtEventQueueAccess() {
    Permission perm = SecurityConstants.AWT.CHECK_AWT_EVENTQUEUE_PERMISSION;
    if (perm == null) {
        perm = SecurityConstants.ALL_PERMISSION;
    }
    checkPermission(perm);
}
```

## 3.26 checkPackage
```java
/*
如果不允许调用线程访问参数指定的包，则引发 SecurityException。

类加载器的 loadClass 方法使用此方法。
此方法首先通过从对 java.security.Security.getProperty("package.access") 的调用中获取受限制软件包的列表（使用逗号分隔），
然后检查 pkg 是否以任何受限制软件包开头或等于。如果是这样，那么将使用 RuntimePermission("accessClassInPackage."+pkg)
权限调用 checkPermission 。

如果此方法被覆盖，则应将 super.checkPackageAccess 作为被覆盖方法的第一行。
*/
public void checkPackageAccess(String pkg) {
    if (pkg == null) {
        throw new NullPointerException("package name can't be null");
    }

    String[] pkgs;
    synchronized (packageAccessLock) {
        // 如果缓存无效
        if (!packageAccessValid) {
            String tmpPropertyStr =
                AccessController.doPrivileged(
                    new PrivilegedAction<String>() {
                        public String run() {
                            return java.security.Security.getProperty("package.access");
                        }
                    }
                );
            // 获取最新的受限制包列表并赋值给 packageAccess
            packageAccess = getPackages(tmpPropertyStr);
            packageAccessValid = true;
        }

        // 使用 packageAccess 的快照 -- 不用担心静态字段在之后是否更改，数组内容不会更改。
        pkgs = packageAccess;
    }

    // 遍历所有受限制包，看看是否与 pkg 匹配
    for (int i = 0; i < pkgs.length; i++) {
        if (pkg.startsWith(pkgs[i]) || pkgs[i].equals(pkg + ".")) {
            checkPermission(new RuntimePermission("accessClassInPackage."+pkg));
            break;  // 无需继续; 只需要检查一次
        }
    }
}

/*
如果不允许调用线程在参数指定的包中定义类，则抛出 SecurityException。

某些类加载器的 loadClass 方法使用此方法。
此方法首先通过从对 java.security.Security.getProperty("package.definition") 的调用中获取受限制软件包的列表（用逗号分隔），
然后检查 pkg 是否以任何受限制软件包开头或等于。如果是，则用 RuntimePermission("defineClassInPackage."+pkg)
权限调用 checkPermission。

如果此方法被覆盖，则应将 super.checkPackageDefinition 作为覆盖方法的第一行。
*/
public void checkPackageDefinition(String pkg) {
    if (pkg == null) {
        throw new NullPointerException("package name can't be null");
    }

    String[] pkgs;
    synchronized (packageDefinitionLock) {
        // 如果缓存无效
        if (!packageDefinitionValid) {
            String tmpPropertyStr =
                AccessController.doPrivileged(
                    new PrivilegedAction<String>() {
                        public String run() {
                            return java.security.Security.getProperty("package.definition");
                        }
                    }
                );
            // 获取最新的受限制包列表并赋值给 packageDefinition
            packageDefinition = getPackages(tmpPropertyStr);
            packageDefinitionValid = true;
        }
        // 使用 packageDefinition 的快照 -- 不用担心静态字段在之后是否更改，数组内容不会更改。
        pkgs = packageDefinition;
    }

    // 遍历所有受限制包，看看是否与 pkg 匹配
    for (int i = 0; i < pkgs.length; i++) {
        if (pkg.startsWith(pkgs[i]) || pkgs[i].equals(pkg + ".")) {
            checkPermission(new RuntimePermission("defineClassInPackage."+pkg));
            break; // 无需继续; 只需要检查一次
        }
    }
}

private static String[] getPackages(String p) {
    String packages[] = null;
    if (p != null && !p.equals("")) {
        // 使用逗号分割列表
        java.util.StringTokenizer tok = new java.util.StringTokenizer(p, ",");
        int n = tok.countTokens();
        if (n > 0) {
            packages = new String[n];
            int i = 0;
            while (tok.hasMoreElements()) {
                String s = tok.nextToken().trim();
                packages[i++] = s;
            }
        }
    }

    if (packages == null)
        packages = new String[0];
    return packages;
}
```

## 3.27 checkSetFactory
```java
/*
如果不允许调用线程设置 ServerSocket 或 Socket 使用的套接字工厂，或 URL 使用的流处理程序工厂，则抛出 SecurityException。

此方法使用 RuntimePermission("setFactory") 权限调用 checkPermission。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点调用 super.checkSetFactory。
*/
public void checkSetFactory() {
    checkPermission(new RuntimePermission("setFactory"));
}
```

## 3.28 checkMemberAccess
```java
/*
如果不允许调用线程访问成员，则引发 SecurityException。

默认策略是允许访问 PUBLIC 成员，以及访问与调用者具有相同类加载器的类。
在所有其他情况下，此方法使用 RuntimePermission("accessDeclaredMembers") 权限调用 checkPermission。

如果重写此方法，则无法调用 super.checkMemberAccess，因为 checkMemberAccess 的默认实现依赖于所检查的代码的堆栈深度为 4。

@deprecated 此方法依赖于调用者的堆栈深度为 4，这很容易出错，并且运行时无法强制执行。相反，
此方法的用户应直接调用 checkPermission。在将来的发行版中，将更改此方法以检查权限 java.security.AllPermission。
*/
@Deprecated
// 注解 @CallerSensitive 的方法对其调用类敏感
@CallerSensitive
public void checkMemberAccess(Class<?> clazz, int which) {
    if (clazz == null) {
        throw new NullPointerException("class can't be null");
    }
    if (which != Member.PUBLIC) {
        Class<?> stack[] = getClassContext();
        /*
         * 堆栈深度 4 上应该是调用 java.lang.Class 中方法之一的调用者。堆栈应看起来像：
         *
         * someCaller                        [3]
         * java.lang.Class.someReflectionAPI [2]
         * java.lang.Class.checkMemberAccess [1]
         * SecurityManager.checkMemberAccess [0]
         *
         */
        if ((stack.length<4) || (stack[3].getClassLoader() != clazz.getClassLoader())) {
            checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
        }
    }
}
```

## 3.29 checkSecurityAccess
```java
/*
确定是否应授予或拒绝具有指定权限目标名称的权限。如果允许所请求的权限，则此方法安静地返回。如果被拒绝，则会引发 SecurityException。

此方法为给定的权限目标名称创建一个 SecurityPermission 对象，使用它调用 checkPermission。
有关可能的权限目标名称的列表，请参阅 SecurityPermission 的文档。

如果覆盖此方法，则应在被覆盖的方法通常会引发异常的点上调用 super.checkSecurityAccess。
*/
public void checkSecurityAccess(String target) {
    checkPermission(new SecurityPermission(target));
}
```


[permissions]: https://docs.oracle.com/javase/8/docs/technotes/guides/security/permissions.html
[security]: https://docs.oracle.com/javase/8/docs/technotes/guides/security/index.html
[sm]: 安全管理器.md
[finalize-attack]: finalize攻击.md