package com.runtimeverification.rvpredict.log.compact;

import java.util.Arrays;

public class TraceData extends ReadableAggregateData {
    private final Address pc;
    private final UInt32 threadId;
    private final UInt64 generation;

    public TraceData(TraceHeader header) throws InvalidTraceDataException {
        pc = new Address(header);
        threadId = new UInt32();
        generation = new UInt64();
        setData(Arrays.asList(pc, threadId, generation));
    }

    public long getPc() {
        return pc.getAsLong();
    }

    public int getThreadId() {
        return threadId.getValueAsInt();
    }

    public long getGeneration() {
        return generation.getValueAsLong();
    }
}
