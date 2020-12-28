`java.util.Spliterator`接口的声明如下：
```java
public interface Spliterator<T>
```

> 介绍

`Spliterator`接口自`Java8`被引入，它是可拆分迭代器，也用于遍历数据源中的元素，但它是为了并行执行而设计的。
基本思想就是把一个集合分割成多个小集合由多个线程遍历，这类似于分治算法。

`Spliterator`是用于遍历和划分源元素的对象。`Spliterator`元素的来源可以是数组，`Collection`，`IO`通道或生成器函数。

`Spliterator`可以单独遍历元素（`tryAdvance()`）或批量遍历元素（`forEachRemaining()`）。
分离器还可以将其中的某些元素（使用`trySplit`）划分为另一个分离器，以用于可能的并行操作。对于使用`Spliterator`无法拆分，
或以高度不平衡或效率低下的方式拆分的操作，将很难从并行性中受益。通过遍历和拆分剩余元素，每个`Spliterator`将用在单个批量计算。

`Spliterator`通过`characteristics()`声明它的结构、源和元素的特征。这些特征的取值为：
 - `ORDERED`：元素具有一定的顺序
 - `DISTINCT`：元素不重复
 - `SORTED`：元素是排序的
 - `SIZED`：具有有限的大小
 - `NONNULL`：元素非`null`
 - `IMMUTABLE`：元素源不可变（不可增加、删除、修改）
 - `CONCURRENT`：元素源可以由多个线程安全地并行修改
 - `SUBSIZED`：分割后的子`Spliterator`也具有确切的大小

`Spliterator`报告的特征可能不止一个。这些特征可能被`Spliterator`客户端用来控制，专门化或简化计算。
例如，一个`Collection Spliterator`将声明`SIZED`；一个`Set Spliterator`将声明`DISTINCT`，
而`SortedSet Spliterator`还会将报告`SORTED`。特征为简单的`int`二进制位。
一些特征还限制了方法的行为。例如，如果为`ORDERED`，则`iterator`方法必须符合其记录的顺序。

将来可能会定义新的特性，因此实现者不应为未列出的值分配含义。

**绑定**。不声明`IMMUTABLE`或`CONCURRENT`的`Spliterator`应声明如下策略：  
 - `Spliterator`怎样绑定到元素源
 - 绑定后检测到元素源的结构发生改变应该怎样。

**后期绑定**的`Spliterator`会在第一次遍历，第一次拆分或首次查询大小时绑定到元素源，
而不是在创建`Spliterator`时绑定。不是后期绑定的`Spliterator`会在创建时或任何方法的首次调用时绑定到元素源。
遍历`Spliterator`时，会反映出绑定之前对源所做的修改。绑定后，如果检测到元素源结构改变，
则`Spliterator`应尽最大努力抛出`ConcurrentModificationException`。执行此操作的`Spliterator`称为`fail-fast`
（这类似于使用迭代器遍历`list`的时候，调用此`list`的方法改变`list`的结构——删除或增加元素——将会抛出异常一样）。
批量遍历方法`forEachRemaining()`可以优化遍历并在遍历所有元素之后检查结构改变，而不是检查每个元素并立即抛出异常。

`Spliterator`可以通过`estimateSize`方法计算剩余元素的数量。理想情况下，如特性`SIZED`所反映的，
该值正好对应于实际的元素数。但是，即使此值是不准确的，这个估计值对于在源上执行的操作仍然有用，
例如有助于确定是否更进一步地拆分或遍历其余元素。

尽管`Spliterator`在并行算法中具有明显的实用性，但它并不是线程安全的。相反，
使用`Spliterator`的并行算法的实现应确保`Spliterator`一次只能由一个线程使用。这很容易通过分治递归实现。

