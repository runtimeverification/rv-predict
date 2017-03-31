package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Math.toIntExact;

public class SignalOutstandingDepthReader implements CompactEventReader.Reader {
    private final LazyInitializer<VariableInt> reader =
            new LazyInitializer<>(header -> new VariableInt(header, 4));

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        VariableInt outstandingDepth = reader.getInit(header);
        outstandingDepth.read(buffer);
        return compactEventReader.signalOutstandingDepth(context, toIntExact(outstandingDepth.getAsLong()));
    }
}
