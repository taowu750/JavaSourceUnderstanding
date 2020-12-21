package java_.lang;

import org.junit.jupiter.api.Test;

/**
 * 演示{@code finalize}攻击。
 */
public class FinalizeAttackTest {

    /**
     * 测试{@code finalize}方法的漏洞。
     */
    @Test
    public void testFinalizeRisk() {
        try {
            new Zombie(-100);
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
        System.gc();
        System.runFinalization();

        System.out.println(Zombie.zombie);

        /*
        输出：
        java.lang.IllegalArgumentException: Negative Zombie value
        Zombie{value=0}
         */
    }

    /**
     * 测试{@code finalize}攻击。
     */
    @Test
    public void testFinalizeAttack() {
        try {
            SensitiveOperation sensitiveOperation = new SensitiveOperationFinalizer();
            sensitiveOperation.storeMoney();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        System.gc();
        System.runFinalization();

        /*
        输出：
        Security check failed!
        We can still do store Money action!
        Store 1000000 RMB!
         */
    }

    /**
     * 测试使用标志变量防范{@code finalize}攻击。
     */
    @Test
    public void testFlagProtect() {
        try {
            SensitiveOperationFlag sensitiveOperationFlag = new SensitiveOperationFlagFinalizer();
            sensitiveOperationFlag.storeMoney();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        System.gc();
        System.runFinalization();

        /*
        输出：
        Security check failed!
        We can still do store Money action!
         */
    }

    /**
     * 测试使用 this 防范{@code finalize}攻击。
     */
    @Test
    public void testThisProtect() {
        try {
            SensitiveOperationThis sensitiveOperationThis = new SensitiveOperationThisFinalizer();
            sensitiveOperationThis.storeMoney();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        System.gc();
        System.runFinalization();

        /*
        输出：
        Security check failed!
         */
    }
}

class Zombie {
    static Zombie zombie;
    int value;

    public Zombie(int value) {
        if(value < 0) {
            throw new IllegalArgumentException("Negative Zombie value");
        }
        this.value = value;
    }

    public void finalize() {
        zombie = this;
    }

    @Override
    public String toString() {
        return "Zombie{" +
                "value=" + value +
                '}';
    }
}

class SensitiveOperation {

    public SensitiveOperation(){
        // 进行检查
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
    }

    private boolean doSecurityCheck(){
        return false;
    }

    // 敏感操作
    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
}

class SensitiveOperationFinalizer extends SensitiveOperation {

    public SensitiveOperationFinalizer(){
    }

    @Override
    protected void finalize() {
        // 执行敏感操作
        System.out.println("We can still do store Money action!");
        this.storeMoney();
    }
}

class SensitiveOperationFlag {

    private volatile boolean initialized = false;

    public SensitiveOperationFlag(){
        if(!doSecurityCheck()){
            throw new SecurityException("Security check failed!");
        }
        initialized = true;
    }

    private boolean doSecurityCheck(){
        return false;
    }

    public void storeMoney(){
        if(!initialized) {
            throw new SecurityException("Object is not initiated yet!");
        }
        System.out.println("Store 1000000 RMB!");
    }
}

class SensitiveOperationFlagFinalizer extends SensitiveOperationFlag {

    public SensitiveOperationFlagFinalizer(){
    }

    @Override
    protected void finalize() {
        // 执行敏感操作
        System.out.println("We can still do store Money action!");
        this.storeMoney();
    }
}

class SensitiveOperationThis {

    public SensitiveOperationThis(){
        this(doSecurityCheck());
    }

    private SensitiveOperationThis(boolean secure) {
    }

    private static boolean doSecurityCheck(){
        throw new SecurityException("Security check failed!");
    }

    public void storeMoney(){
        System.out.println("Store 1000000 RMB!");
    }
}

class SensitiveOperationThisFinalizer extends SensitiveOperationThis {

    public SensitiveOperationThisFinalizer(){
    }

    @Override
    protected void finalize() {
        // 执行敏感操作
        System.out.println("We can still do store Money action!");
        this.storeMoney();
    }
}