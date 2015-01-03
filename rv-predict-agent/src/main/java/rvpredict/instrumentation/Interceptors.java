package rvpredict.instrumentation;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public class Interceptors {

    public static final String LOG_FIELD_ACCESS = "logFieldAcc";
    public static final String LOG_FIELD_INIT = "logFieldInit";
    public static final String LOG_ARRAY_ACCESS = "logArrayAcc";
    public static final String LOG_ARRAY_INIT = "logArrayInit";
    public static final String LOG_MONITOR_ENTER = "logMonitorEnter";
    public static final String LOG_MONITOR_EXIT = "logMonitorExit";
    public static final String LOG_BRANCH = "logBranch";

    // Thread methods
    public static final String RVPREDICT_START = "rvPredictStart";
    public static final String RVPREDICT_JOIN = "rvPredictJoin";
    public static final String RVPREDICT_INTERRUPT = "rvPredictInterrupt";
    public static final String RVPREDICT_INTERRUPTED = "rvPredictInterrupted";
    public static final String RVPREDICT_IS_INTERRUPTED = "rvPredictIsInterrupted";
    public static final String RVPREDICT_SLEEP = "rvPredictSleep";

    // Object monitor methods
    public static final String RVPREDICT_WAIT = "rvPredictWait";

    // java.util.concurrent.locks.Lock methods
    // note that this doesn't provide mocks for methods specific in concrete lock implementation
    public static final String RVPREDICT_LOCK = "rvPredictLock";
    public static final String RVPREDICT_LOCK_INTERRUPTIBLY = "rvPredictLockInterruptibly";
    public static final String RVPREDICT_TRY_LOCK = "rvPredictTryLock";
    public static final String RVPREDICT_UNLOCK = "rvPredictUnlock";

    // java.util.concurrent.locks.ReadWriteLock methods
    public static final String RVPREDICT_RW_LOCK_READ_LOCK = "rvPredictReadWriteLockReadLock";
    public static final String RVPREDICT_RW_LOCK_WRITE_LOCK = "rvPredictReadWriteLockWriteLock";

    public static final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;ZZ)V";
    public static final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";
    public static final String DESC_LOG_FIELD_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";
    public static final String DESC_LOG_ARRAY_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";

    public static final String DESC_LOG_MONITOR_ENTER = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_MONITOR_EXIT = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_BRANCH = "(I)V";
    public static final String DESC_RVPREDICT_START = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT = "(ILjava/lang/Thread;J)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT_NANO = "(ILjava/lang/Thread;JI)V";
    public static final String DESC_RVPREDICT_INTERRUPT = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_INTERRUPTED = "(I)Z";
    public static final String DESC_RVPREDICT_IS_INTERRUPTED = "(ILjava/lang/Thread;)Z";
    public static final String DESC_RVPREDICT_SLEEP = "(IJ)V";
    public static final String DESC_RVPREDICT_SLEEP_NANOS = "(IJI)V";
    public static final String DESC_RVPREDICT_WAIT = "(ILjava/lang/Object;)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT = "(ILjava/lang/Object;J)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT_NANO = "(ILjava/lang/Object;JI)V";

    public static final String RVPREDICT_SYSTEM_ARRAYCOPY = "rvPredictSystemArraycopy";
    public static final String DESC_RVPREDICT_SYSTEM_ARRAYCOPY = "(ILjava/lang/Object;ILjava/lang/Object;II)V";

    public static final String DESC_RVPREDICT_LOCK = "(ILjava/util/concurrent/locks/Lock;)V";
    public static final String DESC_RVPREDICT_LOCK_INTERRUPTIBLY = "(ILjava/util/concurrent/locks/Lock;)V";
    public static final String DESC_RVPREDICT_TRY_LOCK = "(ILjava/util/concurrent/locks/Lock;)Z";
    public static final String DESC_RVPREDICT_TRY_LOCK_TIMEOUT = "(ILjava/util/concurrent/locks/Lock;JLjava/util/concurrent/TimeUnit;)Z";
    public static final String DESC_RVPREDICT_UNLOCK = "(ILjava/util/concurrent/locks/Lock;)V";

    public static final String DESC_RVPREDICT_RW_LOCK_READ_LOCK = "(ILjava/util/concurrent/locks/ReadWriteLock;)Ljava/util/concurrent/locks/Lock;";
    public static final String DESC_RVPREDICT_RW_LOCK_WRITE_LOCK = "(ILjava/util/concurrent/locks/ReadWriteLock;)Ljava/util/concurrent/locks/Lock;";

    /*
     * Some useful constants.
     */
    private static final String I               =   "I";
    private static final String J               =   "J";
    private static final String JL_OBJECT       =   "java/lang/Object";
    private static final String JL_THREAD       =   "java/lang/Thread";
    private static final String JL_SYSTEM       =   "java/lang/System";
    private static final String JUCL_LOCK       =   "java/util/concurrent/locks/Lock";
    private static final String JUCL_RW_LOCK    =   "java/util/concurrent/locks/ReadWriteLock";

    private static Map<String, MethodCallSubst> STATIC_METHOD_CALL_SUBST = new HashMap<>();

    private static Map<String, MethodCallSubst> VIRTUAL_METHOD_CALL_SUBST = new HashMap<>();

    public static MethodCallSubst getMethodCallSubst(int opcode, String sig) {
        return opcode == Opcodes.INVOKESTATIC ?
            STATIC_METHOD_CALL_SUBST.get(sig) :
            VIRTUAL_METHOD_CALL_SUBST.get(sig);
    }

    private static String getMethodSignature(String methodName, String... argTypeDescs) {
        StringBuilder sb = new StringBuilder(methodName);
        sb.append("(");
        for (int i = 0; i < argTypeDescs.length; i++) {
            sb.append(argTypeDescs[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private static void registerStaticMethodSubstitution(String substName, String substDesc,
            String owner, String origName, String... origArgTypeDescs) {
        STATIC_METHOD_CALL_SUBST.put(getMethodSignature(origName, origArgTypeDescs),
                new MethodCallSubst(substName, substDesc, owner, origArgTypeDescs));
    }

    private static void registerVirtualMethodSubstitution(String substName, String substDesc,
            String owner, String origName, String... origArgTypeDescs) {
        VIRTUAL_METHOD_CALL_SUBST.put(getMethodSignature(origName, origArgTypeDescs),
                new MethodCallSubst(substName, substDesc, owner, origArgTypeDescs));
    }

    static {
        /* static methods */
        registerStaticMethodSubstitution(RVPREDICT_INTERRUPTED, DESC_RVPREDICT_INTERRUPTED,
                JL_THREAD, "interrupted");
        registerStaticMethodSubstitution(RVPREDICT_SLEEP, DESC_RVPREDICT_SLEEP,
                JL_THREAD, "sleep", J);
        registerStaticMethodSubstitution(RVPREDICT_SLEEP, DESC_RVPREDICT_SLEEP_NANOS,
                JL_THREAD, "sleep", J, I);

        registerStaticMethodSubstitution(RVPREDICT_SYSTEM_ARRAYCOPY,
                DESC_RVPREDICT_SYSTEM_ARRAYCOPY, JL_SYSTEM, "arraycopy", "Ljava/lang/Object;",
                I, "Ljava/lang/Object;", I, I);

        /* thread start/join/interrupt methods */
        registerVirtualMethodSubstitution(RVPREDICT_START, DESC_RVPREDICT_START,
                JL_THREAD, "start");
        registerVirtualMethodSubstitution(RVPREDICT_JOIN, DESC_RVPREDICT_JOIN,
                JL_THREAD, "join");
        registerVirtualMethodSubstitution(RVPREDICT_JOIN, DESC_RVPREDICT_JOIN_TIMEOUT,
                JL_THREAD, "join", J);
        registerVirtualMethodSubstitution(RVPREDICT_JOIN, DESC_RVPREDICT_JOIN_TIMEOUT_NANO,
                JL_THREAD, "join", J, I);
        registerVirtualMethodSubstitution(RVPREDICT_INTERRUPT, DESC_RVPREDICT_INTERRUPT,
                JL_THREAD, "interrupt");
        registerVirtualMethodSubstitution(RVPREDICT_IS_INTERRUPTED, DESC_RVPREDICT_IS_INTERRUPTED,
                JL_THREAD, "isInterrupted");

        /* object monitor methods */
        /* YilongL: we don't need to instrument notify/notifyAll because we
         * assume spurious wakeup can happen at any time in the prediction engine */
        registerVirtualMethodSubstitution(RVPREDICT_WAIT, DESC_RVPREDICT_WAIT,
                JL_OBJECT, "wait");
        registerVirtualMethodSubstitution(RVPREDICT_WAIT, DESC_RVPREDICT_WAIT_TIMEOUT,
                JL_OBJECT, "wait", J);
        registerVirtualMethodSubstitution(RVPREDICT_WAIT, DESC_RVPREDICT_WAIT_TIMEOUT_NANO,
                JL_OBJECT, "wait", J, I);

        /* java.util.concurrent.locks.Lock methods */
        registerVirtualMethodSubstitution(RVPREDICT_LOCK, DESC_RVPREDICT_LOCK,
                JUCL_LOCK, "lock");
        registerVirtualMethodSubstitution(RVPREDICT_LOCK_INTERRUPTIBLY, DESC_RVPREDICT_LOCK_INTERRUPTIBLY,
                JUCL_LOCK, "lockInterruptibly");
        registerVirtualMethodSubstitution(RVPREDICT_TRY_LOCK, DESC_RVPREDICT_TRY_LOCK,
                JUCL_LOCK, "tryLock");
        registerVirtualMethodSubstitution(RVPREDICT_TRY_LOCK, DESC_RVPREDICT_TRY_LOCK_TIMEOUT,
                JUCL_LOCK, "tryLock", J, "Ljava/util/concurrent/TimeUnit;");
        registerVirtualMethodSubstitution(RVPREDICT_UNLOCK, DESC_RVPREDICT_UNLOCK,
                JUCL_LOCK, "unlock");

        /* java.util.concurrent.locks.ReadWriteLock methods */
        registerVirtualMethodSubstitution(RVPREDICT_RW_LOCK_READ_LOCK,
                DESC_RVPREDICT_RW_LOCK_READ_LOCK, JUCL_RW_LOCK,
                "readLock");
        registerVirtualMethodSubstitution(RVPREDICT_RW_LOCK_WRITE_LOCK,
                DESC_RVPREDICT_RW_LOCK_WRITE_LOCK, JUCL_RW_LOCK,
                "writeLock");
    }

    static class MethodCallSubst {

        /**
         * Name of the substitution method.
         */
        final String name;

        /**
         * Descriptor of the substitution method.
         */
        final String desc;

        /**
         * Owner class name of the original method.
         */
        final String owner;

        /**
         * Argument type descriptors of the original method
         */
        final String[] argDescs;

        private MethodCallSubst(String name, String desc, String owner, String... argDescs) {
            this.name = name;
            this.desc = desc;
            this.owner = owner;
            this.argDescs = argDescs;
        }
    }

}
