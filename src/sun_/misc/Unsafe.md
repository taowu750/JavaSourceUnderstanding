`sun.misc.Unsafe`类的声明如下：
```java
public final class Unsafe
```
一个用于执行低级、不安全操作的方法集合。虽然该类和所有方法都是公开的，但该类的使用是有限的，
因为只有受信任的代码才能获得它的实例。

`Unsafe` 主要提供一些用于执行低级别、不安全操作的方法，如直接访问系统内存资源、自主管理内存资源等，
这些方法在提升 Java 运行效率、增强 Java 语言底层资源操作能力方面起到了很大的作用。

但由于 `Unsafe` 类使 Java 语言拥有了类似 C 语言指针一样操作内存空间的能力，这无疑也增加了程序发生相关指针问题的风险。
在程序中过度、不正确使用 `Unsafe` 类会使得程序出错的概率变大，使得 Java 这种安全的语言变得不再“安全”，
因此对 `Unsafe` 的使用一定要慎重。

# 1. 构造器/块
```java
private static native void registerNatives();
static {
    registerNatives();
    // 让用户的 Java 类不能获取 Unsafe 的实例
    sun.reflect.Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
}

private Unsafe() {}
```

# 2. 成员字段

## 2.1 实例
```java
private static final Unsafe theUnsafe = new Unsafe();
```

## 2.2 常量

### 2.2.1 INVALID_FIELD_OFFSET
```java
// 这个常量与所有从 staticFieldOffset()、objectFieldOffset() 或 arrayBaseOffset() 方法返回的结果不同。
// 表示不合法的偏移量
public static final int INVALID_FIELD_OFFSET   = -1;
```

### 2.2.2 ADDRESS_SIZE
```java
// 
public static final int ADDRESS_SIZE = theUnsafe.addressSize();
```

### 2.2.3 ARRAY_BASE_OFFSET
```java
public static final int ARRAY_BOOLEAN_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(boolean[].class);

public static final int ARRAY_BYTE_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(byte[].class);

public static final int ARRAY_SHORT_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(short[].class);

public static final int ARRAY_CHAR_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(char[].class);

public static final int ARRAY_INT_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(int[].class);

public static final int ARRAY_LONG_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(long[].class);

public static final int ARRAY_FLOAT_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(float[].class);

public static final int ARRAY_DOUBLE_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(double[].class);

public static final int ARRAY_OBJECT_BASE_OFFSET
        = theUnsafe.arrayBaseOffset(Object[].class);
```

### 2.2.4 ARRAY_INDEX_SCALE
```java
public static final int ARRAY_BOOLEAN_INDEX_SCALE
        = theUnsafe.arrayIndexScale(boolean[].class);

public static final int ARRAY_BYTE_INDEX_SCALE
        = theUnsafe.arrayIndexScale(byte[].class);

public static final int ARRAY_SHORT_INDEX_SCALE
        = theUnsafe.arrayIndexScale(short[].class);

public static final int ARRAY_CHAR_INDEX_SCALE
        = theUnsafe.arrayIndexScale(char[].class);

public static final int ARRAY_INT_INDEX_SCALE
        = theUnsafe.arrayIndexScale(int[].class);

public static final int ARRAY_LONG_INDEX_SCALE
        = theUnsafe.arrayIndexScale(long[].class);

public static final int ARRAY_FLOAT_INDEX_SCALE
        = theUnsafe.arrayIndexScale(float[].class);

public static final int ARRAY_DOUBLE_INDEX_SCALE
        = theUnsafe.arrayIndexScale(double[].class);

public static final int ARRAY_OBJECT_INDEX_SCALE
        = theUnsafe.arrayIndexScale(Object[].class);
```

# 3. 方法

