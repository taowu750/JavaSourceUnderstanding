package java_.util.concurrent.atomic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 用于对 {@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater} 类进行测试。
 */
public class AtomicIntegerFieldUpdaterTest {

    AtomicIntegerFieldUpdater<Son> newUpdater(String name) {
        return AtomicIntegerFieldUpdater.newUpdater(Son.class, name);
    }

    /**
     * 测试可访问性
     */
    @Test
    public void testAccess() {
        Son son = new Son();

        // 不能访问父类的任何字段
        Assertions.assertThrows(RuntimeException.class,
                () -> System.out.println("Father.publicField = " + newUpdater("fatherPublicField")));
        Assertions.assertThrows(RuntimeException.class,
                () -> System.out.println("Father.publicField = " + newUpdater("fatherProtectedField")));

        // 不能访问非 volatile 字段
        Assertions.assertThrows(RuntimeException.class,
                () -> System.out.println("noVolatileField = " + newUpdater("noVolatileField")));

        // 可以不受限制地访问 public 字段
        Assertions.assertEquals(newUpdater("publicField").getAndIncrement(son), 4);
        // 调用者和 Son 在同一个包下，所以可以访问 protected、package 字段
        Assertions.assertEquals(newUpdater("protectedField").getAndIncrement(son), 5);
        Assertions.assertEquals(newUpdater("packageField").getAndIncrement(son), 6);

        // 不能访问 private 字段
        Assertions.assertThrows(RuntimeException.class,
                () -> System.out.println("noVolatileField = " + newUpdater("privateField")));

        // 不能访问 static 字段
        Assertions.assertThrows(RuntimeException.class,
                () -> System.out.println("noVolatileField = " + newUpdater("staticField")));

        // 自己可以访问自己的 private 字段
        // 输出 7
        son.testPrivate();
    }
}

class Father {
    public volatile int fatherPublicField = 1;
    protected volatile int fatherProtectedField = 2;
}

class Son extends Father {

    public int noVolatileField = 3;
    public volatile int publicField = 4;
    protected volatile int protectedField = 5;
    volatile int packageField = 6;
    private volatile int privateField = 7;

    public volatile static int staticField = 8;

    public void testPrivate() {
        Son son = new Son();
        System.out.println(AtomicIntegerFieldUpdater.newUpdater(Son.class, "privateField")
                .getAndIncrement(son));
    }
}