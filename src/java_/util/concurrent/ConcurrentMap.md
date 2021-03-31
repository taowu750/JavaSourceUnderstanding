`java.util.concurrent.ConcurrentMap`接口的声明如下：
```java
public interface ConcurrentMap<K, V> extends Map<K, V>
```
一个提供线程安全和原子性保证的 `Map`。

内存一致性效果：和其他并发集合一样，在一个线程中将一个对象作为键或值放入 `ConcurrentMap` 之前的操作，
**happens-before** 另一个线程访问或从 `ConcurrentMap` 中移除该对象的后续操作。

此接口方法的更多说明参见 [Map][map] 接口。

# 1. 方法

## 1.1 getOrDefault
```java
/*
返回 key 的映射值，如果不存在，则返回 defaultValue。
这个实现假定 ConcurrentMap 不能包含 null 值，get() 返回 null 意味着 key 不存在。支持空值的实现必须覆盖这个默认实现。
*/
@Override
default V getOrDefault(Object key, V defaultValue) {
    V v;
    return ((v = get(key)) != null) ? v : defaultValue;
}
```

## 1.2 forEach
```java
// 这个实现认为，由 getKey() 或 getValue() 抛出的 IllegalStateException 表示该条目已被删除，无法处理。
// 对于后续的条目继续操作。
@Override
default void forEach(BiConsumer<? super K, ? super V> action) {
    Objects.requireNonNull(action);
    for (Map.Entry<K, V> entry : entrySet()) {
        K k;
        V v;
        try {
            k = entry.getKey();
            v = entry.getValue();
        } catch(IllegalStateException ise) {
            // 此异常通常意味着条目已经不在此 Map 中了
            continue;
        }
        action.accept(k, v);
    }
}
```

## 1.3 putIfAbsent
```java
/*
键不存在，则插入键值对。相当于：
 if (!map.containsKey(key))
   return map.put(key, value);
 else
   return map.get(key);
*/
V putIfAbsent(K key, V value);
```

## 1.4 remove
```java
/*
只有在 key 映射的值等于 value 的情况下，才会删除 key。这相当于：
 if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
   map.remove(key);
   return true;
 } else
   return false;
*/
boolean remove(Object key, Object value);
```

## 1.5 replace
```java
/*
仅在 key 映射的值等于 oldValue 的情况下将值替换为 newValue。这相当于：
 if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
   map.put(key, newValue);
   return true;
 } else
   return false;
*/
boolean replace(K key, V oldValue, V newValue);

/*
只有当 key 映射到某个值时，才会替换 key 的值。这相当于：
 if (map.containsKey(key)) {
   return map.put(key, value);
 } else
   return null;
*/
V replace(K key, V value);
```

## 1.6 replaceAll
```java
/*
此默认实现相当于：
     for ((Map.Entry<K, V> entry : map.entrySet())
        do {
            K k = entry.getKey();
            V v = entry.getValue();
        } while(!replace(k, v, function.apply(k, v)));

当多个线程尝试更新时，默认的实现可能会重试这些步骤，包括有可能为给定的键重复调用 function。

这个实现假定 ConcurrentMap 不能包含 null 值，并且 get() 返回 null 意味着 key 不存在。
支持 null 值的实现必须覆盖这个默认实现。
*/
@Override
default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Objects.requireNonNull(function);
    forEach((k,v) -> {
        // 如果 v 被其他线程改变或 k 不存在了，则自旋
        while(!replace(k, v, function.apply(k, v))) {
            // 如果 k 不存在了，退出循环
            if ( (v = get(k)) == null) {
                break;
            }
        }
    });
}
```

