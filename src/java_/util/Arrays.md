`java.util.Arrays`类的声明如下：
```java
public class Arrays
```
`Arrays`是一个工具类，包含用于操纵数组的各种方法（例如排序和搜索）。 此类还包含一个静态工厂，该工厂允许将数组视为列表。
如果指定的数组引用为`null`，则除非另有说明，否则此类中的方法都抛出`NullPointerException`。

此类中包含的方法的文档包括对实现的简要说明。此类描述应被视为实现说明，而不是规范的一部分。只要遵守规范本身，
实现者就可以随意替换其他算法。（例如，`sort(Object[])`使用的算法不必是`MergeSort`，但必须是稳定的。）

# 1. 成员字段
```java
/*
最小排序划分数组长度，低于该长度，并行排序算法将不会进一步划分排序任务。
使用较小的大小通常会导致内存争用，使得并行加速的可能性不大。
*/
private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;  // 8192

// 调整参数：数组大小等于或小于将使用插入排序，用在 legacyMergeSort 中。此字段在将来的版本中将会被删除。
private static final int INSERTIONSORT_THRESHOLD = 7;
```

# 2. 内部类

## 2.1 NaturalOrder
```java
/*
一个比较器，实现一组 Comparable 元素的自然排序。当提供的 Comparator 为 null 时可以使用此比较器。
为了简化基础实现中的代码共享，使用 Object 作为泛型参数。

Arrays 类实现者需要注意：ComparableTimSort 是否比使用这个比较器的 TimSort 性能更好是一个经验问题。
如果不是，最好删除或不使用 ComparableTimSort。目前还没有将它们的并行排序性能进行对比的经验案例，
因此所有的 public Object parallelSort() 方法都使用相同的基于比较器的实现。
*/
static final class NaturalOrder implements Comparator<Object> {
    @SuppressWarnings("unchecked")
    public int compare(Object first, Object second) {
        return ((Comparable<Object>)first).compareTo(second);
    }
    static final NaturalOrder INSTANCE = new NaturalOrder();
}
```
参见 [TimSort.md][tim-sort] 和 [ComparableTimSort.md][comparable-tim-sort]。

## 2.2 LegacyMergeSort
```java
/*
可以使用系统属性选择旧的归并排序实现（以与损坏的比较器兼容）。
由于循环依赖性，userRequested 在外部类中不能声明为静态布尔值，所以要放在内部类里面

此字段在将来的版本中将会被删除。
*/
static final class LegacyMergeSort {
    private static final boolean userRequested =
        java.security.AccessController.doPrivileged(
            new sun.security.action.GetBooleanAction(
                "java.util.Arrays.useLegacyMergeSort")).booleanValue();
}
```

## 2.3 ArrayList
```java
// 包装数组作为 List 的包装类。它不支持添加和删除操作
private static class ArrayList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable
{
    private static final long serialVersionUID = -2764017481108945198L;
    private final E[] a;

    ArrayList(E[] array) {
        a = Objects.requireNonNull(array);
    }

    @Override
    public int size() {
        return a.length;
    }

    @Override
    public Object[] toArray() {
        return a.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            return Arrays.copyOf(this.a, size, (Class<? extends T[]>) a.getClass());
        System.arraycopy(this.a, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public E get(int index) {
        return a[index];
    }

    @Override
    public E set(int index, E element) {
        E oldValue = a[index];
        a[index] = element;
        return oldValue;
    }

    @Override
    public int indexOf(Object o) {
        E[] a = this.a;
        if (o == null) {
            for (int i = 0; i < a.length; i++)
                if (a[i] == null)
                    return i;
        } else {
            for (int i = 0; i < a.length; i++)
                if (o.equals(a[i]))
                    return i;
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(a, Spliterator.ORDERED);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (E e : a) {
            action.accept(e);
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        E[] a = this.a;
        for (int i = 0; i < a.length; i++) {
            a[i] = operator.apply(a[i]);
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        Arrays.sort(a, c);
    }
}
```

# 3. 方法

## 3.1 rangeCheck
```java
private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
        throw new IllegalArgumentException(
                "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
    if (fromIndex < 0) {
        throw new ArrayIndexOutOfBoundsException(fromIndex);
    }
    if (toIndex > arrayLength) {
        throw new ArrayIndexOutOfBoundsException(toIndex);
    }
}
```

