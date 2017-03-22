package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.*;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class ThreadBeginReader implements CompactEvent.Reader {
    private final LazyInitializer<TraceElement> reader = new LazyInitializer<>(TraceElement::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return CompactEvent.begin(
                context, element.threadId, element.generation);
    }

    private static class TraceElement extends ReadableAggregateData {
        private final ThreadId threadId;
        private final Generation generation;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            threadId = new ThreadId(header);
            generation = new Generation(header);
            setData(Arrays.asList(threadId, generation));
        }
    }
}
