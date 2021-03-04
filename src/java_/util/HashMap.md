`java.util.HashMap`类的声明如下：
```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
```

# 0. 介绍

## 0.1 简介

基于哈希表的`Map`接口的实现。此实现提供所有可选的`Map`操作，并允许`null`值和`null`键。
（`HashMap`类与`Hashtable`大致等效，不同之处在于它是不同步的，并且允许`null`。）
此类不保证键值对的顺序。特别是，它不能保证顺序会随着时间的推移保持恒定。

假设哈希函数将元素均匀地分散在存储桶（哈希表中的一个槽）中，则此实现为基本操作（`get`和`put`）提供常数时间的性能。
集合视图上的迭代所需的时间与`HashMap`实例的“容量”（存储桶数）及其大小（键值对数）成正比。
因此，如果迭代性能很重要，则不要将初始容量设置得过高（或负载因子过低），这一点非常重要。

`HashMap`的实例具有两个影响其性能的参数：初始容量和负载因子。容量是哈希表中存储桶的数量，
初始容量只是创建哈希表时的容量。负载因子是在自动增长哈希表容量之前所允许的哈希表元素数和容量之比。
当哈希表中的元素数量超过负载因子和当前容量的乘积时，哈希表将被重新哈希（即，内部数据结构将被重建），
然后哈希表的容量增大为原来的大约两倍。

通常，默认负载因子（0.75）在时间和空间成本之间提供了一个很好的权衡。较高的值会减少空间开销，
但会增加查找成本（在`HashMap`类的大多数操作中都得到体现，包括`get`和`put`）。设置其初始容量时，
应考虑`HashMap`中的预期条目数及其负载因子，以最大程度地减少重哈希操作的次数。
如果初始容量大于最大条目数除以负载因子，则不会发生重哈希操作。

如果有许多键具有相同的`hashCode`，则肯定会降低`HashMap`的性能。为了减少影响，此类可以使用键之间的比较顺序来构造**红黑树**。
当键为`Comparable`时，则可以更好的进行这种比较。

请注意，此实现未同步。如果多个线程同时访问`HashMap`，并且至少有一个线程在结构上修改此`HashMap`，则必须在外部进行同步。
 （结构修改是添加或删除一个或多个键值对的任何操作；仅更改已经包含的键相关联的值不是结构修改。）
通常通过在封装了`HashMap`的某个对象上进行同步来实现。如果不存在这样的对象，
则应使用`Collections.synchronizedMap`方法“包装”`HashMap`。最好在创建时完成此操作，以防止意外不同步地访问`HashMap`：
```java
Map m = Collections.synchronizedMap(new HashMap(...));
```

此类的所有“集合视图方法”返回的迭代器都是快速失败的：如果在创建迭代器后的任何时间进行结构修改，
则除了通过迭代器自己的`remove`方法之外，该迭代器都将抛出`ConcurrentModificationException`。
因此，面对并发修改，迭代器会快速干净地失败，而不会在未来的不确定时间冒着任意，不确定的行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为通常来说，在存在不同步的并发修改的情况下，
不可能做出任何严格的保证。快速失败的迭代器会尽最大努力抛出`ConcurrentModificationException`。因此，
编写依赖于此异常的程序是错误的，迭代器的快速失败行为应仅用于检测错误。

更多信息参见 [Map.md][map] 和 [AbstractMap.md][abstract-map]。

## 0.2 实现要点

`HashMap`是一个散列表，包含很多存储桶，通常情况下每个存储桶使用`Node`结点构造链表结构。
但是当链表太长时（长度大于`TREEIFY_THRESHOLD`），它们会被转换成红黑树，结构类似于`java.util.TreeMap`。
大多数方法使用`instanceof`检查节点，以确定是使用普通的链表，还是使用`TreeNode`方法。
`TreeNode`可以像链表一样被遍历和使用，在具有大量元素的情况下支持更快的查找。然而，
由于在正常使用中的绝大多数存储桶的元素不会太多，因此在方法中检查`TreeNode`的操作可能会被延迟。

`TreeNode`主要通过哈希码进行排序，如果哈希码相同，且两个元素具有相同的`class C implements Comparable<C>`类型，
那么就用它们的`compareTo`方法来排序。(我们通过反射检查泛型来验证这一点--参见`comparableClassFor`方法)。
在插入时，使用类名和`identityHashCode`作为最后的比较手段。

因为`TreeNode`的大小是`Node`的两倍，所以我们只在存储桶包含足够多的元素时才会使用它们
（参见`TREEIFY_THRESHOLD`）。当存储桶变得太小的时候(由于`remove`或`resizing`)，它们又会被转换回链表。
在用户哈希码分布良好的情况中，`TreeNode`很少被使用。理想情况下，也就是随机哈希码，
桶中结点数量遵循泊松分布，如果负载因子为默认的 0.75，则泊松分布的平均参数约为 0.5。
尽管由于大小调整的粒度，会有较大的方差。如果忽略方差，链表大小为`k`的可能性是`(exp(-0.5) * pow(0.5, k) / factorial(k))`。
`k`的几种大小可能性如下：
 - 0:    0.60653066
 - 1:    0.30326533
 - 2:    0.07581633
 - 3:    0.01263606
 - 4:    0.00157952
 - 5:    0.00015795
 - 6:    0.00001316
 - 7:    0.00000094
 - 8:    0.00000006
 
红黑树根通常是它的第一个节点。然而，有时（目前只有在`Iterator.remove`时），根节点可能会在其他地方，
但可以通过`parent`链接（方法`TreeNode.root()`）恢复。

一些内部方法接受一个哈希码作为参数（通常由`public`方法提供），允许它们相互调用而不需要重新计算用户哈希码。
大多数内部方法还接受一个 "tab" 参数，通常是当前的哈希表，但在调整大小或转换时可能是一个新的或旧的哈希表。

当链表转化为红黑树、被拆分或红黑树转化为链表时，我们让它们保持相同的相对访问/遍历顺序（即字段`Node.next`），
以更好地保存位置性，并稍微简化了对调用`iterator.remove`的拆分和遍历的处理。

由于子类`LinkedHashMap`的存在，普通模式与树模式之间调用和转换变得复杂。我们定义了`afterNodeAccess(Node<K,V> p)`、
`afterNodeInsertion`和`afterNodeRemoval(Node<K,V> p)`回调方法，这些方法在插入、删除和访问后被调用，
允许`LinkedHashMap`实现自己的机制。(这也要求将`map`实例传递给一些可能创建新节点的方法。)

# 1. 内部类

## 1.1 Node
```java
// 基本哈希表节点。单链表结构，用于大多数条目。
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;

    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }

    public final K getKey()        { return key; }
    public final V getValue()      { return value; }
    public final String toString() { return key + "=" + value; }

    public final int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    public final V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Map.Entry) {
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            if (Objects.equals(key, e.getKey()) &&
                Objects.equals(value, e.getValue()))
                return true;
        }
        return false;
    }
}
```

## 1.2 TreeNode
```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V>
```
红黑树结点。继承`LinkedHashMap.Entry`（进而继承`Node`），因此也可以用作常规节点或链接节点。
红黑树的定义和算法参见算法导论。

红黑树性质：
1. 每个结点或是红色、或是黑色的。
2. 根结点是黑色的。
3. 底层的`null`结点（又叫外部结点）是黑色的。
4. 如果一个结点是红色的，则它的两个子结点都是黑色的。这保证了不会有连续的红结点。
5. 对于每个结点，从该结点到其所有后代叶结点的简单路径上，均包含相同数目的黑色结点。

