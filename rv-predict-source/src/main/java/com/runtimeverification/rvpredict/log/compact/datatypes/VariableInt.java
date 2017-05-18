package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;

public class VariableInt implements ReadableData {
    private UInt64 uInt64;
    private UInt32 uInt32;
    private UInt16 uInt16;
    private UInt8 uInt8;
    private final ReadableData readableData;
    private final ValueGetter valueGetter;

    public VariableInt(TraceHeader header, int dataSizeInBytes) throws InvalidTraceDataException {
        int storageDataSizeInBytes = dataSizeInBytes;
        if (storageDataSizeInBytes < header.getDefaultDataWidthInBytes()) {
            storageDataSizeInBytes = header.getDefaultDataWidthInBytes();
        }
        switch (storageDataSizeInBytes) {
            case 1:
                uInt8 = new UInt8();
                readableData = uInt8;
                valueGetter = () -> uInt8.getAsByte();
                break;
            case 2:
                uInt16 = new UInt16();
                readableData = uInt16;
                valueGetter = () -> uInt16.getAsShort();
                break;
            case 4:
                uInt32 = new UInt32();
                readableData = uInt32;
                valueGetter = () -> uInt32.getAsInt();
                break;
            case 8:
                uInt64 = new UInt64();
                readableData = uInt64;
                valueGetter = () -> uInt64.getAsLong();
                break;
            default:
                throw new InvalidTraceDataException("Can't handle ints with size=" + storageDataSizeInBytes + ".");
        }
    }

    @Override
    public int size() {
        return readableData.size();
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        readableData.read(buffer);
    }

    public long getAsLong() {
        return valueGetter.getAsLong();
    }

    private interface ValueGetter {
        long getAsLong();
    }
}
