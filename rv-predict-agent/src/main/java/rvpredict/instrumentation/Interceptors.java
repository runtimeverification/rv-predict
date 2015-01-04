package rvpredict.instrumentation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import rvpredict.runtime.RVPredictRuntime;

public class Interceptors {

    private static final Class<Boolean> Z   =   boolean.class;
    private static final Class<Integer> I   =   int.class;
    private static final Class<Long>    J   =   long.class;
    private static final Class<Object>  O   =   Object.class;
    private static final Class<Thread>  T   =   Thread.class;

    /** Short-hand for {@link Interceptor#create(String, Class...)}. */
    private static Interceptor init(String name, Class<?>... parameterTypes) {
        return Interceptor.create(name, parameterTypes);
    }

    public static final Interceptor LOG_FIELD_ACCESS  =  init("logFieldAcc", I, O, I, O, Z, Z);
    public static final Interceptor LOG_FIELD_INIT    =  init("logFieldInit", I, O, I, O);
    public static final Interceptor LOG_ARRAY_ACCESS  =  init("logArrayAcc", I, O, I, O, Z);
    public static final Interceptor LOG_ARRAY_INIT    =  init("logArrayInit", I, O, I, O);
    public static final Interceptor LOG_MONITOR_ENTER =  init("logMonitorEnter", I, O);
    public static final Interceptor LOG_MONITOR_EXIT  =  init("logMonitorExit", I, O);
    public static final Interceptor LOG_BRANCH        =  init("logBranch", I);

    // Thread methods
    private static final Interceptor RVPREDICT_START              =  init("rvPredictStart", I, T);
    private static final Interceptor RVPREDICT_JOIN               =  init("rvPredictJoin", I, T);
    private static final Interceptor RVPREDICT_JOIN_TIMEOUT       =  init("rvPredictJoin", I, T, J);
    private static final Interceptor RVPREDICT_JOIN_TIMEOUT_NANO  =  init("rvPredictJoin", I, T, J, I);
    private static final Interceptor RVPREDICT_INTERRUPT          =  init("rvPredictInterrupt", I, T);
    private static final Interceptor RVPREDICT_INTERRUPTED        =  init("rvPredictInterrupted", I);
    private static final Interceptor RVPREDICT_IS_INTERRUPTED     =  init("rvPredictIsInterrupted", I, T);
    private static final Interceptor RVPREDICT_SLEEP              =  init("rvPredictSleep", I, J);
    private static final Interceptor RVPREDICT_SLEEP_NANO         =  init("rvPredictSleep", I, J, I);

    // Object monitor methods
    private static final Interceptor RVPREDICT_WAIT               =  init("rvPredictWait", I, O);
    private static final Interceptor RVPREDICT_WAIT_TIMEOUT       =  init("rvPredictWait", I, O, J);
    private static final Interceptor RVPREDICT_WAIT_TIMEOUT_NANO  =  init("rvPredictWait", I, O, J, I);

    // java.lang.System methods
    private static final Interceptor RVPREDICT_SYSTEM_ARRAYCOPY   =  init("rvPredictSystemArraycopy",
            I, O, I, O, I, I);

    // java.util.concurrent.locks.Lock methods
    // note that this doesn't provide mocks for methods specific in concrete lock implementation
    private static final Interceptor RVPREDICT_LOCK               =
            init("rvPredictLock", I, Lock.class);
    private static final Interceptor RVPREDICT_LOCK_INTERRUPTIBLY =
            init("rvPredictLockInterruptibly", I, Lock.class);
    private static final Interceptor RVPREDICT_TRY_LOCK           =
            init("rvPredictTryLock", I, Lock.class);
    private static final Interceptor RVPREDICT_TRY_LOCK_TIMEOUT   =
            init("rvPredictTryLock", I, Lock.class, J, TimeUnit.class);
    private static final Interceptor RVPREDICT_UNLOCK             =
            init("rvPredictUnlock", I, Lock.class);

    // java.util.concurrent.locks.ReadWriteLock methods
    private static final Interceptor RVPREDICT_RW_LOCK_READ_LOCK  =
            init("rvPredictReadWriteLockReadLock", I, ReadWriteLock.class);
    private static final Interceptor RVPREDICT_RW_LOCK_WRITE_LOCK =
            init("rvPredictReadWriteLockWriteLock", I, ReadWriteLock.class);

    // java.util.concurrent.locks.AbstractQueueSynchronizer
    private static final Interceptor RVPREDICT_AQS_GETSTATE  =
            init("rvPredictAbstractQueuedSynchronizerGetState", I, AbstractQueuedSynchronizer.class);
    private static final Interceptor RVPREDICT_AQS_SETSTATE  =
            init("rvPredictAbstractQueuedSynchronizerSetState", I, AbstractQueuedSynchronizer.class, I);
    private static final Interceptor RVPREDICT_AQS_CASSTATE  =
            init("rvPredictAbstractQueuedSynchronizerCASState", I, AbstractQueuedSynchronizer.class, I, I);

