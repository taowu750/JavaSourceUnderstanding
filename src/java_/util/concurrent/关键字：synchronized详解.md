# 1. synchronized 的使用

`synchronized` 的使用方法可以参见 [Java并发-线程基础.md][thread-base] 第 6.1 节 synchronized。

在应用 `synchronized` 关键字时需要把握如下注意点：
 - 它是互斥锁，一把锁只能同时被一个线程获取，没有获得锁的线程只能等待。
 - 每个实例都对应有自己的一把锁(`this`),不同实例之间的锁互不影响。此外，锁对象是 *.class 以及 `synchronized` 修饰的是 `static` 方法的时候，
 使用的是类对象锁，每个类只有一个。
 - `synchronized` 修饰的方法，无论方法正常执行完毕还是抛出异常，都会释放锁。

# 2. 实现原理

JVM 基于进入和退出 `Monitor`（对象监视器）对象来实现方法同步和代码块同步。在 Java 虚拟机(HotSpot)中，`Monitor` 是使用 C++ 实现的，
由 `ObjectMonitor` 类实现。每个 Java 对象中都内置了一个 `ObjectMonitor` 对象。

另外，`wait/notify` 等方法也依赖于 `Monitor` 对象，这就是为什么只有在同步的块或者方法中才能调用 `wait/notify` 等方法，
否则会抛出 `java.lang.IllegalMonitorStateException` 的异常的原因。

方法同步和代码块同步两者的实现细节不一样：
 - 代码块同步: 通过使用 `monitorenter` 和 `monitorexit` 指令实现的.
 - 同步方法: 通过访问控制符 `ACC_SYNCHRONIZED` 修饰。

## 2.1 反编译代码

为了证明 JVM 的实现方式, 下面通过反编译代码来证明：
```java
public class Demo {

    public void f1() {
        synchronized (Demo.class) {
            System.out.println("Hello World.");
        }
    }

    public synchronized void f2() {
        System.out.println("Hello World.");
    }

}
```
反编译之后的字节码如下(只摘取了方法的字节码):
```java
public void f1();
  descriptor: ()V
  flags: ACC_PUBLIC
  Code:
    stack=2, locals=3, args_size=1
       0: ldc           #2                  // class me/snail/base/Demo
       2: dup
       3: astore_1
       4: monitorenter
       5: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
       8: ldc           #4                  // String Hello World.
      10: invokevirtual #5                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      13: aload_1
      14: monitorexit
      15: goto          23
      18: astore_2
      19: aload_1
      20: monitorexit
      21: aload_2
      22: athrow
      23: return
    Exception table:
       from    to  target type
           5    15    18   any
          18    21    18   any
    LineNumberTable:
      line 6: 0
      line 7: 5
      line 8: 13
      line 9: 23
    StackMapTable: number_of_entries = 2
      frame_type = 255 /* full_frame */
        offset_delta = 18
        locals = [ class me/snail/base/Demo, class java/lang/Object ]
        stack = [ class java/lang/Throwable ]
      frame_type = 250 /* chop */
        offset_delta = 4

public synchronized void f2();
  descriptor: ()V
  flags: ACC_PUBLIC, ACC_SYNCHRONIZED
  Code:
    stack=2, locals=1, args_size=1
       0: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
       3: ldc           #4                  // String Hello World.
       5: invokevirtual #5                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
       8: return
    LineNumberTable:
      line 12: 0
      line 13: 8
}
```

可以看到，在 `f1()` 方法中，发现其中一个 `monitorenter` 对应了两个 `monitorexit`, 这让人迷惑。但是仔细看 `#15: goto` 语句, 
直接跳转到了 `#23: return` 处, 再看 `#22: athrow` 语句发现, 原来第二个 `monitorexit` 是保证同步代码块抛出异常时锁能得到正确的释放而存在的。

`f2()` 方法有一个 `ACC_SYNCHRONIZED` 的标志，因此不需要 `monitorenter` 和 `monitorexit` 指令。

