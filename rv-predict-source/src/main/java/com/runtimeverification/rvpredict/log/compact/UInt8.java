package com.runtimeverification.rvpredict.log.compact;

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
    public byte getValueAsByte() {
        return value;
    }
}
