`java.lang.ThreadLocal`类的声明如下：
```java
public class ThreadLocal<T>
```

> 作用

`ThreadLocal`用来提供线程局部变量。这些变量与普通变量不同，每个访问线程（通过其`get`或`set`方法）都有自己独立初始化的变量副本。

`ThreadLocal`实例通常在类中是`private static`字段，用于将一个状态（例如，用户 ID 或事务 ID）与一个线程相关联。
例如，下面的类生成每个线程本地的唯一标识符。线程的 ID 是在第一次调用`ThreadId.get()`时分配的，并且在后续调用中保持不变：
```java
import java.util.concurrent.atomic.AtomicInteger;
  
public class ThreadId {
    // Atomic integer containing the next thread ID to be assigned
    private static final AtomicInteger nextId = new AtomicInteger(0);
  
    // Thread local variable containing each thread's ID
    private static final ThreadLocal<Integer> threadId =
        new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return nextId.getAndIncrement();
            }
        };
  
    // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
        return threadId.get();
    }
}
```

通常有人会将`ThreadLocal`和`synchronized`等放在一起。其实我觉得这是比较容易使人误导的，因为两者的目的性完全不一样。
`ThreadLocal`主要的是用于独享自己的变量，避免一些资源的争夺，从而实现了空间换时间的思想。
而`synchronized`则主要用于临界（冲突）资源的分配，从而能够实现线程间信息同步，公共资源共享等，
所以严格来说`synchronized`其实是能够实现`ThreadLocal`所需要的达到的效果的，只不过这样会带来资源争夺导致并发性能下降

> 为什么需要是 static?

之所以建议`ThreadLocal`是`static`字段，是因为如果它是一个实例字段，那么会变成“一个线程-一个 ThreadLocal 实例”，
而不是我们通常需要的的“每个线程-一个 ThreadLocal 实例”。大多数情况下我们希望`ThreadLocal`对象是个单例，
而不要创建多个`ThreadLocal`对象。

> ThreadLocal 和 GC

只要线程是活动的并且`ThreadLocal`实例是可访问的，则每个线程都对其线程局部变量的副本持有隐式引用。
线程回收后，其线程`ThreadLocal`的所有副本都将进行垃圾回收（除非存在对这些副本的其他引用）。
如果某个`ThreadLocal`对象被回收，那么线程中与此`ThreadLocal`对应的局部变量也会被删除（这是 lazy 实现的）。

`ThreadLocal`一般情况下不会导致内存泄漏。但因为`ThreadLocalMap`的生命周期跟`Thread`一样长，如果线程一直存活，
比如你用的是线程池，那池子里面的线程自始至终都是活的，线程不被销毁，并且你用完后就没怎么操作过这个`ThreadLocal`，
`key`虽然会在`gc`时被回收，`value`一直被`ThreadLocalMap`引用着，可能会造成`value`的累积，从而导致内存泄漏。
因此，记得在一个线程中用完某个`ThreadLocal`就`remove`，可以防止线程不终止情况下的内存泄漏。

> ThreadLocal 初始化方法非线程安全

需要注意的是，`ThreadLocal`的`initialValue`方法不是线程安全的。如果在此初始化方法中用到了一些共享资源，
就需要使用同步，或者线程安全的容器、原子类。

> ThreadLocalMap

每个`Thread`在使用`ThreadLocal`时都会创建此线程自己的`ThreadLocalMap`对象，
它用来持有此线程访问的`ThreadLocal`和对应的局部变量。由于一个`ThreadLocalMap`只会被自己的持有线程访问，
因此它不需要同步，从而效率很高。由于`ThreadLocalMap`被线程持有，当线程被回收时它也会被回收；
`ThreadLocalMap`底层使用`WeakReference`持有`ThreadLocal`键，当`ThreadLocal`被回收时，
每个线程中的局部变量也会被回收（lazy 方式）。这使得`ThreadLocalMap`还是**GC-friendly**的。
可以说`ThreadLocal`的精华所在就是`ThreadLocalMap`。

