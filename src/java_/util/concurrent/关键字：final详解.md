# 1. final 的基础使用<sup id="a1">[\[1\]](#f1)</sup>

## 1.1 修饰类

当某个类的整体定义为 `final` 时，就表明了你不能打算继承该类，而且也不允许别人这么做。即这个类是不能有子类的。
注意：`final` 类中的所有方法都隐式为 `final`，因为无法覆盖他们，所以在 `final` 类中给任何方法添加 `final` 关键字是没有任何意义的。

## 1.2 修饰方法

注意以下三点：
 - `private` 方法是隐式的 `final`。
 - `final` 方法不能被重写。
 - `final` 方法是可以被重载的。

## 1.3 修饰参数

Java 允许在参数列表中以声明的方式将参数指明为 `final`，这意味这你无法在方法中更改参数引用所指向的对象。
这个特性主要用来向匿名内部类传递数据。

## 1.4 修饰变量

常规的用法比较简单，这里通过下面三个问题进一步说明。

### 1.4.1 所有的 final 修饰的字段都是编译期常量吗?

现在来看看编译期常量和非编译期常量, 如：
```java
public class Test {
    //编译期常量
    final int i = 1;
    final static int J = 1;
    final int[] a = {1,2,3,4};
    //非编译期常量
    Random r = new Random();
    final int k = r.nextInt();

    public static void main(String[] args) {

    }
}
```

k 的值由随机数对象决定，所以不是所有的 `final` 修饰的字段都是编译期常量，只是k的值在被初始化后无法被更改。

### 1.4.2 static final

一个既是 `static` 又是 `final` 的字段只占据一段不能改变的存储空间，它必须在定义的时候进行赋值，否则编译器将不予通过。

### 1.4.3 blank final

Java 允许生成空白 `final`，也就是说被声明为 `final` 但又没有给出定值的字段。
但是必须在该字段被使用之前被赋值，也就是在构造器里面。

# 2. final 域重排序规则

在 Java 内存模型中我们知道 Java 内存模型为了能让处理器和编译器底层发挥他们的最大优势，对底层的约束就很少，
也就是说针对底层来说 Java 内存模型就是弱内存数据模型。同时，处理器和编译为了性能优化会对指令序列有编译器和处理器重排序。
那么，在多线程情况下，`final` 会进行怎样的重排序? 会导致线程安全的问题吗? 下面，就来看看 `final` 的重排序。

## 2.1 final 域为基本类型

先看一段示例性的代码：
```java
public class FinalDemo {
    private int a;                      // 普通域
    private final int b;                // final域
    private static FinalDemo finalDemo;

    public FinalDemo() {
        a = 1;                          // 1. 写普通域
        b = 2;                          // 2. 写final域
    }

    public static void writer() {
        finalDemo = new FinalDemo();
    }

    public static void reader() {
        FinalDemo demo = finalDemo;     // 3.读对象引用
        int a = demo.a;                 // 4.读普通域
        int b = demo.b;                 // 5.读final域
    }
}
```

假设线程 A 在执行 `writer()` 方法，线程 B 执行 `reader()` 方法。

### 2.1.1 写 final 域重排序规则

写 `final` 域的重排序规则禁止对 `final` 域的写重排序到构造函数之外，这个规则的实现主要包含了两个方面：
 - JMM 禁止编译器把 `final` 域的写重排序到构造函数之外；
 - 编译器会在 `final` 域写之后，构造函数返回之前，插入一个 StoreStore 屏障。
 这个屏障可以禁止处理器把 `final` 域的写重排序到构造函数之外。

我们再来分析 `writer` 方法，虽然只有一行代码，但实际上做了两件事情：
 - 构造了一个 `FinalDemo` 对象；
 - 把这个对象赋值给成员变量 `finalDemo`。

我们来画下存在的一种可能执行时序图，如下：

![写 final 变量时序图][write-reorder]