## 3.2 equals
```java
/*
如果两个指定的 int 数组彼此相等，则返回 true。如果两个数组包含相同数量的元素，
并且两个数组中所有对应的元素对均相等，则认为两个数组相等。换句话说，
如果两个数组包含相同顺序的相同元素，则它们相等。
另外，如果两个数组引用均为空，则它们被视为相等。
*/
public static boolean equals(int[] a, int[] a2) {
    if (a==a2)
        return true;
    if (a==null || a2==null)
        return false;

    int length = a.length;
    if (a2.length != length)
        return false;

    for (int i=0; i<length; i++)
        if (a[i] != a2[i])
            return false;

    return true;
}

public static boolean equals(Object[] a, Object[] a2) {
    if (a==a2)
        return true;
    if (a==null || a2==null)
        return false;

    int length = a.length;
    if (a2.length != length)
        return false;

    for (int i=0; i<length; i++) {
        Object o1 = a[i];
        Object o2 = a2[i];
        if (!(o1==null ? o2==null : o1.equals(o2)))
            return false;
    }

    return true;
}

// boolean、byte、short、char、long、float、double 数组的 equals 方法与 int 数组类似，
// 在此不再列出
```

## 3.3 deepEquals
```java
/*
如果两个指定的数组彼此深度相等，则返回 true。与 equals(Object[], Object[]) 方法不同，
此方法适用于任意深度的嵌套数组。

如果两个数组引用都为 null，或者两个数组引用都引用了包含相同数量元素的数组，
并且两个数组中所有对应的元素都相等，则认为它们是完全相等的。

如果满足以下任一条件，则两个可能为 null 的元素 e1 和 e2 完全相等：
 - e1 和 e2 都是对象引用类型的数组，并且 Arrays.deepEquals(e1，e2) 将返回 true
 - e1 和 e2 是相同基本类型的数组，并且 Arrays.equals(e1，e2) 的将返回 true。
 - e1 == e2
 - e1.equals(e2) 将返回 true。
请注意，此定义允许在任何深度使用 null 元素。

如果任何指定的数组通过一个或多个级别的数组直接或间接包含自身作为元素，则此方法的行为未定义。
*/
public static boolean deepEquals(Object[] a1, Object[] a2) {
    if (a1 == a2)
        return true;
    if (a1 == null || a2==null)
        return false;
    int length = a1.length;
    if (a2.length != length)
        return false;

    for (int i = 0; i < length; i++) {
        Object e1 = a1[i];
        Object e2 = a2[i];

        if (e1 == e2)
            continue;
        if (e1 == null)
            return false;

        // e1 和 e2 可能也是数组
        boolean eq = deepEquals0(e1, e2);

        if (!eq)
            return false;
    }
    return true;
}

static boolean deepEquals0(Object e1, Object e2) {
    assert e1 != null;
    boolean eq;
    // 判断 e1 和 e2 的类型，调用不同的方法
    if (e1 instanceof Object[] && e2 instanceof Object[])
        // 递归地判断相等性
        eq = deepEquals ((Object[]) e1, (Object[]) e2);
    else if (e1 instanceof byte[] && e2 instanceof byte[])
        eq = equals((byte[]) e1, (byte[]) e2);
    else if (e1 instanceof short[] && e2 instanceof short[])
        eq = equals((short[]) e1, (short[]) e2);
    else if (e1 instanceof int[] && e2 instanceof int[])
        eq = equals((int[]) e1, (int[]) e2);
    else if (e1 instanceof long[] && e2 instanceof long[])
        eq = equals((long[]) e1, (long[]) e2);
    else if (e1 instanceof char[] && e2 instanceof char[])
        eq = equals((char[]) e1, (char[]) e2);
    else if (e1 instanceof float[] && e2 instanceof float[])
        eq = equals((float[]) e1, (float[]) e2);
    else if (e1 instanceof double[] && e2 instanceof double[])
        eq = equals((double[]) e1, (double[]) e2);
    else if (e1 instanceof boolean[] && e2 instanceof boolean[])
        eq = equals((boolean[]) e1, (boolean[]) e2);
    else
        // 判断对象之间地相等性
        eq = e1.equals(e2);
    return eq;
}
```

