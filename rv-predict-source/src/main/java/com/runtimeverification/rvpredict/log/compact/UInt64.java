package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;

public class UInt64 implements ReadableData {
    private long value = 0;

    @Override
    public int size() {
        return 8;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        value = buffer.getInt();
    }

    public long getValueAsLong() {
        return value;
    }
}
