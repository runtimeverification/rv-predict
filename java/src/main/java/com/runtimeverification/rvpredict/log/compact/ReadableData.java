package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;

public interface ReadableData {
    int size();
    void read(ByteBuffer buffer) throws InvalidTraceDataException;
}
