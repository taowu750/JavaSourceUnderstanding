`java.util.Tripwire`类的声明如下：
```java
final class Tripwire
```
这是一个工具类，专门用于检测`java.util`包中对于装箱操作的不正确使用。例如在`java.util.PrimitiveIterator.OfInt.next`方法中，
这个方法源自`java.util.Iterator`，返回`Integer`，但是`OfInt`接口是一个针对基本类型`int`设计的迭代器，所以当使用`next`方法时，
会使用`Tripwire.trip`方法发出警告。

`Tripwire`类默认情况下关闭，要想启用就需要设置系统属性`org.openjdk.java.util.stream.tripwire`为`true`。

## 1. 属性

### 1.1 TRIPWIRE_PROPERTY
```java
private static final String TRIPWIRE_PROPERTY = "org.openjdk.java.util.stream.tripwire";
```

<!-- TODO: AccessController 和 PlatformLogger 类 -->

### 1.2 ENABLED
```java
static final boolean ENABLED = AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean(TRIPWIRE_PROPERTY));
```

## 2. 方法

### 2.1 trip
```java
static void trip(Class<?> trippingClass, String msg) {
    PlatformLogger.getLogger(trippingClass.getName()).warning(msg, trippingClass.getName());
}
```