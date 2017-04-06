package com.runtimeverification.rvpredict.log.compact;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompactEventFactory {
    private static final List<CompactEvent> NO_EVENTS = Collections.emptyList();

    // TODO(virgil): Make the Context a private local variable here, I don't think anyone else needs it.

    public List<CompactEvent> dataManipulation(
            Context context,
            CompactEventReader.DataManipulationType dataManipulationType,
            int dataSizeInBytes,
            long address,
            long value,
            CompactEventReader.Atomicity atomicity) throws InvalidTraceDataException {
        CompactEvent.Type compactType;
        switch (dataManipulationType) {
            case LOAD:
                compactType = CompactEvent.Type.READ;
                break;
            case STORE:
                compactType = CompactEvent.Type.WRITE;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown data manipulation type: " + dataManipulationType);
        }
        if (atomicity == CompactEventReader.Atomicity.NOT_ATOMIC) {
            return Collections.singletonList(
                    dataManipulationEvent(context, dataSizeInBytes, address, value, compactType));
        }
        // TODO(virgil): These locks should be something more fine-grained, e.g. write_locks.
        // Also, it would probably be nice if an atomic write to a variable would also be atomic for
        // access to a part of the variable (i.e. union in c/c++), if that's indeed how it should
        // work.
        return Arrays.asList(
                lockManipulationEvent(context, CompactEventReader.LockManipulationType.LOCK, address),
                dataManipulationEvent(context, dataSizeInBytes, address, value, compactType),
                lockManipulationEvent(context, CompactEventReader.LockManipulationType.UNLOCK, address)
        );
    }

    private CompactEvent dataManipulationEvent(
            Context context, int dataSizeInBytes, long address, long value, CompactEvent.Type compactType) {
        return new CompactEvent(context, compactType) {
            int getDataSizeInBytes() {
                return dataSizeInBytes;
            }
            long getDataAddress() {
                return address;
            }
            long getDataValue() {
                return value;
            }
        };
    }

    public List<CompactEvent> atomicReadModifyWrite(
            Context context,
            int dataSizeInBytes,
            long address, long readValue, long writeValue) throws InvalidTraceDataException {
        return Arrays.asList(
                lockManipulationEvent(context, CompactEventReader.LockManipulationType.LOCK, address),
                dataManipulationEvent(context, dataSizeInBytes, address, readValue, CompactEvent.Type.READ),
                dataManipulationEvent(context, dataSizeInBytes, address, writeValue, CompactEvent.Type.WRITE),
                lockManipulationEvent(context, CompactEventReader.LockManipulationType.UNLOCK, address));
    }

    public List<CompactEvent> changeOfGeneration(Context context, long generation) {
        context.changeOfGeneration(generation);
        return NO_EVENTS;
    }

    public List<CompactEvent> lockManipulation(
            Context context, CompactEventReader.LockManipulationType lockManipulationType, long address)
            throws InvalidTraceDataException {
        return Collections.singletonList(lockManipulationEvent(context, lockManipulationType, address));
    }

    private CompactEvent lockManipulationEvent(
            Context context, CompactEventReader.LockManipulationType lockManipulationType, long address)
            throws InvalidTraceDataException {
        CompactEvent.Type compactType;
        switch (lockManipulationType) {
            case LOCK:
                compactType = CompactEvent.Type.LOCK;
                break;
            case UNLOCK:
                compactType = CompactEvent.Type.UNLOCK;
                break;
            default:
                throw new InvalidTraceDataException("Unknown lock manipulation type: " + lockManipulationType);
        }
        return new CompactEvent(context, compactType) {
            @Override
            long getLockAddress() {
                return address;
            }
        };
    }

    List<CompactEvent> jump(Context context, long address) throws InvalidTraceDataException {
        context.jump(address);
        return NO_EVENTS;
    }

    // Signal events.

    public List<CompactEvent> establishSignal(
            Context context,
            long handler, long signalNumber, long signalMaskNumber) {
        context.establishSignal(handler, signalNumber, signalMaskNumber);
        long signalMask = context.getMemoizedSignalMask(signalMaskNumber);
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.ESTABLISH_SIGNAL) {
            @Override
            long getSignalMask() {
                return signalMask;
            }
            @Override
            long getSignalNumber() {
                return signalNumber;
            }
            @Override
            long getSignalHandlerAddress() {
                return handler;
            }
        });
    }

    public List<CompactEvent> disestablishSignal(
            Context context, long signalNumber) {
        context.disestablishSignal(signalNumber);
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.DISESTABLISH_SIGNAL) {
            long getSignalNumber() {
                return signalNumber;
            }
        });
    }

    public List<CompactEvent> enterSignal(
            Context context, long generation, long signalNumber) throws InvalidTraceDataException {
        context.enterSignal(signalNumber, generation);
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.ENTER_SIGNAL) {
            long getSignalNumber() {
                return signalNumber;
            }
        });
    }

    List<CompactEvent> exitSignal(Context context) {
        long currentSignal = context.getSignalNumber();
        context.exitSignal();
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.EXIT_SIGNAL) {
            @Override
            long getSignalNumber() {
                return currentSignal;
            }
        });
    }

    public List<CompactEvent> signalOutstandingDepth(Context context, int signalDepth) {
        context.setSignalDepth(signalDepth);
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMaskMemoization(
            Context context, long signalMask, long originBitCount, long signalMaskNumber) {
        context.memoizeSignalMask(signalMask, originBitCount, signalMaskNumber);
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMask(Context context, long signalMaskNumber) {
        long signalMask = context.getMemoizedSignalMask(signalMaskNumber);
        context.maskSignals(signalMask);
        return NO_EVENTS;
    }

    // Function events.

    List<CompactEvent> enterFunction(Context context) {
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.ENTER_FUNCTION) {
        });
    }

    List<CompactEvent> exitFunction(Context context) {
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.EXIT_FUNCTION) {
        });
    }

    // Thread events.

    public List<CompactEvent> beginThread(Context context, long threadId, long generation)
            throws InvalidTraceDataException {
        context.beginThread(threadId, generation);
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.BEGIN_THREAD) {
        });
    }

    List<CompactEvent> endThread(Context context) {
        long threadId = context.getThreadId();
        context.endThread();
        return Collections.singletonList(new CompactEvent(context, CompactEvent.Type.END_THREAD) {
            @Override
            long getThreadId() {
                return threadId;
            }
        });
    }

    public List<CompactEvent> threadSync(
            Context context, CompactEventReader.ThreadSyncType threadSyncType, long threadId)
            throws InvalidTraceDataException {
        CompactEvent.Type compactType;
        switch (threadSyncType) {
            case JOIN:
                context.joinThread(threadId);
                compactType = CompactEvent.Type.JOIN_THREAD;
                break;
            case FORK:
                context.forkThread(threadId);
                compactType = CompactEvent.Type.FORK;
                break;
            case SWITCH:
                context.switchThread(threadId);
                return NO_EVENTS;
            default:
                throw new InvalidTraceDataException("Unknown thread sync event: " + threadSyncType);
        }
        return Collections.singletonList(new CompactEvent(context, compactType) {
            @Override
            long getOtherThreadId() {
                return threadId;
            }
        });
    }
}
