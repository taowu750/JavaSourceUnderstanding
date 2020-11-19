# Java源码解读

这个是本人对`Java`源码的解读项目，包含阅读`Java`源码的理解和感悟。我会一直长期更新，期待自己~~头秃~~成为`Java`高手的一天。

本项目解读的`Java`源码版本为 1.8.0_231，还用到了`JUnit5`库进行测试和验证工作。

## 项目结构

1. src 文件夹：包含对`Java`源码的解读文档和其他相关知识的文档，和`Java`源码目录保持一致，只是在最前面加上**u**以区分
2. test 文件夹：包含一些测试和验证代码
3. res 文件夹：包含图片等资源

## 源码的阅读顺序

1. 精读源码: 这些类比较常用且简单，看看它们有助于锻炼看源码的感觉，也了解一下大神们写代码的风格
    - java.lang
    - java.util
    - java.io
2. 深刻理解: 这个级别要求的类，全都是一些进阶到高级所必须了解的
    - java.lang.reflect: 反射要了解清楚的话，需要搞明白JVM的类加载机制
    - java.util.concurrent: 需要了解并发、多线程的相关知识
    - java.nio: 需要了解非阻塞io
    - java.net
    - javax.net: 网络IO要搞清楚的话，需要清楚TCP/IP和HTTP、HTTPS
3. 会用即可
    - java.lang.annotation
    - java.lang.ref
    - java.util.regex
    - java.util.logging
    - java.util.prefs
    - java.util.jar
    - java.util.zip
    - javax.annotation.*
    - java.math
    - java.rmi.*
    - javax.rmi.*
    - java.security.*
    - javax.security.*
    - java.sql
    - javax.sql.*
    - javax.transaction.*
    - java.text
    - javax.xml.*
    - org.w3c.dom.*
    - org.xml.sax.*
    - javax.crypto.*
    - javax.imageio.*
    - javax.jws.*
