`java.util.AbstractMap`抽象类的声明如下：
```java
public abstract class AbstractMap<K,V> implements Map<K,V>
```
此类提供`Map`接口的基本实现，以最大程度地减少实现此接口所需的工作。

要实现不可修改的`Map`，程序员只需扩展此类并为`entrySet`方法提供实现，该方法将返回`Map`映射的试图。
通常，返回的`Set`将是`AbstractSet`的子类。此`Set`不支持`add`或`remove`方法，
并且其迭代器不支持`remove`方法。

要实现可修改的`Map`，程序员必须另外重写此类的`put`方法（否则将引发`UnsupportedOperationException`），
并且`entrySet()`、`iterator()`返回的迭代器必须另外实现其`remove`方法。

根据`Map`接口规范中的建议，程序员通常应提供一个无参构造器和一个`Map`参数构造器。

此类中每个非抽象方法的文档都详细描述了其实现。如果要实现的`Map`有更高效的实现，则可以覆盖这些方法。

参见 [Map.md][map]。

# 1. 成员字段
```java
// keySet() 方法返回的视图 Set，第一次使用时创建
transient Set<K>        keySet;
// values() 方法返回的视图 Collection，第一次使用时创建
transient Collection<V> values;
```

# 2. 方法

## 2.1 equals
```java
public boolean equals(Object o) {
    if (o == this)
        return true;

    if (!(o instanceof Map))
        return false;
    Map<?,?> m = (Map<?,?>) o;
    if (m.size() != size())
        return false;

    try {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            if (value == null) {
                if (!(m.get(key)==null && m.containsKey(key)))
                    return false;
            } else {
                if (!value.equals(m.get(key)))
                    return false;
            }
        }
    } catch (ClassCastException unused) {
        return false;
    } catch (NullPointerException unused) {
        return false;
    }

    return true;
}
```

## 2.2 hashCode
```java
public int hashCode() {
    int h = 0;
    Iterator<Entry<K,V>> i = entrySet().iterator();
    // 计算所有条目的 hashCode 之和
    while (i.hasNext())
        h += i.next().hashCode();
    return h;
}
```

## 2.3 toString
```java
public String toString() {
    Iterator<Entry<K,V>> i = entrySet().iterator();
    if (! i.hasNext())
        return "{}";

    StringBuilder sb = new StringBuilder();
    sb.append('{');
    for (;;) {
        Entry<K,V> e = i.next();
        K key = e.getKey();
        V value = e.getValue();
        sb.append(key   == this ? "(this Map)" : key);
        sb.append('=');
        sb.append(value == this ? "(this Map)" : value);
        if (! i.hasNext())
            return sb.append('}').toString();
        sb.append(',').append(' ');
    }
}
```

## 2.4 clone
```java
protected Object clone() throws CloneNotSupportedException {
    AbstractMap<?,?> result = (AbstractMap<?,?>)super.clone();
    result.keySet = null;
    result.values = null;
    return result;
}
```

## 2.5 元素数量查询
```java
public int size() {
    return entrySet().size();
}

public boolean isEmpty() {
    return size() == 0;
}
```

## 2.6 contains
```java
public boolean containsKey(Object key) {
    Iterator<Map.Entry<K,V>> i = entrySet().iterator();
    if (key==null) {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (e.getKey()==null)
                return true;
        }
    } else {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (key.equals(e.getKey()))
                return true;
        }
    }
    return false;
}

public boolean containsValue(Object value) {
    Iterator<Entry<K,V>> i = entrySet().iterator();
    if (value==null) {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (e.getValue()==null)
                return true;
        }
    } else {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (value.equals(e.getValue()))
                return true;
        }
    }
    return false;
}
```

## 2.7 get
```java
public V get(Object key) {
    Iterator<Entry<K,V>> i = entrySet().iterator();
    if (key==null) {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (e.getKey()==null)
                return e.getValue();
        }
    } else {
        while (i.hasNext()) {
            Entry<K,V> e = i.next();
            if (key.equals(e.getKey()))
                return e.getValue();
        }
    }
    return null;
}
```

