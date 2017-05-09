package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;

public class FunctionEnterReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                Address::new,
                (context, compactEventFactory, canonicalFrameAddress) ->
                        compactEventFactory.enterFunction(context, canonicalFrameAddress.getAsLong()));
    }
}