## 2.2 monitorenter 和 monitorexit

`monitorenter` 和 `monitorexit` 指令，会让对象在执行同步代码块时，使其锁计数器加 1 或者减 1。
每一个对象在同一时间只与一个 `Monitor` 相关联，而一个 `Monitor` 在同一时间只能被一个线程获得，
一个线程在尝试获得与这个对象相关联的 `Monitor` 锁的所有权的时候，`monitorenter`会发生如下 3 种情况之一：
 - `Monitor` 计数器为 0，意味着目前还没有被获得，那这个线程就会立刻获得锁，然后把计数器加 1。一旦加 1，别的线程就需要等待。
 - 如果线程已经拿到了这个锁的所有权，又重入了这把锁，那锁计数器就会累加，变成 2，并且随着重入的次数，会一直累加。
 - 这把锁已经被别的线程获取了，当前线程会等待锁释放。

`monitorexit` 指令：释放对于 `Monitor` 的所有权。释放过程很简单，就是将 `Monitor` 的计数器减 1。如果减完以后，计数器不是 0，
则代表刚才是重入进来的，当前线程还继续持有这把锁的所有权。如果计数器变成 0，则代表当前线程不再拥有该 `Monitor` 的所有权，就会释放锁。

下图表现了对象，对象监视器，同步队列以及执行线程状态之间的关系：

![锁的原理][monitor]

## 2.3 保证可见性的原理：happens-before 规则

`synchronized` 的 happens-before 规则，即监视器锁规则：对同一个监视器的解锁，happens-before 于对该监视器的加锁。
继续来看代码：
```java
public class MonitorDemo {
    private int a = 0;

    public synchronized void writer() {     // 1
        a++;                                // 2
    }                                       // 3

    public synchronized void reader() {    // 4
        int i = a;                         // 5
    }                                      // 6
}
```

该代码的 happens-before 关系如图所示：

![happens-before 关系][happens-before]

在图中每一个箭头连接的两个节点就代表之间的 happens-before 关系，其中：
 - 黑色的是根据程序顺序规则推导出来的。
 - 红色的为监视器锁规则推导而出的，线程 A 释放锁 happens-before 线程 B 加锁。
 - 蓝色的则是通过传递性规则推到而出的。

于是，由 2 happens-before 5 关系可知线程 A 的执行结果对线程 B 可见，即线程 B 所读取到的 a 的值为 1。

# 3. JVM 中锁的优化

简单来说在 JVM 中 `monitorenter` 和 `monitorexit` 字节码依赖于底层的操作系统的 Mutex Lock 来实现的，
但是由于使用 Mutex Lock 需要将当前线程挂起并从用户态切换到内核态来执行，这种切换的代价是非常昂贵的。
然而在现实中的大部分情况下，同步方法是运行在单线程环境(无锁竞争环境)，如果每次都调用 Mutex Lock 那么将严重的影响程序的性能。
这种锁是重量级锁。

不过在 JDK1.6 中对锁的实现引入了大量的优化，如锁粗化(Lock Coarsening)、锁消除(Lock Elimination)、适应性自旋(Adaptive Spinning)、
偏向锁(Biased Locking)、轻量级锁(Lightweight Locking)等技术来减少锁操作的开销。

## 3.1 Java 对象头(存储锁类型)

在 HotSpot 虚拟机中, 对象在内存中的布局分为三块区域: 对象头, 实例数据和对齐填充.

对象头中包含两部分: MarkWord 和类型指针。如果是数组对象的话, 对象头还有一部分是存储数组的长度。

多线程下 `synchronized` 的加锁就是对同一个对象的对象头中的 `MarkWord` 中的 bit 区域进行 CAS 操作。

其中：
 - Mark Word 用于存储对象自身的运行时数据, 如 HashCode, GC 分代年龄, 锁状态标志, 线程持有的锁, 偏向线程 ID 等等。
 占用内存大小与虚拟机位长一致：32 位 JVM 的 MarkWord 是 32位, 64 位 JVM 的 MarkWord 是 64 位。
 - 类型指针指向对象的类元数据, 虚拟机通过这个指针确定该对象是哪个类的实例。

