# 1. 简介

集合框架是一个统一的体系结构，用于表示和操作集合，使它们能够独立于其实现的细节进行操作。它减少了编程工作量，同时提高了性能。
它支持`api`无关的互操作性，减少设计和学习新`api`的工作量，并促进软件重用。该框架基于十几个集合接口。
它包括这些接口的实现和操作它们的算法。

简而言之，集合可以看作是一种容器，用来存储对象信息。大部分集合类都位于`java.util`包下，
但支持多线程的集合类位于`java.util.concurrent`包下。

# 2. 框架结构

## 2.1 概览

`Java`集合类主要由两个根接口`Collection`和`Map`派生而来。下图是框架简图。其中蓝色为接口，浅绿色为抽象类，深绿色为具体类。
虚线箭头表示实现关系，实线箭头表示继承关系：

![Collection 继承树][collection]

![Map 继承树][map]

可以看到，`Collection`派生出了三个子接口：`List`、`Set`、`Queue`（`Java5`新增的队列），
因此`Java`集合大致也可分成`List`、`Set`、`Queue`、`Map`四种接口体系。
其中`List`代表了有序可重复集合，可直接根据元素的索引来访问；`Set`代表无序不可重复集合，只能根据元素本身来访问；
`Queue`是队列集合；`Map`代表的是存储`key-value`对的集合，可根据元素的`key`来访问`value`。其中`key`是不可重复的，
`value`是可重复的，每个键最多映射到一个值。

## 2.2 组成

`Java`集合框架主要包含以下部分：
 - 集合接口。表示不同类型的集合，例如`List`、`Set`、`Queue`、`Map`。这些接口构成了框架的基础。
 - 抽象实现。集合接口的部分实现，以方便自定义实现，例如`AbstractList`、`AbstractMap`。
 - 通用实现。集合接口的主要实现，例如`ArrayList`、`HashMap`。
 - 遗留实现。对早期版本中的集合类`Vector`和`Hashtable`进行了改造，以实现集合接口。
 - 特殊用途的实现。为在特殊情况下使用而设计的实现。这些实现显示非标准的性能特征、使用限制或行为，如`WeakMap`。
 - 并行实现。为高度并发使用而设计的实现，如`ConcurrentHashMap`。
 - 包装器实现。向其他实现添加功能，如同步：`Collections.SynchronizedList`。
 - 便利实现。集合接口的高性能“迷你实现”。
 - 算法。对集合执行有用功能的静态方法，例如对列表排序。
 - 基础设施。为集合接口提供基本支持，如`Collections`。
 - 数组实用程序。基本类型和引用对象数组的工具类`Arrays`。严格地说，这个特性并不是集合框架的一部分，
 而是与集合框架同时添加到`Java`平台中的，并且依赖于一些相同的基础设施。
 
## 2.3 实现细节

### 2.3.1 可选操作

集合接口中的许多修改方法都标记为可选。允许实现不实现这些方法中的一个或多个，如果尝试执行这些方法，
则会引发运行时异常`UnsupportedOperationException`。每个实现的文档必须指定支持哪些可选操作。在本规范中引入了几个术语：
 - 不支持修改操作（如添加、删除和清除）的集合称为**不可修改**。
 - 另外保证集合对象中的任何更改都不可见的集合称为**不可变**。
 - 即使元素可以更改也能保证其大小保持不变的列表称为**固定大小**。
 - 支持快速（通常是固定时间）索引元素访问的列表称为**随机访问列表**。不支持快速索引元素访问的列表称为**顺序访问列表**。
 `RandomAccess`标记接口表示列表支持随机访问。这使得通用算法在应用于随机或顺序访问列表时能够改变其行为以提供良好的性能。
 
### 2.3.2 对存储元素的限制
 
有些实现限制了可以存储的元素。包括以下可能需要限制的要素：
 - 属于某一类型。
 - 不为`null`。
 - 遵守一些条件（如`Comparable`）。

尝试添加违反实现限制的元素会导致运行时异常，通常是`ClassCastException`、`IllegalArgumentException`或`NullPointerException`。
尝试移除或测试是否存在违反实现限制的元素也可能会导致异常。

### 2.3.3 通用实现

实现集合接口的类通常以`<Implementation style><Interface>`的形式命名。下表总结了通用实现：

| Interface | Hash Table | Resizable Array | Balanced Tree | Linked List | Hash Table + Linked List |
| --------- | ---------- | --------------- | ------------- | ----------- | ------------------------ |
| Set | HashSet | | TreeSet | | LinkedHashSet |
| List | | ArrayList | | LinkedList | | |
| Deque | | ArrayDeque | | LinkedList | |
| Map | HashMap | | TreeMap | |LinkedHashMap |

通用实现支持集合接口中的所有可选操作，并且对它们可能包含的元素没有限制。它们是不同步的，
但`Collections`类包含称为同步包装器的静态工厂，可用于向许多不同步的集合添加同步。所有新的实现都有`fail-fast`迭代器，
它可以检测到无效的并发修改，并且能够快速地抛出异常。

`AbstractCollection`、`AbstractSet`、`AbstractList`、`AbstractSequentialList`和`AbstractMap`类提供核心集合接口的基本实现，
以最大程度地减少实现它们所需的工作量。这些类的`API`文档精确地描述了每个方法是如何实现的，
以便实现者知道在给定特定实现的基本操作性能的情况下，哪些方法必须被重写。


[collection]: ../../../res/img/jcf-collection.png
[map]: ../../../res/img/jcf-map.png