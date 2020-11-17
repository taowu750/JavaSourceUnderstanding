package ujava.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 对{@link Object}进行测试和验证
 */
public class ObjectTest {

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
