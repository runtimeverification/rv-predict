package com.runtimeverification.rvpredict.log.compact;

public class SignalMaskNumber extends VariableInt {
    public SignalMaskNumber(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
