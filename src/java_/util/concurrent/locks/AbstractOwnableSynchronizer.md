`java.util.concurrent.locks.AbstractOwnableSynchronizer`抽象类的声明如下：
```java
public abstract class AbstractOwnableSynchronizer implements java.io.Serializable
```
一个可以由线程独占的同步器。这个类为创建锁和相关的同步器提供了一个基础，这些同步器可能涉及到所有权的概念。
`AbstractOwnableSynchronizer`类本身并不管理或使用这些信息。然而，
子类和工具可以使用适当维护的值来帮助控制和监视访问并提供诊断。

# 1. 属性
```java
private static final long serialVersionUID = 3737899427754241961L;

// 当前拥有独占访问权的线程
private transient Thread exclusiveOwnerThread;
```

# 2. 构造器
```java
protected AbstractOwnableSynchronizer() { }
```

# 3. 方法

## 3.1 setExclusiveOwnerThread
```java
// 设置当前拥有独占访问权的线程。null 参数表示没有线程拥有访问权。本方法不施加任何同步或 volatile 字段访问。
protected final void setExclusiveOwnerThread(Thread thread) {
    exclusiveOwnerThread = thread;
}
```

## 3.2 getExclusiveOwnerThread
```java
// 返回上次由 setExclusiveOwnerThread 设置的线程，如果从未设置，则返回 null。本方法不施加任何同步或 volatile 字段访问。
protected final Thread getExclusiveOwnerThread() {
    return exclusiveOwnerThread;
}
```