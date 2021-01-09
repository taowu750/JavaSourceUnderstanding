`java.util.TimSort`类的声明如下：
```java
class TimSort<T>
```
一种稳定的、自适应的、迭代的归并排序。在部分已排序的数组上运行时，所需的比较少于 Nlog(N)，而在随机数组上运行时，
其性能可与传统的合并排序相当。

这种排序是稳定的，运行时间为 O(Nlog(N))（最坏的情况）。在最坏的情况下，这种排序需要为 n/2 个对象引用提供临时存储空间。
在最佳情况下，它只需要少量常数空间。此实现改编自 Tim Peters 针对`Python`的列表排序，

在此处进行了详细描述：http://svn.python.org/projects/python/trunk/Objects/listsort.txt 。
可以在此处找到`TimSort`的`C`代码：http://svn.python.org/projects/python/trunk/Objects/listobject.c 。

本文介绍了底层技术（可能有更早的起源）：
```
"Optimistic Sorting and Information Theoretic Complexity"
Peter McIlroy
SODA (Fourth Annual ACM-SIAM Symposium on Discrete Algorithms),
pp 467-474, Austin, Texas, 25-27 January 1993.
```

虽然此类的`API`仅由静态方法组成，但它可以（构造器是私有的）实例化。小数组使将用二分插入排序。

`TimSort`可以探测数据中的结构化部分，并采取更高效的查找和合并策略，从而提高性能。

# 1. 成员字段

## 1.1 常量
```java
/*
将被合并的序列的最短长度。更短的序列将通过调用 binarySort 来延长。如果整个数组小于此长度，将不执行合并。

该常数应为 2 的幂。在 Tim Peter 的 C 实现中为 64，但根据经验确定 32 在此实现中效果更好。
在极少数情况下，将此常量设置为不是 2 的幂的数字，则需要更改 minRunLength 方法的计算规则。
如果减小此常数，则必须在 TimSort 构造函数中更改 stackLen 计算，否则可能会引发 ArrayOutOfBound 异常。

请参阅 listsort.txt，以获取所需的最小堆栈长度（取决于要排序的数组的长度和最小合并序列长度）的讨论。
*/
private static final int MIN_MERGE = 32;

/*
当两个游程（一段递增或递减的连续序列）中的某一个连续获胜次数大于 MIN_GALLOP 次，将会进入 gallop 模式。
gallop 模式表示数据可能在某一范围内高度结构化，因此可以使用 gallop 方法加速查找和合并。
获胜指的是：哪个游程获胜了，将此游程的元素复制到合并位置处。

这就是一个阈值，程序根据阈值决定该如何查找和合并，从而获得最大性能
*/
private static final int  MIN_GALLOP = 7;

/*
tmp 数组的最大初始大小，它用于合并。该数组可以增长以适应需求。与 Tim 最初的 C 版本不同，在对较小的数组进行排序时，
我们不会分配太多存储空间。
*/
private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
```

## 1.2 成员变量
```java
// 要排序的数组
private final T[] a;

// 排序的比较器
private final Comparator<? super T> c;

/*
这控制了我们何时进入 gallop 模式。初始化为 MIN_GALLOP。
对于随机数据，mergeLo 和 mergeHi 方法将其增大；对于高度结构化的数据，将其减少。

这是个动态阈值。
*/
private int minGallop = MIN_GALLOP;

// 用于合并的临时存储。可以选择在构造函数中提供一个工作区数组，如果此工作区数组足够大，则将使用它。
private T[] tmp;
private int tmpBase; // tmp 数组的开始
private int tmpLen;  // tmp 数组的长度

/*
尚未合并的待处理的游程堆栈。数组右端是栈顶，左端是栈底。
第 i 个游程从 runBase[i] 开始，具有 runLen[i] 个元素。下列式子总是为 true（只要索引在边界内）：
runBase[i] + runLen[i] == runBase[i + 1]
*/
private int stackSize = 0;
private final int[] runBase;
private final int[] runLen;
```

