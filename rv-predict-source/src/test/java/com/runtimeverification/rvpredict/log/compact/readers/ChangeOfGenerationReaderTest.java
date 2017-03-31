package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeOfGenerationReaderTest {
    private static final long GENERATION = 1234567890123456L;
    @Test
    public void computesTheCorrectSize() throws InvalidTraceDataException {
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);

        ChangeOfGenerationReader reader = new ChangeOfGenerationReader();
        Assert.assertEquals(8, reader.size(mockHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        CompactEvent mockCompactEvent = mock(CompactEvent.class);

        Context mockContext = mock(Context.class);
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        CompactEventReader mockCompactEventReader = mock(CompactEventReader.class);
        when(mockCompactEventReader.changeOfGeneration(mockContext, GENERATION))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(GENERATION).putLong(Long.MAX_VALUE);
        buffer.rewind();

        ChangeOfGenerationReader reader = new ChangeOfGenerationReader();
        List<CompactEvent> events = reader.readEvent(mockContext, mockCompactEventReader, mockHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
