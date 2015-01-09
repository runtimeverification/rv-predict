package rvpredict.instrumentation;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.Type;

import rvpredict.runtime.RVPredictRuntime;

import com.google.common.collect.ImmutableList;
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

    public static final RVPredictRuntimeMethod LOG_FIELD_ACCESS  =  init("logFieldAcc", I, O, I, O, Z, Z);
    public static final RVPredictRuntimeMethod LOG_FIELD_INIT    =  init("logFieldInit", I, O, I, O);
    public static final RVPredictRuntimeMethod LOG_ARRAY_ACCESS  =  init("logArrayAcc", I, O, I, O, Z);
    public static final RVPredictRuntimeMethod LOG_ARRAY_INIT    =  init("logArrayInit", I, O, I, O);
    public static final RVPredictRuntimeMethod LOG_MONITOR_ENTER =  init("logMonitorEnter", I, O);
    public static final RVPredictRuntimeMethod LOG_MONITOR_EXIT  =  init("logMonitorExit", I, O);
    public static final RVPredictRuntimeMethod LOG_BRANCH        =  init("logBranch", I);

    /*
     * Some useful constants.
     */
    private static final int STATIC     =   0x0;
    private static final int VIRTUAL    =   0x1;
    private static final int SPECIAL    =   0x2;
    private static final String JL_OBJECT       =   "java/lang/Object";
    private static final String JL_THREAD       =   "java/lang/Thread";
    private static final String JL_SYSTEM       =   "java/lang/System";
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

    // java.util.concurrent.locks.Lock methods
    // note that this doesn't provide mocks for methods specific in concrete lock implementation
    public static final RVPredictInterceptor RVPREDICT_LOCK               =
            register(VIRTUAL, JUCL_LOCK, "lock", "rvPredictLock");
    public static final RVPredictInterceptor RVPREDICT_LOCK_INTERRUPTIBLY =
            register(VIRTUAL, JUCL_LOCK, "lockInterruptibly", "rvPredictLockInterruptibly");
    public static final RVPredictInterceptor RVPREDICT_TRY_LOCK           =
            register(VIRTUAL, JUCL_LOCK, "tryLock", "rvPredictTryLock");
    public static final RVPredictInterceptor RVPREDICT_TRY_LOCK_TIMEOUT   =
            register(VIRTUAL, JUCL_LOCK, "tryLock", "rvPredictTryLock", J, TimeUnit.class);
    public static final RVPredictInterceptor RVPREDICT_UNLOCK             =
            register(VIRTUAL, JUCL_LOCK, "unlock", "rvPredictUnlock");
    public static final RVPredictInterceptor RVPREDICT_LOCK_NEW_COND      =
            register(VIRTUAL, JUCL_LOCK, "newCondition", "rvPredictLockNewCondition");

    // java.util.concurrent.locks.Condition methods
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT         =
            register(VIRTUAL, JUCL_CONDITION, "await", "rvPredictConditionAwait");
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_TIMEOUT =
            register(VIRTUAL, JUCL_CONDITION, "await", "rvPredictConditionAwait", J, TimeUnit.class);
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_NANOS   =
            register(VIRTUAL, JUCL_CONDITION, "awaitNanos", "rvPredictConditionAwaitNanos", J);
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_UNINTERRUPTIBLY =
            register(VIRTUAL, JUCL_CONDITION, "awaitUninterruptibly", "rvPredictConditionAwaitUninterruptibly");
    public static final RVPredictInterceptor RVPREDICT_COND_AWAIT_UNTIL   =
            register(VIRTUAL, JUCL_CONDITION, "awaitUntil", "rvPredictConditionAwaitUntil", Date.class);

    // java.util.concurrent.locks.ReadWriteLock methods
    public static final RVPredictInterceptor RVPREDICT_RW_LOCK_READ_LOCK  =
            register(VIRTUAL, JUCL_RW_LOCK, "readLock", "rvPredictReadWriteLockReadLock");
    public static final RVPredictInterceptor RVPREDICT_RW_LOCK_WRITE_LOCK =
            register(VIRTUAL, JUCL_RW_LOCK, "writeLock", "rvPredictReadWriteLockWriteLock");

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
                    if (Utility.isSubclassOf(owner, interceptor.classOrInterface)) {
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
     * Represents a RV-Predict runtime library method.
     */
    public static class RVPredictRuntimeMethod {

        /* method name and descriptor are used by ASM to uniquely locate a
         * method in RVPredictRuntime */
        public final String name;

        public final String desc;

        private static RVPredictRuntimeMethod create(String name, Class<?>... parameterTypes) {
            Method method = getMethodHandler(name, parameterTypes);
            return new RVPredictRuntimeMethod(method.getName(), Type.getMethodDescriptor(method));
        }

        private static Method getMethodHandler(String name, Class<?>... parameterTypes) {
            Method method = null;
            try {
                method = RVPredictRuntime.class.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
            return method;
        }

        protected RVPredictRuntimeMethod(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

    }

    /**
     * Represents a special kind of RV-Predict runtime library method, i.e.
     * interceptor, that is bound to some corresponding Java method and can be
     * used to replace it during bytecode transformation.
     * <p>
     * For example, {@link RVPredictRuntime#rvPredictWait(int, Object, long)} is
     * associated with {@link Object#wait(long)}.
     */
    public static class RVPredictInterceptor extends RVPredictRuntimeMethod {

        /**
         * Method type of the associated Java method. Can be
         * {@link RVPredictRuntimeMethods#STATIC}, {@link RVPredictRuntimeMethods#VIRTUAL}, or
         * {@link RVPredictRuntimeMethods#SPECIAL}.
         */
        private final int methodType;

        /**
         * Represents the class or interface in which the associated Java method
         * is declared.
         */
        private final String classOrInterface;

        /**
         * The associated Java method's name.
         */
        private final String methodName;

        /**
         * The parameter type descriptors of the associated Java method.
         */
        public final ImmutableList<String> paramTypeDescs;

        private static RVPredictInterceptor create(int methodType, String classOrInterface,
                String methodName, String interceptorName, Class<?>... parameterTypes)
                throws ClassNotFoundException {
            Class<?>[] interceptorParamTypes;
            int length = parameterTypes.length;
            if (methodType == STATIC) {
                interceptorParamTypes = new Class<?>[length + 1];
                interceptorParamTypes[0] = I;
                System.arraycopy(parameterTypes, 0, interceptorParamTypes, 1, length);
            } else {
                interceptorParamTypes = new Class<?>[length + 2];
                interceptorParamTypes[0] = I;
                interceptorParamTypes[1] = Class.forName(classOrInterface.replace("/", "."));
                System.arraycopy(parameterTypes, 0, interceptorParamTypes, 2, length);
            }

            Method methodHandler = RVPredictRuntimeMethod.getMethodHandler(interceptorName,
                    interceptorParamTypes);
            return new RVPredictInterceptor(interceptorName,
                    Type.getMethodDescriptor(methodHandler), methodType, classOrInterface,
                    methodName, parameterTypes);
        }

        private RVPredictInterceptor(String name, String desc, int opcode, String classOrInterface,
                String methodName, Class<?>[] parameterTypes) {
            super(name, desc);
            this.methodType = opcode;
            this.classOrInterface = classOrInterface;
            this.methodName = methodName;
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (Class<?> cls : parameterTypes) {
                builder.add(Type.getDescriptor(cls));
            }
            paramTypeDescs = builder.build();
        }

        private String getOriginalMethodSig() {
            StringBuilder sb = new StringBuilder(methodName);
            sb.append("(");
            for (String paramTypeDesc : paramTypeDescs) {
                sb.append(paramTypeDesc);
            }
            sb.append(")");
            return sb.toString();
        }

    }

}