`ThreadLocalMap`之所以设计为`ThreadLocal`的嵌套类，是因为：
 - `ThreadLocalMap`会被绑定到线程并存储给定线程的所有`ThreadLocal`键值对。因此，将其绑定到具体的`ThreadLocal`实例是没有意义的。
 - 使`ThreadLocalMap`成为`Thread`类的内部类似乎更合乎逻辑。但是`ThreadLocalMap`不需要`Thread`对象来操作，
 因为这会增加一些不必要的开销。它位于`ThreadLocal`类内的原因是`ThreadLocal`负责`ThreadLocalMap`的创建。
 仅当在当前线程中设置第一个`ThreadLocal`时，才为当前线程创建`ThreadLocalMap`。在那之后，
 将相同的`ThreadLocalMap`用于所有其他`ThreadLocal`变量。
 - `ThreadLocalMap`所有的方法都是`private`的。也就意味着除了`ThreadLocal`这个类，
 其他类是不能操作`ThreadLocalMap`中的任何方法的，这样就可以对其他类是透明的。同时这个类的权限是包级别的，
 也就意味着只有同一个包下面的类才能引用`ThreadLocalMap`这个类，这也是`Thread`为什么可以引用`ThreadLocalMap`的原因。
 这样的设计在使用的时候就显得简单，然后封装性又特别好。

`ThreadLocalMap`是一个基于开放地址线性探测法的散列表，之所以不使用类似于`HashMap`的拉链法+红黑树实现，我想有几点原因：
 - 历史原因：`Java8`的时候才将`HashMap`改为拉链法+红黑树实现
 - `HashMap`需要处理更通用的情况，而`ThreadLocalMap`只需要针对一个场景，现有的设计足以应付。
 
> 代码要点

 - 1.1 `hashCode`: 乘法哈希和黄金分割点
 - 3.1.1 `Entry`: 使用`WeakRefence`
 - 3.1.4.2 清理: 启发式地清理

# 1. 成员变量

## 1.1 hashCode
```java
// 用于生成下一个 hashCode，初始化为 0
private static AtomicInteger nextHashCode = new AtomicInteger();

/*
ThreadLocal 依赖于附加到每个线程的 ThreadLocalMap，这是个基于开放地址线性探测的散列表。
ThreadLocal 对象充当键，通过 threadLocalHashCode 搜索。这是一个自定义哈希码（仅在 ThreadLocalMap 中使用），
它消除了在相同线程中使用连续构造的 ThreadLocal 的情形下发生的冲突，而在不太常见的情况下保持良好的行为。
*/
private final int threadLocalHashCode = nextHashCode();

// hashCode 增长值。
private static final int HASH_INCREMENT = 0x61c88647;

private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
}
```
`HASH_INCREMENT`的值是一个魔法数字`0x61c88647`，它的十进制值是`1640531527`，二进制是`01100001110010001000011001000111`。
如果我们将这个二进制取反加 1，可以得到`10011110001101110111100110111001`，它是无符号十进制是`2654435769`，
而这正是[哈希算法.md][hash] 2.2.2 节提到的黄金分割点，它能达到最佳的散列效果。而`1640531527`是`2 ^ 32 * (1 - 1/φ)`，
这是 32 位的另一个黄金分割点。

# 2. 构造器
```java
public ThreadLocal() {
}
```
`ThreadLocal`只有一个无参构造器。

# 3. 内部类

## 3.1 ThreadLocalMap
```java
static class ThreadLocalMap
```
`ThreadLocalMap`是一个基于开放地址线性探测法的散列表，它仅适用于维护线程局部变量。该类是包私有的，以允许在`Thread`类中使用它。
为了处理`ThreadLocal`可能被回收的情况，哈希表条目使用`WeakReference`作为键。但是，由于未使用`ReferenceQueue`，
因此仅在表空间不足时，才保证删除**所有**过时条目。当调用`ThreadLocalMap`的任意方法（除`index`方法）时，
会删除遇到的过时条目。

