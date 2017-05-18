package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignalNumberTest {
    @Test
    public void returnsCorrectSizeWithDataWidth4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(8);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        SignalNumber signalNumber = new SignalNumber(header);
        Assert.assertEquals(4, signalNumber.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(8);
        SignalNumber signalNumber = new SignalNumber(header);
        Assert.assertEquals(8, signalNumber.size());
    }

    @Test
    public void readsSignalNumberOfSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        SignalNumber signalNumber = new SignalNumber(header);
        Assert.assertEquals(4, signalNumber.size());

        ByteBuffer buffer = ByteBuffer.allocate(8).putInt(12345).putInt(Integer.MAX_VALUE);
        buffer.rewind();
        signalNumber.read(buffer);
        Assert.assertEquals(12345, signalNumber.getAsLong());
    }

    @Test
    public void readsSignalNumberOfSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(8);
        SignalNumber signalNumber = new SignalNumber(header);
        Assert.assertEquals(8, signalNumber.size());
        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(1234567890123456L).putLong(Long.MAX_VALUE);
        buffer.rewind();
        signalNumber.read(buffer);
        Assert.assertEquals(1234567890123456L, signalNumber.getAsLong());
    }
}
