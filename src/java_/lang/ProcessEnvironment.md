`java.lang.ProcessEnviroment`类的声明如下：
```java
final class ProcessEnvironment extends HashMap<String,String>
```
`ProcessEnviroment`是线程的执行环境，也就是一组系统环境变量。

`ProcessEnviroment`代码中比较值得注意的有：
 - 2.5 NameComparator: 定制忽略大小写的字符串比较规则。
 - 3.1 静态构造块：`Collections.unmodifiable`仍可通过更改参数容器改变此方法的返回容器内容。

# 1. 成员字段
```java
// 环境变量名最小长度
// 注意 Windows 允许将 '=' 作为名称的第一个字符，表示一些 magic 环境变量， 如 =C:=C:\DIR
static final int MIN_NAME_LENGTH = 1;

// 环境变量名比较器，大小写不敏感
private static final NameComparator nameComparator;
// 环境变量键值对比较器，使用环境变量名进行比较，大小写不敏感
private static final EntryComparator entryComparator;
// 当前环境。它可以更改，大小写敏感
private static final ProcessEnvironment theEnvironment;
// 不能更改的环境，大小写敏感
private static final Map<String,String> theUnmodifiableEnvironment;
// 可以更改的环境（但未给外部提供修改接口），大小写不敏感
private static final Map<String,String> theCaseInsensitiveEnvironment;
```

# 2. 内部类

## 2.1 CheckedEntry
```java
// CheckedEntry 表示环境变量键值对，键和值都是字符串。
private static class CheckedEntry implements Map.Entry<String,String> {
    private final Map.Entry<String,String> e;

    public CheckedEntry(Map.Entry<String,String> e) {this.e = e;}

    public String getKey()   { return e.getKey();}
    public String getValue() { return e.getValue();}
    public String setValue(String value) {
        return e.setValue(validateValue(value));
    }

    public String toString() { return getKey() + "=" + getValue();}
    public boolean equals(Object o) {return e.equals(o);}
    public int hashCode()    {return e.hashCode();}
}
```

## 2.2 CheckedEntrySet
```java
// 用于存放环境变量键值对的 Set
private static class CheckedEntrySet extends AbstractSet<Map.Entry<String,String>> {
    private final Set<Map.Entry<String,String>> s;
    
    public CheckedEntrySet(Set<Map.Entry<String,String>> s) {this.s = s;}
    
    public int size()        {return s.size();}
    public boolean isEmpty() {return s.isEmpty();}
    public void clear()      {       s.clear();}
    
    public Iterator<Map.Entry<String,String>> iterator() {
        return new Iterator<Map.Entry<String,String>>() {
            Iterator<Map.Entry<String,String>> i = s.iterator();
            public boolean hasNext() { return i.hasNext();}
            public Map.Entry<String,String> next() {
                return new CheckedEntry(i.next());
            }
            public void remove() { i.remove();}
        };
    }

    public boolean contains(Object o) {return s.contains(checkedEntry(o));}
    public boolean remove(Object o)   {return s.remove(checkedEntry(o));}

    private static Map.Entry<String,String> checkedEntry(Object o) {
        @SuppressWarnings("unchecked")
        Map.Entry<String,String> e = (Map.Entry<String,String>) o;
        // 调用 ProcessEnvironment 方法 nonNullString，当参数字符串为 null 时抛出异常
        nonNullString(e.getKey());
        nonNullString(e.getValue());
        return e;
    }
}
```

## 2.3 CheckedValues
```java
// 用于存放环境变量值的集合
private static class CheckedValues extends AbstractCollection<String> {
    private final Collection<String> c;
    
    public CheckedValues(Collection<String> c) {this.c = c;}
    
    public int size()                  {return c.size();}
    public boolean isEmpty()           {return c.isEmpty();}
    public void clear()                {       c.clear();}
    public Iterator<String> iterator() {return c.iterator();}
    public boolean contains(Object o)  {return c.contains(nonNullString(o));}
    public boolean remove(Object o)    {return c.remove(nonNullString(o));}
}
```

## 2.4 CheckedKeySet
```java
// 存放环境变量名的 Set
private static class CheckedKeySet extends AbstractSet<String> {
    private final Set<String> s;
    
    public CheckedKeySet(Set<String> s) {this.s = s;}
    
    public int size()                  {return s.size();}
    public boolean isEmpty()           {return s.isEmpty();}
    public void clear()                {       s.clear();}
    public Iterator<String> iterator() {return s.iterator();}
    public boolean contains(Object o)  {return s.contains(nonNullString(o));}
    public boolean remove(Object o)    {return s.remove(nonNullString(o));}
}
```