调用`trySplit()`线程可以将返回的`Spliterator`移交给另一个线程，该线程又可以遍历或进一步拆分该`Spliterator`。
如果两个或多个线程在同一个拆分器上同时运行，则拆分和遍历的行为是不确定的。如果原始线程将`Spliterator`移交给另一个线程进行处理，
则最好在`tryAdvance()`使用任何元素之前进行该移交，因为某些保证（例如声明`SIZED`特征的`Spliterator`的`estimateSize()`的准确性）
仅在遍历开始之前有效。

我们为`int`，`long`和`double`基本类型提供了专门的`Spliterator`。`tryAdvance(Consumer)`和`forEachRemaining(Consumer)`
方法将原始类型装箱。这种装箱可能会造成性能损失。为避免装箱，应使用相应的基于原始类型的方法。例如，
应该优先使用`Spliterator.OfInt.tryAdvance(IntConsumer)`和`Spliterator.OfInt.forEachRemaining(IntConsumer)`。
使用基于装箱的方法遍历基本类型值不会影响值（转换为装箱值）的遍历顺序。

> 用法

像`Iterator`一样，`Spliterator`用于遍历元素。通过支持分解和单元素迭代，`Spliterator API`旨在支持并行遍历。此外，
通过使用`Spliterator`旨在使访问元素的平均开销比`Iterator`小，并且避免`hasNext()`和`next()`单独方法所涉及的固有竞争。

对于可变源，如果在`Spliterator`绑定到其数据源时到遍历结束之间，源的结构发生改变（添加，替换或删除），
则可能会未定义的行为。可以通过以下方式来管理源的结构改变：
 - 使用结构上不会改变的源。例如，`java.util.concurrent.CopyOnWriteArrayList`的实例是不可变的源。
 从此类源上创建的`Spliterator`将具有`IMMUTABLE`特征。
 - 源能够管理并发修改。例如，`java.util.concurrent.ConcurrentHashMap`的键集是并发源。
 从此源创建的`Spliterator`具有`CONCURRENT`的特征。
 - 可变源提供了后期绑定和`fail-fast`的`Spliterator`。后期绑定可能影响计算的时间窗口；
 故障快速检测会尽最大努力检测到遍历开始之后发生的结构改变并抛出`ConcurrentModificationException`。例如，
 `ArrayList`和`JDK`中的许多其他非并行`Collection`类提供了后期绑定和`fail-fast`的`Spliterator`。
 - 可变源提供了非后期绑定但`fail-fast`的`Spliterator`。由于潜在结构改变的时间窗口较大，
 因此源增加了引发`ConcurrentModificationException`的可能性。
 - 可变源提供了后期绑定和非`fail-fast`的`Spliterator`。遍历开始后，由于未检测到结构改变，此源可能会具有不确定的行为。
 - 可变源提供了非后期绑定和非`fail-fast`的`Spliterator`。由于在构造之后可能会发生未检测到的结构，
 此源有较大发生不确定行为的风险。

