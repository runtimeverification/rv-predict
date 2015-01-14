package rvpredict.instrumentation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetaData {

    public static final Map<String, Set<String>> classNameToFieldNames = new ConcurrentHashMap<>();
    public static final Map<String, String> classNameToSuperclassName = new ConcurrentHashMap<>();

    /**
     * YilongL: Those fields starting with `unsaved` are used for incremental
     * saving of metadata. They are mostly not concurrent data-structures and,
     * thus, synchronize with their main data-structure counterparts, except for
     * {@code unsavedThreadIdToName} because we assume thread Id to be unique in
     * our case.
     */

    public static final ConcurrentHashMap<String, Integer> varSigToId = new ConcurrentHashMap<>();

    private static final int MAX_NUM_OF_FIELDS = 10000;

    public static final String[] varSigs = new String[MAX_NUM_OF_FIELDS];

    public static final ConcurrentHashMap<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public static final List<Map.Entry<String, Integer>> unsavedStmtSigToLocId = new ArrayList<>();

    public static final Set<String> volatileVariables = Collections
            .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public static final List<String> unsavedVolatileVariables = new ArrayList<>();

    private MetaData() { }

    public static void setSuperclass(String className, String superclassName) {
        className = className.replace("/", ".");
        superclassName = superclassName.replace("/", ".");
        if (classNameToSuperclassName.containsKey(className)) {
            System.err.println("[Warning]: class " + className + " is instrumented more than once!");
        } else {
            classNameToSuperclassName.put(className, superclassName);
            classNameToFieldNames.put(className,
                    Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
        }
    }

    public static void addField(String className, String fieldName) {
        className = className.replace("/", ".");
        classNameToFieldNames.get(className).add(fieldName);
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
                    unsavedStmtSigToLocId.add(new AbstractMap.SimpleImmutableEntry(sig, locId));
                }
            }
        }

        return locId;
    }

    private static String getVariableSignature(String className, String fieldName) {
        return (className + "." + fieldName).replace("/", ".");
    }
}
