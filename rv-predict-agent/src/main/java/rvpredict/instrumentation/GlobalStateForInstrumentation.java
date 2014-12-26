package rvpredict.instrumentation;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

public class GlobalStateForInstrumentation {

    private static final Map<String, Set<String>> classNameToFieldNames = new ConcurrentHashMap<>();
    private static final Map<String, String> classNameToSuperclassName = new ConcurrentHashMap<>();

    /**
     * YilongL: Those fields starting with `unsaved` are used for incremental
     * saving of metadata. They are mostly not concurrent data-structures and,
     * thus, synchronize with their main data-structure counterparts, except for
     * {@code unsavedThreadIdToName} because we assume thread Id to be unique in
     * our case.
     */

    public static final ConcurrentHashMap<Long, String> threadIdToName = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, Integer> varSigToId = new ConcurrentHashMap<>();
    private static final String[] varSigs = new String[10000];

    public static final ConcurrentHashMap<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public static final List<Map.Entry<String, Integer>> unsavedStmtSigToLocId = new ArrayList<>();

    public static final Set<String> volatileVariables = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public static final List<String> unsavedVolatileVariables = new ArrayList<>();

    private static final String NATIVE_INTERRUPTED_STATUS_VAR = "$interruptedStatus";

    public static int NATIVE_INTERRUPTED_STATUS_VAR_ID = getVariableId("java.lang.Thread",
            NATIVE_INTERRUPTED_STATUS_VAR);

    private GlobalStateForInstrumentation() { }

    public static void setSuperclass(String className, String superclassName) {
        className = className.replace("/", ".");
        superclassName = superclassName.replace("/", ".");
        if (classNameToSuperclassName.containsKey(className)) {
            System.err.println("[Warning]: class " + className + " is instrumented more than once!");
        } else {
            classNameToSuperclassName.put(className, superclassName);
            classNameToFieldNames.put(className, Sets.<String>newConcurrentHashSet());
        }
    }

    public static void addField(String className, String fieldName) {
        className = className.replace("/", ".");
        classNameToFieldNames.get(className).add(fieldName);
    }

    public static void registerThreadName(long tid, String name) {
        threadIdToName.put(tid, name);
    }

    public static int getVariableId(String className, String fieldName) {
        /* YilongL: the following double-checked locking is correct because
         * varSigToId is a ConcurrentHashMap */
        String sig = getVariableSignature(className, fieldName);
        Integer variableId = varSigToId.get(sig);
        if (variableId == null) {
            synchronized (varSigToId) {
                variableId = varSigToId.get(sig);
                if (variableId == null) {
                    variableId = varSigToId.size() + 1;
                    varSigToId.put(sig, variableId);
                    varSigs[variableId] = sig;
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
                    unsavedVolatileVariables.add(sig);
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
                    unsavedStmtSigToLocId.add(new SimpleEntry<>(sig, locId));
                }
            }
        }

        return locId;
    }

    private static String getVariableSignature(String className, String fieldName) {
        return (className + "." + fieldName).replace("/", ".");
    }

    public static int resolveVariableId(int variableId) {
        String varSig = varSigs[variableId];
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
            // TODO(YilongL): make sure this doesn't happen

//            System.out.println("[Warning]: unable to retrieve field information of class "
//                    + className + "; resolving field " + fieldName);

            return variableId;
        } else {
            assert fieldNames.contains(fieldName);
            return getVariableId(className, fieldName);
        }
    }
}