下表列出了对象头的内容：

| 长度 | 内容 | 说明 |
| ---- | --- | ---- |
| 32/64 bit | MarkWord | 存储对象的 hashCode 或锁信息等 |
| 32/64 bit | Class Metadata Address | 存储对象类型数据的指针 |
| 32/64 bit | Array Length | 数组的长度(如果当前对象是数组) |

如果是数组对象的话, 虚拟机用 3 个字宽(32/64bit + 32/64bit + 32/64bit)存储对象头; 如果是普通对象的话, 
虚拟机用 2 字宽存储对象头(32/64bit + 32/64bit)。

## 3.2 锁的类型和对象头

在 Java SE 1.6 里 `synchronized` 同步锁，一共有四种状态：无锁、偏向锁、轻量级锁、重量级锁，它会随着竞争情况逐渐升级。
**锁可以升级（又叫锁膨胀）但是不可以降级**，目的是为了提高获取锁和释放锁的效率。

下面看一下每个锁状态时, 对象头中的 MarkWord 这一个字节中的内容是什么。以 32 位为例：

### 3.2.1 无锁状态

|     25 bit      |    4 bit     | 1 bit(是否是偏向锁) | 2 bit(锁标志位) |
| :-------------: | :----------: | :-----------------: | :-------------: |
| 对象的 hashCode | 对象分代年龄 |          0          |       01        |

### 3.2.2 偏向锁状态

| 23 bit  | 2 Bit |    4 bit     | 1 bit(是否是偏向锁) | 2 bit(锁标志位) |
| :-----: | :---: | :----------: | :-----------------: | :-------------: |
| 线程 ID | epoch | 对象分代年龄 |          1          |       01        |

### 3.2.3 轻量级锁状态

|        30 bit        | 2 bit |
| :------------------: | :---: |
| 指向栈中锁记录的指针 |  00   |

### 3.2.4 重量级锁状态

|           30 bit           | 2 bit |
| :------------------------: | :---: |
| 指向堆中重量级锁(Monitor)的指针 |  10   |

### 3.2.4 指针压缩

我们可以看到，指针只有 30 bit，当地址是 32 位的，那么 30 位是怎么存的下 32 位的指针呢？
这涉及到 Java 的[指针压缩][pointer-compress]技术了。通过指针压缩，30 位的地址可以映射 32 位的地址空间。

## 3.3 锁粗化

锁粗话就是减少不必要的连在一起的 unlock、lock 操作，将多个连续的锁扩展成一个范围更大的锁。
例如：
```java
public void doSomethingMethod() {
    synchronized(lock){
        //do some thing
    }
    //这是还有一些代码，做其它不需要同步的工作，但能很快执行完毕
    synchronized(lock){
        //do other thing
    }
}

// 锁粗化
public void doSomethingMethod() {
    //进行锁粗化：整合成一次锁请求、同步、释放
    synchronized(lock){
        //do some thing
        //做其它不需要同步但能很快执行完的工作
        //do other thing
    }
}
```
注意这样做是有前提的，就是中间不需要同步的代码能够很快速地完成，如果不需要同步的代码需要花很长时间，
就会导致同步块的执行需要花费很长的时间，这样做也就不合理了。

另一种需要锁粗化的情况是：
```java
for(int i = 0; i < size; i++) {
    synchronized(lock) {
        // ...
    }
}

// 锁粗化
synchronized(lock){
    for(int i = 0; i < size; i++) {
    }
}
```

## 3.4 锁消除

锁消除就是通过运行时 JIT 编译器的逃逸分析来消除一些没有在当前同步块以外被其他线程共享的数据的锁保护。
通过逃逸分析也可以在线程本地栈上进行对象空间的分配(同时还可以减少 Heap 上的垃圾收集开销)。

