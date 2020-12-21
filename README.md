# Java源码解读

这个是本人对`Java`源码的解读项目，包含阅读`Java`源码的理解和感悟。我会一直长期更新，期待自己~~头秃~~成为`Java`高手的一天。

本项目解读的`Java`源码版本为 1.8.0_231，还用到了`JUnit5`库进行测试和验证工作。

## 项目结构

1. src 文件夹：包含对`Java`源码的解读文档和其他相关知识的文档，和`Java`源码目录保持一致，只是在后面加上<strong>"_"</strong>以区分
2. test 文件夹：包含一些测试和验证代码
3. res 文件夹：包含图片等资源

## 源码的阅读顺序

1. 精读源码: 这些类比较常用且简单，看看它们有助于锻炼看源码的感觉，也了解一下大神们写代码的风格
    - java.lang
    - java.util
    - java.io
2. 深刻理解: 这个级别要求的类，全都是一些进阶到高级所必须了解的
    - java.nio: 需要了解非阻塞 io
    - java.lang.reflect: 反射要了解清楚的话，需要搞明白 JVM 的类加载机制
    - java.util.concurrent: 需要了解并发、多线程的相关知识
    - java.net
    - javax.net: 网络 IO 要搞清楚的话，需要清楚 TCP/IP 和 HTTP、HTTPS
    - java.util.stream: Java8 新特性流
3. 会用即可
    - java.lang.ref
    - java.math
    - java.util.regex
    - java.security.*
    - javax.security.*
    - java.sql
    - javax.sql.*
    - java.lang.annotation
    - javax.transaction.*
    - java.rmi.*
    - javax.rmi.*
    - java.text
    - java.util.logging
    - java.util.prefs
    - java.util.jar
    - java.util.zip
    - javax.annotation.*
    - javax.xml.*
    - org.w3c.dom.*
    - org.xml.sax.*
    - javax.crypto.*
    - javax.imageio.*
    - javax.jws.*
    
## 不止源码

在阅读源码的过程中，我发现我不止掌握了源码的执行过程、设计和算法等知识。为了弄懂源码中一些底层原理，或是看似不起眼的细节，
我查阅了很多资料，最终掌握了很多计算机领域的相关知识、`JVM`的底层原理和计算机的底层原理。

因此，不仅仅是源码中蕴含的知识，从源码延伸开来的细节和背后的原理才是精华所在，这也是我阅读源码路上的最大收获。
