package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;

public class LockManipulationReader {
    public static CompactEventReader.Reader createReader(CompactEventReader.LockManipulationType lockManipulationType) {
        return new SimpleDataReader<>(
                Address::new,
                (context, compactEventReader, address) ->
                        compactEventReader.lockManipulation(context, lockManipulationType, address.getAsLong()));
    }
}
