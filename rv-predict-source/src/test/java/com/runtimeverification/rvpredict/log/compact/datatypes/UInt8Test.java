package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class UInt8Test {
    @Test
    public void returnsSize() {
        UInt8 uInt8 = new UInt8();
        Assert.assertEquals(1, uInt8.size());
    }

    @Test
    public void readsValue() throws InvalidTraceDataException {
        UInt8 uInt8 = new UInt8();
        Assert.assertEquals(1, uInt8.size());

        ByteBuffer buffer = ByteBuffer.allocate(8).put((byte)1).put((byte)2).put((byte)3);
        buffer.rewind();
        uInt8.read(buffer);
        Assert.assertEquals(1, uInt8.getAsByte());
        uInt8.read(buffer);
        Assert.assertEquals(2, uInt8.getAsByte());
        uInt8.read(buffer);
        Assert.assertEquals(3, uInt8.getAsByte());
    }
}