`LinkedHashMap.Entry`定义如下所示：
```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

### 1.2.1 成员字段
```java
// 父结点指针。用于恢复根节点，以及可以使用循环替代递归
TreeNode<K,V> parent;
// 左子结点指针
TreeNode<K,V> left;
// 右子结点指针
TreeNode<K,V> right;
// 链式结构中前一个结点
TreeNode<K,V> prev;
// 当前结点是不是红结点
boolean red;
```

### 1.2.2 构造器
```java
TreeNode(int hash, K key, V val, Node<K,V> next) {
    super(hash, key, val, next);
}
```

### 1.2.3 方法

#### 1.2.3.1 root
```java
// 返回包含此节点的树的根。
final TreeNode<K,V> root() {
    for (TreeNode<K,V> r = this, p;;) {
        if ((p = r.parent) == null)
            return r;
        r = p;
    }
}
```

#### 1.2.3.2 checkInvariants
```java
// 递归地检查红黑树是否合法
static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
    TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
            tb = t.prev, tn = (TreeNode<K,V>)t.next;
    // 如果 t 的 prev 结点的 next 指针不等于 t，返回 false
    if (tb != null && tb.next != t)
        return false;
    // 如果 t 的 next 结点的 prev 指针不等于 t, 返回 false
    if (tn != null && tn.prev != t)
        return false;
    // 如果 t 不是它的 parent 结点的子结点，返回 false
    if (tp != null && t != tp.left && t != tp.right)
        return false;
    // 如果 t 的左子节点 tl 的 parent 指针不等于 t，或者 tl 的 hash 大于 t 的 hash，
    // 返回 false
    if (tl != null && (tl.parent != t || tl.hash > t.hash))
        return false;
    // 如果 t 的右子节点 tr 的 parent 指针不等于 t，或者 tr 的 hash 小于 t 的 hash，
    // 返回 false
    if (tr != null && (tr.parent != t || tr.hash < t.hash))
        return false;
    // 如果有连续的红结点，返回 false
    if (t.red && tl != null && tl.red && tr != null && tr.red)
        return false;
    // 左子结点存在，递归地检查左子结点
    if (tl != null && !checkInvariants(tl))
        return false;
    // 右子结点存在，递归地检查右子结点
    if (tr != null && !checkInvariants(tr))
        return false;
    return true;
}
```

#### 1.2.3.3 moveRootToFront
```java
// 确保给定的根是其存储桶的第一个结点。
static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
    int n;
    if (root != null && tab != null && (n = tab.length) > 0) {
        // 计算 root 结点在哈希表中的下标
        int index = (n - 1) & root.hash;
        // 获取计算下标处存储桶第一个结点
        TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
        // 如果 root 不是第一个结点，则将其移到开头
        if (root != first) {
            Node<K,V> rn;
            // 将 root 放到开头
            tab[index] = root;
            TreeNode<K,V> rp = root.prev;
            // 如果存在的话，将 root 原来的 prev 和 next 连接起来
            if ((rn = root.next) != null)
                ((TreeNode<K,V>)rn).prev = rp;
            if (rp != null)
                rp.next = rn;
            // 将 root 和 first 连接起来
            if (first != null)
                first.prev = root;
            root.next = first;
            root.prev = null;
        }
        assert checkInvariants(root);
    }
}
```

#### 1.2.3.4 查找
```java
// 从当前结点处开始查找和键 k 匹配的结点。
// h 是 k 的哈希码；kc 是 k 的 Class
final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
    TreeNode<K,V> p = this;
    do {
        int ph, dir; K pk;
        TreeNode<K,V> pl = p.left, pr = p.right, q;
        // 首先使用哈希码进行比较
        if ((ph = p.hash) > h)
            p = pl;
        else if (ph < h)
            p = pr;
        // hash 码相等，就比较 key 是否相等
        else if ((pk = p.key) == k || (k != null && k.equals(pk)))
            return p;
        // 否则如果左子结点为 null，则移动到右子节点
        else if (pl == null)
            p = pr;
        // 否则如果右子结点为 null，则移动到左子节点
        else if (pr == null)
            p = pl;
        // 否则如果左右子结点都不为 null
        else if ((kc != null ||
                  // 如果 k 的形式为 “class C implements Comparable<C>”，
                  // 则返回 k 的 Class，否则返回 null。
                  (kc = comparableClassFor(k)) != null) &&
                 // 如果 pk 的 Class 不等于 kc，则返回 0；否则返回 k.compareTo(pk)。
                 // 由于之前使用过 k.equals(pk)，所以按常理来说 k.compareTo(pk) 不会返回 0
                 (dir = compareComparables(kc, k, pk)) != 0)
            // 根据比较结果决定移到左子结点还是右子结点
            p = (dir < 0) ? pl : pr;
        // 否则如果不能进行 Comparable 比较，就先在右子树中查找。
        // 之所以不使用 “p = pr” 的方式，是因为此时不能确定查找键在哪个子树里面，
        // 需要都搜索一遍才能确定
        else if ((q = pr.find(h, k, kc)) != null)
            return q;
        // 否则如果右子树中没有找到，就移到左子结点
        else
            p = pl;
    } while (p != null);
    return null;
}

// 从根节点开始查找键 k
final TreeNode<K,V> getTreeNode(int h, Object k) {
    return ((parent != null) ? root() : this).find(h, k, null);
}
```

#### 1.2.3.4 tieBreakOrder
```java
/*
用于在 hashCode 相等且不可比较时对插入进行排序。我们不需要总体顺序完全一致，
只需一个一致的插入规则即可在再平衡中保持等价性。
*/
static int tieBreakOrder(Object a, Object b) {
    int d;
    // 如果 a、b 的类名相等，则比较它们的 identityHashCode；
    // 否则比较 a、b 的类名
    if (a == null || b == null ||
        (d = a.getClass().getName().
         compareTo(b.getClass().getName())) == 0)
        // 如果 a、b 的 identityHashCode 相等，也返回 -1。
        // 这样就会导致向左子树插入
        d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
             -1 : 1);
    return d;
}
```

#### 1.2.3.5 旋转
```java
/*
对 p 进行左旋转，操作如下：
    pp            pp
    |             |
    p      =>     r
   / \           / \
  l   r         p  rr
     / \       / \
    rl  rr    l  rl
*/
static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                      TreeNode<K,V> p) {
    TreeNode<K,V> r, pp, rl;
    if (p != null && (r = p.right) != null) {
        // 将 p 的右子结点 r 的 left 赋值给 p 的 right
        if ((rl = p.right = r.left) != null)
            rl.parent = p;
        // 如果 p 的父结点 pp 为 null，表示 p 是根节点，
        // 则左旋转后根结点变为 r，且根节点颜色设为黑色（红黑树根节点颜色在操作结束后必须为黑色）
        if ((pp = r.parent = p.parent) == null)
            (root = r).red = false;
        // 否则将 pp 和 r 连接
        else if (pp.left == p)
            pp.left = r;
        else
            pp.right = r;
        // 连接 r 和 p
        r.left = p;
        p.parent = r;
    }
    // 根节点保持不变或变成 r
    return root;
}