# 2. 构造器
```java
/*
创建一个 TimSort 实例以维护正在进行的排序状态。

下面三个参数是并发排序中使用的
@param work 工作区数组切片
@param workBase 工作区数组可用范围的开始
@param workLen 工作区数组可用范围的长度
*/
private TimSort(T[] a, Comparator<? super T> c, T[] work, int workBase, int workLen) {
    this.a = a;
    this.c = c;

    // 分配临时存储（如有必要，可在以后增加）
    int len = a.length;
    // 临时存储初始大小：如果原数组长度小于 2 倍的 INITIAL_TMP_STORAGE_LENGTH，则设为原数组长度的一半；
    // 否则设为 INITIAL_TMP_STORAGE_LENGTH
    int tlen = (len < 2 * INITIAL_TMP_STORAGE_LENGTH /* 256 */ ) ?
        len >>> 1 : INITIAL_TMP_STORAGE_LENGTH;
    // work 数组满足要求就使用 work 数组；否则新建临时数组
    if (work == null || workLen < tlen || workBase + tlen > work.length) {
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        T[] newArray = (T[])java.lang.reflect.Array.newInstance
            (a.getClass().getComponentType(), tlen);
        tmp = newArray;
        tmpBase = 0;
        tmpLen = tlen;
    }
    else {
        tmp = work;
        tmpBase = workBase;
        tmpLen = workLen;
    }

    /*
    分配待合并的游程的堆栈（它不能增长）。堆栈长度如 listsort.txt 要求的那样。C 版本总是使用相同的堆栈长度（85），
    但是在 Java 中对“中等大小”数组（例如 100 个元素）进行排序时，这个开销过大。
    因此，对于较小的数组，我们使用较小（但足够大）的堆栈长度。

    如果减少 MIN_MERGE，下面计算中的“魔法数字”必须改变。有关详细信息，请参阅上面的 MIN_MERGE 注释。
    最大值 49 允许最大长度为 Integer.MAX - 4 的数组。如果数组的元素是最坏情形，堆栈元素数量将增长到这么多。

    更多解释见以下第 4 节：
    http://envisage-project.eu/wp-content/uploads/2015/02/sorting.pdf
     */
    int stackLen = (len <    120  ?  5 :
                    len <   1542  ? 10 :
                    len < 119151  ? 24 : 49);
    runBase = new int[stackLen];
    runLen = new int[stackLen];
}
```

# 3. 方法

## 3.1 ensureCapacity
```java
// 确保辅助数组 tmp 至少具有指定数量的元素，并在必要时增加其大小。
// 大小按指数增长，以确保平均线性时间复杂度。
private T[] ensureCapacity(int minCapacity) {
    // 只在容量小于 minCapacity 的时候扩容
    if (tmpLen < minCapacity) {
        // 计算大于 minCapacity 的最小 2 的幂
        int newSize = minCapacity;
        newSize |= newSize >> 1;
        newSize |= newSize >> 2;
        newSize |= newSize >> 4;
        newSize |= newSize >> 8;
        newSize |= newSize >> 16;
        newSize++;

        if (newSize < 0) // 如果溢出的话（虽然不太可能发生）
            newSize = minCapacity;
        else
            // 选择 newSize 和数组一半长度的较小值
            newSize = Math.min(newSize, a.length >>> 1);

        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        T[] newArray = (T[])java.lang.reflect.Array.newInstance
            (a.getClass().getComponentType(), newSize);
        tmp = newArray;
        tmpLen = newSize;
        tmpBase = 0;
    }
    return tmp;
}
```

## 3.2 reverseRange
```java
// 反转指定数组的指定范围。参数 lo 包含，hi 不包含
private static void reverseRange(Object[] a, int lo, int hi) {
    hi--;
    while (lo < hi) {
        Object t = a[lo];
        a[lo++] = a[hi];
        a[hi--] = t;
    }
}
```

## 3.3 countRunAndMakeAscending
```java
/*
返回从指定数组中指定位置开始的游程长度。

如果游程是递减的，则将其反转（确保在方法返回时运行将始终递增）。

游程是一段连续的升序序列：a [lo] <= a [lo + 1] <= a [lo + 2] <= ...；
或连续的降序序列是：a [lo] > a [lo + 1] > a [lo + 2] > ...。

为了在稳定的归并排序如预期那样运行，需要严格定义“降序”，以便调用可以安全地反转降序而不破坏稳定性。
*/
private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi,
                                                Comparator<? super T> c) {
    assert lo < hi;
    int runHi = lo + 1;
    if (runHi == hi)
        return 1;

    // 找到游程的结束位置，如果它是降序就反转它
    if (c.compare(a[runHi++], a[lo]) < 0) { // 降序
        while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0)
            runHi++;
        reverseRange(a, lo, runHi);
    } else {                              // 升序
        while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0)
            runHi++;
    }

    return runHi - lo;
}
```

