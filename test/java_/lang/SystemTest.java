package java_.lang;

import org.junit.jupiter.api.Test;

/**
 * 由于{@link System}类的测试和验证。
 */
public class SystemTest {

    /**
     * 测试{@link System#getProperty(String)}能够访问的系统属性。
     */
    @Test
    public void testProps() {
        System.out.println(System.getProperty("java.version"));
        System.out.println(System.getProperty("java.vendor"));
        System.out.println(System.getProperty("java.vendor.url"));
        System.out.println(System.getProperty("java.home"));
        System.out.println(System.getProperty("java.class.version"));
        System.out.println(System.getProperty("java.class.path"));
        System.out.println(System.getProperty("os.name"));
        System.out.println(System.getProperty("os.arch"));
        System.out.println(System.getProperty("os.version"));
        System.out.println(System.getProperty("file.separator"));
        System.out.println(System.getProperty("path.separator"));
        System.out.println(System.getProperty("line.separator"));
        System.out.println(System.getProperty("user.name"));
        System.out.println(System.getProperty("user.home"));
        System.out.println(System.getProperty("user.dir"));

        /*
        输出：
        1.8.0_231
        Oracle Corporation
        http://java.oracle.com/
        C:\Program Files\Java\jdk1.8.0_231\jre
        52.0
        C:\Program Files\JetBrains\IntelliJ IDEA 2019.2\lib\idea_rt.jar;......C:\Program Files\Java\jdk1.8.0_231\jre\lib\rt.jar;D:\projects\idea\JavaSourceUnderstanding\out\test\JavaSourceUnderstanding;D:\projects\idea\JavaSourceUnderstanding\out\production\JavaSourceUnderstanding;......
        Windows 10
        amd64
        10.0
        \
        ;
        \r\n  // 显示为两个空行
        dell
        C:\Users\dell
        D:\projects\idea\JavaSourceUnderstanding
         */
    }

    /**
     * 测试{@link System#getSecurityManager()}
     */
    @Test
    public void testSecurityManager() {
        System.out.println(System.getSecurityManager());

        /*
        输出：
        null
         */
    }
}
