package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerationTest {
    @Test
    public void returnsCorrectSize() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Generation generation = new Generation(header);
        Assert.assertEquals(8, generation.size());
    }
    @Test
    public void readsGeneration() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(4);
        Generation generation = new Generation(header);
        Assert.assertEquals(8, generation.size());
        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(1234567890123456L).putLong(Long.MAX_VALUE);
        buffer.rewind();
        generation.read(buffer);
        Assert.assertEquals(1234567890123456L, generation.getAsLong());
    }
}
