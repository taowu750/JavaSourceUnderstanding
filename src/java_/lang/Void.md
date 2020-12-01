`java.lang.Void`的代码如下：
```java
public final class Void {

    public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");

    /*
     * The Void class cannot be instantiated.
     */
    private Void() {}
}
```
它的代码定义就和它的表示含义一样：空。