package com.runtimeverification.rvpredict.metadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import com.runtimeverification.rvpredict.config.Configuration;

public class Metadata implements Opcodes {

    private static final ConcurrentHashMap<String, ClassMetadata> cnameToClassMetadata = new ConcurrentHashMap<>();

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

    public static final Set<Integer> volatileVariableIds = Collections
            .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    public static final List<Integer> unsavedVolatileVariableIds = new ArrayList<>();

    public static final Set<Integer> trackedVariableIds = Collections
            .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    private Metadata() { }

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

    private static void addVolatileVariable(int varId) {
        if (!volatileVariableIds.contains(varId)) {
            synchronized (volatileVariableIds) {
                if (!volatileVariableIds.contains(varId)) {
                    volatileVariableIds.add(varId);
                    if (!Configuration.online) {
                        unsavedVolatileVariableIds.add(varId);
                    }
                }
            }
        }
    }

    public static void trackVariable(String className, String fieldName, int access) {
        int varId = getVariableId(className, fieldName);
        if ((access & ACC_VOLATILE) != 0) {
            Metadata.addVolatileVariable(varId);
        }
        trackedVariableIds.add(varId);
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
        ClassMetadata classMetadata = cnameToClassMetadata.get(className);
        Set<String> fieldNames = null;
        if (classMetadata != null) {
            fieldNames = classMetadata.getFieldNames();
            while (!fieldNames.contains(fieldName)) {
                className = classMetadata.getSuperName();
                if (className == null) {
                    fieldNames = null;
                    break;
                } else {
                    classMetadata = cnameToClassMetadata.get(className);
                    fieldNames = classMetadata.getFieldNames();
                }
            }
        }

        if (fieldNames == null) {
            /* failed to resolve this variable Id */
            // TODO(YilongL): uncomment this and make sure it doesn't happen!
            System.err.println("[Warning]: unable to retrieve information of field " + fieldName
                    + " in class " + className);

            result = fieldId;
        } else {
            assert fieldNames.contains(fieldName);
            result = getVariableId(className, fieldName);
        }
        resolvedFieldId[fieldId] = result;
        return result;
    }

    public static ClassMetadata getOrInitClassMetadata(String cname, byte[] cbuf) {
        return getOrInitClassMetadata(cname, new ClassReader(cbuf));
    }

    public static ClassMetadata getOrInitClassMetadata(String cname, ClassReader cr) {
        ClassMetadata classMetadata = getClassMetadata(cname);
        if (classMetadata != null) {
            return classMetadata;
        }
        classMetadata = ClassMetadata.create(cr);
        cnameToClassMetadata.put(cname, classMetadata);
        return classMetadata;
    }

    public static ClassMetadata getClassMetadata(String cname) {
        return cnameToClassMetadata.get(cname);
    }
}
