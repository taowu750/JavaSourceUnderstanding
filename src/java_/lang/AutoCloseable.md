`java.lang.AutoCloseable`接口的声明如下：
```java
public interface AutoCloseable
```
此接口表示在关闭之前可以释放资源（例如文件或`Socket`）的对象。当退出`try-with-resources`块时，
将自动调用`AutoCloseable`接口的`close()`方法。这种构造可确保及时释放，避免资源耗尽异常和可能发生的错误。

一种可能并且常见的情形是：基类实现了`AutoCloseable`接口，但不是所有的子类或实例都拥有可释放的资源。
对于必须完全通用的代码，或已知`AutoCloseable`实例需要释放资源的代码，建议使用`try-with-resources`构造。
但是，当使用同时支持基于 I/O 和基于非 I/O 的设施时（例如`java.util.stream.Stream`），
使用其中的非 I/O 功能时通常不需要`try-with-resources`。

## 1. 方法
```java
/*
关闭此资源。在 try-with-resources 语句管理的对象上自动调用此方法。

虽然声明此接口方法抛出 Exception ，但强烈建议实现者声明 close 方法的具体实现以抛出更具体的异常，
或者如果 close 操作不会失败，则不抛出任何异常。

关闭操作可能失败的情况需要小心实现。强烈建议在抛出异常之前，放弃基础资源，并在内部将资源标记为已关闭。
close 方法不太可能被多次调用，因此可以确保及时释放资源。此外，它减少了在资源被另一资源包装时可能出现的问题。

强烈建议此接口的实现者不要使用 close 方法抛出 InterruptedException。此异常与线程的中断状态相互作用，
如果 InterruptedException 被抑制，则可能会发生运行时异常。更一般而言，如果抑制异常会导致其他问题，
则 AutoCloseable.close 方法不应抛出异常。

请注意，与 java.io.Closeable 的 close 方法不同的是，AutoCloseable.close 不需要是幂等的。
换句话说，多次调用此 close 方法可能会产生一些明显的副作用，这与 Closeable.close 不同，后者多次调用和一次调用行为一致。
但是，强烈建议此接口的实现者使其 close 方法成为幂等的。
*/
void close() throws Exception;
```
有关抑制异常，参见 [try-with-resources及异常抑制.md][suppress]。

[suppress]: try-with-resources及异常抑制.md