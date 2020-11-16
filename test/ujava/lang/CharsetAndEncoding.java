package ujava.lang;

import org.junit.jupiter.api.Test;

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
        for(char c: chars){
            System.out.printf("%x\n",(short)c);
        }
    }
}