## 3.1 getUnsafe
```java
/*
为调用者提供执行不安全操作的能力。

返回的 Unsafe 对象应该由调用者小心保护，因为它可以被用于在任意内存地址读写数据。
绝不能将它传递给不受信任的代码。

本类中的大多数方法都是非常低级的，对应于少量的硬件指令（在典型的机器上）。我们鼓励编译器对这些方法进行相应的优化。
下面是一个建议的使用不安全操作的习惯用法。
       class MyTrustedClass {
         private static final Unsafe unsafe = Unsafe.getUnsafe()。
         ...
         private long myCountAddress = ...;

         public int getCount() { return unsafe.getByte(myCountAddress); }.
       }

(它可以帮助编译器将局部变量变成 final 变量。)
*/
@CallerSensitive
public static Unsafe getUnsafe() {
    Class<?> caller = Reflection.getCallerClass();
    // 仅在引导类加载器 BootstrapClassLoader 加载时才合法
    if (!VM.isSystemDomainLoader(caller.getClassLoader()))
        throw new SecurityException("Unsafe");
    return theUnsafe;
}
```
那如若想使用这个类，该如何获取其实例？有如下两个可行方案。
1. 从 `getUnsafe` 方法的使用限制条件出发，通过 Java 命令行命令 `-Xbootclasspath/a` 把调用 `Unsafe` 相关方法的类 `A` 所在 `jar` 包的路径，
追加到默认的 bootstrap 路径中，使得 `A` 被引导类加载器加载，从而通过 `Unsafe.getUnsafe` 方法安全的获取 `Unsafe` 实例。
2. 通过反射获取单例对象 `theUnsafe`。
    ```java
    private static Unsafe reflectGetUnsafe() {
        try {
          Field field = Unsafe.class.getDeclaredField("theUnsafe");
          field.setAccessible(true);
          return (Unsafe) field.get(null);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          return null;
        }
    }
    ```

## 3.2 offset 和 base
```java
// 返回数组中第一个元素的偏移地址
public native int arrayBaseOffset(Class<?> arrayClass);

// 返回数组中一个元素占用的大小
public native int arrayIndexScale(Class<?> arrayClass);

// 返回静态字段在内存地址相对于此对象的内存地址的偏移量
public native long staticFieldOffset(Field f);

// 获取一个类中给定静态字段的对象指针
public native Object staticFieldBase(Field f);

// 返回对象字段在内存地址相对于此对象的内存地址的偏移量
public native long objectFieldOffset(Field f);
```

## 3.3 地址操作
```java
/*
以字节为单位报告通过 putAddress 存储的本地指针的大小。这个值将是 4 或 8。
请注意，其他基本类型（存储在本地内存块中）的大小完全由其信息内容决定。
*/
public native int addressSize();

// 以字节为单位报告本地内存页的大小。这个值永远是 2 的幂。
public native int pageSize();

/*
从给定的内存地址中获取一个本地指针，如果地址为零，或者没有指向从 allocateMemory 获得的块，则结果未定义。
。
如果本地指针的宽度小于 64 位，它将作为一个无符号数扩展为一个 Java long。指针可以通过任何给定的字节偏移量进行索引，
只需将该偏移量（整数）和代表指针的 long 相加即可。

实际写入目标地址的字节数也可以通过 addressSize() 方法来确定。
*/
public native long getAddress(long address);

/*
将一个本地指针存储到一个给定的内存地址中。如果地址为零，或者没有指向从 allocateMemory 获得的块，则结果未定义。

实际写入目标地址的字节数也可以通过 addressSize() 方法来确定。
*/
public native void putAddress(long address, long x);
```

