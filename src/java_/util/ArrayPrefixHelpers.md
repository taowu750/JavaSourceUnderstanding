`java.util.ArrayPrefixHelpers`类声明如下：
```java
class ArrayPrefixHelpers
```
`Arrays.parallelPrefix`并行方法的帮助类。`Arrays.parallelPrefix`方法类似于`reduce`，
只不过它将每一步都保存在数组的对应位置。

Parallel prefix（aka cumulate，scan）任务类基于 Guy Blelloch 的[原始算法][scan-linear]：
持续除以 2 直到达到阈值段大小，然后：
1. 第一轮：创建每段部分之和树
2. 第二轮：每段中，累积元素（计算和并存入数组位置）

这个版本主要通过允许准备好的左边第二轮继续进行（即使一些右边第一轮仍然在执行）来改进 FJ 框架的性能。
它还将最左边段的第一轮和第二轮合并，并跳过最右边段的第一轮（第二轮不需要其结果）。类似地，
它通过跟踪第一个元素作为 base 的那些段/子任务来避免要求用户为累加提供标识 base。

实现这一点依赖于在`pendingCount`中对阶段/状态的某些位进行或运算：`CUMULATE`, `SUMMED`, 和`FINISHED`。
 - `SUMMED`是求和阶段。对于叶，它是在求和时设置的。对于内部节点，当一个子节点求和时变为`true`。
 当第二个子结点完成求和时，我们将向上移动树以触发累积阶段。
 - `CUMULATE`表示累积阶段。如果为`false`，则段仅计算其总和。如果为`true`，则累积数组元素。
 `CUMULATE`在第二轮开始时设置在根部，然后向下传播。但是对于`lo==0`（树的左脊）的子树，也可以提前设置。
 - `FINISHED`是完成状态。对于叶子，在累积时设置。对于内部节点，当一个子节点已累积完成时变为`true`。
 当第二个子节点完成累积后，它会向上移动，在根节点结束任务。
 
为了更好地利用局部性并减少开销，`CountedCompleter.compute`方法从当前任务开始循环，如果可能的话，
移动到它的一个子任务，而不是派生子任务。

`ArrayPrefixHelpers`中有 4 个版本，对应于四种类型。除了类型以外，它们的算法流程都一模一样。

# 1. 成员字段
```java
// 表示累积状态的位
static final int CUMULATE = 1;
// 表示求和状态的位
static final int SUMMED   = 2;
// 表示完成状态的位
static final int FINISHED = 4;

// 最小子任务数组分区大小
static final int MIN_PARTITION = 16;
```

# 2. 内部类

