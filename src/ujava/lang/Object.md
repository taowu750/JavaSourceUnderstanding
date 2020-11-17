`java.lang.Object`类的声明如下：
```java
public class Object
```
这是`Java`中所有类的基类，任何类最终都会溯源到`Object`类。

## 1. 方法

### 1.1 equals
在Java规范中，它对equals()方法的使用必须要遵循如下几个规则：
1. 自反性：对于任何非空引用值 x，x.equals(x) 都应返回 true。
2. 对称性：对于任何非空引用值 x 和 y，当且仅当y.equals(x) 返回 true 时，x.equals(y) 才应返回 true。
3. 传递性：对于任何非空引用值 x、y 和z，如果 x.equals(y) 返回 true，并且 y.equals(z) 返回 true，那么 x.equals(z) 应返回 true。
4. 一致性：对于任何非空引用值 x 和 y，多次调用 x.equals(y) 始终返回 true 或始终返回false，前提是对象上 equals 比较中所用的信息没有被修改
5. 对于任何非空引用值 x，x.equals(null) 都应返回false。

`equals`方法编写建议：
1. 判断比较的两个对象引用是否相等，如果引用相等那么表示是同一个对象，那么当然相等
2. 如果`obj`为`null`，直接返回`false`，表示不相等
3. 比较`this`和`obj`是否是同一个类：如果`equals`的语义在每个子类中有所改变，就使用`getClass`检测；如果所有的子类都有统一的定义，那么使用`instanceof`检测
4. 将`obj`转换成对应的类类型变量`other`
5. 最后对对象的属性进行比较。使用 == 比较基本类型，使用`equals`比较对象。如果都相等则返回`true`，否则返回`false`。注意如果是在子类中定义`equals`，
则要包含`super.equals(other)`

一个标准的`equals`方法写法如下：
```java
@Override
public boolean equals(Object obj) {
    if (this == obj)
        return true;

    /*
     equals 语义在子类中未改变
    */
    // instanceof 包含了对 null 的检查
    if (!(obj instanceof Type)) {
        return false;
    }

    /*
     equals 语义在子类中已变
    */
    if(obj == null || getClass() != obj.getClass()){
        return false;
    }
    
    Type other = (Type) obj;

    return this 和 other 的比较结果
}
```

### 1.2 hashCode
默认的`hashCode`方法返回对象的地址（这依赖于`JVM`的实现）。如果我们希望对象用在`HashMap`、`HashSet`之类的容器中，
那么最好重写`hashCode`方法。哈希码的最佳实践在[ujava/lang/哈希码.md][hashCode]中给出。

需要注意的是，重写`equals`方法之后必须重写`hashCode`方法，原则如下：
1. 在程序执行期间，只要equals方法的比较操作用到的信息没有被修改，那么对这同一个对象调用多次，hashCode方法必须始终如一地返回同一个整数。
2. 如果两个对象根据equals方法比较是相等的，那么调用两个对象的hashCode方法必须返回相同的整数结果。
3. 如果两个对象根据equals方法比较是不等的，则hashCode方法不一定得返回不同的整数。

还需要注意，`hashCode`方法最好不要依赖对象中易变的字段。如果对象作为`HashMap`中的键，它的某个字段值一变，
那么用这个对象就不能访问原来在`HashMap`中的值了。如果需要依赖易变字段的话，那么保证该字段可以表示对象的类型，如姓名等。

### 1.3 clone
```java
protected native Object clone() throws CloneNotSupportedException
```
`clone`方法比较特殊。它是`protected`方法，而且如果对象没有实现`java.lang.Cloneable`接口的话，
调用这个方法将会将会抛出`java.lang.CloneNotSupportedException`。`Object`类没有实现`Cloneable`接口，
所有不能在`Object`上调用此方法。

子类重写`clone`方法如下：
```java
public class A implements Cloneable {

    private int a;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    @Override
    protected A clone() {
        try {
            return (A) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
```
通过`Java`的协变返回类型，我们可以在重写的`clone`方法中返回自身。注意，调用`super.clone()`时可以直接进行向下转型，
编译器和`JVM`会帮我们正确处理对象。