### 3.1.1 Entry
```java
/*
Entry 继承自 WeakReference，使用它的主 ref 字段作为键（它总是一个 ThreadLocal 对象）。
注意空键（即 entry.get() == null）表示不再引用该键，因此可以从表中删除该项。在下面的代码中，这些条目称为“过时条目”。

WeakReference 是弱引用，当一个对象仅仅被弱引用指向, 而没有任何其他强引用指向的时候, 
如果这时 GC 运行, 不论当前的内存空间是否足够，这个对象都会被回收。使用弱引用就是保证当某个线程被回收时，
它关联的局部变量副本也会被回收。
*/
static class Entry extends WeakReference<ThreadLocal<?>> {
    // 和 ThreadLocal 关联的值
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        // 将 ThreadLocal 保存在弱引用中
        super(k);
        value = v;
    }
}
```

### 3.1.2 成员字段
```java
// 初始容量，容量必须是 2 的倍数
private static final int INITIAL_CAPACITY = 16;

// 条目表，这是个环形数组
private Entry[] table;

// 条目表中实际条目的数量
private int size = 0;

// 负载因子，再 hash 时的临界容量
private int threshold;
```

### 3.1.3 构造器
```java
// 构造一个初始包含（firstKey，firstValue）的 ThreadLocalMap。ThreadLocalMap 是延迟构造的，
// 因此只有在至少要放置一个条目时才创建一个。
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
    // 构造初始容量为 INITIAL_CAPACITY 条目表
    table = new Entry[INITIAL_CAPACITY];
    // 计算 firstKey 在条目表中的下标
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
    // 为 firstKey, firstValue 创建条目放入表中
    table[i] = new Entry(firstKey, firstValue);
    // 条目数量此时为 1
    size = 1;
    // 设置 threshold 为容量的 2/3
    setThreshold(INITIAL_CAPACITY);
}

// 构造一个新 ThreadLocalMap，包括 parentMap 中的所有可用的 ThreadLocal。仅由 createInheritedMap 方法调用。
// 此构造器为 ThreadLocal 子类 InheritableThreadLocal 提供
private ThreadLocalMap(ThreadLocalMap parentMap) {
    // 获取 parentMap 的条目表
    Entry[] parentTable = parentMap.table;
    // 获取 parentMap 条目表的容量
    int len = parentTable.length;
    // 设置 threshold 为 parentMap 条目表容量的 2/3
    setThreshold(len);
    // 构造新的条目表
    table = new Entry[len];

    // 循环获取 parentMap 条目表中的每一 Entry
    for (int j = 0; j < len; j++) {
        Entry e = parentTable[j];
        if (e != null) {
            // 当条目不为 null 时，从其中获取持有的 ThreadLocal
            @SuppressWarnings("unchecked")
            ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
            if (key != null) {
                // 当 ThreadLocal 存在没有被回收时，获取它的值
                Object value = key.childValue(e.value);
                // 创建新的条目
                Entry c = new Entry(key, value);
                // 计算条目在条目表中的下标
                int h = key.threadLocalHashCode & (len - 1);
                // 如果出现冲突，则需要使用线性探测法找到下一个可用的下标。
                while (table[h] != null)
                    h = nextIndex(h, len);
                // 将条目放到适当的位置
                table[h] = c;
                size++;
            }
        }
    }
}

// 设置 threshold 为容量的 2/3
private void setThreshold(int len) {
    threshold = len * 2 / 3;
}
```

### 3.1.4 方法

#### 3.1.4.1 index
```java
// 获取 i 的下一个下标，相当于 (i + 1) % len。
private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}

// 获取 i 的上一个下标。
private static int prevIndex(int i, int len) {
    return ((i - 1 >= 0) ? i - 1 : len - 1);
}
```

