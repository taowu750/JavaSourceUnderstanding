`java.lang.Readable`接口的声明如下：
```java
public interface Readable
```
`Readable`是字符的来源。`Readable`的字符可通过`java.nio.CharBuffer`提供给调用者。

# 1. 方法
```java
// 尝试将字符读入指定的字符缓冲区。该缓冲区按原样用作字符存储库：所做的唯一更改是放置操作的结果。
// 不执行缓冲区的 flip 或 rewind 操作。
// 返回添加到缓冲区的 char 的数量；如果当前字符源在其末尾，则返回 -1
public int read(java.nio.CharBuffer cb) throws IOException;
```