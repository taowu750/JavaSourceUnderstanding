package ujava.lang;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 对编码字符集和字符集编码的验证。
 */
public class CharsetAndEncoding {

    @Test
    public void testSystemDefaultCharset() {
        System.out.println(System.getProperty("file.encoding"));
    }

    @Test
    public void testJavaEncoding() {
        char han = '汉';
        System.out.printf("%x\n", (short) han);

        han = 0x6c49;
        System.out.println(han);
    }

    @Test
    public void testSupplementCharacter() {
        String s = String.valueOf(Character.toChars(0x2F81A));
        char[] chars = s.toCharArray();
        for (char c : chars) {
            System.out.printf("%x\n", (short) c);
        }
    }

    @Test
    public void testCodePoint() throws IOException {
        // 从0xD800到0xDFFF的码位是不能分配给其他字符的。
        int[] codePoints = {0xd801, 0xd802, 0xdf00, 0xdf01, 0x34};
        String s = new String(codePoints, 0, 5);
        System.out.println(s + "\n");

        for (char c : s.toCharArray()) {
            // 输出？？？,因为Unicode中不存在这样的char
            System.out.print(c + "--" + Integer.toHexString(c) + " ");
        }
        System.out.println("\n");

        // 测试能否写入文件
        File temp = File.createTempFile("temp", ".tmp");
        try {
            try (FileWriter writer = new FileWriter(temp)) {
                writer.write(s.toCharArray());
            }

            try (FileReader reader = new FileReader(temp)) {
                int c;
                /*
                 * 对比结果发现非代理范围的字符可以正常写入与读出,但是来自高代理与低代理范围的
                 * 字符无法正常写入，而是被转化为0x3f。
                 */
                while ((c = reader.read()) != -1)
                    System.out.print((char)c + "--" + Integer.toHexString(c) + " ");
            }
        } finally {
            temp.delete();
        }
    }
}
