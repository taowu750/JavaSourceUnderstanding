package java_.lang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;
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
    };

    /**
     * 测试{@code float}的二进制表示
     */
    @Test
    public void testFloatBinary() {
        fP.accept(22.8125f);
        fP.accept(-22.8125f);
        fP.accept(0.4f);

        /*
        输出：
        0 10000011 01101101000000000000000
        1 10000011 01101101000000000000000
        0 01111101 10011001100110011001101
         */
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

        /*
        输出：
        1 11111111 00000000000000000000000
        1 11111110 11111111111111111111111
        0 00000001 00000000000000000000000
        1 00000000 11111111111111111111111
        1 00000000 00000000000000000000001
        1 00000000 00000000000000000000000
        0 00000000 00000000000000000000000
        0 00000000 00000000000000000000001
        0 00000000 11111111111111111111111
        0 00000001 00000000000000000000000
        0 11111110 11111111111111111111111
        0 11111111 00000000000000000000000
        0 11111111 10000000000000000000000
         */
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

        /*
        输出：
        舍入前:         10.10011111111111111111101
        舍入后:0 10000000 01001111111111111111110

        舍入前:         10.10011111111111111111111
        舍入后:0 10000000 01010000000000000000000

        舍入前:         10.10011111111111111111101011
        舍入后:0 10000000 01001111111111111111111

        舍入前:         10.10011111111111111111100011
        舍入后:0 10000000 01001111111111111111110

        舍入前:        -10.10011111111111111111101
        舍入后:1 10000000 01001111111111111111110

        舍入前:        -10.10011111111111111111111
        舍入后:1 10000000 01010000000000000000000

        舍入前:        -10.10011111111111111111101011
        舍入后:1 10000000 01001111111111111111111

        舍入前:        -10.10011111111111111111100011
        舍入后:1 10000000 01001111111111111111110
         */
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

        /*
        输出：
        0 01111111011 1001100110011001100110011001100110011001100110011010
        0 01111111100 1001100110011001100110011001100110011001100110011010
        0 01111111101 0011001100110011001100110011001100110011001100110100
        0.30000000000000004
         */
    }

    /**
     * 测试浮点数十进制精度。
     */
    @Test
    public void testAccuracy() {
        // double 16 位部分精确
        System.out.println(1. + 1e15 - 1e15);
        System.out.println(3. + 1e15 - 1e15);
        System.out.println(1. + 1e16 - 1e16);
        System.out.println(3. + 1e16 - 1e16);
        System.out.println(1.01 + 1e16 - 1e16);
        System.out.println();

        // float 8 位部分精确
        System.out.println(1.f + 1e7f - 1e7f);
        System.out.println(3.f + 1e7f - 1e7f);
        System.out.println(1.4f + 1e7f - 1e7f);
        System.out.println(1.5f + 1e7f - 1e7f);
        System.out.println(1f + 1e8f - 1e8f);
        System.out.println(4f + 1e8f - 1e8f);
        System.out.println(5f + 1e8f - 1e8f);

        // 需要注意的是，浮点数转字符串时会自动进行舍入，因此浮点数的字符串表示会增大误差

        /*
        输出：
        1.0
        3.0
        0.0
        4.0
        2.0

        1.0
        3.0
        1.0
        2.0
        0.0
        0.0
        8.0
         */
    }

    /**
     * 测试{@code float}的最小精度间隔。
     */
    @Test
    public void testFloatEps() {
        float bin1 = 0x1.0P-23f, bin2 = 0x1.0P-24f;
        float dec1 = 0.0000_00059603f, dec2 = 0.0000_00059605f;
        System.out.println(bin1);
        System.out.println(bin2);
        System.out.println(dec1);
        System.out.println(dec2);
        System.out.println();

        /*
        以上输出：
        1.1920929E-7
        5.9604645E-8
        5.9603E-8
        5.6605E-8
         */

        // 1 在二进制中只需要一位表示，因此它作为高位和最小精度间隔相加可以被 24 位有效数字（二进制）表示
        float value = 1.f;
        Consumer<Float> print = v -> {
            System.out.print(Float.compare(v, v + bin1) + " ");
            System.out.print(Float.compare(v, v + bin2) + "    ");
            System.out.print(Float.compare(v, v + dec1) + " ");
            System.out.println(Float.compare(v, v + dec2) + "\n");
        };
        print.accept(value);

        // 2 在二进制中需要两位表示，因此它作为高位和最小精度间隔相加，最小精度间隔会被舍入丢弃，造成误差
        value = 2.f;
        print.accept(value);
        // 1.1921E-7f 比 2^-23 次方稍大一些，可以看到它和 2 相加没有被舍入
        System.out.println(Float.compare(value, value + 1.1921E-7f) + "\n");

        // 0.1 作为高位和最小精度间隔相加可以被 24 位有效数字表示
        value = 0.1f;
        print.accept(value);

        // 0.30005 作为高位和最小精度间隔相加可以被 24 位有效数字表示
        value = 0.30005f;
        print.accept(value);

        /*
        以上输出：
        -1 0    0 -1

        0 0    0 0

        -1

        -1 -1    -1 -1

        -1 -1    -1 -1
         */

        /*
        从结果中可以看出，两个 float 的最小精度间隔应该比 2^-24 次方稍大一点点。
        只要 float 运算结果可以被 24 位有效数字（二进制）表示，较小的数就不会被舍入丢弃。
         */
    }

    /**
     * 测试{@code double}的最小精度间隔。
     */
    @Test
    public void testDoubleEps() {
        double bin1 = 0x1.0P-52, bin2 = 0x1.0P-53;
        double dec1 = 0.0000_0000_0000_000111, dec2 = 0.0000_0000_0000_0001111;
        System.out.println(bin1);
        System.out.println(bin2);
        System.out.println(dec1);
        System.out.println(dec2);
        System.out.println();

        /*
        以上输出：
        2.220446049250313E-16
        1.1102230246251565E-16
        1.11E-16
        1.111E-16
         */

        // 1 在二进制中只需要一位表示，因此它作为高位和最小精度间隔相加可以被 53 位有效数字（二进制）表示
        double value = 1.;
        DoubleConsumer print = v -> {
            System.out.print(Double.compare(v, v + bin1) + " ");
            System.out.print(Double.compare(v, v + bin2) + "    ");
            System.out.print(Double.compare(v, v + dec1) + " ");
            System.out.println(Double.compare(v, v + dec2) + "\n");
        };
        print.accept(value);

        // 2 在二进制中需要两位表示，因此它作为高位和最小精度间隔相加，最小精度间隔会被舍入丢弃，造成误差
        value = 2.;
        print.accept(value);
        // 2.221E-16 比 2^-52 次方稍大一些，可以看到它和 2 相加没有被舍入
        System.out.println(Double.compare(2.0, 2.0 + 2.221E-16) + "\n");

        // 0.1 作为高位和最小精度间隔相加可以被 53 位有效数字表示
        value = 0.1;
        print.accept(value);

        // 0.30005 作为高位和最小精度间隔相加可以被 53 位有效数字表示
        value = 0.30005;
        print.accept(value);

        /*
        以上输出：
        -1 0    0 -1

        0 0    0 0

        -1

        -1 -1    -1 -1

        -1 -1    -1 -1
         */

        /*
        从结果中可以看出，两个 double 的最小精度间隔应该比 2^-53 次方稍大一点点。
        只要 double 运算结果可以被 53 位有效数字（二进制）表示，较小的数就不会被舍入丢弃。
         */
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

        /*
        输出：
        0 01111111101 0011001100110011001100110011001100110011001100110000
        0.2999999999999998

        0 10000110100 0001110000110111100100110111111000001000000000000000
        0 10000000001 0000000000000000000000000000000000000000000000000000
        0 10000110100 0001110000110111100100110111111000001000000000000010
        0 10000000001 0000000000000000000000000000000000000000000000000000
        4.0

        0 10000000001 0100000000000000000000000000000000000000000000000000
        0 10000110100 0001110000110111100100110111111000001000000000000010
        0 10000000001 0000000000000000000000000000000000000000000000000000
        4.0

        0 00000000000 0000000000000000000000000000000000000000000000000000
        0.0
         */
    }
}
