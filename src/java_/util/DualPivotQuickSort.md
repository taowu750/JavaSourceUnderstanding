`java.util.DualPivotQuicksort`类的声明如下：
```java
final class DualPivotQuicksort
```
此类实现了 Vladimir Yaroslavskiy，Jon Bentley 和 Josh Bloch的`Dual-Pivot Quicksort`算法。
该算法可在许多数据集上提供 O(NlogN) 性能，并且通常比传统的（一个切分点）`Quicksort`实现要快。
此类是包私有的，旨在在执行任何必要的数组边界检查并将参数变成此类所需形式之后从公共方法（在`Arrays`类中）调用。

`Dual-Pivot`快速排序算法使用两个切分点把数组分成三份排序。如果按照元素比较次数来算的话，`Dual-Pivot`比经典快排要多:
```
1.7043NlogN VS 1.5697NlogN
```

首先需要知道 CPU 和内存的速度是不匹配的，距统计在过去的 25 年里面，CPU 的速度平均每年增长 46%, 而内存的带宽每年只增长 37%，
经过 25 年的这种不均衡发展，它们之间的差距已经很大了。假如这种不均衡持续持续发展，有一天 CPU 速度再增长也不会让程序变得更快，
因为 CPU 始终在等待内存传输数据，这就是传说中**内存墙(Memory Wall)**。

光比较**元素比较次数**这种计算排序算法复杂度的方法已经无法客观的反映算法优劣了，我们还要计算**扫描元素个数**。
我们把对于数组里面一个元素的访问 `array[i]` 称为一次扫描。但是对于同一个下标，并且对应的值也不变得话，
即使访问多次我们也只算一次。而且我们不管这个访问到底是读还是写。而这个所谓的扫描元素个数反应的是 CPU 与内存之间的数据流量的大小。

因为内存比较慢，统计 CPU 与内存之间的数据流量的大小也就把这个比较慢的内存的因素考虑进去了，
因此也就比元素比较次数更能体现算法在当下计算机里面的性能指标。在这种新的算法下面`Dual-Pivot`快排和经典快排的扫描元素个数分别为:
```
1.4035NlogN VS 1.5697NlogN
```
在这种新的算法下面，`Dual-Pivot`快排比经典快排节省了 12% 的元素扫描，从实验来看节省了 10% 的时间。

有关`Dual-Pivot`快排的核心思想简单实现参见 [DualPivotQuickSortSimpleImpl.java][dual-pivot-qs]。

有关`Dual-Pivot`快排的思想论述参见 [Why Is Dual-Pivot Quicksort Fast?][why-fast] 这篇论文。

# 1. 成员字段
```java
// 归并排序中允许的最多游程数量。游程是一段升序或降序序列
private static final int MAX_RUN_COUNT = 67;

// 归并排序中允许的相等序列最大长度
private static final int MAX_RUN_LENGTH = 33;

// 如果要排序的数组长度小于等于这个值，使用快速排序替代归并排序
private static final int QUICKSORT_THRESHOLD = 286;

// 如果要排序的数组长度小于这个值，使用插入排序替代快速排序
private static final int INSERTION_SORT_THRESHOLD = 47;

// 如果要排序的 byte 数组长度大于这个值，使用计数排序替代插入排序
private static final int COUNTING_SORT_THRESHOLD_FOR_BYTE = 29;

// 如果要排序的 short、char 数组长度大于这个值，使用基数排序（又叫计数排序、桶排序）替代快速排序
private static final int COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR = 3200;

// 不同 short 值的数量
private static final int NUM_SHORT_VALUES = 1 << 16;

// 不同 char 值的数量
private static final int NUM_CHAR_VALUES = 1 << 16;

// 不同 byte 值的数量
private static final int NUM_BYTE_VALUES = 1 << 8;
```
`long`数组的归并排序和`int`数组相同，不再列出。

# 2. 方法

