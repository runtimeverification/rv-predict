package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;

import java.util.Arrays;

public class TraceData extends ReadableAggregateData {
    private final Address pc;
    private final ThreadId threadId;
    private final Generation generation;

    public TraceData(TraceHeader header) throws InvalidTraceDataException {
        pc = new Address(header);
        threadId = new ThreadId(header);
        generation = new Generation(header);
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
