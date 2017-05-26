package com.runtimeverification.rvpredict.metadata;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompactMetadata implements MetadataInterface {
    private final Map<Long, Pair<Long, Long>> otidToCreationInfo = new ConcurrentHashMap<>();

    @Override
    public String getLocationSig(long locationId) {
        return String.format("{0x%016x}", locationId);
    }

    @Override
    public void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId) {
        otidToCreationInfo.put(childOTID, Pair.of(parentOTID, locId));
    }

    @Override
    public long getOriginalThreadCreationLocId(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? -1 : info.getRight();
    }

    @Override
    public long getParentOTID(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? 0 : info.getLeft();
    }

    @Override
    public String getVariableSig(long idx) {
        return String.format("[0x%016x]", idx);
    }

    @Override
    public boolean isVolatile(long addressForVolatileCheck) {
        return false;
    }
}