## 2.1 int 数组归并排序
```java
/*
对 int 数组的 [left, right] 范围元素进行归并排序。

当数组长度小于等于 QUICKSORT_THRESHOLD、不是结构化的、或重复元素序列较长，则切换到快速排序。
结构化指数组是由一些升序、降序序列构成的，不是随机的。

此归并排序是自底向上的，不是递归式的自顶向下。

下面三个参数是并发排序中使用的
@param work 工作区数组切片
@param workBase 工作区数组可用范围的开始
@param workLen 工作区数组可用范围的长度
*/
static void sort(int[] a, int left, int right, int[] work, int workBase, int workLen) {
    // 小于等于 QUICKSORT_THRESHOLD，使用快速排序替代归并排序
    if (right - left < QUICKSORT_THRESHOLD /* 286 */ ) {
        sort(a, left, right, true);
        return;
    }

    /*
     * run[i] 是第 i 个游程的开始下标（游程是一段连续的升序或降序序列）
     */
    // 注意游程数组长度为 MAX_RUN_COUNT + 1
    int[] run = new int[MAX_RUN_COUNT /* 67 */ + 1];
    int count = 0; run[0] = left;

    // 检查数组是不是近似排序
    for (int k = left; k < right; run[count] = k) {
        if (a[k] < a[k + 1]) { // 如果是升序
            // 找到升序序列最后的位置（如果一直到数组最后一个元素都在这个序列中，则 k = right + 1）
            while (++k <= right && a[k - 1] <= a[k]);
        } else if (a[k] > a[k + 1]) { // 如果是降序
            // 找到降序序列最后的位置
            while (++k <= right && a[k - 1] >= a[k]);
            // 将这个降序序列反转，变成升序序列
            for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                int t = a[lo]; a[lo] = a[hi]; a[hi] = t;
            }
        } else { // 如果出现相等元素
            // 循环查找相等序列
            for (int m = MAX_RUN_LENGTH /* 33 */; ++k <= right && a[k - 1] == a[k]; ) {
                if (--m == 0) {
                    // 如果这个相等序列长度等于 MAX_RUN_LENGTH，切换到快速排序
                    sort(a, left, right, true);
                    return;
                }
            }
        }

        /*
         * 如果游程个数达到 MAX_RUN_COUNT 个，表示数组不是结构化的，切换到快速排序。
         */
        if (++count == MAX_RUN_COUNT) {
            sort(a, left, right, true);
            return;
        }
    }
    // 处理完成，总共会有 count + 1 个序列。除了下面第一个 if 中的特殊情况，
    // 其他情况下最后一个游程开始下标 run[count] = right + 1，包含 0 个元素。
    // run[count] 是一个哨兵值，为了简化归并时的判断。除了它，实际的序列个数是 count 个。

    // 检查特殊情况
    // 注意：变量 right 此时已经加 1
    if (run[count] == right++) { 
        // 如果最后一个游程只包含一个元素。增加一个游程，这个最后的游程包含 0 个元素。
        // 增加这个 0 元素的游程是为了和一般情况保持一致。
        run[++count] = right;
    } else if (count == 1) { // 如果数组已经有序，直接返回
        return;
    }

    // 确定归并开始时怎么交替排序的数组和辅助数组，通过交替操作可以避免数组复制。
    // 归并排序的运行轨迹可以视作一颗树，⌈log(count)⌉ 是树高：
    // - 当这颗树高为偶数时，最后一次归并会将源数组归并到辅助数组，然后交替。这样开始时不需要交替，odd 为 1；
    // - 当这颗树高为奇数时，最后一次归并会将辅助数组归并到源数组，然后交替。这样开始时需要交替，odd 为 0。
    byte odd = 0;
    for (int n = 1; (n <<= 1) < count; odd ^= 1);

    // 归并排序需要辅助数组。如果工作区数组参数 work 不为 null 且 workLen 大于等于需要排序范围长度，
    // 使用 work；否则新建一个辅助数组
    int[] b;                 // 临时数组，和 a 进行交替
    int ao, bo;              // 以 left 开始的数组偏移量
    int blen = right - left; // 辅助数组大小，和排序范围长度相等
    // work 不满足条件，新建一个辅助数组
    if (work == null || workLen < blen || workBase + blen > work.length) {
        work = new int[blen];
        workBase = 0;
    }
    // 如果 odd 等于 0，交换 a 和辅助数组
    if (odd == 0) {
        // 将 a 中内容复制到辅助数组
        System.arraycopy(a, left, work, workBase, blen);
        // 令 b 等于原数组，a 等于辅助数组
        b = a;
        bo = 0;
        a = work;
        // 减以 left，因为在归并操作中要将这个偏移量加上源数组中的下标，而此下标从 left 开始
        ao = workBase - left;
    } else {
        // 令 b 等于辅助数组，a 保持不变
        b = work;
        ao = 0;
        bo = workBase - left;
    }

    // 自底向上的归并
    for (int last; count > 1; count = last) {
        // 每次选择两个游程（游程之前已经处理过了，都是升序序列），进行归并
        for (int k = (last = 0) + 2; k <= count; k += 2) {
            // 找到两个序列的最右边位置（第二个游程的结束位置或哨兵值，不包括）和中间位置（第二个游程的开始位置，包括）
            // 哨兵值，也就是 right
            int hi = run[k], mi = run[k - 1];
            // 将 a 归并到 b
            for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
                if (q >= hi || p < mi && a[p + ao] <= a[q + ao]) {
                    b[i + bo] = a[p++ + ao];
                } else {
                    b[i + bo] = a[q++ + ao];
                }
            }
            // 两个序列已经归并完毕，将下一个序列的开头设为原来后一个序列的开头
            run[++last] = hi;
        }
        // 如果 count 是奇数，表示有奇数个序列，那么就会剩下最后一个序列不会和其它序列归并
        if ((count & 1) != 0) {
            // 将这个剩下的序列从 a 复制到 b
            for (int i = right, lo = run[count - 1]; --i >= lo;
                b[i + bo] = a[i + ao]
            );
            // 设置哨兵值
            run[++last] = right;
        }
        // 交换 a、b，这样就避免了数组复制
        int[] t = a; a = b; b = t;
        int o = ao; ao = bo; bo = o;
    }
}
```
整个实现中的思路是，首先检查数组的长度，比一个阈值小的时候直接使用`Dual-Pivot`快排。其它情况下，先检查数组中数据的顺序连续性。
把数组中连续升序或者连续降序的信息记录下来，顺便把连续降序的部分倒置。这样数据就被切割成一段段连续升序的数列。

