`java.util.concurrent.atomic.AtomicIntegerFieldUpdater`类的声明如下：
```java
public abstract class AtomicIntegerFieldUpdater<T>
```
一个基于反射的原子类，可以对指定类的指定 `volatile int` 字段进行原子更新。该类被设计用于原子数据结构中，
其中同一节点的多个字段被独立地进行原子更新。

请注意，该类中 `compareAndSet` 方法的保证比其他原子类中的保证要弱。因为这个类不能保证字段的所有使用都适合原子访问的目的，
所以它只能保证在同一更新器上对 `compareAndSet`、`set` 以及其他调用的原子性。

由于 `AtomicIntegerFieldUpdater` 是基于反射的原子更新字段的值。要想原子地更新字段，需要满足以下条件:
 - 因为 `AtomicIntegerFieldUpdater` 是抽象类，每次使用的时候必须使用静态方法 `newUpdater()` 创建一个更新器，
然后设置想要更新的类和属性。
 - 想要更新的类的字段必须使用 `volatile` 修饰。
 - 字段的访问控制权限（修饰符 `public/protected/default/private`）需要与调用者一致。
 也就是说如果调用者能够直接操作对象字段，那么就可以反射地进行原子操作。
 - 对于父类的字段，子类是不能直接操作的，尽管子类可以访问父类的字段。
 - 只能是实例变量，不能是类变量，也就是说不能加 `static` 关键字。
 - 只能是可修改变量，不能是 `final` 变量，因为 `final` 的语义就是不可修改。

相关测试参见 [AtomicIntegerFieldUpdaterTest][test]。

# 1. 构造器
```java
protected AtomicIntegerFieldUpdater() {
}
```

# 2. 方法

## 2.1 newUpdater
```java
// 为给定字段的对象创建并返回一个更新器。
@CallerSensitive
public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                          String fieldName) {
    return new AtomicIntegerFieldUpdaterImpl<U>
        (tclass, fieldName, Reflection.getCallerClass());
}
```

## 2.2 get
```java
// 获取该更新器管理的给定对象的字段中的当前值。
public abstract int get(T obj);
```

## 2.3 set
```java
/*
将该更新器管理的给定对象的字段设置为给定的更新值。
这个操作被保证在以后调用 compareAndSet 时作为一个 volatile 写操作。
*/
public abstract void set(T obj, int newValue);
```

## 2.4 lazySet
```java
/*
lazySet 不会立刻(但是最终会)修改旧值，别的线程看到新值的时间会延迟一些。
lazySet 比 set 具有性能优势，但是使用场景很有限，在编写非阻塞数据结构微调代码时可能会很有用。

其语义是保证写的内容不会与之前的任何写的内容重新排序，但可能会与后续的操作重新排序（或者等价地，可能对其他线程不可见），
直到其他一些易失性写或同步操作发生）。

lazySet 提供了一个前置的 store-store 屏障（这在当前的平台上要么是无操作，要么是非常快速地的），
但没有 store-load 屏障（这通常是 volatile-write 的耗时操作）。
*/
public abstract void lazySet(T obj, int newValue);
```

## 2.5 compareAndSet
```java
/*
如果当前值==expect，则原子地将该更新器管理的给定对象的字段设置为 update。
这个方法保证与其他 compareAndSet 和 set 调用之间是原子性的，
但不一定与该字段的其他变化（例如使用对象改变）之间是原子的。
*/
public abstract boolean compareAndSet(T obj, int expect, int update);
```

## 2.6 weakCompareAndSet
```java
// 可能会杂乱地失败，并且不提供排序保证，所以只有在很少情况下才适合作为 compareAndSet 的替代方法。
public abstract boolean weakCompareAndSet(T obj, int expect, int update);
```

## 2.7 getAndSet
```java
// 原子化地将该更新器管理的对象的字段设置为给定值，并返回旧值。
public int getAndSet(T obj, int newValue) {
    int prev;
    do {
        prev = get(obj);
    } while (!compareAndSet(obj, prev, newValue));
    return prev;
}
```

## 2.8 increment
```java
public int getAndIncrement(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}

public int incrementAndGet(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

## 2.9 decrement
```java
public int getAndDecrement(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}

public int decrementAndGet(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

## 2.10 add
```java
public int getAndAdd(T obj, int delta) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}

public int addAndGet(T obj, int delta) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

## 2.11 update
```java
public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}

public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

