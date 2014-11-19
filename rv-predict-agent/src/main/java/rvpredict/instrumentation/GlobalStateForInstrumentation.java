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
    public final ConcurrentHashMap<Integer, String> arrayIdMap = new ConcurrentHashMap<>();

    public final Set<String> volatileVariables = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public final ConcurrentHashMap<String, Integer> stmtSigIdMap = new ConcurrentHashMap<>();
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
         * variableIdMap is a ConcurrentHashMap */
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
        Integer locId = stmtSigIdMap.get(sig);
        if (locId == null) {
            synchronized (stmtSigIdMap) {
                locId = stmtSigIdMap.get(sig);
                if (locId == null) {
                    locId = stmtSigIdMap.size() + 1;
                    stmtSigIdMap.put(sig, locId);
                }
            }
        }

        return locId;
    }

    public int getArrayLocationId(String sig) {
        int id = getLocationId(sig);

        arrayIdMap.put(id, sig);

        return id;
    }

    public String getArrayLocationSig(int id) {
        return arrayIdMap.get(id);
    }
}