## 3.4 binarySort
```java
/*
使用二分插入排序对指定数组的指定部分进行排序。这是对少量元素进行排序的最佳方法。
它需要 O(NlogN) 次比较，但是需要 O(n^2) 次数据移动（最坏的情况）。

如果指定范围的起始部分已经排序，则此方法可以利用这个情形：该方法假定从索引 lo（包括）到 start（不包括）的元素已经被排序。

需要注意，lo 是排序范围开始下标（包含），hi 是排序范围结束下标（不包含）
*/
private static <T> void binarySort(T[] a, int lo, int hi, int start,
                                   Comparator<? super T> c) {
    assert lo <= start && start <= hi;
    // start 等于 lo，则让它加 1。头一个元素始终是排序好的
    if (start == lo)
        start++;
    for ( ; start < hi; start++) {
        T pivot = a[start];

        int left = lo;
        int right = start;
        assert left <= right;
        /*
         * 排序元素满足下面的不等式:
         *   pivot >= all in [lo, left).
         *   pivot <  all in [right, start).
         */
        // 使用二分查找确定插入位置
        while (left < right) {
            // 如果溢出，使用 >>> 可以把溢出值当成无符号整数使用
            int mid = (left + right) >>> 1;
            if (c.compare(pivot, a[mid]) < 0)
                right = mid;
            else
                left = mid + 1;
        }
        assert left == right;

        /*
        不等式仍然成立：
            pivot >= all in [lo, left).
            pivot <  all in [right, start).

        注意，如果有等于 pivot 的元素，left 将指向它们后面的第一个位置——这就是为什么这种排序是稳定的。
        移动元素以腾出空间放置 pivot。
         */
        int n = start - left;  // 需要移动的元素数目
        // 对少量移动元素的情况进行优化
        switch (n) {
            case 2:  a[left + 2] = a[left + 1];
            case 1:  a[left + 1] = a[left]; // 注意，case 2 和 case 1 之间没有 break
            break;
            default: System.arraycopy(a, left, a, left + 1, n);
        }
        // 将 pivot 放到合适位置
        a[left] = pivot;
    }
}
```

## 3.5 minRunLength
```java
/*
返回指定长度的数组的最小可接受游程长度。序列中原始游程长度短于这个返回值的，将会使用 binarySort 扩展长度。

如果 n < MIN_MERGE，则返回 n；
如果 n 为 2 的幂，则返回 MIN_MERGE / 2；
否则返回 k，其中 MIN_MERGE / 2 <= k <= MIN_MERGE。这样 n / k 将接近但严格小于 2 的幂。

有关原理，请参阅 listsort.txt。
*/
private static int minRunLength(int n) {
    assert n >= 0;
    int r = 0;
    while (n >= MIN_MERGE) {
        r |= (n & 1);  // 如果在 n 移位的过程中将任何末尾的 1 移掉，r 变为 1
        n >>= 1;  // n 除以 2
    }
    return n + r;
}
```

## 3.6 pushRun
```java
// 将指定的游程压入到运行堆栈中。
private void pushRun(int runBase, int runLen) {
    this.runBase[stackSize] = runBase;
    this.runLen[stackSize] = runLen;
    stackSize++;
}
```

