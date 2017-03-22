package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

public class SignalMaskNumber extends VariableInt {
    public SignalMaskNumber(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
