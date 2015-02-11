package com.runtimeverification.rvpredict.instrumentation;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.Opcodes;

import com.runtimeverification.rvpredict.instrumentation.transformer.MethodTransformer;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

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

    public static final RVPredictRuntimeMethod LOG_FIELD_ACCESS  =  init("logFieldAcc", O, J, I, Z, I);
    public static final RVPredictRuntimeMethod LOG_ARRAY_ACCESS  =  init("logArrayAcc", O, I, J, Z, I);
    public static final RVPredictRuntimeMethod LOG_MONITOR_ENTER =  init("logMonitorEnter", O, I);
    public static final RVPredictRuntimeMethod LOG_MONITOR_EXIT  =  init("logMonitorExit", O, I);
    public static final RVPredictRuntimeMethod LOG_BRANCH        =  init("logBranch", I);

    /*
     * Some useful constants.
     */
    public static final int STATIC     =   Opcodes.INVOKESTATIC;
    public static final int VIRTUAL    =   Opcodes.INVOKEVIRTUAL;
    public static final int INTERFACE  =   Opcodes.INVOKEINTERFACE;
    public static final int SPECIAL    =   Opcodes.INVOKESPECIAL;
    private static final String JL_OBJECT       =   "java/lang/Object";
    private static final String JL_THREAD       =   "java/lang/Thread";
    private static final String JL_SYSTEM       =   "java/lang/System";
    private static final String JL_ITERABLE     =   "java/lang/Iterable";
    private static final String JU_ITERATOR     =   "java/util/Iterator";
    private static final String JU_COLLECTION   =   "java/util/Collection";
    private static final String JU_MAP          =   "java/util/Map";
    private static final String JUCL_LOCK       =   "java/util/concurrent/locks/Lock";
    private static final String JUCL_CONDITION  =   "java/util/concurrent/locks/Condition";
    private static final String JUCL_RW_LOCK    =   "java/util/concurrent/locks/ReadWriteLock";
    private static final String JUCL_AQS        =   "java/util/concurrent/locks/AbstractQueuedSynchronizer";
    private static final String JUCA_ATOMIC_BOOL    =   "java/util/concurrent/atomic/AtomicBoolean";

    /*
     * Map from method signature to possible {@link RVPredictInterceptor}'s.
     */
    private static final Map<String, List<RVPredictInterceptor>> STATIC_METHOD_INTERCEPTION = Maps.newHashMap();
    private static final Map<String, List<RVPredictInterceptor>> SPECIAL_METHOD_INTERCEPTION = Maps.newHashMap();
    private static final Map<String, List<RVPredictInterceptor>> VIRTUAL_METHOD_INTERCEPTION = Maps.newHashMap();

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

    // java.lang.Iterable methods
    public static final RVPredictInterceptor RVPREDICT_ITERABLE_ITERATOR  =
            register(INTERFACE, JL_ITERABLE, "iterator", "rvPredictIterableGetIterator");

    // java.util.Iterator methods
    public static final RVPredictInterceptor RVPREDICT_ITERATOR_HAS_NEXT  =
            register(INTERFACE, JU_ITERATOR, "hasNext", "rvPredictIteratorHasNext");
    public static final RVPredictInterceptor RVPREDICT_ITERATOR_NEXT      =
            register(INTERFACE, JU_ITERATOR, "next", "rvPredictIteratorNext");
    public static final RVPredictInterceptor RVPREDICT_ITERATOR_REMOVE    =
            register(INTERFACE, JU_ITERATOR, "remove", "rvPredictIteratorRemove");

    // java.util.Collection methods
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_ADD     =
            register(INTERFACE, JU_COLLECTION, "add", "rvPredictCollectionAdd", O);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_ADD_ALL =
            register(INTERFACE, JU_COLLECTION, "addAll", "rvPredictCollectionAddAll", Collection.class);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_REMOVE  =
            register(INTERFACE, JU_COLLECTION, "remove", "rvPredictCollectionRemove", O);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_REMOVE_ALL =
            register(INTERFACE, JU_COLLECTION, "removeAll", "rvPredictCollectionRemoveAll", Collection.class);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_RETAIN_ALL =
            register(INTERFACE, JU_COLLECTION, "retainAll", "rvPredictCollectionRetainAll", Collection.class);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_CONTAINS  =
            register(INTERFACE, JU_COLLECTION, "contains", "rvPredictCollectionContains", O);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_CONTAINS_ALL =
            register(INTERFACE, JU_COLLECTION, "containsAll", "rvPredictCollectionContainsAll", Collection.class);
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_CLEAR   =
            register(INTERFACE, JU_COLLECTION, "clear", "rvPredictCollectionClear");
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_TOARRAY =
            register(INTERFACE, JU_COLLECTION, "toArray", "rvPredictCollectionToArray");
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_TOARRAY_GENERIC =
            register(INTERFACE, JU_COLLECTION, "toArray", "rvPredictCollectionToArray", Object[].class);

    // java.util.Map methods
    public static final RVPredictInterceptor RVPREDICT_MAP_GET            =
            register(INTERFACE, JU_MAP, "get", "rvPredictMapGet", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT            =
            register(INTERFACE, JU_MAP, "put", "rvPredictMapPut", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT_ALL        =
            register(INTERFACE, JU_MAP, "putAll", "rvPredictMapPutAll", Map.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_REMOVE         =
            register(INTERFACE, JU_MAP, "remove", "rvPredictMapRemove", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_CONTAINS_KEY   =
            register(INTERFACE, JU_MAP, "containsKey", "rvPredictMapContainsKey", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_CONTAINS_VAL   =
            register(INTERFACE, JU_MAP, "containsValue", "rvPredictMapContainsValue", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_CLEAR          =
            register(INTERFACE, JU_MAP, "clear", "rvPredictMapClear");
    public static final RVPredictInterceptor RVPREDICT_MAP_ENTRY_SET      =
            register(INTERFACE, JU_MAP, "entrySet", "rvPredictMapEntrySet");
    public static final RVPredictInterceptor RVPREDICT_MAP_KEY_SET        =
            register(INTERFACE, JU_MAP, "keySet", "rvPredictMapKeySet");
    public static final RVPredictInterceptor RVPREDICT_MAP_VALUES         =
            register(INTERFACE, JU_MAP, "values", "rvPredictMapValues");

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
            Map<String, List<RVPredictInterceptor>> interceptorTable = getInterceptorTable(methodType);
            List<RVPredictInterceptor> interceptors = interceptorTable.get(interceptor
                    .getOriginalMethodSig());
            if (interceptors == null) {
                interceptors = Lists.newArrayList();
                interceptorTable.put(interceptor.getOriginalMethodSig(), interceptors);
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
     * @param opcode
     *            either INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
     *            INVOKEINTERFACE
     * @param owner
     *            the class/interface name of the object whose member method is
     *            invoked or the class name of the static method
     * @param methodSig
     *            the signature of the invoked method
     * @param loader
     *            the defining loader of the class being transformed by
     *            {@link MethodTransformer#visitMethodInsn(int, String, String, String, boolean)}
     *            , may be null if it is the bootstrap class loader or unknown
     * @param itf
     * @return the {@link RVPredictInterceptor} if successful or {@code null} if
     *         no suitable interceptor found
     */
    public static RVPredictInterceptor lookup(int opcode, String owner, String methodSig,
            ClassLoader loader, boolean itf) {
        /*
         * TODO(YilongL): figure out how to the deal with `itf' introduced for Java 8
         * http://stackoverflow.com/questions/24510785/explanation-of-itf-parameter-of-visitmethodinsn-in-asm-5
         */
        List<RVPredictInterceptor> interceptors = getInterceptorTable(opcode).get(methodSig);
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
                        assert interceptor.methodType == opcode;
                        return interceptor;
                    }
                    break;
                case VIRTUAL:
                case INTERFACE:
                    if (InstrumentationUtils.isSubclassOf(loader, owner, interceptor.classOrInterface)) {
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
     * Retrieves the method interceptor table corresponding to a given method
     * invocation opcode.
     * <p>
     * This prevents us from intercepting method invocation whose opcode is
     * different from the interceptor's declared method type.
     * <p>
     * For example, we don't want to intercept interface/virtual method that is
     * called with {@code INVOKESPECIAL} because it means that we haven't
     * correctly exclude classes/interfaces we are mocking from instrumentation.
     * E.g., if we fail to exclude an implementation of the {@code Map}
     * interface which calls {@code super.put(...)} from its own put method, it
     * would cause infinite recursion inside our interceptor method at runtime.
     *
     * @param opcode
     *            the method invocation opcode
     * @return the method interceptor table
     */
    private static Map<String, List<RVPredictInterceptor>> getInterceptorTable(int opcode) {
        switch (opcode) {
        case Opcodes.INVOKESTATIC:
            return STATIC_METHOD_INTERCEPTION;
        case Opcodes.INVOKESPECIAL:
            return SPECIAL_METHOD_INTERCEPTION;
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKEINTERFACE:
            return VIRTUAL_METHOD_INTERCEPTION;
        default:
            assert false : "unreachable";
            return null;
        }
    }

}