## 3.4 hashCode
```java
public static int hashCode(int a[]) {
    if (a == null)
        return 0;

    int result = 1;
    for (int element : a)
        result = 31 * result + element;

    return result;
}

public static int hashCode(boolean a[]) {
    if (a == null)
        return 0;

    int result = 1;
    for (boolean element : a)
        // 魔法数字 1231、1237
        result = 31 * result + (element ? 1231 : 1237);

    return result;
}

public static int hashCode(Object a[]) {
    if (a == null)
        return 0;

    int result = 1;

    for (Object element : a)
        result = 31 * result + (element == null ? 0 : element.hashCode());

    return result;
}

// byte、short、char、long、float、double 数组的 hashCode 方法与 int 数组类似，
// 在此不再列出
```
有关于哈希码的计算参见 [哈希算法.md][hash-code]。
`boolean`哈希码的魔法数字参见 [Boolean.md][boolean]。

## 3.5 deepHashCode
```java
/*
返回指定的数组哈希码。如果该数组包含其他数组作为元素，则哈希码将基于其内容，等等。
不能直接或间接通过一个包含自身作为元素的数组上调用此方法。

对于任何两个 Arrays.deepEquals(a，b) 的数组 a 和 b，
也会有 Arrays.deepHashCode(a) == Arrays.deepHashCode(b)。
*/
public static int deepHashCode(Object a[]) {
    if (a == null)
        return 0;

    int result = 1;

    for (Object element : a) {
        int elementHash = 0;
        if (element instanceof Object[])
            elementHash = deepHashCode((Object[]) element);
        else if (element instanceof byte[])
            elementHash = hashCode((byte[]) element);
        else if (element instanceof short[])
            elementHash = hashCode((short[]) element);
        else if (element instanceof int[])
            elementHash = hashCode((int[]) element);
        else if (element instanceof long[])
            elementHash = hashCode((long[]) element);
        else if (element instanceof char[])
            elementHash = hashCode((char[]) element);
        else if (element instanceof float[])
            elementHash = hashCode((float[]) element);
        else if (element instanceof double[])
            elementHash = hashCode((double[]) element);
        else if (element instanceof boolean[])
            elementHash = hashCode((boolean[]) element);
        else if (element != null)
            elementHash = element.hashCode();

        result = 31 * result + elementHash;
    }

    return result;
}
```

## 3.6 toString
```java
/*
返回指定数组内容的字符串表示形式。字符串表示形式包括所有数组元素，并用方括号（“[]”）括起来。
相邻元素由字符“，”（逗号后跟空格）分隔。元素通过 String.valueOf(int) 转换为字符串。

如果 a 为 null，则返回 “null”。
*/
public static String toString(int[] a) {
    if (a == null)
        return "null";
    int iMax = a.length - 1;
    if (iMax == -1)
        return "[]";

    StringBuilder b = new StringBuilder();
    b.append('[');
    for (int i = 0; ; i++) {
        b.append(a[i]);
        if (i == iMax)
            return b.append(']').toString();
        b.append(", ");
    }
}

// boolean、byte、short、char、long、float、double 和对象数组的 toString 方法与 int 数组类似，
// 在此不再列出
```

