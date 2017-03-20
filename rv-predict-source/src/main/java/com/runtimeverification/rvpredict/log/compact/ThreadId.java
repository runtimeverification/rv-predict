package com.runtimeverification.rvpredict.log.compact;

public class ThreadId extends VariableInt {
    public ThreadId(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
