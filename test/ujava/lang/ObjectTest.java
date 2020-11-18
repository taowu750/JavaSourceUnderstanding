package ujava.lang;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 对{@link Object}进行测试和验证
 */
public class ObjectTest {

    /**
     * 在 {@code finalize} 方法中“复活”对象
     */
    @Test
    public void testFinalize() throws InterruptedException {
        User user = new User();
        user = null;
        System.gc();
        TimeUnit.MILLISECONDS.sleep(500);

        user = User.user;
        assertNotNull(user);

        System.gc();
        TimeUnit.MILLISECONDS.sleep(500);
        assertNotNull(user);
    }
}

class User{

    public static User user = null;

    @Override
    protected void finalize() {
        System.out.println("User-->finalize()");
        user = this;
    }
}
