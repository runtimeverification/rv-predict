package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Address;
import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.UInt32;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;

public class ThreadSyncReader implements Event.Reader {
    private final Event.ThreadSyncType threadSyncType;
    private ReaderInitializer<VariableInt> reader;

    public ThreadSyncReader(Event.ThreadSyncType threadSyncType) {
        this.threadSyncType = threadSyncType;
        this.threadId = new ReaderInitializer<>(header -> new VariableInt(header, 4))
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        VariableInt threadId = reader.getInit(header);
        threadId.read(buffer);
        return Event.threadSync(threadSyncType, threadId.getAsLong());
    }
}
