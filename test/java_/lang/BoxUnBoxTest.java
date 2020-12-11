package java_.lang;

import org.junit.jupiter.api.Test;

/**
 * 自动装箱拆箱的测试。
 */
public class BoxUnBoxTest {

    /**
     * 测试常量池和自动装箱拆箱
     */
    @SuppressWarnings("NumberEquality")
    @Test
    public void testCache() {
        Integer i1 = 33;
        Integer i2 = 33;
        System.out.println(i1 == i2);
        Integer i11 = 333;
        Integer i22 = 333;
        System.out.println(i11 == i22);

        /*
        输出：
        true
        false
         */
    }

    /**
     * 测试构造器和字面量的不同
     */
    @SuppressWarnings({"UnnecessaryBoxing", "NewObjectEquality", "NumberEquality"})
    @Test
    public void testConstructorLiteral() {
        Integer i1 = 40;
        Integer i2 = new Integer(40);
        System.out.println(i1 == i2);

        /*
        输出：
        false
         */
    }

    /**
     * 测试等式和表达式在装箱拆箱中的影响
     */
    @SuppressWarnings({"NumberEquality", "ConstantConditions", "EqualsBetweenInconvertibleTypes"})
    @Test
    public void testEqualExpr() {
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;
        Long h = 2L;

        System.out.println(c == d);
        System.out.println(e == f);
        System.out.println(c == (a + b));
        System.out.println(c.equals(a + b));
        System.out.println(g == (a + b));
        System.out.println(g.equals(a + b));
        System.out.println(g.equals(a + h));

        /*
        输出：
        true
        false
        true
        true
        true
        false
        true
         */
    }

    void wrapperAndObject(int i) {
        System.out.println("wrapperAndObject(int) 方法调用");
    }

    void wrapperAndObject(Object o) {
        System.out.println("wrapperAndObject(Object) 方法调用");
    }

    void wrapperAndPrimitive(int i) {
        System.out.println("wrapperAndPrimitive(int) 方法调用");
    }

    void wrapperAndPrimitive(Integer i) {
        System.out.println("wrapperAndPrimitive(Integer) 方法调用");
    }

    /**
     * 测试参数重载和装箱。
     */
    @Test
    public void testWrapperAndObject() {
        wrapperAndObject(3);
        wrapperAndObject(Integer.valueOf(3));
        wrapperAndPrimitive(3);
        wrapperAndPrimitive(Integer.valueOf(3));

        /*
        输出：
        wrapperAndObject(int) 方法调用
        wrapperAndObject(Object) 方法调用
        wrapperAndPrimitive(int) 方法调用
        wrapperAndPrimitive(Integer) 方法调用
         */
    }
}
