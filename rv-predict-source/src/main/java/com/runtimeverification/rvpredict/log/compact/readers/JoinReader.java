package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class JoinReader implements CompactEvent.Reader {
    private final LazyInitializer<ThreadId> threadId = new LazyInitializer<>(ThreadId::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return threadId.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        ThreadId element = threadId.getInit(header);
        element.read(buffer);
        return CompactEvent.join(element.getAsLong());
    }

}