## 3.7 deepToString
```java
/*
返回指定数组的“深层内容”的字符串表示形式。如果数组包含其他数组作为元素，则字符串表示形式包含其内容，
依此类推。此方法旨在将多维数组转换为字符串。

字符串表示形式包括数组元素列表，并用方括号（“[]”）括起来。相邻元素由字符“，”（逗号后跟空格）分隔。
除非它们本身是数组，否则元素通过 String.valueOf(Object) 转换为字符串。

如果元素 e 是基本类型的数组，则通过调用 Arrays.toString(e) 的将其转换为字符串。
如果元素 e 是对象类型的数组，则通过递归调用此方法将其转换为字符串。

为避免无限递归，如果指定的数组包含自身作为元素，或通过一个或多个级别的数组包含对自身的间接引用，
则自引用将转换为字符串 “[...]”。例如，仅包含对自身的引用的数组将返回 “[[...]]”。

如果指定的数组为 null，则此方法返回 “null”。
*/
public static String deepToString(Object[] a) {
    if (a == null)
        return "null";

    int bufLen = 20 * a.length;
    // 防止溢出
    if (a.length != 0 && bufLen <= 0)
        bufLen = Integer.MAX_VALUE;
    StringBuilder buf = new StringBuilder(bufLen);
    deepToString(a, buf, new HashSet<Object[]>());
    return buf.toString();
}

private static void deepToString(Object[] a, StringBuilder buf,
                                 Set<Object[]> dejaVu) {
    if (a == null) {
        buf.append("null");
        return;
    }
    int iMax = a.length - 1;
    if (iMax == -1) {
        buf.append("[]");
        return;
    }

    dejaVu.add(a);
    buf.append('[');
    for (int i = 0; ; i++) {
        Object element = a[i];
        if (element == null) {
            buf.append("null");
        } else {
            Class<?> eClass = element.getClass();

            if (eClass.isArray()) {
                if (eClass == byte[].class)
                    buf.append(toString((byte[]) element));
                else if (eClass == short[].class)
                    buf.append(toString((short[]) element));
                else if (eClass == int[].class)
                    buf.append(toString((int[]) element));
                else if (eClass == long[].class)
                    buf.append(toString((long[]) element));
                else if (eClass == char[].class)
                    buf.append(toString((char[]) element));
                else if (eClass == float[].class)
                    buf.append(toString((float[]) element));
                else if (eClass == double[].class)
                    buf.append(toString((double[]) element));
                else if (eClass == boolean[].class)
                    buf.append(toString((boolean[]) element));
                else { // 如果对象数组已经包含了，就不再包含它避免无限递归
                    if (dejaVu.contains(element))
                        buf.append("[...]");
                    else
                        deepToString((Object[])element, buf, dejaVu);
                }
            } else {
                // element 不是数组
                buf.append(element.toString());
            }
        }
        if (i == iMax)
            break;
        buf.append(", ");
    }
    buf.append(']');
    dejaVu.remove(a);
}
```

## 3.8 基本类型 sort
```java
/*
请注意，所有公共“sort”方法都采用相同的形式：如果需要，执行参数检查，然后将参数变成
其他包私有类中的内部实现方法所需的参数（legacyMergeSort 除外，它包含在此类中）。
*/

public static void sort(int[] a) {
    DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
}

public static void sort(int[] a, int fromIndex, int toIndex) {
    rangeCheck(a.length, fromIndex, toIndex);
    DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
}

// 其他基本类型方法都相似，不再列出
```
参见 [DualPivotQuickSort.md][dual-pivot]。

## 3.9 基本类型 parallelSort
```java
// 并发排序。Java8 引入
public static void parallelSort(int[] a) {
    int n = a.length, p, g;
    // 如果数组小于等于 MIN_ARRAY_SORT_GRAN 或 ForkJoinPool 的并行度只有 1，则不使用并发排序
    if (n <= MIN_ARRAY_SORT_GRAN /* 8192 */ ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    else
        // 使用 ArraysParallelSortHelpers 中的对应类型排序器进行并发排序
        new ArraysParallelSortHelpers.FJInt.Sorter
            (null, a, new int[n], 0, n, 0,
             ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             MIN_ARRAY_SORT_GRAN : g).invoke();
}

public static void parallelSort(int[] a, int fromIndex, int toIndex) {
    rangeCheck(a.length, fromIndex, toIndex);
    int n = toIndex - fromIndex, p, g;
    if (n <= MIN_ARRAY_SORT_GRAN ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    else
        new ArraysParallelSortHelpers.FJInt.Sorter
            (null, a, new int[n], fromIndex, n, 0,
             ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             MIN_ARRAY_SORT_GRAN : g).invoke();
}

// 其他基本类型方法都相似，不再列出
```
参见 [ArraysParallelSortHelpers.md][parallel-sort]。