下面是一个示例，它维护一个数组，其中实际数据保存在偶数位置，标记数据保存在奇数位置。它的`Spliterator`将忽略标记数据：
````java
 class TaggedArray<T> {
   private final Object[] elements; // 在构造之后就不可变

   TaggedArray(T[] data, Object[] tags) {
     int size = data.length;
     if (tags.length != size) throw new IllegalArgumentException();
     this.elements = new Object[2 * size];
     for (int i = 0, j = 0; i < size; ++i) {
       elements[j++] = data[i];
       elements[j++] = tags[i];
     }
   }

   public Spliterator<T> spliterator() {
     return new TaggedArraySpliterator<>(elements, 0, elements.length);
   }

   static class TaggedArraySpliterator<T> implements Spliterator<T> {
     private final Object[] array; // 元素源
     private int origin; // 当前下标，在遍历或拆分时增加
     private final int fence; // 最大下标

     TaggedArraySpliterator(Object[] array, int origin, int fence) {
       this.array = array;
       this.origin = origin;
       this.fence = fence;
     }

     public void forEachRemaining(Consumer<? super T> action) {
       for (; origin < fence; origin += 2)
         action.accept((T) array[origin]);
     }

     // 一次遍历一个元素
     public boolean tryAdvance(Consumer<? super T> action) {
       if (origin < fence) {
         action.accept((T) array[origin]);
         origin += 2;
         return true;
       }
       else // 不能再继续遍历
         return false;
     }

     public Spliterator<T> trySplit() {
       int lo = origin; // 将当前范围分半
       int mid = ((lo + fence) >>> 1) & ~1; // 使 mid 是偶数（二进制最低位是 0 的数就是偶数）
       if (lo < mid) { // 分割左半边并返回
         origin = mid; // 将当前的 origin 设置到右半边的开头
         return new TaggedArraySpliterator<>(array, lo, mid);
       }
       else       // 如果太小了，则不能分割
         return null;
     }

     public long estimateSize() {
       return (long)((fence - origin) / 2);
     }

     public int characteristics() {
       // 此 Spliterator 具有如下特征：
       return ORDERED | SIZED | IMMUTABLE | SUBSIZED;
     }
   }
 }
````
使用上面的类，我们看看诸如`java.util.stream`包之类的并行计算框架将如何使用`Spliterator`，
这是一种实现关联的并行`forEach`的方法，该方法说明了将子任务拆分为主要任务的惯用法。如果估计的工作量很小，可以按顺序执行。
在这里，我们假设子任务的处理顺序无关紧要；不同（分叉）的任务可能会进一步以不确定的顺序同时拆分和处理元素。
本示例使用`java.util.concurrent.CountedCompleter`，类似的用法适用于其他并行任务构造。
```java
 static <T> void parEach(TaggedArray<T> a, Consumer<T> action) {
   Spliterator<T> s = a.spliterator();
   // ForkJoinPool.getCommonPoolParallelism() 获取 cpu 核心数
   long targetBatchSize = s.estimateSize() / (ForkJoinPool.getCommonPoolParallelism() * 8);
   new ParEach(null, s, action, targetBatchSize).invoke();
 }

 static class ParEach<T> extends CountedCompleter<Void> {
   final Spliterator<T> spliterator;
   final Consumer<T> action;
   final long targetBatchSize;

   ParEach(ParEach<T> parent, Spliterator<T> spliterator,
           Consumer<T> action, long targetBatchSize) {
     super(parent);
     this.spliterator = spliterator;
     this.action = action;
     this.targetBatchSize = targetBatchSize;
   }

   public void compute() {
     Spliterator<T> sub;
     // 循环切分数据，直到无法切分
     while (spliterator.estimateSize() > targetBatchSize &&
            (sub = spliterator.trySplit()) != null) {
       addToPendingCount(1);
       // 增加新的任务处理切分的 Spliterator
       new ParEach<>(this, sub, action, targetBatchSize).fork();
     }
     spliterator.forEachRemaining(action);
     propagateCompletion();
   }
 }
```

> 注意

如果布尔系统属性`org.openjdk.java.util.stream.tripwire`设置为`true`，且在对基本类型进行操作时如果发生装箱，
则会报告警告信息。参见 [Tripwire.md][tripwire]。

