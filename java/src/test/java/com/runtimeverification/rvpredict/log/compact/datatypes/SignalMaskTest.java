package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignalMaskTest {
    @Test
    public void returnsCorrectSize() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        SignalMask signalMask = new SignalMask(header);
        Assert.assertEquals(8, signalMask.size());
    }
    @Test
    public void readsGeneration() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        SignalMask signalMask = new SignalMask(header);
        Assert.assertEquals(8, signalMask.size());
        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(1234567890123456L).putLong(Long.MAX_VALUE);
        buffer.rewind();
        signalMask.read(buffer);
        Assert.assertEquals(1234567890123456L, signalMask.getAsLong());
    }
}
