package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ThreadId;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;
import java.util.List;

public class ThreadSyncReader implements CompactEvent.Reader {
    private final CompactEvent.ThreadSyncType threadSyncType;
    private final LazyInitializer<ThreadId> reader;

    public ThreadSyncReader(CompactEvent.ThreadSyncType threadSyncType) {
        this.threadSyncType = threadSyncType;
        this.reader = new LazyInitializer<>(ThreadId::new);
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        VariableInt threadId = reader.getInit(header);
        threadId.read(buffer);
        return CompactEvent.threadSync(threadSyncType, threadId.getAsLong());
    }
}