## 3.4 本地内存分配
```java
/*
分配一个新的本地内存块，大小以字节为单位。内存的内容是未初始化的，通常是垃圾。
由此产生的本地指针永远不会为零，并且会对所有值类型进行对齐。

通过调用 freeMemory 来清理这个内存，或者通过 reallocateMemory 来调整它的大小。
*/
public native long allocateMemory(long bytes);

/*
重新调整本地内存块的大小，以字节为单位。新块中超过旧块大小的内容是未初始化的；它们通常是垃圾。
如果且仅当请求的大小为零时，产生的本机指针将为零。
由此产生的本机指针将对所有值类型进行对齐。

通过调用 freeMemory 来清理这个内存，或者通过 reallocateMemory 来调整它的大小。
传递给该方法的地址可能是 null，在这种情况下，将执行分配。
*/
public native long reallocateMemory(long address, long bytes);

/*
将一个给定内存块中的指定字节设置为一个固定值（通常为零）。

这个方法通过两个参数来确定一个块的基地址，因此它提供了（实际上）一个双寄存器寻址模式，如 getInt(Object, long) 中讨论的那样。
当对象引用为空时，偏移量提供一个绝对的基地址。

内存块是连续的，其大小由地址和长度（bytes）参数决定。
如果有效地址和长度都是 8 的倍数，则存储以 "long" 单位进行。
如果有效地址和长度都是 4 或 2 的倍数，则存储以 "int" 或 "short" 为单位。
*/
public native void setMemory(Object o, long offset, long bytes, byte value);

/*
将一个给定内存块中的指定字节设置为一个固定值（通常为零）。这提供了一个单寄存器寻址模式，
如 getInt(Object, long) 中讨论的那样。

相当于 setMemory(null, address, bytes, value)。
*/
public void setMemory(long address, long bytes, byte value) {
    setMemory(null, address, bytes, value);
}

/*
将一个给定内存块中的指定字节拷贝到另一个内存块中。

这个方法通过两个参数来确定每个块的基地址，因此它提供了（实际上）一个双寄存器寻址模式，如 getInt(Object, long) 中讨论的那样。
当对象引用为空时，偏移量提供一个绝对的基地址。

内存块是连续的，其大小由地址和长度参数决定。
如果有效地址和长度都是 8 的倍数，则拷贝以 "long" 单位进行。
如果有效地址和长度都是 4 或 2 的倍数，则拷贝以 "int" 或 "short" 为单位。
*/
public native void copyMemory(Object srcBase, long srcOffset,
                              Object destBase, long destOffset,
                              long bytes);

/*
将一个给定内存块中的指定字节拷贝到另一个内存块中。这提供了一个单寄存器寻址模式，如 getInt(Object, long) 中讨论的那样。

相当于 copyMemory(null, srcAddress, null, destAddress, bytes)。
*/
public void copyMemory(long srcAddress, long destAddress, long bytes) {
    copyMemory(null, srcAddress, null, destAddress, bytes);
}

/*
释放从 allocateMemory 或 reallocateMemory 获得的本地内存块。
传递给本方法的地址可能是 null，在这种情况下，不会采取任何行动。
*/
public native void freeMemory(long address);
```
通常，我们在 Java 中创建的对象都处于堆内内存（heap）中，堆内内存是由 JVM 所管控的 Java 进程内存，
并且它们遵循 JVM 的内存管理机制，JVM 会采用垃圾回收机制统一管理堆内存。与之相对的是堆外内存，
存在于 JVM 管控之外的内存区域，Java 中对堆外内存的操作，依赖于 `Unsafe` 提供的操作堆外内存的 `native` 方法。

使用堆外内存有如下原因：
 - 对垃圾回收停顿的改善。由于堆外内存是直接受操作系统管理而不是 JVM，所以当我们使用堆外内存时，
 即可保持较小的堆内内存规模。从而在 GC 时减少回收停顿对于应用的影响。
 - 提升程序 I/O 操作的性能。通常在 I/O 通信过程中，会存在堆内内存到堆外内存的数据拷贝操作，
 对于需要频繁进行内存间数据拷贝且生命周期较短的暂存数据，都建议存储到堆外内存。

`DirectByteBuffer` 是 Java 用于实现堆外内存的一个重要类，通常用在通信过程中做缓冲池，如在 Netty、MINA 等 NIO 框架中应用广泛。
`DirectByteBuffer` 对于堆外内存的创建、使用、销毁等逻辑均由 `Unsafe` 提供的堆外内存 API 来实现。