# 1. 特征值
```java
/*
特征值：表示元素出现顺序。如果具有此特征值，则此 Spliterator 保证 trySplit 方法将分割元素源的开始部分，
tryAdvance 和 forEachRemaining 将按元素出现顺序执行操作。

如果 Collection.iterator() 声明了一个顺序，则此 Collection 有一个元素出现顺序。
如果是这样，则元素出现顺序与声明的顺序相同。否则，Collection 没有元素出现顺序。

对于任何 List 出现顺序均保证为升序索引顺序。但是，对于基于哈希的集合（例如 HashSet）无法保证任何顺序。
声明 ORDERED 的 Spliterator 的调用者应在非交换并行计算中保留顺序约束。
*/
public static final int ORDERED    = 0x00000010;

/*
表示对于每对遇到的元素(x, y)，有 !x.equals(y)。
例如基于 Set 的 Spliterator。
*/
public static final int DISTINCT   = 0x00000001;

/*
特征值：表示元素出现顺序遵循规定的排序顺序。如果是这样，则方法 getComparator() 返回关联的 Comparator；
如果所有元素都是 Comparable 并按其自然顺序排序，则返回 null。

声明 SORTED 的 Spliterator 也必须声明 ORDERED。JDK 中实现 NavigableSet 或 SortedSet 的类的 Spliterator 声明 SORTED。
*/
public static final int SORTED     = 0x00000004;

/*
特征值：表示在遍历或分割之前 estimateSize() 方法返回一个有限大小，在没有结构修改的情况下，
该值表示完整遍历遇到的元素数的精确计数。

大多数集合（涵盖所有 Collection 的子类）的 Spliterator 声明这一特征。子 Spliterator（例如 HashSet 子 Spliterator）
将会返回近似大小。
*/
public static final int SIZED      = 0x00000040;

/*
特征值：表示元素源保证其中的元素不会为 null。
这适用于大多数并发 Collection，队列和 Map。
*/
public static final int NONNULL    = 0x00000100;

/*
特征值：表示元素源无法进行结构修改；也就是说，不能添加，替换或删除元素，因此在遍历期间不会发生此类更改。

不声明 IMMUTABLE 或 CONCURRENT 应当具有有关遍历过程中检测到的结构改变的策略（例如，抛出 ConcurrentModificationException）。
*/
public static final int IMMUTABLE  = 0x00000400;

/*
特征值：表示元素源可以由多个线程安全地并行修改（允许添加，替换和/或删除）而无需外部同步。
如果是这样，则它的 Spliterator 具有关于遍历期间修改影响的策略。

顶级 Spliterator 不应同时声明 CONCURRENT 和 SIZED，因为如果在遍历期间同时修改源，则有限大小（如果已知）可能会更改。
这样的 Spliterator 是不一致的，并且不能保证使用该 Spliterator 进行的任何计算。如果已知子 Spliterator 的大小，
并且遍历时未出现元素源的添加或删除，则子 Spliterator 可能会声明 SIZED。

大多数并发的集合都维护一致性策略，以保证在 Splitter 构造时出现的元素的准确性，但可能无法反映后续的添加或删除。
*/
public static final int CONCURRENT = 0x00001000;

/*
特征值：表示 trySplit() 产生的所有 Spliterator 都将有 SIZED 和 SUBSIZED 特征值。
这意味着所有子 Spliterator，无论是直接还是间接的，都将为 SIZED。

声明了 SUBSIZED 而没有声明 SIZED 的 Spliterator 是不一致的，并且不能保证使用该 Spliterator 进行的任何计算。

一些 Spliterator（例如，近似平衡的二叉树的顶级 Spliterator）将声明 SIZED 而不声明 SUBSIZED，
因为通常知道整个树的大小而不直到子树的确切大小。
*/
public static final int SUBSIZED = 0x00004000;
```

# 2. 方法

## 2.1 遍历元素
```java
// 如果存在剩余元素，则对它执行给定的操作，返回true；否则返回false。如果此 Spliterator 为 ORDERED，
// 则按遇到顺序对下一个元素执行操作。该操作引发的异常将传递给调用方。
boolean tryAdvance(Consumer<? super T> action);

/*
在当前线程中按顺序对每个剩余元素执行给定的操作，直到所有元素都已处理或该操作引发异常。
如果此 Spliterator 为 ORDERED ，则按元素出现顺序执行操作。该操作引发的异常将传递给调用方。
*/
default void forEachRemaining(Consumer<? super T> action) {
    do { } while (tryAdvance(action));
}
```

