`java.lang.Compiler`类的声明如下：
```java
public final class Compiler
```
`Compiler`类用来支持`Java`到`native`代码的编译器和相关服务。按照设计，`Compiler`类不执行任何操作，它充当`JIT`编译器实现的占位符。

`Java`虚拟机首次启动时，它将确定系统属性`java.compiler`是否存在。
（系统属性可通过`System.getProperty(String)`和`System.getProperty(String, String)`获取）。
如果存在，则假定它是库的名称（具有与平台相关的确切位置和类型）；`System.loadLibrary`如果加载成功，
则会调用该库中名为`java_lang_Compiler_start()`的函数。如果没有可用的编译器，则`Compiler`的方法将不执行任何操作。

# 1. 构造器/块
```java
// 这是个工具类，不能构造任何实例
private Compiler() {}

private static native void initialize();

private static native void registerNatives();

static {
    registerNatives();
    java.security.AccessController.doPrivileged(
        new java.security.PrivilegedAction<Void>() {
            public Void run() {
                boolean loaded = false;
                // 获取系统属性 java.compiler，它代表编译器相关库名称
                String jit = System.getProperty("java.compiler");
                // 如果库存在
                if ((jit != null) && (!jit.equals("NONE")) && (!jit.equals(""))) {
                    try {
                        System.loadLibrary(jit);
                        initialize();
                        loaded = true;
                    } catch (UnsatisfiedLinkError e) {
                        System.err.println("Warning: JIT compiler \"" +
                          jit + "\" not found. Will use interpreter.");
                    }
                }
                // 将编译器信息写入到 java.vm.info
                String info = System.getProperty("java.vm.info");
                if (loaded) {
                    System.setProperty("java.vm.info", info + ", " + jit);
                } else {
                    System.setProperty("java.vm.info", info + ", nojit");
                }
                return null;
            }
        });
}
```

# 2. 方法

## 2.1 compile
```java
// 编译指定的类。如果编译成功，则返回 true；否则如果编译失败或没有编译器，则返回 false
public static native boolean compileClass(Class<?> clazz);

// 编译名称与指定字符串匹配的所有类。如果编译成功，则返回 true；否则如果编译失败或没有编译器，则返回 false
public static native boolean compileClasses(String string);
```

## 2.2 command
```java
// 检查参数类型及其字段，并执行一些记录的操作。不需要特定的操作。
// 返回一个特定于编译器的值；如果没有可用的编译器，则为 null
public static native Object command(Object any);
```

## 2.3 enable/disable
```java
// 使编译器恢复运行。
public static native void enable();

// 使编译器停止运行。
public static native void disable();
```