## 2.5 NameComparator
```java
// 环境变量名的比较器。它对大小写不敏感
private static final class NameComparator implements Comparator<String> {
        
    public int compare(String s1, String s2) {
        // 我们不能使用 String.compareToIgnoreCase，因为它将字符转为小写进行比较，
        // 而 Windows 对环境变量名的默认比较行为是将字符转为大写。
        // 例如，"_" 应该排在 "Z" 之后，而不是前面。
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2)
                    // No overflow because of numeric promotion
                    return c1 - c2;
            }
        }
        return n1 - n2;
    }
}
```

## 2.6 EntryComparator
```java
// 环境变量键值对的比较器。实现中使用 NameComparator 对键比较的结果
private static final class EntryComparator implements Comparator<Map.Entry<String,String>> {
    public int compare(Map.Entry<String,String> e1,
                       Map.Entry<String,String> e2) {
        return nameComparator.compare(e1.getKey(), e2.getKey());
    }
}
```

# 3. 构造器/块

## 3.1 静态构造块
```java
static {
    nameComparator  = new NameComparator();
    entryComparator = new EntryComparator();
    // 创建当前环境
    theEnvironment  = new ProcessEnvironment();
    // Collections.unmodifiableMap 返回的 Map 无法被修改，
    // 但是 UnmodifiableMap 底层使用参数 Map。更改参数 Map，可以间接地修改 UnmodifiableMap
    theUnmodifiableEnvironment = Collections.unmodifiableMap(theEnvironment);

    String envblock = environmentBlock();
    // 环境变量开始位置；环境变量结束位置；键值对 '=' 分隔符位置
    int beg, end, eql;
    for (beg = 0;
          // 环境变量之间用 '\0' 分隔
         ((end = envblock.indexOf('\u0000', beg)) != -1 &&
          // 某些环境变量首字母为 '='，表示一个 magic Windows 变量名称。
          // 所以需要从 beg + 1 处开始查找
          (eql = envblock.indexOf('=', beg+1)) != -1);
         beg = end + 1) {
        // 忽视损坏的环境变量
        if (eql < end)
            theEnvironment.put(envblock.substring(beg, eql), envblock.substring(eql+1,end));
    }

    theCaseInsensitiveEnvironment = new TreeMap<>(nameComparator);
    // theEnvironment、theUnmodifiableEnvironment、theCaseInsensitiveEnvironment 初始化内容相同
    theCaseInsensitiveEnvironment.putAll(theEnvironment);
}

// 本地方法，获取所有环境变量。每个环境变量用 '\0' 分隔。最后以两个 '\0' 结束
private static native String environmentBlock();
```

## 3.2 构造器
```java
private ProcessEnvironment() {
    super();
}

private ProcessEnvironment(int capacity) {
    super(capacity);
}
```

# 4. 方法

## 4.1 验证
```java
// 验证环境变量名是否符合规范。环境变量名除开头外不能包含 '='；环境变量名不能包含 '\0'。
private static String validateName(String name) {
    // 某些环境变量首字母为 '='，表示一个 magic Windows 变量名称
    if (name.indexOf('=', 1)   != -1 ||
        // 环境变量名不能包含 '\0'
        name.indexOf('\u0000') != -1)
        throw new IllegalArgumentException("Invalid environment variable name: \"" + name + "\"");
    return name;
}

// 验证环境变量值是否符合规范。环境变量值不能包含 '\0'。
private static String validateValue(String value) {
    if (value.indexOf('\u0000') != -1)
        throw new IllegalArgumentException("Invalid environment variable value: \"" + value + "\"");
    return value;
}

// 检测 o 不是 null，返回字符串。
private static String nonNullString(Object o) {
    if (o == null)
        throw new NullPointerException();
    return (String) o;
}
```

## 4.2 Map 方法
```java
// 以下 Map 方法都是对 ProcessEnvironment 的内容进行操作。它可更改并且对大小写敏感。

public String put(String key, String value) {
    return super.put(validateName(key), validateValue(value));
}

public String get(Object key) {
    return super.get(nonNullString(key));
}

public boolean containsKey(Object key) {
    return super.containsKey(nonNullString(key));
}

public boolean containsValue(Object value) {
    return super.containsValue(nonNullString(value));
}

public String remove(Object key) {
    return super.remove(nonNullString(key));
}

public Set<String> keySet() {
    return new CheckedKeySet(super.keySet());
}

public Collection<String> values() {
    return new CheckedValues(super.values());
}

public Set<Map.Entry<String,String>> entrySet() {
    return new CheckedEntrySet(super.entrySet());
}
```

