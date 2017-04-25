package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
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
public class GetSignalMaskReaderTest {
    private static final int SIGNAL_MASK_NUMBER = 10;
    @Mock
    private Context mockContext;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEventFactory mockCompactEventFactory;

    @Test
    public void computesDataSize_UsesDefaultDataSize4() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);

        CompactEventReader.Reader reader = GetSignalMaskReader.createReader();
        Assert.assertEquals(4, reader.size(mockTraceHeader));
    }


    @Test
    public void computesDataSize_UsesDefaultDataSize8() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(8);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);

        CompactEventReader.Reader reader = GetSignalMaskReader.createReader();
        Assert.assertEquals(8, reader.size(mockTraceHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);
        when(mockCompactEventFactory.getSignalMask(mockContext, SIGNAL_MASK_NUMBER))
                .thenReturn(Collections.emptyList());

        ByteBuffer buffer = ByteBuffer.allocate(24)
                .putInt(SIGNAL_MASK_NUMBER).putLong(Long.MAX_VALUE);
        buffer.rewind();

        CompactEventReader.Reader reader = GetSignalMaskReader.createReader();
        List<CompactEvent> events = reader.readEvent(mockContext, mockCompactEventFactory, mockTraceHeader, buffer);

        Assert.assertEquals(0, events.size());
    }
}