#### 3.1.4.2 清理
```java
// 清除 staleSlot 下标处过时的条目，并进行重新插入右边不为 null 的条目。重新插入过程中遇到的过时条目也会被清理。
// 返回 staleSlot 右边第一个条目为 null 的下标
private int expungeStaleEntry(int staleSlot) {
    Entry[] tab = table;
    int len = tab.length;

    // 先将条目的值置为 null，在将这个条目置为 null
    tab[staleSlot].value = null;
    tab[staleSlot] = null;
    size--;

    // 对 staleSlot 右边所有不为 null 的条目进行重新插入
    Entry e;
    int i;
    for (i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
        ThreadLocal<?> k = e.get();
        if (k == null) {
            // 在重新插入的过程中发现还有过时条目，则进行清理
            e.value = null;
            tab[i] = null;
            size--;
        } else {
            // 计算当前条目的下标
            int h = k.threadLocalHashCode & (len - 1);
            // 如果计算的下标不等于它当前被放置的下标，则需要给它重新找个位置
            if (h != i) {
                tab[i] = null;

                // 与算法 knuth 6.4 R 不同，我们必须一直扫描直到 null，因为可能多个条目已经过时。
                while (tab[h] != null)
                    h = nextIndex(h, len);
                tab[h] = e;
            }
        }
    }
    // 返回 staleSlot 右边第一个为 null 的下标
    return i;
}

// 对所有过时条目进行清理
private void expungeStaleEntries() {
    Entry[] tab = table;
    int len = tab.length;
    for (int j = 0; j < len; j++) {
        Entry e = tab[j];
        if (e != null && e.get() == null)
            expungeStaleEntry(j);
    }
}

/*
启发式扫描某些单元以查找陈旧条目。当添加了新元素或 replaceStaleEntry 方法中清理另一旧元素时，将调用此方法。
它执行对数扫描，这是无扫描（快速但保留垃圾）和扫描全部之间的平衡。扫描全部会发现所有过时条目，
但会导致某些插入花费 O(n) 时间。

 - 参数 i: 一个未过时条目的下标。扫描从 i 之后的元素开始。
 - 参数 n: 扫描控制。如果没找到过时条目，将扫描 log2(n) 个条目；否则将扫描额外的 log2(table.length) - 1 个条目。
   在插入中调用此方法时，此参数是元素数；而在 replaceStaleEntry 方法中调用此方法时，它是表长。
   注意：可以通过加权 n 让它更具探测性，而不是仅使用直接对数 n。但是此版本简单，快速，并且运行良好。
 - 返回: 如果有任何过时条目被删除返回 true
*/
private boolean cleanSomeSlots(int i, int n) {
    boolean removed = false;
    Entry[] tab = table;
    int len = tab.length;
    do {
        i = nextIndex(i, len);
        Entry e = tab[i];
        if (e != null && e.get() == null) {
            // 找到过时条目，将 n 设为表长，此时会扫描额外的 log2(table.length) - 1 个条目
            n = len;
            removed = true;
            // 清除过时元素
            i = expungeStaleEntry(i);
        }
    // 扫描 log2(n) 个条目
    } while ( (n >>>= 1) != 0);
    return removed;
}
```

#### 3.1.4.3 re-hash
```java
// 创建新的条目表，新表是原来的两倍大，然后重新插入所以元素。这个过程中也会清理过时元素
private void resize() {
    Entry[] oldTab = table;
    int oldLen = oldTab.length;
    // 新表是旧表的两倍大
    int newLen = oldLen * 2;
    Entry[] newTab = new Entry[newLen];
    // 由于添加过程中会清理过时条目，count 记录未过时的条目数量
    int count = 0;

    for (int j = 0; j < oldLen; ++j) {
        // 遍历旧表中所有条目，对不为 null 的条目进行操作
        Entry e = oldTab[j];
        if (e != null) {
            ThreadLocal<?> k = e.get();
            if (k == null) {
                // 清理过时条目
                e.value = null; // Help the GC
            } else {
                // 计算在新表中的下标
                int h = k.threadLocalHashCode & (newLen - 1);
                while (newTab[h] != null)
                    h = nextIndex(h, newLen);
                newTab[h] = e;
                count++;
            }
        }
    }

    setThreshold(newLen);
    size = count;
    table = newTab;
}

// 首先扫描整个表，删除过时条目。如果这样空间还不够，则将条目表大小加倍并重排所有元素。
// 这个过程中也会清理过时元素
private void rehash() {
    expungeStaleEntries();

    // 当清除过时元素之后，size 大于等于 0.75 * threshold，就执行 resize，避免滞后
    if (size >= threshold - threshold / 4)
        resize();
}
```

