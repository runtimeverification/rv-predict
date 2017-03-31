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

public class DataManipulationReaderTest {
    private static final long ADDRESS = 1234567890123456L;
    private static final int READ_VALUE = 1234;

    @Test
    public void computesTheCorrectSizeDataSize_UsesDefaultSizeWhenLarger() throws InvalidTraceDataException {
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockHeader.getPointerWidthInBytes()).thenReturn(4);

        DataManipulationReader reader = new DataManipulationReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(8, reader.size(mockHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_DoesNotUseDefaultSizeWhenSmaller() throws InvalidTraceDataException {
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(1);
        when(mockHeader.getPointerWidthInBytes()).thenReturn(4);

        DataManipulationReader reader = new DataManipulationReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(6, reader.size(mockHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_UsesPointerSizeForAddress() throws InvalidTraceDataException {
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockHeader.getPointerWidthInBytes()).thenReturn(8);

        DataManipulationReader reader = new DataManipulationReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(12, reader.size(mockHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        CompactEvent mockCompactEvent = mock(CompactEvent.class);

        Context mockContext = mock(Context.class);
        TraceHeader mockHeader = mock(TraceHeader.class);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockHeader.getPointerWidthInBytes()).thenReturn(8);
        CompactEventReader mockCompactEventReader = mock(CompactEventReader.class);
        when(mockCompactEventReader.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.LOAD, 2,
                ADDRESS, READ_VALUE, CompactEventReader.Atomicity.NOT_ATOMIC))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(24)
                .putLong(ADDRESS).putInt(READ_VALUE).putLong(Long.MAX_VALUE);
        buffer.rewind();

        DataManipulationReader reader = new DataManipulationReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        List<CompactEvent> events = reader.readEvent(mockContext, mockCompactEventReader, mockHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
