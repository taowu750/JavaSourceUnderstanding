# 1. 概览

![JUC 概览][overview]

主要包含: 
 - `Atomic`: 原子类
 - `Lock` 框架和 `Tools` 类(把图中这两个放到一起理解)
 - `Collections`: 并发集合
 - `Executors`: 线程池

# 2. Atomic

 - 基础类型：AtomicBoolean，AtomicInteger，AtomicLong
 - 数组：AtomicIntegerArray，AtomicLongArray，BooleanArray
 - 引用：AtomicReference，AtomicMarkedReference，AtomicStampedReference
 - FieldUpdater：AtomicLongFieldUpdater，AtomicIntegerFieldUpdater，AtomicReferenceFieldUpdater

# 3. Lock 框架和 Tools 类

![Lock 和 Tools][lock-tools]

# 4. Collections

![Collections][collections]

# 5. Executors

![Executors][executors]


[overview]: ../../../../res/img/juc-overview.png
[lock-tools]: ../../../../res/img/juc-lock-tools.png
[collections]: ../../../../res/img/juc-collections.png
[executors]: ../../../../res/img/juc-executors.png