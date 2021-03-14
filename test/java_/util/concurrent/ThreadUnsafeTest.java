package java_.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程不安全测试。
 */
public class ThreadUnsafeTest {

    /**
     * 线程不安全示例。
     */
    @Test
    public void testThreadUnsafeExample() throws InterruptedException {
        final int threadSize = 1000;
        ThreadUnsafeExample example = new ThreadUnsafeExample();
        final CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < threadSize; i++) {
            executorService.execute(() -> {
                example.add();
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        executorService.shutdown();
        System.out.println(example.get());

        /*
        输出结果有很多种，可能不等于 1000
         */
    }

    @Test
    public void testReorderExample() throws InterruptedException {
        for (int i = 0; i < 100000; i++) {
            ReorderExample example = new ReorderExample();
            final CountDownLatch countDownLatch = new CountDownLatch(2);

            new Thread(() -> {
                example.writer();
                countDownLatch.countDown();
            }).start();
            new Thread(() -> {
                example.reader();
                countDownLatch.countDown();
            }).start();

            countDownLatch.await();
        }
    }
}

class ThreadUnsafeExample {

    private int cnt = 0;

    public void add() {
        cnt++;
    }

    public int get() {
        return cnt;
    }
}

class ReorderExample {
    int a = 0;
    boolean flag = false;

    public void writer() {
        a = 2;                   //1
        flag = true;             //2
    }

    public void reader() {
        if (flag) {                //3
            int i =  a * a;        //4
            if (i != 4)
                System.out.println(i);
        }
    }
}