## 3.10 legacySort
```java
// 旧版本的归并排序实现（也就是普通的归并排序），将在之后版本删除
private static void legacyMergeSort(Object[] a) {
    Object[] aux = a.clone();
    mergeSort(aux, a, 0, a.length, 0);
}

private static void legacyMergeSort(Object[] a,
                                    int fromIndex, int toIndex) {
    Object[] aux = copyOfRange(a, fromIndex, toIndex);
    mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
}

private static void mergeSort(Object[] src,
                              Object[] dest,
                              int low,
                              int high,
                              int off) {
    int length = high - low;

    // 在小型数组上使用插入排序
    if (length < INSERTIONSORT_THRESHOLD) {
        for (int i=low; i<high; i++)
            for (int j=i; j>low &&
                     ((Comparable) dest[j-1]).compareTo(dest[j])>0; j--)
                swap(dest, j, j-1);
           return;
    }

    // 递归地将 src 中的内容排序到 dest 中
    int destLow  = low;
    int destHigh = high;
    low  += off;
    high += off;
    int mid = (low + high) >>> 1;
    // 交换 src 和 dest，这样就不必复制数组
    mergeSort(dest, src, low, mid, -off);
    mergeSort(dest, src, mid, high, -off);

    // 如果左半部分最大元素小于右半部分最小元素，那么数组已经有序
    if (((Comparable)src[mid-1]).compareTo(src[mid]) <= 0) {
        System.arraycopy(src, low, dest, destLow, length);
        return;
    }

    // 将排好序的 src 归并到 dest 中
    for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
        if (q >= high || p < mid && ((Comparable)src[p]).compareTo(src[q])<=0)
            dest[i] = src[p++];
        else
            dest[i] = src[q++];
    }
}

private static <T> void legacyMergeSort(T[] a, Comparator<? super T> c) {
    T[] aux = a.clone();
    if (c==null)
        mergeSort(aux, a, 0, a.length, 0);
    else
        mergeSort(aux, a, 0, a.length, 0, c);
}

private static <T> void legacyMergeSort(T[] a, int fromIndex, int toIndex,
                                        Comparator<? super T> c) {
    T[] aux = copyOfRange(a, fromIndex, toIndex);
    if (c==null)
        mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
    else
        mergeSort(aux, a, fromIndex, toIndex, -fromIndex, c);
}

@SuppressWarnings({"rawtypes", "unchecked"})
private static void mergeSort(Object[] src,
                              Object[] dest,
                              int low, int high, int off,
                              Comparator c) {
    int length = high - low;

    // Insertion sort on smallest arrays
    if (length < INSERTIONSORT_THRESHOLD) {
        for (int i=low; i<high; i++)
            for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
                swap(dest, j, j-1);
        return;
    }

    // Recursively sort halves of dest into src
    int destLow  = low;
    int destHigh = high;
    low  += off;
    high += off;
    int mid = (low + high) >>> 1;
    mergeSort(dest, src, low, mid, -off, c);
    mergeSort(dest, src, mid, high, -off, c);

    if (c.compare(src[mid-1], src[mid]) <= 0) {
        System.arraycopy(src, low, dest, destLow, length);
        return;
    }

    // Merge sorted halves (now in src) into dest
    for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
        if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
            dest[i] = src[p++];
        else
            dest[i] = src[q++];
    }
}

private static void swap(Object[] x, int a, int b) {
    Object t = x[a];
    x[a] = x[b];
    x[b] = t;
}
```

## 3.11 对象 sort
```java
public static void sort(Object[] a) {
    // 如果设置了 java.util.Arrays.useLegacyMergeSort 为 true，则使用旧版本的归并排序
    if (LegacyMergeSort.userRequested)
        legacyMergeSort(a);
    // 否则使用 ComparableTimSort
    else
        ComparableTimSort.sort(a, 0, a.length, null, 0, 0);
}

public static void sort(Object[] a, int fromIndex, int toIndex) {
    rangeCheck(a.length, fromIndex, toIndex);
    if (LegacyMergeSort.userRequested)
        legacyMergeSort(a, fromIndex, toIndex);
    else
        ComparableTimSort.sort(a, fromIndex, toIndex, null, 0, 0);
}

public static <T> void sort(T[] a, Comparator<? super T> c) {
    if (c == null) {
        sort(a);
    } else {
        if (LegacyMergeSort.userRequested)
            legacyMergeSort(a, c);
        else
            TimSort.sort(a, 0, a.length, c, null, 0, 0);
    }
}

public static <T> void sort(T[] a, int fromIndex, int toIndex,
                            Comparator<? super T> c) {
    if (c == null) {
        sort(a, fromIndex, toIndex);
    } else {
        rangeCheck(a.length, fromIndex, toIndex);
        if (LegacyMergeSort.userRequested)
            legacyMergeSort(a, fromIndex, toIndex, c);
        else
            TimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
    }
}
```
参见 [TimSort.md][tim-sort] 和 [ComparableTimSort.md][comparable-tim-sort]。

