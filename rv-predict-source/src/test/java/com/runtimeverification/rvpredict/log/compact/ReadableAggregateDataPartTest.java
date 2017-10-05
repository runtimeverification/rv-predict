package com.runtimeverification.rvpredict.log.compact;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReadableAggregateDataPartTest {
    @Mock private ReadableAggregateData mockData1;
    @Mock private ReadableAggregateData mockData2;
    @Mock private ReadableAggregateDataPart.Initializer<ReadableAggregateData, ReadableAggregateData> mockInitializer;
    @Mock private ReadableAggregateDataPart<ReadableAggregateData, ReadableAggregateDataForTest> mockDataPart;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private ByteBuffer mockBuffer;

    @Test
    public void firstDataPart() throws InvalidTraceDataException {
        ReadableAggregateDataPart<ReadableAggregateData, ReadableAggregateData> part =
                new ReadableAggregateDataPart<>(Optional.empty(), mockInitializer);

        when(mockInitializer.initialize(mockTraceHeader, Optional.empty())).thenReturn(mockData1);
        verify(mockInitializer, never()).initialize(any(), any());
        part.initialize(mockTraceHeader);
        verify(mockInitializer).initialize(mockTraceHeader, Optional.empty());

        Assert.assertEquals(mockData1, part.getValue());

        verify(mockData1, never()).read(mockBuffer);
        part.read(mockBuffer);
        verify(mockData1).read(mockBuffer);
    }

    @Test
    public void secondDataPart() throws InvalidTraceDataException {
        when(mockInitializer.initialize(mockTraceHeader, Optional.of(mockData1))).thenReturn(mockData2);
        when(mockDataPart.getValue()).thenReturn(mockData1);

        ReadableAggregateDataPart<ReadableAggregateData, ReadableAggregateData> part =
                new ReadableAggregateDataPart<>(Optional.of(mockDataPart), mockInitializer);

        verify(mockInitializer, never()).initialize(any(), any());
        part.initialize(mockTraceHeader);
        verify(mockInitializer).initialize(mockTraceHeader, Optional.of(mockData1));

        Assert.assertEquals(mockData2, part.getValue());

        verify(mockData2, never()).read(mockBuffer);
        part.read(mockBuffer);
        verify(mockData2).read(mockBuffer);

    }

    private class ReadableAggregateDataForTest extends ReadableAggregateData {}
}