## 3.7 gallop
```java
/*
找到将指定键插入到指定范围（已排序）内的位置；如果范围包含等于 key 的元素，则返回最左边相等元素的索引。

下面的描述中另 n = len，b = base。

参数 hint 是开始搜索的索引，0 <= hint < n。hint 越接近结果，该方法将运行得越快。
此方法使用二次探测法来缩小查找范围。

返回 k，0 <= k <= n，使得 a[b + k-1] < key <= a[b + k]，假设 a[b-1] 为负无穷大，而 a[b + n] 是无穷大。
换句话说，a 的前 k 个元素应在 key 之前，而后 n-k 个元素应在 key 之后。
*/
private static <T> int gallopLeft(T key, T[] a, int base, int len, int hint,
                                  Comparator<? super T> c) {
    assert len > 0 && hint >= 0 && hint < len;
    int lastOfs = 0;  // 上一个偏移量
    int ofs = 1;  // 偏移量
    // 如果 key > a[base + hint]
    if (c.compare(key, a[base + hint]) > 0) {
        // 在 hint 右边进行搜索，直到 a[base+hint+lastOfs] < key <= a[base+hint+ofs]
        int maxOfs = len - hint;
        // 使用二次探测法缩小插入范围
        while (ofs < maxOfs && c.compare(key, a[base + hint + ofs]) > 0) {
            lastOfs = ofs;
            ofs = (ofs << 1) + 1;  // offset 乘 2 加 1
            if (ofs <= 0)   // 如果 offset 溢出，设为 maxOfs
                ofs = maxOfs;
        }
        if (ofs > maxOfs)
            ofs = maxOfs;

        // 让偏移量相对于 base 偏移
        lastOfs += hint;
        ofs += hint;

    // 如果 key <= a[base + hint]
    } else {
        // 在 hint 左边进行搜索，直到 a[base+hint-ofs] < key <= a[base+hint-lastOfs]
        final int maxOfs = hint + 1;
        while (ofs < maxOfs && c.compare(key, a[base + hint - ofs]) <= 0) {
            lastOfs = ofs;
            ofs = (ofs << 1) + 1;
            if (ofs <= 0)   // 如果 offset 溢出，设为 maxOfs
                ofs = maxOfs;
        }
        if (ofs > maxOfs)
            ofs = maxOfs;

        // 让偏移量相对于 base 偏移
        int tmp = lastOfs;
        lastOfs = hint - ofs;
        ofs = hint - tmp;
    }
    assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

    /*
    现在有 a[base+lastOfs] < key <= a[base+ofs]。
    执行二分查找，保证 a[base + lastOfs - 1] < key <= a[base + ofs]
     */
    lastOfs++;
    while (lastOfs < ofs) {
        int m = lastOfs + ((ofs - lastOfs) >>> 1);

        if (c.compare(key, a[base + m]) > 0)
            lastOfs = m + 1;  // a[base + m] < key
        else
            // 注意不能是 ofs = m - 1。因为如果有等于 key 的元素，我们要找到最左边相等元素
            ofs = m;          // key <= a[base + m]
    }
    assert lastOfs == ofs;    // so a[base + ofs - 1] < key <= a[base + ofs]
    return ofs;
}

// 和 gallopLeft 类似，只不过如果范围包含等于 key 的元素，则返回最右边相等元素的索引
private static <T> int gallopRight(T key, T[] a, int base, int len,
                                   int hint, Comparator<? super T> c) {
    assert len > 0 && hint >= 0 && hint < len;

    int ofs = 1;
    int lastOfs = 0;
    if (c.compare(key, a[base + hint]) < 0) {
        // 在 hint 左边进行搜索，直到 a[b+hint - ofs] <= key < a[b+hint - lastOfs]
        int maxOfs = hint + 1;
        while (ofs < maxOfs && c.compare(key, a[base + hint - ofs]) < 0) {
            lastOfs = ofs;
            ofs = (ofs << 1) + 1;
            if (ofs <= 0)   // int overflow
                ofs = maxOfs;
        }
        if (ofs > maxOfs)
            ofs = maxOfs;

        // Make offsets relative to b
        int tmp = lastOfs;
        lastOfs = hint - ofs;
        ofs = hint - tmp;
    } else { // a[b + hint] <= key
        // 在 hint 右边进行搜索，直到 a[b+hint + lastOfs] <= key < a[b+hint + ofs]
        int maxOfs = len - hint;
        while (ofs < maxOfs && c.compare(key, a[base + hint + ofs]) >= 0) {
            lastOfs = ofs;
            ofs = (ofs << 1) + 1;
            if (ofs <= 0)   // int overflow
                ofs = maxOfs;
        }
        if (ofs > maxOfs)
            ofs = maxOfs;

        // Make offsets relative to b
        lastOfs += hint;
        ofs += hint;
    }
    assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

    /*
     * 现在有 a[b + lastOfs] <= key < a[b + ofs]。
     * 执行二分查找，在这个过程中保证 a[b + lastOfs - 1] <= key < a[b + ofs].
     */
    lastOfs++;
    while (lastOfs < ofs) {
        int m = lastOfs + ((ofs - lastOfs) >>> 1);

        if (c.compare(key, a[base + m]) < 0)
            ofs = m;          // key < a[b + m]
        else
            lastOfs = m + 1;  // a[b + m] <= key
    }
    assert lastOfs == ofs;    // so a[b + ofs - 1] <= key < a[b + ofs]
    return ofs;
}
```

