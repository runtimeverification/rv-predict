package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LockManipulationReaderTest {
    private static final long ADDRESS = 1234567890123456L;

    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEvent mockCompactEvent;
    @Mock private Context mockContext;
    @Mock private TraceHeader mockHeader;
    @Mock private CompactEventReader mockCompactEventReader;

    @Test
    public void computesTheCorrectSizeDataSize_UsesPointerSizeForAddress8Bytes() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);

        LockManipulationReader reader = new LockManipulationReader(CompactEventReader.LockManipulationType.LOCK);
        Assert.assertEquals(8, reader.size(mockTraceHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_UsesPointerSizeForAddress4Bytes() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);

        LockManipulationReader reader = new LockManipulationReader(CompactEventReader.LockManipulationType.LOCK);
        Assert.assertEquals(4, reader.size(mockTraceHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockHeader.getPointerWidthInBytes()).thenReturn(8);
        when(mockCompactEventReader.lockManipulation(
                mockContext, CompactEventReader.LockManipulationType.LOCK, ADDRESS))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(24).putLong(ADDRESS).putLong(Long.MAX_VALUE);
        buffer.rewind();

        LockManipulationReader reader = new LockManipulationReader(CompactEventReader.LockManipulationType.LOCK);
        List<CompactEvent> events = reader.readEvent(mockContext, mockCompactEventReader, mockHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
