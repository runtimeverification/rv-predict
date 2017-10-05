package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Given its length, reads an ASCII string encoded on a whole number of int32s.
 */
public class StringData implements ReadableData {
    private final int nameLength;
    private final VariableInt block;
    private String data;

    public StringData(TraceHeader header, int nameLength) throws InvalidTraceDataException {
        this.nameLength = nameLength;
        this.block = new VariableInt(header, 4);
    }

    @Override
    public int size() {
        return 4 * ((nameLength + 3) / 4);
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        byte[] bytes = new byte[nameLength];
        int sizeInInts = size()/4;
        for (int i = 0; i < sizeInInts; i++) {
            block.read(buffer);
            long value = block.getAsLong();
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;
                if (index < bytes.length) {
                    bytes[index] =(byte) (value & 0xff);
                }
                value = value >>> 8;
            }
        }
        data = new String(bytes, StandardCharsets.US_ASCII);
    }

    public String getAsString() {
        return data;
    }
}
