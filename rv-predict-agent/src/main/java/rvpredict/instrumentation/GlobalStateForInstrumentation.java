package rvpredict.instrumentation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

public class GlobalStateForInstrumentation {
    public static GlobalStateForInstrumentation instance = new GlobalStateForInstrumentation();

    // can be computed during offline analysis
    public ConcurrentHashMap<Long, String> threadTidNameMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> variableIdMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, String> arrayIdMap = new ConcurrentHashMap<>();

    public Set<String> volatileVariables = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public ConcurrentHashMap<String, Integer> stmtSigIdMap = new ConcurrentHashMap<>();
    private HashSet<String> sharedVariables = Sets.newHashSet();
    private HashSet<String> sharedArrayLocations = Sets.newHashSet();

    public void registerThreadName(long tid, String name) {
        threadTidNameMap.put(tid, name);
    }

    public boolean isVariableShared(String sig) {
        return sharedVariables.contains(sig);
    }

    public boolean shouldInstrumentArray(String loc) {
        return sharedArrayLocations.contains(loc);
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
        Integer variableId = variableIdMap.get(sig);
        if (variableId == null) {
            synchronized (variableIdMap) {
                variableId = variableIdMap.get(sig);
                if (variableId == null) {
                    variableId = variableIdMap.size() + 1;
                    variableIdMap.put(sig, variableId);
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
