package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMask;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;
import com.runtimeverification.rvpredict.log.compact.readers.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public abstract class CompactEvent {
    private static final List<CompactEvent> NO_EVENTS = Collections.emptyList();

    public interface Reader {
        int size(TraceHeader header) throws InvalidTraceDataException;
        List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer)
                throws InvalidTraceDataException;
    }
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
        THREAD_END(1, new NoDataReader(CompactEvent::endThread)),  // thread termination
        THREAD_SWITCH(18, new ThreadSyncReader(ThreadSyncType.SWITCH)),  // switch thread context

        LOCK_ACQUIRE(14, new LockManipulationReader(LockManipulationType.LOCK)),  // acquire lock
        LOCK_RELEASE(15, new LockManipulationReader(LockManipulationType.UNLOCK)),  // release lock

        FUNCTION_ENTER(16, new NoDataReader(CompactEvent::enterFunction)),  // enter a function
        FUNCTION_EXIT(17, new NoDataReader(CompactEvent::exitFunction)),  // exit a function

        CHANGE_OF_GENERATION(34, new ChangeOfGenerationReader()),  // change of generation

        SIG_ESTABLISH(35, new SignalEstablishReader()),  // establish signal action
        // signal delivery
        SIG_ENTER(36, new SignalEnterReader()),
        SIG_EXIT(37, new NoDataReader(CompactEvent::exitSignal)),
        SIG_DISESTABLISH(38, new SignalDisestablishReader()),  // disestablish signal action.
        // establish a new number -> mask mapping (memoize mask).
        SIG_MASK_MEMOIZATION(39, new SignalMaskMemoizationReader()),
        SIG_MASK(40, new SignalMaskReader()),  // mask signals
        // Set the number of signals running concurrently on the current thread.  Note that
        // this is a level of "concurrency," not a signal "depth," because the wrapper function for signal
        // handlers is reentrant, and it may race with itself to increase the
        // number of interrupts outstanding ("depth").
        SIG_OUTSTANDING_DEPTH(41, new SignalOutstandingDepthReader());
        // TODO(virgil): I think that this might be used only for counting, so it's not
        // needed in Java.
        // NOPS(42);

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

        public List<CompactEvent> read(Context context, TraceHeader header, InputStream stream)
                throws InvalidTraceDataException, IOException {
            if (buffer == null) {
                buffer = new byte[reader.size(header)];
            }
            if (buffer.length != stream.read(buffer)) {
                throw new InvalidTraceDataException("Short read for " + this + ".");
            }
            return reader.readEvent(context, header, ByteBuffer.wrap(buffer).order(header.getByteOrder()));
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

    public enum CompactType {
        READ,
        WRITE,

        // ATOMIC_READ,
        // ATOMIC_WRITE,
        ATOMIC_READ_THEN_WRITE,

        LOCK,

        UNLOCK,

        /**
         * Event generated before calling {@code Thread#start()}.
         */
        START,

        /**
         * Event generated after a thread is awakened from {@code Thread#join()}
         * because the joining thread finishes.
         */
        JOIN,

        INVOKE_METHOD,
        FINISH_METHOD,
    }

    private final long id;
    private final long threadId;
    private final CompactType compactType;

    private CompactEvent(Context context, CompactType compactType) {
        this.id = context.newId();
        this.threadId = context.getThreadId();
        this.compactType = compactType;
    }

    long getId() {
        return id;
    }
    long getThreadId() {
        return threadId;
    }
    CompactType getCompactType() {return compactType;}
    int dataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long dataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long value() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }

    public static List<CompactEvent> dataManipulation(
            Context context,
            DataManipulationType dataManipulationType,
            int dataSizeInBytes,
            long address,
            long value,
            Atomicity atomicity) {
        CompactType compactType;
        switch (dataManipulationType) {
            case LOAD:
                compactType = CompactType.READ;
                break;
            case STORE:
                compactType = CompactType.WRITE;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown data manipulation type: " + dataManipulationType);
        }
        return Collections.singletonList(new CompactEvent(context, compactType) {
            int dataSizeInBytes() {
                return dataSizeInBytes;
            }
            long dataAddress() {
                return address;
            }
            long value() {
                return value;
            }
        });
    }

    public static List<CompactEvent> atomicReadModifyWrite(
            Context context,
            int dataSizeInBytes,
            Address address, VariableInt readValue, VariableInt writeValue) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> changeOfGeneration(Context context, Generation generation) {
        context.changeOfGeneration(generation);
        return NO_EVENTS;
    }


    public static List<CompactEvent> join(Context context, ThreadId threadId) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> lockManipulation(
            Context context, LockManipulationType lockManipulationType, Address address) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> disestablishSignal(
            Context context, SignalNumber signalNumber) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> enterSignal(
            Context context, Generation generation, SignalNumber signalNumber) {
        context.enterSignal(generation);
        return Collections.singletonList(new CompactEvent(context) {
            zuma;
        });
    }

    public static List<CompactEvent> establishSignal(
            Context context,
            Address handler, SignalNumber signalNumber, SignalMask signalMask) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> threadSync(
            Context context, ThreadSyncType threadSyncType, ThreadId threadId) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    private static List<CompactEvent> endThread(Context context) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    private static List<CompactEvent> enterFunction(Context context) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    private static List<CompactEvent> exitSignal(Context context) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    private static List<CompactEvent> exitFunction(Context context) {
        return Collections.singletonList(new CompactEvent(context) {
        });
    }

    public static List<CompactEvent> begin(
            long minDeltaAndEventType, Context context, ThreadId threadId, Generation generation)
            throws InvalidTraceDataException {
        context.beginThread(minDeltaAndEventType, threadId, generation);
        return NO_EVENTS;
    }

    public static List<CompactEvent> jump(Context context, long address) {
        context.jump(address);
    }


    static CompactType dataManipulationTypeToCompactType(DataManipulationType dataManipulationType) {
        switch (dataManipulationType) {
            case LOAD:
                return CompactType.READ;
            case STORE:
                return CompactType.WRITE;
            default:
                throw new IllegalArgumentException("Unknown data manipulation type: " + dataManipulationType + ".");
        }
    }
}
