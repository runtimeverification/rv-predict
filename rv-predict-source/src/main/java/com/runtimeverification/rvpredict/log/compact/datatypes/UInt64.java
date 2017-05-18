package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;

import java.nio.ByteBuffer;

public class UInt64 implements ReadableData {
    private long value = 0;

    @Override
    public int size() {
        return 8;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        value = buffer.getLong();
    }

    long getAsLong() {
        return value;
    }
}
