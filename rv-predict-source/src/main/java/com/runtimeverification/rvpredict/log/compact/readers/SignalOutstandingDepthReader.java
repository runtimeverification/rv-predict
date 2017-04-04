package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import static java.lang.Math.toIntExact;

public class SignalOutstandingDepthReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                header -> new VariableInt(header, 4),
                (context, compactEventFactory, outstandingDepth) ->
                        compactEventFactory.signalOutstandingDepth(context, toIntExact(outstandingDepth.getAsLong())));
    }
}