如果顺序连续性好，直接使用`TimSort`算法。`TimSort`算法的核心在于利用数列中的原始顺序，所以可以提高很多效率。
这里的`TimSort`算法是 [TimSort.md][tim-sort] 的精简版，剪掉了动态阈值的那一部分。

## 2.2 int 数组 Dual-Pivot 快排
```java
/*
通过 Dual-Pivot 快排对指定范围的数组进行排序。

@param left 要排序的第一个元素的索引（包含）
@param right 要排序的最后一个元素的索引（包含）
@param leftmost 指示此排序部分是否在整个排序范围的最左边
*/
private static void sort(int[] a, int left, int right, boolean leftmost) {
    int length = right - left + 1;

    // 小于 INSERTION_SORT_THRESHOLD，使用插入排序替代快速排序
    if (length < INSERTION_SORT_THRESHOLD /* 47 */) {
        if (leftmost) {
            // 传统的（没有哨兵）插入排序，针对服务器 VM 进行了优化，用于最左边的部分。
            for (int i = left, j = i; i < right; j = ++i) {
                int ai = a[i + 1];
                while (ai < a[j]) {
                    a[j + 1] = a[j];
                    if (j-- == left) {
                        break;
                    }
                }
                a[j + 1] = ai;
            }
        } else {
            // 当不是最左边的部分，此部分的元素都大于等于左边相邻的部分

            // 跳过前面的升序序列。此升序序列长度大于等于 1
            do {
                if (left >= right) {
                    return;
                }
            } while (a[++left] >= a[left - 1]);

            /*
            相邻部分的每一个元素都扮演着哨兵的角色，因此这允许我们在每次迭代中避免左范围检查。
            此外，我们使用了更优化的算法，即所谓的成对插入排序，它比传统的插入排序实现更快（在快速排序的上下文中）。
             */
            for (int k = left; ++left <= right; k = ++left) {
                // 取相邻的一对元素
                int a1 = a[k], a2 = a[left];

                // 当 a1 小于 a2 时，交换它们。保证 a1 总是大于等于 a2
                if (a1 < a2) {
                    a2 = a1; a1 = a[left];
                }
                // 注意，之前跳过了一段升序序列。现在我们要把 a1 插入到升序序列中。
                // 由于哨兵的存在，不用担心数组越界
                while (a1 < a[--k]) {
                    // 将升序序列中的元素往后移两个位置
                    a[k + 2] = a[k];
                }
                // 将 a1 放到合适位置
                a[++k + 1] = a1;

                // 从上面移动的终止处开始，继续查找 a2 的位置
                while (a2 < a[--k]) {
                    a[k + 1] = a[k];
                }
                a[k + 1] = a2;
            }
            int last = a[right];

            // 如果排序部分长度是奇数，那么还剩下最后一个待排序元素
            while (last < a[--right]) {
                a[right + 1] = a[right];
            }
            a[right + 1] = last;

            // 可以看出成对插入排序的每次先插入较大的数，这样我们就可以确定另一个数肯定在插入位置的左侧，
            // 这种处理方式有点像 TimSort 排序中合并相邻部分的处理，先找到左边最大数在右边的位置，
            // 再找到右边最小数在左边的位置，然后只需合并这重合的部分就行了。因此它比传统的插入排序实现更快。
        }
        return;
    }

    // length / 7 的估计值
    int seventh = (length >> 3) + (length >> 6) + 1;

    // 分别选择 5 个点：e1、e2、e3、e4、e5。间隔相同且均匀分布在排序范围内。
    int e3 = (left + right) >>> 1;  // 中点
    int e2 = e3 - seventh;
    int e1 = e2 - seventh;
    int e4 = e3 + seventh;
    int e5 = e4 + seventh;

    // 使用插入排序对 5 个元素进行排序
    if (a[e2] < a[e1]) { int t = a[e2]; a[e2] = a[e1]; a[e1] = t; }

    if (a[e3] < a[e2]) { int t = a[e3]; a[e3] = a[e2]; a[e2] = t;
        if (t < a[e1]) { a[e2] = a[e1]; a[e1] = t; }
    }
    if (a[e4] < a[e3]) { int t = a[e4]; a[e4] = a[e3]; a[e3] = t;
        if (t < a[e2]) { a[e3] = a[e2]; a[e2] = t;
            if (t < a[e1]) { a[e2] = a[e1]; a[e1] = t; }
        }
    }
    if (a[e5] < a[e4]) { int t = a[e5]; a[e5] = a[e4]; a[e4] = t;
        if (t < a[e3]) { a[e4] = a[e3]; a[e3] = t;
            if (t < a[e2]) { a[e3] = a[e2]; a[e2] = t;
                if (t < a[e1]) { a[e2] = a[e1]; a[e1] = t; }
            }
        }
    }

    // Pointers
    // Dual-Pivot 快排使用两个切分点，把排序部分切分为三块
    int less  = left;  // 中间块的第一个元素的索引
    int great = right; // 右边块第一个元素之前的索引

    if (a[e1] != a[e2] && a[e2] != a[e3] && a[e3] != a[e4] && a[e4] != a[e5]) {
        // 如果 5 个元素互不相同

        /*
        使用五个排序元素中的第二个和第四个作为切分值，这样把数组近似分成相等的三块。
        请注意，pivot1 < pivot2。
         */
        int pivot1 = a[e2];
        int pivot2 = a[e4];

        /*
        要排序的第一个和最后一个元素被移动到以前由切分值占据的位置。当分区完成时，切分值被交换回它们的最终位置，
        并不再用于之后的排序。
         */
        a[e2] = a[left];
        a[e4] = a[right];

        // 跳过小于或大于切分值的元素
        while (a[++less] < pivot1);
        while (a[--great] > pivot2);

        /*
         * 切分:
         *
         *   left part           center part                   right part
         * +--------------------------------------------------------------+
         * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
         * +--------------------------------------------------------------+
         *               ^                          ^       ^
         *               |                          |       |
         *              less                        k     great
         *
         * 排序的元素满足下列不等式:
         *
         *              all in (left, less)   < pivot1
         *    pivot1 <= all in [less, k)     <= pivot2
         *              all in (great, right) > pivot2
         *
         * k 是 ?-部分 的第一个元素的下标
         */
        outer:
        for (int k = less - 1; ++k <= great; ) {
            int ak = a[k];
            // 如果 a[k] 小于 pivot1，交换到左边部分。
            if (ak < pivot1) {
                a[k] = a[less];
                /*
                 * Here and below we use "a[i] = b; i++;" instead
                 * of "a[i++] = b;" due to performance issue.
                 */
                // JDK 代码注释里面说下面的代码更快，参见：
                // https://stackoverflow.com/questions/37617021/why-ai-b-i-perform-better-than-ai-b
                a[less] = ak;
                ++less;

            // 如果 a[k] 大于 pivot2，将其移到右边部分。
            } else if (ak > pivot2) {
                // 从最右边一直移动 great，直到 a[great] 小于等于 pivot2
                while (a[great] > pivot2) {
                    // 如果这个过程中 great 等于 k，则不需要切分了
                    if (great-- == k) {
                        break outer;
                    }
                }

                // 如果 a[great] 小于 pivot1，则还是交换到左边部分去
                if (a[great] < pivot1) {
                    a[k] = a[less];
                    a[less] = a[great];
                    ++less;

                // 否则，pivot1 <= a[great] <= pivot2，交换 a[great] 和 a[k]
                } else {
                    a[k] = a[great];
                }

                // 将上面 if else 中相同代码提取出来
                a[great] = ak;
                --great;
            }
        }

        // 将切分点放到到最终的位置处
        a[left]  = a[less  - 1]; a[less  - 1] = pivot1;
        a[right] = a[great + 1]; a[great + 1] = pivot2;

        // 进行递归排序，除了两个切分点
        // 递归排序左边
        sort(a, left, less - 2, leftmost);
        // 递归排序右边
        sort(a, great + 2, right, false);

        /*
        如果中心部分太大（大于数组的 4/7），继续切分数组，
        分成 等于 pivot1、pivot1 和 pivot2 之间、等于 pivot2 三部分。
         */
        if (less < e1 && e5 < great) {
            /*
             * 跳过等于切分值的元素。
             */
            while (a[less] == pivot1) {
                ++less;
            }
            while (a[great] == pivot2) {
                --great;
            }

            /*
             * 切分:
             *
             *   left part         center part                  right part
             * +----------------------------------------------------------+
             * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
             * +----------------------------------------------------------+
             *              ^                        ^       ^
             *              |                        |       |
             *             less                      k     great
             *
             * 排序的元素满足下列不等式:
             *
             *              all in (*,  less) == pivot1
             *     pivot1 < all in [less,  k)  < pivot2
             *              all in (great, *) == pivot2
             *
             * k 是 ?-部分 的第一个元素的下标
             */
            outer:
            for (int k = less - 1; ++k <= great; ) {
                int ak = a[k];
                // 如果 a[k] 等于 pivot1，交换到左边部分。
                if (ak == pivot1) {
                    a[k] = a[less];
                    a[less] = ak;
                    ++less;

                // 如果 a[k] 等于 pivot2，交换到右边部分。
                } else if (ak == pivot2) {
                    while (a[great] == pivot2) {
                        if (great-- == k) {
                            break outer;
                        }
                    }
                    if (a[great] == pivot1) {
                        a[k] = a[less];
                        /*
                        如果是在 float 和 double 排序方法中：
                        即使 a[great] 等于 pivot1，如果 a[great] 和 pivot1 是不同符号的浮点零，
                        那么赋值 a[less]=pivot1 也可能不正确。因此，我们必须使用更精确的赋值 a[less]=a[great]。
                         */
                        a[less] = pivot1;
                        ++less;
                    } else {
                        a[k] = a[great];
                    }
                    a[great] = ak;
                    --great;
                }
            }
       }

       // 递归地排序中间部分
       sort(a, less, great, false);

    } else {
        // 如果 5 个排序元素有重复值，那就只使用 a[e3] 进行切分。
        int pivot = a[e3];

        /*
         * 退化为传统的三向切分模式：
         *
         *   left part    center part              right part
         * +-------------------------------------------------+
         * |  < pivot  |   == pivot   |     ?    |  > pivot  |
         * +-------------------------------------------------+
         *              ^              ^        ^
         *              |              |        |
         *             less            k      great
         *
         * 排序的元素满足下列不等式:
         *
         *   all in (left, less)   < pivot
         *   all in [less, k)     == pivot
         *   all in (great, right) > pivot
         *
         * k 是 ?-部分 的第一个元素的下标
         */
        for (int k = less; k <= great; ++k) {
            if (a[k] == pivot) {
                continue;
            }
            int ak = a[k];
            if (ak < pivot) { // Move a[k] to left part
                a[k] = a[less];
                a[less] = ak;
                ++less;
            } else { // a[k] > pivot - Move a[k] to right part
                while (a[great] > pivot) {
                    --great;
                }
                if (a[great] < pivot) { // a[great] <= pivot
                    a[k] = a[less];
                    a[less] = a[great];
                    ++less;
                } else { // a[great] == pivot
                    /*
                    如果是在 float 和 double 排序方法中：
                    即使 a[great] 等于 pivot，如果 a[great] 和 pivot 是不同符号的浮点零，
                    那么赋值 a[k]=pivot 也可能不正确。因此，我们必须使用更精确的赋值 a[k]=a[great]。
                     */
                    a[k] = pivot;
                }
                a[great] = ak;
                --great;
            }
        }

        /*
         * 递归地排序左边和右边，中间元素都相等，不用排序
         */
        sort(a, left, less - 1, leftmost);
        sort(a, great + 1, right, false);
    }
}
```
`long`数组的快速排序和`int`数组相同，不再列出。

