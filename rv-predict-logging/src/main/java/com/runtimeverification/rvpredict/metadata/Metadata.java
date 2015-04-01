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
     * thus, synchronize with their main data-structure counterparts.
     */

    public final Map<String, Integer> varSigToVarId = new ConcurrentHashMap<>();
    public final List<Pair<Integer, String>> unsavedVarIdToVarSig = new ArrayList<>();

    public final ArrayList<String> varSigs = new ArrayList<>(); {
        varSigs.ensureCapacity(5000);
        varSigs.add(null);
    }

    public final Map<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();
    public final Map<Integer, String> locIdToStmtSig = new HashMap<>();
    public final List<Pair<Integer, String>> unsavedLocIdToStmtSig = new ArrayList<>();

    public final Set<Integer> volatileVariableIds = Collections
            .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    public final List<Integer> unsavedVolatileVariableIds = new ArrayList<>();

    private static Metadata instance = new Metadata();

    public static Metadata instance() {
        return instance;
    }

    private Metadata() { }

    public int getVariableId(String className, String fieldName) {
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

    public void addVolatileVariable(String cname, String fname) {
        int varId = getVariableId(cname, fname);
        if (!volatileVariableIds.contains(varId)) {
            synchronized (volatileVariableIds) {
                if (!volatileVariableIds.contains(varId)) {
                    volatileVariableIds.add(varId);
                    unsavedVolatileVariableIds.add(varId);
                }
            }
        }
    }

    public int getLocationId(String sig) {
        Integer locId = stmtSigToLocId.get(sig);
        if (locId == null) {
            synchronized (stmtSigToLocId) {
                locId = stmtSigToLocId.get(sig);
                if (locId == null) {
                    locId = stmtSigToLocId.size() + 1;
                    stmtSigToLocId.put(sig, locId);
                    if (Configuration.profile) {
                        locIdToStmtSig.put(locId, sig);
                    }
                    unsavedLocIdToStmtSig.add(Pair.of(locId, sig));
                }
            }
        }

        return locId;
    }

    public String getLocationClass(int locId) {
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

    private String getVariableSignature(String className, String fieldName) {
        return className + "." + fieldName;
    }

}