## 3.5 get/put 对象中的字段
```java
/*
从给定的 Java 变量中获取一个值。更具体地说，是在给定的偏移量处从给定对象 o 中获取一个字段或数组元素，
或者（如果 o 为空）从数值为给定偏移量的内存地址中获取。

如果以下情况都不满足，则结果是未定义的：
 - 偏移量是从某个 Java 字段的 Field 上的 objectFieldOffset 获得的，并且 o 所引用的对象与该字段的类兼容。
 - 偏移量和对象引用 o(要么为空，要么为非空)都是通过 staticFieldOffset 和 staticFieldBase (分别)
   从某个Java字段的反射字段表示中获得的。
 - o 所引用的对象是一个数组，偏移量是一个整数，其形式为 B+N*S，其中 N 是数组的有效索引，B 和 S 分别是通过
   arrayBaseOffset 和 arrayIndexScale 从数组的类中得到的值。所指的值是数组的第 N 个元素。

如果上述情况之一为真，则返回一个特定的 Java 变量（字段或数组元素）。但是，如果该变量实际上不是本方法返回的类型，
则结果是未定义的。

这个方法通过两个参数来引用一个变量，因此它为 Java 变量提供了（实际上）一种双寄存器寻址模式。
当对象引用为空时，本方法将其偏移量作为绝对地址。这与 getInt(long) 等方法的操作类似，
后者为非 Java 变量提供（实际上）单寄存器寻址模式。

然而，由于 Java 变量在内存中的布局可能与非 Java 变量不同，所以程序员不应该认为这两种寻址模式永远是等同的。
另外，程序员应该记住，双寄存器寻址模式的偏移量不能与单寄存器寻址模式中使用的 long 值相混淆。
*/
public native int getInt(Object o, long offset);

/*
将一个值存储到一个给定的 Java 变量中。

前两个参数的解释与 getInt(Object, long) 完全相同，是指一个特定的 Java 变量（字段或数组元素）。
给定的值被存储到该变量中，该变量必须与方法参数 x 的类型相同。
*/
public native void putInt(Object o, long offset, int x);

// 从一个给定的 Java 变量中获取一个引用值，参见 getInt(Object, long)。
public native Object getObject(Object o, long offset);

/*
将一个引用值存储到一个给定的 Java 变量中。

除非被存储的引用 x 为空或与字段类型相匹配，否则结果是未定义的。如果引用 o 为非空，
则会更新该对象的 car marks(对象头？)或其他内存屏障（如果 VM 需要的话）。
*/
public native void putObject(Object o, long offset, Object x);

public native boolean getBoolean(Object o, long offset);

public native void    putBoolean(Object o, long offset, boolean x);

public native byte    getByte(Object o, long offset);

public native void    putByte(Object o, long offset, byte x);

public native short   getShort(Object o, long offset);

public native void    putShort(Object o, long offset, short x);

public native char    getChar(Object o, long offset);

public native void    putChar(Object o, long offset, char x);

public native long    getLong(Object o, long offset);

public native void    putLong(Object o, long offset, long x);

public native float   getFloat(Object o, long offset);

public native void    putFloat(Object o, long offset, float x);

public native double  getDouble(Object o, long offset);

public native void    putDouble(Object o, long offset, double x);
```

## 3.6 get/put 绝对地址上的值
```java
/*
从一个给定的内存地址中获取一个字节。
如果地址为零，或者没有指向从 allocateMemory 获取的块，则结果未定义。
*/
public native byte    getByte(long address);

/*
将一个字节存储到一个给定的内存地址中。
如果地址为零，或者没有指向从 allocateMemory 获取的块，则结果未定义。
*/
public native void    putByte(long address, byte x);

public native short   getShort(long address);

public native void    putShort(long address, short x);

public native char    getChar(long address);

public native void    putChar(long address, char x);

public native int     getInt(long address);

public native void    putInt(long address, int x);

public native long    getLong(long address);

public native void    putLong(long address, long x);

public native float   getFloat(long address);

public native void    putFloat(long address, float x);

public native double  getDouble(long address);

public native void    putDouble(long address, double x);
```
这些方法和用来操作分配的本地内存。

