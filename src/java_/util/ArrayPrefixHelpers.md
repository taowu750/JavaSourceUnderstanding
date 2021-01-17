`java.util.ArrayPrefixHelpers`类声明如下：
```java
class ArrayPrefixHelpers
```
`Arrays.parallelPrefix`并行方法的帮助类。`Arrays.parallelPrefix`方法类似于`reduce`，
只不过它将每一步都保存在数组的对应位置（累积操作）。

Parallel prefix（aka cumulate，scan）任务类基于 Guy Blelloch 的[原始算法][scan-linear]：
算法流程如下：
 - 先不断往下，创建左右子任务。
 - 直到到达叶子任务，进入`SUMMED`阶段，完成求和操作。如果是最左边的叶子任务，直接进行累积运算；
 如果是最右边的叶子任务，不进行求和。
 - 然后再往上。如果内部结点的子结点都已处于`SUMMED`状态，则内部结点任务进行`fork`，为下一阶段准备。
 一直往上，直到内部结点的两个子结点没有都完成求和为止；或移动到了根节点。
 - 再从内部结点一直往下。这次子结点将进入`CUMULATE`阶段，完成累积运算，并且状态会转为`FINISHED`。
 - 最后再次往上。如果两个子结点都处于`FINISHED`阶段，则可以一直往上。直到到达根节点，
 此时判断是不是子任务都已完成，如果都完成了则最终完成。
 
