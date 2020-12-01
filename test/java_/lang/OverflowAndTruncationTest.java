package java_.lang;

import org.junit.jupiter.api.Test;

/**
 * 溢出和截断测试
 */
public class OverflowAndTruncationTest {

    /**
     * 测试最大、最小值的溢出
     */
    @SuppressWarnings("NumericOverflow")
    @Test
    public void testOverflow() {
        int[] i = {Integer.MIN_VALUE - 1};
        Runnable print = () -> System.out.println(i[0] + " " + Integer.toHexString(i[0]));
        print.run();

        i[0] = Integer.MIN_VALUE - 2;
        print.run();

        i[0] = Integer.MAX_VALUE + 1;
        print.run();

        i[0] = Integer.MAX_VALUE + 2;
        print.run();

        /*
        输出：
        2147483647 7fffffff
        2147483646 7ffffffe
        -2147483648 80000000
        -2147483647 80000001
         */
    }
}
