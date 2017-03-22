package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;

public class UInt32 implements ReadableData {
    private int value = 0;

    @Override
    public int size() {
        return 4;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        value = buffer.getInt();
    }

    public int getValueAsInt() {
        return value;
    }
}