## 2.8 put
```java
// 默认未实现
public V put(K key, V value) {
    throw new UnsupportedOperationException();
}

public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
        put(e.getKey(), e.getValue());
}
```

## 2.9 remove
```java
public V remove(Object key) {
    Iterator<Entry<K,V>> i = entrySet().iterator();
    Entry<K,V> correctEntry = null;
    if (key==null) {
        while (correctEntry==null && i.hasNext()) {
            Entry<K,V> e = i.next();
            if (e.getKey()==null)
                correctEntry = e;
        }
    } else {
        while (correctEntry==null && i.hasNext()) {
            Entry<K,V> e = i.next();
            if (key.equals(e.getKey()))
                correctEntry = e;
        }
    }

    V oldValue = null;
    if (correctEntry !=null) {
        oldValue = correctEntry.getValue();
        i.remove();
    }
    return oldValue;
}
```

## 2.10 视图方法
```java
public abstract Set<Entry<K,V>> entrySet();

public Set<K> keySet() {
    Set<K> ks = keySet;
    if (ks == null) {
        ks = new AbstractSet<K>() {
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    // 使用 entrySet() 实现
                    private Iterator<Entry<K,V>> i = entrySet().iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public K next() {
                        return i.next().getKey();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }

            public int size() {
                return AbstractMap.this.size();
            }

            public boolean isEmpty() {
                return AbstractMap.this.isEmpty();
            }

            public void clear() {
                AbstractMap.this.clear();
            }

            public boolean contains(Object k) {
                return AbstractMap.this.containsKey(k);
            }
        };
        keySet = ks;
    }
    return ks;
}

public Collection<V> values() {
    Collection<V> vals = values;
    if (vals == null) {
        vals = new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    // 使用 entrySet() 实现
                    private Iterator<Entry<K,V>> i = entrySet().iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public V next() {
                        return i.next().getValue();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }

            public int size() {
                return AbstractMap.this.size();
            }

            public boolean isEmpty() {
                return AbstractMap.this.isEmpty();
            }

            public void clear() {
                AbstractMap.this.clear();
            }

            public boolean contains(Object v) {
                return AbstractMap.this.containsValue(v);
            }
        };
        values = vals;
    }
    return vals;
}
```

## 2.11 clear
```java
public void clear() {
    entrySet().clear();
}
```

## 2.12 eq
```java
/*
SimpleEntry 和 SimpleImmutableEntry 的实用程序方法。测试是否相等前，检查是否为空。

注意：在解决 JDK-8015417 之前，不要用 Object.equals 替换。
*/
private static boolean eq(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
}
```

# 3. 内部类

## 3.1 SimpleEntry
```java
/*
维护键和值的条目。可以使用 setValue 方法更改该值。此类有助于构建自定义 Map 实现。
例如，在方法 Map.entrySet()、toArray 中返回 SimpleEntry 实例的数组可能会很方便。
*/
public static class SimpleEntry<K,V>
    implements Entry<K,V>, java.io.Serializable
{
    private static final long serialVersionUID = -8499721149061103585L;

    private final K key;
    private V value;

    public SimpleEntry(K key, V value) {
        this.key   = key;
        this.value = value;
    }

    public SimpleEntry(Entry<? extends K, ? extends V> entry) {
        this.key   = entry.getKey();
        this.value = entry.getValue();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    public int hashCode() {
        return (key   == null ? 0 :   key.hashCode()) ^
               (value == null ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }
}
```

## 3.2 SimpleImmutableEntry
```java
/*
维护键和值的条目。不支持 setValue 方法。此类有助于构建自定义 Map 实现。
例如，在方法 Map.entrySet()、toArray 中返回 SimpleEntry 实例的数组可能会很方便。
*/
public static class SimpleImmutableEntry<K,V>
    implements Entry<K,V>, java.io.Serializable
{
    private static final long serialVersionUID = 7138329143949025153L;

    private final K key;
    private final V value;

    public SimpleImmutableEntry(K key, V value) {
        this.key   = key;
        this.value = value;
    }

    public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
        this.key   = entry.getKey();
        this.value = entry.getValue();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    public int hashCode() {
        return (key   == null ? 0 :   key.hashCode()) ^
               (value == null ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }
}
```


[map]: Map.md