package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.ByteBufferBackedInputStream;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.runtimeverification.rvpredict.testutils.CompactEventTestUtils.CURRENT_VERSION;
import static com.runtimeverification.rvpredict.testutils.CompactEventTestUtils.UNSUPPORTED_VERSION;

public class TraceHeaderTest {
    @Test
    public void failsWithoutTheProperMagicString() throws IOException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'Z', '_',  // magic string, should be "RVP_"
                CURRENT_VERSION[0], CURRENT_VERSION[1], CURRENT_VERSION[2], CURRENT_VERSION[3],  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.
        }));
        MoreAsserts.assertException(
                "Expected exception for wrong magic string",
                InvalidTraceDataException.class,
                () -> new TraceHeader(inputStream));
    }

    @Test
    public void failsWithUnsupportedVersion() throws IOException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'P', '_',  // magic string
                UNSUPPORTED_VERSION[0], UNSUPPORTED_VERSION[1], UNSUPPORTED_VERSION[2], UNSUPPORTED_VERSION[3],
                '0', '1', '2', '3',  // byte order identifier
                8,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.
        }));
        MoreAsserts.assertException(
                "Expected exception for wrong magic string",
                InvalidTraceDataException.class,
                () -> new TraceHeader(inputStream));
    }

    @Test
    public void readsHeaderData() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'P', '_',  // magic string
                CURRENT_VERSION[0], CURRENT_VERSION[1], CURRENT_VERSION[2], CURRENT_VERSION[3],  // version number
                '0', '1', '2', '3',  // byte order identifier
                8,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.
        }));
        TraceHeader traceHeader = new TraceHeader(inputStream);
        Assert.assertEquals(ByteOrder.LITTLE_ENDIAN, traceHeader.getByteOrder());
        Assert.assertEquals(8, traceHeader.getPointerWidthInBytes());
        Assert.assertEquals(4, traceHeader.getDefaultDataWidthInBytes());
    }

    @Test
    public void alsoReadsBigEndian() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'P', '_',  // magic string
                CURRENT_VERSION[0], CURRENT_VERSION[1], CURRENT_VERSION[2], CURRENT_VERSION[3],  // version number
                '3', '2', '1', '0',  // byte order identifier
                8,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.
        }));
        TraceHeader traceHeader = new TraceHeader(inputStream);
        Assert.assertEquals(ByteOrder.BIG_ENDIAN, traceHeader.getByteOrder());
        Assert.assertEquals(8, traceHeader.getPointerWidthInBytes());
        Assert.assertEquals(4, traceHeader.getDefaultDataWidthInBytes());
    }
}