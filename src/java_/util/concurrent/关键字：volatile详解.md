# 1. volatile 的作用

## 1.1 保证可见性

可见性是指可见性：一个线程对共享变量的修改，另外一个线程能够立刻看到。

我们先看下面一个示例：
```java
public class VolatileDemo {
    public static boolean finishFlag = false;

    public static void main(String[] args) throws InterruptedException {
        new Thread(()->{
            int i = 0;
            while (!finishFlag){
                i++;
            }
        },"t1").start();

        //确保 t1 先进入 while 循环后主线程才修改 finishFlag
        Thread.sleep(1000);
        finishFlag = true;
    }
}
```
这里运行之后 t1 线程中的 `while` 循环是停不下来的，因为我们是在主线程修改了 `finishFlag` 的值，而此值对 t1 线程不可见。
如果我们把变量 `finishFlag` 加上 `volatile` 修饰:
```java
public static volatile boolean finishFlag = false;
```
这时候再去运行就会发现 `while` 循环很快就可以停下来了。

## 1.2 保证有序性

有序性即程序执行的顺序按照代码的先后顺序执行。重排序会破坏有序性，而 `volatile` 能防止对所修饰变量进行指令重排序。

这方面最好的例子是 `volatile` 实现[线程安全的单例模式][singleton]。

## 1.3 保证原子性(单次读写)

原子性，即一个操作或者多个操作，要么全部执行并且执行的过程不会被任何因素打断，要么就都不执行

对于原子性，需要强调一点，也是大家容易误解的一点：对 `volatile` 变量的单次读/写操作可以保证原子性的，
但是并不能保证 `i++` 这种操作的原子性，因为本质上 `i++` 是读、写两次操作。
```java
public class VolatileTest01 {
    volatile int i;

    public void addI() {
        i++;
    }

    public static void main(String[] args) throws InterruptedException {
        final  VolatileTest01 test01 = new VolatileTest01();
        for (int n = 0; n < 1000; n++) {
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                test01.addI();
            }).start();
        }
        //等待10秒，保证上面程序执行完成
        Thread.sleep(10000);
        System.out.println(test01.i);
    }
}
```
运行的结果本应该是 1000，但多次运行会发现每次的值都不一样。原因也很简单，`i++` 其实是一个复合操作，包括三步骤：
1. 读取 `i` 的值。
2. 对 `i` 加 1。
3. 将 `i` 的值写回内存。
   
`volatile` 是无法保证这三个操作是具有原子性的，我们可以通过 `AtomicInteger` 或者 `synchronized` 来保证 +1 操作的原子性。
注：上面几段代码中多处执行了 `Thread.sleep()` 方法，目的是为了增加并发问题的产生几率，无其他作用。

### 1.3.1 防止字分裂

在 32 位虚拟机上，JVM 被允许将 64 位的 `long` 和 `double` 变量的读写作为两个单独的 32 位操作执行。
这增加了在读写过程中发生上下文切换的可能性，因此其他任务会看到不正确的结果。这被称为 **Word tearing （字分裂）**。

而使用 `volatile` 修饰变量，能保证任何情况下对 `long` 和 `double` 的单次读/写操作都具有原子性。

目前各种平台下的 64 位的商用虚拟机都选择把 64 位数据的读写操作作为原子操作来对待，
因此我们在编写代码时一般不把 `long` 和 `double` 变量专门声明为 `volatile` 多数情况下也是不会错的。

# 2. volatile 实现原理

## 2.1 volatile 可见性实现

下面是一个简单的代码：
```java
public class Test {
    private volatile int a;

    public void update() {
        a = 1;
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.update();
    }
}
```
通过 hsdis 和 jitwatch 工具可以得到编译后的汇编代码:
```assembly
0x000000000295158c: lock cmpxchg %rdi,(%rdx)  # 在 volatile 修饰的共享变量进行写操作的时候会多出 lock 前缀的指令
```

`lock` 前缀的指令在多核处理器下会引发两件事情:
 - 将当前处理器缓存行的数据写回到系统内存。
 - 写回内存的操作会使在其他 CPU 里缓存了该内存地址的数据无效。

`lock` 是一种控制指令，在多处理器环境下，`lock` 汇编指令可以基于总线锁或者缓存锁的机制来达到可见性的一个效果。

## 2.2 可见性的本质

### 2.2.1 硬件层面