例如，`StringBuffer.append` 方法使用了 `synchronized` 关键词，它是线程安全的。
但我们可能仅在线程内部把 `StringBuffer` 当作局部变量使用：
```java
public static String createStringBuffer(String str1, String str2) {
    StringBuffer sBuf = new StringBuffer();
    sBuf.append(str1);// append 方法是同步操作
    sBuf.append(str2);
    return sBuf.toString();
}
```
此时的 `append` 方法若是使用同步操作，就是白白浪费的系统资源。

这时我们可以通过编译器将其优化，将锁消除。前提是 Java 必须运行在 server 模式（server 模式会比 client 模式作更多的优化），
同时必须开启逃逸分析:
```
-server -XX:+DoEscapeAnalysis -XX:+EliminateLocks
```
其中 `+DoEscapeAnalysis` 表示开启逃逸分析，`+EliminateLocks` 表示锁消除。

锁削除的主要判定依据来源于逃逸分析的数据支持，如果判断到一段代码中，在堆上的所有数据都不会逃逸出去被其他线程访问到，
那就可以把它们当作栈上数据对待，认为它们是线程私有的，同步加锁自然就无须进行。 

也许读者会有疑问，变量是否逃逸，对于虚拟机来说需要使用数据流分析来确定，但是程序员自己应该是很清楚的，
怎么会在明知道不存在数据争用的情况下要求同步呢？答案是有许多同步措施并不是程序员自己加入的，而是使用的类中存在的。

## 3.5 自旋锁与自适应自旋锁

### 3.5.1 自旋锁

在没有加入锁优化时，`synchronized` 是一个非常“胖大”的家伙。在多线程竞争锁时，当一个线程获取锁时，
它会阻塞所有正在竞争的线程，这样对性能带来了极大的影响。在挂起线程和恢复线程的操作都需要转入内核态中完成，
这些操作对系统的并发性能带来了很大的压力。

同时 HotSpot 团队注意到在很多情况下，共享数据的锁定状态只会持续很短的一段时间，为了这段时间去挂起和回复阻塞线程并不值得。
在如今多处理器环境下，完全可以让另一个没有获取到锁的线程在门外等待一会(自旋)，但不放弃 CPU 的执行时间。
等待持有锁的线程是否很快就会释放锁。为了让线程等待，我们只需要让线程执行一个**忙循环(自旋)**，这便是自旋锁由来的原因。

自旋锁，是指当一个线程在获取锁的时候，如果锁已经被其它线程获取，那么该线程将循环等待，然后不断的判断锁是否能够被成功获取，
直到获取到锁才会退出循环。自旋锁不会使线程状态发生切换，一直处于用户态，即线程一直都是活动的，不会使线程进入阻塞状态，
减少了不必要的上下文切换，执行速度快。

自旋锁早在 JDK1.4 中就引入了，只是当时默认时关闭的。在 JDK 1.6 后默认为开启状态。先不考虑其对多处理器的要求，
如果锁占用的时间非常的短，那么自旋锁的性能会非常的好，相反，其会带来更多的性能开销(因为在线程自旋时，始终会占用 CPU 的时间片，
如果锁占用的时间太长，那么自旋的线程会白白消耗掉 CPU 资源)。
**因此自旋等待的时间必须要有一定的限度，如果自旋超过了限定的次数仍然没有成功获取到锁，就应该使用传统的方式去挂起线程了**。

在 JDK 定义中，自旋锁默认的自旋次数为 10 次，用户可以使用参数 `-XX:PreBlockSpin` 来更改。
可是现在又出现了一个问题：如果锁在线程自旋刚结束时就被释放了，就会使用传统的方式去挂起线程，那么是不是有点得不偿失。
所以这时候我们需要更加聪明的锁来实现更加灵活的自旋。来提高并发的性能。

### 3.5.2 适应性自旋锁

在 JDK 1.6 中引入了自适应自旋锁。这就意味着自旋的时间不再固定了，而是由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定的。

