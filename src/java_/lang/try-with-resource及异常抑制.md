# 1. 背景<sup id="a1">[\[1\]](#f1)</sup>

我们知道，在`Java`编程过程中，如果打开了外部资源（文件、数据库连接、网络连接等），我们必须在这些外部资源使用完毕后，
手动关闭它们。因为外部资源不由`JVM`管理，无法享用`JVM`的垃圾回收机制，如果我们不在编程时确保在正确的时机关闭外部资源，
就会导致外部资源泄露，紧接着就会出现文件被异常占用，数据库连接过多导致连接池溢出等诸多很严重的问题。

# 2. 传统的资源关闭方式

为了确保外部资源一定要被关闭，通常关闭代码被写入`finally`代码块中，当然我们还必须注意到关闭资源时可能抛出的异常，
于是便有了下面的经典代码：
```java
public static void main(String[] args) {
    FileInputStream inputStream = null;
    try {
        inputStream = new FileInputStream(new File("test"));
        System.out.println(inputStream.read());
    } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
    } finally {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // 注意，这种写法会导致异常丢失
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
```
熟悉其他语言的朋友可能会开始吐槽了，在`C++`中，我们可以把关闭资源的代码放在析构函数中，在`C#`中，我们有`using`代码块。
这些语法都有一个共同的特性，让外部资源的关闭行为与外部资源的句柄对象的生命周期关联，当外部资源的句柄对象生命周期终结时
（例如句柄对象已出作用域），外部资源的关闭行为将被自动调用。这样不仅更加符合面向对象的编程理念
（将关闭外部资源的行为内聚在外部资源的句柄对象中），也让代码更加简洁易懂。怎么到了`Java`这里，
就找不到自动关闭外部资源的语法特性了呢。

# 3. JDK7 及其之后的资源关闭方式

## 3.1 try-with-resource 语法

在`JDK7`以前，`Java`没有自动关闭外部资源的语法特性，直到`JDK7`中新增了`try-with-resource`语法，才实现了这一功能。
那什么是`try-with-resource`呢？简而言之，当一个外部资源的句柄对象（比如`FileInputStream`对象）实现了`AutoCloseable`接口，
那么就可以将上面的板式代码简化为如下形式：
```java
public static void main(String[] args) {
    try (FileInputStream inputStream = new FileInputStream(new File("test"))) {
        System.out.println(inputStream.read());
    } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
    }
}
```
将外部资源的句柄对象的创建放在`try`关键字后面的括号中，当这个`try-catch`代码块执行完毕后，`Java`会确保外部资源的`close`方法被调用。
代码瞬间简洁许多！

## 3.2 实现原理

`try-with-resource`并不是`JVM`虚拟机的新增功能，只是`JDK`实现了一个语法糖，当你将上面代码反编译后会发现，
其实对`JVM`虚拟机而言，它看到的依然是之前的写法：
```java
public static void main(String[] args) {
    try {
        FileInputStream inputStream = new FileInputStream(new File("test"));
        Throwable var2 = null;

        try {
            System.out.println(inputStream.read());
        } catch (Throwable var12) {
            var2 = var12;
            throw var12;
        } finally {
            if (inputStream != null) {
                if (var2 != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var11) {
                        // 将关闭异常添加到处理异常的“被抑制异常”列表里
                        var2.addSuppressed(var11);
                    }
                } else {
                    inputStream.close();
                }
            }

        }

    } catch (IOException var14) {
        throw new RuntimeException(var14.getMessage(), var14);
    }
}
```

通过反编译的代码，大家可能注意到代码中有一处对异常的特殊处理：
```java
var2.addSuppressed(var11);
```
这是`try-with-resource`语法涉及的另外一个知识点，叫做**异常抑制**。当对外部资源进行处理（例如读或写）时，如果遭遇了异常，
且在随后的关闭外部资源过程中，又遭遇了异常，那么你`catch`到的将会是对外部资源进行处理时遭遇的异常，
关闭资源时遭遇的异常将被“抑制”但不是丢弃，通过异常的`getSuppressed`方法，可以提取出被抑制的异常。

# 4. 总结

1. 当一个外部资源的句柄对象实现了`AutoCloseable`接口，`JDK7`中便可以利用`try-with-resource`语法更优雅的关闭资源，消除板式代码。
2. `try-with-resource`时，如果对外部资源的处理和对外部资源的关闭均遭遇了异常，“关闭异常”将被抑制，“处理异常”将被抛出，
但“关闭异常”并没有丢失，而是存放在“处理异常”的被抑制的异常列表中。


<b id="f1">\[1\]</b> 参考 https://www.cnblogs.com/itZhy/p/7636615.html [↩](#a1)