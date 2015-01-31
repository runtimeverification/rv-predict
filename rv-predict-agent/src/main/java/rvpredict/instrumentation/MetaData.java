package rvpredict.instrumentation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
public class MetaData {

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

    public static final ConcurrentHashMap<String, Integer> varSigToVarId = new ConcurrentHashMap<>();
    public static final List<Pair<Integer, String>> unsavedVarIdToVarSig = new ArrayList<>();

    private static final int MAX_NUM_OF_FIELDS = 10000;
    public static final String[] varSigs = new String[MAX_NUM_OF_FIELDS];

    public static final ConcurrentHashMap<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public static final List<Pair<Integer, String>> unsavedLocIdToStmtSig = new ArrayList<>();

    public static final Set<String> volatileVariables = Collections
            .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public static final List<String> unsavedVolatileVariables = new ArrayList<>();

    private MetaData() { }

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
        /* YilongL: the following double-checked locking is correct because
         * varSigToId is a ConcurrentHashMap */
        String sig = getVariableSignature(className, fieldName);
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
                    unsavedLocIdToStmtSig.add(Pair.of(locId, sig));
                }
            }
        }

        return locId;
    }

    private static String getVariableSignature(String className, String fieldName) {
        return className + "." + fieldName;
    }
}