## 3.12 对象 parallelSort
```java
// Java8 引入
@SuppressWarnings("unchecked")
public static <T extends Comparable<? super T>> void parallelSort(T[] a) {
    int n = a.length, p, g;
    if (n <= MIN_ARRAY_SORT_GRAN ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        TimSort.sort(a, 0, n, NaturalOrder.INSTANCE, null, 0, 0);
    else
        // 使用对象类型并发排序类
        new ArraysParallelSortHelpers.FJObject.Sorter<T>
            (null, a,
             (T[])Array.newInstance(a.getClass().getComponentType(), n),
             0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             // 使用 NaturalOrder.INSTANCE 自然排序
             MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
}

@SuppressWarnings("unchecked")
public static <T extends Comparable<? super T>>
void parallelSort(T[] a, int fromIndex, int toIndex) {
    rangeCheck(a.length, fromIndex, toIndex);
    int n = toIndex - fromIndex, p, g;
    if (n <= MIN_ARRAY_SORT_GRAN ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        TimSort.sort(a, fromIndex, toIndex, NaturalOrder.INSTANCE, null, 0, 0);
    else
        new ArraysParallelSortHelpers.FJObject.Sorter<T>
            (null, a,
             (T[])Array.newInstance(a.getClass().getComponentType(), n),
             fromIndex, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             // 使用 NaturalOrder.INSTANCE 自然排序
             MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
}

@SuppressWarnings("unchecked")
public static <T> void parallelSort(T[] a, Comparator<? super T> cmp) {
    if (cmp == null)
        cmp = NaturalOrder.INSTANCE;
    int n = a.length, p, g;
    if (n <= MIN_ARRAY_SORT_GRAN ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        TimSort.sort(a, 0, n, cmp, null, 0, 0);
    else
        new ArraysParallelSortHelpers.FJObject.Sorter<T>
            (null, a,
             (T[])Array.newInstance(a.getClass().getComponentType(), n),
             0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             MIN_ARRAY_SORT_GRAN : g, cmp).invoke();
}
```
参见 [ArraysParallelSortHelpers.md][parallel-sort]。

## 3.13 parallelPrefix
```java
/*
使用提供的函数并行地将给定数组的每个元素累积到位。例如，如果数组最初是 [2，1，0，3]，
并且 op 执行加法，则返回时数组将为 [2，3，3，6]。
对于大型数组，并行前缀计算通常比顺序循环更有效。

Java8 引入
*/
public static <T> void parallelPrefix(T[] array, BinaryOperator<T> op) {
    Objects.requireNonNull(op);
    if (array.length > 0)
        new ArrayPrefixHelpers.CumulateTask<>
                (null, op, array, 0, array.length).invoke();
}

// 在数组给定范围进行累积操作。fromIndex 包含，toIndex 不包含
public static <T> void parallelPrefix(T[] array, int fromIndex,
                                      int toIndex, BinaryOperator<T> op) {
    Objects.requireNonNull(op);
    rangeCheck(array.length, fromIndex, toIndex);
    if (fromIndex < toIndex)
        new ArrayPrefixHelpers.CumulateTask<>
                (null, op, array, fromIndex, toIndex).invoke();
}

// 还有针对 int、long 和 double 数组的 parallelPrefix 方法，
// 模式基本一样，在此不再列出
```
参见 [ArrayPrefixHelpers.md][parallel-prefix]。

## 3.14 binarySearch
```java
/*
使用二分查找算法在 int 数组内搜索指定的值。
在进行此调用之前，必须对数组进行排序。如果未排序，则结果不确定。
如果数组包含具有指定值的多个元素，则不能保证将找到哪个元素。

如果 key 存在数组中，返回它在数组中的索引；否则返回 -(插入点 + 1)。
插入点定义为将键插入数组的位置：数组内的第一个大于 key 的索引；如果数组内的所有元素都小于指定的键，
则是数组长度。
*/
public static int binarySearch(int[] a, int key) {
    return binarySearch0(a, 0, a.length, key);
}

/*
使用二分查找算法在 int 数组范围内搜索指定的值。
在进行此调用之前，必须对范围进行排序。如果未排序，则结果不确定。
如果范围包含具有指定值的多个元素，则不能保证将找到哪个元素。

如果 key 包含在指定范围内，返回它在数组中的索引；否则返回 -(插入点 + 1)。
插入点定义为将键插入数组的位置：范围内的第一个大于 key 的索引；如果范围内的所有元素都小于指定的键，
则是 toIndex。
*/
public static int binarySearch(int[] a, int fromIndex, int toIndex,
                               int key) {
    rangeCheck(a.length, fromIndex, toIndex);
    return binarySearch0(a, fromIndex, toIndex, key);
}

private static int binarySearch0(int[] a, int fromIndex, int toIndex,
                                 int key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        int midVal = a[mid];

        if (midVal < key)
            low = mid + 1;
        else if (midVal > key)
            high = mid - 1;
        else
            return mid; // key found
    }
    return -(low + 1);  // key not found.
}

// byte、short、char、long、float、double 和对象数组的 binarySearch 方法与 int 数组类似，
// 在此不再列出
```

