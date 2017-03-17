package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;

public class EndReader implements Event.Reader {
    @Override
    public int size(TraceHeader header) {
        return 0;
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        return Event.end();
    }
}
