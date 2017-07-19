package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;
import com.runtimeverification.rvpredict.log.compact.readers.AtomicReadModifyWriteReader;
import com.runtimeverification.rvpredict.log.compact.readers.BlockSignalsReader;
import com.runtimeverification.rvpredict.log.compact.readers.ChangeOfGenerationReader;
import com.runtimeverification.rvpredict.log.compact.readers.DataManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.FunctionEnterReader;
import com.runtimeverification.rvpredict.log.compact.readers.GetSetSignalMaskReader;
import com.runtimeverification.rvpredict.log.compact.readers.GetSignalMaskReader;
import com.runtimeverification.rvpredict.log.compact.readers.LockManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.NoDataReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalDisestablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEnterReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEstablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskMemoizationReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalDepthReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadBeginReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadSyncReader;
import com.runtimeverification.rvpredict.log.compact.readers.UnblockSignalsReader;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class CompactEventReader implements IEventReader {
    enum Type {
        // load: 1, 2, 4, 8, 16 bytes wide.
        LOAD1(2, DataManipulationReader.createReader(1, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD2(3, DataManipulationReader.createReader(2, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD4(4, DataManipulationReader.createReader(4, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD8(5, DataManipulationReader.createReader(8, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD16(6, DataManipulationReader.createReader(16, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),

        // store: 1, 2, 4, 8, 16 bytes wide.
        STORE1(7, DataManipulationReader.createReader(1, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE2(8, DataManipulationReader.createReader(2, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE4(9, DataManipulationReader.createReader(4, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE8(10, DataManipulationReader.createReader(8, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE16(11, DataManipulationReader.createReader(16, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),

        // atomic load: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_LOAD1(24, DataManipulationReader.createReader(1, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD2(25, DataManipulationReader.createReader(2, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD4(26, DataManipulationReader.createReader(4, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD8(27, DataManipulationReader.createReader(8, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD16(28, DataManipulationReader.createReader(16, DataManipulationType.LOAD, Atomicity.ATOMIC)),

        // atomic store: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_STORE1(29, DataManipulationReader.createReader(1, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE2(30, DataManipulationReader.createReader(2, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE4(31, DataManipulationReader.createReader(4, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE8(32, DataManipulationReader.createReader(8, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE16(33, DataManipulationReader.createReader(16, DataManipulationType.STORE, Atomicity.ATOMIC)),

        // atomic read-modify-write: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_RMW1(19, AtomicReadModifyWriteReader.createReader(1)),
        ATOMIC_RMW2(20, AtomicReadModifyWriteReader.createReader(2)),
        ATOMIC_RMW4(21, AtomicReadModifyWriteReader.createReader(4)),
        ATOMIC_RMW8(22, AtomicReadModifyWriteReader.createReader(8)),
        ATOMIC_RMW16(23, AtomicReadModifyWriteReader.createReader(16)),

        THREAD_FORK(12, ThreadSyncReader.createReader(ThreadSyncType.FORK)),  // create a new thread
        THREAD_JOIN(13, ThreadSyncReader.createReader(ThreadSyncType.JOIN)),  // join an existing thread
        THREAD_BEGIN(0, ThreadBeginReader.createReader()),  // start of a thread
        THREAD_END(1, new NoDataReader(CompactEventFactory::endThread)),  // thread termination
        THREAD_SWITCH(18, ThreadSyncReader.createReader(ThreadSyncType.SWITCH)),  // switch thread context

        LOCK_ACQUIRE(14, LockManipulationReader.createReader(LockManipulationType.LOCK)),  // acquire lock
        LOCK_RELEASE(15, LockManipulationReader.createReader(LockManipulationType.UNLOCK)),  // release lock

        FUNCTION_ENTER(16, FunctionEnterReader.createReader()),  // enter a function
        FUNCTION_EXIT(17, new NoDataReader(CompactEventFactory::exitFunction)),  // exit a function

        CHANGE_OF_GENERATION(34, ChangeOfGenerationReader.createReader()),  // change of generation

        SIG_ESTABLISH(35, SignalEstablishReader.createReader()),  // establish signal action
        // signal delivery
        SIG_ENTER(36, SignalEnterReader.createReader()),
        SIG_EXIT(37, new NoDataReader(CompactEventFactory::exitSignal)),
        SIG_DISESTABLISH(38, SignalDisestablishReader.createReader()),  // disestablish signal action.
        // establish a new number -> mask mapping (memoize mask).
        SIG_MASK_MEMOIZATION(39, SignalMaskMemoizationReader.createReader()),
        SIG_SET_MASK(40, SignalMaskReader.createReader()),  // mask signals
        // Set the number of signals running concurrently on the current thread.
        SIG_DEPTH(41, SignalDepthReader.createReader()),
        BLOCK_SIGS(42, BlockSignalsReader.createReader()),
        UNBLOCK_SIGS(43, UnblockSignalsReader.createReader()),
        SIG_GETSET_MASK(44, GetSetSignalMaskReader.createReader()),
        SIG_GET_MASK(45, GetSignalMaskReader.createReader());

        private static int maxIntValue = 0;
        private static Map<Integer, Type> intToType;

        private final int intValue;
        private byte[] buffer;
        private Reader reader;

        Type(int intValue, Reader reader) {
            this.intValue = intValue;
            this.reader = reader;
        }
        public int intValue() {
            return intValue;
        }

        public static int getNumberOfValues() {
            return maxIntValue + 1;
        }

        public List<ReadonlyEventInterface> read(
                Context context, CompactEventFactory compactEventFactory,
                TraceHeader header, InputStreamByteCounterWrapper stream)
                throws InvalidTraceDataException, IOException {
            if (buffer == null) {
                buffer = new byte[reader.size(header)];
            }
            int readSize = stream.read(buffer);
            if (buffer.length != readSize) {
                throw new InvalidTraceDataException(
                        "Short read for " + this + ", expected " + buffer.length + " bytes, got " + readSize + ".");
            }
            return reader.readEvent(
                    context, compactEventFactory, header,
                    ByteBuffer.wrap(buffer).order(header.getByteOrder()));
        }

        static {
            OptionalInt max = EnumSet.allOf(Type.class).stream()
                    .mapToInt(Type::intValue)
                    .max();
            assert max.isPresent();
            maxIntValue = max.getAsInt();

            intToType = new HashMap<>();
            EnumSet.allOf(Type.class).forEach(event -> intToType.put(event.intValue(), event));
        }

        public static Type fromInt(int type) {
            return intToType.get(type);
        }
    }

    public enum Atomicity {
        ATOMIC,
        NOT_ATOMIC,
    }

    public enum DataManipulationType {
        LOAD,
        STORE,
    }

    public enum ThreadSyncType {
        FORK,
        JOIN,
        SWITCH,
    }
    public enum LockManipulationType {
        LOCK,
        UNLOCK,
    }

    public interface Reader {
        int size(TraceHeader header) throws InvalidTraceDataException;
        List<ReadonlyEventInterface> readEvent(
                Context context,
                CompactEventFactory compactEventFactory,
                TraceHeader header,
                ByteBuffer buffer)
                throws InvalidTraceDataException;
    }

    private TraceHeader header;
    private CompactEventFactory factory;
    private Context context;
    private InputStreamByteCounterWrapper inputStream;
    private ByteBuffer pcBuffer;
    private Address pc;
    private long minDeltaAndEventType;
    private long maxDeltaAndEventType;

    private List<ReadonlyEventInterface> events = Collections.emptyList();
    private int currentEvent = 0;

    public CompactEventReader(Path path) throws IOException, InvalidTraceDataException {
        this(new BufferedInputStream(new FileInputStream(path.toFile())));
    }
    public CompactEventReader(InputStream inputStream) throws IOException, InvalidTraceDataException {
        this.inputStream = new InputStreamByteCounterWrapper(inputStream);
        header = new TraceHeader(inputStream);
        pc = new Address(header);
        pcBuffer = ByteBuffer.allocate(pc.size()).order(header.getByteOrder());

        readData(inputStream, pc, "first event descriptor.");

        minDeltaAndEventType =
                pc.getAsLong() - (Constants.JUMPS_IN_DELTA / 2) * CompactEventReader.Type.getNumberOfValues();
        maxDeltaAndEventType =
                minDeltaAndEventType + Constants.JUMPS_IN_DELTA * CompactEventReader.Type.getNumberOfValues() - 1;

        context = new Context(minDeltaAndEventType);
        factory = new CompactEventFactory();

        DeltaAndEventType deltaAndEventType =
                DeltaAndEventType.parseFromPC(minDeltaAndEventType, maxDeltaAndEventType, pc.getAsLong());
        if (deltaAndEventType == null
                || deltaAndEventType.getEventType() != CompactEventReader.Type.THREAD_BEGIN) {
            throw new InvalidTraceDataException("All traces should start with begin, this one starts with "
                    + (deltaAndEventType == null ? "a jump" : deltaAndEventType.getEventType())
                    + ".");
        }

        ThreadId threadId = new ThreadId(header);
        readData(inputStream, threadId, "thread id for the start event");

        events = factory.beginThread(context, threadId.getAsLong(), 0);  // The first generation is always 0.

        currentEvent = -1;
        readEvent();
    }

    private void readData(InputStream inputStream, ReadableData data, String description)
            throws IOException, InvalidTraceDataException {
        ByteBuffer dataBuffer = ByteBuffer.allocate(data.size()).order(header.getByteOrder());
        int readSize = inputStream.read(dataBuffer.array());
        if (readSize != dataBuffer.capacity()) {
            throw new InvalidTraceDataException(
                    "Cannot read the " + description + ", expected " + dataBuffer.capacity()
                            + " bytes, got " + readSize + ".");
        }
        data.read(dataBuffer);
    }

    @Override
    public ReadonlyEventInterface readEvent() throws IOException {
        currentEvent++;
        while (currentEvent >= events.size()) {
            currentEvent = 0;
            try {
                int readCount = inputStream.read(pcBuffer.array());
                if (readCount == -1) {
                    events = null;
                    throw new EOFException();
                }
                if (readCount != pcBuffer.capacity()) {
                    throw new InvalidTraceDataException(
                            "Incomplete event descriptor, expected " + pcBuffer.capacity() + " bytes, got "
                                    + readCount + ".");
                }
                pcBuffer.rewind();
                pc.read(pcBuffer);
                DeltaAndEventType deltaAndEventType =
                        DeltaAndEventType.parseFromPC(minDeltaAndEventType,maxDeltaAndEventType, pc.getAsLong());
                if (deltaAndEventType == null) {
                    events = factory.jump(context, pc.getAsLong());
                    continue;
                }
                context.updatePcWithDelta(deltaAndEventType.getPcDelta());
                events = deltaAndEventType.getEventType().read(context, factory, header, inputStream);
            } catch(InvalidTraceDataException e) {
                throw new IOException(e);
            }
        }
        return events.get(currentEvent);
    }

    @Override
    public ReadonlyEventInterface lastReadEvent() {
        if (events == null) {
            return null;
        }
        return events.get(currentEvent);
    }

    @Override
    public long bytesRead() throws IOException {
        return inputStream.getBytesRead();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    static class InputStreamByteCounterWrapper {
        private final InputStream inputStream;
        private long bytesRead;

        private InputStreamByteCounterWrapper(InputStream inputStream) {
            this.inputStream = inputStream;
            this.bytesRead = 0;
        }

        private int read(byte[] buffer) throws IOException {
            int count = inputStream.read(buffer);
            bytesRead += count;
            return count;
        }

        private void close() throws IOException {
            inputStream.close();
        }

        private long getBytesRead() {
            return bytesRead;
        }
    }
}