## 1.7 computeIfAbsent
```java
/*
此默认实现等同于下面的操作：
 if (map.get(key) == null) {
     V newValue = mappingFunction.apply(key);
     if (newValue != null)
         return map.putIfAbsent(key, newValue);
 }

当多个线程尝试更新时，默认实现可能会重试这些步骤，包括可能多次调用 mappingFunction。

这个实现假定 ConcurrentMap 不能包含 null 值，并且 get() 返回 null 意味着 key 不存在。
支持 null 值的实现必须覆盖这个默认实现。
*/
@Override
default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V v, newValue;
    return ((v = get(key)) == null &&
            (newValue = mappingFunction.apply(key)) != null &&
            (v = putIfAbsent(key, newValue)) == null) ? newValue : v;
}
```

## 1.8 computeIfPresent
```java
/*
此默认实现等同于下面的操作：
 if (map.get(key) != null) {
     V oldValue = map.get(key);
     V newValue = remappingFunction.apply(key, oldValue);
     if (newValue != null)
         map.replace(key, oldValue, newValue);
     else
         map.remove(key, oldValue);
 }

当多个线程尝试更新时，默认实现可能会重试这些步骤，包括可能多次调用 mappingFunction。

这个实现假定 ConcurrentMap 不能包含 null 值，并且 get() 返回 null 意味着 key 不存在。
支持 null 值的实现必须覆盖这个默认实现。
*/
@Override
default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue;
    while((oldValue = get(key)) != null) {
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue != null) {
            // 当替换失败时，自旋
            if (replace(key, oldValue, newValue))
                return newValue;
        } else if (remove(key, oldValue))
            return null;
    }
    return oldValue;
}
```

## 1.9 compute
```java
/*
此默认实现等同于下面的操作：
 V oldValue = map.get(key);
 V newValue = remappingFunction.apply(key, oldValue);
 if (oldValue != null ) {
    if (newValue != null)
       map.replace(key, oldValue, newValue);
    else
       map.remove(key, oldValue);
 } else {
    if (newValue != null)
       map.putIfAbsent(key, newValue);
    else
       return null;
 }

当多个线程尝试更新时，默认实现可能会重试这些步骤，包括可能多次调用 mappingFunction。

这个实现假定 ConcurrentMap 不能包含 null 值，并且 get() 返回 null 意味着 key 不存在。
支持 null 值的实现必须覆盖这个默认实现。
*/
@Override
default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);
    for(;;) {
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            // 删除键值对
            if (oldValue != null || containsKey(key)) {
                if (remove(key, oldValue)) {
                    // 删除了旧值返回 null
                    return null;
                }

                // 其他线程改变了 oldValue，再次尝试
                oldValue = get(key);
            } else {
                return null;
            }
        } else {
            // 插入或替换键值对
            if (oldValue != null) {
                // 替换
                if (replace(key, oldValue, newValue)) {
                    return newValue;
                }

                // 其他线程改变了 oldValue，再次尝试
                oldValue = get(key);
            } else {
                // 插入
                if ((oldValue = putIfAbsent(key, newValue)) == null) {
                    return newValue;
                }

                // 其他线程改变了 oldValue，再次尝试
            }
        }
    }
}
```

## 1.10 merge
```java
/*
此默认实现等同于下面的操作：
 V oldValue = map.get(key);
 V newValue = (oldValue == null) ? value :
              remappingFunction.apply(oldValue, value);
 if (newValue == null)
     map.remove(key);
 else
     map.put(key, newValue);

当多个线程尝试更新时，默认实现可能会重试这些步骤，包括可能多次调用 mappingFunction。

这个实现假定 ConcurrentMap 不能包含 null 值，并且 get() 返回 null 意味着 key 不存在。
支持 null 值的实现必须覆盖这个默认实现。
*/
@Override
default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    Objects.requireNonNull(value);
    V oldValue = get(key);
    for (;;) {
        // 替换或删除
        if (oldValue != null) {
            V newValue = remappingFunction.apply(oldValue, value);
            if (newValue != null) {
                if (replace(key, oldValue, newValue))
                    return newValue;
            } else if (remove(key, oldValue)) {
                return null;
            }
            oldValue = get(key);
        } else {
            // 插入
            if ((oldValue = putIfAbsent(key, value)) == null) {
                return value;
            }
        }
    }
}
```


[map]: ../Map.md