我们将使用`pendingCount`表示状态，它不再表示“直至完成的待处理任务数”来使用。这些状态通过或运算设置：
`CUMULATE`, `SUMMED`, 和`FINISHED`。
 - `SUMMED`是表示求和阶段。对于叶子任务，它是在求和时设置的。对于内部任务，当一个子任务求和时变为`true`。
 当第二个子任务完成求和时，我们将向上移动树以触发`CUMULATE`阶段。
 - `CUMULATE`表示累积阶段。如果为`false`，则任务仅计算其总和。如果为`true`，则累积数组元素。
 `CUMULATE`在第二轮开始时设置在根部，然后向下传播。但是对于`lo==0`（树的左脊）的子树，将会提前设置。
 - `FINISHED`是完成阶段。对于叶子任务，在累积操作时设置。对于内部节点，当一个子任务已累积完成时变为`true`。
 当第二个子任务完成累积后，它会向上移动，在根节点结束任务。
 
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
        // 在此类中，pendingCount 不再作为“直至完成的待处理任务数”来使用，
        // 而是作为标志变量，表示 CUMULATE、SUMMED、FINISHED 三种状态
        final BinaryOperator<T> fn;
        final T[] a;
        // 检查参数
        if ((fn = this.function) == null || (a = this.array) == null)
            throw new NullPointerException();
        int th = threshold, org = origin, fnc = fence, l, h;
        CumulateTask<T> t = this;  // t 首先被赋值为当前任务
        /*
        阅读以下代码需要注意：由于有好几个状态，程序运行结构是个树型结构，状态转换和任务切换很复杂，
        因此我们需要理清逻辑。首先从初始状态出发，跟踪程序的调用轨迹，抓住主干。
        不要一行一行以顺序的方式理解程序，而应跟踪状态转换和任务切换，从而理清思路。

        在循环中
        1. 如果任务 t 大小大于阈值（是内部结点）：
            1. 如果子任务未创建，则生成两个子任务。将 t 赋值为左子任务，从树中向下移动，右子任务 fork。
               继续 outer 循环。
            2. 如果子任务已创建
                1. 如果两个子任务都处于 SUMMED 阶段，将它们的 CUMULATE 状态都设为 true。
                   t 赋值为左子任务，从树中向下移动，右子任务再次 fork，它们将会进行累积操作。
                   继续 outer 循环。
                2. 如果其中一个任务处于 SUMMED 阶段，将它的 CUMULATE 状态设为 true。
                   t 赋值为那个任务，从树中向下移动，将进行累积操作。继续 outer 循环。
                3. 如果两个任务都处于 CUMULATE 阶段，退出 outer 循环
        2. 否则，t 是叶子任务
            1. 叶子任务第一次运行
                1. 如果叶子任务在最左边，将其 SUMMED|FINISHED 都设为 true。然后直接进行累积计算
                   （因为它是第一段，需要进行累积为后面的段提供初始结果），累积最后结果写入 out。
                2. 否则，进入 SUMMED 阶段。
                    - 如果叶子任务不在最右边，完成求和，将求和结果写入 out；
                    - 如果叶子任务在最右边，不进行求和（因为它已是最后一段，不需要求和结果）。
                      它会在 CUMULATE 阶段累积最后的值。
                   子任务中任意一个求和完成，它的父任务 SUMMED 状态也变为 true。           
            2. 如果叶子任务处于 FINISHED 阶段，直接退出 outer 循环
            3. 如果叶子任务处于 CUMULATE 阶段（由父任务设置），则将叶子任务 FINISHED 状态设为 true。
               然后进行累积计算，累积最终结果写入 out
           如果未退出 outer 循环的话，开启一个内部循环，不断获取子任务的父任务，从树中向上移动：
            1. 如果只有一个子任务 SUMMED 被设为 true，就将它的父任务的 SUMMED 状态也设为 true，
               然后退出 outer 循环。
               注意，最左边的子任务也会将它的父任务的 FINISHED 状态设为 true；
               子任务处于 CUMULATE 状态，父任务的 FINISHED 状态也会被设为 true。
            2. 否则如果两个子任务 SUMMED 都被设为 true，父任务的 out 设为两个子任务 out 的合并。
               将子任务的状态也设置到父任务中，然后将 t 设为父任务，并向上移动，继续内部循环。
               如果父任务的 CUMULATE 状态为 false，并且它是最左边的任务，将它的 CUMULATE 状态设为 true，
               然后再次 fork。
               - 因为累积操作要从左往右进行，所以必须先将最左边的任务的 CUMULATE 状态设为 true，
                  其他任务的 CUMULATE 状态将由父任务设置。
               - 之所以再次 fork，是因为要让这个父任务从树中往下，设置子任务的 CUMULATE 状态。
            3. 如果子任务和父任务的 FINISHED 状态都为 true，将 t 设为父任务，并向上移动，继续内部循环。
           内部循环最终会退出，由于（1）只有一个子任务求和完成（2）遇到根节点。在内部循环过程中，
           会将子任务都求和完成，还未进入累积阶段，且是最左边的父任务 fork，让它进行累积操作的调度工作。

        从上述运行过程可以看出，叶子结点是实际进行求和和累积操作的地方，它负责算法的实际工作流程；
        而内部节点只是负责将子结点的结果相加，为下一个兄弟结点提供输入，它负责调度任务和合并任务结果。

        总结如下：
        - 先不断往下，创建左右子任务。
        - 直到到达叶子任务，进入 SUMMED 阶段，完成求和操作。如果是最左边的叶子任务，直接进行累积运算；
          如果是最右边的叶子任务，不进行求和。
        - 然后再往上。如果内部结点的子结点都已处于 SUMMED 状态，则内部结点任务进行 fork，为下一阶段准备。
          一直往上，直到内部结点的两个子结点没有都完成求和为止；或移动到了根节点。
        - 再从内部结点一直往下。这次子结点将进入 CUMULATE 阶段，完成累积运算，并且状态会转为 FINISHED。
        - 最后再次往上。如果两个子结点都处于 FINISHED 阶段，则可以一直往上。直到到达根节点，
          此时判断是不是子任务都已完成，如果都完成了则最终完成。
         */
        outer: 
        while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
            // 如果大小大于阈值，表示这是一个内部任务
            if (h - l > th) {
                // 取两个子任务
                CumulateTask<T> lt = t.left, rt = t.right, f;
                // 如果两个子任务还未创建，此时将从树中往下不断创建子任务
                if (lt == null) {
                    int mid = (l + h) >>> 1;
                    // 切分成两半，分别对应一个子任务
                    f = rt = t.right =
                            new CumulateTask<T>(t, fn, a, org, fnc, th, mid, h);
                    // 将 t 赋值为左子任务
                    t = lt = t.left  =
                            new CumulateTask<T>(t, fn, a, org, fnc, th, l, mid);
                }
                // 否则父任务将从树中不断往下，让子任务进入 CUMULATE 阶段
                else {
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
                    // 否则两个子任务 CUMULATE 状态都为 true，则 t 此时为 null，跳出 outer 循环
                    if (t == null)
                        break;
                }
                // f 代表的是右子任务。f 不为 null，则进行 fork
                // 注意右子任务首次被创建的时候会 fork 一次；
                // 当右子任务进入 CUMULATE 阶段的时候又会被 fork 一次
                if (f != null)
                    f.fork();
            }
            // 否则如果大小小于或等于阈值，表示这是一个叶子任务
            else {
                int state;
                for (int b;;) {
                    // 如果 t 的 FINISHED 状态为 true，表示已经完成，跳出 outer 循环
                    if (((b = t.getPendingCount()) & FINISHED /* 4 */) != 0)
                        break outer;
                    // 如果 t 处于 CUMULATE 阶段，则 FINISHED 状态设为 true；
                    // 否则如果 t 是最左端任务，则 SUMMED 和 FINISHED 状态都设为 true，
                    // 这样让它直接完成累积操作；否则将 t 的 SUMMED 状态设为 true。
                    state = ((b & CUMULATE) != 0? FINISHED :
                             (l > org) ? SUMMED /* 2 */ : (SUMMED|FINISHED));
                    // 将 t 的状态与 state 进行或运算
                    if (t.compareAndSetPendingCount(b, b|state))
                        break;
                }

                T sum;
                // 如果 t 处于 CUMULATE 阶段或 t 是最左端任务，则进行累积操作
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
                // 否则如果 t 处于 SUMMED 阶段，且不是最右端任务，进行求和运算
                else if (h < fnc) {
                    // 计算和，但不修改数组
                    sum = a[l];
                    for (int i = l + 1; i < h; ++i)
                        sum = fn.apply(sum, a[i]);
                }
                // 处于 SUMMED 阶段的最右端的任务，不需要进行求和
                else
                    sum = t.in;
                // out 设为求和结果
                t.out = sum;
                // 子任务完成求和或累积运算，将从树中不断往上
                for (CumulateTask<T> par;;) {
                    // 如果 t 往上达到了根任务
                    if ((par = (CumulateTask<T>)t.getCompleter()) == null) {
                        // 如果子任务都已完成，则完成对整个数组的 prefix 操作
                        if ((state & FINISHED) != 0)
                            t.quietlyComplete();
                        // 跳出最外层循环
                        break outer;
                    }
                    // 获取父任务的状态
                    int b = par.getPendingCount();
                    // 如果父任务和子任务都处于 FINISHED 阶段
                    if ((b & state & FINISHED) != 0)
                        // 将 t 赋值为父任务，在循环中继续向上移动
                        t = par;
                    // 否则如果父任务和子任务 SUMMED 状态为 true
                    else if ((b & state & SUMMED) != 0) {
                        int nextState; CumulateTask<T> lt, rt;
                        if ((lt = par.left) != null &&
                            (rt = par.right) != null) {
                            T lout = lt.out;
                            // 父任务的 out 是子任务的 out 合并
                            par.out = (rt.hi == fnc ? lout :
                                       fn.apply(lout, rt.out));
                        }
                        // 如果父任务的 CUMULATE 状态为 false，并且它是最左边的任务，
                        // 将它的 CUMULATE 状态设为 true，然后再次 fork。
                        int refork = (((b & CUMULATE) == 0 &&
                                       par.lo == org) ? CUMULATE : 0);
                        if ((nextState = b|state|refork) == b ||
                            par.compareAndSetPendingCount(b, nextState)) {
                            // 清除掉 FINISHED 状态，防止将它错误的向上传播。
                            state = SUMMED;
                            // 将 t 赋值为父任务，在循环中继续向上移动
                            t = par;
                            if (refork != 0)
                                par.fork();
                        }
                    }
                    // 否则只有一个子任务处于 SUMMED 状态，则将它的父任务的 SUMMED 状态设为 true，
                    // 然后不再往上
                    else if (par.compareAndSetPendingCount(b, b|state))
                        break outer;
                }
            }
        }
    }
}
```


[scan-linear]: http://www.cs.cmu.edu/~scandal/alg/scan.html