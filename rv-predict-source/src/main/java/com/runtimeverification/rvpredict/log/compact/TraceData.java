package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;
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

    public Address getPc() {
        return pc;
    }

    public UInt32 getThreadId() {
        return threadId;
    }

    public UInt64 getGeneration() {
        return generation;
    }
}