    // java.util.concurrent.atomic.AtomicBoolean
    private static final Interceptor RVPREDICT_ATOMIC_BOOL_GET = init("rvPredictAtomicBoolGet", I,
            AtomicBoolean.class);
    private static final Interceptor RVPREDICT_ATOMIC_BOOL_SET = init("rvPredictAtomicBoolSet", I,
            AtomicBoolean.class, Z);
    private static final Interceptor RVPREDICT_ATOMIC_BOOL_CAS = init("rvPredictAtomicBoolCAS", I,
            AtomicBoolean.class, Z, Z);
    private static final Interceptor RVPREDICT_ATOMIC_BOOL_GAS = init("rvPredictAtomicBoolGAS", I,
            AtomicBoolean.class, Z);


    /*
     * Some useful constants.
     */
    private static final String JL_OBJECT       =   "java/lang/Object";
    private static final String JL_THREAD       =   "java/lang/Thread";
    private static final String JL_SYSTEM       =   "java/lang/System";
    private static final String JUCL_LOCK       =   "java/util/concurrent/locks/Lock";
    private static final String JUCL_RW_LOCK    =   "java/util/concurrent/locks/ReadWriteLock";
    private static final String JUCL_AQS        =   "java/util/concurrent/locks/AbstractQueuedSynchronizer";
    private static final String JUCA_ATOMIC_BOOL    =   "java/util/concurrent/atomic/AtomicBoolean";

    /**
     * Map from static method's signature to its {@link Interceptor}.
     */
    private static Map<String, InterceptionInfo> STATIC_METHOD_INTERCEPTION = new HashMap<>();

    /**
     * Map from instance method's signature to its {@link Interceptor}.
     */
    private static Map<String, InterceptionInfo> VIRTUAL_METHOD_INTERCEPTION = new HashMap<>();

    public static InterceptionInfo getInterceptionInfo(int opcode, String methodSig) {
        return opcode == Opcodes.INVOKESTATIC ?
            STATIC_METHOD_INTERCEPTION.get(methodSig) :
            VIRTUAL_METHOD_INTERCEPTION.get(methodSig);
    }

    private static String[] calcParamTypeDescs(Interceptor interceptor, boolean isStatic) {
        Type[] paramTypes = Type.getArgumentTypes(interceptor.desc);
        int k = isStatic ? 1 : 2;
        String[] paramTypeDescs = new String[paramTypes.length - k];
        for (int i = k; i < paramTypes.length; i++) {
            paramTypeDescs[i - k] = paramTypes[i].getDescriptor();
        }
        return paramTypeDescs;
    }

    /**
     * Computes the original method's signature from the interceptor's
     * descriptor.
     *
     * @param interceptorDesc
     *            the interceptor's descriptor
     * @param origName
     *            the original method's name
     * @param isStatic
     *            specifies if the original method is static
     * @return the original method's signature
     */
    private static String getOrigMethodSignature(String interceptorDesc, String origName,
            boolean isStatic) {
        int from = (isStatic ? interceptorDesc.indexOf('I') : interceptorDesc.indexOf(';')) + 1;
        int to = interceptorDesc.lastIndexOf(')') + 1;
        return origName + "(" + interceptorDesc.substring(from, to);
    }

    /**
     * Registers an {@link Interceptor} for a given static method.
     *
     * @param interceptor
     *            the {@code Interceptor}
     * @param owner
     *            the owner class of the method to intercept
     * @param toIntercept
     *            the method to intercept
     */
    private static void registerStaticMethodInterceptor(Interceptor interceptor, String owner,
            String toIntercept) {
        STATIC_METHOD_INTERCEPTION.put(getOrigMethodSignature(interceptor.desc, toIntercept, true),
                new InterceptionInfo(interceptor, owner, calcParamTypeDescs(interceptor, true)));
    }

    /**
     * Registers an {@link Interceptor} for a given instance method.
     *
     * @param interceptor
     *            the {@code Interceptor}
     * @param owner
     *            the owner class of the method to intercept
     * @param toIntercept
     *            the method to intercept
     */
    private static void registerVirtualMethodInterceptor(Interceptor interceptor, String owner,
            String toIntercept) {
        VIRTUAL_METHOD_INTERCEPTION.put(getOrigMethodSignature(interceptor.desc, toIntercept, false),
                new InterceptionInfo(interceptor, owner, calcParamTypeDescs(interceptor, false)));
    }