## 3.15 fill
```java
// 将指定的 int 值赋值到给指定的 int 数组的每个位置。
public static void fill(int[] a, int val) {
    for (int i = 0, len = a.length; i < len; i++)
        a[i] = val;
}

// 将指定的 int 值赋值到给指定的 int 数组指定范围的每个位置。
// 要填充的范围从索引 fromIndex（包含）到索引toIndex（不包含）。
// （如果 fromIndex == toIndex，则要填充的范围为空。）
public static void fill(int[] a, int fromIndex, int toIndex, int val) {
    rangeCheck(a.length, fromIndex, toIndex);
    for (int i = fromIndex; i < toIndex; i++)
        a[i] = val;
}

// boolean、byte、short、char、long、float、double 和对象数组的 fill 方法与 int 数组类似，
// 在此不再列出
```
需要注意，对象数组的`fill`方法类似浅拷贝，数组中的每个对象引用指向同一个对象。

## 3.16 copyOf
```java
/*
复制指定的数组，截断或填充为空（如果需要），以便副本具有指定的长度。
对于在原始数组和副本中均有效的所有索引，两个数组将包含相同的值。
对于副本中有效但原始索引无效的任何索引，副本将包含 null。当且仅当指定长度大于原始数组的长度时，
此类索引才会存在。
返回数组与原始数组具有完全相同的类型。
*/
@SuppressWarnings("unchecked")
public static <T> T[] copyOf(T[] original, int newLength) {
    return (T[]) copyOf(original, newLength, original.getClass());
}

/*
复制指定的数组，截断或填充为 null（如果需要），以便副本具有指定的长度。
对于在原始数组和副本中均有效的所有索引，两个数组将包含相同的值。
对于副本中有效但原始索引无效的任何索引，副本将包含 null。当且仅当指定长度大于原始数组的长度时，
此类索引才会存在。
返回数组属于 newType 类。
*/
public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
    @SuppressWarnings("unchecked")
    // 如果 newType 是 Object 数组类型，则创建 Object 数组；否则创建指定类型数组            
    T[] copy = ((Object)newType == (Object)Object[].class)
        ? (T[]) new Object[newLength]
        : (T[]) Array.newInstance(newType.getComponentType(), newLength);
    // 复制元素
    System.arraycopy(original, 0, copy, 0,
                     Math.min(original.length, newLength));
    return copy;
}

// boolean、byte、short、char、int、long、float、double 的 copyOf 方法与对象数组类似，
// 在此不再列出
```

## 3.17 copyOfRange
```java
/*
将指定数组的指定范围复制到新数组中。范围的起始索引（from）必须介于零和 original.length（包含）之间。

original[from] 处的值放置在副本的 0 位置（除非from == original.length或from == to）。
来自原始数组中后续元素的值将放入副本中的后续位置中。

范围的最终索引（to，必须大于或等于from）可以大于 original.length，在这种情况下，
副本中索引大于或等于 original.length - from 所有元素将为 null。

返回数组的长度为 to-from。返回数组与原始数组具有完全相同的类型。
*/
public static <T> T[] copyOfRange(T[] original, int from, int to) {
    return copyOfRange(original, from, to, (Class<? extends T[]>) original.getClass());
}

/*
将指定数组的指定范围复制到新数组中。范围的起始索引（from）必须介于零和 original.length（包含）之间。

original[from] 处的值放置在副本的 0 位置（除非from == original.length或from == to）。
来自原始数组中后续元素的值将放入副本中的后续位置中。

范围的最终索引（to，必须大于或等于from）可以大于 original.length，在这种情况下，
副本中索引大于或等于 original.length - from 所有元素将为 null。

返回数组的长度为 to-from。返回数组属于 newType 类。
*/
public static <T,U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
    int newLength = to - from;
    if (newLength < 0)
        throw new IllegalArgumentException(from + " > " + to);
    @SuppressWarnings("unchecked")
    T[] copy = ((Object)newType == (Object)Object[].class)
        ? (T[]) new Object[newLength]
        : (T[]) Array.newInstance(newType.getComponentType(), newLength);
    System.arraycopy(original, from, copy, 0,
                     Math.min(original.length - from, newLength));
    return copy;
}

// boolean、byte、short、char、int、long、float、double 的 copyOfRange 方法与对象数组类似，
// 在此不再列出
```