/*
对 p 进行右旋转，操作如下：
    pp            pp
    |             |
    p      =>     l
   / \           / \
  l   r         ll  p
 / \               / \
ll  lr            lr  r
*/
static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                       TreeNode<K,V> p) {
    TreeNode<K,V> l, pp, lr;
    if (p != null && (l = p.left) != null) {
        // 将 p 的左子结点 l 的 right 赋值给 p 的 left
        if ((lr = p.left = l.right) != null)
            lr.parent = p;
        // 如果 p 的父结点 pp 为 null，表示 p 是根节点，
        // 则左旋转后根结点变为 l，且根节点颜色设为黑色（红黑树根节点颜色在操作结束后必须为黑色）
        if ((pp = l.parent = p.parent) == null)
            (root = l).red = false;
        // 否则将 pp 和 l 连接
        else if (pp.right == p)
            pp.right = l;
        else
            pp.left = l;
        // 连接 l 和 p
        l.right = p;
        p.parent = l;
    }
    // 根节点保持不变或变成 l
    return root;
}
```

#### 1.2.3.6 putTreeVal
```java
// 将键值对插入树中。如果 k 已经存在就返回该结点。
final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                               int h, K k, V v) {
    Class<?> kc = null;
    boolean searched = false;
    // 获取根节点
    TreeNode<K,V> root = (parent != null) ? root() : this;
    for (TreeNode<K,V> p = root;;) {
        int dir, ph; K pk;
        // 先比较哈希码
        if ((ph = p.hash) > h)
            dir = -1;
        else if (ph < h)
            dir = 1;
        // 哈希码相等，就比较键是否相等
        else if ((pk = p.key) == k || (k != null && k.equals(pk)))
            // 键相等直接返回此结点
            return p;
        // 如果键也不相等，且不能使用 Comparable 比较
        else if ((kc == null &&
                  (kc = comparableClassFor(k)) == null) ||
                 (dir = compareComparables(kc, k, pk)) == 0) {
            if (!searched) {
                TreeNode<K,V> q, ch;
                // 使用 searched 标志变量，只进行一次局部搜索；
                // 因为这次的局部搜索会搜索当前整个子树，因此只需要进行一次就可以了
                searched = true;
                // 在左右子树中搜索看看能不能找到匹配结点
                if (((ch = p.left) != null &&
                     (q = ch.find(h, k, kc)) != null) ||
                    ((ch = p.right) != null &&
                     (q = ch.find(h, k, kc)) != null))
                    // 找到返回此结点
                    return q;
            }
            // 使用最后的比较手段：比较类名和 identityHashCode
            dir = tieBreakOrder(k, pk);
        }

        TreeNode<K,V> xp = p;
        // 根据 dir 的结果，让 p 移动到左结点或右结点；
        // 如果 p 等于 null，则需要插入新结点
        if ((p = (dir <= 0) ? p.left : p.right) == null) {
            Node<K,V> xpn = xp.next;
            TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
            // 根据 dir，将新结点作为左子结点或右子结点
            if (dir <= 0)
                xp.left = x;
            else
                xp.right = x;
            // 新结点作为它的父结点的 next
            xp.next = x;
            // 新结点的 prev 和 parent 指针指向它的父结点
            x.parent = x.prev = xp;
            // 父结点原来的 next 结点不为 null，则把新结点插入父结点和原来 next 结点之间
            if (xpn != null)
                ((TreeNode<K,V>)xpn).prev = x;
            // 平衡插入，并保证根节点在开头
            moveRootToFront(tab, balanceInsertion(root, x));
            // 插入了新结点，因此返回 null
            return null;
        }
    }
}
```

#### 1.2.3.7 balanceInsertion
```java
// 新结点 x 已经插入到了以 root 为根的红黑树中，balanceInsertion 进行红黑树的平衡操作。
// 返回新的根节点
static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                            TreeNode<K,V> x) {
    // 新结点总是红结点。这样就不会破坏性质 5（参见 TreeNode 开头注释）。
    // 如果 x 的父结点 xp 是根结点，则 xp 会是黑结点。
    // 这样，可能被破坏的就有性质 2、性质 4。
    // 当 x 是唯一的结点时，就会破坏性质 4；但 xp 是红结点时，就会破坏性质 4。
    // 我们需要修复对性质 2、4 的破坏。
    x.red = true;
    for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
        // 如果 x 的父结点 xp 为 null，那么 x 是根节点
        if ((xp = x.parent) == null) {
            // 将根结点颜色变为黑色，修正对性质 5 的破坏
            x.red = false;
            return x;
        }
        // 否则如果 xp 是黑结点，或者 xp 是根节点，则性质 2、4 都保持，已经平衡。
        else if (!xp.red || (xpp = xp.parent) == null)
            return root;
        // 否则 xp 是红结点，性质 4 被破坏，需要修正。如果 xp 是 x 的爷爷结点 xpp 的左子节点
        if (xp == (xppl = xpp.left)) {
            // 因为 xp 是红色的，由性质 4 可推出 xpp 一定是黑色的。

            // 【情况1】：如果 xpp 的右子节点存在，且它是红结点，此时 xpp 左右子结点都是红结点。
            if ((xppr = xpp.right) != null && xppr.red) {
                /*
                这种【情况1】中，我们将 xpp 和它的子结点颜色反转，这保证了路径上的黑结点
                数量保持不变，性质 5 没有被破坏。
                x 向上移动，现在性质 4 的破坏只可能发生在新的 x 和它的父结点之间。

                        |                 |
                     (xpp)              [xpp] <- 新的 x
                    /     \            /     \
                  [xp]   [xppr]  =>  (xp)   (xppr)
                   |                  |
                  [x]                [x]

                注：我用 (..) 表示黑结点，[..] 表示红结点。不加任何括号表示颜色未知。
                同时，直直的链接表示可以是左子结点也可以是右子结点。
                */
                // 反转颜色
                xppr.red = false;
                xp.red = false;
                xpp.red = true;
                // x 向上移动
                x = xpp;
            }
            // 否则从 xpp 到 x 将有两个连续的红结点
            else {
                // 【情况2】：如果 x 是 xp 的右结点
                if (x == xp.right) {
                    /*
                    通过左旋，将【情况2】变为【情况3】，进行统一处理

                        (xpp)          (xpp)
                       /              /
                     [xp]     =>    [xp]
                         \          /
                         [x]      [x]
                    */
                    root = rotateLeft(root, x = xp);
                    // 重新设置 xp 和 xpp
                    xpp = (xp = x.parent) == null ? null : xp.parent;
                }
                if (xp != null) {
                    /*
                    【情况3】：有连续两个左子红结点。通过改变结点颜色和右旋转，
                    我们仍然维持了性质 5，并且此时性质 4 也被修复：不再有连续的红结点。
                    循环将会退出。

                          (xpp)         (xp)
                         /             /    \
                       [xp]     =>   [x]   [xpp]
                       /
                     [x]
                    */
                    xp.red = false;
                    if (xpp != null) {
                        xpp.red = true;
                        root = rotateRight(root, xpp);
                    }
                }
            }
        }
        // 【镜像】如果 xp 是父父结点 xpp 的右子结点
        else {
            if (xppl != null && xppl.red) {
                /*
                        |                 |
                     (xpp)              [xpp] <- 新的 x
                    /     \            /     \
                 [xppl]   [xp]  =>  (xppl)   (xp)
                           |                  |
                          [x]                [x]
                */
                xppl.red = false;
                xp.red = false;
                xpp.red = true;
                x = xpp;
            }
            else {
                if (x == xp.left) {
                    /*
                     (xpp)            (xpp)
                          \                \
                          [xp]   =>       [xp]
                          /                  \
                        [x]                  [x]
                    */
                    root = rotateRight(root, x = xp);
                    xpp = (xp = x.parent) == null ? null : xp.parent;
                }
                if (xp != null) {
                    /*
                     (xpp)              (xp)
                          \            /    \
                          [xp]   =>  [xpp]  [x]
                             \
                             [x]
                    */
                    xp.red = false;
                    if (xpp != null) {
                        xpp.red = true;
                        root = rotateLeft(root, xpp);
                    }
                }
            }
        }
    }
}
```

#### 1.2.3.8 treeify
```java
// 将以当前结点开头的链表转化为红黑树
final void treeify(Node<K,V>[] tab) {
    TreeNode<K,V> root = null;
    // x 是当前结点
    for (TreeNode<K,V> x = this, next; x != null; x = next) {
        // 获取 x 的下一个结点
        next = (TreeNode<K,V>)x.next;
        x.left = x.right = null;
        // 如果根节点还未指定，将 x 作为根节点。也就是将当前节点(this)作为根节点
        if (root == null) {
            x.parent = null;
            // 根节点是黑结点
            x.red = false;
            root = x;
        }
        // 否则根节点已指定
        else {
            K k = x.key;
            int h = x.hash;
            Class<?> kc = null;
            // 从根节点开始，插入 x
            for (TreeNode<K,V> p = root;;) {
                int dir, ph;
                K pk = p.key;
                // 先比较哈希码
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                // 这里和 putTreeVal 有些差别，没有使用 equals 比较键的过程，
                // 这是因为我们树化的过程需要安排每个结点的位置，必须给出大小排列。

                // 如果不能使用 Comparable 比较，或比较结果为 0
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0)
                    // 使用最后的比较手段：比较类名和 identityHashCode
                    dir = tieBreakOrder(k, pk);

                TreeNode<K,V> xp = p;
                // 根据 dir 的结果，让 p 移到左结点或右结点；
                // 如果 p 等于 null，则这里就是插入 x 的位置
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    // 将 xp 和 x 连接起来
                    x.parent = xp;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    // 对插入进行平衡
                    root = balanceInsertion(root, x);
                    break;
                }
            }
        }
    }
    // 将 root 移到存储桶开头
    moveRootToFront(tab, root);
}
```

#### 1.2.3.9 untreeify
```java
// 将从当前结点开头的链式结构上的所有结点转化为普通的链表
final Node<K,V> untreeify(HashMap<K,V> map) {
    Node<K,V> hd = null, tl = null;
    for (Node<K,V> q = this; q != null; q = q.next) {
        // 将 q 转换为 Node 结点，q.next 设为 null
        Node<K,V> p = map.replacementNode(q, null);
        // 连接上个结点和 p
        if (tl == null)
            hd = p;
        else
            tl.next = p;
        tl = p;
    }
    return hd;
}
```

#### 1.2.3.10 removeTreeNode
```java
/*
删除当前结点，这个结点必须在这个调用之前存在。

如果当前树的结点数少于阈值 UNTREEIFY_THRESHOLD，那么就将树转化为链表。
经测试触发转换的的节点数在 2 到 6 个之间，这取决于树的结构。

@param movable 如果为 false，则删除操作后不会再移动结点的位置
*/
final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                          boolean movable) {
    int n;
    if (tab == null || (n = tab.length) == 0)
        return;
    // 计算当前结点(this)在哈希表中的下标
    int index = (n - 1) & hash;
    // 获取第一个结点 first 和根结点。根结点一般情况下就是第一个结点
    TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
    // 获取当前结点的前驱结点 pred，和后继结点 succ
    TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
    // 调整被删除结点前后结点的 prev 和 next 指针。
    // 如果 pred 为 null，则当前结点是根结点
    if (pred == null)
        // 让 succ 作为存储桶第一个结点
        tab[index] = first = succ;
    else
        // 否则将 pred 和 succ 连接
        pred.next = succ;
    // succ 不为 null，将它的 prev 指针指向 pred
    if (succ != null)
        succ.prev = pred;
    // 如果 first 等于 null，表示树中只有一个结点，删除后直接返回
    if (first == null)
        return;
    // 如果 root 不是根结点，找出真正的根结点
    if (root.parent != null)
        root = root.root();
    // 如果 root 等于 null，或者树已经很小了
    if (root == null
        || (movable
            && (root.right == null
                || (rl = root.left) == null
                || rl.left == null))) {
        // 将树转化为链表。之前的代码已经将当前结点的后继结点作为存储桶开头，
        // 因此在 untreeify 方法中，将跳过当前结点
        tab[index] = first.untreeify(map);
        return;
    }

    // 下面的代码中，p 是需要被删除的结点。如果 p 是黑结点，则删除 p 将引起红黑性质的破坏。
    TreeNode<K,V> p = this, pl = left, pr = right, replacement;
    // 如果待删除结点的左子结点和右子结点都存在
    if (pl != null && pr != null) {
        TreeNode<K,V> s = pr, sl;
        // 找到右子树中的最左结点 s，这也是右子树中具有最小值的结点
        while ((sl = s.left) != null) // find successor
            s = sl;
        // 交换待删除结点和 s 的颜色
        boolean c = s.red; s.red = p.red; p.red = c; // swap colors
        TreeNode<K,V> sr = s.right;
        // 令 pp 为 p 的父结点
        TreeNode<K,V> pp = p.parent;
        // 下面我们将要交换 p 和 s 的位置
        // 如果 s 是 p 的右子结点。说明 p 的右子树中只有 s 一个结点
        if (s == pr) { // p was s's direct parent
            /*
               p       s
                \  =>   \
                 s       p
            */
            p.parent = s;
            s.right = p;
        }
        // 否则 s 不是唯一的结点
        else {
            /*
                p           s
                 \           \
                  ..          ..
                   |   =>      |
                   sp          sp
                  /           /
                 s           p
            */
            TreeNode<K,V> sp = s.parent;
            if ((p.parent = sp) != null) {
                if (s == sp.left)
                    sp.left = p;
                else
                    sp.right = p;
            }
            if ((s.right = pr) != null)
                pr.parent = s;
        }
        p.left = null;
        // 将 s 的右结点和 p 连接
        if ((p.right = sr) != null)
            sr.parent = p;
        // 将 p 的左结点和 s 连接
        if ((s.left = pl) != null)
            pl.parent = s;
        // 将 p 的父结点 pp 和 s 连接
        if ((s.parent = pp) == null)
            // 如果 p 原来是根节点，则新的根节点指向 s
            root = s;
        else if (p == pp.left)
            pp.left = s;
        else
            pp.right = s;
        if (sr != null)
            replacement = sr;
        else
            replacement = p;
    }
    /*
    此时 p 如果有子结点，它的子结点将是叶子结点
       p      p         p
      /   或    \   或
     pl         pr
    */
    // 如果 p 没有右结点，左结点 pl 存在
    else if (pl != null)
        // replacement 指向 pl
        replacement = pl;
    // 如果 p 没有左结点，右结点 pr 存在
    else if (pr != null)
        // replacement 指向 pr
        replacement = pr;
    // 否则 p 是叶子结点
    else
        // replacement 指向 p
        replacement = p;

    /*
    p 要被删除，可以用 p 的后继结点 replacement（下图中简写为 re）替换 p。有以下两种情况：

    （1）replacement 指向 p
          pp       pp
         /           \
        p <- re       p <- re
        此时需要用 null 替换 p，也就是删除 p。为了后续的便利性，
        我们假定 p 此时已被 null 替换。删除操作将在平衡后进行。
        如果不这样处理，而是直接删除 p，并从 pp 开始平衡的话，看看下面的情况：
             |
            (pp)
           /    \
         (p)    [r]
               /   \
             (l)   (r)
        这将导致 balanceDeletion 方法中不会对 r 进行平衡。

    （2）replacement 不指向 p
           p    p
          /      \
        re        re
        此时可以用 replacement 替换 p。
    */

    // 如果 replacement 不等于 p，用 replacement 替换 p
    if (replacement != p) {
        /*
           pp       pp
           |        |
           p   =>   re
           |        |
           re       p
        */
        TreeNode<K,V> pp = replacement.parent = p.parent;
        if (pp == null)
            root = replacement;
        else if (p == pp.left)
            pp.left = replacement;
        else
            pp.right = replacement;
        // 经过交换，p 被换到了树的最底层，将 p 解除链接，从树中删除
        p.left = p.right = p.parent = null;
    }

    // 此时，p 是叶结点。如果 p 是红结点，则可以删除它而不破坏任何性质。
    // 否则，p 是黑结点，此时需要进行平衡。
    TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

    // 如果 replacement 等于 p，则 p 已被视为 null 结点，结果平衡后可以安全地删除 p
    if (replacement == p) {
        TreeNode<K,V> pp = p.parent;
        p.parent = null;
        if (pp != null) {
            if (p == pp.left)
                pp.left = null;
            else if (p == pp.right)
                pp.right = null;
        }
    }
    // 如果允许移动结点
    if (movable)
        // 将根结点移到存储桶的开头
        moveRootToFront(tab, r);
}
```

#### 1.2.3.11 balanceDeletion
```java
// 从树底开始平衡树，以便能够安全地删除结点 x
static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                           TreeNode<K,V> x) {
    /*
    在删除操作中，由于被删除的结点是黑结点，原来路径上的黑结点数量减 1，会导致性质 5 的破坏。
    如果根结点被替换为红结点，则性质 2 可能被破坏；
    如果 x 和父结点 xp 都是红结点，又会导致性质 4 被破坏。

    为了修复性质 5，我们需要使得 x 路径上的黑结点数量加 1；在此过程中，也修复性质 2、4。
    */

    for (TreeNode<K,V> xp, xpl, xpr;;) {
        // 如果 x 等于 null 或指向根结点，则可以直接返回
        if (x == null || x == root)
            return root;
        // 如果平衡后 x 变成了根结点
        else if ((xp = x.parent) == null) {
            // 将根结点置为黑色，修正性质 2
            x.red = false;
            return x;
        }
        // 如果 x 是红结点，将其变为黑结点，这样可以增加路径上的黑色结点数量，
        // 修复性质 5。而且如果 x 是红结点，则由性质 2、4，它的父结点必定存在且为黑结点。
        // 因此，树的性质全部保持，可以返回。
        else if (x.red) {
            x.red = false;
            return root;
        }
        // 否则如果 x 是 xp 的左子结点
        else if ((xpl = xp.left) == x) {
            // 【情况1】 如果 xp 的右子结点存在且是红结点
            if ((xpr = xp.right) != null && xpr.red) {
                /*
                在【情况1】中，改变结点颜色和进行左旋转。这些变化不会改变
                任何路径上的黑结点数量，也不会破坏其他性质。
                【情况1】主要用来向【情况2】、【情况3】、【情况4】转换。

                     |                   |
                    (xp)               (xpr)
                   /    \      =>     /     \
                 (x)   [xpr]        [xp]    (r)
                      /     \      /    \
                     (l)    (r)   (x)   (l) <- 新的 xpr
                */
                xpr.red = false;
                xp.red = true;
                root = rotateLeft(root, xp);
                xpr = (xp = x.parent) == null ? null : xp.right;
            }
            // 如果 xpr 等于 null，x 向上移动。
            if (xpr == null)
                x = xp;
            else {
                TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                // 【情况2】如果 xpr 的两个子结点都不是红结点
                if ((sr == null || !sr.red) &&
                    (sl == null || !sl.red)) {
                    /*
                    在【情况2】中，将 xpr 变为红结点，这样 xpr 路径上的
                    黑结点数量减 1，和 x 路径上黑结点数量相等了。但是，xp 这一路径
                    上的黑结点数量将会减少 1。
                    如果是由【情况1】转到【情况2】，那么 xp 就是红结点，通过将其变为
                    黑结点，可以让路径上的黑结点数量保持平衡，则红黑树所有的性质得以保持，
                    可以返回。

                        xp                 xp <- 新的 x
                       /  \               /  \
                     (x)  (xpr)    =>   (x)  [xpr]
                          /    \             /   \
                         (l)   (r)          (l)  (r)
                    */
                    xpr.red = true;
                    // x 向上移动
                    x = xp;
                }
                else {
                    // 否则 xpr 有一个红结点。

                    // 【情况3】如果 xpr 的左子结点是红色的
                    if (sr == null || !sr.red) {
                        /*
                        通过变色和右旋转，将【情况3】变为【情况4】
                             xp                xp
                            /  \              /  \
                          (x)  (xpr)        (x)  (sl) <- 新的 xpr
                              /     \   =>          \
                             [sl]   (sr)            [xpr]
                                                      \
                                                      (sr)
                        */
                        if (sl != null)
                            sl.red = false;
                        xpr.red = true;
                        root = rotateRight(root, xpr);
                        xpr = (xp = x.parent) == null ?
                            null : xp.right;
                    }
                    // 此时 xpr 的右子结点是红结点。

                    /*
                    【情况4】：通过变色和左旋转，将 x 路径上的黑结点数量加 1，
                    且此时其他结点的黑色结点数不变，则性质 5 被修复，也没有破坏其他性质，
                    红黑树平衡，可以返回。

                        xp                 xpr
                       /  \               /   \
                     (x)  (xpr)    =>   (xp)  (sr)
                              \         /
                              [sr]    (x)
                    */
                    if (xpr != null) {
                        xpr.red = (xp == null) ? false : xp.red;
                        if ((sr = xpr.right) != null)
                            sr.red = false;
                    }
                    if (xp != null) {
                        xp.red = false;
                        root = rotateLeft(root, xp);
                    }
                    // 此时树已平衡，可以返回了
                    x = root;
                }
            }
        }
        // 【镜像】否则 x 是它的父结点 xp 的右子结点
        else {
            if (xpl != null && xpl.red) {
                /*
                       |                        |
                      (xp)                    (xpl)
                     /    \      =>          /     \
                   [xpl]  (x)              (l)     [xp]
                  /     \                         /    \
                 (l)    (r)          新的 xpl -> (r)   (x)
                */
                xpl.red = false;
                xp.red = true;
                root = rotateRight(root, xp);
                xpl = (xp = x.parent) == null ? null : xp.left;
            }
            if (xpl == null)
                x = xp;
            else {
                TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                // 如果 xpl 的两个子结点都不是具有红色链接的结点
                if ((sl == null || !sl.red) &&
                    (sr == null || !sr.red)) {
                    /*
                           xp              xp
                         /   \           /   \
                      (xpl)  (x)  =>  [xpl]  (x)
                      /   \           /   \
                    (l)   (r)       (l)   (r)
                    */
                    xpl.red = true;
                    x = xp;
                }
                else {
                    if (sl == null || !sl.red) {
                        /*
                                xp                 xp
                              /   \              /   \
                           (xpl)   x           (sr)   x
                           /    \       =>     /  ^
                         (sl)   [sr]        [xpr]  \ 
                                             /      新的 xpl
                                           (sl)
                        */
                        if (sr != null)
                            sr.red = false;
                        xpl.red = true;
                        root = rotateLeft(root, xpl);
                        xpl = (xp = x.parent) == null ?
                            null : xp.left;
                    }

                    /*
                           xp              xpl
                         /   \            /   \
                       (xpl) (x)    =>  (sl)  (xp)
                      /                          \
                     [sl]                        (x)
                    */
                    if (xpl != null) {
                        xpl.red = (xp == null) ? false : xp.red;
                        if ((sl = xpl.left) != null)
                            sl.red = false;
                    }
                    if (xp != null) {
                        xp.red = false;
                        root = rotateRight(root, xp);
                    }
                    x = root;
                }
            }
        }
    }
}
```

#### 1.2.3.12 split
```java
/*
将存储桶中的结点拆分为 lo(不需要改变位置) 和 hi(需要改变位置) 两部分。
如果拆分出的部分小于阈值，则转为链表。

此方法仅被 HashMap.resize() 方法调用。

@param tab: 新的哈希表
@param index: 此结点在原来哈希表中的索引
@param bit: 用于拆分的哈希位。这是原来哈希表的容量，也是新的哈希表的掩码最高位
*/
final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
    TreeNode<K,V> b = this;
    // 将此存储桶中的结点重新链接到 lo 和 hi 列表中，保持原有顺序。
    TreeNode<K,V> loHead = null, loTail = null;
    TreeNode<K,V> hiHead = null, hiTail = null;
    int lc = 0, hc = 0;
    for (TreeNode<K,V> e = b, next; e != null; e = next) {
        next = (TreeNode<K,V>)e.next;
        e.next = null;
        // 如果结点哈希码与 bit 与运算等于 0，则其在新的哈希表 tab 中位置不需要变，
        // 将其放到 lo 链表中
        if ((e.hash & bit) == 0) {
            if ((e.prev = loTail) == null)
                loHead = e;
            else
                loTail.next = e;
            loTail = e;
            ++lc;
        }
        // 否则放到 hi 链表中
        else {
            if ((e.prev = hiTail) == null)
                hiHead = e;
            else
                hiTail.next = e;
            hiTail = e;
            ++hc;
        }
    }

    // 如果 lo 链表不为空
    if (loHead != null) {
        // 如果 lo 链表中结点数量小于 UNTREEIFY_THRESHOLD，将其转为链表
        if (lc <= UNTREEIFY_THRESHOLD)
            tab[index] = loHead.untreeify(map);
        else {
            // lo 中结点还是放到原来位置
            tab[index] = loHead;
            // 如果 hiHead 等于 null，那树结构就和原来一样，不需要变。
            // 否则有一些结点移到了 hi 链表里面，则需要对剩下的结点重新树化
            if (hiHead != null)
                loHead.treeify(tab);
        }
    }
    // 如果 lo 链表不为空
    if (hiHead != null) {
        // 如果 hi 链表中结点数量小于 UNTREEIFY_THRESHOLD，将其转为链表
        if (hc <= UNTREEIFY_THRESHOLD)
            // 新的位置就是原来的位置加上新的掩码最高位
            tab[index + bit] = hiHead.untreeify(map);
        else {
            tab[index + bit] = hiHead;
            if (loHead != null)
                hiHead.treeify(tab);
        }
    }
}
```

## 1.3 HashIterator
```java
// 基类 HashIterator，迭代结点。注意到它没有实现 Iterator 接口，
// 这是为了子类实现的灵活性
abstract class HashIterator {
    Node<K,V> next;        // 下一个返回的条目
    Node<K,V> current;     // 当前条目，也就是上次 next() 方法返回的条目
    int expectedModCount;  // fast-fail 变量
    int index;             // 当前存储桶下标

