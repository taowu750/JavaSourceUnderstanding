package java_.lang;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 用于对{@link Runtime}类进行测试和验证
 */
public class RuntimeTest {

    /**
     * 测试{@link Runtime#exit(int)}和关闭钩子
     */
    @Test
    public void testExit() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("关闭钩子被调用")));

        Runtime.getRuntime().exit(0);

        /*
        输出：
        关闭钩子被调用
         */
    }

    /**
     * 测试{@link Runtime#halt(int)}和关闭钩子
     */
    @Test
    public void testHalt() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("关闭钩子被调用")));

        Runtime.getRuntime().halt(0);

        /*
        无任何输出
         */
    }

    /**
     * 测试{@link Runtime#availableProcessors()}、{@link Runtime#freeMemory()}、
     * {@link Runtime#totalMemory()}、{@link Runtime#maxMemory()}方法
     */
    @Test
    public void testProcessorsAndMemory() {
        System.out.println(Runtime.getRuntime().availableProcessors());
        System.out.println(Runtime.getRuntime().freeMemory());
        System.out.println(Runtime.getRuntime().totalMemory());
        System.out.println(Runtime.getRuntime().maxMemory());

        /*
        输出：
        6
        234019872  // 大概 200 多 MB
        255328256  // 大概 200 多 MB
        3776970752  // 大概 3 个 GB
         */
    }

    /**
     * 测试{@link Runtime#exec(String)}方法
     *
     * @throws IOException
     */
    @Test
    public void testExec() throws IOException {
        // 列举目录下的所有文件和子目录
        Process process = Runtime.getRuntime().exec("ls");

        try (InputStream pin = process.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(pin))) {
            br.lines().forEach(System.out::println);
        }

        /*
        输出：
        JavaSourceUnderstanding.iml
        LICENSE
        META-INF
        README.md
        out
        res
        src
        test
         */
    }
}
