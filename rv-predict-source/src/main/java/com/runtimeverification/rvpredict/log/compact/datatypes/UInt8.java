package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;

import java.nio.ByteBuffer;

public class UInt8 implements ReadableData {
    private byte value = 0;

    @Override
    public int size() {
        return 1;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        value = buffer.get();
    }

    byte getAsByte() {
        return value;
    }
}
