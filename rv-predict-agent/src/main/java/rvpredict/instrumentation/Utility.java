package rvpredict.instrumentation;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

public class Utility {

    public static final String DESC_ARRAY_PREFIX    =   "[";

    public static boolean isSubclassOf(String class0, String class1) {
        if (class0.equals(class1)) {
            return true;
        }

        switch (class1) {
        case "java/lang/Object":
            return true;
        case "java/lang/Thread":
            return isThreadClass(class0);
        case "java/util/concurrent/locks/Lock":
            return isLockClass(class0);
        case "java/util/concurrent/locks/Condition":
            return isConditionClass(class0);
        case "java/util/concurrent/locks/ReadWriteLock":
            return isReadWriteLockClass(class0);
        case "java/util/concurrent/locks/AbstractQueuedSynchronizer":
            return isAQSClass(class0);
        case "java/util/concurrent/atomic/AtomicBoolean":
            return false;
        default:
            System.err.println("[Warning]: unexpected case isSubclassOf(" + class0 + ", " + class1
                    + ")");
            return false;
        }
    }

    private static boolean isThreadClass(String className) {
        if (className.startsWith(DESC_ARRAY_PREFIX)) {
            return false;
        }

        while (className != null && !className.equals("java/lang/Object")) {
            if (className.equals("java/lang/Thread")) {
                return true;
            }

            try {
                className = new ClassReader(className).getSuperName();
            } catch (IOException e) {
                System.err.println("ASM ClassReader: unable to read " + className);
                return false;
            }
        }
        return false;
    }

    private static boolean isLockClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/Lock".equals(className)
            || "java/util/concurrent/locks/ReentrantLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock".equals(className);
    }

    private static boolean isConditionClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/Condition".equals(className)
            || "java/util/concurrent/locks/AbstractQueuedSynchronizer$ConditionObject".equals(className)
            || "java/util/concurrent/locks/AbstractQueuedLongSynchronizer$ConditionObject".equals(className);
    }

    private static boolean isReadWriteLockClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/ReadWriteLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock".equals(className);
    }

    private static boolean isAQSClass(String class0) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/Semaphore$Sync".equals(class0)
            || "java/util/concurrent/Semaphore$FairSync".equals(class0)
            || "java/util/concurrent/Semaphore$NonfairSync".equals(class0)
            || "java/util/concurrent/CountDownLatch$Sync".equals(class0);
    }

}
