# 1. 引言

在`String.trim`方法中，可以看到一个有意思的注释：
```java
public String trim() {
    int len = value.length;
    int st = 0;
    char[] val = value;    /* avoid getfield opcode */

    while ((st < len) && (val[st] <= ' ')) {
        st++;
    }
    while ((st < len) && (val[len - 1] <= ' ')) {
        len--;
    }
    return ((st > 0) || (len < value.length)) ? substring(st, len) : this;
}
```
在`avoid getfield opcode`处，我们可以看到将`String`的`value`成员字段赋值给了局部变量，为了避免`getfield`这个操作码的执行。
`getfield`是`JVM`的操作码，它的作用是获取指定类的实例域，并将其值压入到栈顶。

# 2. 例子

为了弄懂这个问题，我们需要从字节码的角度分析，下面我们来看一个例子<sup id="a1">[\[1\]](#f1)</sup>：
```java
public class Main {
 
    public char[] chars = new char[10];
 
    public void test() {
        System.out.println(chars[0]);
        System.out.println(chars[1]);
        System.out.println(chars[2]);
    }
 
    public static void main(String args[]) {
        Main m = new Main();
        m.test();
    }
}
```
执行`javap -c Main`，分析生成的字节码：

![Main 字节码][multi-call]

我们分析以下`test`方法下的字节码：
1. 4: 获取指定的实例域，并将其值压入栈顶
2. 7: 将 int 值 0 推送至栈顶
3. 8: 将 char 数组指定索引的值推至栈顶
4. 9: 调用实例方法

总结一下的话，就是每次获取实例域，推到栈顶，然后推被操作的索引到栈顶，然后取到对应数组的指定索引的值推到栈顶，
然后就是调用输出方法了。可以看到输出三次，`getfield`操作码就调用了三次，假想我们在遍历这个`char`数组，
那就要频繁调用`getfield`操作码了。

# 3. 避免 getfield 频繁调用

学习`String`中的做法，我们可以避免`getfield`的频繁调用：
```java
public class Main {
 
    public char[] chars = new char[10];
 
    public void test() {
        // 使用局部变量引用 this.chars
        char[] chars = this.chars;
        System.out.println(chars[0]);
        System.out.println(chars[1]);
        System.out.println(chars[2]);
    }
 
    public static void main(String args[]) {
        Main m = new Main();
        m.test();
    }
}
```
执行`javap -c Main`，分析生成的字节码：

![优化 Main][one-call]

我们还是分析一下`test`方法中的字节码：
1. 0: 将第一个引用类型本地变量推送至栈顶（就是将局部变量引用`chars`放到栈顶）
2. 1: 获取实例域的引用推到栈顶（实例变量的成员`chars`放到栈顶）
3. 4: 将栈顶顶引用类型数值存入指定本地变量（就是把成员变量`chars`的赋值给给到局部变量）
4. 8: 将第二个引用类型本地变量推送至栈顶
5. 9: 将`int`值 0 推到栈顶
6. 10: 将`char`数组指定的索引的值压入到栈顶
7. 11: 调用实例方法（输出）

在 14-20 的过程中没有再发生`getfield`操作，而是用`aload_1`操作码将第二个本地引用（被赋值后的本地引用）推至栈顶，
就可以执行接下来的一系列操作。

到这里的话`avoid getfield opcode`的意思已经非常清楚明了，在遍历实例的`char`数组的时候，将实例数组的引用赋值给一个本地引用，
不需要频繁调用操作用码`getfield`，只需要在第一次对本地引用赋值的时候，调用一次`getfield`，接下来的遍历取值的时候，
只需要将本地引用压入到栈顶，这样就节省了开销。


[multi-call]: ../../../res/img/getfield-multi-call.png
[one-call]: ../../../res/img/getfield-one-call.png

<b id="f1">\[1\]</b> 参考 https://www.cnblogs.com/think-in-java/p/6130917.html [↩](#a1) 