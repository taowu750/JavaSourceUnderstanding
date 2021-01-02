# 1. 补码

`Java`中，整数是以补码表示的。补码所能表示的数的范围是有限的，一旦大于所能表示的最大正数，就会发生**上溢**，导致变成负数；
而如果小于所能表示的最小负数，就会发生**下溢**，导致变成正数。详细情况可看下图：

![补码数字环][circle]

在上面的环中，沿着顺时针的方向数字增加，逆时针的方向数字减少。可以看到，上溢过程和下溢过程发生在最大正数和最小负数之间，
补码表示的数看起来很像是圆环。

# 2. overflow conscious code

在`Java`很多数组扩容的代码中（比如`AbstractStringBuilder`、`ArrayList`、`ByteArrayOutputStream`等），
会有很多考虑了溢出而编写的代码，这些代码前会有注释：**"overflow-conscious code"**，说明下面这段代码是考虑了溢出的情况的。

下面是`ArrayList`中的扩容核心代码：
```java
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;

    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

// grow 方法仅被 ensureExplicitCapacity 调用
private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```

首先我们需要知道，传入`ensureExplicitCapacity`方法的参数`minCapacity`可以是用户指定的容量，
此时其他验证代码会保证`minCapacity`是正数。但是`minCapacity`也可以是`size + 1`（`add(E)`方法中），
或是`size + numNew`（`add(Collection)`方法中，`numNew`是参数集合的大小），
此时`minCapacity`可能会因为溢出变成负数。而`minCapacity`溢出的负数范围为`[Integer.MIN_VALUE, -2]`
（`Integer.MAX_VALUE + Integer.MAX_VALUE = -2`）。

那么`newCapacity`什么情况下会溢出呢？因为`newCapacity = oldCapacity + (oldCapacity >> 1) = oldCapacity * 1.5`，
所以当`oldCapacity`大于等于`ceil(Integer.MAX_VALUE * 2 / 3) = 1431655766`时，`newCapacity`会溢出为负数。
`newCapacity`溢出的负数范围为`[Integer.MIN_VALUE, -1073741826]`
（`Integer.MAX_VALUE + (Integer.MAX_VALUE >> 1) = -1073741826`）。

代码需要在`minCapacity`或`newCapacity`溢出时要么抛出异常，要么尽可能给出正确容量。

在接下来的分析中，另`o = oldCapacity`、`n = newCapacity`、`m = minCapacity`、`M = MAX_ARRAY_SIZE`、
`IM = Integer.MAX_VALUE`，简化我们的分析。

## 2.1 若 minCapacity 是正数

如果`m`是正数，只有当`m > o`时，`grow`方法才会被调用。

下面来分析此时`grow`方法内的调用情况：
1. 如果`n`未溢出。`n - m < 0`时，`n`被赋值为`m`，否则保持原值。也就是选择`n`、`m`中的较大值
    - 如果`n - M > 0`，`hugeCapacity(m)`返回`M`或`IM`（都大于等于`m`），数组扩容为这么大。
    因为`m`是我们期望的数组容量，因此即时扩容大小小于`n`也没关系。这也保证了数组大小最大的两个值为`M`或`IM`，
    不会有它们的中间值。
    - 如果`n - M <= 0`，数组扩容为`n`。
2. 如果`n`溢出了，意味着`o > IM * 2 / 3`。而`m > o`，那么`n - m`一定会下溢变成正数（可以画个环看一下）。
因此`n`保持原值，`n - M`也一定会大于 0，`hugeCapacity(m)`将会返回`M`或`IM`，使得数组正确扩容。

可以看出，`overflow conscious code`代码保证了`minCapacity`为正数时的正确扩容行为。

## 2.2 如果 minCapacity 是负数（溢出）

如果`m`因为溢出变成负数，那么只有当`m > IM + o`时，`grow`方法才会被调用。要搞懂这一点，看看下面的图例：

![oldCapacity][min-cap]

`o`一定是个正数（因为它是数组大小），所以`m - o`相当于在环上逆时针旋转`o`，只有当`m`处于阴影部分时，`m - o`相减才会大于 0。
并且由图可知`-m > abs(IM) - o`。

下面来分析此时`grow`方法内的调用情况：
1. 如果`n`未溢出，此时`o <= IM * 2 / 3`。有`-m > abs(IM) - o > abs(IM) - n`，所以此时`n + (-m)`一定会因为溢出小于 0。
于是`n`被赋值为`m`。因为`m > IM + o`，所以`m - M`一定会发生下溢从而变成正数，`hugeCapacity(m)`将会抛出`OutOfMemory`异常。
2. 如果`n`溢出，此时`o > IM * 2 / 3`，有`n < IM + o / 2`
    - 如果`n - m < 0`，`n`被赋值为`m`。若`m - M > 0`，`hugeCapacity(m)`将会抛出`OutOfMemory`异常；
    否则`Arrays.copyOf`将会抛出`NegativeArraySizeException`异常。
    - 如果`n - m >= 0`，`n`保持原值，而`n - M`一定会因为下溢而变成正数，`hugeCapacity(m)`将会抛出`OutOfMemory`异常。
    
可以看出，`overflow conscious code`代码保证了`minCapacity`为负数时一定会抛出异常。


[circle]: ../../../res/img/complement-overflow-circle.png
[min-cap]: ../../../res/img/complement-overflow-min-cap.png