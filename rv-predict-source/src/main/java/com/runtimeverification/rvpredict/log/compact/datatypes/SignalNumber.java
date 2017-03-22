package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

public class SignalNumber extends VariableInt {
    public SignalNumber(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
