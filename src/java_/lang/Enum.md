`java.lang.Enum`抽象类的声明如下：
```java
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable
```
这是所有`Java`语言枚举类型的通用基类。当我们使用`enum`关键字定义一个枚举时，它会自动继承`Enum`类，并被标记为`final`。
这就是为什么枚举无法继承其他类也无法被其他类继承的原因。不过枚举还是可以实现接口的。

`Enum`类中没有`values()`和`valueOf(String)`方法，而在我们定义的枚举中，编译器会帮我们加上这些方法。

枚举中的元素都是枚举的对象，我们可以在枚举中添加方法，甚至可以定义构造器：
```java
public enum OzWitch {
    // 定义枚举元素必须先于方法
    // 构造器只能在定义枚举元素时调用
    WEST("Miss Gulch, aka the Wicked Witch of the West"),
    NORTH("Glinda, the Good Witch of the North"),
    EAST("Wicked Witch of the East, wearer of the Ruby " +
            "Slippers, crushed by Dorothy's house"),
    SOUTH("Good by inference, but missing"); // 注意要以分号结束

    private String description;

    // 构造器必须是 private 或包私有的，一旦 enum 的定义结束，编译器就不允许我们再使用其构造器来创建任何实例了
    private OzWitch(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    public static void main(String[] args) {
        for(OzWitch witch : OzWitch.values())
            System.out.println(witch + ": " + witch.getDescription());
    }
}
```
所有枚举元素都会被标记为`public static final`，因此当枚举在其他类的内部被定义，它们无法访问外部类的非`static`元素或方法。

枚举可以被用来实现职责链、状态机或多路分发。有关枚举的使用和高级主题可参见[On Java8][on-java8]。

有关枚举的更多信息，包括由编译器生成的隐式声明的方法的描述，
可以在`Java™`语言规范的 8.9 节中找到。请注意，在将枚举类型用作集合的类型或映射中的键的类型时，
可以使用专用且高效的集合`EnumSet`和映射`EnumMap`。

# 1. 成员字段
```java
// 枚举中声明的枚举常量的名称。
private final String name;

// 枚举常量的序数（在枚举声明中的位置，其中初始常量的序数为零）。
private final int ordinal;
```

# 2. 构造器
```java
// Sole 构造器。程序员不能调用此构造函数。它由编译器根据枚举常量调用。
protected Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
}
```

# 3. 方法

## 3.1 属性
```java
public final String name() {
    return name;
}

public final int ordinal() {
    return ordinal;
}
```

## 3.2 toString
```java
public String toString() {
    return name;
}
```

## 3.3 equals
```java
public final boolean equals(Object other) {
    // 每个枚举常量只能在编译时定义好，因此只需要“==”判断即可
    return this==other;
}
```

## 3.4 hashCode
```java
public final int hashCode() {
    // 直接返回 Object.hashCode()
    return super.hashCode();
}
```

## 3.5 clone
```java
// 枚举常量必须保证唯一，所以调用 clone 方法将抛出异常。这对于保留它们的“单例”状态是必要的。
protected final Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
}
```

## 3.6 finalize
```java
// 枚举不能有 finalize 方法
protected final void finalize() { }
```

## 3.7 compareTo
```java
// 枚举常量仅可与相同枚举类型的其他枚举常量比较。此方法实现的自然顺序是声明枚举常量的顺序。
public final int compareTo(E o) {
    Enum<?> other = (Enum<?>)o;
    Enum<E> self = this;
    if (self.getClass() != other.getClass() && // optimization
        self.getDeclaringClass() != other.getDeclaringClass())
        throw new ClassCastException();
    return self.ordinal - other.ordinal;
}
```

## 3.8 getDeclaringClass
```java
// 返回与此枚举常量的枚举类型相对应的 Class 对象。当且仅当 e1.getDeclaringClass() == e2.getDeclaringClass() 时，
// 两个枚举常量 e1 和 e2 属于相同的枚举类型。
public final Class<E> getDeclaringClass() {
    Class<?> clazz = getClass();
    Class<?> zuper = clazz.getSuperclass();
    return (zuper == Enum.class) ? (Class<E>)clazz : (Class<E>)zuper;
}
```
在使用枚举类的时候，建议用`getDeclaringClass`方法返回枚举的`Class`。但是为什么不用`getClass`呢？我们来看下面一个例子：
```java
public enum FruitEnum{
    BANANA{
        String getName() {
            return "香蕉";
        }
    },
    APPLE{
        String getName() {
            return "苹果";
        }
    };

    abstract String getName();

    public static void main(String[] args) {
        System.out.println(BANANA.getDeclaringClass());
        System.out.println(BANANA.getClass());
    }
}
```
> 输出：  
> class FruitEnum  
> class FruitEnum$1

枚举可以有`abstract`方法，此时`BANANA`和`APPLE`相当于`FruitEnum`的内部类。

## 3.9 valueOf
```java
// 返回具有指定名称的指定枚举类型的枚举常量。名称必须与用于声明此类型的枚举常量的标识符完全匹配。（不允许使用多余的空格字符。）
// 请注意，对于特定的枚举类型 T，可以使用对该枚举上编译器添加的的 public static T valueOf(String) 方法来代替此方法，
// 以从名称映射到相应的枚举常量。
// 还可以通过调用该枚举上编译器添加的 public static T[] values() 方法来获取枚举类型的所有常量。
public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
    T result = enumType.enumConstantDirectory().get(name);
    if (result != null)
        return result;
    if (name == null)
        throw new NullPointerException("Name is null");
    throw new IllegalArgumentException(
        "No enum constant " + enumType.getCanonicalName() + "." + name);
}
```

## 3.10 序列化
```java
// 阻止默认的序列化行为
private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    throw new InvalidObjectException("can't deserialize enum");
}

private void readObjectNoData() throws ObjectStreamException {
    throw new InvalidObjectException("can't deserialize enum");
}
```


[on-java8]: https://lingcoder.github.io/OnJava8/#/book/22-Enumerations