    HashIterator() {
        expectedModCount = modCount;
        Node<K,V>[] t = table;
        current = next = null;
        index = 0;
        if (t != null && size > 0) {
            // 找到第一个不为 null 的存储桶的第一个条目
            do {} while (index < t.length && (next = t[index++]) == null);
        }
    }

    public final boolean hasNext() {
        return next != null;
    }

    final Node<K,V> nextNode() {
        Node<K,V>[] t;
        Node<K,V> e = next;
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        if (e == null)
            throw new NoSuchElementException();
        // next 移动到下一个结点。如果 next 为 null，当前存储桶遍历完成，
        // 搜索下一个存储桶
        if ((next = (current = e).next) == null && (t = table) != null) {
            do {} while (index < t.length && (next = t[index++]) == null);
        }
        return e;
    }

    public final void remove() {
        Node<K,V> p = current;
        if (p == null)
            throw new IllegalStateException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        current = null;
        K key = p.key;
        // 使用 HashMap.removeNode 方法删除 current
        removeNode(hash(key), key, null, false, false);
        expectedModCount = modCount;
    }
}
```

## 1.4 KeyIterator
```java
// 键的迭代器
final class KeyIterator extends HashIterator implements Iterator<K> {
    public final K next() { return nextNode().key; }
}
```

## 1.5 ValueIterator
```java
// 值的迭代器
final class ValueIterator extends HashIterator implements Iterator<V> {
    public final V next() { return nextNode().value; }
}
```

## 1.6 EntryIterator
```java
// 条目的迭代器
final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K,V>> {
    public final Map.Entry<K,V> next() { return nextNode(); }
}
```

## 1.7 HashMapSpliterator
```java
// 基类 Spliterator，。注意到它没有实现 Spliterator 接口，这是为了子类实现的灵活性
static class HashMapSpliterator<K,V> {
    final HashMap<K,V> map;
    Node<K,V> current;          // 当前结点
    int index;                  // 当前存储桶下标, 会被 advance/split 方法修改
    int fence;                  // 存储桶下标最大范围
    int est;                    // 估计的 size，元素数量
    int expectedModCount;       // fast-fail 检测

