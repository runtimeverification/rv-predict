package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;

public class UnblockSignalsReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                SignalMaskNumber::new,
                (context, originalEventId, compactEventFactory, signalMaskNumber) ->
                        compactEventFactory.unblockSignals(context, originalEventId, signalMaskNumber.getAsLong()));
    }
}
