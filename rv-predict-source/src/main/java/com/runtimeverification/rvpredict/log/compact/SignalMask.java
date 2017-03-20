package com.runtimeverification.rvpredict.log.compact;

public class SignalMask extends VariableInt {
    public SignalMask(TraceHeader header) throws InvalidTraceDataException {
        super(header, 8);
    }
}
