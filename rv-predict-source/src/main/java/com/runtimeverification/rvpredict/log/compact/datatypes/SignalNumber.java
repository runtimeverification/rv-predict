package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

public class SignalNumber extends VariableInt {
    public SignalNumber(TraceHeader header) throws InvalidTraceDataException {
        super(header, 4);
    }
}
