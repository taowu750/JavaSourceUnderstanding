`java.util.ArraysParallelSortHelpers`类的声明如下：
```java
class ArraysParallelSortHelpers
```
`Arrays.parallelSort`并行排序方法的帮助类。我们为每种基本类型和对象类型定义一个静态类包含排序实现：`Sorter`和`Merger`。
`Sorter`基本算法如下：
1. 如果数组大小较小，则使用顺序排序（通过`Arrays`）。
2. 否则：
    1. 将数组分成两半。
    2. 对于每一半
        1. 分成两半（即四分之一）。
        2. 对每个四分之一块继续执行 1-2 的操作。
        3. 将它们归并在一起
    3. 将两半归并在一起。

拆分为四分之一的原因是，这保证了最终排序在主数组中，而不是在工作区数组中。（每个子排序步骤上的工作区数组会和和主数组交替。）
最底层排序使用顺序排序。

`Merger`类对`Sorter`执行合并。如果基础排序是稳定的（`TimSort`是这样的），则归并的排序也是如此。
归并的过程如下：
1. 将左右两个分区中的较大分区一分为二，取中点`m`；
2. 找到中点在另一个分区中的位置`b`；
3. 为较大分区`m`的右边部分和较小分区`b`的右边部分生成`Merger`，递归合并；
4. 对两块分区的左边部分同样进行 1-3 的步骤，直到两块分区长度都小于阈值，则进行常规归并。

为了确保任务以稳定的顺序触发，当前的`CountedCompleter`需要一些作为触发完成任务的占位符的子任务。
这些类（`EmptyCompleter`和`Relay`）不需要跟踪数组，而且它们本身也从不派生其他任务，因此不保持任何任务状态。

除类型声明外，基本类型（`FJByte`, ..., `FJDouble`）算法流程相同。
 
顺序排序由`TimSort`，`ComparableTimSort`和`DualPivotQuicksort`类实现，它们的方法接受我们已经分配的临时工作空间数组，
因此避免了冗余分配。（除了`DualPivotQuicksort.sort(byte[])`，它不需要工作区数组。）

# 1. 内部类

## 1.1 EmptyCompleter
```java
// Sorter 的占位符任务，用在四分之一排序任务中，不需要维护数组状态。
static final class EmptyCompleter extends CountedCompleter<Void> {
    static final long serialVersionUID = 2446542900576103244L;
    EmptyCompleter(CountedCompleter<?> p) { super(p); }
    public final void compute() { }
}
```

## 1.2 Relay
```java
// 两次排序操作后的触发器。用来在两次排序操作完成后进行合并
static final class Relay extends CountedCompleter<Void> {
    static final long serialVersionUID = 2446542900576103244L;
    final CountedCompleter<?> task;
    Relay(CountedCompleter<?> task) {
        super(null, 1);  // pending 设为 1，这使得第二次排序完成后触发归并
        this.task = task;
    }
    public final void compute() { }
    public final void onCompletion(CountedCompleter<?> t) {
        task.compute();
    }
}
```
参见`CountedCompleter`的`Java API`文档最后一节 **Triggers**。

