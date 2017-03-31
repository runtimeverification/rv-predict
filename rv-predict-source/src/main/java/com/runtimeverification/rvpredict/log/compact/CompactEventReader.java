package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.readers.AtomicReadModifyWriteReader;
import com.runtimeverification.rvpredict.log.compact.readers.ChangeOfGenerationReader;
import com.runtimeverification.rvpredict.log.compact.readers.DataManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.LockManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.NoDataReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalDisestablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEnterReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEstablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskMemoizationReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalOutstandingDepthReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadBeginReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadSyncReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class CompactEventReader {
    private static final List<CompactEvent> NO_EVENTS = Collections.emptyList();

    enum Type {
        // load: 1, 2, 4, 8, 16 bytes wide.
        LOAD1(2, new DataManipulationReader(1, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD2(3, new DataManipulationReader(2, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD4(4, new DataManipulationReader(4, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD8(5, new DataManipulationReader(8, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),
        LOAD16(6, new DataManipulationReader(16, DataManipulationType.LOAD, Atomicity.NOT_ATOMIC)),

        // store: 1, 2, 4, 8, 16 bytes wide.
        STORE1(7, new DataManipulationReader(1, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE2(8, new DataManipulationReader(2, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE4(9, new DataManipulationReader(4, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE8(10, new DataManipulationReader(8, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),
        STORE16(11, new DataManipulationReader(16, DataManipulationType.STORE, Atomicity.NOT_ATOMIC)),

        // atomic load: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_LOAD1(24, new DataManipulationReader(1, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD2(25, new DataManipulationReader(2, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD4(26, new DataManipulationReader(4, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD8(27, new DataManipulationReader(8, DataManipulationType.LOAD, Atomicity.ATOMIC)),
        ATOMIC_LOAD16(28, new DataManipulationReader(16, DataManipulationType.LOAD, Atomicity.ATOMIC)),

        // atomic store: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_STORE1(29, new DataManipulationReader(1, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE2(30, new DataManipulationReader(2, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE4(31, new DataManipulationReader(4, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE8(32, new DataManipulationReader(8, DataManipulationType.STORE, Atomicity.ATOMIC)),
        ATOMIC_STORE16(33, new DataManipulationReader(16, DataManipulationType.STORE, Atomicity.ATOMIC)),

        // atomic read-modify-write: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_RMW1(19, new AtomicReadModifyWriteReader(1)),
        ATOMIC_RMW2(20, new AtomicReadModifyWriteReader(2)),
        ATOMIC_RMW4(21, new AtomicReadModifyWriteReader(4)),
        ATOMIC_RMW8(22, new AtomicReadModifyWriteReader(8)),
        ATOMIC_RMW16(23, new AtomicReadModifyWriteReader(16)),

        THREAD_FORK(12, new ThreadSyncReader(ThreadSyncType.FORK)),  // create a new thread
        THREAD_JOIN(13, new ThreadSyncReader(ThreadSyncType.JOIN)),  // join an existing thread
        THREAD_BEGIN(0, new ThreadBeginReader()),  // start of a thread
        THREAD_END(1, new NoDataReader(CompactEventReader::endThread)),  // thread termination
        THREAD_SWITCH(18, new ThreadSyncReader(ThreadSyncType.SWITCH)),  // switch thread context

        LOCK_ACQUIRE(14, new LockManipulationReader(LockManipulationType.LOCK)),  // acquire lock
        LOCK_RELEASE(15, new LockManipulationReader(LockManipulationType.UNLOCK)),  // release lock

        FUNCTION_ENTER(16, new NoDataReader(CompactEventReader::enterFunction)),  // enter a function
        FUNCTION_EXIT(17, new NoDataReader(CompactEventReader::exitFunction)),  // exit a function

        CHANGE_OF_GENERATION(34, new ChangeOfGenerationReader()),  // change of generation

        SIG_ESTABLISH(35, new SignalEstablishReader()),  // establish signal action
        // signal delivery
        SIG_ENTER(36, new SignalEnterReader()),
        SIG_EXIT(37, new NoDataReader(CompactEventReader::exitSignal)),
        SIG_DISESTABLISH(38, new SignalDisestablishReader()),  // disestablish signal action.
        // establish a new number -> mask mapping (memoize mask).
        SIG_MASK_MEMOIZATION(39, new SignalMaskMemoizationReader()),
        SIG_MASK(40, new SignalMaskReader()),  // mask signals
        // Set the number of signals running concurrently on the current thread.  Note that
        // this is a level of "concurrency," not a signal "depth," because the wrapper function for signal
        // handlers is reentrant, and it may race with itself to increase the
        // number of interrupts outstanding ("depth").
        SIG_OUTSTANDING_DEPTH(41, new SignalOutstandingDepthReader());

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

        public List<CompactEvent> read(
                Context context, CompactEventReader compactEventReader,
                TraceHeader header, InputStream stream)
                throws InvalidTraceDataException, IOException {
            if (buffer == null) {
                buffer = new byte[reader.size(header)];
            }
            if (buffer.length != stream.read(buffer)) {
                throw new InvalidTraceDataException("Short read for " + this + ".");
            }
            return reader.readEvent(
                    context, compactEventReader, header,
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
        List<CompactEvent> readEvent(
                Context context,
                CompactEventReader compactEventReader,
                TraceHeader header,
                ByteBuffer buffer)
                throws InvalidTraceDataException;
    }

    public List<CompactEvent> dataManipulation(
            Context context,
            DataManipulationType dataManipulationType,
            int dataSizeInBytes,
            long address,
            long value,
            Atomicity atomicity) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    public List<CompactEvent> atomicReadModifyWrite(
            Context context,
            int dataSizeInBytes,
            long address, long readValue, long writeValue) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    public List<CompactEvent> changeOfGeneration(Context context, long generation) {
        return NO_EVENTS;
    }

    public List<CompactEvent> lockManipulation(
            Context context, LockManipulationType lockManipulationType, long address)
            throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    static List<CompactEvent> jump(Context context, long address) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    // Signal events.

    public List<CompactEvent> establishSignal(
            Context context,
            long handler, long signalNumber, long signalMaskNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> disestablishSignal(
            Context context, long signalNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> enterSignal(
            Context context, long generation, long signalNumber) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    private static List<CompactEvent> exitSignal(Context context) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalOutstandingDepth(Context context, int signalDepth) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMaskMemoization(
            Context context, long signalMask, long originBitCount, long signalMaskNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMask(Context context, long signalMaskNumber) {
        return NO_EVENTS;
    }

    // Function events.

    private static List<CompactEvent> enterFunction(Context context) {
        return NO_EVENTS;
    }

    private static List<CompactEvent> exitFunction(Context context) {
        return NO_EVENTS;
    }

    // Thread events.

    public List<CompactEvent> beginThread(Context context, long threadId, long generation)
            throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    private static List<CompactEvent> endThread(Context context) {
        return NO_EVENTS;
    }

    public List<CompactEvent> threadSync(
            Context context, ThreadSyncType threadSyncType, long threadId) throws InvalidTraceDataException {
        return NO_EVENTS;
    }
}