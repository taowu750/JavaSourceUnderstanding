`java.util.ComparableTimSort`的声明如下：
```java
class ComparableTimSort
```
`ComparableTimSort`和 [TimSort.md][tim-sort] 实现几乎一样，只是它对`Comparable`元素解析排序。
如果您使用的是优化的`VM`，则可能会发现`ComparableTimSort`与使用仅返回`((Comparable)first).compareTo(Second)`的比较器的`TimSort`相比，
性能没有提升。在这种情况下，最好删除`ComparableTimSort`以消除代码重复。（有关详细信息，请参见 [Arrays.md][arrays]。）


[tim-sort]: TimSort.md
[arrays]: Arrays.md