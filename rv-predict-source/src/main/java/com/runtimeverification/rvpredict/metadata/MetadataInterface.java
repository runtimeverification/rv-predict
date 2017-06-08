package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.Trace;

public interface MetadataInterface {
    String getLocationSig(long locationId);
    String getRaceLocationSig(ReadonlyEventInterface e1, ReadonlyEventInterface e2, Trace trace, Configuration config);
    String getLocationPrefix();
    void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId);
    long getOriginalThreadCreationLocId(long otid);
    long getParentOTID(long otid);
    String getVariableSig(long idx);
    boolean isVolatile(long addressForVolatileCheck);
}
