package ujava.lang;

import org.junit.jupiter.api.Test;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Integer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegerTest {

    static IntConsumer print = i -> System.out.println(String.format("%32s", Integer.toBinaryString(i)));

    /**
     * 测试{@link Integer}.{@code getChars}方法中的魔法数字 65536, 52429
     */
    @SuppressWarnings("NumericOverflow")
    @Test
    public void testGetCharsMagicNumber() {
        int i = 65536 * 52429;
        // Integer.toHexString 生成的是无符号形式
        System.out.println(i + " " + toHexString(i) + " " + toUnsignedString(i));

        long j = 65536L * 52429L;  // 注意一定得有一个数字是 "L" long 类型，不然就是两个 int 相乘再转型成 long
        System.out.println(j + " " + Long.toHexString(j));
    }

    /**
     * 测试{@link Integer#compareUnsigned(int, int)}方法
     */
    @Test
    public void testCompareUnsigned() {
        IntFunction<String> toBiStr = i -> toBinaryString(i) + " ";
        int x = 1, y = 2;
        System.out.println(toBiStr.apply(x) + toBiStr.apply(y));
        System.out.println(toBiStr.apply(x + MIN_VALUE) + toBiStr.apply(x + MIN_VALUE));
        System.out.println(compareUnsigned(x, y));
        System.out.println(compare(x + MIN_VALUE, y + MIN_VALUE));

        System.out.println(IntStream.range(0, 20).mapToObj(i -> "*").collect(Collectors.joining()));

        x = -1;
        y = -2;
        System.out.println(toBiStr.apply(x) + toBiStr.apply(y));
        System.out.println(toBiStr.apply(x + MIN_VALUE) + toBiStr.apply(y + MIN_VALUE));
        System.out.println(x + MIN_VALUE + " " + (y + MIN_VALUE));
        System.out.println(compareUnsigned(x, y));
    }

    /**
     * 测试{@link Integer#highestOneBit(int)}方法
     */
    @Test
    public void testHighestOneBit() {
        assertEquals(highestOneBit(0), 0);
        assertEquals(highestOneBit(1), 1);
        assertEquals(highestOneBit(9), 1 << 3);
        assertEquals(highestOneBit(MAX_VALUE), 1 << 30);
        assertEquals(highestOneBit(MIN_VALUE), MIN_VALUE);
    }

    /**
     * 测试{@link Integer#lowestOneBit(int)} 方法
     */
    @Test
    public void testLowestOneBit() {
        print.accept(3);
        print.accept(-3);
        print.accept(Integer.lowestOneBit(3));
        print.accept(Integer.lowestOneBit(-3));
        System.out.println();

        print.accept(14);
        print.accept(-14);
        print.accept(Integer.lowestOneBit(14));
        print.accept(Integer.lowestOneBit(-14));
    }

    @Test
    public void testSignum() {
        print.accept(1 >> 31);
        print.accept(749 >> 31);
        print.accept(MAX_VALUE >> 31);
        System.out.println();

        print.accept(-1 >> 31);
        print.accept(-749 >> 31);
        print.accept(-MAX_VALUE >> 31);
        print.accept(MIN_VALUE >> 31);
        System.out.println();

        print.accept(-MIN_VALUE >>> 31);
    }
}
