package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressTest {
    @Test
    public void returnsCorrectSizeWithPointerSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Address address = new Address(header);
        Assert.assertEquals(4, address.size());
    }

    @Test
    public void returnsCorrectSizeWithPointerSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(8);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Address address = new Address(header);
        Assert.assertEquals(8, address.size());
    }

    @Test
    public void readsAddressOfSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Address address = new Address(header);
        Assert.assertEquals(4, address.size());
        ByteBuffer buffer = ByteBuffer.allocate(8).putInt(12345).putInt(Integer.MAX_VALUE);
        buffer.rewind();
        address.read(buffer);
        Assert.assertEquals(12345, address.getAsLong());
    }

    @Test
    public void readsAddressOfSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(8);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Address address = new Address(header);
        Assert.assertEquals(8, address.size());
        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(1234567890123456L).putLong(Long.MAX_VALUE);
        buffer.rewind();
        address.read(buffer);
        Assert.assertEquals(1234567890123456L, address.getAsLong());
    }

    @Test
    public void usesDataWidthWhenSizeIsLower() throws InvalidTraceDataException {
        // TODO(virgil): Make sure that this is the intended behaviour.
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(8);
        Address address = new Address(header);
        Assert.assertEquals(8, address.size());

        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).putLong(1L).put((byte)2);
        buffer.rewind();
        address.read(buffer);
        Assert.assertEquals(1, address.getAsLong());
        Assert.assertEquals(2, buffer.get());

        buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putLong(1L).put((byte)2);
        buffer.rewind();
        address.read(buffer);
        Assert.assertEquals(1, address.getAsLong());
        Assert.assertEquals(2, buffer.get());
    }
}