如果在同一个锁对象上，自旋等待刚刚成功获取过锁，并且持有锁的线程正在运行中，那么 JVM 会认为该锁自旋获取到锁的可能性很大，
会自动增加等待时间。相反，如果对于某个锁，自旋很少成功获取锁，那再以后要获取这个锁时将可能省略掉自旋过程，
以避免浪费处理器资源。有了自适应自旋，JVM 对程序的锁的状态预测会越来越准确，JVM 也会越来越聪明。

## 3.6 偏向锁

在大多实际环境下，锁不仅不存在多线程竞争，而且总是由同一个线程多次获取，那么在同一个线程反复获取所释放锁中，
其中并没有锁的竞争，那么这样看上去，多次的获取锁和释放锁带来了很多不必要的性能开销和上下文切换。

为了解决这一问题，HotSpot 的作者在 Java SE 1.6 中对 `synchronized` 进行了优化，引入了偏向锁。
偏向锁，顾名思义，它会偏向于第一个访问锁的线程，线程获得锁之后就不会再有解锁等操作了, 这样可以省略很多开销。
假如有两个及以上线程来竞争该锁的话, 那么偏向锁就失效了, 进而升级成轻量级锁了。

### 3.6.1 偏向锁的加锁

首先需要知道<strong>全局安全点（safepoint）</strong>的概念。简单来说就是其代表了一个状态，在该状态下所有线程都是暂停的，
又叫做“stop the word”。

1. 无锁状态下 Mark Word 的一个比特位用于标识该对象偏向锁是否被使用或者是否被禁止。如果该 bit 位为 0，则该对象未被锁定，
   并且禁止偏向；如果该 bit 位为 1，则意味着该对象处于以下三种状态：
    - **匿名偏向(Anonymously biased)**：在此状态下线程 ID 为 0，意味着还没有线程偏向于这个锁对象。
    第一个试图获取该锁的线程将会遇到这个情况，使用原子 CAS 指令可将该偏向锁绑定于当前线程。
     这是允许偏向锁的类对象的初始状态。
    - **可重偏向(Rebiasable)**：在此状态下，偏向锁的 epoch 字段是无效的（“批量重偏向/撤销”一节会讲），
    下一个试图获取锁对象的线程将会面临这个情况。使用原子 CAS 指令可将该偏向锁绑定于当前线程。在批量重偏向的操作中，
     未被持有的锁对象都被至于这个状态，以便允许被快速重偏向。
    - **已偏向(Biased)**：这种状态下，线程 ID 非空，且 epoch 为有效值——意味着其他线程正在使用这个锁对象。
2. 如果处于匿名偏向或可重偏向状态，则可执行同步代码。
3. 否则如果处于已偏向状态，则表示有第二个线程访问这个对象。因为偏向锁不会主动释放，所以第二个线程可以看到对象的偏向状态，
这时表明在这个对象上已经存在竞争了，此时参见“偏向锁的撤销”一节。

偏向锁有以下几点需要注意：
 - 当一个对象已经计算过 identity hash code，它就无法进入偏向锁状态。HotSpot VM 是假定“实际上只有很少对象会计算 identity hash code”来做优化的。
 - 当一个对象当前正处于偏向锁状态，并且需要计算其 identity hash code 的话，则它的偏向锁会被撤销，并且锁会升级。
 - 重量锁的实现中，`ObjectMonitor` 类里有字段可以记录非加锁状态下的 Mark Word，其中可以存储 identity hash code 的值。
 或者简单说就是重量锁可以存下 identity hash code。

### 3.6.2 偏向锁的撤销

偏向锁的撤销在上述第 3 步骤中有提到。**偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，线程不会主动去释放偏向锁**。
**偏向锁的撤销，需要等待全局安全点**，它会首先暂停拥有偏向锁的线程 A，然后线程 A 的状态：
 - A 线程已经不再存活了，此时就会直接撤销偏向锁，将对象回复成无锁状态，然后重新偏向。
 - A 线程还在同步代码块中，此时将 A 线程的偏向锁升级为轻量级锁。具体怎么升级的看下面的“轻量级锁的加锁”一节。
 - 最后线程 A 如果还活着，则唤醒它。

