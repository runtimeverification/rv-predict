package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;

public class SignalMaskReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                SignalMaskNumber::new,
                (context, compactEventFactory, signalMaskNumber) ->
                        compactEventFactory.signalMask(context, signalMaskNumber.getAsLong()));
    }
}
