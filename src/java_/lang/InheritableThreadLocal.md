`java.lang.InteritableThreadLocal`类的声明如下：
```java
public class InheritableThreadLocal<T> extends ThreadLocal<T>
```

> 作用

此类扩展了`ThreadLocal`来提供从父线程到子线程的局部变量继承：创建子线程时，它将接受所有父线程局部变量。
子线程指的是被父线程创建的线程。通常，子线程的局部变量值和父线程相同，但是，通过覆盖此类中的`childValue`方法，可以改变默认行为。
当需要将父线程中的局部变量自动传输到创建的任何子线程时，`InheritableThreadLocal`优于`ThreadLocal`。

例如调用链追踪：在调用链系统设计中，为了优化系统运行速度，会使用多线程编程，为了保证调用链 ID 能够自然的在多线程间传递，
需要考虑`ThreadLocal`传递问题。

> 原理

`Thread`类中有一个`inheritableThreadLocals`字段，用来存储与`InheritableThreadLocal`相关的`ThreadLocalMap`对象。
`Thread`类创建的时候会进行一些处理，从而可以继承父线程中的局部变量：
```java
// 创建线程
public Thread() {
    init(null, null, "Thread-" + nextThreadNum(), 0);
}

// 初始化方法
private void init(ThreadGroup g, Runnable target, String name, long stackSize) {
    // 默认情况下，设置 inheritThreadLocals 可传递
    init(g, target, name, stackSize, null, true);
}

/*
初始化一个线程.
此函数有两处调用，
 - 上面的 init()，不传 AccessControlContext，inheritThreadLocals=true
 - 传递 AccessControlContext，inheritThreadLocals=false
*/
private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
    ......(其他代码)

    if (inheritThreadLocals && parent.inheritableThreadLocals != null)
        this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);

    ......(其他代码)
}
```
可以看到，采用默认方式产生子线程时，`inheritThreadLocals=true`；若此时父线程`inheritableThreadLocals`不为空，
则使用`ThreadLocal.createInheritedMap`工厂方法将父线程的`inheritableThreadLocals`传递至子线程。

其他代码实现参见[ThreadLocal.md][thread-local]。

# 1. 方法

## 1.1 childValue
```java
// 在创建子线程时，根据父线程的值计算此子线程的初始值。在启动子线程之前，从父线程中调用此方法。
// 通过复写此方法，可以自定义从父线程传递的值
@Override
protected T childValue(T parentValue) {
    return parentValue;
}
```

## 1.2 threadLocalMap
```java
// 获取与线程 t 的 ThreadLocalMap。
@Override
ThreadLocalMap getMap(Thread t) {
    // 返回线程的 inheritableThreadLocals 变量
    return t.inheritableThreadLocals;
}

// 创建线程 t 的 ThreadLocalMap。firstValue 是初始条目的值。
void createMap(Thread t, T firstValue) {
    // 为线程的 inheritableThreadLocals 创建 ThreadLocalMap
    t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
}
```


[thread-local]: ThreadLocal.md