线程是 CPU 调度的最小单元，线程设计的目的最终仍然是更充分的利用计算机处理的效能，
但是绝大部分的运算任务不能只依靠处理器“计算”就能完成，处理器还需要与内存交互，比如读取运算数据、存储运算结果，
这个 I/O 操作是很难消除的。

而由于计算机的存储设备与处理器的运算速度差距非常大，所以现代计算机系统都会增加一层读写速度尽可能接近处理器运算速度的高速缓存来作为内存和处理器之间的缓冲：
将运算需要使用的数据复制到缓存中，让运算能快速进行，当运算结束后再从缓存同步到内存之中。
查看我们个人电脑的配置可以看到，CPU 有 L1,L2,L3 三级缓存,大致粗略的结构如下图所示:

![CPU 三级缓存][cache]

从上图可以知道，L1 和 L2 缓存为各个 CPU 独有，而有了高速缓存的存在以后，每个 CPU 的处理过程是，
先将计算需要用到的数据缓存在 CPU 高速缓存中，在 CPU 进行计算时，直接从高速缓存中读取数据并且在计算完成之后写入到缓存中。
在整个运算过程完成后，再把缓存中的数据同步到主内存。

由于在多 CPU 中，每个线程可能会运行在不同的 CPU 内，并且每个线程拥有自己的高速缓存。同一份数据可能会被缓存到多个 CPU 中，
如果在不同 CPU 中运行的不同线程看到同一份内存的缓存值不一样就会存在缓存不一致的问题，那么怎么解决缓存一致性问题呢？
CPU 层面提供了两种解决方法：**总线锁和缓存锁**。

### 2.2.2 总线锁

总线锁，简单来说就是，在多 CPU下，当其中一个处理器要对共享内存进行操作的时候，在总线上发出一个 `LOCK#` 信号，
这个信号使得其他处理器无法通过总线来访问到共享内存中的数据，总线锁定把 CPU 和内存之间的通信锁住了(CPU 和内存之间通过总线进行通讯)，
这使得锁定期间，其他处理器不能操作其他内存地址的数据。

然而这种做法的代价显然太大，那么如何优化呢？优化的办法就是降低锁的粒度，所以 CPU 就引入了缓存锁。

### 2.2.3 缓存锁

缓存锁的核心机制是基于**缓存一致性协议**来实现的，一个处理器的缓存回写到内存会导致其他处理器的缓存无效，
IA-32 处理器和 Intel 64 处理器使用 **MESI** 实现缓存一致性协议(注意，缓存一致性协议不仅仅是通过 MESI 实现的，
不同处理器实现了不同的缓存一致性协议)。

#### 2.2.3.1 MESI（缓存一致性协议）

缓存是分段(line)的，一个段对应一块存储空间，称之为**缓存行**，它是 CPU 缓存中可分配的最小存储单元，
大小 32 字节、64 字节、128 字节不等，这与 CPU 架构有关，通常来说是 64 字节。
`LOCK#` 因为锁总线效率太低，因此使用了多组缓存。为了使其行为看起来如同一组缓存那样。因而设计了缓存一致性协议。

缓存一致性协议有多种，但是日常处理的大多数计算机设备都属于**嗅探(snooping)协议**。
所有内存的传输都发生在一条共享的总线上，而所有的处理器都能看到这条总线。

缓存本身是独立的，但是内存是共享资源，所有的内存访问都要经过**仲裁(同一个指令周期中，只有一个 CPU 缓存可以读写内存)**。
CPU 缓存不仅仅在做内存传输的时候才与总线打交道，而是不停在嗅探总线上发生的数据交换，跟踪其他缓存在做什么。
当一个缓存代表它所属的处理器去读写内存时，其它处理器都会得到通知，它们以此来使自己的缓存保持同步。
只要某个处理器写内存，其它处理器马上知道这块内存在它们的缓存段中已经失效。

MESI 是一种比较常用的缓存一致性协议，MESI 表示缓存行的四种状态，分别是：
1. **M(Modify)**: 表示共享数据只缓存在当前 CPU 缓存中，并且是被修改状态，也就是缓存的数据和主内存中的数据不一致
2. **E(Exclusive)**: 表示缓存的独占状态，数据只缓存在当前CPU缓存中，并且没有被修改
3. **S(Shared)**: 表示数据可能被多个 CPU 缓存，并且各个缓存中的数据和主内存数据一致
4. **I(Invalid)**: 表示缓存已经失效

