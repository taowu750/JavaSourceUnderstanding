`java.util.concurrent.locks.Condition`接口的声明如下：
```java
public interface Condition
```
`Condition` 将 Object monitor 方法（`wait`、`notify` 和 `notifyAll`）转化为不同的对象，
通过将它们与使用任意 `Lock` 实现相结合，达到每个对象有多个等待集的效果。其中 `Lock` 代替了同步方法和语句的使用，
`Condition` 代替了对象监控方法的使用。

`Condition`（也称为条件队列或条件变量）提供了一种方法，使一个线程可以暂停执行（"等待"），
直到被另一个线程通知现在某个状态条件可能为真。由于对这种共享状态信息的访问发生在不同的线程中，所以必须对其进行保护，
因此，某种形式的 `Lock` 与 `Condition` 相关联。等待条件提供的关键属性是，它原子性地释放关联的锁，
并暂停当前线程，就像 `Object.wait` 一样。

一个 `Condition` 实例本质上是绑定在锁上的。要获得一个特定 `Lock` 实例的 `Condition` 实例，可以使用 `Lock.newCondition()` 方法。

举个例子，假设我们有一个的缓冲区，它支持 `put` 和 `take` 方法。如果在一个空的缓冲区上尝试 `take`，那么线程将阻塞，
直到缓冲区有新的条目；如果在一个满的缓冲区上尝试 `put`，那么线程将阻塞，直到腾出一个空间。
我们希望将等待的 `put` 线程和 `take` 线程分别保持在不同的等待集中，这样我们就可以使用当缓冲区中的条目或空间变得可用时，
每次只通知单个线程的优化方式。这可以使用两个 `Condition` 实例来实现。
```java
class BoundedBuffer {
    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition(); 
    final Condition notEmpty = lock.newCondition(); 
  
    final Object[] items = new Object[100];
    int putptr, takeptr, count;
  
    public void put(Object x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)
                notFull.waiting();
            items[putptr] = x;
            if (++putptr == items.length)
                putptr = 0;
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
  
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)
                notEmpty.await();
            Object x = items[takeptr];
            if (++takeptr == items.length) 
                takeptr = 0;
            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}
```
注意：`java.util.concurrent.ArrayBlockingQueue`类提供了这个功能。

一个 `Condition` 实现可以提供与对象监控方法不同的行为和语义，例如保证通知的顺序，或者在执行通知时不要求持有锁。
如果一个实现提供了这样的专门语义，那么该实现必须在文档中记录这些语义。

请注意，`Condition` 实例只是普通的对象，它本身可以作为同步语句中的目标，并且可以有自己的监控等待和通知方法被调用。
获取一个 `Condition` 实例的监控锁，或者使用它的监控方法，与获取与该 `Condition` 相关联的 `Lock`、使用它的等待和通知方法没有指定的关系。
为了避免混淆，建议你永远不要以这种方式使用 `Condition` 实例，除非可能在它们自己的实现中使用。

除非有说明，任何参数传递一个空值都会导致 `NullPointerException` 被抛出。

当等待一个 `Condition` 时，一般来说，允许发生 "虚假唤醒"，作为对底层平台语义的一种让步。
虚假唤醒就是使用 `if` 进行等待条件的判断，而通常情况下无论哪一个线程抢到了资源, 另一个线程的唤醒就可以被认为是没有必要的, 
也就是被虚假唤醒了。

这对大多数应用程序没有什么实际影响，因为 `Condition` 应该总是在循环中被等待，测试被等待的状态。
一个实现可以自由地消除虚假唤醒的可能性，但建议应用程序员总是假设它们可能发生，所以应该总是在循环中等待。

条件等待的三种形式(可中断、非中断和定时)在某些平台上的实现难易程度和性能特点可能有所不同。特别是，
提供这些特性并保持特定的语义（如排序保证）可能是困难的。此外，中断线程的能力可能并不总是在所有平台上都能实现。
因此，不要求一个实现为所有三种形式的等待定义完全相同的保证或语义，也不要求它支持中断线程的实际暂停。

