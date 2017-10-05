package com.runtimeverification.rvpredict.log.compact;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultipartReadableAggregateDataTest {
    @Mock private ReadableAggregateDataPart mockPart1;
    @Mock private ReadableAggregateDataPart mockPart2;
    @Mock private ReadableAggregateData mockData1;
    @Mock private ReadableAggregateData mockData2;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private ByteBuffer mockBuffer;

    @Test
    public void zeroPartsData() {
        MultipartReadableAggregateData data = new MultipartReadableAggregateDataForTest();
        Assert.assertFalse(data.stillHasPartsToRead());
    }

    @Test
    public void onePartData() throws InvalidTraceDataException {
        MultipartReadableAggregateData data = new MultipartReadableAggregateDataForTest(mockPart1);

        verify(mockPart1, never()).initialize(mockTraceHeader);
        data.startReading(mockTraceHeader);
        verify(mockPart1).initialize(mockTraceHeader);

        Assert.assertTrue(data.stillHasPartsToRead());

        when(mockPart1.size()).thenReturn(10);
        Assert.assertEquals(10, data.nextPartSize());

        verify(mockPart1, never()).read(mockBuffer);
        data.readNextPartAndAdvance(mockTraceHeader, mockBuffer);
        verify(mockPart1).read(mockBuffer);

        Assert.assertFalse(data.stillHasPartsToRead());
    }

    @Test
    public void multipartData() throws InvalidTraceDataException {
        MultipartReadableAggregateData data = new MultipartReadableAggregateDataForTest(mockPart1, mockPart2);

        verify(mockPart1, never()).initialize(mockTraceHeader);
        data.startReading(mockTraceHeader);
        verify(mockPart1).initialize(mockTraceHeader);

        Assert.assertTrue(data.stillHasPartsToRead());

        when(mockPart1.size()).thenReturn(10);
        Assert.assertEquals(10, data.nextPartSize());

        verify(mockPart1, never()).read(mockBuffer);
        verify(mockPart2, never()).initialize(mockTraceHeader);
        data.readNextPartAndAdvance(mockTraceHeader, mockBuffer);
        verify(mockPart1).read(mockBuffer);
        verify(mockPart2).initialize(mockTraceHeader);

        Assert.assertTrue(data.stillHasPartsToRead());

        when(mockPart2.size()).thenReturn(15);
        Assert.assertEquals(15, data.nextPartSize());

        verify(mockPart2, never()).read(mockBuffer);
        data.readNextPartAndAdvance(mockTraceHeader, mockBuffer);
        verify(mockPart2).read(mockBuffer);

        Assert.assertFalse(data.stillHasPartsToRead());
    }

    private static class MultipartReadableAggregateDataForTest extends MultipartReadableAggregateData {
        MultipartReadableAggregateDataForTest(ReadableAggregateDataPart... initializers) {
            setData(initializers);
        }
    }
}
