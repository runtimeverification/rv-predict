package com.runtimeverification.rvpredict.metadata;

public interface MetadataInterface {
    String getLocationSig(long locationId);
    void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId);
    long getOriginalThreadCreationLocId(long otid);
    long getParentOTID(long otid);
    String getVariableSig(long idx);
    boolean isVolatile(long addressForVolatileCheck);
}