## 2.2 trySplit
```java
/*
如果该 Spliterator 可以分割，则返回一个子 Spliterator。从此方法返回后，此 Spliterator 将不包含切分出去的元素。

如果此 Spliterator 为 ORDERED，则返回的 Spliterator 必须包含元素的前缀。

除非此 Spliterator 包含无限数量的元素，否则对 trySplit() 重复调用最终将返回 null。
返回非 null：分割前报告的 estimateSize() 的值，必须大于或等于分割后两个 Spliterator 的 estimateSize()；
如果此 Spliterator 为 SUBSIZED，则它在拆分之前的 estimateSize() 必须严格等于分割后两个 Spliterator 的 estimateSize()。
此方法可能出于任何原因返回 null，包括元素数量不足以拆分、遍历开始后无法拆分、数据结构约束以及效率方面的考虑。

理想的 trySplit 方法可以有效地（不需要遍历）将其元素精确地分成两半，从而实现平衡的并行计算。
许多偏离这一理想的做法仍然非常有效。例如，仅对近似平衡的树进行近似拆分；或者对于其中叶节点可能包含一个或两个元素的树，
不对这些叶结点进行拆分。
但是，拆分出来的两块数量差很大或者效率低下的 trySplit 通常会导致较差的并行性能。
*/
Spliterator<T> trySplit();
```

## 2.3 计算数量
```java
/*
返回 forEachRemaining 遍历将遇到的元素数量的估计值；如果无限，未知或计算成本太高，则返回 Long.MAX_VALUE。

如果此 Spliterator 为 SIZED，尚未部分遍历或拆分，或者此 Spliterator 为 SUBSIZED，尚未部分遍历，
则此估计值必须是完整遍历会遇到的元素的准确计数。否则，此估计值可能是不准确的的，但必须在 trySplit 操作后减少。

即使是不精确的估算值，也通常对计算有用。例如，近似平衡的二叉树的 Spliterator 可能返回一个值，
该值估计元素的数量为其父代的一半。如果根 Spliterator 不能保证准确的计数，则可以将大小估计为与其最大深度相对应的 2 的幂。
*/
long estimateSize();

// 如果此 Spliterator 为 SIZED，该方法返回 estimateSize()；否则返回 -1
default long getExactSizeIfKnown() {
    return (characteristics() & SIZED) == 0 ? -1L : estimateSize();
}
```

## 2.4 特征值
```java
/*
返回此 Spliterator 及其元素的一组特征。结果被表示为 ORDERED，DISTINCT，SORTED，SIZED，NONNULL，IMMUTABLE，
CONCURRENT，SUBSIZED 的或运算值。

在 trySplit 调用之前或之间重复调用给定的 Spliterator 上的 characteristics()，应始终返回相同的结果。

如果 Spliterator 返回了一组不一致的特征（从单个调用返回的特征或跨多个调用返回的特征），则不能保证使用此拆分器进行的任何计算。

拆分前给定 Spliterator 的特征可能与拆分后的征性不同。有关特定示例，请参见 SIZED，SUBSIZED 和 CONCURRENT。
*/
int characteristics();

// 如果此 Spliterator 的特征包含所有给定的特征，则返回 true。
default boolean hasCharacteristics(int characteristics) {
    return (characteristics() & characteristics) == characteristics;
}
```

## 2.5 getComparator
```java
/*
如果这个 Spliterator 的元素源是 SORTED 且有 Comparator，返回该 Comparator。
如果元素源是 SORTED 且采用自然排序（Comparable），返回 null。
如果元素源不是 SORTED，则抛出 IllegalStateException。
*/
default Comparator<? super T> getComparator() {
    throw new IllegalStateException();
}
```

# 3. 内部接口

