package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.MultipartReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
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
public class MultipartDataReaderTest {
    @Mock private MultipartReadableAggregateDataForTest mockMultipartReadableAggregateData;
    @Mock private SimpleDataReader.ReadableDataToEventListConverter<MultipartReadableAggregateDataForTest>
            mockConverter;
    @Mock private TraceHeader mockHeader;
    @Mock private ByteBuffer mockBuffer;
    @Mock private CompactEventFactory mockCompactEventFactory;
    @Mock private Context mockContext;

    @Test
    public void zeroPartsData() {
        MultipartDataReader<MultipartReadableAggregateDataForTest> reader =
                new MultipartDataReader<>(
                        mockMultipartReadableAggregateData, mockConverter);
        when(mockMultipartReadableAggregateData.stillHasPartsToRead())
                .thenReturn(false);
        Assert.assertFalse(reader.stillHasPartsToRead());
    }

    @Test
    public void onePartData() throws InvalidTraceDataException {
        MultipartDataReader<MultipartReadableAggregateDataForTest> reader =
                new MultipartDataReader<>(
                        mockMultipartReadableAggregateData, mockConverter);

        verify(mockMultipartReadableAggregateData, never()).startReading(mockHeader);
        verify(mockMultipartReadableAggregateData, never()).readNextPartAndAdvance(any(), any());

        reader.startReading(mockHeader);

        verify(mockMultipartReadableAggregateData).startReading(mockHeader);
        verify(mockMultipartReadableAggregateData, never()).readNextPartAndAdvance(any(), any());

        when(mockMultipartReadableAggregateData.stillHasPartsToRead()).thenReturn(true);
        Assert.assertTrue(reader.stillHasPartsToRead());

        when(mockMultipartReadableAggregateData.nextPartSize()).thenReturn(10);
        Assert.assertEquals(10, reader.nextPartSize(mockHeader));

        verify(mockMultipartReadableAggregateData).startReading(mockHeader);
        verify(mockMultipartReadableAggregateData, never()).readNextPartAndAdvance(any(), any());

        reader.addPart(mockBuffer, mockHeader);

        verify(mockMultipartReadableAggregateData).startReading(mockHeader);
        verify(mockMultipartReadableAggregateData).readNextPartAndAdvance(mockHeader, mockBuffer);

        verify(mockConverter, never())
                .dataElementToEvent(mockContext, mockCompactEventFactory, mockMultipartReadableAggregateData);
        reader.build(mockContext, mockCompactEventFactory, mockHeader);
        verify(mockConverter)
                .dataElementToEvent(mockContext, mockCompactEventFactory, mockMultipartReadableAggregateData);
    }

    private static class MultipartReadableAggregateDataForTest extends MultipartReadableAggregateData {}
}