## 3.8 合并游程
```java
/*
检查等待合并的游程堆栈并合并相邻的游程，直到重新满足以下不等式为止：
1. runLen[i-3] > runLen[i-2] + runLen[i-1]
2. runLen[i-2] > runLen[i-1]

每次将新的游程压入堆栈时都会调用此方法，因此在调用此方法时，保证 i < stackSize。

这看起来像是斐波那契数列
*/
private void mergeCollapse() {
    // 如果游程数量大于 1
    while (stackSize > 1) {
        int n = stackSize - 2;
        // 如果不满足不等式 runLen[i-3] > runLen[i-2] + runLen[i-1]
        if (n > 0 && runLen[n-1] <= runLen[n] + runLen[n+1]) {
            // 如果 runLen[i-3] 小于 runLen[i-1]，则合并 run[i-3] 和 run[i-2]；
            // 否则合并 run[i-2] 和 run[i-1]
            if (runLen[n - 1] < runLen[n + 1])
                n--;
            mergeAt(n);

        // 如果不满足不等式 runLen [i-2] > runLen [i-1]
        } else if (runLen[n] <= runLen[n + 1]) {
            // 合并 run[i-2] 和 run[i-1]
            mergeAt(n);
        
        // 否则，满足不等式则直接退出循环
        } else {
            break;
        }

        // 如果没有 break，则合并了两个游程，stackSize 减 1
    }
}

/*
合并堆栈索引 i 和 i + 1 处的游程。

游程 i 必须是栈中倒数第二或倒数第三个游程。换句话说，i 必须等于 stackSize-2 或 stackSize-3。
*/
private void mergeAt(int i) {
    assert stackSize >= 2;
    assert i >= 0;
    assert i == stackSize - 2 || i == stackSize - 3;

    int base1 = runBase[i];
    int len1 = runLen[i];
    int base2 = runBase[i + 1];
    int len2 = runLen[i + 1];
    assert len1 > 0 && len2 > 0;
    assert base1 + len1 == base2;

    /*
    写入合并的游程的长度；如果 i 是倒数第三个游程，
    将栈中倒数第一个游程记录移到倒数第二个游程记录处
     */
    runLen[i] = len1 + len2;
    if (i == stackSize - 3) {
        runBase[i + 1] = runBase[i + 2];
        runLen[i + 1] = runLen[i + 2];
    }
    stackSize--;

    /*
    找到 run2 的第一个元素在 run1 中的位置。这样 run1 中先前的元素可以忽略。
     */
    int k = gallopRight(a[base2], a, base1, len1, 0, c);
    assert k >= 0;
    base1 += k;
    len1 -= k;
    // 如果 run2 的第一个元素大于等于 run1 的最大元素，则不需要合并这两个游程
    if (len1 == 0)
        return;

    /*
    查找 run1 的最后一个元素在 run2 中的位置。这样 run2 中的后续元素可以忽略。
     */
    len2 = gallopLeft(a[base1 + len1 - 1], a, base2, len2, len2 - 1, c);
    assert len2 >= 0;
    // 如果 run1 的最后一个元素小于等于 run2 的最小元素，则不需要合并这两个游程
    if (len2 == 0)
        return;

    // 合并游程。此时 run1 的第一个元素大于 run2 的第一个元素，
    // 且 run1 的最后一个元素大于 run2 的所有元素
    if (len1 <= len2)
        mergeLo(base1, len1, base2, len2);
    else
        mergeHi(base1, len1, base2, len2);
}

/*
以稳定的方式合并两个相邻的游程。第一个游程的第一个元素必须大于第二个游程的第一个元素（a[base1] > a[base2]），
并且第一个游程的最后一个元素（a[base1 + len1-1]）必须大于大于第二个游程的所有元素。

为了提高性能，仅当 len1 <= len2 时才应调用此方法。如果 len1 >= len2，则应调用 mergeHi。
（如果 len1 == len2，则可以调用这两种方法。）
*/
private void mergeLo(int base1, int len1, int base2, int len2) {
    assert len1 > 0 && len2 > 0 && base1 + len1 == base2;

    // 将第一个游程复制到 tmp
    T[] a = this.a; // 避免 getfield 指令调用
    // 确保 tmp 的容量
    // 由于 len1 小于等于 len2，所以最大为数组长度一半的 tmp 足够使用
    T[] tmp = ensureCapacity(len1);
    int cursor1 = tmpBase; // cursor1 是 tmp 的下标
    int cursor2 = base2;   // cursor2 是第二个游程的下标
    int dest = base1;      // dest 是合并位置的下标
    System.arraycopy(a, base1, tmp, cursor1, len1);

    // 将第二个游程的第一个元素移动到第一个游程的开始位置。注意第一个游程已被复制在 tmp 中
    a[dest++] = a[cursor2++];
    // 如果第二个游程只有一个元素
    if (--len2 == 0) {
        System.arraycopy(tmp, cursor1, a, dest, len1);
        return;
    }
    // 如果第一个游程只有一个元素
    if (len1 == 1) {
        System.arraycopy(a, cursor2, a, dest, len2);
        a[dest + len2] = tmp[cursor1]; // 将第一个游程的最后一个元素放在合并的序列末尾
        return;
    }

    Comparator<? super T> c = this.c;  // 避免 getfield 指令调用
    int minGallop = this.minGallop;
    outer:
        while (true) {
            int count1 = 0; // 第一个游程连续获胜的次数
            int count2 = 0; // 第二个游程连续获胜的次数
            // 哪个游程获胜了，将此游程的元素复制到合并位置处。

            /*
             * 一直运行，直到某个游程持续获胜次数大于等于 minGallop。
             */
            do {
                assert len1 > 1 && len2 > 0;
                // 如果第二个游程的元素小于第一个游程的元素
                if (c.compare(a[cursor2], tmp[cursor1]) < 0) {
                    // 将第二个游程的元素复制到合并位置
                    a[dest++] = a[cursor2++];
                    // 更新连续获胜次数
                    count2++;
                    count1 = 0;
                    // 第二个游程已经没有元素了，跳出循环
                    if (--len2 == 0)
                        break outer;

                // 否则第一个游程的元素小于等于第二个游程的元素
                } else {
                    // 将第一个游程的元素复制到合并位置
                    a[dest++] = tmp[cursor1++];
                    // 更新连续获胜次数
                    count1++;
                    count2 = 0;
                    // 第一个游程只有一个元素了，跳出循环
                    if (--len1 == 1)
                        break outer;
                }
            } while ((count1 | count2) < minGallop);

            /*
            如果一个游程一直获胜，表示数据可能是结构化的。
            此时我们可以试着使用 gallop，直到两个游程不再一直获胜
             */
            do {
                assert len1 > 1 && len2 > 0;
                // 找到当前第二个游程最小元素在第一个游程中的位置，使用 gallopRight
                // 它也是第一个游程的连续获胜次数
                count1 = gallopRight(a[cursor2], tmp, cursor1, len1, 0, c);
                if (count1 != 0) {
                    // 将第一个游程中小于等于第二个游程最小元素的所有元素移到合并位置处
                    System.arraycopy(tmp, cursor1, a, dest, count1);
                    dest += count1;
                    cursor1 += count1;
                    len1 -= count1;
                    if (len1 <= 1) // len1 == 1 || len1 == 0
                        break outer;
                }
                // 将当前第二个游程最小元素移到合并位置
                a[dest++] = a[cursor2++];
                if (--len2 == 0)
                    break outer;

                // 找到当前第一个游程最小元素在第二个游程中的位置，使用 gallopLeft
                // 它也是第二个游程的连续获胜次数
                count2 = gallopLeft(tmp[cursor1], a, cursor2, len2, 0, c);
                if (count2 != 0) {
                    // 将第二个游程中小于第一个游程最小元素的所有元素移到合并位置处
                    System.arraycopy(a, cursor2, a, dest, count2);
                    dest += count2;
                    cursor2 += count2;
                    len2 -= count2;
                    if (len2 == 0)
                        break outer;
                }
                // 将当前第一个游程最小元素移到合并位置
                a[dest++] = tmp[cursor1++];
                if (--len1 == 1)
                    break outer;
                // 结构化数据，减小 minGallop
                minGallop--;

            // 当两个游程的连续获胜次数都大于等于固定阈值 MIN_GALLOP 时，表示数据高度结构化，
            // 将一直处于 gallop 模式
            } while (count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
            // 遇到随机数据，退出 gallop 模式

            if (minGallop < 0)
                minGallop = 0;
            // 增大下次进入 gallop 模式的阈值
            minGallop += 2;
        }  // End of "outer" loop
    
    this.minGallop = minGallop < 1 ? 1 : minGallop;  // Write back to field

    // 如果第一个游程只剩一个元素
    if (len1 == 1) {
        assert len2 > 0;
        // 将第二个游程的剩余元素复制到合并位置处
        System.arraycopy(a, cursor2, a, dest, len2);
        // 将第一个游程的最后一个元素放到合并后序列的末尾，注意此元素大于所有第二个游程的元素
        a[dest + len2] = tmp[cursor1]; 

    // 如果第一个游程只有 0 个元素
    } else if (len1 == 0) {
        // 这表示第一个游程的最后一个元素不大于大于第二个游程的所有元素，抛出异常
        throw new IllegalArgumentException(
            "Comparison method violates its general contract!");

    // 否则第二个游程中的元素全部都防止好了
    } else {
        assert len2 == 0;
        assert len1 > 1;
        // 将第一个游程的剩余元素复制到合并后序列的末尾
        System.arraycopy(tmp, cursor1, a, dest, len1);
    }
}

/*
此方法和 mergeLo 类似。不同之处在于仅当 len1> = len2 时才应调用此方法。

由于它的实现与 mergeLo 高度相似，在此不再列出
*/
private void mergeHi(int base1, int len1, int base2, int len2);

// 合并堆栈上的所有游程，直到只剩下一个。此方法在排序的最后调用一次
private void mergeForceCollapse() {
    while (stackSize > 1) {
        int n = stackSize - 2;
        if (n > 0 && runLen[n - 1] < runLen[n + 1])
            n--;
        mergeAt(n);
    }
}
```
有关`getfield`参见 [避免getfield频繁调用.md][getfield]。

