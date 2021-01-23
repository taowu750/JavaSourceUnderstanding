`java.util.Map`接口的声明如下：
```java
public interface Map<K,V>
```
将键映射到值的字典。映射不能包含重复的键；每个键最多可以映射到一个值。该接口代替了`Dictionary`类（已废弃），
后者是一个抽象类，而不是一个接口。

`Map`接口提供了三个集合视图，这些视图允许将地图的`Map`视为一组键的`Set`、一组值的集合或一组键值对映射。
`Map`的顺序定义为`Map`的集合视图上的迭代器返回其元素的顺序。
一些`Map`实现（例如`TreeMap`类）对其顺序做出特定的保证。其他的（例如`HashMap`类）则没有。

注意：如果将可变对象用作`Map`键，则必须格外小心。
如果在对象是`Map`中的键的情况下以影响`equals`方法比较的方式更改对象的值，则不会`Map`的行为未定义。
此外，不允许`Map`包含自身作为键。

所有通用`Map`实现类均应提供两个“标准”构造函数：一个无参数构造函数，用于创建一个空的`Map`；
以及一个具有单个`Map`类型参数的构造函数，其用于创建具有相同键值的新`Map`。后一个构造函数允许用户复制任何`Map`。

一些`Map`实现对它们可能包含的键和值有限制。例如，某些实现禁止空键和空值，而某些实现对其键的类型有限制。
尝试插入不合格的键或值会引发异常，通常为`NullPointerException`或`ClassCastException`。
尝试查询不合格的键或值的存在可能会引发异常，或者可能仅返回`false`。
一些实现将表现出前一种行为，而某些将表现出后者。此类异常在此接口的规范中标记为“可选”。

`Collections Framework`接口中的许多方法都是根据`equals`方法定义的。例如，`containsKey(Object key)`方法。

`Map`接口中以下代码值得注意：
 - 1. Entry：`comparingByKey`中的多重转型。

# 1. 内部接口
```java
// Map 条目（键值对）。Map.entrySet 方法返回 Map 的集合视图，包含 Map 的所有元素。
interface Entry<K,V> {
    
    // 返回与此条目对应的键。
    K getKey();

    // 返回此条目对应的值
    V getValue();

    /*
    用指定的值替换与此条目对应的值（可选操作）。如果已经从 Map 中删除了此条目（通过迭代器的 remove 操作），
    则此调用的行为是不确定的。
     */
    V setValue(V value);

    /*
    比较指定对象与此条目的相等性。如果给定对象也是一个 Map 条目，并且两个 Map 表示相同的条目，则返回 true。
    更正式地说，如果
            (e1.getKey()==null ?
             e2.getKey()==null : e1.getKey().equals(e2.getKey())) &&
            (e1.getValue()==null ?
             e2.getValue()==null : e1.getValue().equals(e2.getValue()))
    则两个条目 e1 和 e2 相同
     */
    boolean equals(Object o);

    /*
    返回此 Map 条目的哈希码值。Map 条目 e 的哈希码定义为：
            (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
            (e.getValue()==null ? 0 : e.getValue().hashCode())
    这样可以确保满足 Object.hashCode 的约定。
     */
    int hashCode();

    /*
    返回一个比较器，该比较器以键的自然顺序比较 Map.Entry。
    返回的比较器是可序列化的，并且在将条目与空键进行比较时抛出 NullPointerException。
     */
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K,V>> comparingByKey() {
        // 使用 “&” 语法可以进行多重转型
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> c1.getKey().compareTo(c2.getKey());
    }

    /*
    返回一个比较器，该比较器以值的自然顺序比较 Map.Entry。
    返回的比较器是可序列化的，并且在将条目与空键进行比较时抛出 NullPointerException。
     */
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> comparingByValue() {
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> c1.getValue().compareTo(c2.getValue());
    }

    /*
    返回一个比较器，该比较器使用给定的 Comparator 通过键比较 Map.Entry。
    如果指定的比较器也可序列化，则返回的比较器可序列化。
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
        Objects.requireNonNull(cmp);
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> cmp.compare(c1.getKey(), c2.getKey());
    }

    /*
    返回一个比较器，该比较器使用给定的 Comparator 通过值比较 Map.Entry。
    如果指定的比较器也可序列化，则返回的比较器可序列化。
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
        Objects.requireNonNull(cmp);
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> cmp.compare(c1.getValue(), c2.getValue());
    }
}
```

# 2. 方法

## 2.1 equals
```java
/*
比较指定对象与此 Map 是否相等。如果给定对象也是一个 Map ，并且两个 Map 表示相同的 Map ，则返回true。
更正式地说，如果 m1.entrySet().equals(m2.entrySet())，则两个 Map m1 和 m2 表示相同的 Map。
这样可确保 equals 方法可在 Map 接口的不同实现中正常工作。
*/
boolean equals(Object o);
```

## 2.2 hashCode
```java
/*
返回此 Map 的哈希码值。Map 的哈希码定义为 Map 的 entrySet() 视图中每个条目的哈希码之和。
这确保了符合 Object.hashCode 的规定。
*/
int hashCode();
```