    static {
        /* static methods */
        registerStaticMethodInterceptor(RVPREDICT_INTERRUPTED, JL_THREAD, "interrupted");
        registerStaticMethodInterceptor(RVPREDICT_SLEEP, JL_THREAD, "sleep");
        registerStaticMethodInterceptor(RVPREDICT_SLEEP_NANO, JL_THREAD, "sleep");

        registerStaticMethodInterceptor(RVPREDICT_SYSTEM_ARRAYCOPY, JL_SYSTEM, "arraycopy");

        /* thread start/join/interrupt methods */
        registerVirtualMethodInterceptor(RVPREDICT_START, JL_THREAD, "start");
        registerVirtualMethodInterceptor(RVPREDICT_JOIN, JL_THREAD, "join");
        registerVirtualMethodInterceptor(RVPREDICT_JOIN_TIMEOUT, JL_THREAD, "join");
        registerVirtualMethodInterceptor(RVPREDICT_JOIN_TIMEOUT_NANO, JL_THREAD, "join");
        registerVirtualMethodInterceptor(RVPREDICT_INTERRUPT, JL_THREAD, "interrupt");
        registerVirtualMethodInterceptor(RVPREDICT_IS_INTERRUPTED, JL_THREAD, "isInterrupted");

        /* object monitor methods */
        /* YilongL: we don't need to instrument notify/notifyAll because we
         * assume spurious wakeup can happen at any time in the prediction engine */
        registerVirtualMethodInterceptor(RVPREDICT_WAIT, JL_OBJECT, "wait");
        registerVirtualMethodInterceptor(RVPREDICT_WAIT_TIMEOUT, JL_OBJECT, "wait");
        registerVirtualMethodInterceptor(RVPREDICT_WAIT_TIMEOUT_NANO, JL_OBJECT, "wait");

        /* java.util.concurrent.locks.Lock methods */
        registerVirtualMethodInterceptor(RVPREDICT_LOCK, JUCL_LOCK, "lock");
        registerVirtualMethodInterceptor(RVPREDICT_LOCK_INTERRUPTIBLY, JUCL_LOCK,
                "lockInterruptibly");
        registerVirtualMethodInterceptor(RVPREDICT_TRY_LOCK, JUCL_LOCK, "tryLock");
        registerVirtualMethodInterceptor(RVPREDICT_TRY_LOCK_TIMEOUT, JUCL_LOCK, "tryLock");
        registerVirtualMethodInterceptor(RVPREDICT_UNLOCK, JUCL_LOCK, "unlock");

        /* java.util.concurrent.locks.ReadWriteLock methods */
        registerVirtualMethodInterceptor(RVPREDICT_RW_LOCK_READ_LOCK, JUCL_RW_LOCK, "readLock");
        registerVirtualMethodInterceptor(RVPREDICT_RW_LOCK_WRITE_LOCK, JUCL_RW_LOCK, "writeLock");

        /* java.util.concurrent.locks.AbstractQueuedSynchronizer methods */
        registerVirtualMethodInterceptor(RVPREDICT_AQS_GETSTATE, JUCL_AQS, "getState");
        registerVirtualMethodInterceptor(RVPREDICT_AQS_SETSTATE, JUCL_AQS, "setState");
        registerVirtualMethodInterceptor(RVPREDICT_AQS_CASSTATE, JUCL_AQS, "compareAndSetState");

        /* java.util.concurrent.atomic.AtomicBoolean methods */
        // TODO(YilongL): investigate how/whether to mock lazySet & weakCompareAndSet
        registerVirtualMethodInterceptor(RVPREDICT_ATOMIC_BOOL_GET, JUCA_ATOMIC_BOOL, "get");
        registerVirtualMethodInterceptor(RVPREDICT_ATOMIC_BOOL_SET, JUCA_ATOMIC_BOOL, "set");
        registerVirtualMethodInterceptor(RVPREDICT_ATOMIC_BOOL_GAS, JUCA_ATOMIC_BOOL, "getAndSet");
        registerVirtualMethodInterceptor(RVPREDICT_ATOMIC_BOOL_CAS, JUCA_ATOMIC_BOOL, "compareAndSet");
    }

    static class Interceptor {

        /**
         * The interceptor method's name.
         */
        final String name;

        /**
         * The interceptor method's descriptor (see {@link Type}).
         */
        final String desc;

        private static Interceptor create(String name, Class<?>... parameterTypes) {
            Method method;
            try {
                method = RVPredictRuntime.class.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
                return null;
            }

            return new Interceptor(method.getName(), Type.getMethodDescriptor(method));
        }

        private Interceptor(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    static class InterceptionInfo {

        final Interceptor interceptor;

        /**
         * Owner class name of the original method.
         */
        final String owner;

        /**
         * Parameter type descriptors of the original method
         */
        final String[] paramTypeDescs;

        private InterceptionInfo(Interceptor interceptor, String owner,
                String... paramTypeDescs) {
            this.interceptor = interceptor;
            this.owner = owner;
            this.paramTypeDescs = paramTypeDescs;
        }
    }

}
