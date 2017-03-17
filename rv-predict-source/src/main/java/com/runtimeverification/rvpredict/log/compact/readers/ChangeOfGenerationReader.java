package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;

public class ChangeOfGenerationReader implements Event.Reader {
    private ReaderInitializer<VariableInt> generation
            = new ReaderInitializer<>(header -> new VariableInt(header, 4));

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return generation.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        VariableInt element = generation.getInit(header);
        return Event.changeOfGeneration(element.getAsLong());
    }
}