在 MESI 协议中，每个缓存的缓存控制器不仅知道自己的读写操作，而且也监听(snoop)其它 CPU 的读写操作。
对于 MESI 协议，从 CPU 读写角度来说会遵循以下原则：
 - CPU 读请求：缓存处于 M、E、S 状态都可以被读取，I 状态 CPU 只能从主存中读取数据。
 - CPU 写请求：缓存处于 M、E 状态才可以被写。对于 S 状态的写，需要将其他 CPU 中缓存行置为无效才行。

使用总线锁和缓存锁机制之后，CPU 对于内存的操作大概可以抽象成下面这样的结构。从而达到缓存一致性效果：

![CPU 内存抽象][cpu]

## 2.3 volatile 有序性实现

### 2.3.1 volatile 的 happens-before 关系

happens-before 规则中有一条是 `volatile` 变量规则：对一个 `volatile` 域的写，happens-before 于任意后续对这个 `volatile` 域的读。

```java
//假设线程A执行writer方法，线程B执行reader方法
class VolatileExample {
    int a = 0;
    volatile boolean flag = false;
    
    public void writer() {
        a = 1;                  // 1 线程A修改共享变量
        flag = true;            // 2 线程A写volatile变量
    } 
    
    public void reader() {
        if (flag) {             // 3 线程B读同一个volatile变量
            int i = a;          // 4 线程B读共享变量
            // ……
        }
    }
}
```

根据 happens-before 规则，上面过程会建立 3 类 happens-before 关系。 
 - 根据程序次序规则：1 happens-before 2 且 3 happens-before 4。
 - 根据 volatile 规则：2 happens-before 3。
 - 根据 happens-before 的传递性规则：1 happens-before 4。

![happens-before 顺序][happens-before]

因为以上规则，当线程 A 将 `volatile` 变量 `flag` 更改为 `true` 后，线程 B 能够迅速感知。

### 2.3.2 volatile 禁止重排序

volatile 禁止重排序的特性基于**内存屏障(Memory Barrier)**。内存屏障，又称内存栅栏，是一个 CPU 指令。
在程序运行时，为了提高执行性能，编译器和处理器会对指令进行重排序，JMM 为了保证在不同的编译器和 CPU 上有相同的结果，
通过插入特定类型的内存屏障来禁止特定类型的编译器重排序和处理器重排序。插入一条内存屏障会告诉编译器和 CPU：
不管什么指令都不能和这条 Memory Barrier 指令重排序。

JMM 会针对编译器制定 `volatile` 重排序规则表，"NO" 表示禁止重排序：

![volatile 重排序规则表][reorder]

为了实现 `volatile` 内存语义时，编译器在生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。
对于编译器来说，发现一个最优布置来最小化插入屏障的总数几乎是不可能的，为此，JMM 采取了保守的策略：
 - 在每个 `volatile` 写操作的前面插入一个 StoreStore 屏障。
 - 在每个 `volatile` 写操作的后面插入一个 StoreLoad 屏障。
 - 在每个 `volatile` 读操作的后面插入一个 LoadLoad 屏障。
 - 在每个 `volatile` 读操作的后面插入一个 LoadStore 屏障。

`volatile` 写是在前面和后面分别插入内存屏障，而 `volatile` 读操作是在后面插入两个内存屏障。

| 内存屏障 | 说明 |
| ------- | ---- |
| StoreStore 屏障 | 禁止上面的普通写和下面的 volatile 写重排序。 |
| StoreLoad 屏障 | 防止上面的 volatile 写与下面可能有的 volatile 读/写重排序。 |
| LoadLoad 屏障 | 禁止下面所有的普通读操作和上面的 volatile 读重排序。 |
| LoadStore 屏障 | 禁止下面所有的普通写操作和上面的 volatile 读重排序。 |

![volatile 写屏障][write]

![volatile 读屏障][read]


[singleton]: ../../../设计模式/单例模式.md

[cache]: ../../../../res/img/volatile-cache.png
[cpu]: ../../../../res/img/volatile-cpu.png
[happens-before]: ../../../../res/img/volatile-happpens-before.png
[reorder]: ../../../../res/img/volatile-reorder.png
[write]: ../../../../res/img/volatile-write.png
[read]: ../../../../res/img/volatile-read.png