由于 `a,b` 之间没有数据依赖性，普通域(普通变量) `a` 可能会被重排序到构造函数之外，线程 B 就有可能读到的是普通变量 `a` 初始化之前的值(零值)，
这样就可能出现错误。而 `final` 域变量 `b`，根据重排序规则，会禁止 `final` 修饰的变量 `b` 重排序到构造函数之外，
从而 `b` 能够正确赋值，线程 B 就能够读到 `final` 变量初始化后的值。

因此，写 `final` 域的重排序规则可以确保：在对象引用为任意线程可见之前，对象的 `final` 域已经被正确初始化过了，
而普通域就不具有这个保障。比如在上例，线程 B 中得到的有可能就是一个未正确初始化的对象 `finalDemo`。

### 2.1.2 读 final 域重排序规则

读 `final` 域重排序规则为：**在一个线程中，初次读对象引用和初次读该对象包含的 `final` 域，JMM 会禁止这两个操作的重排序**。

处理器会在读 `final` 域操作的前面插入一个 LoadLoad 屏障。实际上，读对象的引用和读该对象的 `final` 域存在间接依赖性，
一般处理器不会重排序这两个操作。但是有一些处理器会重排序，因此，这条禁止重排序规则就是针对这些处理器而设定的。

`read()` 方法主要包含了三个操作：
 - 初次读引用变量 `finalDemo`;
 - 初次读引用变量 `finalDemo` 的普通域 `a`;
 - 初次读引用变量 `finalDemo` 的 `final` 与 `b`。

假设线程 A 写过程没有重排序，那么线程 A 和线程 B 有一种的可能执行时序为下图：

![final 读过程重排序][read-reorder]

读对象的普通域被重排序到了读对象引用的前面，就会出现线程 B 还未读到对象引用就在读取该对象的普通域变量，这显然是错误的操作。
而 `final` 域的读操作就“限定”了在读 `final` 域变量前已经读到了该对象的引用，从而就可以避免这种情况。

读 `final` 域的重排序规则可以确保：在读一个对象的 `final` 域之前，一定会先读这个包含这个 `final` 域的对象的引用。

## 2.2 final 域为引用类型

### 2.2.1 对 final 修饰的对象的成员域写操作

针对引用数据类型，`final` 域写针对编译器和处理器重排序增加了这样的约束：在构造函数内对一个 `final` 修饰的对象的成员域的写入，
与随后在构造函数之外把这个被构造的对象的引用赋给一个引用变量，这两个操作是不能被重排序的。

注意这里的是“增加”也就说前面对 `final` 基本数据类型的重排序规则在这里还是使用。下面来看一个例子：
```java
public class FinalReferenceDemo {
    final int[] arrays;
    private FinalReferenceDemo finalReferenceDemo;

    public FinalReferenceDemo() {
        arrays = new int[1];                            //1
        arrays[0] = 1;                                  //2
    }

    public void writerOne() {
        finalReferenceDemo = new FinalReferenceDemo();  //3
    }

    public void writerTwo() {
        arrays[0] = 2;                                  //4
    }

    public void reader() {
        if (finalReferenceDemo != null) {               //5
            int temp = finalReferenceDemo.arrays[0];    //6
        }
    }
}
```

针对上面的实例程序，线程 A 执行 `writerOne` 方法，执行完后线程 B 执行 `writerTwo` 方法，然后线程 C 执行 `reader` 方法。
下图就以这种执行时序出现的一种情况来讨论：

![final 引用写重排序][reference-write-reorder]

由于对 `final` 域的写禁止重排序到构造方法外，因此 1 和 3 不能被重排序。
由于一个 `final` 域的引用对象的成员域写入不能与随后将这个被构造出来的对象赋给引用变量重排序，因此 2 和 3 不能重排序。

### 2.2.2 对 final 修饰的对象的成员域读操作

