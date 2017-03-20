package com.runtimeverification.rvpredict.log.compact;

import java.util.Arrays;

public class TraceData extends ReadableAggregateData {
    private final Address pc;
    private final ThreadId threadId;
    private final Generation generation;

    public TraceData(TraceHeader header) throws InvalidTraceDataException {
        pc = new Address(header);
        threadId = new ThreadId();
        generation = new Generation();
        setData(Arrays.asList(pc, threadId, generation));
    }

    public Address getPc() {
        return pc;
    }

    public ThreadId getThreadId() {
        return threadId;
    }

    public Generation getGeneration() {
        return generation;
    }
}
