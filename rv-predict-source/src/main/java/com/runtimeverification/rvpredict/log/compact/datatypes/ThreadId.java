package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

public class ThreadId extends VariableInt {
    public ThreadId(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
