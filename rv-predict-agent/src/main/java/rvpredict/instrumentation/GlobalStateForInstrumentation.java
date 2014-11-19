package rvpredict.instrumentation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

import rvpredict.config.Config;
import rvpredict.logging.DBEngine;
import rvpredict.logging.RecordRT;

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

    public void saveMetaData(DBEngine db) {
        if (!Config.instance.commandLine.agentOnlySharing)
            RecordRT.saveMetaData(db, GlobalStateForInstrumentation.instance
            );
        else {
            // show arrayId
            HashSet<Integer> sharedArrayIds = new HashSet<Integer>();
            for (Integer sid : RecordRT.sharedArrayIds) {
                HashSet<Integer> ids = RecordRT.arrayIdsMap.get(sid);
                sharedArrayIds.addAll(ids);
            }

            sharedVariables = new HashSet<String>();
            // show variableId
            for (Map.Entry<String, Integer> entry : variableIdMap.entrySet()) {
                Integer id = entry.getValue();
                String var = entry.getKey();
                if (RecordRT.sharedVariableIds.contains(id))
                    sharedVariables.add(var);

            }

            sharedArrayLocations = new HashSet<String>();

            for (Integer id : arrayIdMap.keySet()) {
                String var = arrayIdMap.get(id);
                if (sharedArrayIds.contains(id))
                    sharedArrayLocations.add(var);
            }

            if (Config.instance.verbose) {
                int size_var = variableIdMap.entrySet().size();
                int size_array = arrayIdMap.entrySet().size();

                double svar_percent = size_var == 0 ? 0 : ((double) RecordRT.sharedVariableIds
                        .size() / variableIdMap.entrySet().size());
                double sarray_percent = size_array == 0 ? 0
                        : ((double) sharedArrayIds.size() / arrayIdMap.entrySet().size());

                System.out.println("\nSHARED VARIABLE PERCENTAGE: " + svar_percent);
                System.out.println("SHARED ARRAY PERCENTAGE: " + sarray_percent);
            }
            // save the sharedvariable to database??
            RecordRT.saveSharedMetaData(db, sharedVariables, sharedArrayLocations);
        }
    }

    public int getVariableId(String sig) {
        if (variableIdMap.get(sig) == null) {
            synchronized (variableIdMap) {
                if (variableIdMap.get(sig) == null) {
                    int size = variableIdMap.size() + 1;
                    variableIdMap.put(sig, size);
                }
            }
        }
        int sid = variableIdMap.get(sig);

        return sid;
    }

    public void addVolatileVariable(String sig) {
        if (!volatileVariables.contains(sig)) {
            synchronized (volatileVariables) {
                if (!volatileVariables.contains(sig)) {
                    volatileVariables.add(sig);
                }
            }
        }
    }

    public int getLocationId(String sig) {
        if (stmtSigIdMap.get(sig) == null) {
            synchronized (stmtSigIdMap) {
                if (stmtSigIdMap.get(sig) == null) {
                    int size = stmtSigIdMap.size() + 1;
                    stmtSigIdMap.put(sig, size);
                }
            }
        }

        return stmtSigIdMap.get(sig);
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
