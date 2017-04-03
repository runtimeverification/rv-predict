package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;

public class SignalDisestablishReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                SignalNumber::new,
                (context, compactEventFactory, signalNumber) ->
                        compactEventFactory.disestablishSignal(context, signalNumber.getAsLong()));
    }
}
