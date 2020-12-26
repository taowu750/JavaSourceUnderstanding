package java_.lang;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * 用于{@link Process}和{@link ProcessBuilder}的测试和验证。
 */
public class ProcessTest {

    /**
     * 测试管道方式获取子进程输出
     *
     * @throws IOException
     */
    @Test
    public void testPipeIO() throws IOException {
        Process process = new ProcessBuilder("ls")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
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

    /**
     * 测试继承方式获取子进程输出
     *
     * @throws IOException
     */
    @Test
    public void testInheritIO() throws IOException {
        Process process = new ProcessBuilder("ls")
                .inheritIO()
                .start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
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

    /**
     * 测试将子进程输出写入到文件
     *
     * @throws IOException
     */
    @Test
    public void testFileIO() throws IOException {
        Process process = new ProcessBuilder("ls")
                .redirectOutput(new File(Paths.get("test/java_/lang/ProcessTest_testFileIO.txt").toString()))
                .start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        /*
        控制台没有任何输出，ProcessTest_testFileIO.txt 文件内容如下：
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
