package java_.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 对 "overflow-conscious code" 进行测试和验证，以{@link java.util.ArrayList}为例。
 */
public class OverflowConsciousCodeTest {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    int newCap(int oldCapacity) {
        return oldCapacity + (oldCapacity >> 1);
    }

    void ensureExplicitCapacity(int minCapacity, int oldCapacity) {
        // overflow-conscious code
        if (minCapacity - oldCapacity > 0)
            grow(minCapacity, oldCapacity);
        else
            System.out.println("grow 未被调用");
    }

    void grow(int minCapacity, int oldCapacity) {
        // overflow-conscious code
        int newCapacity = newCap(oldCapacity);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);

        if (newCapacity < 0)
            throw new NegativeArraySizeException();
        System.out.println(newCapacity);
    }

    int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    /**
     * 测试 minCapacity 未溢出时的情况
     *
     * @throws Throwable
     */
    @Test
    public void testPositiveMinCapacity() throws Throwable {
        // 用 MAX 指代 MAX_ARRAY_SIZE 或 Integer.MAX_VALUE

        // 如果 newCapacity 未溢出，数组被扩容为 minCapacity 或 newCapacity 或 MAX
        int[] minCapacity = {Integer.MAX_VALUE - 10}, oldCapacity = {1431655750};
        Executable exec = () -> ensureExplicitCapacity(minCapacity[0], oldCapacity[0]);
        // 正常执行，输出 minCapacity[0] 2147483637
        exec.execute();

        // 如果 newCapacity 溢出，数组被扩容为 MAX
        oldCapacity[0] = 1431655766;
        // 正常执行，输出 MAX_ARRAY_SIZE 2147483639
        exec.execute();
    }

    /**
     * 测试 minCapacity 溢出时的情况
     *
     * @throws Throwable
     */
    @Test
    public void testNegativeMinCapacity() throws Throwable {
        // 只有当 minCapacity 小于 Integer.MIN_VALUE + oldCapacity 时，grow 才会被调用
        int[] oldCapacity = {1431655750}, minCapacity = {Integer.MIN_VALUE + oldCapacity[0]};
        Executable exec = () -> ensureExplicitCapacity(minCapacity[0], oldCapacity[0]);
        // 输出 “grow 未被调用”
        exec.execute();

        // grow 被调用，如果 newCapacity 未溢出，则一定会抛出 OutOfMemoryError 异常
        minCapacity[0] = Integer.MIN_VALUE + oldCapacity[0] - 1;
        assertThrows(OutOfMemoryError.class, exec);

        // grow 被调用，如果 newCapacity 溢出，newCapacity - minCapacity < 0，
        // 且 minCapacity - MAX_ARRAY_SIZE <= 0，则会抛出 NegativeArraySizeException 异常
        oldCapacity[0] = Integer.MAX_VALUE;
        minCapacity[0] = -3;
        assertThrows(NegativeArraySizeException.class, exec);
        // 否则，抛出 OutOfMemoryError 异常
        oldCapacity[0] = Integer.MAX_VALUE / 3 * 2 + 100;
        minCapacity[0] = Integer.MIN_VALUE + 100;
        assertThrows(OutOfMemoryError.class, exec);
    }
}
