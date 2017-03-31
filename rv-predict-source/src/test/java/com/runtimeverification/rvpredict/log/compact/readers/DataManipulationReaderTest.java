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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataManipulationReaderTest {
    private static final long ADDRESS = 1234567890123456L;
    private static final int READ_VALUE = 1234;

    @Mock private CompactEvent mockCompactEvent;
    @Mock private Context mockContext;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEventReader mockCompactEventReader;

    @Test
    public void computesTheCorrectSizeDataSize_UsesDefaultSizeWhenLarger() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);

        CompactEventReader.Reader reader = DataManipulationReader.createReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(8, reader.size(mockTraceHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_DoesNotUseDefaultSizeWhenSmaller() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(1);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);

        CompactEventReader.Reader reader = DataManipulationReader.createReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(6, reader.size(mockTraceHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_UsesPointerSizeForAddress() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);

        CompactEventReader.Reader reader = DataManipulationReader.createReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        Assert.assertEquals(12, reader.size(mockTraceHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);
        when(mockCompactEventReader.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.LOAD, 2,
                ADDRESS, READ_VALUE, CompactEventReader.Atomicity.NOT_ATOMIC))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(24)
                .putLong(ADDRESS).putInt(READ_VALUE).putLong(Long.MAX_VALUE);
        buffer.rewind();

        CompactEventReader.Reader reader = DataManipulationReader.createReader(
                2, CompactEventReader.DataManipulationType.LOAD, CompactEventReader.Atomicity.NOT_ATOMIC);
        List<CompactEvent> events = reader.readEvent(mockContext, mockCompactEventReader, mockTraceHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