JMM 可以确保线程 C 至少能看到写线程 A 对 `final` 引用的对象的成员域的写入，即能看到 `arrays[0] = 1`，
而写线程 B 对数组元素的写入可能看到可能看不到。

JMM 不保证线程 B 的写入对线程 C 可见，线程 B 和线程 C 之间存在数据竞争，此时的结果是不可预知的。
如果想要是可见的，可使用锁或者 `volatile`。

## 2.3 关于 final 重排序的总结

按照 `final` 修饰的数据类型分类： 
 - 基本数据类型: 
    - `final` 域写：禁止 `final` 域写重排序到构造方法之外，从而保证该对象对所有线程可见时，该对象的 `final` 域全部已经初始化过。
    - `final` 域读：禁止初次读对象的引用与读该对象包含的 `final` 域的重排序。
 - 引用数据类型： 
    - 额外增加约束：禁止在构造函数对一个 `final` 修饰的对象的成员域的写入与随后将这个被构造的对象的引用赋值给引用变量重排序。

# 3. final 深入理解

## 3.1 final 的实现原理

上面我们提到过，写 `final` 域会要求编译器在 `final` 域写之后，构造函数返回前插入一个 StoreStore 屏障。
读 `final` 域的重排序规则会要求编译器在读 `final` 域的操作前插入一个 LoadLoad 屏障。

很有意思的是，x86 处理器不会对写-写重排序，所以 StoreStore 屏障可以省略。由于不会对有间接依赖性的操作重排序，
所以在 x86 处理器中，读 `final` 域需要的 LoadLoad 屏障也会被省略掉。也就是说，以 x86 为例的话，
对 `final` 域的读/写的内存屏障都会被省略！

所以具体是否插入内存屏障还是得看是什么处理器。

## 3.2 对象引用在构造函数中“溢出”

上面对 `final` 域写重排序规则可以确保我们在使用一个对象引用的时候该对象的 `final` 域已经在构造函数被初始化过了。
但是这里其实是有一个前提条件：**在构造函数，不能让这个被构造的对象被其他线程可见，也就是说该对象引用不能在构造函数中“逸出”**。

以下面的例子来说：
```java
public class FinalReferenceEscapeDemo {
    private final int a;
    private FinalReferenceEscapeDemo referenceDemo;

    public FinalReferenceEscapeDemo() {
        a = 1;                              //1
        referenceDemo = this;               //2
    }

    public void writer() {
        new FinalReferenceEscapeDemo();
    }

    public void reader() {
        if (referenceDemo != null) {        //3
            int temp = referenceDemo.a;     //4
        }
    }
}
```

可能的执行时序如图所示：

![引用溢出][overflow]

假设一个线程 A 执行 `writer` 方法，另一个线程 B 执行 `reader` 方法。因为构造函数中操作 1 和 2 之间没有数据依赖性，
1 和 2 可以重排序。先执行了 2，这个时候引用对象 `referenceDemo` 是个没有完全初始化的对象，
而当线程 B 去读取该对象时就会出错。

尽管依然满足了 `final` 域写重排序规则，但是引用对象“this”逸出，该代码依然存在线程安全的问题。

## 3.3 一个有趣的现象

看下面的代码：
```java
byte a = 1, b = 2;
byte c = a + b;  // 报错，因为 Java 会将 a、b 算术运算的结果转化为 int，所以必须强转才行

final byte a = 1, b = 2;
byte c = a + b;  // 没有问题，因为常量折叠的缘故
```
参见[常量折叠][constant-fold]。


[constant-fold]: ../../lang/常量折叠.md

[write-reorder]: ../../../../res/img/final-write-reorder.png
[read-reorder]: ../../../../res/img/final-read-reorder.png
[reference-write-reorder]: ../../../../res/img/final-reference-write-reorder.png
[overflow]: ../../../../res/img/final-overflow.png

<b id="f1">\[1\]</b> https://www.pdai.tech/md/java/thread/java-thread-x-key-final.html [↩](#a1)  