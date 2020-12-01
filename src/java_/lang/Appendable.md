`java.lang.Appendable`接口的声明如下：
```java
public interface Appendable
```
此接口表示可以附加字符序列和字符的对象。它的子类必须能够接受来自`java.util.Formatter`的格式化输出。
要追加的字符应该是`Unicode`字符规范中描述的有效`Unicode`字符。注意，增补字符可能由多个 16 位字符（char）组成。
此接口对于多线程访问不一定是安全的。线程安全是扩展和实现这个接口的类的职责。由于此接口可能由具有不同错误处理风格的类实现，
因此不能保证错误将传播到调用方。

<!-- TODO: 了解 Formatter -->

# 1. 方法

1. `Appendable append(CharSequence csq) throws IOException;`
2. `Appendable append(CharSequence csq, int start, int end) throws IOException;`
3. `Appendable append(char c) throws IOException;`