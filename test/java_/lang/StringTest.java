package java_.lang;

import org.junit.jupiter.api.Test;

/**
 * 用于{@link String}类和全局字符串常量池的测试、验证。
 */
public class StringTest {

    /**
     * 测试字符串字面量和字符串对象的相等性
     */
    @SuppressWarnings({"StringEquality", "NewObjectEquality"})
    @Test
    public void testStringConstantEquality() {
        String s1 = "Hello";
        String s2 = "Hello";
        String s3 = "Hel" + "lo";
        String s4 = "Hel" + new String("lo");
        String s5 = new String("Hello");
        String s7 = "H";
        String s8 = "ello";
        String s9 = s7 + s8;

        System.out.println(s1 == s2);
        System.out.println(s1 == s3);
        System.out.println(s1 == s4);
        System.out.println(s1 == s5);
        System.out.println(s1 == s9);

        /*
        输出：
        true
        true
        false
        false
        false
         */
    }

    /**
     * 测试{@link String#intern()}方法。
     */
    @SuppressWarnings({"StringBufferReplaceableByString", "StringEquality"})
    @Test
    public void testIntern() {
        String s1 = new StringBuilder("实例").append("1").toString();
        String s2 = "实例1";
        System.out.println(s1 == s2);
        s1 = s1.intern();
        System.out.println(s1 == s2);

        String s3 = new StringBuilder("计算机").append("软件").toString();
        System.out.println(s3.intern() == s3);

        String s4 = new StringBuilder("ja").append("va").toString();
        System.out.println(s4.intern() == s4);

        String s5 = new String("xyz");
        System.out.println(s5.intern() == s5);

        /*
        输出：
        false
        true
        true
        false
        false
         */
    }
}