#### 3.1.4.4 get
```java
// 以 ThreadLocal 为键获取对应的 Entry。
// 此方法本身仅处理 fastpath：直接命中。否则，它将调用 getEntryAfterMiss。
// 这是为了最大限度地提高直接命中的性能，部分原因是使此方法易于内联化。
private Entry getEntry(ThreadLocal<?> key) {
    // 计算 key 在条目表中的下标
    int i = key.threadLocalHashCode & (table.length - 1);
    Entry e = table[i];
    if (e != null && e.get() == key)
        // 如果命中，则返回这个 Entry
        return e;
    else
        return getEntryAfterMiss(key, i, e);
}

// 当未命中时，采用线性探测法获取 Entry。
private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
    Entry[] tab = table;
    int len = tab.length;

    while (e != null) {
        ThreadLocal<?> k = e.get();
        // 如果两个 ThreadLocal 是一个对象，则找到并返回
        if (k == key)
            return e;
        if (k == null)
            // 如果 ThreadLocal 为 null，表示条目已过时，需要进行清理
            expungeStaleEntry(i);
        else
            // 线性探测，下标加 1
            i = nextIndex(i, len);
        e = tab[i];
    }
    // 未找到返回 null
    return null;
}
```

#### 3.1.4.5 set
```java
// 设置与键关联的值。
private void set(ThreadLocal<?> key, Object value) {
    /*
    我们没有像 get() 那样使用 fastpath，因为使用 set() 创建新条目和替换现有条目一样频繁，
    在这种情况下，fastpath 失败的频率会更高。
    */

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        // 找到键，设置相关的值
        if (k == key) {
            e.value = value;
            return;
        }

        // 如果键为空，则需要替换过时条目
        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    // 如果找不到 key，则需要创建新的条目
    tab[i] = new Entry(key, value);
    int sz = ++size;
    // 如果清除过时条目后空间仍然不足，就进行 rehash
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}

// 将过时条目替换为指定键的条目。无论指定键的条目是否已存在，都将 value 存储在该条目中。
// 副作用是，此方法删除了“run”中的所有过时条目。（run 是两个过时条目之间的一系列输入）
private void replaceStaleEntry(ThreadLocal<?> key, Object value, int staleSlot) {
    Entry[] tab = table;
    int len = tab.length;
    Entry e;

    // Back up to check for prior stale entry in current run.
    // We clean out whole runs at a time to avoid continual
    // incremental rehashing due to garbage collector freeing
    // up refs in bunches (i.e., whenever the collector runs).
    // 备份 staleSlot 之前的过时条目。我们需要清理 staleSlot 附近的过时条目，避免重复调用此方法。
    int slotToExpunge = staleSlot;
    for (int i = prevIndex(staleSlot, len); (e = tab[i]) != null; i = prevIndex(i, len))
        if (e.get() == null)
            slotToExpunge = i;

    // 查找 key 相等的键或尾随的过时条目，以先找到的为准
    for (int i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
        ThreadLocal<?> k = e.get();

        // 如果我们找到了键，那么我们需要将它与过时的条目交换，以保持哈希表的顺序。然后，
        // 使用 expungeStaleEntry 方法将新的过时条目或其之前遇到的任何其他过时条目删除
        if (k == key) {
            e.value = value;

            // 交换 key 和过时条目的位置
            tab[i] = tab[staleSlot];
            tab[staleSlot] = e;

            // 如果 staleSlot 之前没有过时条目，则从 i 处开始清理；否则从备份的过时条目位置开始清理
            if (slotToExpunge == staleSlot)
                slotToExpunge = i;
            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
            return;
        }

        // 如果 staleSlot 之前没有过时条目，那么备份扫描 key 时看到的第一个过时条目。
        if (k == null && slotToExpunge == staleSlot)
            slotToExpunge = i;
    }

    // 如果找不到 key，则将新条目放入 staleSlot 处。原来的过时条目就被删除了
    tab[staleSlot].value = null;
    tab[staleSlot] = new Entry(key, value);

    // 如果之前发现了其他过时条目，清除它们
    if (slotToExpunge != staleSlot)
        cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
}
```