## 2.3 char 数组基数排序
```java
static void sort(char[] a, int left, int right,
                 char[] work, int workBase, int workLen) {
    // 当 char 数组长度大于等于 COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR，使用基数排序代替快速排序
    if (right - left > COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR /* 3200 */ ) {
        // 构造基数桶，长度等于所有 char 值的数量
        int[] count = new int[NUM_CHAR_VALUES /* 1 << 16 */ ];

        // 计算 char 数组中不同字符的数量
        for (int i = left - 1; ++i <= right;
            count[a[i]]++
        );
        // 从右往左进行排序
        for (int i = NUM_CHAR_VALUES, k = right + 1; k > left; ) {
            // 跳过计数为 0 的桶
            while (count[--i] == 0);
            char value = (char) i;
            int s = count[i];

            // 将计数不为 0 的 char 值写入适当位置
            do {
                a[--k] = value;
            } while (--s > 0);
        }
    } else {
        // 在小数组上使用 Dual-Pivot 快排
        doSort(a, left, right, work, workBase, workLen);
    }
}

// 此方法和 int 数组归并排序流程一样，不再列出
private static void doSort(char[] a, int left, int right,
                           char[] work, int workBase, int workLen)

// 此方法和 int 数组 Dual-Pivot 快排流程一样，不再列出
private static void sort(char[] a, int left, int right, boolean leftmost)
```
`short`数组的排序和`char`数组相同，不再列出。

