package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BeginReader implements Event.Reader {
    private ReaderInitializer<TraceElement> reader;

    BeginReader() {
        reader = new ReaderInitializer<>(TraceElement::new);
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return Event.begin(
                element.threadId.getAsLong(), element.generation.getAsLong());
    }

    private class TraceElement extends ReadableAggregateData {
        private final VariableInt threadId;
        private final VariableInt generation;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            threadId = new VariableInt(header, 4);
            generation = new VariableInt(header, 8);
            setData(Arrays.asList(threadId, generation));
        }
    }
}
