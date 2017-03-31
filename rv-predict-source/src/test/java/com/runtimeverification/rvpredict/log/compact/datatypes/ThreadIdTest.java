package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThreadIdTest {
    @Test
    public void returnsCorrectSizeWithDataWidth4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(8);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        ThreadId threadId = new ThreadId(header);
        Assert.assertEquals(4, threadId.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(8);
        ThreadId threadId = new ThreadId(header);
        Assert.assertEquals(8, threadId.size());
    }

    @Test
    public void readsThreadIdOfSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        ThreadId threadId = new ThreadId(header);
        Assert.assertEquals(4, threadId.size());

        ByteBuffer buffer = ByteBuffer.allocate(8).putInt(12345).putInt(Integer.MAX_VALUE);
        buffer.rewind();
        threadId.read(buffer);
        Assert.assertEquals(12345, threadId.getAsLong());
    }

    @Test
    public void readsThreadIdOfSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(8);
        ThreadId threadId = new ThreadId(header);
        Assert.assertEquals(8, threadId.size());
        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(1234567890123456L).putLong(Long.MAX_VALUE);
        buffer.rewind();
        threadId.read(buffer);
        Assert.assertEquals(1234567890123456L, threadId.getAsLong());
    }
}
