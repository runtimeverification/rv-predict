package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class UInt64Test {
    @Test
    public void returnsSize() {
        UInt64 uInt64 = new UInt64();
        Assert.assertEquals(8, uInt64.size());
    }

    @Test
    public void readsValue() throws InvalidTraceDataException {
        UInt64 uInt64 = new UInt64();
        Assert.assertEquals(8, uInt64.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).putLong(1).putLong(2).putLong(3);
        buffer.rewind();
        uInt64.read(buffer);
        Assert.assertEquals(1, uInt64.getAsLong());
        uInt64.read(buffer);
        Assert.assertEquals(2, uInt64.getAsLong());
        uInt64.read(buffer);
        Assert.assertEquals(3, uInt64.getAsLong());
    }
}
