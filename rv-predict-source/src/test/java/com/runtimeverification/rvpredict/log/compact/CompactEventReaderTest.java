package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.datatypes.ByteBufferBackedInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompactEventReaderTest {
    @Test
    public void readsFirstEvent() throws IOException, InvalidTraceDataException {
        byte[] firstPcBytes = encodeInt(encodePcDelta(0, CompactEventReader.Type.THREAD_BEGIN));
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                firstPcBytes[0], firstPcBytes[1], firstPcBytes[2], firstPcBytes[3],  // PC-delta bytes.
                5, 0, 0, 0,  // Thread id.
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());

        event = reader.readEvent();
        Assert.assertTrue(event == null);
    }

    @Test
    public void readsTwoEvents() throws IOException, InvalidTraceDataException {
        byte[] firstPcBytes = encodeInt(encodePcDelta(0, CompactEventReader.Type.THREAD_BEGIN));
        byte[] secondPcBytes = encodeInt(encodePcDelta(1, CompactEventReader.Type.LOAD1));
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                firstPcBytes[0], firstPcBytes[1], firstPcBytes[2], firstPcBytes[3],  // PC-delta bytes.
                5, 0, 0, 0,  // Thread id.

                // Jump
                0, 0, 0, 1,  // Normal program counter

                // The second event.
                secondPcBytes[0], secondPcBytes[1], secondPcBytes[2], secondPcBytes[3],  // PC-delta bytes.
                0, 1, 0, 0,  // Address = 256.
                2, 0, 0, 0,  // Value = 2.
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.READ, event.getType());
        Assert.assertEquals(256, event.getDataAddress());
        Assert.assertEquals(2, event.getDataValue());

        event = reader.readEvent();
        Assert.assertTrue(event == null);
    }

    private byte[] encodeInt(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
    }

    private int encodePcDelta(int pcDelta, CompactEventReader.Type type) {
        return (pcDelta + Constants.JUMPS_IN_DELTA / 2) * CompactEventReader.Type.getNumberOfValues() + type.intValue();
    }
}
