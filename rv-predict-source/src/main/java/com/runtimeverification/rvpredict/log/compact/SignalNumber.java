package com.runtimeverification.rvpredict.log.compact;

public class SignalNumber extends VariableInt {
    public SignalNumber(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
