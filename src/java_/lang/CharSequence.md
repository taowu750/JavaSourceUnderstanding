`java.lang.CharSequence`接口的声明如下：
```java
public interface CharSequence
```
这个接口表示`char`值的**只读**序列，此接口对许多不同种类的`char`序列提供统一的只读访问。
此接口不修改`equals`和`hashCode`方法的常规协定，因此通常未定义比较两个实现了`CharSequence`的对象的结果。
它有几个实现类：`CharBuffer`、`String`、`StringBuffer`、`StringBuilder`。

在`Java8`之后，`CharSequence`多了两个默认方法：`chars()`和`codePoints()`。
它们返回`IntStream`，分别产生字符和代码点流。

代理、代码点的相关概念参见 [字符集编码.md][unicode]。

# 1. 方法

## 1.1 抽象方法
```java
// 返回此字符序列的长度。长度是序列中 char 的数量，因此对于包含了非 BMP 字符的字符序列，
// 此方法返回的长度将大于实际字符数量。
int length();

// 返回指定索引处的 char 值。索引的范围为 [0, length())。
// 如果 index 处是增补字符，将返回它的高代理或低代理部分。 
char charAt(int index);

// 返回一个 CharSequence 它是此序列的子序列。该子序列范围为 [start, end)。返回序列的长度（以char单位）为 end-start，
// 因此，如果start == end，则返回一个空序列。
CharSequence subSequence(int start, int end);

// 返回一个包含此序列中字符的字符串，该字符串的顺序与此序列相同。 字符串的长度将是此序列的长度。
public String toString();
```

## 1.2 chars
```java
// 将 CharSequence 中的字符返回为 IntStream
public default IntStream chars() {
    class CharIterator implements PrimitiveIterator.OfInt {
        int cur = 0;

        public boolean hasNext() {
            return cur < length();
        }

        public int nextInt() {
            if (hasNext()) {
                return charAt(cur++);
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void forEachRemaining(IntConsumer block) {
            for (; cur < length(); cur++) {
                block.accept(charAt(cur));
            }
        }
    }

    return StreamSupport.intStream(() ->
               Spliterators.spliterator(
                       new CharIterator(),
                       length(),
                       Spliterator.ORDERED),
               Spliterator.SUBSIZED | Spliterator.SIZED | Spliterator.ORDERED,
               false);
}
```
可以看出，这个方法使用`StreamSupport.intStream()`和`Spliterators.spliterator()`方法创建`IntStream`。
其中用到了一个接口[java.util.PrimitiveIterator.OfInt][PrimitiveIterator]。
<!-- TODO: 解释 StreamSupport.intStream() 方法和 Spliterators.spliteratorUnknownSize 方法 -->

## 1.3 codePoints
```java
// 将 CharSequence 中的代码点返回为 IntStream
public default IntStream codePoints() {
    class CodePointIterator implements PrimitiveIterator.OfInt {
        int cur = 0;

        @Override
        public void forEachRemaining(IntConsumer block) {
            final int length = length();
            int i = cur;
            try {
                while (i < length) {
                    char c1 = charAt(i++);
                    // 如果 c1 不是高代理部分
                    if (!Character.isHighSurrogate(c1) || i >= length) {
                        block.accept(c1);
                    } else {
                        char c2 = charAt(i);
                        // 如果 c1 是高代理部分，那就将后面的低代理部分 c2 也提取出来，将它们解析为代码点
                        if (Character.isLowSurrogate(c2)) {
                            i++;
                            block.accept(Character.toCodePoint(c1, c2));
                        } else {
                            block.accept(c1);
                        }
                    }
                }
            } finally {
                cur = i;
            }
        }

        public boolean hasNext() {
            return cur < length();
        }

        public int nextInt() {
            final int length = length();

            if (cur >= length) {
                throw new NoSuchElementException();
            }
            char c1 = charAt(cur++);
            if (Character.isHighSurrogate(c1) && cur < length) {
                char c2 = charAt(cur);
                if (Character.isLowSurrogate(c2)) {
                    cur++;
                    return Character.toCodePoint(c1, c2);
                }
            }
            return c1;
        }
    }

    return StreamSupport.intStream(() ->
               Spliterators.spliteratorUnknownSize(
                      new CodePointIterator(),
                      Spliterator.ORDERED),
               Spliterator.ORDERED,
               false);
}
```


[unicode]: 字符集编码.md
[PrimitiveIterator]: ../util/PrimitiveIterator.md