# 1. finalize 与对象再生

通过`finalize`方法，我们可以使得本应该被回收的对象再次“复活”（参见 [Object.md][object] 第 1.6 节 finalize）。
下面是一个例子：
```java
public class Zombie {
  static Zombie zombie;
 
  public void finalize() {
    // 在 finalize 中恢复对象引用
    zombie = this;
  }
}
```

# 2. finalize 攻击<sup id="a1">[\[1\]](#f1)</sup>

## 2.1 隐患

此代码的一种存在更大隐患的版本甚至允许恢复部分构造的对象。即使对象在初始化过程中不能通过正确性检查，
其仍能够被`finalize`创建出来，如下所示：
```java
public class Zombie {
  static Zombie zombie;
  int value;
 
  public Zombie(int value) {
    if(value < 0) {
      throw new IllegalArgumentException("Negative Zombie2 value");
    }
    this.value = value;
  }

  public void finalize() {
    zombie = this;
  }

  @Override
  public String toString() {
      return "Zombie{" +
              "value=" + value +
              '}';
  }
}
```
在上面的例子中，如果传递不正确的`value`参数，会导致抛出异常，对象创建失败，对象会被垃圾回收器回收。
而`finalize`方法的存在使得能够保存这个部分初始化对象，详细代码参见[FinalizeAttackTest.java][test]。

## 2.2 攻击原理

`finalize`攻击就是通过继承类，重载`finalize`方法，在此方法内保存对象。这个对象是部分初始化对象，它本应该被清除。
但是因为`finalize`得关系，导致它已经初始化的信息泄漏，并且获取对象后可以调用其方法。如果对象包含一些操作敏感信息的方法，
就会被不法分子利用。

假设下面是一个包含敏感操作的类：
```java
public class SensitiveOperation {
 
    public SensitiveOperation(){
        // 进行检查
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
    }
 
    private boolean doSecurityCheck(){
        return false;
    }
 
    // 敏感操作
    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
}
```
我们创建一个子类去继承它，并且重写`finalize`方法：
```java
public class SensitiveOperationFinalizer extends SensitiveOperation {
 
    public SensitiveOperationFinalizer(){
    }
 
    @Override
    protected void finalize() {
        // 执行敏感操作
        System.out.println("We can still do store Money action!");
        this.storeMoney();
    }
}
```
接下来我们就可以进行`finalize`攻击：
```java
try {
    SensitiveOperation sensitiveOperation = new SensitiveOperationFinalizer();
    sensitiveOperation.storeMoney();
}catch (Exception e){
    System.out.println(e.getMessage());
}
System.gc();
System.runFinalization();
```
> 输出:  
> Security check failed!
> We can still do store Money action!  
> Store 1000000 RMB!

可以看到，虽然我们构造函数抛出了异常，但是`storeMoney`的操作还是被执行了！详细代码参见[FinalizeAttackTest.java][test]。

## 2.3 解决 finalize 攻击

### 2.3.1 使用 final 类

如果使用`final`类，那么类是不能够被继承的，问题自然就解决了。
```java
public final class SensitiveOperationFinal {
 
    public SensitiveOperationFinal(){
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
    }
 
    private boolean doSecurityCheck(){
        return false;
    }
 
    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
}
```

### 2.3.2 使用 final finalize 方法

在父类中将`finalize`方法定义为`final`，禁止子类重写`finalize`方法，也可以解决这个问题。
```java
public final class SensitiveOperationFinal {
 
    public SensitiveOperationFinal(){
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
    }
 
    private boolean doSecurityCheck(){
        return false;
    }
 
    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
    
    final protected void finalize() {
    }
}
```

### 2.3.3 使用标志变量

我们可以在对象构建完毕的时候设置一个`flag`变量，然后在每次安全操作的时候都去判断一下这个`flag`变量，
这样也可以避免之前提到的问题。
```java
public class SensitiveOperationFlag {
 
    private volatile boolean initialized = false;
 
    public SensitiveOperationFlag(){
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
        initialized = true;
    }
 
    private boolean doSecurityCheck(){
        return false;
    }
 
    public void storeMoney(){
        if(!initialized) {
            throw new SecurityException("Object is not initiated yet!");
        }
        System.out.println("Store 1000000 RMB!");
    }
}
```
注意，这里`initialized`需要设置为`volatile`，只有这样才能保证构造函数在`initialized`设值之前执行。
这是利用了`volatile`禁止指令重排序的特性。详细代码参见[FinalizeAttackTest.java][test]。

### 2.3.4 使用 this 或 super

在`JDK6`或者更高版本中，如果对象的构造函数在`java.lang.Object`构造函数退出之前引发异常，则`JVM`将不会执行该对象的`finalize`方法。
`Java`确保`java.lang.Object`构造函数在任何构造函数的第一条语句之前执行。
如果构造函数中的第一个语句是对超类的构造函数或同一个类中的另一个构造函数的调用，则`java.lang.Object`构造函数将在此调用中的某个位置执行。
否则，`Java`将在该构造函数的代码中的任何一行代码之前执行超类的默认构造函数，并且隐式调用`java.lang.Object`构造函数。

也就是说如果异常发生在构造函数中的第一条`this`或者`super`中的时候，`JVM`将不会调用对象的`finalize`方法：
```java
public class SensitiveOperationThis {
 
    public SensitiveOperationThis(){
        this(doSecurityCheck());
    }
 
    private SensitiveOperationThis(boolean secure) {
    }
 
    private static boolean doSecurityCheck(){
         throw new SecurityException("Security check failed!");
    }
 
    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
}
```
详细代码参见[FinalizeAttackTest.java][test]。


[object]: Object.md
[test]: ../../../test/java_/lang/FinalizeAttackTest.java

<b id="f1">\[1\]</b> 参考 https://blog.csdn.net/weixin_42765516/article/details/108342208 [↩](#a1)