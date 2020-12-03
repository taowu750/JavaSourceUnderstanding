package java_.lang;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static java.lang.Float.compare;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 对{@link Float}进行测试和验证
 */
public class FloatTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testSpecialValueWithDouble() {
        //noinspection ComparisonToNaN
        assertTrue(Float.NaN != Double.NaN);
        // 下面之所以成立，是因为 Float.POSITIVE_INFINITY 会先转型为 double 值，
        // 而它转型之后就是 Double.POSITIVE_INFINITY
        assertEquals(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        assertFalse(Float.POSITIVE_INFINITY < Double.POSITIVE_INFINITY);
    }

    @Test
    public void testCompare() {
        // 由于精度问题，1.4f 被表示为 1.3999_9997，
        // 1.1f 被表示为  1.1000_0002，0.3f 被表示为 3.0000_001e-1。
        // 所以会有 1.4f < 1.1f + 0.3f
        assertNotEquals(1.4f, 1.1f + 0.3f, 0.0f);
        assertEquals(compare(1.4f, 1.1f + 0.3f), -1);

        // 使用阈值进行比较
        double threshold = 1e-6;
        assertTrue(Math.abs(1.4f - (1.1f + 0.3f)) < threshold);

        // 使用 BigDecimal 进行比较
        assertEquals(new BigDecimal("1.4").compareTo(new BigDecimal("1.1").add(new BigDecimal("0.3"))),
                0);
    }
}