### 3.6.3 批量重偏向/撤销

对于存在明显多线程竞争的场景下使用偏向锁是不合适的，比如生产者-消费者队列。生产者线程获得了偏向锁，消费者线程再去获得锁的时候，
就涉及到这个偏向锁的撤销操作，而这个撤销是比较昂贵的，需要等到全局安全点时将偏向锁撤销为无锁状态或升级为轻量级/重量级锁。
那么怎么判断这些对象是否适合偏向锁呢？

JVM 采用以类为单位的做法。HotSpot 为所有加载的类型，在 `Class` 元数据——`InstanceKlass` 中保留了一个 Mark Word 原型
——`mark_prototype`。这个值的 `bias` 位域决定了该类型的对象是否允许被偏向锁定。与此同时，
当前的 `epoch` 位也被保留在 `prototype` 中。这意味着，对应 `Class` 的新创建对象可以简单地直接拷贝这个原型值，
对象的 Mark Word 里中就会有 `bias` 和 `epoch`。

下面是两个概念的解释：
 - **批量重偏向(bulk rebias)**：如果一个类的大量对象被一个线程 T1 执行了同步操作，也就是大量对象先偏向了 T1。T1 同步结束后，
 另一个线程 T2 也将这些对象作为锁对象进行操作，会导致偏向锁指向 T2，也就是重偏向的操作。
 - **批量撤销(bulk revoke)**：当一个偏向锁的撤销次数到达阈值(40)的时候就认为这个对象设计的有问题；
 那么 JVM 会把它的类的所有的对象都撤销偏向锁，并且这样新实例化的对象也是不可偏向的。

在批量重偏向的操作中，`prototype` 的 `epoch` 位将会被更新，也就是加 1；在批量撤销的操作中，
`prototype` 将会被置成不可偏向的状态，`bias` 位被置 0。

下面给出新的获取偏向锁的步骤：
1. 验证对象的 bias 位:  
   如果是 0，则该对象不可偏向，应该使用轻量级锁算法。
2. 验证对象所属 `InstanceKlass` 的 `prototype` 的 `bias` 位:  
   确认 `prototype` 的 `bias` 为是否为 1。如果为 0，则该类所有对象全部不允许被偏向锁定，并且该类所有对象的 `bias` 位都需要被设为 0，
   使用轻量级锁替换。
3. 校验 `epoch` 位:  
   校验对象的 Mark Word 的 `epoch` 位是否与该对象所属 `InstanceKlass` 的 `prototype` 中 Mark Word 的 `epoch` 匹配。
   如果不匹配，则表明偏向已过期，需要重新偏向。这种情况，偏向线程可以简单地使用原子 CAS 指令重新偏向于这个锁对象。
4. 校验 owner 线程:  
   比较偏向线程 ID 与当前线程 ID。如果匹配，则表明当前线程已经获得了偏向，可以安全返回。否则处于已偏向状态，
   则表示有第二个线程访问这个对象，参见“偏向锁的撤销”。

### 3.6.4 开启/关闭偏向锁

偏向锁在 Java 6 及更高版本中是默认启用的, 但是它在程序启动几秒钟后才激活。即使偏向锁的特性被打开，
出于性能（启动时间）的原因在 JVM 启动后的的头几秒钟这个特性是被禁止的。这也意味着在此期间，
`prototype` 的 Mark Word 中 `bias` 位被设置为 0，以禁止实例化的对象被偏向。几秒钟之后，
所有的`prototype` 的 Mark Word 中 `bias` 位被设置为 1，如此新的对象就可以被偏向锁定了。

