package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class ChangeOfGenerationReader implements CompactEventReader.Reader {
    private final LazyInitializer<Generation> generation = new LazyInitializer<>(Generation::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return generation.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        Generation element = generation.getInit(header);
        element.read(buffer);
        return compactEventReader.changeOfGeneration(context, element.getAsLong());
    }
}
