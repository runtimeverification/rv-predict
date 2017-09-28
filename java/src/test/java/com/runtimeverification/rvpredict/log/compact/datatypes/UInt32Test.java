package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class UInt32Test {
    @Test
    public void returnsSize() {
        UInt32 uInt32 = new UInt32();
        Assert.assertEquals(4, uInt32.size());
    }

    @Test
    public void readsValue() throws InvalidTraceDataException {
        UInt32 uInt32 = new UInt32();
        Assert.assertEquals(4, uInt32.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).putInt(1).putInt(2).putInt(3);
        buffer.rewind();
        uInt32.read(buffer);
        Assert.assertEquals(1, uInt32.getAsInt());
        uInt32.read(buffer);
        Assert.assertEquals(2, uInt32.getAsInt());
        uInt32.read(buffer);
        Assert.assertEquals(3, uInt32.getAsInt());
    }
}
