package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
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
public class SignalEnterReaderTest {
    private static final long GENERATION = 1234567890123456L;
    private static final int SIGNAL_NUMBER = 12345;
    private static final long SIGNAL_HANDLER = 12346L;

    @Mock private CompactEvent mockCompactEvent;
    @Mock private Context mockContext;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEventFactory mockCompactEventFactory;

    @Test
    public void computesTheCorrectSizeDataSize_UsesDefaultDataSize4Bytes() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);

        CompactEventReader.Reader reader = SignalEnterReader.createReader();
        Assert.assertEquals(20, reader.size(mockTraceHeader));
    }

    @Test
    public void computesTheCorrectSizeDataSize_UsesDefaultDataSize8Bytes() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(8);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);

        CompactEventReader.Reader reader = SignalEnterReader.createReader();
        Assert.assertEquals(24, reader.size(mockTraceHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(8);
        when(mockCompactEventFactory.enterSignal(mockContext, GENERATION, SIGNAL_NUMBER, SIGNAL_HANDLER))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(32)
                .putLong(SIGNAL_HANDLER).putLong(GENERATION).putInt(SIGNAL_NUMBER).putLong(Long.MAX_VALUE);
        buffer.rewind();

        CompactEventReader.Reader reader = SignalEnterReader.createReader();
        List<ReadonlyEventInterface> events =
                reader.readEvent(mockContext, mockCompactEventFactory, mockTraceHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
