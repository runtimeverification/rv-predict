package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LZ4Utils;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.Trace;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("serial")
public class Metadata implements MetadataInterface, Serializable {

    public static final int MAX_NUM_OF_VARIABLES = 1024 * 1024;

    // this should be enough for more than 1 million lines of code
    public static final int MAX_NUM_OF_LOCATIONS = 1024 * 1024;

    private transient final AtomicInteger nextVarId = new AtomicInteger(1);

    private transient final AtomicInteger nextLocId = new AtomicInteger(1);

    private transient final ConcurrentHashMap<String, Integer> varSigToVarId = new ConcurrentHashMap<>();

    private transient final ConcurrentHashMap<String, Integer> locSigToLocId = new ConcurrentHashMap<>();

    private final String[] varIdToVarSig = new String[MAX_NUM_OF_VARIABLES];

    private final String[] locIdToLocSig = new String[MAX_NUM_OF_LOCATIONS];

    private final Set<Integer> volatileVarIds = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private final Map<Long, Pair<Long, Long>> otidToCreationInfo = new ConcurrentHashMap<>();

    private static final Metadata instance = new Metadata();

    /**
     * Note: This method should be used only in a few places and definitely NOT
     * in offline prediction which should get its {@code Metadata} instance from
     * {@link #readFrom(Path, boolean)}.
     *
     * @return a singleton instance of {@code Metadata}.
     */
    public static Metadata singleton() {
        return instance;
    }

    private Metadata() { }

    public int getVariableId(String cname, String fname) {
        String varSig = cname + "." + fname;
        Integer varId = varSigToVarId.get(varSig);
        if (varId == null) {
            varSigToVarId.putIfAbsent(varSig, nextVarId.getAndIncrement());
            varId = varSigToVarId.get(varSig);
            varIdToVarSig[varId] = varSig;
        }
        return varId;
    }

    public String getRaceDataSigHelper(ReadonlyEventInterface e1, ReadonlyEventInterface e2, Configuration config) {
        StringBuilder sb = new StringBuilder();
        if(config.isLLVMPrediction()) {
            long idx = e1.getDataObjectExternalIdentifier();
            if(idx != 0) {
                String sig = getVariableSig(idx).replace("/", ".");
                return "@" + sig;
            } else {
                return "#" + e1.getFieldIdOrArrayIndex();
            }
        } else {
            int idx = e1.getFieldIdOrArrayIndex();
            if (idx < 0) {
                String sig = getVariableSig(-idx).replace("/", ".");
                long object = e1.getDataObjectExternalIdentifier();
                return object == 0 ? "@" + sig : sig;
            }
            return "#" + idx;
        }
    }

    @Override
    public String getRaceDataSig(ReadonlyEventInterface e1,
        ReadonlyEventInterface e2, Trace trace, Configuration config) {
        String locSig = getRaceDataSigHelper(e1, e2, config);
        switch (locSig.charAt(0)) {
            case '#':
                return "array element " + locSig;
            case '@':
                return locSig.substring(1);
            default:
                return "field " + locSig;
        }
    }

    @Override
    public String getVariableSig(long varId) {
        return varIdToVarSig[Math.toIntExact(varId)];
    }

    public class TooManyVariables extends RuntimeException {}

    public void setVariableSig(int varId, String sig) {
        if (varId >= MAX_NUM_OF_VARIABLES) {
            throw new TooManyVariables();
        }
        assert varIdToVarSig[varId] == null;
        varIdToVarSig[varId] = sig;
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

    @Override
    public String getLocationSig(long locId) {
        String sig = locIdToLocSig[Math.toIntExact(locId)];
        if (Configuration.debug && sig == null) {
            System.err.println("getLocationSig(" + locId + ") -> null");
        }
        return sig;
    }

    @Override
    public String getLocationPrefix() {
        return "at ";
    }

    public void setLocationSig(int locId, String sig) {
        assert locIdToLocSig[locId] == null;
        locIdToLocSig[locId] = sig;
    }

    public void addVolatileVariable(String cname, String fname) {
        volatileVarIds.add(getVariableId(cname, fname));
    }

    /**
     * Checks if a memory address is volatile.
     *
     * @param addr
     *            the memory address
     * @return {@code true} if the address is {@code volatile}; otherwise,
     *         {@code false}
     */
    @Override
    public boolean isVolatile(long addr) {
        int varId = (int) addr;
        return varId < 0 && volatileVarIds.contains(-varId);
    }

    @Override
    public void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId) {
        otidToCreationInfo.put(childOTID, Pair.of(parentOTID, locId));
    }

    @Override
    public long getParentOTID(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? 0 : info.getLeft();
    }

    @Override
    public long getOriginalThreadCreationLocId(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? -1 : info.getRight();
    }

    /**
     * Deserializes the {@code Metadata} object stored at the specified location.
     * <p>
     * This method should only be used in offline prediction to obtain an
     * instance of {@code Metadata}.
     *
     * @param path
     *            the location where the metadata is stored
     * @return the {@code Metadata} object
     */
    public static Metadata readFrom(Path path, boolean isCompactTrace) {
        try (ObjectInputStream metadataIS = new ObjectInputStream(
                LZ4Utils.createDecompressionStream(path))) {
            Metadata metadata = (Metadata) metadataIS.readObject();
            return metadata;
        } catch (FileNotFoundException e) {
            System.err.println("Error: Metadata file not found.");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error: Metadata for the logged execution is corrupted.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

}
