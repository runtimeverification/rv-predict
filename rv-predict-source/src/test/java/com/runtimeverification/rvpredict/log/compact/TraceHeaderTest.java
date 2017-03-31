package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.ByteBufferBackedInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TraceHeaderTest {
    @Test
    public void failsWithoutTheProperMagicString() throws IOException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'Z', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.
        }));
        try {
            TraceHeader traceHeader = new TraceHeader(inputStream);
            Assert.assertTrue("The magic string is wrong, should have thrown an exception.", false);
        } catch (InvalidTraceDataException e) {
            // This is what we're testing here.
        }
    }

    @Test
    public void readsHeaderData() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                'R', 'V', 'P', '_',  // magic string
                0, 0, 0, 0,  // version number
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
                0, 0, 0, 0,  // version number
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