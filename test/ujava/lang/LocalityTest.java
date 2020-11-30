package ujava.lang;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 局部性原理验证
 */
public class LocalityTest {

    static final int LEN = 10000;
    static int[][] arr;

    @BeforeAll
    static void beforeAll() {
        arr = new int[LEN][LEN];
    }

    /**
     * 测试二维数组不同访问方式的速度差别
     */
    @Test
    public void testIn2dArray() {
        long start, finish;

        start = System.currentTimeMillis();
        // 先访问行
        for (int i = 0; i < LEN; i++) {
            for (int j = 0; j < LEN; j++) {
                arr[i][j] = j;
            }
        }
        finish = System.currentTimeMillis();
        System.out.println("行优先访问消耗时间：" + (finish - start) + "ms");

        start = System.currentTimeMillis();
        // 先访问列
        for (int j = 0; j < LEN; j++) {
            for (int i = 0; i < LEN; i++) {
                arr[i][j] = i;
            }
        }
        finish = System.currentTimeMillis();
        System.out.println("列优先访问消耗时间：" + (finish - start) + "ms");
    }
}
