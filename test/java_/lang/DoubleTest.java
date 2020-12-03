package java_.lang;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static java.lang.Double.*;
import static java.lang.Math.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 关于双精度浮点数 double 的测试和验证
 */
public class DoubleTest {

    /**
     * 测试能产生{@link Double#NaN}的运算
     */
    @SuppressWarnings("divzero")
    @Test
    public void testProduceNaN() {
        // 和 NaN 进行运算
        assertTrue(isNaN(NaN + 100));
        assertTrue(isNaN(NaN - 47));
        assertTrue(isNaN(NaN * 20));
        assertTrue(isNaN(NaN / 31));
        assertTrue(isNaN(NaN + NaN));
        // 0 除
        assertTrue(isNaN(0. / 0.));
        assertTrue(isNaN(0. / -0.));
        // 无穷除无穷
        assertTrue(isNaN(POSITIVE_INFINITY / POSITIVE_INFINITY));
        assertTrue(isNaN(POSITIVE_INFINITY / NEGATIVE_INFINITY));
        assertTrue(isNaN(NEGATIVE_INFINITY / POSITIVE_INFINITY));
        assertTrue(isNaN(NEGATIVE_INFINITY / NEGATIVE_INFINITY));
        // 0 乘无穷
        assertTrue(isNaN(0 * POSITIVE_INFINITY));
        assertTrue(isNaN(0 * NEGATIVE_INFINITY));
        // 无穷加减
        assertTrue(isNaN(POSITIVE_INFINITY + NEGATIVE_INFINITY));
        assertTrue(isNaN(POSITIVE_INFINITY - POSITIVE_INFINITY));
        assertTrue(isNaN(NEGATIVE_INFINITY - NEGATIVE_INFINITY));
        // 产生复数结果的实数运算
        assertTrue(isNaN(sqrt(-1)));
        assertTrue(isNaN(log(-1)));
        assertTrue(isNaN(acos(2)));
    }

    /**
     * 测试{@link Double#NaN}的比较结果
     */
    @SuppressWarnings({"ConstantConditions", "ComparisonToNaN"})
    @Test
    public void testNaNCompare() {
        assertTrue(NaN != NaN);
        assertFalse(-10. < NaN);
        assertFalse(-10. > NaN);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testInfiniteOperation() {
        // 无穷加减
        assertEquals(POSITIVE_INFINITY + POSITIVE_INFINITY, POSITIVE_INFINITY);
        assertEquals(POSITIVE_INFINITY - NEGATIVE_INFINITY, POSITIVE_INFINITY);
        assertEquals(NEGATIVE_INFINITY + NEGATIVE_INFINITY, NEGATIVE_INFINITY);
        assertEquals(NEGATIVE_INFINITY - POSITIVE_INFINITY, NEGATIVE_INFINITY);
        assertEquals(POSITIVE_INFINITY + 100, POSITIVE_INFINITY);
        assertEquals(POSITIVE_INFINITY - 100, POSITIVE_INFINITY);
        assertEquals(NEGATIVE_INFINITY + 100, NEGATIVE_INFINITY);
        assertEquals(NEGATIVE_INFINITY - 100, NEGATIVE_INFINITY);
        // 无穷相乘
        assertEquals(POSITIVE_INFINITY * POSITIVE_INFINITY, POSITIVE_INFINITY);
        assertEquals(POSITIVE_INFINITY * NEGATIVE_INFINITY, NEGATIVE_INFINITY);
        assertEquals(NEGATIVE_INFINITY * NEGATIVE_INFINITY, POSITIVE_INFINITY);
        // 无穷数学运算
        assertEquals(sqrt(POSITIVE_INFINITY), POSITIVE_INFINITY);
        assertEquals(log(POSITIVE_INFINITY), POSITIVE_INFINITY);
        // 无穷比较
        assertTrue(NEGATIVE_INFINITY < POSITIVE_INFINITY);
        assertTrue(NEGATIVE_INFINITY < 100);
        assertTrue(NEGATIVE_INFINITY < -1e10);
        assertTrue(POSITIVE_INFINITY > 100);
        assertTrue(POSITIVE_INFINITY > 1e10);
    }

    @SuppressWarnings("UnnecessaryCallToStringValueOf")
    @Test
    public void testToString() {
        System.out.println(Double.toString(0.));
        System.out.println(Double.toString(1.));
        System.out.println(Double.toString(1. + pow(10, -3)));
        System.out.println(Double.toString(pow(10, 7) - 0.1));
        System.out.println(Double.toString(-47.));
        System.out.println(Double.toString(pow(10, -4)));
        System.out.println(Double.toString(pow(10, 8)));

        /*
        输出：
        0.0
        1.0
        1.001
        9999999.9
        -47.0
        1.0E-4
        1.0E8
         */
    }

    @Test
    public void testToHexString() {
        System.out.println(Double.toHexString(0.));
        System.out.println(Double.toHexString(1.));
        System.out.println(Double.toHexString(100.47));
        System.out.println(Double.toHexString(-100.47));
        System.out.println(Double.toHexString(MAX_VALUE - 100));
        System.out.println(Double.toHexString(MIN_NORMAL / 2.));

        /*
        输出：
        0x0.0p0
        0x1.0p0
        0x1.91e147ae147aep6
        -0x1.91e147ae147aep6
        0x1.fffffffffffffp1023
        0x0.8p-1022
         */
    }

    @Test
    public void testParseDouble() {
        System.out.println(parseDouble("3"));
        System.out.println(parseDouble("3.1"));
        System.out.println(parseDouble("0.9"));
        System.out.println(parseDouble("-1.001"));
        System.out.println(parseDouble("1e-3"));
        System.out.println(parseDouble("3.235e7"));
        System.out.println(parseDouble("0x1.fp10"));

        /*
        输出：
        3.0
        3.1
        0.9
        -1.001
        0.001
        3.235E7
        1984.0
         */
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testCompare() {
        // 由于精度问题，0.1 被表示为 1.0000_0001_4901_1611e-1，
        // 0.2 被表示为 2.0000_0000_0000_0001e-1，0.3 被表示为 2.9999_9999_9999_9998e-1。
        // 所以会有 0.3 < 0.1 + 0.2
        assertNotEquals(0.3, 0.1 + 0.2, 0.0);
        assertEquals(compare(0.3, 0.1 + 0.2), -1);

        // 使用阈值进行比较
        double threshold = 1e-15;
        assertTrue(Math.abs(0.1 + 0.2 - 0.3) < threshold);

        // 使用 BigDecimal 进行比较
        assertEquals(new BigDecimal("0.3").compareTo(new BigDecimal("0.1").add(new BigDecimal("0.2"))),
                0);
    }
}