#### 3.1.4.6 remove
```java
// 清理 key 对应的条目
private void remove(ThreadLocal<?> key) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
    for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
        if (e.get() == key) {
            e.clear();
            expungeStaleEntry(i);
            return;
        }
    }
}
```

## 3.2 SuppliedThreadLocal
```java
// 1.8 引入，为 withInitial 方法使用。创建一个重写 initialValue 方法的 ThreadLocal 子类
static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

    private final Supplier<? extends T> supplier;

    SuppliedThreadLocal(Supplier<? extends T> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    protected T initialValue() {
        return supplier.get();
    }
}
```

# 4. 方法

## 4.1 initialValue
```java
/*
返回此 ThreadLocal 的当前线程“初始值”。除非线程先前调用了 set 方法，否则线程第一次使用 get 方法访问该变量时将调用此方法。
通常，每个线程最多调用一次此方法，但是在随后依次调用 remove 和 get 的情况下，可以再次调用此方法。

这个实现只是返回 null；如果程序员希望线程局部变量具有非 null 的初始值，则必须继承 ThreadLocal 并重写此方法。
通常，使用匿名内部类或者 withInitial 方法。
*/
protected T initialValue() {
    return null;
}
```

## 4.2 withInitial
```java
// 创建 ThreadLocal。初始值是通过在 Supplier 上调用 get 方法确定的。
public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
    return new SuppliedThreadLocal<>(supplier);
}
```

## 4.3 threadMap
```java
// 获取与 ThreadLocal 关联的 ThreadMap。此方法在 InheritableThreadLocal 中重写。
ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

// 创建与 ThreadLocal 关联的 ThreadMap。此方法在 InheritableThreadLocal 中重写。
void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}

// 此方法是创建 InheritableThreadLocal 的 ThreadLocalMap 的工厂方法。
static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
    return new ThreadLocalMap(parentMap);
}
```

## 4.4 get
```java
// 返回此 ThreadLocal 的当前线程副本中的值。如果该变量没有当前线程的值，
// 则返回调用 initialValue 方法返回的值。
public T get() {
    // 获取当前线程
    Thread t = Thread.currentThread();
    // 获取当前线程上绑定的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    // 如果当前线程没有绑定 ThreadLocalMap，或者此线程本地变量不存在，则调用 setInitialValue 方法
    return setInitialValue();
}

private T setInitialValue() {
    // 获得初始值
    T value = initialValue();
    // 获取当前线程
    Thread t = Thread.currentThread();
    // 获取当前线程绑定的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null)
        // map 存在，为当前线程创建和当前 ThreadLocal 关联的局部变量
        map.set(this, value);
    else
        // map 不存在，为当前线程创建 ThreadLocalMap 并设置和当前 ThreadLocal 关联的初始值
        createMap(t, value);
    // 返回初始值
    return value;
}
```

## 4.5 set
```java
public void set(T value) {
    // 获取当前线程和与它绑定的 ThreadLocalMap
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    // 如果 map 非空，设置新的值；否则创建 map 并设置初始值
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
}
```

## 4.6 remove
```java
// 删除当前线程和当前 ThreadLocal 绑定的局部变量。
public void remove() {
    ThreadLocalMap m = getMap(Thread.currentThread());
    if (m != null)
        m.remove(this);
}
```

## 4.7 childValue
```java
/*
childValue 方法在 ThreadLocal 子类 InheritableThreadLocal 中实现。在 ThreadLocal 内部定义是为了提供
createInheritedMap 工厂方法而无需在 InheritableThreadLocal 中子类化 ThreadLocalMap 类。
此技术优于在方法中使用 instanceof 测试。

此方法不能在 ThreadLocal 中使用，否则会抛出 UnsupportedOperationException 异常。
*/
T childValue(T parentValue) {
    throw new UnsupportedOperationException();
}
```


[hash]: 哈希算法.md