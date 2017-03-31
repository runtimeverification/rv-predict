package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Math.toIntExact;

public class SignalOutstandingDepthReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                header -> new VariableInt(header, 4),
                (context, compactEventReader, outstandingDepth) ->
                        compactEventReader.signalOutstandingDepth(context, toIntExact(outstandingDepth.getAsLong())));
    }
}