    HashMapSpliterator(HashMap<K,V> m, int origin,
                       int fence, int est,
                       int expectedModCount) {
        this.map = m;
        this.index = origin;
        this.fence = fence;
        this.est = est;
        this.expectedModCount = expectedModCount;
    }

    // 第一次使用 fence、size 和 expectedModCount 时对它们进行初始化。返回 fence
    final int getFence() {
        int hi;
        if ((hi = fence) < 0) {
            HashMap<K,V> m = map;
            est = m.size;
            expectedModCount = m.modCount;
            Node<K,V>[] tab = m.table;
            hi = fence = (tab == null) ? 0 : tab.length;
        }
        return hi;
    }

    // 返回估计大小
    public final long estimateSize() {
        getFence(); // force init
        return (long) est;
    }
}
```
有关`Spliterator`参见 [Spliterator.md][spliterator]。此`Spliterator`实现是后期绑定的，
参见 [ArrayList.md][array-list] 第 4.3 节 ArrayListSpliterator。

## 1.8 KeySpliterator
```java
// 键的 Spliterator
static final class KeySpliterator<K,V>
    extends HashMapSpliterator<K,V>
    implements Spliterator<K> {

    KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                   int expectedModCount) {
        super(m, origin, fence, est, expectedModCount);
    }

    // 尝试切分
    public KeySpliterator<K,V> trySplit() {
        // 切分为两半，注意这个两半是将哈希表分半，只有当元素均匀分布时，
        // 元素数量才可能也分半
        int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
        // 如果太小不足以切分，返回 null；否则返回左半边的 Spliterator
        return (lo >= mid || current != null) ? null :
            new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                                    expectedModCount);
    }

    // 遍历剩余键
    public void forEachRemaining(Consumer<? super K> action) {
        int i, hi, mc;
        if (action == null)
            throw new NullPointerException();
        HashMap<K,V> m = map;
        Node<K,V>[] tab = m.table;
        // 未初始化则先进行初始化
        if ((hi = fence) < 0) {
            mc = expectedModCount = m.modCount;
            hi = fence = (tab == null) ? 0 : tab.length;
        }
        else
            mc = expectedModCount;
        if (tab != null && tab.length >= hi &&
            (i = index) >= 0 && (i < (index = hi) || current != null)) {
            Node<K,V> p = current;
            current = null;
            // 遍历完一个存储桶，再遍历下一个，直到完成
            do {
                if (p == null)
                    p = tab[i++];
                else {
                    action.accept(p.key);
                    p = p.next;
                }
            } while (p != null || i < hi);
            // 遍历完成后检查一次 modCount，查看是否有并发修改
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    // 尝试访问下一个键，类似于迭代器的 next
    public boolean tryAdvance(Consumer<? super K> action) {
        int hi;
        if (action == null)
            throw new NullPointerException();
        Node<K,V>[] tab = map.table;
        if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
            while (current != null || index < hi) {
                if (current == null)
                    current = tab[index++];
                else {
                    K k = current.key;
                    current = current.next;
                    action.accept(k);
                    if (map.modCount != expectedModCount)
                        throw new ConcurrentModificationException();
                    return true;
                }
            }
        }
        return false;
    }

    // 返回特征值
    public int characteristics() {
        // 如果大小是精确的（第一个 Spliterator），则有特征值 SIZED。
        // 特征值 DISTINCT 始终会有
        return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
            Spliterator.DISTINCT;
    }
}
```

## 1.9 ValueSpliterator
```java
// 值的 Spliterator，代码类似于 KeySpliterator
static final class ValueSpliterator<K,V>
    extends HashMapSpliterator<K,V>
    implements Spliterator<V> {
  
    ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                     int expectedModCount) {
        super(m, origin, fence, est, expectedModCount);
    }

    public ValueSpliterator<K,V> trySplit() {
        int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
        return (lo >= mid || current != null) ? null :
            new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                                      expectedModCount);
    }

    public void forEachRemaining(Consumer<? super V> action) {
        int i, hi, mc;
        if (action == null)
            throw new NullPointerException();
        HashMap<K,V> m = map;
        Node<K,V>[] tab = m.table;
        if ((hi = fence) < 0) {
            mc = expectedModCount = m.modCount;
            hi = fence = (tab == null) ? 0 : tab.length;
        }
        else
            mc = expectedModCount;
        if (tab != null && tab.length >= hi &&
            (i = index) >= 0 && (i < (index = hi) || current != null)) {
            Node<K,V> p = current;
            current = null;
            do {
                if (p == null)
                    p = tab[i++];
                else {
                    action.accept(p.value);
                    p = p.next;
                }
            } while (p != null || i < hi);
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    public boolean tryAdvance(Consumer<? super V> action) {
        int hi;
        if (action == null)
            throw new NullPointerException();
        Node<K,V>[] tab = map.table;
        if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
            while (current != null || index < hi) {
                if (current == null)
                    current = tab[index++];
                else {
                    V v = current.value;
                    current = current.next;
                    action.accept(v);
                    if (map.modCount != expectedModCount)
                        throw new ConcurrentModificationException();
                    return true;
                }
            }
        }
        return false;
    }

    public int characteristics() {
        return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
    }
}
```

## 1.10 EntrySpliterator
```java
// 条目的迭代器，代码类似于 KeySpliterator
static final class EntrySpliterator<K,V>
    extends HashMapSpliterator<K,V>
    implements Spliterator<Map.Entry<K,V>> {
    
    EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                     int expectedModCount) {
        super(m, origin, fence, est, expectedModCount);
    }

    public EntrySpliterator<K,V> trySplit() {
        int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
        return (lo >= mid || current != null) ? null :
            new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                                      expectedModCount);
    }

    public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
        int i, hi, mc;
        if (action == null)
            throw new NullPointerException();
        HashMap<K,V> m = map;
        Node<K,V>[] tab = m.table;
        if ((hi = fence) < 0) {
            mc = expectedModCount = m.modCount;
            hi = fence = (tab == null) ? 0 : tab.length;
        }
        else
            mc = expectedModCount;
        if (tab != null && tab.length >= hi &&
            (i = index) >= 0 && (i < (index = hi) || current != null)) {
            Node<K,V> p = current;
            current = null;
            do {
                if (p == null)
                    p = tab[i++];
                else {
                    action.accept(p);
                    p = p.next;
                }
            } while (p != null || i < hi);
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
        int hi;
        if (action == null)
            throw new NullPointerException();
        Node<K,V>[] tab = map.table;
        if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
            while (current != null || index < hi) {
                if (current == null)
                    current = tab[index++];
                else {
                    Node<K,V> e = current;
                    current = current.next;
                    action.accept(e);
                    if (map.modCount != expectedModCount)
                        throw new ConcurrentModificationException();
                    return true;
                }
            }
        }
        return false;
    }

    public int characteristics() {
        return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
            Spliterator.DISTINCT;
    }
}
```

## 1.11 KeySet
```java
// keySet() 方法返回此 Set
final class KeySet extends AbstractSet<K> {
    public final int size()                 { return size; }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<K> iterator()     { return new KeyIterator(); }
    public final boolean contains(Object o) { return containsKey(o); }
    public final boolean remove(Object key) {
        return removeNode(hash(key), key, null, false, true) != null;
    }
    public final Spliterator<K> spliterator() {
        return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
    }
    public final void forEach(Consumer<? super K> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }
}
```
参见 [AbstractSet.md][abstract-set]。

## 1.12 values
```java
// values() 方法返回此 Collection
final class Values extends AbstractCollection<V> {
    public final int size()                 { return size; }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<V> iterator()     { return new ValueIterator(); }
    public final boolean contains(Object o) { return containsValue(o); }
    public final Spliterator<V> spliterator() {
        return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
    }
    public final void forEach(Consumer<? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }
}
```
参见 [AbstractCollection.md][abstract-collection]。

## 1.13 entrySet
```java
final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public final int size()                 { return size; }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator();
    }
    public final boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>) o;
        Object key = e.getKey();
        Node<K,V> candidate = getNode(hash(key), key);
        return candidate != null && candidate.equals(e);
    }
    public final boolean remove(Object o) {
        if (o instanceof Map.Entry) {
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Object value = e.getValue();
            return removeNode(hash(key), key, value, true, true) != null;
        }
        return false;
    }
    public final Spliterator<Map.Entry<K,V>> spliterator() {
        return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
    }
    public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }
}
```

# 2. 成员字段

## 2.1 常量
```java
// 默认的初始容量，必须是 2 的幂
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;  // 16

