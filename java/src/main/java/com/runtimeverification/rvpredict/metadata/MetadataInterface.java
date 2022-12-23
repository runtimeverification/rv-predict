package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.OptionalLong;

public interface MetadataInterface {
    String getLocationSig(long locationId);
    String getRaceDataSig(
            ReadonlyEventInterface e1,
            Collection<ReadonlyEventInterface> stackTrace1,
            Collection<ReadonlyEventInterface> stackTrace2,
            Configuration config);
    String getLocationPrefix();
    void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId);
    OptionalLong getOriginalThreadCreationLocId(long otid);
    OptionalLong getParentOTID(long otid);
    String getVariableSig(long idx);
    boolean isVolatile(long addressForVolatileCheck);
    String getLockSig(ReadonlyEventInterface event, Collection<ReadonlyEventInterface> stackTrace);
}
