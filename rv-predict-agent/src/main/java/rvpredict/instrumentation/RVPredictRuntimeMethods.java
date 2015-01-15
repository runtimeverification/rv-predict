package rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.ClassReader;

import rvpredict.runtime.RVPredictRuntime;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RVPredictRuntimeMethods {

    /*
     * Some class literals.
     */
    private static final Class<Boolean> Z   =   boolean.class;
    private static final Class<Integer> I   =   int.class;
    private static final Class<Long>    J   =   long.class;
    private static final Class<Object>  O   =   Object.class;

    public static final RVPredictRuntimeMethod LOG_FIELD_ACCESS  =  init("logFieldAcc", O, J, I, Z, Z, I);
    public static final RVPredictRuntimeMethod LOG_FIELD_INIT    =  init("logFieldInit", O, J, I, I);
    public static final RVPredictRuntimeMethod LOG_ARRAY_ACCESS  =  init("logArrayAcc", O, I, J, Z, I);
    public static final RVPredictRuntimeMethod LOG_ARRAY_INIT    =  init("logArrayInit", O, I, J, I);
    public static final RVPredictRuntimeMethod LOG_MONITOR_ENTER =  init("logMonitorEnter", O, I);
    public static final RVPredictRuntimeMethod LOG_MONITOR_EXIT  =  init("logMonitorExit", O, I);
    public static final RVPredictRuntimeMethod LOG_BRANCH        =  init("logBranch", I);

    /*
     * Some useful constants.
     */
    public static final int STATIC     =   0;
    public static final int VIRTUAL    =   1;
    public static final int INTERFACE  =   2;
    public static final int SPECIAL    =   3;
    private static final String JL_OBJECT       =   "java/lang/Object";
    private static final String JL_THREAD       =   "java/lang/Thread";
    private static final String JL_SYSTEM       =   "java/lang/System";
    private static final String JU_COLLECTION   =   "java/util/Collection";
    private static final String JU_MAP          =   "java/util/Map";
    private static final String JUCL_LOCK       =   "java/util/concurrent/locks/Lock";
    private static final String JUCL_CONDITION  =   "java/util/concurrent/locks/Condition";
    private static final String JUCL_RW_LOCK    =   "java/util/concurrent/locks/ReadWriteLock";
    private static final String JUCL_AQS        =   "java/util/concurrent/locks/AbstractQueuedSynchronizer";
    private static final String JUCA_ATOMIC_BOOL    =   "java/util/concurrent/atomic/AtomicBoolean";

    /**
     * Map from method signature to possible {@link RVPredictInterceptor}'s.
     */
    private static final Map<String, List<RVPredictInterceptor>> METHOD_INTERCEPTION = Maps.newHashMap();

    // Thread methods
    public static final RVPredictInterceptor RVPREDICT_START              =
            register(VIRTUAL, JL_THREAD, "start", "rvPredictStart");
    public static final RVPredictInterceptor RVPREDICT_JOIN               =
            register(VIRTUAL, JL_THREAD, "join", "rvPredictJoin");
    public static final RVPredictInterceptor RVPREDICT_JOIN_TIMEOUT       =
            register(VIRTUAL, JL_THREAD, "join", "rvPredictJoin", J);
    public static final RVPredictInterceptor RVPREDICT_JOIN_TIMEOUT_NANO  =
            register(VIRTUAL, JL_THREAD, "join", "rvPredictJoin", J, I);
    public static final RVPredictInterceptor RVPREDICT_INTERRUPT          =
            register(VIRTUAL, JL_THREAD, "interrupt", "rvPredictInterrupt");
    public static final RVPredictInterceptor RVPREDICT_INTERRUPTED        =
            register(STATIC,  JL_THREAD, "interrupted", "rvPredictInterrupted");
    public static final RVPredictInterceptor RVPREDICT_IS_INTERRUPTED     =
            register(VIRTUAL, JL_THREAD, "isInterrupted", "rvPredictIsInterrupted");
    public static final RVPredictInterceptor RVPREDICT_SLEEP              =
            register(STATIC,  JL_THREAD, "sleep", "rvPredictSleep", J);
    public static final RVPredictInterceptor RVPREDICT_SLEEP_NANO         =
            register(STATIC,  JL_THREAD, "sleep", "rvPredictSleep", J, I);

    // Object monitor methods
    public static final RVPredictInterceptor RVPREDICT_WAIT               =
            register(VIRTUAL, JL_OBJECT, "wait", "rvPredictWait");
    public static final RVPredictInterceptor RVPREDICT_WAIT_TIMEOUT       =
            register(VIRTUAL, JL_OBJECT, "wait", "rvPredictWait", J);
    public static final RVPredictInterceptor RVPREDICT_WAIT_TIMEOUT_NANO  =
            register(VIRTUAL, JL_OBJECT, "wait", "rvPredictWait", J, I);

    // java.lang.System methods
    public static final RVPredictInterceptor RVPREDICT_SYSTEM_ARRAYCOPY   =
            register(STATIC, JL_SYSTEM, "arraycopy", "rvPredictSystemArraycopy", O, I, O, I, I);

    // java.util.Collection methods
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_ADD     =
            register(INTERFACE, JU_COLLECTION, "add", "rvPredictCollectionAdd", O);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_ADD_ALL =
            register(INTERFACE, JU_COLLECTION, "addAll", "rvPredictCollectionAddAll", Collection.class);

    // java.util.Map methods
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT            =
            register(INTERFACE, JU_MAP, "put", "rvPredictMapPut", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT_ALL        =
            register(INTERFACE, JU_MAP, "putAll", "rvPredictMapPutAll", Map.class);

    // java.util.concurrent.locks.Lock methods
    // note that this doesn't provide mocks for methods specific in concrete lock implementation
    public static final RVPredictInterceptor RVPREDICT_LOCK               =
            register(INTERFACE, JUCL_LOCK, "lock", "rvPredictLock");
    public static final RVPredictInterceptor RVPREDICT_LOCK_INTERRUPTIBLY =
            register(INTERFACE, JUCL_LOCK, "lockInterruptibly", "rvPredictLockInterruptibly");
    public static final RVPredictInterceptor RVPREDICT_TRY_LOCK           =
            register(INTERFACE, JUCL_LOCK, "tryLock", "rvPredictTryLock");
    public static final RVPredictInterceptor RVPREDICT_TRY_LOCK_TIMEOUT   =
            register(INTERFACE, JUCL_LOCK, "tryLock", "rvPredictTryLock", J, TimeUnit.class);
    public static final RVPredictInterceptor RVPREDICT_UNLOCK             =
            register(INTERFACE, JUCL_LOCK, "unlock", "rvPredictUnlock");
    public static final RVPredictInterceptor RVPREDICT_LOCK_NEW_COND      =
            register(INTERFACE, JUCL_LOCK, "newCondition", "rvPredictLockNewCondition");

    // java.util.concurrent.locks.Condition methods
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT         =
            register(INTERFACE, JUCL_CONDITION, "await", "rvPredictConditionAwait");
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_TIMEOUT =
            register(INTERFACE, JUCL_CONDITION, "await", "rvPredictConditionAwait", J, TimeUnit.class);
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_NANOS   =
            register(INTERFACE, JUCL_CONDITION, "awaitNanos", "rvPredictConditionAwaitNanos", J);
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_UNINTERRUPTIBLY =
            register(INTERFACE, JUCL_CONDITION, "awaitUninterruptibly", "rvPredictConditionAwaitUninterruptibly");
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_UNTIL   =
            register(INTERFACE, JUCL_CONDITION, "awaitUntil", "rvPredictConditionAwaitUntil", Date.class);

    // java.util.concurrent.locks.ReadWriteLock methods
    public static final RVPredictInterceptor RVPREDICT_RW_LOCK_READ_LOCK  =
            register(INTERFACE, JUCL_RW_LOCK, "readLock", "rvPredictReadWriteLockReadLock");
    public static final RVPredictInterceptor RVPREDICT_RW_LOCK_WRITE_LOCK =
            register(INTERFACE, JUCL_RW_LOCK, "writeLock", "rvPredictReadWriteLockWriteLock");

    // java.util.concurrent.locks.AbstractQueueSynchronizer
    public static final RVPredictInterceptor RVPREDICT_AQS_GETSTATE  =
            register(VIRTUAL, JUCL_AQS, "getState", "rvPredictAbstractQueuedSynchronizerGetState");
    public static final RVPredictInterceptor RVPREDICT_AQS_SETSTATE  =
            register(VIRTUAL, JUCL_AQS, "setState", "rvPredictAbstractQueuedSynchronizerSetState", I);
    public static final RVPredictInterceptor RVPREDICT_AQS_CASSTATE  =
            register(VIRTUAL, JUCL_AQS, "compareAndSetState", "rvPredictAbstractQueuedSynchronizerCASState", I, I);

    // java.util.concurrent.atomic.AtomicBoolean
    public static final RVPredictInterceptor RVPREDICT_ATOMIC_BOOL_GET =
            register(VIRTUAL, JUCA_ATOMIC_BOOL, "get", "rvPredictAtomicBoolGet");
    public static final RVPredictInterceptor RVPREDICT_ATOMIC_BOOL_SET =
            register(VIRTUAL, JUCA_ATOMIC_BOOL, "set", "rvPredictAtomicBoolSet", Z);
    public static final RVPredictInterceptor RVPREDICT_ATOMIC_BOOL_CAS =
            register(VIRTUAL, JUCA_ATOMIC_BOOL, "compareAndSet", "rvPredictAtomicBoolCAS", Z, Z);
    public static final RVPredictInterceptor RVPREDICT_ATOMIC_BOOL_GAS =
            register(VIRTUAL, JUCA_ATOMIC_BOOL, "getAndSet", "rvPredictAtomicBoolGAS", Z);

    /** Short-hand for {@link RVPredictRuntimeMethod#create(String, Class...)}. */
    private static RVPredictRuntimeMethod init(String name, Class<?>... parameterTypes) {
        return RVPredictRuntimeMethod.create(name, parameterTypes);
    }

    /**
     * Initializes, registers, and returns a {@link RVPredictInterceptor}.
     * <p>
     * <b>Note:</b> {@code classOrInterface} is the class or interface in which
     * the associated Java method is <em>declared</em>. Therefore, if we would
     * like the interceptor to have different behaviors according to the runtime
     * type of the method's owner object, we are going to implement it in
     * {@link RVPredictRuntime}. For example:
     *
     * <pre>
     * public static Object rvPredictMapPut(int locId, Map map, Object key, Object value) {
     *     if (map instanceof ConcurrentMap) {
     *        ... ...
     *     } else if (map instanceof HashMap) {
     *        ... ...
     *     } else {
     *        ... ...
     *     }
     * }
     * </pre>
     *
     * @see RVPredictInterceptor
     */
    private static RVPredictInterceptor register(int methodType, String classOrInterface,
            String methodName, String interceptorName, Class<?>... parameterTypes) {
        RVPredictInterceptor interceptor = null;
        try {
            interceptor = RVPredictInterceptor.create(methodType, classOrInterface, methodName,
                    interceptorName, parameterTypes);
            List<RVPredictInterceptor> interceptors = METHOD_INTERCEPTION.get(interceptor
                    .getOriginalMethodSig());
            if (interceptors == null) {
                interceptors = Lists.newArrayList();
                METHOD_INTERCEPTION.put(interceptor.getOriginalMethodSig(), interceptors);
            }
            interceptors.add(interceptor);
        } catch (Exception e) {
            /* no exception shall happen during Interceptor initialization */
            e.printStackTrace();
        }
        return interceptor;
    }

    /**
     * Looks up the corresponding interceptor method for a given Java method
     * call.
     *
     * @param owner
     *            the class/interface name of the object whose member method is
     *            invoked or the class name of the static method
     * @param methodSig
     *            the signature of the invoked method
     * @param itf
     * @return the {@link RVPredictInterceptor} if successful or {@code null} if
     *         no suitable interceptor found
     */
    public static RVPredictInterceptor lookup(String owner, String methodSig, boolean itf) {
        /*
         * TODO(YilongL): figure out how to the deal with `itf' introduced for Java 8
         * http://stackoverflow.com/questions/24510785/explanation-of-itf-parameter-of-visitmethodinsn-in-asm-5
         */
        List<RVPredictInterceptor> interceptors = METHOD_INTERCEPTION.get(methodSig);
        if (interceptors != null) {
            for (RVPredictInterceptor interceptor : interceptors) {
                /* Method signature alone is not enough to determine if an
                 * interceptor is indeed what we are looking for. We need the
                 * owner class name of the Java method call as well. However, we
                 * cannot simply put it in the map key because the owner class
                 * name could be more concrete than the registered class name of
                 * the interceptor if the method call is not static or private. */
                switch (interceptor.methodType) {
                case STATIC:
                case SPECIAL:
                    if (interceptor.classOrInterface.equals(owner)) {
                        return interceptor;
                    }
                    break;
                case VIRTUAL:
                case INTERFACE:
                    if (isSubclassOf(owner, interceptor.classOrInterface,
                            interceptor.methodType == INTERFACE)) {
                        return interceptor;
                    }
                    break;
                default:
                    assert false: "unreachable";
                }
            }
        }
        return null;
    }

    /**
     * Checks if one class or interface extends or implements another class or
     * interface.
     *
     * @param class0
     *            the name of the first class or interface
     * @param class1
     *            the name of the second class of interface
     * @param itf
     *            if {@code class1} represents an interface
     * @return {@code true} if {@code class1} is assignable from {@code class0}
     */
    private static boolean isSubclassOf(String class0, String class1, boolean itf) {
        assert !class1.startsWith("[");

        if (class0.startsWith("[")) {
            return class1.equals("java/lang/Object");
        }
        if (class0.equals(class1)) {
            return true;
        }

        List<String> superclasses = getSuperclasses(class0);
        if (!itf) {
            return superclasses.contains(class1);
        } else {
            boolean result = getInterfaces(class0).contains(class1);
            for (String superclass : superclasses) {
                result = result || getInterfaces(superclass).contains(class1);
            }
            return result;
        }
    }

    /**
     * Obtains the {@link ClassReader} used to retrieve the superclass and
     * interfaces information of a class or interface.
     * <p>
     * This method is meant to avoid class loading when checking inheritance
     * relation because class loading during
     * {@link ClassFileTransformer#transform(ClassLoader, String, Class, java.security.ProtectionDomain, byte[])}
     * can not be properly intercepted by the java agent.
     *
     * @param className
     *            the class or interface to read
     * @return the {@link ClassReader}
     */
    private static ClassReader getClassReader(String className) {
        try {
            return new ClassReader(className);
        } catch (IOException e) {
            System.err.println("ASM ClassReader: unable to read " + className);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all superclasses of a class or interface.
     * <p>
     * The superclass of an interface will be the {@code Object}.
     *
     * @param className
     *            the internal name of a class or interface
     * @return set of superclasses
     */
    private static List<String> getSuperclasses(String className) {
        List<String> result = new ArrayList<>();
        while (className != null) {
            className = getClassReader(className).getSuperName();
            if (className != null) {
                result.add(className);
            }
        }
        return result;
    }

    /**
     * Gets all implemented interfaces (including parent interfaces) of a class
     * or all parent interfaces of an interface.
     *
     * @param className
     *            the internal name of a class or interface
     * @return set of interfaces
     */
    private static Set<String> getInterfaces(String className) {
        Set<String> interfaces = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(className);
        while (!queue.isEmpty()) {
            String cls = queue.poll();
            for (String itf : getClassReader(cls).getInterfaces()) {
                if (interfaces.add(itf)) {
                    queue.add(itf);
                }
            }
        }
        return interfaces;
    }

}