// 最大容量值。如果构造函数指定的容量大于它，则使用该容量。它也必须是 2 的幂。
// 这样 HashMap 允许的最大容量就只有 ArrayList 的 1/2，大约是 10 亿多。
// 但元素数量最多可以有 Integer.MAX_VALUE
static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认的加载因子。HashMap 元素数超过(容量 * 加载因子)时，会扩容并重哈希 
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 但存储桶元素数大于此值时，存储桶将由链表变为红黑树
static final int TREEIFY_THRESHOLD = 8;

// 但存储桶元素数小于此值时，存储桶将由红黑树变为链表
static final int UNTREEIFY_THRESHOLD = 6;

// 可以对存储桶树化的最小容量。应该至少是 4 * TREEIFY_THRESHOLD，
// 以避免 resize 和树化阈值之间的冲突。
static final int MIN_TREEIFY_CAPACITY = 64;
```

## 2.2 成员变量
```java
/*
哈希表。该表在首次使用时才进行创建，并根据需要调整大小。分配大小时，长度始终是 2 的幂。

设置为 transient 是为了在序列化时不要将所有结点序列化，只序列化实际存在的元素
*/
transient Node<K,V>[] table;

// 保存 entrySet() 方法的返回值。在 AbstractMap 中，同样有字段 keySet 和 values 
// 保存 keySet() 和 values() 的返回值
transient Set<Map.Entry<K,V>> entrySet;

// 键值对数量
transient int size;

/*
对该 HashMap 进行结构修改的次数。结构修改是指更改 HashMap 中的条目的数量或以其他方式修改其内部结构
（例如，重新哈希）的修改。此字段用于 HashMap 迭代器检测并发修改。
（请参见 ConcurrentModificationException）。
*/
transient int modCount;

// 哈希表的加载因子
final float loadFactor;

// 等于 loadFactor * capacity，元素数量超过 threshold 将发生重哈希。
// 如果哈希表数组还没有分配，这个字段保存初始容量（构造器传入）。
// 如果 threshold 等于 0，则表示此 HashMap 具有 DEFAULT_INITIAL_CAPACITY 容量。
int threshold;
```

# 3. 构造器
```java
// 构造一个具有指定初始容量和负载因子的空 HashMap。
// 此操作不会立即创建内部哈希表，在添加元素的时候才会创建
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    // 保证 initialCapacity 不大于 MAXIMUM_CAPACITY
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    // threshold 保存初始容量
    this.threshold = tableSizeFor(initialCapacity);
}

// 构造一个具有指定初始容量和默认负载因子的空 HashMap。
// 此操作不会立即创建内部哈希表，在添加元素的时候才会创建
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}

// 使用默认的初始容量（16）和默认的加载因子（0.75）构造一个空的 HashMap。
// 此操作不会立即创建内部哈希表，在添加元素的时候才会创建
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
}

