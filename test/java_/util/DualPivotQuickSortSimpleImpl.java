package java_.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dual-Pivot 快排的简单实现。
 * <p>
 * 普通快排选择一个 pivot 作为切分元素，将数组切分为两份；
 * 而 Dual-Pivot 快排选择两个 pivot 作为切分元素，将数组切分为三份。
 */
public class DualPivotQuickSortSimpleImpl {

    /**
     * 测试排序算法是否正确实现，没有 bug。
     */
    @Test
    public void testSort() {
        int size = 300;
        for (int i = 0; i < 10000; i++) {
            int min = -10000, max = 10000;
            int[] testData = new Random().ints(size, min, max).toArray();
            sort(testData);
            assertTrue(isSorted(testData));
        }
    }

    public static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    public static void sort(int[] a) {
        sort(a, 0, a.length - 1);
    }

    private static void sort(int[] a, int lo, int hi) {
        if (hi > lo) {
            // 取最左边和最右边两个元素作为切分点。
            int p = min(a[lo], a[hi]), q = max(a[lo], a[hi]);
            int l = lo + 1, g = hi - 1, k = l;

            while (k <= g) {
                if (a[k] < p)
                    exch(a, k, l++);
                else if (a[k] >= q) {
                    while (a[g] > q && k < g)
                        g--;
                    exch(a, k, g--);
                    if (a[k] < p)
                        exch(a, k, l++);
                }
                k++;
            }
            // 此时数组被切分为：
            // - (lo, l - 1]: α < p
            // - [l, g]: p <= α < q
            // - [g + 1, hi): α >= q

            l--; g++;
            a[lo] = a[l]; a[l] = p;
            a[hi] = a[g]; a[g] = q;
            // 此时数组切分情况如下：
            // - [lo, l - 1]: α < p
            // - a[l] = p
            // - [l + 1, g - 1]: p <= α < q
            // - a[g] = q
            // - [g + 1, hi): α >= q

            sort(a, lo, l - 1);
            sort(a, l + 1, g - 1);
            sort(a, g + 1, hi);
        }
    }

    private static void exch(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     * 演示切分的过程。
     */
    @Test
    public void testSplit() {
        int[] a = {4, 2, 3, 4, 1, 6, 5, 7, 9, 8, 0, 7};
        split(a);

        /*
        输出：
        l = 5, g = 7, k = 9
        [4, 2, 3, 1, 0, 6, 5, 4, 9, 8, 7, 7]

        l = 4, g = 8, k = 9
        [0, 2, 3, 1, 4, 6, 5, 4, 7, 8, 7, 9]
         */
    }

    private static void split(int[] a) {
        int lo = 0, hi = a.length - 1;
        int p = min(a[lo], a[hi]), q = max(a[lo], a[hi]);
        int l = lo + 1, g = hi - 1, k = l;

        while (k <= g) {
            if (a[k] < p)
                exch(a, k, l++);
            else if (a[k] >= q) {
                while (a[g] > q && k < g)
                    g--;
                exch(a, k, g--);
                if (a[k] < p)
                    exch(a, k, l++);
            }
            k++;
        }

        System.out.printf("l = %d, g = %d, k = %d\n", l, g, k);
        System.out.println(Arrays.toString(a));

        l--; g++;
        a[lo] = a[l]; a[l] = p;
        a[hi] = a[g]; a[g] = q;

        System.out.printf("\nl = %d, g = %d, k = %d\n", l, g, k);
        System.out.println(Arrays.toString(a));
    }
}