一个实现需要清楚地记录每个等待方法提供的语义和保证，当一个实现确实支持线程暂停时的中断时，那么它必须服从这个接口中定义的中断语义。
由于中断一般意味着取消，而对中断的检查往往是不频繁的，因此，一个实现可以倾向于响应中断而不是正常的方法返回。
即使可以证明中断发生在可能已经解除线程阻塞的另一个操作之后，也是如此。实现应该在文档中记录这种行为。

# 1. 方法

## 1.1 await
```java
/*
导致当前线程等待，直到它被 signal 或中断。

与该 Condition 相关联的锁会被原子式地释放，当前线程出于线程调度的目的而被禁用，并处于休眠状态，直到四种情况之一发生。
 - 其他线程调用该 Condition 的 signal 方法，而当前线程恰好被选为被唤醒的线程；
 - 其他线程为调用这个 Condition 的 signalAll方法
 - 其他一些线程中断当前线程，并且锁支持中断线程
 - 发生 "虚假唤醒"。
在所有情况下，在这个方法返回之前，当前线程必须重新获取与这个 Condition 相关的 Lock。当线程返回时，保证它能持有这个锁。

如果当前线程：
 - 在进入本方法时设置了中断状态；
 - 在等待时被中断。
则会抛出 InterruptedException，并清除当前线程的中断状态。在第一种情况下，没有规定是否在释放锁之前进行中断测试。

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。

一个实现可以偏重于响应中断而不是响应 signal。在这种情况下，实现必须确保信号被重定向到另一个等待的线程，如果有的话。
*/
void await() throws InterruptedException;
```

## 1.2 awaitUninterruptibly
```java
/*
使得当前线程等待，直到它被接收到 signal。

与该条件相关联的锁会被原子式地释放，当前线程出于线程调度的目的而被禁用，并处于休眠状态，直到三种情况之一发生。
 - 其他线程调用该 Condition 的 signal 方法，而当前线程恰好被选为被唤醒的线程；
 - 其他线程为这个 Condition 调用 signalAll 方法；
 - 发生 "虚假唤醒"。
在所有情况下，在这个方法返回之前，当前线程必须重新获取与这个条件相关的锁。当线程返回时，保证它能持有这个锁。

如果当前线程在进入这个方法时，它的中断状态已经被设置，或者它在等待时被中断，它将继续等待，直到收到信号。
当它最终从这个方法返回时，它的中断状态仍然会被设置。

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。
*/
void awaitUninterruptibly();
```