## 4.3 env
```java
// 根据环境变量名获取对应的值。忽略大小写。
static String getenv(String name) {
    /*
     原始实现使用本地调用 _wgetenv，但事实证明，如果使用 "wmain" 而不是 "main"，
     即使是使用 CREATE_UNICODE_ENVIRONMENT 创建的进程，_wgetenv 仅与 GetEnvironmentStringsW（对于非 ASCII）一致。
     相反，我们自己执行不区分大小写的比较。至少这保证了 System.getenv().get(String) 与 System.getenv(String) 一致。
     */
    return theCaseInsensitiveEnvironment.get(name);
}

// 获取所有环境变量。返回的 Map 不可修改，大小写敏感。
static Map<String,String> getenv() {
    return theUnmodifiableEnvironment;
}

// 获取 theEnvironment 的克隆。仅用于 ProcessBuilder.environment()
static Map<String,String> environment() {
    return (Map<String,String>) theEnvironment.clone();
}

// 返回空的 ProcessEnvironment。仅用于 ProcessBuilder.environment(String[] envp)
static Map<String,String> emptyEnvironment(int capacity) {
    return new ProcessEnvironment(capacity);
}
```

## 4.4 addToEnv
```java
// 将 name 和 val 最为键值对（以 '=' 分隔）添加到 sb 中，最后以 '\0' 结尾。
private static void addToEnv(StringBuilder sb, String name, String val) {
    sb.append(name).append('=').append(val).append('\u0000');
}

// 如果环境变量 name 存在于 theCaseInsensitiveEnvironment 中，将其添加到 sb 中
private static void addToEnvIfSet(StringBuilder sb, String name) {
    String s = getenv(name);
    if (s != null)
        addToEnv(sb, name, s);
}
```

## 4.5 toEnvironmentBlock
```java
// 将当前 ProcessEnvironment 的内容转换为 environmentBlock 字符串。
// environmentBlock 每个环境变量用 '\0' 分隔。最后以两个 '\0' 结束
String toEnvironmentBlock() {
    List<Map.Entry<String,String>> list = new ArrayList<>(entrySet());
    // 以 Unicode 大小写不敏感的方式根据环境变量名排序
    Collections.sort(list, entryComparator);

    StringBuilder sb = new StringBuilder(size()*30);
    int cmp = -1;

    // 一些版本的 MSVCRT.DLL 需要设置 SystemRoot 环境变量。所以我们需要确保它总是被设置，
    // 即使调用者没有提供。
    final String SYSTEMROOT = "SystemRoot";

    for (Map.Entry<String,String> e : list) {
        String key = e.getKey();
        String value = e.getValue();
        // compare 结果小于 0 忽略，这样 cmp 还是小于 0；
        // 等于 0 表示存在 SystemRoot 无需设置；
        // 大于 0 表示之前一直没遇到 SystemRoot，此时需要从 theCaseInsensitiveEnvironment 中查找
        if (cmp < 0 && (cmp = nameComparator.compare(key, SYSTEMROOT)) > 0) {
            // 将 theCaseInsensitiveEnvironment 的 SystemRoot 环境变量添加到当前 ProcessEnvironment 中
            addToEnvIfSet(sb, SYSTEMROOT);
        }
        addToEnv(sb, key, value);
    }
    if (cmp < 0) {
        // 查找到列表的末尾还没找到 SystemRoot，则从 theCaseInsensitiveEnvironment 中查找并加入
        addToEnvIfSet(sb, SYSTEMROOT);
    }
    if (sb.length() == 0) {
        // 当前 ProcessEnvironment 是空的并且 theCaseInsensitiveEnvironment 没有 SystemRoot，
        // 需要添加一个 '\0'，保证最终结果末尾有两个 '\0'
        sb.append('\u0000');
    }
    // 以两个 '\0' 结束
    sb.append('\u0000');
    return sb.toString();
}

// map 为 null 返回 null，否则将其转型为 ProcessEnvironment 并返回它的 environmentBlock。
static String toEnvironmentBlock(Map<String,String> map) {
    return map == null ? null : ((ProcessEnvironment)map).toEnvironmentBlock();
}
```