## 2.1 CumulateTask
```java
// 用在对象数组中的 Parallel prefix 任务类
static final class CumulateTask<T> extends CountedCompleter<Void> {
    final T[] array;  // 数组
    final BinaryOperator<T> function;  // 合并两个元素的函数
    // 左子任务和右子任务
    CumulateTask<T> left, right;
    // 当前任务的输入和输出
    T in, out;
    // lo、hi 是当前任务处理的范围；origin、fence 是最初处理数组的范围；threshold 是阈值
    final int lo, hi, origin, fence, threshold;

    // 根任务的构造器
    public CumulateTask(CumulateTask<T> parent,
                        BinaryOperator<T> function,
                        T[] array, int lo, int hi) {
        super(parent);
        this.function = function; this.array = array;
        // 根任务的 lo、origin，hi、fence 是相等的
        this.lo = this.origin = lo; this.hi = this.fence = hi;
        int p;
        // 定义阈值为 (数组范围大小 / (8 * 可用并行度))、MIN_PARTITION 中的较大值
        this.threshold =
                (p = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3))
                <= MIN_PARTITION /* 16 */ ? MIN_PARTITION : p;
    }

    // 子任务的构造器
    CumulateTask(CumulateTask<T> parent, BinaryOperator<T> function,
                 T[] array, int origin, int fence, int threshold,
                 int lo, int hi) {
        super(parent);
        this.function = function; this.array = array;
        // 子任务的 lo、origin，hi、fence 都是分别设置的
        this.origin = origin; this.fence = fence;
        // 子任务直接使用传递过来的阈值，不自己计算
        this.threshold = threshold;
        this.lo = lo; this.hi = hi;
    }

    @SuppressWarnings("unchecked")
    public final void compute() {
        // 在此类中，pending 不再作为“直至完成的待处理任务数”来使用，
        // 而是作为标志变量，表示 CUMULATE、SUMMED、FINISHED 三种状态
        final BinaryOperator<T> fn;
        final T[] a;
        // 检查参数
        if ((fn = this.function) == null || (a = this.array) == null)
            throw new NullPointerException();
        int th = threshold, org = origin, fnc = fence, l, h;
        CumulateTask<T> t = this;  // t 首先被赋值为当前任务
        outer: 
        /*
        在循环中
        1. 如果任务 t 大小大于阈值（是内部结点）：
            1. 如果子任务未创建，则生成两个子任务。将 t 赋值为左子任务，右子任务 fork。继续循环。
            2. 如果子任务已创建
                1. 如果两个子任务都处于 SUMMED 阶段，将它们的 CUMULATE 状态设为 true。
                   t 赋值为左子任务，右子任务再次 fork。继续循环。
                2. 如果其中一个任务处于 SUMMED 阶段，将它的 CUMULATE 状态设为 true。
                   t 赋值为那个任务。继续循环。
                3. 如果两个任务都处于 CUMULATE 阶段，退出循环
        2. 否则，进入到叶子任务中
            1. 叶子任务第一次运行
                1. 如果叶子任务在最左边，将其 SUMMED|FINISHED 都设为 true。然后进行累积计算，
                   累积最终结果写入 out。
                2. 否则，进入 SUMMED 阶段。
                    - 如果叶子任务不在最右边，完成求和，将求和结果写入 out；
                    - 如果叶子任务在最右边，不进行求和（因为它已是最后一段，不需要求和结果）。
                      它会在 CUMULATE 阶段累积最后的值。
                   子任务中任意一个求和完成，它的父任务 SUMMED 状态也变为 true。
            2. 如果叶子任务处于 FINISHED 阶段，直接退出循环
            3. 如果叶子任务处于 CUMULATE 阶段（由父任务设置），则将叶子任务 FINISHED 状态设为 true。
               然后进行累积计算，累积最终结果写入 out
         */
        while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
            // 如果大小大于阈值
            if (h - l > th) {
                // 取两个子任务
                CumulateTask<T> lt = t.left, rt = t.right, f;
                // 如果两个子任务还未创建（父任务第一次在循环中运行），则进入第一轮
                if (lt == null) {
                    int mid = (l + h) >>> 1;
                    // 切分成两半，分别对应一个子任务
                    f = rt = t.right =
                            new CumulateTask<T>(t, fn, a, org, fnc, th, mid, h);
                    // 将 t 赋值为左子任务
                    t = lt = t.left  =
                            new CumulateTask<T>(t, fn, a, org, fnc, th, l, mid);
                }
                // 否则父任务在第二次在循环中运行，
                else {                           // possibly refork
                    T pin = t.in;
                    // 将左子任务的输入设为父任务的输入
                    lt.in = pin;
                    f = t = null;
                    // 如果右子任务存在
                    if (rt != null) {
                        T lout = lt.out;
                        // 右子任务的输入等于左子任务的输出和父任务的输入的组合。其中最左端的父任务没有输入
                        rt.in = (l == org ? lout : fn.apply(pin, lout));
                        // 将右子任务的 CUMULATE 状态设为 true
                        for (int c;;) {
                            // 如果右子任务已处于 CUMULATE 状态，则跳出循环
                            if (((c = rt.getPendingCount()) & CUMULATE /* 1 */) != 0)
                                break;
                            // 设置右子任务的 CUMULATE 状态为 true
                            if (rt.compareAndSetPendingCount(c, c|CUMULATE)){
                                // 将 t 赋值为右子任务
                                t = rt;
                                break;
                            }
                        }
                    }
                    // 将左子任务的 CUMULATE 状态设为 true
                    for (int c;;) {
                        // 如果左子任务已处于 CUMULATE 状态，则跳出循环
                        if (((c = lt.getPendingCount()) & CUMULATE) != 0)
                            break;
                        // 设置左子任务的 CUMULATE 状态为 true
                        if (lt.compareAndSetPendingCount(c, c|CUMULATE)) {
                            if (t != null)
                                // t 不为 null，则 t 此时表示的是右子任务，将其赋值给 f
                                f = t;
                            // 将 t 赋值为左子任务
                            t = lt;
                            break;
                        }
                    }
                    // 如果左子任务的 CUMULATE 状态之前为 false，则 t 设为左子任务；
                    // 否则如果右子任务的 CUMULATE 状态之前为 false，则 t 设为右子任务；
                    // 否则两个子任务 CUMULATE 状态都为 true，则 t 此时为 null，跳出最外层循环
                    if (t == null)
                        break;
                }
                // f 代表的是右子任务。f 不为 null，则进行 fork
                // 注意右子任务首次被创建的时候会 fork 一次；
                if (f != null)
                    f.fork();
            }
            // 否则如果大小小于或等于阈值
            else {
                int state; // Transition to sum, cumulate, or both
                for (int b;;) {
                    // 如果 t 的 FINISHED 状态为 true，表示已经完成，跳出循环
                    if (((b = t.getPendingCount()) & FINISHED /* 4 */) != 0)
                        break outer;
                    // 如果 t 的 CUMULATE 状态为 true，则 state 为 FINISHED；
                    // 否则如果 t 不是最左端的任务，state 为 SUMMED；
                    // 否则 state 为 SUMMED|FINISHED
                    state = ((b & CUMULATE) != 0? FINISHED :
                             (l > org) ? SUMMED /* 2 */ : (SUMMED|FINISHED));
                    // 将 t 的状态与 state 进行或运算
                    if (t.compareAndSetPendingCount(b, b|state))
                        break;
                }

                T sum;
                // 如果 state 不等于 SUMMED，则进行累积运算
                if (state != SUMMED) {
                    int first;
                    // 最左端任务，没有 in
                    if (l == org) {
                        sum = a[org];
                        first = org + 1;
                    }
                    else {
                        sum = t.in;
                        first = l;
                    }
                    // 进行累积运算，为数组每一位计算累计值
                    for (int i = first; i < h; ++i)
                        a[i] = sum = fn.apply(sum, a[i]);
                }
                // 如果不是最右端任务，进行求和运算
                else if (h < fnc) {
                    // 计算和，但不修改数组
                    sum = a[l];
                    for (int i = l + 1; i < h; ++i)
                        sum = fn.apply(sum, a[i]);
                }
                // 最右端的任务
                else
                    sum = t.in;
                // 累积计算完成，out 设为累积结果
                t.out = sum;
                // 子任务完成，向上传播
                for (CumulateTask<T> par;;) {
                    // 根如果没有父任务，表示此任务是任务
                    if ((par = (CumulateTask<T>)t.getCompleter()) == null) {
                        // 如果 FINISHED 状态为 true，结束这个任务
                        if ((state & FINISHED) != 0)      // enable join
                            t.quietlyComplete();
                        // 跳出最外层循环
                        break outer;
                    }
                    // 如果父任务存在，获取父任务的状态
                    int b = par.getPendingCount();
                    // 如果父任务和子任务 FINISHED 状态都为 true
                    if ((b & state & FINISHED) != 0)
                        // 将 t 赋值为父任务
                        t = par;
                    // 否则如果父任务和子任务 SUMMED 状态都为 true
                    else if ((b & state & SUMMED) != 0) {
                        int nextState; CumulateTask<T> lt, rt;
                        // 如果左右子结点都存在
                        if ((lt = par.left) != null &&
                            (rt = par.right) != null) {
                            T lout = lt.out;
                            // 父任务的 out 是子任务的 out 合并
                            par.out = (rt.hi == fnc ? lout :
                                       fn.apply(lout, rt.out));
                        }
                        // 如果父任务 CUMULATE 为 false 且是最左端的任务，
                        int refork = (((b & CUMULATE) == 0 &&
                                       par.lo == org) ? CUMULATE : 0);
                        if ((nextState = b|state|refork) == b ||
                            par.compareAndSetPendingCount(b, nextState)) {
                            state = SUMMED;               // drop finished
                            t = par;
                            if (refork != 0)
                                par.fork();
                        }
                    }
                    // 否则将父任务的状态和 state 进行或运算
                    else if (par.compareAndSetPendingCount(b, b|state))
                        break outer;                      // sib not ready
                }
            }
        }
    }
}
```


[scan-linear]: http://www.cs.cmu.edu/~scandal/alg/scan.html