## 1.3 awaitNanos
```java
/*
使得当前线程等待，直到它收到信号或被中断，或指定的等待时间过去。

与该条件相关联的锁会被原子式地释放，当前线程会因为线程调度的目的而被禁用，并处于休眠状态，直到五种情况之一发生。
 - 其他线程调用该 Condition 的 signal 方法，而当前线程恰好被选为被唤醒的线程；
 - 其他线程为这个 Condition 调用 signalAll 方法；
 - 其他一些线程中断当前线程，并且锁支持中断线程
 - 指定的等待时间已过；
 - 发生 "虚假唤醒"。
在所有情况下，在这个方法返回之前，当前线程必须重新获取与这个条件相关的锁。当线程返回时，保证它能持有这个锁。

如果当前线程：
 - 在进入本方法时设置了中断状态；
 - 在等待时被中断。
则会抛出 InterruptedException，并清除当前线程的中断状态。在第一种情况下，没有规定是否在释放锁之前进行中断测试。

该方法返回 nanosTimeout 值，表示一个剩余等待纳秒数的估计值，如果超时，则返回一个小于或等于零的值。
这个值可以用来决定在等待返回但等待条件仍然不成立的情况下是否要重新等待以及等待的时间。
这个方法的典型用法有以下几种形式：
 boolean aMethod(long timeout, TimeUnit unit) {
   long nanos = unit.toNanos(timeout);
   lock.lock();
   try {
     while (!conditionBeingWaitedFor()) {
       if (nanos <= 0L)
         return false。
       nanos = theCondition.awaitNanos(nanos);
     }
     // ...
   } finally {
     lock.unlock();
   }
 }

该方法需要一个纳秒参数，以避免报告剩余时间时出现截断错误。这样的精度损失将使程序员难以确保在发生重新等待时，
总的等待时间不会系统性地短于指定时间。

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。

一个实现可以偏重于响应中断而不是响应 signal。在这种情况下，实现必须确保信号被重定向到另一个等待的线程，如果有的话。

@return nanosTimeout 的估计值减去从本方法返回时的等待时间。如果大于 0 可以作为后续调用本方法的参数，
以完成所需时间的等待；小于等于零的值表示没有剩余时间。
*/
long awaitNanos(long nanosTimeout) throws InterruptedException;

/*
使当前线程等待，直到收到信号或被中断，或指定的等待时间结束。这个方法在行为上等同于
  awaitNanos(unit.toNanos(time)) > 0
*/
boolean await(long time, TimeUnit unit) throws InterruptedException;
```

## 1.4 awaitUntil
```java
/*
导致当前线程等待，直到它收到信号或被中断，或者指定的最后期限过去。
与该条件相关联的锁会被原子式地释放，当前线程会因为线程调度的目的而被禁用，并处于休眠状态，直到五种情况之一发生。
与该条件相关联的锁会被原子式地释放，当前线程会因为线程调度的目的而被禁用，并处于休眠状态，直到五种情况之一发生。
 - 其他线程调用该 Condition 的 signal 方法，而当前线程恰好被选为被唤醒的线程；
 - 其他线程为这个 Condition 调用 signalAll 方法；
 - 其他一些线程中断当前线程，并且锁支持中断线程
 - 指定的期限已过；
 - 发生 "虚假唤醒"。
在所有情况下，在这个方法返回之前，当前线程必须重新获取与这个条件相关的锁。当线程返回时，保证它能持有这个锁。

如果当前线程：
 - 在进入本方法时设置了中断状态；
 - 在等待时被中断。
则会抛出 InterruptedException，并清除当前线程的中断状态。在第一种情况下，没有规定是否在释放锁之前进行中断测试。

返回值表示是否已经过了deadline，可以按如下方式使用。  
 boolean aMethod(Date deadline) {
   boolean stillWaiting = true;
   lock.lock();
   try {
     while (!conditionBeingWaitedFor()) {
       if (!stillWaiting)
         return false;
       stillWaiting = theCondition.awaitUntil(deadline);
     }
     // ...
   } finally {
     lock.unlock();
   }
 }

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。

实现可以偏向于响应中断而不是响应信号，或者偏向于到达截止日期。无论哪种情况，
实现都必须确保信号被重定向到另一个等待的线程（如果有的话）。

@return 如果最后期限已过，则为 false，否则为 true。
*/
boolean awaitUntil(Date deadline) throws InterruptedException;
```

## 1.5 signal
```java
/*
唤醒一个等待的线程。如果有任何线程在此 Condition 下等待，则选择一个线程进行唤醒。
然后该线程必须在从等待中返回之前重新获取锁。

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。
*/
void signal();
```

## 1.6 signalAll
```java
/*
唤醒所有等待的线程。如果有任何线程在等待这个 Condition，那么它们都会被唤醒。
每个线程在从等待中返回之前必须重新获取锁。

当这个方法被调用时，假设当前线程持有与这个 Condition 相关的锁。如果不是，通常情况下，
会抛出一个异常（如 IllegalMonitorStateException），实现必须记录具体的实施行为。
*/
void signalAll();
```