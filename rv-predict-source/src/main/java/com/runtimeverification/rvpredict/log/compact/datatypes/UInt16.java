package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;

import java.nio.ByteBuffer;

public class UInt16 implements ReadableData {
    private short value = 0;

    @Override
    public int size() {
        return 2;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        value = buffer.getShort();
    }
    public short getValueAsShort() {
        return value;
    }
}

