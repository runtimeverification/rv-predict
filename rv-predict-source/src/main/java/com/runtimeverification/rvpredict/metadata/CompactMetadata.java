package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompactMetadata implements MetadataInterface {
    private transient final ConcurrentHashMap<Long, String> addressToLocationSig = new ConcurrentHashMap<>();
    private final Map<Long, Pair<Long, Long>> otidToCreationInfo = new ConcurrentHashMap<>();

    @Override
    public String getLocationSig(long locationId) {
        String sig = addressToLocationSig.get(locationId);
        if (sig == null) {
            if (Configuration.debug) {
                System.err.println("getLocationSig("
                        + locationId + " " + Long.toHexString(locationId) + ") -> null");
            }
            return String.format("0x%016x", locationId) + ";file:UNKNOWN;line:UNKNOWN";
        }
        return sig;
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
        return String.format("0x%016x", idx);
    }

    @Override
    public boolean isVolatile(long addressForVolatileCheck) {
        return false;
    }

    public void setLocationSig(String signature) {
        String[] signatureParts = signature.split(";");
        String[] addressParts = signatureParts[0].split(":");
        String addressString = addressParts[1];
        assert addressString.startsWith("0x");
        long address = Long.parseLong(addressString.substring(2), 16);
        addressToLocationSig.put(address, signature);
    }
}