## 2.12 accumulate
```java
public final int getAndAccumulate(T obj, int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}

public final int accumulateAndGet(T obj, int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

# 3. AtomicIntegerFieldUpdaterImpl
```java
private static final class AtomicIntegerFieldUpdaterImpl<T>
    extends AtomicIntegerFieldUpdater<T> {
```

## 3.1 成员字段
```java
// Unsafe 对象
private static final sun.misc.Unsafe U = sun.misc.Unsafe.getUnsafe();
// 指定字段的偏移量
private final long offset;

// 传入的想要更新的类
private final Class<T> tclass;
// 如果字段被 protected 修饰，目标类是调用类的父类，并且调用类和目标类不在同一个包下时，cclass 存储调用者的类；
// 否则 cclass 和 tclass 相同。这是为了抛出异常的时候能够提供更准确的信息。
private final Class<?> cclass;
```

## 3.2 构造器
```java
AtomicIntegerFieldUpdaterImpl(final Class<T> tclass,
                              final String fieldName,
                              final Class<?> caller) {
    final Field field;
    final int modifiers;
    try {
        // 获取指定字段，并保证访问是合法的
        field = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Field>() {
                    public Field run() throws NoSuchFieldException {
                        return tclass.getDeclaredField(fieldName);
                    }
                });
        modifiers = field.getModifiers();
        // 确保 field 是可访问的（也就是调用者可以直接访问这个 field）
        sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                caller, tclass, null, modifiers);
        // 获取传入类的类加载器
        ClassLoader cl = tclass.getClassLoader();
        // 获取调用类的类加载器
        ClassLoader ccl = caller.getClassLoader();
        if ((ccl != null) && (ccl != cl) &&
            ((cl == null) || !isAncestor(cl, ccl))) {
            // 如果这两个类的类加载器没有委派关系则进行包可访问性检查
            sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
        }
    } catch (PrivilegedActionException pae) {
        throw new RuntimeException(pae.getException());
    } catch (Exception ex) {
        throw new RuntimeException(ex);
    }

    // 必须要为 int
    if (field.getType() != int.class)
        throw new IllegalArgumentException("Must be integer type");

    // 操作的属性必须要用 volatile
    if (!Modifier.isVolatile(modifiers))
        throw new IllegalArgumentException("Must be volatile type");

    /*
    如果字段被 protected 修饰，目标类是调用类的父类，并且调用类和目标类不在同一个包下时，cclass 存储调用者的类；
    否则 cclass 和 tclass 相同。这一步的目的是为了抛出异常的时候能够提供更准确的信息。
    */
    this.cclass = (Modifier.isProtected(modifiers) &&
                   tclass.isAssignableFrom(caller) &&
                   !isSamePackage(tclass, caller))
                   ? caller : tclass;
    this.tclass = tclass;
    // 计算字段偏移量
    this.offset = U.objectFieldOffset(field);
}

// 如果第二个类加载器是第一个类加载器的祖先，即两个类加载器有委派关系，返回 true。
private static boolean isAncestor(ClassLoader first, ClassLoader second) {
    ClassLoader acl = first;
    do {
        acl = acl.getParent();
        if (second == acl) {
            return true;
        }
    } while (acl != null);
    return false;
}

private static boolean isSamePackage(Class<?> class1, Class<?> class2) {
    return class1.getClassLoader() == class2.getClassLoader()
               && Objects.equals(getPackageName(class1), getPackageName(class2));
}

private static String getPackageName(Class<?> cls) {
    String cn = cls.getName();
    int dot = cn.lastIndexOf('.');
    return (dot != -1) ? cn.substring(0, dot) : "";
}
```

## 3.3 方法

### 3.3.1 accessCheck
```java
// 检查目标参数是否是 cclass 的实例（或子类实例），不是的话抛出异常。
private final void accessCheck(T obj) {
    if (!cclass.isInstance(obj))
        throwAccessCheckException(obj);
}

private final void throwAccessCheckException(T obj) {
    // 根据类型的不同抛出不同的异常
    if (cclass == tclass)
        throw new ClassCastException();
    else
        throw new RuntimeException(
                new IllegalAccessException(
                    "Class " +
                    cclass.getName() +
                    " can not access a protected member of class " +
                    tclass.getName() +
                    " using an instance of " +
                    obj.getClass().getName()));
}
```

### 3.3.2 get
```java
public final int get(T obj) {
    accessCheck(obj);
    return U.getIntVolatile(obj, offset);
}
```

### 3.3.3 set
```java
public final void set(T obj, int newValue) {
    accessCheck(obj);
    U.putIntVolatile(obj, offset, newValue);
}
```

### 3.3.4 lazySet
```java
public final void lazySet(T obj, int newValue) {
    accessCheck(obj);
    U.putOrderedInt(obj, offset, newValue);
}
```

### 3.3.5 compareAndSet
```java
public final boolean compareAndSet(T obj, int expect, int update) {
    accessCheck(obj);
    return U.compareAndSwapInt(obj, offset, expect, update);
}
```

### 3.3.6 weakCompareAndSet
```java
public final boolean weakCompareAndSet(T obj, int expect, int update) {
    accessCheck(obj);
    return U.compareAndSwapInt(obj, offset, expect, update);
}
```

### 3.3.7 getAndSet
```java
public final int getAndSet(T obj, int newValue) {
    accessCheck(obj);
    return U.getAndSetInt(obj, offset, newValue);
}
```

### 3.3.8 increment
```java
public final int getAndIncrement(T obj) {
    return getAndAdd(obj, 1);
}

public final int incrementAndGet(T obj) {
    return getAndAdd(obj, 1) + 1;
}
```

### 3.3.9 decrement
```java
public final int getAndDecrement(T obj) {
    return getAndAdd(obj, -1);
}

public final int decrementAndGet(T obj) {
    return getAndAdd(obj, -1) - 1;
}
```

### 3.3.10 add
```java
public final int getAndAdd(T obj, int delta) {
    accessCheck(obj);
    return U.getAndAddInt(obj, offset, delta);
}

public final int addAndGet(T obj, int delta) {
    return getAndAdd(obj, delta) + delta;
}
```


[test]: ../../../../../test/java_/util/concurrent/atomic/AtomicIntegerFieldUpdaterTest.java