## 3.4 byte 数组排序
```java
static void sort(byte[] a, int left, int right) {
    // 当 byte 数组长度大于等于 COUNTING_SORT_THRESHOLD_FOR_BYTE，使用基数排序代替快速排序
    if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE /* 29 */ ) {
        int[] count = new int[NUM_BYTE_VALUES /* 1 << 8 */ ];

        for (int i = left - 1; ++i <= right;
            count[a[i] - Byte.MIN_VALUE]++
        );
        for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
            while (count[--i] == 0);
            // java 的 byte 是有符号数，所以需要加上 Byte.MIN_VALUE
            byte value = (byte) (i + Byte.MIN_VALUE);
            int s = count[i];

            do {
                a[--k] = value;
            } while (--s > 0);
        }
    } else {
        // 在小数组上使用插入排序。因为此时数组最大长度只有 28，因此不需要快排
        for (int i = left, j = i; i < right; j = ++i) {
            byte ai = a[i + 1];
            while (ai < a[j]) {
                a[j + 1] = a[j];
                if (j-- == left) {
                    break;
                }
            }
            a[j + 1] = ai;
        }
    }
}
```

## 3.5 float 数组排序
```java
static void sort(float[] a, int left, int right,
                 float[] work, int workBase, int workLen) {
    /*
     * 第一阶段: 将 NaN 移动到数组的末尾
     */
    // 找出不是 NaN 的数字数量
    while (left <= right && Float.isNaN(a[right])) {
        --right;
    }
    for (int k = right; --k >= left; ) {
        float ak = a[k];
        // NaN 是唯一不等于自身的数
        if (ak != ak) { // a[k] is NaN
            // 将 NaN 移动到数组末尾
            a[k] = a[right];
            a[right] = ak;
            --right;
        }
    }

    /*
     * 第二阶段: 对除 NaN 以外的所有内容进行排序.
     */
    doSort(a, left, right, work, workBase, workLen);

    /*
     * 第三阶段: 将 -0 排在 +0 前面
     */
    int hi = right;

    /*
     * 查找第一个零，或第一个正数，或最后一个负数
     */
    while (left < hi) {
        int middle = (left + hi) >>> 1;
        float middleValue = a[middle];

        if (middleValue < 0.0f) {
            left = middle + 1;
        } else {
            hi = middle;
        }
    }

    /*
     * 跳过最后一个负值（如果有）或所有前导负零。
     */
    while (left <= right && Float.floatToRawIntBits(a[left]) < 0) {
        ++left;
    }

    /*
     * 将负零移到子范围的开头
     *
     * 切分:
     *
     * +----------------------------------------------------+
     * |   < 0.0   |   -0.0   |   0.0   |   ?  ( >= 0.0 )   |
     * +----------------------------------------------------+
     *              ^          ^         ^
     *              |          |         |
     *             left        p         k
     *
     * 排序的元素满足下列不等式:
     *
     *   all in (*,  left)  <  0.0
     *   all in [left,  p) == -0.0
     *   all in [p,     k) ==  0.0
     *   all in [k, right] >=  0.0
     *
     * k 是 ?-部分 的第一个元素下标
     */
    for (int k = left, p = left - 1; ++k <= right; ) {
        float ak = a[k];
        if (ak != 0.0f) {
            break;
        }
        if (Float.floatToRawIntBits(ak) < 0) { // ak is -0.0f
            a[k] = 0.0f;
            a[++p] = -0.0f;
        }
    }
}

// 此方法和 int 数组归并排序流程一样，不再列出
private static void doSort(float[] a, int left, int right,
                           float[] work, int workBase, int workLen)

// 此方法和 int 数组 Dual-Pivot 快排流程一样，不再列出
private static void sort(float[] a, int left, int right, boolean leftmost)
```
`double`数组的排序和`float`数组相同，不再列出。


[dual-pivot-qs]: ../../../test/java_/util/DualPivotQuickSortSimpleImpl.java
[why-fast]: https://arxiv.org/pdf/1511.01138.pdf
[tim-sort]: TimSort.md