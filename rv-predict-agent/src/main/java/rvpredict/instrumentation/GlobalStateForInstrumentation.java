package rvpredict.instrumentation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalStateForInstrumentation {
    public static GlobalStateForInstrumentation instance = new GlobalStateForInstrumentation();

    // can be computed during offline analysis
    public final ConcurrentHashMap<Long, String> threadIdToName = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Integer> varSigToId = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Integer> stmtSigToLocId = new ConcurrentHashMap<>();

    public final Set<String> volatileVariables = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private HashSet<String> sharedVariables;
    private HashSet<String> sharedArrayLocations;

    public void registerThreadName(long tid, String name) {
        threadIdToName.put(tid, name);
    }

    public boolean isVariableShared(String sig) {
        return sharedVariables == null || sharedVariables.contains(sig);
    }

    public boolean shouldInstrumentArray(String loc) {
        return sharedArrayLocations == null || sharedArrayLocations.contains(loc);
    }

    public void setSharedArrayLocations(HashSet<String> locs) {
        this.sharedArrayLocations = locs;
    }

    public void setSharedVariables(HashSet<String> locs) {
        this.sharedVariables = locs;
    }

    public int getVariableId(String sig) {
        /* YilongL: the following double-checked locking is correct because
         * varSigToId is a ConcurrentHashMap */
        Integer variableId = varSigToId.get(sig);
        if (variableId == null) {
            synchronized (varSigToId) {
                variableId = varSigToId.get(sig);
                if (variableId == null) {
                    variableId = varSigToId.size() + 1;
                    varSigToId.put(sig, variableId);
                }
            }
        }

        return variableId;
    }

    public void addVolatileVariable(String sig) {
        volatileVariables.add(sig);
    }

    public int getLocationId(String sig) {
        Integer locId = stmtSigToLocId.get(sig);
        if (locId == null) {
            synchronized (stmtSigToLocId) {
                locId = stmtSigToLocId.get(sig);
                if (locId == null) {
                    locId = stmtSigToLocId.size() + 1;
                    stmtSigToLocId.put(sig, locId);
                }
            }
        }

        return locId;
    }
}
