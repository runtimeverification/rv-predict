package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

public class Address extends VariableInt {
    public Address(TraceHeader header) throws InvalidTraceDataException {
        super(header, header.getPointerWidthInBytes());
    }
}
