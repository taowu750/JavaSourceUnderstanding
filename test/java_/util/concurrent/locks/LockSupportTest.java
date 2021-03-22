package java_.util.concurrent.locks;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 用于测试{@link LockSupport}类。
 */
public class LockSupportTest {

    /**
     * 测试{@link LockSupport#park()}调用下，线程的状态。
     */
    @Test
    public void testStatus() throws InterruptedException {
        Thread myThread = new Thread(() -> {
            System.out.println("before park");
            LockSupport.park();
            System.out.println("after park");
        });
        myThread.start();
        // 让 myThread 先运行
        Thread.sleep(300);

        System.out.println(myThread.getState());
        System.out.println("before unpark");
        LockSupport.unpark(myThread);
        System.out.println("after unpark");

        /*
        输出：
        before park
        WAITING
        before unpark
        after unpark
        after park
         */
    }

    /**
     * 测试{@link LockSupport#parkNanos(long)}调用下，线程的状态。
     */
    @Test
    public void testTimeStatus() throws InterruptedException {
        Thread myThread = new Thread(() -> {
            System.out.println("before park");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(600));
            System.out.println("after park");
        });
        myThread.start();
        // 让 myThread 先运行
        Thread.sleep(300);

        System.out.println(myThread.getState());
        System.out.println("before unpark");
        LockSupport.unpark(myThread);
        System.out.println("after unpark");

        /*
        输出：
        before park
        TIMED_WAITING
        before unpark
        after unpark
        after park
         */
    }

    /**
     * 测试{@link LockSupport#park()}被中断。
     */
    @Test
    public void testInterrupt() throws InterruptedException {
        Thread myThread = new Thread(() -> {
            System.out.println("before park");
            LockSupport.park();
            System.out.println("after park");
        });
        myThread.start();
        // 让 myThread 先运行
        Thread.sleep(300);

        System.out.println("before interrupt");
        myThread.interrupt();
        System.out.println("after interrupt: " + myThread.isInterrupted());

        /*
        大致上有两种输出：
        before park
        before interrupt
        after interrupt: true
        after park

        或

        before park
        before interrupt
        after park
        after interrupt: false
         */
    }

    /**
     * 测试序可证效果。
     */
    @Test
    public void testPermit() throws InterruptedException {
        final Thread mainThread = Thread.currentThread();
        Thread myThread = new Thread(() -> {
            System.out.println("before unpark");
            LockSupport.unpark(mainThread);
            System.out.println("after unpark");
        });
        myThread.start();
        // 让 myThread 先运行
        Thread.sleep(300);

        System.out.println("before park");
        LockSupport.park();
        System.out.println("after park");

        /*
        输出：
        before unpark
        after unpark
        before park
        after park
         */
    }
}