`clone`方法只是**浅复制**，这意味着当自定义类的字段的类型不是基本数据类型时，只会复制引用。要想达到**深拷贝**的效果，
必须保证字段类型全部为基本数据类型，或者引用字段是不可变对象，或者引用字段也实现了`clone`方法，
然后在自己的`clone`方法中调用字段的`clone`方法。例子如下所示：
```java
public class Professor implements Cloneable {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public Professor clone() {
        try {
            return (Professor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

public class Student implements Cloneable {
    private String name;
    private int age;
    private Professor professor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }
    
    @Override
    public Student clone() {
        try {
            Student newStudent = (Student) super.clone();
            newStudent.professor = professor.clone();
            return newStudent;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
```

### 1.4 notify
```java
public final native void notify();

public final native void notifyAll();
```
`notify()`方法能够唤醒一个正在等待这个对象的`monitor`（即锁）的线程，如果有多个线程都在等待这个对象的`monitor`，
则只能唤醒其中一个线程，具体是哪一个线程依赖于`JVM`实现，可以看成是随机。因此，当有多个线程等待`monitor`时，推荐使用`notifyAll`方法，
它会唤醒所有正在等待这个对象的`monitor`的线程。

为何这它们和`wait`方法不是`Thread`类声明中的方法，而是`Object`类中声明的方法（当然由于`Thread`类继承了`Object`类，
所以`Thread`也可以调用者三个方法）？其实这个问题很简单，由于每个对象都拥有`monitor`，所以让当前线程等待某个对象的锁，
当然应该通过这个对象来操作了。当前线程可能会等待多个线程的锁，如果通过线程来操作，情况就非常复杂了。

### 1.5 wait
```java
public final native void wait(long timeout) throws InterruptedException;

public final void wait(long timeout, int nanos) throws InterruptedException {
    if (timeout < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException("nanosecond timeout value out of range");
    }

    if (nanos > 0) {
        timeout++;
    }

    wait(timeout);
}

public final void wait() throws InterruptedException {
    wait(0);
}
```
调用某个对象的`wait()`方法能让当前线程阻塞，并且当前线程必须拥有此对象的`monitor`（即锁）。可以看到，它有三个重载，
`wait(timeout)`方法有一个超时时间，单位为毫秒。`wait(timeout, nanos)`虽然带有一个`nanos`(纳秒)参数，但是从代码中可以看出，
只有当`nanos`大于0的时候，给`timeout`加1，因此，这根本不能进行等待纳秒时间的操作。

需要注意的是，调用`wait`的方式需要如下所示：
```java
synchronized (obj) {
     while (<condition does not hold>)
        obj.wait(timeout, nanos);
     // Perform action appropriate to condition
}
```
使用`while`循环包围`wait`是因为：
1. 你可能有多个任务出于相同的条件在等待同一个锁，而第一个唤醒任务可能会改变这种状况（即使你没有这么做，
有人也会通过继承你的类去这么做）。如果属于这种情况，其他任务应该被再次挂起，直至条件发生变化。
2. 在这个任务从其`wait`中被唤醒的时刻，有可能会有某个其他的任务已经做出了改变，从而使得这个任务在此时不能执行，
或者执行其操作已无效。此时，应该通过再次调用`wait`来将其重新挂起。
3. 也有可能某些任务出于不同的条件在等待你的对象上的锁（在这种情况下必须使用`notifyAll`）。在这种情况下，
你需要检查是否已经由正确的原因唤醒，如果不是，就再次调用`wait`。

需要注意，检查条件的语句必须在同步块中，否则可能会发生信号错失的情况，从而导致死锁。

### 1.6 finalize
```java
protected void finalize() throws Throwable { }
```
`finalize`方法用于当虚拟机gc清理对象的时候调用，以做一些清理工作。但是不要依赖于这个方法去做一些释放/关闭资源之类的工作，
因为`finalize`方法的调用时机是不确定的，依赖于它将会产生随机或错误的行为。
`finalize`方法中一般用于释放非`Java`资源（如打开的文件资源、数据库连接等）,或是调用非`Java`方法（`native`方法）时分配的内存
（比如C语言的`malloc`系列函数）。

