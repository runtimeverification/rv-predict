package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedLibraryReaderTest {
    private static final int LIBRARY_ID = 101;
    private static final List<ReadonlyEventInterface> EVENT_LIST = new ArrayList<>();

    @Mock private Context mockContext;
    @Mock private TraceHeader mockTraceHeader;
    @Mock private CompactEventFactory mockCompactEventFactory;

    @Test
    public void readsData() throws InvalidTraceDataException {
        when(mockTraceHeader.getDefaultDataWidthInBytes()).thenReturn(4);
        when(mockTraceHeader.getPointerWidthInBytes()).thenReturn(4);
        when(mockCompactEventFactory.sharedLibrary(mockContext, LIBRARY_ID, "hello"))
                .thenReturn(EVENT_LIST);

        ByteBuffer buffer1 = ByteBuffer.allocate(24)
                .putInt(LIBRARY_ID).putInt(5)
                .putLong(Long.MAX_VALUE);
        buffer1.rewind();
        ByteBuffer buffer2 = ByteBuffer.allocate(24)
                .put((byte)'l').put((byte)'l').put((byte)'e').put((byte)'h')
                .put((byte)0).put((byte)0).put((byte)0).put((byte)'o')
                .putLong(Long.MAX_VALUE);
        buffer2.rewind();

        CompactEventReader.Reader reader = SharedLibraryReader.createReader();

        reader.startReading(mockTraceHeader);

        Assert.assertTrue(reader.stillHasPartsToRead());
        Assert.assertEquals(8, reader.nextPartSize(mockTraceHeader));
        reader.addPart(buffer1, mockTraceHeader);

        Assert.assertTrue(reader.stillHasPartsToRead());
        Assert.assertEquals(8, reader.nextPartSize(mockTraceHeader));
        reader.addPart(buffer2, mockTraceHeader);

        Assert.assertFalse(reader.stillHasPartsToRead());
        Assert.assertEquals(EVENT_LIST, reader.build(mockContext, mockCompactEventFactory, mockTraceHeader));
    }
}
