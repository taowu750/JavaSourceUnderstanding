`java.lang.StackTraceElement`类的声明如下：
```java
public final class StackTraceElement implements java.io.Serializable
```
堆栈跟踪中的元素，由`Throwable.getStackTrace()`方法返回。每个元素代表一个栈帧。除堆栈顶部的帧以外，所有其他帧均表示方法调用。
堆栈顶部的帧表示生成堆栈跟踪的执行点。通常，这是创建与堆栈跟踪相对应的`Throwable`的点。

另请参见[Throwable.md][throwable]。

# 1. 成员字段
```java
// 下列字段通常由虚拟机初始化（public 构造器在 1.5 被添加）

// 当前栈帧元素表示的执行点的类的全限定名称
private String declaringClass;
// 表示的执行点的方法名
private String methodName;
// 表示的执行点的 Java 文件名
private String fileName;
// 表示的执行点的文件行号（从 1 开始）
private int    lineNumber;
```

# 2. 构造器
```java
public StackTraceElement(String declaringClass, String methodName,
                         String fileName, int lineNumber) {
    this.declaringClass = Objects.requireNonNull(declaringClass, "Declaring class is null");
    this.methodName     = Objects.requireNonNull(methodName, "Method name is null");
    this.fileName       = fileName;
    this.lineNumber     = lineNumber;
}
```

# 3. 方法

## 3.1 toString
```java
public String toString() {
    return getClassName() + "." + methodName +
        (isNativeMethod() ? "(Native Method)" :
         (fileName != null && lineNumber >= 0 ?
          "(" + fileName + ":" + lineNumber + ")" :
          (fileName != null ?  "("+fileName+")" : "(Unknown Source)")));
}
```

## 3.2 equals
```java
public boolean equals(Object obj) {
    if (obj==this)
        return true;
    if (!(obj instanceof StackTraceElement))
        return false;
    StackTraceElement e = (StackTraceElement)obj;
    return e.declaringClass.equals(declaringClass) &&
        e.lineNumber == lineNumber &&
        Objects.equals(methodName, e.methodName) &&
        Objects.equals(fileName, e.fileName);
}
```

## 3.3 hashCode
```java
public int hashCode() {
    int result = 31*declaringClass.hashCode() + methodName.hashCode();
    result = 31*result + Objects.hashCode(fileName);
    result = 31*result + lineNumber;
    return result;
}
```

## 3.4 get 字段
```java
// 返回包含此堆栈跟踪元素表示的执行点的源文件的名称。通常，这对应于相关 class 文件的 SourceFile 属性
// （根据 Java 虚拟机规范第 4.7.7节）。在某些系统中，名称可以引用文件以外的某些源代码单元，例如源存储库中的条目。
public String getFileName() {
    return fileName;
}

// 返回包含此堆栈跟踪元素表示的执行点的源行的行号。通常，这是从相关 class 文件的 LineNumberTable 属性派生的
// （根据 Java 虚拟机规范第 4.7.8节）。如果此信息不可用，则为负数。
public int getLineNumber() {
    return lineNumber;
}

// 返回包含此堆栈跟踪元素表示的执行点的类的完全限定名称。
public String getClassName() {
    return declaringClass;
}

// 返回包含此堆栈跟踪元素表示的执行点的方法的名称。如果执行点包含在实例或类初始化块中，
// 则此方法将根据 Java 虚拟机规范的 3.9 节返回适当的特殊方法名称 <init> 或 <clinit>。
public String getMethodName() {
    return methodName;
}
```

## 3.5 isNativeMethod
```java
// 如果此堆栈跟踪元素表示的执行点的方法是 native 方法，则返回 true。
public boolean isNativeMethod() {
    // native 方法行号一律是 -2
    return lineNumber == -2;
}
```


[throwable]: Throwable.md