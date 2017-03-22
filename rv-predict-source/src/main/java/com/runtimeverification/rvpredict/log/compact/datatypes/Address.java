package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;

public class Address implements ReadableData {
    private final int size;
    private long value;

    public Address(TraceHeader header) throws InvalidTraceDataException {
        // TODO(virgil): Is the pointer width measured in bits or bytes?
        size = header.getPointerWidthInBytes();
        if (size != 4 && size != 8) {
            throw new InvalidTraceDataException("The pointer size can be only 4 or 8.");
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        if (size == 4) {
            value = buffer.getInt();
        } else if (size == 8) {
            value = buffer.getLong();
        } else {
            throw new InvalidTraceDataException("The pointer size can be only 4 or 8.");
        }
    }

    public long getAsLong() {
        return value;
    }
};
