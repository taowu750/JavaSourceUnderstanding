`java.lang.Appendable`接口的声明如下：
```java
public interface Appendable
```
此接口表示可以附加字符序列和字符的对象。它的子类必须能够接受来自`java.util.Formatter`的格式化输出。
要追加的字符应该是`Unicode`字符规范中描述的有效`Unicode`字符。注意，增补字符可能由多个 16 位字符（char）组成。
此接口对于多线程访问不一定是安全的。线程安全是扩展和实现这个接口的类的职责。由于此接口可能由具有不同错误处理风格的类实现，
因此不能保证错误将传播到调用方。

有关`Unicode`字符规范和增补字符的相关知识参见 [字符集编码.md][unicode]。

# 1. 方法
```java
/*
将指定的字符序列附加到此 Appendable。

根据 csq 实现类的差别，可能不会附加整个序列。例如，如果 csq 是 java.nio.CharBuffer，
则要追加的子序列由缓冲区的位置和限制定义。
*/
Appendable append(CharSequence csq) throws IOException;

// 将指定字符序列的子序列追加到此 Appendable。子序列范围为 [start, end)。
Appendable append(CharSequence csq, int start, int end) throws IOException;

// 将指定字符追加到此Appendable。
Appendable append(char c) throws IOException;
```


[unicode]: 字符集编码.md