## 3.18 asList
```java
/*
返回由数组支持的固定大小的列表。（对返回列表的更改为作用到数组上。）
与 Collection.toArray() 结合使用，此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
返回的列表是可序列化的，并实现 RandomAccess。
*/
@SafeVarargs
@SuppressWarnings("varargs")
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}
```
`@SafeVarargs`注解参见 [SafeVarargs.md][safe-varargs]。

## 3.19 setAll
```java
/*
使用提供的生成器函数来计算每个元素，并写入相应位置。
如果生成器函数引发异常，它将被传播到调用者，并且数组将处于不确定状态。

Java8 引入
*/
public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
    Objects.requireNonNull(generator);
    for (int i = 0; i < array.length; i++)
        array[i] = generator.apply(i);
}

// int、long、double 的 setAll 方法与对象数组类似，在此不再列出
```

## 3.20 parallelSetAll
```java
/*
使用提供的生成器函数并行地计算每个元素，并写入相应位置。
如果生成器函数引发异常，它将被传播到调用者，并且数组将处于不确定状态。

Java8 引入
*/
public static <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator) {
    Objects.requireNonNull(generator);
    IntStream.range(0, array.length)
        .parallel()
        .forEach(i -> { array[i] = generator.apply(i); });
}

// int、long、double 的 parallelSetAll 方法与对象数组类似，在此不再列出
```

## 3.21 spliterator
```java
/*
返回一个覆盖指定数组所有元素的 Spliterator。

此 Spliterator 具有 Spliterator.SIZED，Spliterator.SUBSIZED，
Spliterator.ORDERED 和 Spliterator.IMMUTABLE 特征。
*/
public static <T> Spliterator<T> spliterator(T[] array) {
    return Spliterators.spliterator(array,
                                    Spliterator.ORDERED | Spliterator.IMMUTABLE);
}

/*
返回一个覆盖数组指定范围元素的 Spliterator。

此 Spliterator 具有 Spliterator.SIZED，Spliterator.SUBSIZED，
Spliterator.ORDERED 和 Spliterator.IMMUTABLE 特征。
*/
public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
                                    Spliterator.ORDERED | Spliterator.IMMUTABLE);
}

public static Spliterator.OfInt spliterator(int[] array) {
    return Spliterators.spliterator(array,
                                    Spliterator.ORDERED | Spliterator.IMMUTABLE);
}

public static Spliterator.OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
                                    Spliterator.ORDERED | Spliterator.IMMUTABLE);
}

// long、double 的 spliterator 方法与 int 数组类似，在此不再列出
```
参见 [Spliterator.md][spliterator]。

## 3.22 stream
```java
// 返回以指定数组为源的顺序 Stream。
public static <T> Stream<T> stream(T[] array) {
    return stream(array, 0, array.length);
}

// 返回以数组指定范围为源的顺序 Stream。
public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
    return StreamSupport.stream(spliterator(array, startInclusive, endExclusive), false);
}

public static IntStream stream(int[] array) {
    return stream(array, 0, array.length);
}

public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
    return StreamSupport.intStream(spliterator(array, startInclusive, endExclusive), false);
}

// long、double 的 stream 方法与 int 数组类似，在此不再列出
```


[hash-code]: ../lang/哈希算法.md
[boolean]: ../lang/Boolean.md
[tim-sort]: TimSort.md
[comparable-tim-sort]: ComparableTimSort.md
[dual-pivot]: DualPivotQuickSort.md
[parallel-sort]: ArraysParallelSortHelpers.md
[parallel-prefix]: ArrayPrefixHelpers.md
[safe-varargs]: ../lang/SafeVarargs.md
[spliterator]: Spliterator.md