// 构造一个具有与指定 Map 相同的映射关系的新 HashMap。使用默认的负载因子（0.75）
// 和足以包含所有元素的初始容量创建 HashMap。
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}
```

# 4. 方法

## 4.1 tableSizeFor
```java
// 返回大于等于 cap 的最小 2 次幂
static final int tableSizeFor(int cap) {
    // cap 可能等于 n 的 2 次幂.为了保证接下来的操作正确，需要减 1
    int n = cap - 1;
    n |= n >>> 1;  // n 的最高 2 位设置为 1
    n |= n >>> 2;  // n 的最高 4 位设置为 1
    n |= n >>> 4;  // n 的最高 8 位设置为 1
    n |= n >>> 8;  // n 的最高 16 位设置为 1
    n |= n >>> 16;  // n 的最高 32 位设置为 1
    // 移位和或运算的结果是，n 的最高位往后都变成了 1，再加 1 就是大于等于 n 的 2 次幂

    // n 小于 0 就返回 1；n 大于等于容量最大值就返回 MAXIMUM_CAPACITY；
    // 否则返回 n + 1，也就是大于等于 n 的 2 次幂
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

## 4.2 hash
```java
/*
计算 key.hashCode()，并将较高位的哈希值扩散（XOR）到较低位。

由于 HashMap 使用了二幂掩码，这样大多数情况下只会利用到哈希码低位的值，所以只使用当前掩码的哈希表总是会发生碰撞。
因此我们应用一个变换，将高位向下扩散。

在速度、实用性和位扩散的质量之间有一个权衡。因为很多常见的哈希码已经是合理分布的（所以并没有从扩散中获益），
而且因为我们使用红黑树来处理大哈希表的哈希碰撞，所以我们只是用最简单的方式：将高位和低位进行异或，以减少性能损耗，
以及合并最高位的影响。
*/
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

## 4.3 compare
```java
// 如果 x 的声明形式为“Class C implements Comparable<C>”，则返回 x 的 Class，否则返回 null
static Class<?> comparableClassFor(Object x) {
    if (x instanceof Comparable) {
        Class<?> c; 
        Type[] ts, as;
        Type t;
        ParameterizedType p;
        // 很多时候都是用 String 做键，因此这里做一个快速检查
        if ((c = x.getClass()) == String.class)
            return c;
        // 确定实现接口中是否有 Comparable<C>
        if ((ts = c.getGenericInterfaces()) != null) {
            for (int i = 0; i < ts.length; ++i) {
                if (((t = ts[i]) instanceof ParameterizedType) &&
                    ((p = (ParameterizedType)t).getRawType() ==
                     Comparable.class) &&
                    (as = p.getActualTypeArguments()) != null &&
                    as.length == 1 && as[0] == c) // type arg is c
                    return c;
            }
        }
    }
    return null;
}

// 如果 x 的 Class 等于 kc，则返回 k.compareTo(x)，否则返回 0。
@SuppressWarnings({"rawtypes","unchecked"})
static int compareComparables(Class<?> kc, Object k, Object x) {
    return (x == null || x.getClass() != kc ? 0 :
            ((Comparable)k).compareTo(x));
}
```

## 4.4 resize
```java
// 扩容为原来两倍大小，然后重新安排所有元素的位置
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    // 如果 oldCap 大于 0
    if (oldCap > 0) {
        // 如果容量已达最大上限
        if (oldCap >= MAXIMUM_CAPACITY) {
            // 允许的元素数量设为 Integer.MAX_VALUE
            threshold = Integer.MAX_VALUE;
            // 原哈希表保持不变
            return oldTab;
        }
        // 如果倍增后容量小于 MAXIMUM_CAPACITY 
        // 并且原来的容量大于等于 DEFAULT_INITIAL_CAPACITY
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            // 倍增 threshold
            newThr = oldThr << 1;
    }
    // oldCap 等于 0，表示哈希表还未创建。
    // 如果 threshold 大于 0，此时它表示初始容量（构造器传入）
    else if (oldThr > 0)
        newCap = oldThr;
    // 如果 threshold 等于 0，表示使用默认构造器创建此 HashMap，
    // 此时容量为 DEFAULT_INITIAL_CAPACITY
    else {
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    // 设置新的 threshold
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        // 容量或新的 threshold 超过 MAXIMUM_CAPACITY，则直接设为 Integer.MAX_VALUE
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    // 创建新的哈希表
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
        // 遍历原来哈希表所有存储桶
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                // 如果存储桶中只有一个结点
                if (e.next == null)
                    // 计算它在新的哈希表中的位置并放入
                    newTab[e.hash & (newCap - 1)] = e;
                // 如果是红黑树结构
                else if (e instanceof TreeNode)
                    // 调用 TreeNode.split 将红黑树写入新的哈希表
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                // 否则是链表结构
                else {
                    // 使用和 TreeNode.split 一样的方法进行拆分
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        // 如果结点哈希码与 bit 与运算等于 0，则其在新的哈希表 tab 中位置不需要变，
                        // 将其放到 lo 链表中
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        // hi 链表新的位置就是原来的位置加上新的掩码最高位
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

## 4.5 创建结点
```java
// 新建一个链表结点
Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
    return new Node<>(hash, key, value, next);
}

// 新建一个红黑树结点
TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
    return new TreeNode<>(hash, key, value, next);
}
```

## 4.6 替换结点
```java
// 将 p 替换成新的链表结点。用于替换 TreeNode
Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
    return new Node<>(p.hash, p.key, p.value, next);
}

// 将 p 替换成新的红黑树结点。用于替换链表结点
TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
    return new TreeNode<>(p.hash, p.key, p.value, next);
}
```

## 4.7 LinkedHashMap 回调
```java
// 以下方法是用在 LinkedHashMap 的后置回调
void afterNodeAccess(Node<K,V> p) { }
void afterNodeInsertion(boolean evict) { }
void afterNodeRemoval(Node<K,V> p) { }
```

## 4.8 treeifyBin
```java
// 将给定哈希索引下存储桶中的所有 Node 结点替换为 TreeNode 结点，并转换为红黑树。
// 如果哈希表未创建或大小小于 MIN_TREEIFY_CAPACITY，则只会进行创建或扩容操作
final void treeifyBin(Node<K,V>[] tab, int hash) {
    int n, index; Node<K,V> e;
    // 如果哈希表未创建，或者哈希表大小小于 MIN_TREEIFY_CAPACITY，则进行创建或扩容操作
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
        resize();
    // 否则将指定位置处的存储桶变成红黑树
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        TreeNode<K,V> hd = null, tl = null;
        // 将所有结点替换为 TreeNode
        do {
            TreeNode<K,V> p = replacementTreeNode(e, null);
            if (tl == null)
                hd = p;
            else {
                p.prev = tl;
                tl.next = p;
            }
            tl = p;
        } while ((e = e.next) != null);
        // 然后转换为红黑树
        if ((tab[index] = hd) != null)
            hd.treeify(tab);
    }
}
```

## 4.9 putVal
```java
/*
写入键值对，实现 Map.put 操作。

@param onlyIfAbsent: 如果为 true，则不改变已经存在的键值对
@param evict: 为 false，表示在 HashMap 构造时调用了此方法。此参数提供给 afterNodeInsertion 方法
*/
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // 如果哈希表还未创建，则先创建它
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 如果此结点是对应位置存储桶的第一个结点，则直接创建结点并放入对应的存储桶
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // 如果存储桶中第一个结点的哈希码和键都与给定值相等
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        // 否则如果存储桶是红黑树
        else if (p instanceof TreeNode)
            // 调用 TreeNode.putTreeVal 方法
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        // 否则在链表中查找
        else {
            for (int binCount = 0; ; ++binCount) {
                // 如果已经遍历到最后一个结点，则插入新的结点
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    // 如果链表结点数量大于 TREEIFY_THRESHOLD，则调用 treeifyBin 方法。
                    // 注意，由于没有算上链表中第一个结点，所以要减 1
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                // 如果找到了哈希码和键都与给定值相等的结点，跳出循环
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        // 如果键已存在
        if (e != null) {
            V oldValue = e.value;
            // 如果允许替换或原来的值为 null，则替换新的值
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            // 调用 LinkedHashMap 回调方法 afterNodeAccess
            afterNodeAccess(e);
            // 返回旧值
            return oldValue;
        }
    }
    ++modCount;
    // 如果元素数量超过 threshold，则需要扩容
    if (++size > threshold)
        resize();
    // 调用 LinkedHashMap 回调方法 afterNodeInsertion
    afterNodeInsertion(evict);
    return null;
}
```

## 4.10 putMapEntries
```java
/*
用在 putAll 方法或构造函数中。

@param evict：为 false，表示在 HashMap 构造时调用了此方法。此参数提供给 afterNodeInsertion 方法
*/
final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
    int s = m.size();
    if (s > 0) {
        // 如果哈希表还未创建
        if (table == null) {
            float ft = ((float)s / loadFactor) + 1.0F;
            // 保证新的容量不超过 MAXIMUM_CAPACITY
            int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                     (int)ft : MAXIMUM_CAPACITY);
            // 哈希表未创建，threshold 此时表示初始容量（构造器传入）
            // 如果计算的容量大于初始容量，则计算新的初始容量
            if (t > threshold)
                threshold = tableSizeFor(t);
        }
        // 否则如果添加的元素数量大于 threshold，则进行扩容操作
        else if (s > threshold)
            resize();
        // 将元素添加到当前 HashMap 中
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            putVal(hash(key), key, value, false, evict);
        }
    }
}
```

## 4.11 getNode
```java
// 获取具有指定哈希码和键的结点
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    // 如果哈希表不为空且对应存储桶不为 null
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        // 检查第一个结点的哈希码和键是否给定值相等
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // 不相等则需要继续查找
        if ((e = first.next) != null) {
            // 如果存储桶是红黑树，则使用 TreeNode.getTreeNode 方法查找
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 否则在链表中进行查找
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

## 4.12 removeNode
```java
/*
删除具有指定哈希码和键的结点

@param matchValue: 为 true，则只有在值等于 value 的情况下才删除
@param movable: 为 false，则删除结点后不移动其他结点
*/
final Node<K,V> removeNode(int hash, Object key, Object value,
                           boolean matchValue, boolean movable) {
    Node<K,V>[] tab; Node<K,V> p; int n, index;
    // 如果哈希表不为空且对应存储桶不为 null
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (p = tab[index = (n - 1) & hash]) != null) {
        Node<K,V> node = null, e; K k; V v;
        // 检查第一个结点的哈希码和键是否与给定值相等
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            node = p;
        // 否则如果还有其他结点
        else if ((e = p.next) != null) {
            // 如果是红黑树结点
            if (p instanceof TreeNode)
                // 使用 TreeNode.getTreeNode 方法进行查找
                node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
            // 否则在链表中进行查找
            else {
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key ||
                         (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
        }
        // 如果找到了结点并且可以删除
        if (node != null && (!matchValue || (v = node.value) == value ||
                             (value != null && value.equals(v)))) {
            // 如果待删除结点是红黑树结点
            if (node instanceof TreeNode)
                // 使用 TreeNode.removeTreeNode 方法
                ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
            // 否则如果只有一个结点
            else if (node == p)
                tab[index] = node.next;
            else
                p.next = node.next;
            ++modCount;
            --size;
            // 调用 LinkedHashMap 的回调方法 afterNodeRemoval
            afterNodeRemoval(node);
            // 返回被删除结点
            return node;
        }
    }
    // 没有找到结点或值不等于 value，返回 null
    return null;
}
```

## 4.13 元素数量查询
```java
public int size() {
    return size;
}

public boolean isEmpty() {
    return size == 0;
}
```

## 4.14 contains
```java
public boolean containsKey(Object key) {
    return getNode(hash(key), key) != null;
}

public boolean containsValue(Object value) {
    Node<K,V>[] tab; V v;
    if ((tab = table) != null && size > 0) {
        // 遍历所有存储桶直到找到匹配
        for (int i = 0; i < tab.length; ++i) {
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                if ((v = e.value) == value ||
                    (value != null && value.equals(v)))
                    return true;
            }
        }
    }
    return false;
}
```

## 4.15 get
```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

@Override
public V getOrDefault(Object key, V defaultValue) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
}
```

## 4.16 put
```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

@Override
public V putIfAbsent(K key, V value) {
    return putVal(hash(key), key, value, true, true);
}

public void putAll(Map<? extends K, ? extends V> m) {
    putMapEntries(m, true);
}
```

## 4.17 compute
```java
@Override
public V computeIfAbsent(K key,
                         Function<? super K, ? extends V> mappingFunction) {
    if (mappingFunction == null)
        throw new NullPointerException();
    int hash = hash(key);
    Node<K,V>[] tab;
    Node<K,V> first;
    int n, i;
    int binCount = 0;
    TreeNode<K,V> t = null;
    Node<K,V> old = null;
    // 如果哈希表未创建或元素数量超过了 threshold，则进行扩容
    if (size > threshold || (tab = table) == null ||
        (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 如果与 key 对应的存储桶不为空
    if ((first = tab[i = (n - 1) & hash]) != null) {
        // 如果存储桶是红黑树
        if (first instanceof TreeNode)
            // 使用 TreeNode.getTreeNode 进行查找
            old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
        // 否则在链表中查找
        else {
            Node<K,V> e = first; K k;
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
        }
        V oldValue;
        // 如果结点找到了并且值不为 null
        if (old != null && (oldValue = old.value) != null) {
            // 调用 LinkedHashMap 的回调方法 afterNodeAccess
            afterNodeAccess(old);
            // 不做操作返回结点值
            return oldValue;
        }
    }
    // 结点不存在或值为 null。
    // 使用 mappingFunction 计算值
    V v = mappingFunction.apply(key);
    // 计算得到了结果为 null，不做任何操作
    if (v == null) {
        return null;
    }
    // 否则如果结点存在（它的值为 null）
    else if (old != null) {
        // 设置值
        old.value = v;
        // 调用 LinkedHashMap 的回调方法 afterNodeAccess
        afterNodeAccess(old);
        return v;
    }
    // 否则结点不存在，且存储桶为红黑树
    else if (t != null)
        // 使用 TreeNode.putTreeVal 插入键值对
        t.putTreeVal(this, tab, hash, key, v);
    // 否则插入链表
    else {
        tab[i] = newNode(hash, key, v, first);
        // 如果链表结点数量大于 TREEIFY_THRESHOLD，则调用 treeifyBin 方法。
        // 注意，由于没有算上链表中第一个结点，所以要减 1
        if (binCount >= TREEIFY_THRESHOLD - 1)
            treeifyBin(tab, hash);
    }
    ++modCount;
    ++size;
    // 调用 LinkedHashMap 的回调方法 afterNodeInsertion
    afterNodeInsertion(true);
    return v;
}

public V computeIfPresent(K key,
                          BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    if (remappingFunction == null)
        throw new NullPointerException();
    Node<K,V> e; V oldValue;
    int hash = hash(key);
    // 结点存在且值不为 null
    if ((e = getNode(hash, key)) != null &&
        (oldValue = e.value) != null) {
        // 使用 key 和 oldValue 计算新值
        V v = remappingFunction.apply(key, oldValue);
        // 新值不为 null，则替换旧值
        if (v != null) {
            e.value = v;
            // 调用 LinkedHashMap 的回调方法 afterNodeAccess
            afterNodeAccess(e);
            return v;
        }
        else
            // 否则删除这个结点
            removeNode(hash, key, null, false, true);
    }
    return null;
}

@Override
public V compute(K key,
                 BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    if (remappingFunction == null)
        throw new NullPointerException();
    int hash = hash(key);
    Node<K,V>[] tab; Node<K,V> first; int n, i;
    int binCount = 0;
    TreeNode<K,V> t = null;
    Node<K,V> old = null;
    // 如果哈希表未创建或元素数量超过了 threshold，则进行扩容
    if (size > threshold || (tab = table) == null ||
        (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 如果与 key 对应的存储桶不为空
    if ((first = tab[i = (n - 1) & hash]) != null) {
        // 如果存储桶是红黑树
        if (first instanceof TreeNode)
            // 使用 TreeNode.getTreeNode 进行查找
            old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
        // 否则在链表中查找
        else {
            Node<K,V> e = first; K k;
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
        }
    }
    // 原来的结点存在则值为 old.value，否则为 null
    V oldValue = (old == null) ? null : old.value;
    // 使用 key 和 oldValue 计算新值
    V v = remappingFunction.apply(key, oldValue);
    // 如果原来的结点存在
    if (old != null) {
        // 新值不为 null，则替换旧值
        if (v != null) {
            old.value = v;
            // 调用 LinkedHashMap 的回调方法 afterNodeAccess
            afterNodeAccess(old);
        }
        // 否则删除原来的结点
        else
            removeNode(hash, key, null, false, true);
    }
    // 否则原来的结点不存在，且新值不为 null
    else if (v != null) {
        // 如果存储桶为红黑树
        if (t != null)
            // 使用 TreeNode.putTreeVal 插入键值对
            t.putTreeVal(this, tab, hash, key, v);
        // 否则插入链表
        else {
            tab[i] = newNode(hash, key, v, first);
            // 如果链表结点数量大于 TREEIFY_THRESHOLD，则调用 treeifyBin 方法。
            // 注意，由于没有算上链表中第一个结点，所以要减 1
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        // 调用 LinkedHashMap 的回调方法 afterNodeInsertion
        afterNodeInsertion(true);
    }
    return v;
}
```

## 4.18 merge
```java
@Override
public V merge(K key, V value,
               BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    if (value == null)
        throw new NullPointerException();
    if (remappingFunction == null)
        throw new NullPointerException();
    int hash = hash(key);
    Node<K,V>[] tab; Node<K,V> first; int n, i;
    int binCount = 0;
    TreeNode<K,V> t = null;
    Node<K,V> old = null;
    // 如果哈希表未创建或元素数量超过了 threshold，则进行扩容
    if (size > threshold || (tab = table) == null ||
        (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 如果与 key 对应的存储桶不为空
    if ((first = tab[i = (n - 1) & hash]) != null) {
        // 如果存储桶是红黑树
        if (first instanceof TreeNode)
            // 使用 TreeNode.getTreeNode 进行查找
            old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
        // 否则在链表中查找
        else {
            Node<K,V> e = first; K k;
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
        }
    }
    // 如果原来的结点存在
    if (old != null) {
        V v;
        // 如果旧值存在，则使用 old.value 和 value 计算新的值
        if (old.value != null)
            v = remappingFunction.apply(old.value, value);
        // 否则直接使用 value
        else
            v = value;
        // 新值不为 null，则替换旧值
        if (v != null) {
            old.value = v;
            // 调用 LinkedHashMap 的回调方法 afterNodeAccess
            afterNodeAccess(old);
        }
        // 否则删除原来的结点
        else
            removeNode(hash, key, null, false, true);
        // 返回新的值
        return v;
    }
    // 如果原来的结点不存在，且 value 不为 null
    if (value != null) {
        // 如果存储桶为红黑树
        if (t != null)
            // 使用 TreeNode.putTreeVal 插入键值对
            t.putTreeVal(this, tab, hash, key, value);
        // 否则插入链表
        else {
            tab[i] = newNode(hash, key, value, first);
            // 如果链表结点数量大于 TREEIFY_THRESHOLD，则调用 treeifyBin 方法。
            // 注意，由于没有算上链表中第一个结点，所以要减 1
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        // 调用 LinkedHashMap 的回调方法 afterNodeInsertion
        afterNodeInsertion(true);
    }
    return value;
}
```

## 4.19 replace
```java
@Override
public boolean replace(K key, V oldValue, V newValue) {
    Node<K,V> e; V v;
    // 如果查找结点存在且其值等于 oldValue
    if ((e = getNode(hash(key), key)) != null &&
        ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
        // 替换为新值
        e.value = newValue;
        // 调用 LinkedHashMap 的回调方法 afterNodeAccess
        afterNodeAccess(e);
        return true;
    }
    return false;
}

@Override
public V replace(K key, V value) {
    Node<K,V> e;
    // 查找结点存在
    if ((e = getNode(hash(key), key)) != null) {
        // 替换值
        V oldValue = e.value;
        e.value = value;
        // 调用 LinkedHashMap 的回调方法 afterNodeAccess
        afterNodeAccess(e);
        return oldValue;
    }
    return null;
}

@Override
public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Node<K,V>[] tab;
    if (function == null)
        throw new NullPointerException();
    if (size > 0 && (tab = table) != null) {
        int mc = modCount;
        // 遍历所有键值对，进行替换
        for (int i = 0; i < tab.length; ++i) {
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                e.value = function.apply(e.key, e.value);
            }
        }
        // 迭代完成后，进行并发修改检测
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
}
```

## 4.20 remove
```java
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false, true)) == null ?
        null : e.value;
}

@Override
public boolean remove(Object key, Object value) {
    return removeNode(hash(key), key, value, true, true) != null;
}
```

## 4.21 forEach
```java
@Override
public void forEach(BiConsumer<? super K, ? super V> action) {
    Node<K,V>[] tab;
    if (action == null)
        throw new NullPointerException();
    if (size > 0 && (tab = table) != null) {
        int mc = modCount;
        for (int i = 0; i < tab.length; ++i) {
            for (Node<K,V> e = tab[i]; e != null; e = e.next)
                action.accept(e.key, e.value);
        }
        // 迭代完成后，进行并发修改检测
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
}
```

## 4.22 视图方法
```java
public Set<K> keySet() {
    Set<K> ks = keySet;
    // 当缓存为 null 时创建视图，否则直接返回缓存
    if (ks == null) {
        ks = new KeySet();
        keySet = ks;
    }
    return ks;
}

public Collection<V> values() {
    Collection<V> vs = values;
    if (vs == null) {
        vs = new Values();
        values = vs;
    }
    return vs;
}

public Set<Map.Entry<K,V>> entrySet() {
    Set<Map.Entry<K,V>> es;
    return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
}
```

## 4.23 clear
```java
public void clear() {
    Node<K,V>[] tab;
    modCount++;
    if ((tab = table) != null && size > 0) {
        size = 0;
        // 将所有存储桶置为 null
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
    }
}
```

## 4.24 reinitialize
```java
// 将当前 HashMap 重置为初始默认状态。被 clone 和 readObject 方法调用。
void reinitialize() {
    table = null;
    entrySet = null;
    keySet = null;
    values = null;
    modCount = 0;
    threshold = 0;
    size = 0;
}
```

## 4.25 clone
```java
@SuppressWarnings("unchecked")
@Override
public Object clone() {
    HashMap<K,V> result;
    try {
        result = (HashMap<K,V>)super.clone();
    } catch (CloneNotSupportedException e) {
        // this shouldn't happen, since we are Cloneable
        throw new InternalError(e);
    }
    result.reinitialize();
    result.putMapEntries(this, false);
    return result;
}
```

## 4.26 序列化
```java
private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    int buckets = capacity();
    // Write out the threshold, loadfactor, and any hidden stuff
    s.defaultWriteObject();
    s.writeInt(buckets);
    s.writeInt(size);
    internalWriteEntries(s);
}

private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {
    // Read in the threshold (ignored), loadfactor, and any hidden stuff
    s.defaultReadObject();
    reinitialize();
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new InvalidObjectException("Illegal load factor: " +
                                         loadFactor);
    // 这里是为了兼容低版本的 Java 代码
    s.readInt();                // Read and ignore number of buckets
    int mappings = s.readInt(); // Read number of mappings (size)
    if (mappings < 0)
        throw new InvalidObjectException("Illegal mappings count: " +
                                         mappings);
    else if (mappings > 0) { // (if zero, use defaults)
        // 只有当加载因子在 [0.25, 4.0] 的范围内，才使用它确定哈希表的大小
        float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
        float fc = (float)mappings / lf + 1.0f;
        // 计算容量，保证其在 DEFAULT_INITIAL_CAPACITY 和 MAXIMUM_CAPACITY 直接，
        // 并且为 2 的幂
        int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                   DEFAULT_INITIAL_CAPACITY :
                   (fc >= MAXIMUM_CAPACITY) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor((int)fc));
        float ft = (float)cap * lf;
        // 计算 threshold
        threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                     (int)ft : Integer.MAX_VALUE);

        // 检查 Map.Entry[].class，因为它是最接近我们实际创建的公共类型。
        SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, cap);
        @SuppressWarnings({"rawtypes","unchecked"})
        // 创建哈希表
        Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
        table = tab;

        // 读取键值对，并使用 putVal 方法写入
        for (int i = 0; i < mappings; i++) {
            @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
            @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
            // 序列化也算是创建了新的 HashMap
            putVal(hash(key), key, value, false, false);
        }
    }
}

// 仅被 writeObject 调用，保证兼容的顺序
void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
    Node<K,V>[] tab;
    if (size > 0 && (tab = table) != null) {
        // 遍历存储桶，键元素键和值写入，当不写入链接
        for (int i = 0; i < tab.length; ++i) {
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                s.writeObject(e.key);
                s.writeObject(e.value);
            }
        }
    }
}

final float loadFactor() { return loadFactor; }

final int capacity() {
    return (table != null) ? table.length :
        (threshold > 0) ? threshold :
        DEFAULT_INITIAL_CAPACITY;
}
```


[map]: Map.md
[abstract-map]: AbstractMap.md
[spliterator]: Spliterator.md
[array-list]: ArrayList.md
[abstract-set]: AbstractSet.md
[abstract-collection]: AbstractCollection.md