## 3.1 OfPrimitive
```java
/*
一个专门用于基本类型的 Spliterator。

@param T 基本类型包装器类型
@param T_CONS 基本类型 Consumer
@param T_SPLITR OfPrimitive 类型对象
*/
public interface OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
        extends Spliterator<T> {
    // 返回基本类型的子 Spliterator
    @Override
    T_SPLITR trySplit();

    // 用于遍历基本类型的 tryAdvance
    @SuppressWarnings("overloads")
    boolean tryAdvance(T_CONS action);

    // 用于遍历基本类型的 forEachRemaining
    @SuppressWarnings("overloads")
    default void forEachRemaining(T_CONS action) {
        do { } while (tryAdvance(action));
    }
}
```

## 3.2 OfInt
```java
// 用于 int 的 Spliterator
public interface OfInt extends OfPrimitive<Integer, IntConsumer, OfInt> {

    @Override
    OfInt trySplit();

    @Override
    boolean tryAdvance(IntConsumer action);

    @Override
    default void forEachRemaining(IntConsumer action) {
        do { } while (tryAdvance(action));
    }

    @Override
    default boolean tryAdvance(Consumer<? super Integer> action) {
        // 如果 action 还实现了 IntConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof IntConsumer) {
            return tryAdvance((IntConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfInt.tryAdvance((IntConsumer) action::accept)");
            return tryAdvance((IntConsumer) action::accept);
        }
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> action) {
        // 如果 action 还实现了 IntConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof IntConsumer) {
            forEachRemaining((IntConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfInt.forEachRemaining((IntConsumer) action::accept)");
            forEachRemaining((IntConsumer) action::accept);
        }
    }
}
```

## 3.3 OfLong
```java
// 用于 long 的 Spliterator
public interface OfLong extends OfPrimitive<Long, LongConsumer, OfLong> {

    @Override
    OfLong trySplit();

    @Override
    boolean tryAdvance(LongConsumer action);

    @Override
    default void forEachRemaining(LongConsumer action) {
        do { } while (tryAdvance(action));
    }

    @Override
    default boolean tryAdvance(Consumer<? super Long> action) {
        // 如果 action 还实现了 LongConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof LongConsumer) {
            return tryAdvance((LongConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfLong.tryAdvance((LongConsumer) action::accept)");
            return tryAdvance((LongConsumer) action::accept);
        }
    }

    @Override
    default void forEachRemaining(Consumer<? super Long> action) {
        // 如果 action 还实现了 LongConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof LongConsumer) {
            forEachRemaining((LongConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfLong.forEachRemaining((LongConsumer) action::accept)");
            forEachRemaining((LongConsumer) action::accept);
        }
    }
}
```

## 3.4 OfDouble
```java
public interface OfDouble extends OfPrimitive<Double, DoubleConsumer, OfDouble> {

    @Override
    OfDouble trySplit();

    @Override
    boolean tryAdvance(DoubleConsumer action);

    @Override
    default void forEachRemaining(DoubleConsumer action) {
        do { } while (tryAdvance(action));
    }

    @Override
    default boolean tryAdvance(Consumer<? super Double> action) {
        // 如果 action 还实现了 DoubleConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof DoubleConsumer) {
            return tryAdvance((DoubleConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfDouble.tryAdvance((DoubleConsumer) action::accept)");
            return tryAdvance((DoubleConsumer) action::accept);
        }
    }

    @Override
    default void forEachRemaining(Consumer<? super Double> action) {
        // 如果 action 还实现了 DoubleConsumer 接口，则使用基本类型的 tryAdvance
        if (action instanceof DoubleConsumer) {
            forEachRemaining((DoubleConsumer) action);
        }
        else {
            // 否则如果 org.openjdk.java.util.stream.tripwire 开启，则发出警告
            if (Tripwire.ENABLED)
                Tripwire.trip(getClass(),
                              "{0} calling Spliterator.OfDouble.forEachRemaining((DoubleConsumer) action::accept)");
            forEachRemaining((DoubleConsumer) action::accept);
        }
    }
}
```


[tripwire]: Tripwire.md