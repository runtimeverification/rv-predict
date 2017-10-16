package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class UInt16Test {
    @Test
    public void returnsSize() {
        UInt16 uInt16 = new UInt16();
        Assert.assertEquals(2, uInt16.size());
    }

    @Test
    public void readsValue() throws InvalidTraceDataException {
        UInt16 uInt16 = new UInt16();
        Assert.assertEquals(2, uInt16.size());

        ByteBuffer buffer = ByteBuffer.allocate(8).putShort((short)1).putShort((short) 2).putShort((short) 3);
        buffer.rewind();
        uInt16.read(buffer);
        Assert.assertEquals(1, uInt16.getAsShort());
        uInt16.read(buffer);
        Assert.assertEquals(2, uInt16.getAsShort());
        uInt16.read(buffer);
        Assert.assertEquals(3, uInt16.getAsShort());
    }
}