## 1.3 FJObject
```java
// 用于并发排序对象的帮助类
static final class FJObject {

    static final class Sorter<T> extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;
        final T[] a, w;  // 需要排序的数组和工作区数组
        // base, size: 排序范围的起始位置和大小
        // wbase: 工作区数组的起始位置
        // gran: 最小排序数组划分长度，低于该长度，并行排序算法将不会进一步划分排序任务。
        //       初始为数组的 1/(可用核心数*4) 长度和 Arrays.MIN_ARRAY_SORT_GRAN 中的较大值
        final int base, size, wbase, gran;
        Comparator<? super T> comparator;  // 对象比较器

        Sorter(CountedCompleter<?> par, T[] a, T[] w, int base, int size,
               int wbase, int gran,
               Comparator<? super T> comparator) {
            super(par);
            this.a = a; this.w = w; this.base = base; this.size = size;
            this.wbase = wbase; this.gran = gran;
            this.comparator = comparator;
        }
        
        public final void compute() {
            CountedCompleter<?> s = this;
            Comparator<? super T> c = this.comparator;
            T[] a = this.a, w = this.w; // 将所有字段全部变成局部变量，防止 getfield 频繁调用
            int b = this.base, n = this.size, wb = this.wbase, g = this.gran;
            /*
            1. 如果剩余大小大于最小排序数组划分长度
               - 将排序区域划分为四块
               - 为后面三块创建子 Sorter 和 Merger 进行排序和归并
               - 对第一块继续执行 1 的操作
            2. 否则使用 TimSort 进行排序
             */
            while (n > g) {
                // h: 1/2 的大小；q: 1/4 的大小；u: 3/4 的大小
                int h = n >>> 1, q = h >>> 1, u = h + q;
                // fc：将工作区数组归并到排序数组的 Merger。将两个 1/2 块进行归并。
                Relay fc = new Relay(new Merger<T>(s, w, a, wb, h,
                                                   wb+h, n-h, b, g, c));

                // rc：将排序数组归并到工作区数组的 Merger。将后面两个 1/4 块进行归并
                Relay rc = new Relay(new Merger<T>(fc, a, w, b+h, q,
                                                   b+u, n-u, wb+h, g, c));
                // 对第四个 1/4 块进行排序的 Sorter，它的父 Completer 是 rc
                new Sorter<T>(rc, a, w, b+u, n-u, wb+u, g, c).fork();              // (1)
                // 对第三个 1/4 块进行排序 Sorter，它的父 Completer 是 rc
                new Sorter<T>(rc, a, w, b+h, q, wb+h, g, c).fork();                // (2)

                // bc：将排序数组归并到工作区数组的 Merger。将前面两个 1/4 块进行归并
                Relay bc = new Relay(new Merger<T>(fc, a, w, b, q,
                                                   b+q, h-q, wb, g, c));
                // 对第二个 1/4 块进行排序的 Sorter，它的父 Completer 是 bc
                new Sorter<T>(bc, a, w, b+q, h-q, wb+q, g, c).fork();              // (3)
                // 接下来对第一个 1/4 块进行处理，它的父 Completer 是 bc。
                s = new EmptyCompleter(bc);                                        // (4)
                n = q;

                // 当 (1) 和 (2) 排序完成后，它们的父 Completer rc 会对右半边两个 1/4 块进行归并
                // 当 (3) 和 (4) 排序完成后，它们的父 Completer bc 会对左半边两个 1/4 块进行归并
                // 当所有排序都完成之后，由顶级 Completer fc 对两个 1/2 块进行归并
            }
            // 使用 TimSort 排序最左边的块
            TimSort.sort(a, b, b + n, c, w, wb, n);
            s.tryComplete();
        }
    }

    static final class Merger<T> extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;
        final T[] a, w; // 需要排序的数组和工作区数组
        // 左半边、右半边排序数组的起始位置和大小；工作区数组的起始位置；最小排序数组划分长度
        final int lbase, lsize, rbase, rsize, wbase, gran;
        Comparator<? super T> comparator;
 
        Merger(CountedCompleter<?> par, T[] a, T[] w,
               int lbase, int lsize, int rbase,
               int rsize, int wbase, int gran,
               Comparator<? super T> comparator) {
            super(par);
            this.a = a; this.w = w;
            this.lbase = lbase; this.lsize = lsize;
            this.rbase = rbase; this.rsize = rsize;
            this.wbase = wbase; this.gran = gran;
            this.comparator = comparator;
        }

        public final void compute() {
            Comparator<? super T> c = this.comparator;
            T[] a = this.a, w = this.w; // 将所有字段全部变成局部变量，防止 getfield 频繁调用
            int lb = this.lbase, ln = this.lsize, rb = this.rbase,
                rn = this.rsize, k = this.wbase, g = this.gran;
            // 检查参数是否合法
            if (a == null || w == null || lb < 0 || rb < 0 || k < 0 ||
                c == null)
                throw new IllegalStateException();
            // 1. 将左右两个分区中的较大分区一分为二，取中点 m；
            // 2. 找到中点在另一个分区中的位置 b；
            // 3. 为较大分区 m 的右边部分和较小分区 b 的右边部分生成归并任务；
            // 4. 对两块分区的左边部分同样进行 1-3 的步骤，直到两块分区长度都小于最小排序数组划分长度
            for (int lh, rh;;) {
                // 如果左半分区较大
                if (ln >= rn) {
                    // 如果左半分区长度小于最小排序数组划分长度，跳出循环
                    if (ln <= g)
                        break;
                    rh = rn;  // 右半分区的 hi
                    // 找到左半分区的中点
                    T split = a[(lh = ln >>> 1) + lb];
                    // 通过二分查找找到中点在右半分区的插入点
                    for (int lo = 0; lo < rh; ) {
                        int rm = (lo + rh) >>> 1;
                        if (c.compare(split, a[rm + rb]) <= 0)
                            rh = rm;
                        else
                            lo = rm + 1;
                    }
                }
                // 否则如果右半分区较大
                else {
                    // 如果右半分区长度小于最小排序数组划分长度，跳出循环
                    if (rn <= g)
                        break;
                    lh = ln;  // 右半分区的 hi
                    // 找到右半分区的中点
                    T split = a[(rh = rn >>> 1) + rb];
                    // 通过二分查找找到中点在左半分区的插入点
                    for (int lo = 0; lo < lh; ) {
                        int lm = (lo + lh) >>> 1;
                        if (c.compare(split, a[lm + lb]) <= 0)
                            lh = lm;
                        else
                            lo = lm + 1;
                    }
                }
                // 生成归并任务。将左半分区的右边部分和右半分区的右边部分归并
                Merger<T> m = new Merger<T>(this, a, w, lb + lh, ln - lh,
                                            rb + rh, rn - rh,
                                            k + lh + rh, g, c);
                rn = rh;
                ln = lh;
                addToPendingCount(1);
                m.fork();
            }

            // 切分到最后，合并两块分区最左边的部分
            int lf = lb + ln, rf = rb + rn;  // 左边一块、右边一块的末尾下标
            // 将 a 中内容归并到 w 中
            while (lb < lf && rb < rf) {
                // 标准的归并操作
                T t, al, ar;
                if (c.compare((al = a[lb]), (ar = a[rb])) <= 0) {
                    lb++; t = al;
                }
                else {
                    rb++; t = ar;
                }
                w[k++] = t;
            }
            // 复制剩余部分
            if (rb < rf)
                System.arraycopy(a, rb, w, k, rf - rb);
            else if (lb < lf)
                System.arraycopy(a, lb, w, k, lf - lb);

            tryComplete();
        }
    }
}
```
关于`CountedCompleter`可以查看`Java API`文档了解用法。`TimSort`参见 [TimSort.md][tim-sort]。
`getfield`参见 [避免getfield频繁调用.md][getfield]。

## 1.4 其他基本类型 FJ

基本类型并发排序帮助类`FJByte`、`FJChar`、`FJShort`、`FJInt`、`FJLong`、`FJFloat`、`FJDouble`算法流程和
`FJObject`一模一样。只不过它们使用`DualPivotQuicksort`进行顺序排序。参见 [DualPivotQuickSort.md][dual-pivot]。


[getfield]: ../lang/避免getfield频繁调用.md
[tim-sort]: TimSort.md
[dual-pivot]: DualPivotQuickSort.md