package ujava.lang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.DoubleConsumer;

/**
 * 浮点数相关测试和验证。
 */
public class FloatingNumberTest {

    static DoubleConsumer fP = f -> {
        char[] binaryChars = new char[32];
        Arrays.fill(binaryChars, '0');
        String binaryString = Integer.toBinaryString(Float.floatToRawIntBits((float) f));
        binaryString.getChars(0, binaryString.length(), binaryChars, 32 - binaryString.length());

        for (int i = 0; i < binaryChars.length; i++) {
            System.out.print(binaryChars[i]);
            if (i == 0 || i == 8)
                System.out.print(' ');
        }
        System.out.println();
    };
    static DoubleConsumer dP = d -> {
        char[] binaryChars = new char[64];
        Arrays.fill(binaryChars, '0');
        String binaryString = Long.toBinaryString(Double.doubleToRawLongBits(d));
        binaryString.getChars(0, binaryString.length(), binaryChars, 64 - binaryString.length());

        for (int i = 0; i < binaryChars.length; i++) {
            System.out.print(binaryChars[i]);
            if (i == 0 || i == 11)
                System.out.print(' ');
        }
        System.out.println();
    };;

    /**
     * 测试{@code float}的二进制表示
     */
    @Test
    public void testFloatBinary() {
        fP.accept(22.8125f);
        fP.accept(-22.8125f);
        fP.accept(0.4f);
    }

    /**
     * 验证{@code float}的特殊值和极值的二进制表示
     */
    @Test
    public void testSpecialFloatBinary() {
        // 以下最大值与最小值指绝对值大小。
        // 规格化阶码不能为 00000000 和 11111111
        // 规格化偏移值为 127，非规格化偏移值为 126。它们的二进制指数范围均在 [-126, 127] 范围内

        // -Infinity
        fP.accept(Float.NEGATIVE_INFINITY);
        // -规格化最大值
        fP.accept(((float) Math.pow(2, -23) - 2.f) * (float) Math.pow(2, 127));
        // -规格化最小值
        fP.accept((float) Math.pow(-2, -126));
        // -非规格化最大值
        fP.accept(((float) Math.pow(2, -23) - 1.f) * (float) Math.pow(2, -126));
        // -非规格化最小值
        fP.accept(-(float) Math.pow(2, -23) * (float) Math.pow(2, -126));
        // -0
        fP.accept(-0.f);
        // +0
        fP.accept(+0.f);
        // +非规格化最小值
        fP.accept((float) Math.pow(2, -23) * (float) Math.pow(2, -126));
        // +非规格化最大值
        fP.accept((1.f - (float) Math.pow(2, -23)) * (float) Math.pow(2, -126));
        // +规格化最小值
        fP.accept((float) Math.pow(2, -126));
        // +规格化最大值
        fP.accept((2.f - (float) Math.pow(2, -23)) * (float) Math.pow(2, 127));
        // +Infinite
        fP.accept(Float.POSITIVE_INFINITY);
        // NaN
        fP.accept(Float.NaN);
    }

    /**
     * 测试浮点数的舍入模式
     */
    @Test
    public void testRoundMode() {
        System.out.println("舍入前:         10.10011111111111111111101");
        System.out.print("舍入后:");
        fP.accept(2.62499964237213134765625f);
        System.out.println();

        System.out.println("舍入前:         10.10011111111111111111111");
        System.out.print("舍入后:");
        fP.accept(2.62499988079071044921875f);
        System.out.println();

        System.out.println("舍入前:         10.10011111111111111111101011");
        System.out.print("舍入后:");
        fP.accept(2.62499968707561492919921875f);
        System.out.println();

        System.out.println("舍入前:         10.10011111111111111111100011");
        System.out.print("舍入后:");
        fP.accept(2.62499956786632537841796875f);
        System.out.println();

        System.out.println("舍入前:        -10.10011111111111111111101");
        System.out.print("舍入后:");
        fP.accept(-2.62499964237213134765625f);
        System.out.println();

        System.out.println("舍入前:        -10.10011111111111111111111");
        System.out.print("舍入后:");
        fP.accept(-2.62499988079071044921875f);
        System.out.println();

        System.out.println("舍入前:        -10.10011111111111111111101011");
        System.out.print("舍入后:");
        fP.accept(-2.62499968707561492919921875f);
        System.out.println();

        System.out.println("舍入前:        -10.10011111111111111111100011");
        System.out.print("舍入后:");
        fP.accept(-2.62499956786632537841796875f);
        System.out.println();
    }

    /**
     * 测试{@code double}加法
     */
    @Test
    public void testDoubleAdd() {
        dP.accept(0.1);
        dP.accept(0.2);
        dP.accept(0.1 + 0.2);
        System.out.println(0.1 + 0.2);
    }

    /**
     * 测试{@code float}负数加法
     */
    @Test
    public void testFloatReduce() {
        float i1 = 0.1f, i2 = -0.2f;
        fP.accept(i1);
        fP.accept(i2);
        fP.accept(i1 + i2);
        System.out.println(i1 + i2);
    }

    /**
     * 测试{@code double}误差
     */
    @Test
    public void testDoubleError() {
        dP.accept(1.4 - 1.1);
        System.out.println(1.4 - 1.1);
        System.out.println();

        dP.accept(1e+16);
        dP.accept(4.0);
        dP.accept(4.0 + 1e+16);
        dP.accept(4.0 + 1e+16 - 1e+16);
        System.out.println(4.0 + 1e+16 - 1e+16);
        System.out.println();

        dP.accept(5.0);
        dP.accept(5.0 + 1e+16);
        dP.accept(5.0 + 1e+16 - 1e+16);
        System.out.println(5.0 + 1e+16 - 1e+16);
        System.out.println();

        dP.accept(4.0 + 1e+17 - 1e+17);
        System.out.println(4.0 + 1e+17 - 1e+17);
    }
}