我们能可以使用 `-XX:BiasedLockingStartupDelay=0` 来关闭偏向锁的启动延迟。也可以使用 `-XX:-UseBiasedLocking=false` 来关闭偏向锁, 
那么程序会直接进入轻量级锁状态。

## 3.7 轻量级锁

在 JDK 1.6 之后引入的轻量级锁，是对在大多数情况下同步块并不会有竞争出现提出的一种优化。
它可以减少重量级锁对线程的阻塞带来的线程开销。从而提高并发性能。

### 3.7.1 轻量级锁的加锁

1. 在代码进入同步块的时候，如果同步对象锁状态为偏向状态（就是锁标志位为“01”，是否为偏向锁标志位为“1”），
   虚拟机首先将在当前线程的栈帧中建立一个名为<strong>锁记录（Lock Record）</strong>的空间，用于存储锁对象目前的 Mark Word 的拷贝。
   官方称之为 Displaced Mark Word。这时候线程堆栈与对象头的状态如图所示：  
    ![轻量级锁线程栈帧][lightweight-before]
2. 拷贝对象头中的 Mark Word 复制到锁记录中。
3. 拷贝成功后，虚拟机将使用 CAS 操作尝试将对象头的 Mark Word 更新为指向 Lock Record 的指针，
并将 Lock record 里的 owner 指针指向对象头的 mark word 。如果更新成功，则执行步骤 4，否则执行步骤 5。
4. 如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象 Mark Word 的锁标志位设置为“00”，
   即表示此对象处于轻量级锁定状态，这时候线程堆栈与对象头的状态如下所示：  
    ![轻量级锁锁定状态][lightweight-lock]
5. 如果这个更新操作失败了，虚拟机首先会检查对象的 Mark Word 是否指向当前线程的栈帧，如果有，说明该锁已经被获取，可以直接调用；
如果不是说明这个锁对象已经被其他线程抢占了，说明此时有多个线程竞争锁，那么它便尝试使用自旋来获取锁。如果一定次数后仍未获得锁对象，
或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁膨胀为重量级锁。重量级锁使除了拥有锁的线程以外的线程都阻塞，防止 CPU 空转。

### 3.7.2 轻量级锁的解锁过程

1. 通过 CAS 操作尝试把线程栈中的 Displaced Mark Word 替换到同步对象的 Mark Word 中去。
2. 如果替换成功，整个同步过程就结束了。
3. 如果替换失败，说明有其他线程尝试过获取该锁，表示当前锁存在竞争，锁就会膨胀成重量级锁。

## 3.8 锁升级过程图示

![锁升级过程][lock-upgrade]

## 3.9 几种锁的比较

|  锁 | 应用场景 | 优点 | 缺点 |
| --- | ------- | ---- | --- |
| 偏向锁 | 只有一个线程进入临界区。 | 加锁和解锁不需要 CAS 操作，没有额外的性能消耗，和执行非同步方法相比仅存在纳秒级的差距 | 如果线程间存在锁竞争，会带来额外的锁撤销的消耗 |
| 轻量级锁 | 多个线程交替进入临界区，追求响应时间，同步块执行时间短。 | 竞争的线程不会阻塞，提高了响应速度。 | 如线程始终得不到锁竞争的线程，使用自旋会消耗 CPU 性能 |
| 重量级锁 | 多个线程同时进入临界区，追求吞吐量，同步块执行时间长。 | 线程竞争不使用自旋，不会消耗 CPU | 线程阻塞，响应时间缓慢，在多线程下，频繁的获取释放锁，会带来巨大的性能消耗 |


[thread-base]: Java并发-线程基础.md
[pointer-compress]: Java指针压缩.md

[monitor]: ../../../../res/img/sync-monitor.png
[happens-before]: ../../../../res/img/sync-happens-before.png
[lightweight-before]: ../../../../res/img/sync-lightweight-before.png
[lightweight-lock]: ../../../../res/img/sync-lightweight-lock.png
[lock-upgrade]: ../../../../res/img/sync-lock-upgrade.png