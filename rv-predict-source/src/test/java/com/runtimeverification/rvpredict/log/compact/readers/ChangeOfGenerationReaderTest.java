package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.testutils.ReaderUtils;
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
public class ChangeOfGenerationReaderTest {
    private static final long GENERATION = 1234567890123456L;

    @Mock private CompactEvent mockCompactEvent;
    @Mock private Context mockContext;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEventFactory mockCompactEventFactory;

    @Test
    public void computesTheCorrectSize() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);

        CompactEventReader.Reader reader = ChangeOfGenerationReader.createReader();
        Assert.assertEquals(8, ReaderUtils.firstPartSize(reader, mockTraceHeader));
    }

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockCompactEventFactory.changeOfGeneration(mockContext, GENERATION))
                .thenReturn(Collections.singletonList(mockCompactEvent));

        ByteBuffer buffer = ByteBuffer.allocate(16).putLong(GENERATION).putLong(Long.MAX_VALUE);
        buffer.rewind();

        CompactEventReader.Reader reader = ChangeOfGenerationReader.createReader();
        List<ReadonlyEventInterface> events =
                ReaderUtils.readSimpleEvent(reader, mockContext, mockCompactEventFactory, mockTraceHeader, buffer);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(mockCompactEvent, events.get(0));
    }
}
