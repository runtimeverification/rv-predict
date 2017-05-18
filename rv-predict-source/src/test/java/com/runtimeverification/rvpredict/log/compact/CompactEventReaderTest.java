package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.datatypes.ByteBufferBackedInputStream;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompactEventReaderTest {
    private static final byte[] DELTA_0_THREAD_BEGIN =
            encodeInt(encodePcDelta(0, CompactEventReader.Type.THREAD_BEGIN));
    private static final byte[] DELTA_1_LOAD1 =
            encodeInt(encodePcDelta(1, CompactEventReader.Type.LOAD1));
    @Test
    public void readsFirstEvent() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2], DELTA_0_THREAD_BEGIN[3],
                5, 0, 0, 0,  // Thread id.
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, event.getLocationId());

        MoreAsserts.assertException(EOFException.class, reader::readEvent);
    }

    @Test
    public void failsForIncompleteFirstEventDeltaOp() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2],
        }));
        MoreAsserts.assertException(
                "Expected exception for invalid first event deltop.",
                InvalidTraceDataException.class,
                "event descriptor",
                () -> new CompactEventReader(inputStream));
    }

    @Test
    public void failsForIncompleteFirstEvent() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2], DELTA_0_THREAD_BEGIN[3],
                5, 0, 0,
        }));
        MoreAsserts.assertException(
                "Expected exception for invalid first event deltop.",
                InvalidTraceDataException.class,
                "thread id",
                () -> new CompactEventReader(inputStream));
    }

    @Test
    public void readsTwoEvents() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2], DELTA_0_THREAD_BEGIN[3],
                5, 0, 0, 0,  // Thread id.

                // Jump
                0, 0, 0, 1,  // Normal program counter = 2^24

                // The second event.
                DELTA_1_LOAD1[0], DELTA_1_LOAD1[1], DELTA_1_LOAD1[2], DELTA_1_LOAD1[3],  // PC-delta bytes.
                0, 1, 0, 0,  // Address = 256.
                2, 0, 0, 0,  // Value = 2.
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, event.getLocationId());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.READ, event.getType());
        Assert.assertEquals(256, event.getDataAddress());
        Assert.assertEquals(2, event.getDataValue());
        Assert.assertEquals((1 << 24) + 1, event.getLocationId());

        MoreAsserts.assertException(EOFException.class, reader::readEvent);
    }

    @Test
    public void exceptionForIncompleteDeltaOp() throws IOException, InvalidTraceDataException {
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2], DELTA_0_THREAD_BEGIN[3],
                5, 0, 0, 0,  // Thread id.

                // Jump
                0, 0, 0, 1,  // Normal program counter = 2^24

                // The second event.
                DELTA_1_LOAD1[0], DELTA_1_LOAD1[1], DELTA_1_LOAD1[2],
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());

        MoreAsserts.assertException(
                "Expected exception for invalid event descriptor.",
                IOException.class,
                "event descriptor",
                reader::readEvent);
    }

    @Test
    public void readsCompoundEvent() throws IOException, InvalidTraceDataException {
        byte[] secondPcBytes = encodeInt(encodePcDelta(2, CompactEventReader.Type.ATOMIC_LOAD1));
        InputStream inputStream = new ByteBufferBackedInputStream(ByteBuffer.wrap(new byte[]{
                // Event header
                'R', 'V', 'P', '_',  // magic string, should be "RVP_"
                0, 0, 0, 0,  // version number
                '0', '1', '2', '3',  // byte order identifier
                4,  // pointer width
                4,  // default data width
                0, 0,  // filler bytes.

                // The first event.
                DELTA_0_THREAD_BEGIN[0], DELTA_0_THREAD_BEGIN[1], DELTA_0_THREAD_BEGIN[2], DELTA_0_THREAD_BEGIN[3],
                5, 0, 0, 0,  // Thread id.

                // Jump
                0, 0, 0, 1,  // Normal program counter = 2^24

                // The second event.
                secondPcBytes[0], secondPcBytes[1], secondPcBytes[2], secondPcBytes[3],  // PC-delta bytes.
                0, 2, 0, 0,  // Address = 512.
                3, 0, 0, 0,  // Value = 3.

                // The third event.
                DELTA_1_LOAD1[0], DELTA_1_LOAD1[1], DELTA_1_LOAD1[2], DELTA_1_LOAD1[3],  // PC-delta bytes.
                0, 1, 0, 0,  // Address = 256.
                2, 0, 0, 0,  // Value = 2.
        }));
        IEventReader reader = new CompactEventReader(inputStream);
        ReadonlyEventInterface event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.BEGIN_THREAD, event.getType());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, event.getLocationId());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.WRITE_LOCK, event.getType());
        Assert.assertEquals(512, event.getLockId());
        Assert.assertEquals((1 << 24) + 2, event.getLocationId());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.READ, event.getType());
        Assert.assertEquals(512, event.getDataAddress());
        Assert.assertEquals(3, event.getDataValue());
        Assert.assertEquals((1 << 24) + 2, event.getLocationId());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.WRITE_UNLOCK, event.getType());
        Assert.assertEquals(512, event.getLockId());
        Assert.assertEquals((1 << 24) + 2, event.getLocationId());

        event = reader.readEvent();
        Assert.assertEquals(5, event.getThreadId());
        Assert.assertEquals(EventType.READ, event.getType());
        Assert.assertEquals(256, event.getDataAddress());
        Assert.assertEquals(2, event.getDataValue());
        Assert.assertEquals((1 << 24) + 3, event.getLocationId());

        MoreAsserts.assertException(EOFException.class, reader::readEvent);
    }

    private static byte[] encodeInt(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
    }

    private static int encodePcDelta(int pcDelta, CompactEventReader.Type type) {
        return (pcDelta + Constants.JUMPS_IN_DELTA / 2) * CompactEventReader.Type.getNumberOfValues() + type.intValue();
    }
}
