package com.runtimeverification.rvpredict.instrumentation;

import com.runtimeverification.rvpredict.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class Metadata {

    public static final ConcurrentHashMap<String, Set<String>> classNameToFieldNames = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, String[]> classNameToInterfaceNames = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, String> classNameToSuperclassName = new ConcurrentHashMap<>();

    /**
     * YilongL: Those fields starting with `unsaved` are used for incremental
     * saving of metadata. They are mostly not concurrent data-structures and,
     * thus, synchronize with their main data-structure counterparts, except for
     * {@code unsavedThreadIdToName} because we assume thread Id to be unique in
     * our case.
     */

    public static final Map<String, Integer> varSigToVarId = new ConcurrentHashMap<>();
    public static final List<Pair<Integer, String>> unsavedVarIdToVarSig = new ArrayList<>();

    public static final int MAX_NUM_OF_FIELDS = 10000;
    public static final String[] varSigs = new String[MAX_NUM_OF_FIELDS];
    public static final int[] resolvedFieldId = new int[Metadata.MAX_NUM_OF_FIELDS];

    public static final Map<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public static final Map<Integer, String> locIdToStmtSig = new HashMap<>();
    public static final List<Pair<Integer, String>> unsavedLocIdToStmtSig = new ArrayList<>();

    public static final Set<String> volatileVariables = Collections
            .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public static final Set<Integer> volatileFieldIds = new HashSet<>();
    public static final List<String> unsavedVolatileVariables = new ArrayList<>();

    private Metadata() { }

    public static void setSuperclass(String className, String superclassName) {
        if ("java/lang/Object".equals(className)) {
            // the only case where superclassName is null
            return;
        }

        String value = classNameToSuperclassName.putIfAbsent(className, superclassName);
        if (value != null && !value.equals(superclassName)) {
            System.err.println("[Warning]: attempts to reset the superclass name of " + className);
        }
    }

    public static void setInterfaces(String className, String[] interfaces) {
        if (interfaces == null) {
            interfaces = new String[0];
        }
        String[] value = classNameToInterfaceNames.putIfAbsent(className,
                Arrays.copyOf(interfaces, interfaces.length));
        if (value != null && !Arrays.deepEquals(value, interfaces)) {
            System.err.println("[Warning]: attempts to reset the interfaces of " + className);
        }
    }

    public static void addField(String className, String fieldName) {
        classNameToFieldNames.putIfAbsent(className,
                Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
        classNameToFieldNames.get(className).add(fieldName);
    }

    public static int getVariableId(String className, String fieldName) {
        return getVariableId(getVariableSignature(className, fieldName));
    }

    public static int getVariableId(String sig) {
        /* YilongL: the following double-checked locking is correct because
         * varSigToId is a ConcurrentHashMap */
        Integer variableId = varSigToVarId.get(sig);
        if (variableId == null) {
            synchronized (varSigToVarId) {
                variableId = varSigToVarId.get(sig);
                if (variableId == null) {
                    variableId = varSigToVarId.size() + 1;
                    varSigToVarId.put(sig, variableId);
                    varSigs[variableId] = sig;
                    unsavedVarIdToVarSig.add(Pair.of(variableId, sig));
                }
            }
        }

        return variableId;
    }

    public static void addVolatileVariable(String className, String fieldName) {
        String sig = getVariableSignature(className, fieldName);
        if (!volatileVariables.contains(sig)) {
            synchronized (volatileVariables) {
                if (!volatileVariables.contains(sig)) {
                    volatileVariables.add(sig);
                    if (Configuration.online) {
                        volatileFieldIds.add(getVariableId(className, fieldName));
                    } else {
                        unsavedVolatileVariables.add(sig);
                    }
                }
            }
        }
    }

    public static int getLocationId(String sig) {
        Integer locId = stmtSigToLocId.get(sig);
        if (locId == null) {
            synchronized (stmtSigToLocId) {
                locId = stmtSigToLocId.get(sig);
                if (locId == null) {
                    locId = stmtSigToLocId.size() + 1;
                    stmtSigToLocId.put(sig, locId);
                    if (Configuration.online) {
                        locIdToStmtSig.put(locId, sig);
                    } else {
                        unsavedLocIdToStmtSig.add(Pair.of(locId, sig));
                    }
                }
            }
        }

        return locId;
    }

    private static String getVariableSignature(String className, String fieldName) {
        return className + "." + fieldName;
    }

    /**
     * Performs field resolution as specified in the JVM specification $5.4.3.2
     * except that we do it at run-time instead of load-time because it's easier
     * to implement. The result is cached to reduce runtime overhead.
     */
    public static int resolveFieldId(int fieldId) {
        int result = resolvedFieldId[fieldId];
        if (result > 0) {
            return result;
        }

        String varSig = varSigs[fieldId];
        int idx = varSig.lastIndexOf(".");
        String className = varSig.substring(0, idx);
        String fieldName = varSig.substring(idx + 1);
        Set<String> fieldNames = classNameToFieldNames.get(className);
        while (fieldNames != null && !fieldNames.contains(fieldName)) {
            className = classNameToSuperclassName.get(className);
            if (className == null) {
                fieldNames = null;
                break;
            }

            fieldNames = classNameToFieldNames.get(className);
        }

        if (fieldNames == null) {
            /* failed to resolve this variable Id */
            // TODO(YilongL): uncomment this and make sure it doesn't happen!
//            System.err.println("[Warning]: unable to retrieve field information of class "
//                    + className + "; resolving field " + fieldName);

            result = fieldId;
        } else {
            assert fieldNames.contains(fieldName);
            result = getVariableId(className, fieldName);
        }
        resolvedFieldId[fieldId] = result;
        return result;
    }
}