## 3.9 sort
```java
/*
对给定范围进行排序，并在可能的情况下使用给定的工作区数组作为临时存储。
此方法设计为在执行任何必要的数组边界检查并将参数扩展为所需形式之后，从公共方法（在 Arrays 类中）调用。

需要注意，lo 是排序范围开始下标（包含），hi 是排序范围结束下标（不包含）
*/
static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c,
                     T[] work, int workBase, int workLen) {
    assert c != null && a != null && lo >= 0 && lo <= hi && hi <= a.length;

    int nRemaining  = hi - lo;
    // 长度为 0 或 1 的数组不需要排序
    if (nRemaining < 2)
        return;  

    // 如果数组长度小于 MIN_MERGE，使用不需要归并的 "mini-TimSort"
    if (nRemaining < MIN_MERGE /* 32 */ ) {
        // 初始游程长度
        int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
        // 使用二分插入排序
        binarySort(a, lo, hi, lo + initRunLen, c);
        return;
    }

    /*
    从左到右遍历数组一次，查找游程，将低于最短长度的游程扩展到要求的长度，并合并游程以保持堆栈不变。
     */
    TimSort<T> ts = new TimSort<>(a, c, work, workBase, workLen);
    // 最小可接受游程长度
    int minRun = minRunLength(nRemaining);
    do {
        // 找到下一个游程，其中 lo 时这个游程的开始下标，runLen 是这个游程的长度
        int runLen = countRunAndMakeAscending(a, lo, hi, c);

        // 如果游程太短，将其扩展为 minRun 和 nRemaining 之间的较小值
        if (runLen < minRun) {
            int force = nRemaining <= minRun ? nRemaining : minRun;
            // 使用 binarySort 进行扩展
            binarySort(a, lo, lo + force, lo + runLen, c);
            runLen = force;
        }

        // 将这个游程压入栈中
        ts.pushRun(lo, runLen);
        // 在必要的时候合并栈中相邻的游程
        ts.mergeCollapse();

        // 找到下一个游程
        lo += runLen;
        nRemaining -= runLen;
    } while (nRemaining != 0);

    assert lo == hi;
    // 合并所有剩余的游程
    ts.mergeForceCollapse();
    assert ts.stackSize == 1;
}
```

[getfield]: ../lang/避免getfield频繁调用.md