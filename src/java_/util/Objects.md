`java.util.Objects`类，声明如下：
```java
public final class Objects
```
这是用于操作对象的工具类，提供了一组操作对象的便利方法。

# 1. 方法

## 1.1 equals
```java
public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
}
```
从源码中可以看出，这个方法只是保证两个对象为`null`的情况下调用对象的equals不会出错，你仍然要实现对象的`equals`方法。

## 1.2 deepEquals
```java
public static boolean deepEquals(Object a, Object b) {
    if (a == b)
        return true;
    else if (a == null || b == null)
        return false;
    else
        return Arrays.deepEquals0(a, b);
}
```
`deepEquals`方法不是你想的那样可以自动判断对象是否相等，而不用自己写`equals`方法。它只是先保证对象为`null`的情况下返回正确的结果，
其他时候会调用`Arrays.deepEquals0`。这个包内方法会先判断`a`和`b`是不是数组，如果是，递归地对其中的元素也做这样的判断。
所以这个方法可以处理多维数组，但具体的元素还是使用它的`equals`方法。

## 1.3 hashCode
```java
public static int hashCode(Object o) {
    return o != null ? o.hashCode() : 0;
}
```
`hashCode`方法也只是保证对象为`null`时不会抛出错误，而是返回0。

## 1.4 hash
```java
public static int hash(Object... values) {
    return Arrays.hashCode(values);
}
```
简单地调用`Arrays.hashCode`方法，这个方法将执行[java_/lang/哈希码.md][hashCode]一节中所述的方法计算哈希码。

## 1.5 toString
```java
public static String toString(Object o) {
    return String.valueOf(o);
}

public static String toString(Object o, String nullDefault) {
    return (o != null) ? o.toString() : nullDefault;
}
```
简单地调用`String.valueOf()`方法。第二个重载方法给出了参数为`null`情况下的值。

## 1.6 compare
```java
public static <T> int compare(T a, T b, Comparator<? super T> c) {
    return (a == b) ? 0 :  c.compare(a, b);
}
```
比较两个对象，也针对`null`情况做了处理。

## 1.7 requireNonNull
```java
public static <T> T requireNonNull(T obj) {
    if (obj == null)
        throw new NullPointerException();
    return obj;
}

public static <T> T requireNonNull(T obj, String message) {
    if (obj == null)
        throw new NullPointerException(message);
    return obj;
}
```
当`obj`为`null`时，抛出`NullPointException`。这个方法看起来没用，但却可以在函数调用的开始处报错，而不是等到用到`obj`的时候突然
出错。第二个重载方法可以让你指定报错信息。


[hashCode]: ../lang/哈希算法.md