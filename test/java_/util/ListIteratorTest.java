package java_.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * 用于{@link java.util.ListIterator}的测试和验证。
 */
public class ListIteratorTest {

    static List<Integer> testList() {
        List<Integer> list = new ArrayList<>();
        Collections.addAll(list, 1, 2, 3, 4, 5, 6, 7);
        // 元素:        1    2    3    4    5    6    7
        // 隐式光标:  ^    ^    ^    ^    ^    ^    ^    ^

        return list;
    }

    /**
     * 测试{@link ListIterator#next()}和{@link ListIterator#previous()}方法。
     */
    @Test
    public void testIter() {
        ListIterator<Integer> listIterator = testList().listIterator();
        while (listIterator.hasNext()) {
            System.out.print(listIterator.next() + " ");
        }
        System.out.println();
        while (listIterator.hasPrevious()) {
            System.out.print(listIterator.previous() + " ");
        }

        /*
        输出：
        1 2 3 4 5 6 7
        7 6 5 4 3 2 1
         */
    }

    /**
     * 测试{@link ListIterator#add(Object)}方法
     */
    @Test
    public void testAdd() {
        List<Integer> list = testList();
        ListIterator<Integer> listIterator = list.listIterator();

        // add 将元素插入到光标之前

        int cnt = 0;
        while (listIterator.hasNext()) {
            if (cnt < 5) {
                // add 将元素插入到下一个 next 返回元素的左边
                listIterator.add(10 + cnt++);
            }
            System.out.print(listIterator.next() + " ");
        }
        System.out.println("\n" + list + "\n");

        list = testList();
        listIterator = list.listIterator(list.size());
        cnt = 0;
        while (listIterator.hasPrevious()) {
            if (cnt < 5) {
                // add 将元素插入到下一个 previous 返回元素的右边
                listIterator.add(10 + cnt++);
            }
            System.out.print(listIterator.previous() + " ");
        }
        System.out.println("\n" + list + "\n");

        /*
        输出：
        1 2 3 4 5 6 7
        [10, 1, 11, 2, 12, 3, 13, 4, 14, 5, 6, 7]

        10 11 12 13 14 7 6 5 4 3 2 1
        [1, 2, 3, 4, 5, 6, 7, 14, 13, 12, 11, 10]
         */
    }

    /**
     * 测试{@link ListIterator#remove()}方法。
     */
    @Test
    public void testRemove() {
        List<Integer> list = testList();
        ListIterator<Integer> listIterator = list.listIterator();

        // remove 方法必须在 next 或 previous 调用之后调用
        Assertions.assertThrows(IllegalStateException.class, listIterator::remove);

        listIterator.next();
        listIterator.remove();
        // 每次 next 和 previous 调用之后仅能调用一次 remove
        Assertions.assertThrows(IllegalStateException.class, listIterator::remove);
        System.out.println(list + "\n");

        listIterator.next();
        listIterator.next();
        listIterator.next();  // 此时光标在 4 和 5 之间
        listIterator.previous();
        listIterator.remove();
        System.out.println(list + "\n");

        listIterator.next();  // 此时光标在 5 和 6 之间
        listIterator.add(10);
        // remove 不能在 add 方法之后调用
        Assertions.assertThrows(IllegalStateException.class, listIterator::remove);
        System.out.println(list + "\n");

        /*
        输出：
        [2, 3, 4, 5, 6, 7]

        [2, 3, 5, 6, 7]

        [2, 3, 5, 10, 6, 7]
         */
    }

    /**
     * 测试 {@link ListIterator#set(Object)}方法。
     */
    @Test
    public void testSet() {
        List<Integer> list = testList();
        ListIterator<Integer> listIterator = list.listIterator();

        // set 方法必须在 next 或 previous 调用之后调用
        Assertions.assertThrows(IllegalStateException.class, () -> listIterator.set(10));

        // set 方法可多次调用
        listIterator.next();
        listIterator.set(10);
        listIterator.set(-10);
        System.out.println(list + "\n");

        listIterator.next();
        listIterator.add(11);
        // set 不能在 add 方法之后调用
        Assertions.assertThrows(IllegalStateException.class, () -> listIterator.set(10));
        System.out.println(list + "\n");

        listIterator.previous();
        listIterator.set(100);
        System.out.println(list + "\n");

        /*
        输出：
        [-10, 2, 3, 4, 5, 6, 7]

        [-10, 2, 11, 3, 4, 5, 6, 7]

        [-10, 2, 100, 3, 4, 5, 6, 7]
         */
    }
}
