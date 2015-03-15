package com.runtimeverification.rvpredict.metadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;

import com.runtimeverification.rvpredict.config.Configuration;

public class Metadata implements Opcodes {

    /**
     * YilongL: Those fields starting with `unsaved` are used for incremental
     * saving of metadata. They are mostly not concurrent data-structures and,
     * thus, synchronize with their main data-structure counterparts, except for
     * {@code unsavedThreadIdToName} because we assume thread Id to be unique in
     * our case.
     */

    public static final Map<String, Integer> varSigToVarId = new ConcurrentHashMap<>();
    public static final List<Pair<Integer, String>> unsavedVarIdToVarSig = new ArrayList<>();

    public static final ArrayList<String> varSigs = new ArrayList<>(); {
        varSigs.ensureCapacity(5000);
        varSigs.add(null);
    }

    public static final Map<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public static final Map<Integer, String> locIdToStmtSig = new HashMap<>();
    public static final List<Pair<Integer, String>> unsavedLocIdToStmtSig = new ArrayList<>();

    public static final Set<Integer> volatileVariableIds = Collections
            .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    public static final List<Integer> unsavedVolatileVariableIds = new ArrayList<>();

    private Metadata() { }

    public static int getVariableId(String className, String fieldName) {
        String sig = getVariableSignature(className, fieldName);
        /* YilongL: the following double-checked locking is correct because
         * varSigToId is a ConcurrentHashMap */
        Integer variableId = varSigToVarId.get(sig);
        if (variableId == null) {
            synchronized (varSigToVarId) {
                variableId = varSigToVarId.get(sig);
                if (variableId == null) {
                    variableId = varSigToVarId.size() + 1;
                    varSigToVarId.put(sig, variableId);
                    varSigs.add(sig);
                    unsavedVarIdToVarSig.add(Pair.of(variableId, sig));
                }
            }
        }

        return variableId;
    }

    public static void addVolatileVariable(String cname, String fname) {
        int varId = Metadata.getVariableId(cname, fname);
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
                        if (Configuration.profile) {
                            locIdToStmtSig.put(locId, sig);
                        }
                        unsavedLocIdToStmtSig.add(Pair.of(locId, sig));
                    }
                }
            }
        }

        return locId;
    }

    public static String getLocationClass(int locId) {
        String stmtSig;
        synchronized (stmtSigToLocId) {
            stmtSig = locIdToStmtSig.get(locId);
        }
        String className;
        if (stmtSig != null) {
            className = stmtSig.substring(0, stmtSig.indexOf("("));
            className = stmtSig.substring(0, className.lastIndexOf("."));
        } else {
            // locId is 0
            className = "N/A";
        }
        return className;
    }

    private static String getVariableSignature(String className, String fieldName) {
        return className + "." + fieldName;
    }

    /**
     * Resolves the declaring class of a given field.
     *
     * @param loader
     *            the loader that can be used to locate the owner class of the
     *            field
     * @param cname
     *            the field's owner class name
     * @param fname
     *            the field's name
     * @return the {@link ClassFile} of the declaring class or {@code null} if
     *         the resolution fails
     */
    public static ClassFile resolveDeclaringClass(ClassLoader loader, String cname, String fname) {
        Deque<String> deque = new ArrayDeque<>();
        deque.add(cname);
        while (!deque.isEmpty()) {
            cname = deque.removeFirst();
            ClassFile classFile = ClassFile.getInstance(loader, cname);
            if (classFile != null) {
                if (classFile.getFieldNames().contains(fname)) {
                    return classFile;
                } else {
                    String superName = classFile.getSuperName();
                    // the superName of any interface is Object
                    if (superName != null && !superName.equals("java/lang/Object")) {
                        deque.addLast(superName);
                    }
                    deque.addAll(classFile.getInterfaces());
                }
            }
        }
        return null;
    }

}