## 3.7 get/put 对象中的字段（volatile）
```java
// getObject(Object, long) 的 volatile 版本
public native Object getObjectVolatile(Object o, long offset);

// putObject(Object o, long offset, Object x) 的 volatile 版本
public native void    putObjectVolatile(Object o, long offset, Object x);

public native int     getIntVolatile(Object o, long offset);

public native void    putIntVolatile(Object o, long offset, int x);

public native boolean getBooleanVolatile(Object o, long offset);

public native void    putBooleanVolatile(Object o, long offset, boolean x);

public native byte    getByteVolatile(Object o, long offset);

public native void    putByteVolatile(Object o, long offset, byte x);

public native short   getShortVolatile(Object o, long offset);

public native void    putShortVolatile(Object o, long offset, short x);

public native char    getCharVolatile(Object o, long offset);

public native void    putCharVolatile(Object o, long offset, char x);

public native long    getLongVolatile(Object o, long offset);

public native void    putLongVolatile(Object o, long offset, long x);

public native float   getFloatVolatile(Object o, long offset);

public native void    putFloatVolatile(Object o, long offset, float x);

public native double  getDoubleVolatile(Object o, long offset);

public native void    putDoubleVolatile(Object o, long offset, double x);
```

## 3.8 putOrdered
```java
/*
putObjectVolatile(Object, long, Object) 方法的另一个版本，它不保证存储空间对其他线程的立即可见性。
这个方法一般只在底层字段是 volatile 的情况下才有用（或者是一个数组单元，否则只能使用 volatile 访问）。
*/
public native void    putOrderedObject(Object o, long offset, Object x);

public native void    putOrderedInt(Object o, long offset, int x);

public native void    putOrderedLong(Object o, long offset, long x);
```

## 3.9 CAS 操作
```java
// 使用 CAS 的方式更新对象偏移位置处的引用字段
public final native boolean compareAndSwapObject(Object o, long offset,
                                                 Object expected,
                                                 Object x);

// 使用 CAS 的方式更新对象偏移位置处的 int 字段
public final native boolean compareAndSwapInt(Object o, long offset,
                                              int expected,
                                              int x);

// 使用 CAS 的方式更新对象偏移位置处的 long 字段
public final native boolean compareAndSwapLong(Object o, long offset,
                                               long expected,
                                               long x);

// 原子化地将给定的值加到给定对象 o 中给定偏移量的字段或数组元素的当前值上。
// 返回之前的值。
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    // 自旋操作
    do {
        v = getIntVolatile(o, offset);
    } while (!compareAndSwapInt(o, offset, v, v + delta));
    return v;
}

public final long getAndAddLong(Object o, long offset, long delta) {
    long v;
    do {
        v = getLongVolatile(o, offset);
    } while (!compareAndSwapLong(o, offset, v, v + delta));
    return v;
}

public final int getAndSetInt(Object o, long offset, int newValue) {
    int v;
    do {
        v = getIntVolatile(o, offset);
    } while (!compareAndSwapInt(o, offset, v, newValue));
    return v;
}

public final long getAndSetLong(Object o, long offset, long newValue) {
    long v;
    do {
        v = getLongVolatile(o, offset);
    } while (!compareAndSwapLong(o, offset, v, newValue));
    return v;
}

public final Object getAndSetObject(Object o, long offset, Object newValue) {
    Object v;
    do {
        v = getObjectVolatile(o, offset);
    } while (!compareAndSwapObject(o, offset, v, newValue));
    return v;
}
```
什么是 CAS? 即比较并替换，实现并发算法时常用到的一种技术。CAS 操作包含三个操作数——内存位置、预期原值及新值。
执行 CAS 操作的时候，将内存位置的值与预期原值比较，如果相匹配，那么处理器会自动将该位置值更新为新值，否则，
处理器不做任何操作。

