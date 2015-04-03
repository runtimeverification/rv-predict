package com.runtimeverification.rvpredict.metadata;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Metadata {

    public static final int MAX_NUM_OF_VARIABLES = 1024 * 128;

    // this should be enough for more than 1 million lines of code
    public static final int MAX_NUM_OF_LOCATIONS = 1024 * 1024;

    private final AtomicInteger nextVarId = new AtomicInteger(1);

    private final AtomicInteger nextLocId = new AtomicInteger(1);

    private final ConcurrentHashMap<String, Integer> varSigToVarId = new ConcurrentHashMap<>();

    private final String[] varIdToVarSig = new String[MAX_NUM_OF_VARIABLES];

    private final ConcurrentHashMap<String, Integer> locSigToLocId = new ConcurrentHashMap<>();

    private final String[] locIdToLocSig = new String[MAX_NUM_OF_LOCATIONS];

    protected final Set<Integer> volatileVarIds = Collections
            .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    private static Metadata instance = new Metadata();

    public static Metadata singleton() {
        return instance;
    }

    public int getVariableId(String cname, String fname) {
        String varSig = getVariableSig(cname, fname);
        Integer varId = varSigToVarId.get(varSig);
        if (varId == null) {
            varSigToVarId.putIfAbsent(varSig, nextVarId.getAndIncrement());
            varId = varSigToVarId.get(varSig);
            varIdToVarSig[varId] = varSig;
        }
        return varId;
    }

    public void addVolatileVariable(String cname, String fname) {
        volatileVarIds.add(getVariableId(cname, fname));
    }

    public int getLocationId(String locSig) {
        Integer locId = locSigToLocId.get(locSig);
        if (locId == null) {
            locSigToLocId.putIfAbsent(locSig, nextLocId.getAndIncrement());
            locId = locSigToLocId.get(locSig);
            locIdToLocSig[locId] = locSig;
        }
        return locId;
    }

    public String getLocationSig(int locId) {
        return locIdToLocSig[locId];
    }

    private String getVariableSig(String cname, String fname) {
        return cname + "." + fname;
    }

    public void writeTo(ObjectOutputStream os) throws IOException {
        os.writeObject(new HashSet<>(volatileVarIds));
        os.writeObject(Arrays.copyOf(varIdToVarSig, nextVarId.get()));
        os.writeObject(Arrays.copyOf(locIdToLocSig, nextLocId.get()));
        os.close();
    }

    public static Object[] readFrom(ObjectInputStream is) throws ClassNotFoundException, IOException {
        Object[] objects = new Object[3];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = is.readObject();
        }
        return objects;
    }
}
