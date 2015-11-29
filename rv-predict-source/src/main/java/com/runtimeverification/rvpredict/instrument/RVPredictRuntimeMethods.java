package com.runtimeverification.rvpredict.instrument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;

import com.runtimeverification.rvpredict.instrument.transformer.MethodTransformer;
import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

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
    public static final RVPredictRuntimeMethod LOG_CLINIT_ENTER  =  init("logClassInitializerEnter");
    public static final RVPredictRuntimeMethod LOG_CLINIT_EXIT   =  init("logClassInitializerExit");
    public static final RVPredictRuntimeMethod LOG_MONITOR_ENTER =  init("logMonitorEnter", O, I);
    public static final RVPredictRuntimeMethod LOG_MONITOR_EXIT  =  init("logMonitorExit", O, I);
    public static final RVPredictRuntimeMethod LOG_INVOKE_METHOD =  init("logInvokeMethod", I);
    public static final RVPredictRuntimeMethod LOG_FINISH_METHOD =  init("logFinishMethod", I);

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
    private static final String JU_LIST         =   "java/util/List";
    private static final String JU_LISTITERATOR =   "java/util/ListIterator";
    private static final String JU_QUEUE        =   "java/util/Queue";
    private static final String JU_DEQUE        =   "java/util/Deque";
    private static final String JU_MAP          =   "java/util/Map";
    private static final String JU_STACK        =   "java/util/Stack";
    private static final String JU_COLLECTIONS  =   "java/util/Collections";

    /*
     * Map from method signature to possible {@link RVPredictInterceptor}'s.
     */
    private static final Map<String, List<RVPredictInterceptor>> STATIC_METHOD_INTERCEPTION = new HashMap<>();
    private static final Map<String, List<RVPredictInterceptor>> SPECIAL_METHOD_INTERCEPTION = new HashMap<>();
    private static final Map<String, List<RVPredictInterceptor>> VIRTUAL_METHOD_INTERCEPTION = new HashMap<>();

    // Thread methods
    public static final RVPredictInterceptor RVPREDICT_START0             =
            register(SPECIAL, JL_THREAD, "start0", "rvPredictStart0");
    public static final RVPredictInterceptor RVPREDICT_IS_ALIVE           =
            register(VIRTUAL, JL_THREAD, "isAlive", "rvPredictIsAlive");
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

    // java.util.List
    public static final RVPredictInterceptor RVPREDICT_LIST_LISTITERATOR  =
            register(INTERFACE, JU_LIST, "listIterator", "rvPredictListGetListIterator");
    public static final RVPredictInterceptor RVPREDICT_LIST_LISTITERATOR_IDX =
            register(INTERFACE, JU_LIST, "listIterator", "rvPredictListGetListIterator", I);
    public static final RVPredictInterceptor RVPREDICT_LIST_GET           =
            register(INTERFACE, JU_LIST, "get", "rvPredictListGet", I);
    public static final RVPredictInterceptor RVPREDICT_LIST_SET           =
            register(INTERFACE, JU_LIST, "set", "rvPredictListSet", I, O);
    public static final RVPredictInterceptor RVPREDICT_LIST_ADD           =
            register(INTERFACE, JU_LIST, "add", "rvPredictListAdd", I, O);
    public static final RVPredictInterceptor RVPREDICT_LIST_REMOVE        =
            register(INTERFACE, JU_LIST, "remove", "rvPredictListRemove", I);
    public static final RVPredictInterceptor RVPREDICT_LIST_INDEX_OF      =
            register(INTERFACE, JU_LIST, "indexOf", "rvPredictListIndexOf", O);

    // java.util.ListIterator methods
    public static final RVPredictInterceptor RVPREDICT_LISTITERATOR_HAS_PREVIOUS  =
            register(INTERFACE, JU_LISTITERATOR, "hasPrevious", "rvPredictListIteratorHasPrevious");
    public static final RVPredictInterceptor RVPREDICT_LISTITERATOR_PREVIOUS  =
            register(INTERFACE, JU_LISTITERATOR, "previous", "rvPredictListIteratorPrevious");
    public static final RVPredictInterceptor RVPREDICT_LISTITERATOR_ADD  =
            register(INTERFACE, JU_LISTITERATOR, "add", "rvPredictListIteratorAdd", O);
    public static final RVPredictInterceptor RVPREDICT_LISTITERATOR_SET  =
            register(INTERFACE, JU_LISTITERATOR, "set", "rvPredictListIteratorSet", O);

    // java.util.Collection methods
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_SIZE    =
            register(INTERFACE, JU_COLLECTION, "size", "rvPredictCollectionSize");
    public static final RVPredictInterceptor RVPREDICT_COLLECTION_ISEMPTY =
            register(INTERFACE, JU_COLLECTION, "isEmpty", "rvPredictCollectionIsEmpty");
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

    // java.util.Queue
    public static final RVPredictInterceptor RVPREDICT_QUEUE_OFFER      =
            register(INTERFACE, JU_QUEUE, "offer", "rvPredictQueueOffer", O);
    public static final RVPredictInterceptor RVPREDICT_QUEUE_REMOVE     =
            register(INTERFACE, JU_QUEUE, "remove", "rvPredictQueueRemove");
    public static final RVPredictInterceptor RVPREDICT_QUEUE_POLL       =
            register(INTERFACE, JU_QUEUE, "poll", "rvPredictQueuePoll");
    public static final RVPredictInterceptor RVPREDICT_QUEUE_ELEMENT    =
            register(INTERFACE, JU_QUEUE, "element", "rvPredictQueueElement");
    public static final RVPredictInterceptor RVPREDICT_QUEUE_PEEK       =
            register(INTERFACE, JU_QUEUE, "peek", "rvPredictQueuePeek");

    // java.util.Deque (far from complete...)
    public static final RVPredictInterceptor RVPREDICT_DEQUE_ADD_FIRST      =
            register(INTERFACE, JU_DEQUE, "addFirst", "rvPredictDequeAddFirst", O);
    public static final RVPredictInterceptor RVPREDICT_DEQUE_ADD_LAST       =
            register(INTERFACE, JU_DEQUE, "addLast", "rvPredictDequeAddLast", O);
    public static final RVPredictInterceptor RVPREDICT_DEQUE_OFFER_FIRST    =
            register(INTERFACE, JU_DEQUE, "offerFirst", "rvPredictDequeOfferFirst", O);
    public static final RVPredictInterceptor RVPREDICT_DEQUE_OFFER_LAST     =
            register(INTERFACE, JU_DEQUE, "offerLast", "rvPredictDequeOfferLast", O);
    public static final RVPredictInterceptor RVPREDICT_DEQUE_REMOVE_FIRST   =
            register(INTERFACE, JU_DEQUE, "removeFirst", "rvPredictDequeRemoveFirst");
    public static final RVPredictInterceptor RVPREDICT_DEQUE_REMOVE_LAST    =
            register(INTERFACE, JU_DEQUE, "removeLast", "rvPredictDequeRemoveLast");
    public static final RVPredictInterceptor RVPREDICT_DEQUE_GET_FIRST      =
            register(INTERFACE, JU_DEQUE, "getFirst", "rvPredictDequeGetFirst");
    public static final RVPredictInterceptor RVPREDICT_DEQUE_GET_LAST    =
            register(INTERFACE, JU_DEQUE, "getLast", "rvPredictDequeGetLast");

    // java.util.Map methods
    public static final RVPredictInterceptor RVPREDICT_MAP_CLEAR          =
            register(INTERFACE, JU_MAP, "clear", "rvPredictMapClear");
    public static final RVPredictInterceptor RVPREDICT_MAP_COMPUTE        =
            register(INTERFACE, JU_MAP, "compute", "rvPredictMapCompute", O, BiFunction.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_COMPUTE_IF_ABSENT =
            register(INTERFACE, JU_MAP, "computeIfAbsent", "rvPredictMapComputeIfAbsent", O, Function.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_COMPUTE_IF_PRESENT =
            register(INTERFACE, JU_MAP, "computeIfPresent", "rvPredictMapComputeIfPresent", O, BiFunction.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_CONTAINS_KEY   =
            register(INTERFACE, JU_MAP, "containsKey", "rvPredictMapContainsKey", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_CONTAINS_VAL   =
            register(INTERFACE, JU_MAP, "containsValue", "rvPredictMapContainsValue", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_ENTRY_SET      =
            register(INTERFACE, JU_MAP, "entrySet", "rvPredictMapEntrySet");
    public static final RVPredictInterceptor RVPREDICT_MAP_FOR_EACH       =
            register(INTERFACE, JU_MAP, "forEach", "rvPredictMapForEach", BiConsumer.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_GET            =
            register(INTERFACE, JU_MAP, "get", "rvPredictMapGet", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_GET_OR_DEFAULT =
            register(INTERFACE, JU_MAP, "getOrDefault", "rvPredictMapGetOrDefault", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_KEY_SET        =
            register(INTERFACE, JU_MAP, "keySet", "rvPredictMapKeySet");
    public static final RVPredictInterceptor RVPREDICT_MAP_MERGE          =
            register(INTERFACE, JU_MAP, "merge", "rvPredictMapMerge", O, O, BiFunction.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT            =
            register(INTERFACE, JU_MAP, "put", "rvPredictMapPut", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT_ALL        =
            register(INTERFACE, JU_MAP, "putAll", "rvPredictMapPutAll", Map.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_PUT_IF_ABSENT  =
            register(INTERFACE, JU_MAP, "putIfAbsent", "rvPredictMapPutIfAbsent", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_REMOVE_KEY     =
            register(INTERFACE, JU_MAP, "remove", "rvPredictMapRemove", O);
    public static final RVPredictInterceptor RVPREDICT_MAP_REMOVE_ENTRY   =
            register(INTERFACE, JU_MAP, "remove", "rvPredictMapRemove", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_REPLACE        =
            register(INTERFACE, JU_MAP, "replace", "rvPredictMapReplace", O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_REPLACE2       =
            register(INTERFACE, JU_MAP, "replace", "rvPredictMapReplace", O, O, O);
    public static final RVPredictInterceptor RVPREDICT_MAP_REPLACE_ALL    =
            register(INTERFACE, JU_MAP, "replaceAll", "rvPredictMapReplaceAll", BiFunction.class);
    public static final RVPredictInterceptor RVPREDICT_MAP_VALUES         =
            register(INTERFACE, JU_MAP, "values", "rvPredictMapValues");

    // TODO: java.util.Vector methods

    // java.util.Stack methods
    public static final RVPredictInterceptor RVPREFDICT_STACK_PUSH        =
            register(VIRTUAL, JU_STACK, "push", "rvPredictStackPush", O);
    public static final RVPredictInterceptor RVPREFDICT_STACK_POP         =
            register(VIRTUAL, JU_STACK, "pop", "rvPredictStackPop");
    public static final RVPredictInterceptor RVPREFDICT_STACK_Peek        =
            register(VIRTUAL, JU_STACK, "peek", "rvPredictStackPeek");

    // java.util.Collections wrapper methods
    public static final RVPredictInterceptor RVPREDICT_SYNC_COLLECTION    =
            register(STATIC, JU_COLLECTIONS, "synchronizedCollection", "rvPredictSynchronizedCollection", Collection.class);
    public static final RVPredictInterceptor RVPREDICT_SYNC_MAP           =
            register(STATIC, JU_COLLECTIONS, "synchronizedMap", "rvPredictSynchronizedMap", Map.class);
    public static final RVPredictInterceptor RVPREDICT_SET_FROM_MAP       =
            register(STATIC, JU_COLLECTIONS, "newSetFromMap", "rvPredictNewSetFromMap", Map.class);

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
                interceptors = new ArrayList<>();
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
                    if (ClassFile.isSubtypeOf(loader, owner, interceptor.classOrInterface)) {
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