### 1.6.1 finalize 的问题
1. 一些与finalize相关的方法，由于一些致命的缺陷，已经被废弃了，如`System.runFinalizersOnExit()`方法、
`Runtime.runFinalizersOnExit()`方法
2. `System.gc()`与`System.runFinalization()`方法增加了`finalize`方法执行的机会，但不可盲目依赖它们
3. `Java`语言规范并不保证`finalize`方法会被及时地执行、而且根本不会保证它们会被执行
4. `finalize`方法可能会带来性能问题。因为`JVM`通常在单独的低优先级线程中完成`finalize`的执行
5. 对象再生问题：`finalize`方法中，可将待回收对象赋值给`GC Roots`可达的对象引用，从而达到对象再生的目的
6. `finalize`方法至多由`GC`执行一次(用户当然可以手动调用对象的`finalize`方法，但并不影响`GC`对`finalize`的行为)

利用`finalize`方法最多只会被调用一次的特性，我们可以[实现延长对象的生命周期][Object]:
```java
class User {
	
	public static User user = null;

	@Override
	protected void finalize() {
		System.out.println("User-->finalize()");
		user = this;
	}
}
```

### 1.6.2 finalize 的执行过程（生命周期）

大致描述一下`finalize`流程：当对象变成(GC Roots)不可达时，`GC`会判断该对象是否覆盖了`finalize`方法，若未覆盖，则直接将其回收。
否则，若对象未执行过`finalize`方法，将其放入`F-Queue`队列，由一低优先级线程执行该队列中对象的`finalize`方法。执行`finalize`方法完毕后，
`GC`会再次判断该对象是否可达，若不可达，则进行回收，否则，对象“复活”。

对象有两种状态，涉及到两类状态空间，一是**终结状态空间** F={unfinalized, finalizable, finalized}；二是**可达状态空间**
R={reachable, finalizer-reachable, unreachable}。各状态含义如下：
1. unfinalized: 新建对象会先进入此状态，`GC`并未准备执行其`finalize`方法，因为该对象是可达的
2. finalizable: 表示`GC`可对该对象执行`finalize`方法，`GC`已检测到该对象不可达。正如前面所述，
`GC`通过`F-Queue`队列和一专用线程完成`finalize`的执行
3. finalized: 表示`GC`已经对该对象执行过`finalize`方法
4. reachable: 表示`GC Roots`引用可达
5. finalizer-reachable(f-reachable)：表示不是`reachable`，但可通过某个`finalizable`对象可达
6. unreachable：对象不可通过上面两种途径可达

![状态变迁图][finalize]

变迁说明：
1. 新建对象首先处于\[reachable, unfinalized\]状态(A)
2. 随着程序的运行，一些引用关系会消失，导致状态变迁，从reachable状态变迁到f-reachable(B, C, D)或unreachable(E, F)状态
3. 若JVM检测到处于unfinalized状态的对象变成f-reachable或unreachable，`JVM`会将其标记为finalizable状态(G,H)。
若对象原处于\[unreachable, unfinalized\]状态，则同时将其标记为f-reachable(H)。
4. 在某个时刻，`JVM`取出某个finalizable对象，将其标记为finalized并在某个线程中执行其`finalize`方法。
由于是在活动线程中引用了该对象，该对象将变迁到(reachable, finalized)状态(K或J)。
该动作将影响某些其他对象从f-reachable状态重新回到reachable状态(L, M, N)
5. 处于finalizable状态的对象不能同时是unreahable的，由第4点可知，将对象finalizable对象标记为finalized时会由某个线程执行该对象的finalize方法，
致使其变成reachable。这也是图中只有八个状态点的原因
6. 程序员手动调用`finalize`方法并不会影响到上述内部标记的变化，因此`JVM`只会至多调用`finalize`一次，即使该对象“复活”也是如此。
程序员手动调用多少次不影响`JVM`的行为
7. 若`JVM`检测到finalized状态的对象变成unreachable，回收其内存(I)
8. 若对象并未覆盖`finalize`方法，`JVM`会进行优化，直接回收对象（O）
9. 注：`System.runFinalizersOnExit()`等方法可以使对象即使处于reachable状态，`JVM`仍对其执行`finalize`方法


[hashCode]: 哈希码.md
[Object]: ../../../test/ujava/lang/ObjectTest.java
[finalize]: finalize.jpg