CAS 是一条 CPU 的原子指令（`cmpxchg` 指令），不会造成所谓的数据不一致问题，`Unsafe` 提供的 CAS 方法（如 `compareAndSwapXXX`）
底层实现即为 CPU 指令 `cmpxchg`。

CAS 在 `java.util.concurrent.atomic` 相关类、Java `AQS`、`CurrentHashMap` 等实现上有非常广泛的应用。

## 3.10 锁操作
```java
// 锁定对象。必须通过 monitorExit 来解锁。
@Deprecated
public native void monitorEnter(Object o);

// 解锁对象。它必须已经被 monitorEnter 锁定。
@Deprecated
public native void monitorExit(Object o);

// 常识锁定对象，返回 true 或 false，表示锁定是否成功。如果锁定成功，则必须通过 monitorExit 解锁对象。
@Deprecated
public native boolean tryMonitorEnter(Object o);

/*
阻塞线程。

@param isAbsolute 是否是绝对时间。为 true 则会实现毫秒定时；为 false 则会实现纳秒定时。
@param time 等待时间值。为 0 表示一直等待。
*/
public native void park(boolean isAbsolute, long time);

// 解锁线程
public native void unpark(Object thread);
```
如上源码说明中，方法 `park`、`unpark` 即可实现线程的挂起与恢复。将一个线程进行挂起是通过 `park` 方法实现的，
调用 `park` 方法后，线程将一直阻塞直到超时或者中断等条件出现；`unpark` 可以终止一个挂起的线程，使其恢复正常。

Java 锁和同步器框架的核心类 `AbstractQueuedSynchronizer`，就是通过调用 `LockSupport.park()` 和 `LockSupport.unpark()` 实现线程的阻塞和唤醒的，
而 `LockSupport` 的 `park`、`unpark` 方法实际是调用 `Unsafe` 的 `park`、`unpark` 方式来实现。

## 3.11 Class 操作
```java
// 判断是否需要初始化一个类，通常在获取一个类的静态属性的时候（因为一个类如果没初始化，它的静态属性也不会初始化）使用。
public native boolean shouldBeInitialized(Class<?> c);

// 确保给定的类是否已经初始化。通常在获取一个类的静态属性的时候（因为一个类如果没初始化，它的静态属性也不会初始化）使用。
public native void ensureClassInitialized(Class<?> c);

// 告诉虚拟机定义一个类，不进行安全检查。默认情况下，loader 和 protectionDomain 来自调用者的类。
public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                   ClassLoader loader,
                                   ProtectionDomain protectionDomain);

/*
定义一个匿名类。
对于每个 CP 条目，对应的 cp patch 必须是 null，或者具有与下面标签相匹配的格式：
 - Integer, Long, Float, Double: 来自 java.lang 的包装对象类型。
 - Utf8：一个字符串（如果用作签名或名称，必须有合适的语法）。
 - Class：任何 java.lang.Class 对象。
 - String：任何对象（不仅仅是 java.lang.String）。
 - InterfaceMethodRef: (NYI)一个方法句柄，用于在该调用位置的参数上调用。

@param hostClass 链接、访问控制、保护域和类加载器的上下文。
@param data 类文件的字节
@param cpPatches 在存在非 null 条目的情况下，它们取代 data 中相应的 CP 条目。
*/
public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);
```
从 Java 8 开始，JDK 使用 `invokedynamic` 及 VM Anonymous Class 结合来实现 Java 语言层面上的 Lambda 表达式。
 - `invokedynamic`： `invokedynamic` 是 Java 7 为了实现在 JVM 上运行动态语言而引入的一条新的虚拟机指令，
 它可以实现在运行期动态解析出调用点限定符所引用的方法，然后再执行该方法。`invokedynamic` 指令的分派逻辑是由用户设定的引导方法决定。
 - VM Anonymous Class：可以看做是一种模板机制，针对于程序动态生成很多结构相同、仅若干常量不同的类时，
 可以先创建包含常量占位符的模板类，而后通过 `Unsafe.defineAnonymousClass` 方法定义具体类时填充模板的占位符生成具体的匿名类。
 生成的匿名类不显式挂在任何 `ClassLoader` 下面，只要当该类没有存在的实例对象、且没有强引用来引用该类的 `Class` 对象时，
 该类就会被 GC 回收。故而 VM Anonymous Class 相比于 Java 语言层面的匿名内部类无需通过 `ClassClassLoader` 进行类加载且更易回收。

