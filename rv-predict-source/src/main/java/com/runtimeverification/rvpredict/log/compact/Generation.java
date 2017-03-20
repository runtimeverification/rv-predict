package com.runtimeverification.rvpredict.log.compact;

public class Generation extends VariableInt {
    public Generation(TraceHeader header) throws InvalidTraceDataException {
        super(header, 8);
    }
}