## 2.3 元素数量查询
```java
// 返回此 Map 中的键值映射数。如果 Map 包含多余 Integer.MAX_VALUE 个元素，
// 则返回 Integer.MAX_VALUE。
int size();

boolean isEmpty();
```

## 2.4 contains
```java
/*
如果此 Map 包含指定键，则返回 true。更正式地说，当且仅当此 Map 包含键 k，使得
(key == null ? k == null: key.equals(k)) 时，才返回 true。（最多可以有一个这样的映射。）
*/
boolean containsKey(Object key);

/*
如果此 Map 将一个或多个指定值，则返回 true。更正式地讲，当且仅当此映射包含至少一个值 v
(v== null ? v == null: value.equals(v)) 时，才返回 true。
对于 Map 接口的大多数实现，此操作可能需要线性时间。
*/
boolean containsValue(Object value);
```

## 2.5 get
```java
/*
返回指定键所映射的值；如果 key 不存在，则返回 null。
更正式地讲，如果此 Map 包含从键 k 到值 v 的映射，使得 (key == null ? k == null: key.equals(k))，
则此方法返回 v；否则返回 null。（最多可以有一个这样的映射。）

如果此 Map 允许 null 值，则返回 null 不一定表示该 Map 不包含 key。
可以使用 containsKey 确定是否包含 key。
*/
V get(Object key);

// 返回指定键所映射到的值，如果此 Map 不包含指定键，则返回 defaultValue。
default V getOrDefault(Object key, V defaultValue) {
    V v;
    return (((v = get(key)) != null) || containsKey(key))
        ? v
        : defaultValue;
}
```

## 2.6 put
```java
/*
键此 Map 中的指定键与指定值关联（可选操作）。如果该 Map 先前包含该键的映射，则旧值将替换为指定值。
（仅当 m.containsKey(k) 返回 true 时，才认为映射 m 包含键 k 的映射。）
*/
V put(K key, V value);

// 如果指定的键尚未与值关联（或其值为 null），则将其与给定值关联并返回 null，否则返回原来的值。
default V putIfAbsent(K key, V value) {
    V v = get(key);
    if (v == null) {
        v = put(key, value);
    }

    return v;
}

/*
将所有映射从指定 Map 复制到此 Map（可选操作）。对于从指定 Map 中的键 k 到值 v 的每个映射，
此调用的效果等效于在此 Map 上调用一次 put(k，v)。

如果在操作进行过程中修改了指定的 Map，则此操作的行为是不确定的。
*/
void putAll(Map<? extends K, ? extends V> m);
```

## 2.7 compute
```java
/*
如果指定的键尚未与值关联（或其值为 null），则尝试使用给定的映射函数计算其值，计算结果不为 null 则将其写入。
如果函数返回 null ，则不会做任何事。如果函数本身引发异常，则该异常传播给调用者，并且不做任何事。

最常见的用法是构造一个新对象，用作初始映射值或备注结果，如下所示：  
    map.computeIfAbsent(key，k -> new Value(f(k)));
 
或实现多值映射 Map<K，Collection<V>>，每个键支持多个值：
    map.computeIfAbsent(key，k -> new HashSet<V>()).add(v);

返回与指定键关联的当前（现有的或计算的）值；如果计算的值为 null，则返回 null
*/
default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V v;
    if ((v = get(key)) == null) {
        V newValue;
        if ((newValue = mappingFunction.apply(key)) != null) {
            put(key, newValue);
            return newValue;
        }
    }

    return v;
}

/*
如果指定键的值存在且非空，则尝试在给定键及其值的情况下计算新映射。

如果函数返回 null，则删除映射。如果函数本身引发异常，则该异常传播给调用者，并且当前映射保持不变。

返回与指定键关联的新值；如果没有，则为 null
*/
default V computeIfPresent(K key,
        BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue;
    if ((oldValue = get(key)) != null) {
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue != null) {
            put(key, newValue);
            return newValue;
        } else {
            remove(key);
            return null;
        }
    } else {
        return null;
    }
}

/*
尝试计算指定键及其值的映射。例如，要将 String msg 写入或附加到值：
    map.compute(key, (k, v) -> (v == null) ? msg：v.concat(msg))
（方法 merge() 通常更容易用于此目的。）

如果函数返回 null，则将删除映射（如果最初不存在，则保持不存在）。
如果函数本身引发异常，则该异常传播给调用者，并且当前映射保持不变。
*/
default V compute(K key,
        BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);

    V newValue = remappingFunction.apply(key, oldValue);
    if (newValue == null) {
        // delete mapping
        if (oldValue != null || containsKey(key)) {
            // something to remove
            remove(key);
            return null;
        } else {
            // nothing to do. Leave things as they were.
            return null;
        }
    } else {
        // add or replace old mapping
        put(key, newValue);
        return newValue;
    }
}
```