在 Lambda 表达式实现中，通过 `invokedynamic` 指令调用引导方法生成调用点，在此过程中，会通过 ASM 动态生成字节码，
而后利用 `Unsafe` 的 `defineAnonymousClass` 方法定义实现相应的函数式接口的匿名类，然后再实例化此匿名类，
并返回与此匿名类中函数式方法的方法句柄关联的调用点；而后可以通过此调用点实现调用相应 Lambda 表达式定义逻辑的功能。

## 3.12 allocateInstance
```java
// 分配一个实例，但不运行任何构造函数。如果尚未初始化该类，则初始化该类。
public native Object allocateInstance(Class<?> cls) throws InstantiationException;
```
典型应用：
 - 常规对象实例化方式：我们通常所用到的创建对象的方式，从本质上来讲，都是通过 `new` 机制来实现对象的创建。
 但是，`new` 机制有个特点就是当类只提供有参的构造函数且无显示声明无参构造函数时，则必须使用有参构造函数进行对象构造，
 而使用有参构造函数时，必须传递相应个数的参数才能完成对象实例化。
 - 非常规的实例化方式：`Unsafe` 中提供 `allocateInstance` 方法，仅通过 `Class` 对象就可以创建此类的实例对象，
 而且不需要调用其构造函数、初始化代码、JVM 安全检查等。它抑制修饰符检测，也就是即使构造器是 `private` 修饰的也能通过此方法实例化，
 只需提类对象即可创建相应的对象。由于这种特性，`allocateInstance` 在 `java.lang.invoke`、Objenesis（提供绕过类构造器的对象生成方式）、
 Gson（反序列化时用到）中都有相应的应用。

## 3.13 内存屏障
```java
// 禁止 load 操作重排序。屏障前的 load 操作不能被重排序到屏障后，屏障后的 load 操作不能被重排序到屏障前
public native void loadFence();

// 内存屏障，禁止 store 操作重排序。屏障前的 store 操作不能被重排序到屏障后，屏障后的 store 操作不能被重排序到屏障前
public native void storeFence();

// 内存屏障，禁止 load、store 操作重排序
public native void fullFence();
```
在 Java 8 中引入，用于定义内存屏障（也称内存栅栏，内存栅障，屏障指令等，是一类同步屏障指令，
是 CPU 或编译器在对内存随机访问的操作中的一个同步点，使得此点之前的所有读写操作都执行后才可以开始执行此点之后的操作），
避免代码重排序。

## 3.14 getLoadAverage
```java
/*
获取系统运行队列中的负载平均值，分配给可用的处理器的各个时间段的平均值。

该方法检索给定的 nelem 数量样本，并写入给定的 loadavg 数组中。
系统最多指定 3 个样本，分别代表过去 1 分钟、5 分钟和 15 分钟的平均数。

@return 实际检索到的样本数量；如果无法获得平均负荷，则为 -1。
*/
public native int getLoadAverage(double[] loadavg, int nelems);
```

## 3.15 抛出异常
```java
// 在不告诉验证者的情况下抛出异常。
public native void throwException(Throwable ee);

// 抛出 IllegalAccessError，被虚拟机使用
private static void throwIllegalAccessError() {
    throw new IllegalAccessError();
}
```