## 2.8 merge
```java
/*
如果指定的键尚未与值关联或其值为 null，请将其与给定的非 null 值关联。否则，
用给定的映射函数的计算值，如果函数结果为 null，则将其删除。

当组合一个键的多个映射值时，此方法可能有用。例如，要将 String msg 写入或附加到值：
    map.merge(key，msg，String::concat)
 
如果函数返回 null，则删除映射。如果函数本身引发异常，则该异常传播给调用者，并且当前映射保持不变。
*/
default V merge(K key, V value,
        BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    Objects.requireNonNull(value);
    V oldValue = get(key);
    V newValue = (oldValue == null) ? value :
               remappingFunction.apply(oldValue, value);
    if(newValue == null) {
        remove(key);
    } else {
        put(key, newValue);
    }
    return newValue;
}
```

## 2.9 replace
```java
// 仅当 key 的值为 oldValue 时，才将其值替换为 newValue。
default boolean replace(K key, V oldValue, V newValue) {
    Object curValue = get(key);
    if (!Objects.equals(curValue, oldValue) ||
        (curValue == null && !containsKey(key))) {
        return false;
    }
    put(key, newValue);
    return true;
}

// 仅当 key 的值存在时，才将其值替换为 value。
default V replace(K key, V value) {
    V curValue;
    if (((curValue = get(key)) != null) || containsKey(key)) {
        curValue = put(key, value);
    }
    return curValue;
}

/*
在该 Map 上调用给定函数替换每个条目的值，直到处理完所有条目或该函数引发异常为止。
该函数引发的异常将传递给调用者。
*/
default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Objects.requireNonNull(function);
    for (Map.Entry<K, V> entry : entrySet()) {
        K k;
        V v;
        try {
            k = entry.getKey();
            v = entry.getValue();
        } catch(IllegalStateException ise) {
            // 发生异常通常意味着该条目不再存在于 Map 中。
            throw new ConcurrentModificationException(ise);
        }

        v = function.apply(k, v);
        try {
            entry.setValue(v);
        } catch(IllegalStateException ise) {
            // 发生异常通常意味着该条目不再存在于 Map 中。
            throw new ConcurrentModificationException(ise);
        }
    }
}
```

## 2.10 remove
```java
/*
如果存在，则从此 Map 中删除指定键的映射（可选操作）。更正式地讲，如果此 Map 包含从键 k 到值 v 的映射，
使得 (key == null ? k == null: key.equals(k))，则将删除该映射。（Map 最多可以包含一个这样的映射。）

返回先前与该键相关联的值；如果此 Map 不包含该键的映射关系，则返回 null。

如果此 Map 允许 null 值，则返回 null 不一定表示该 Map 不包含 key。
*/
V remove(Object key);

// 仅当 key 的值为 oldValue 时，才删除它。
default boolean remove(Object key, Object value) {
    Object curValue = get(key);
    if (!Objects.equals(curValue, value) ||
        (curValue == null && !containsKey(key))) {
        return false;
    }
    remove(key);
    return true;
}
```

## 2.11 forEach
```java
/*
在此 Map 中为每个条目执行给定的操作，直到所有条目都已处理或该操作引发异常为止。

除非实现类另行指定，否则操作将按照 entrySet() 方法迭代的顺序执行（如果指定了迭代顺序。）

操作所引发的异常会传递给调用者。
*/
default void forEach(BiConsumer<? super K, ? super V> action) {
    Objects.requireNonNull(action);
    for (Map.Entry<K, V> entry : entrySet()) {
        K k;
        V v;
        try {
            k = entry.getKey();
            v = entry.getValue();
        } catch(IllegalStateException ise) {
            // 发生异常通常意味着该条目不再存在于 Map 中。
            throw new ConcurrentModificationException(ise);
        }
        action.accept(k, v);
    }
}
```

## 2.12 视图方法
```java
/*
返回此 Map 中包含的键的 Set 视图。该 Set 由 Map 支持，因此对 Map 的更改会反映在 Set 中，反之亦然。

如果在对 Set 进行迭代时修改了 Map（通过迭代器自己的 remove 操作除外），则迭代的结果不确定。

该 Set 支持删除，通过 Iterator.remove，Set.remove，removeAll，retainAll 
和 clear 操作从 Map 中删除相应的映射。

它不支持 add 或 addAll 操作。
*/
Set<K> keySet();

/*
返回此 Map 中包含的值的 Collection 视图。集合由 Map 支持，因此对 Map 的更改会反映在集合中，反之亦然。

如果在对集合进行迭代时修改了 Map（通过迭代器自己的 remove 操作除外），则迭代的结果是不确定的。

集合支持删除，通过 Iterator.remove，Collection.remove，removeAll，retainAll
和 clear 操作从 Map 中删除相应的映射。

它不支持 add 或 addAll 操作。
*/
Collection<V> values();

/*
返回此 Map 中包含的条目的 Set 视图。该 Set 由 Map 支持，因此对 Map 的更改会反映在 Set 中，反之亦然。

如果在对 Set 进行迭代时修改了 Map（通过迭代器自己的 remove 操作除外），则迭代的结果不确定。

该 Set 支持删除，通过 Iterator.remove，Set.remove，removeAll，retainAll 
和 clear 操作从 Map 中删除相应的映射。

它不支持 add 或 addAll 操作。

*/
Set<Map.Entry<K, V>> entrySet();
```

## 2.13 clear
```java
// 删除 Map 中所有的条目
void clear();
```