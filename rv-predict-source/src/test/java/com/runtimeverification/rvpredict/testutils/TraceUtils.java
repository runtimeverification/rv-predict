package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.runtimeverification.rvpredict.log.compact.Constants.LONG_SIZE_IN_BYTES;
import static org.mockito.Mockito.when;

public class TraceUtils {
    private final Context mockContext;
    private final CompactEventFactory compactEventFactory;

    private long nextPc;
    private long threadId;
    private int signalDepth;
    private int nextThreadNumber;

    public TraceUtils(Context context, long initialThreadId, int initialSignalDepth, long nextPc) {
        this.mockContext = context;
        this.compactEventFactory = new CompactEventFactory();
        this.threadId = initialThreadId;
        this.signalDepth = initialSignalDepth;
        this.nextPc = nextPc;
        this.nextThreadNumber = 1;
    }

    public List<ReadonlyEventInterface> switchThread(long threadId, int signalDepth) {
        this.threadId = threadId;
        this.signalDepth = signalDepth;
        return Collections.emptyList();
    }

    public List<ReadonlyEventInterface> setPc(long nextPc) {
        this.nextPc = nextPc;
        return Collections.emptyList();
    }

    public List<ReadonlyEventInterface> enterSignal(
            long signalNumber, long signalHandler, long generation) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.enterSignal(mockContext, generation, signalNumber, signalHandler);
    }

    public List<ReadonlyEventInterface> exitSignal() throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.exitSignal(mockContext);
    }

    public List<ReadonlyEventInterface> setSignalHandler(
            long signalNumber, long signalHandler, long disabledSignalMask)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(disabledSignalMask);
        return compactEventFactory.establishSignal(mockContext, signalHandler, signalNumber, 123);
    }

    public List<ReadonlyEventInterface> getSignalMask(long signalMask) {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(signalMask);
        return compactEventFactory.getSignalMask(mockContext, 123);
    }

    public List<ReadonlyEventInterface> setSignalMask(long signalMask) {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(signalMask);
        return compactEventFactory.signalMask(mockContext, 123);
    }

    public List<ReadonlyEventInterface> getSetSignalMask(
            long readSignalMask, long writeSignalMask) {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(readSignalMask);
        when(mockContext.getMemoizedSignalMask(124)).thenReturn(writeSignalMask);
        return compactEventFactory.getSetSignalMask(mockContext, 123, 124);
    }

    public List<ReadonlyEventInterface> disestablishSignal(long signalNumber)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.disestablishSignal(mockContext, signalNumber);
    }

    public List<ReadonlyEventInterface> disableSignal(long signalNumber)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(1L << Math.toIntExact(signalNumber));
        return compactEventFactory.blockSignals(mockContext, 123);
    }

    public List<ReadonlyEventInterface> enableSignal(long signalNumber)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(1L << Math.toIntExact(signalNumber));
        return compactEventFactory.unblockSignals(mockContext, 123);
    }

    public List<ReadonlyEventInterface> lock(long lockId)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.lockManipulation(mockContext, CompactEventReader.LockManipulationType.LOCK, lockId);
    }

    public List<ReadonlyEventInterface> unlock(long lockId)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.lockManipulation(
                mockContext, CompactEventReader.LockManipulationType.UNLOCK, lockId);
    }

    public List<ReadonlyEventInterface> threadStart(long newThread)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.threadSync(
                mockContext, CompactEventReader.ThreadSyncType.FORK, newThread);
    }

    public List<ReadonlyEventInterface> threadJoin(long newThread)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.threadSync(
                mockContext, CompactEventReader.ThreadSyncType.JOIN, newThread);
    }

    public List<ReadonlyEventInterface> nonAtomicLoad(
            long address, long value) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.LOAD, LONG_SIZE_IN_BYTES,
                address, value,
                CompactEventReader.Atomicity.NOT_ATOMIC);
    }

    public List<ReadonlyEventInterface> nonAtomicStore(
            long address, long value) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.STORE, LONG_SIZE_IN_BYTES,
                address, value,
                CompactEventReader.Atomicity.NOT_ATOMIC);
    }

    public List<ReadonlyEventInterface> atomicLoad(
            long address, long value) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.LOAD, LONG_SIZE_IN_BYTES,
                address, value,
                CompactEventReader.Atomicity.ATOMIC);
    }

    public List<ReadonlyEventInterface> atomicStore(long address, long value) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.STORE, LONG_SIZE_IN_BYTES,
                address, value,
                CompactEventReader.Atomicity.ATOMIC);
    }

    public List<ReadonlyEventInterface> enterFunction(long canonicalFrameAddress) {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.enterFunction(mockContext, canonicalFrameAddress);
    }

    public static ReadonlyEventInterface extractSingleEvent(List<ReadonlyEventInterface> events) {
        Assert.assertEquals(1, events.size());
        return events.get(0);
    }

    private void prepareContextForEvent(long threadId, int signalDepth) {
        when(mockContext.getPC()).thenReturn(nextPc);
        nextPc++;
        when(mockContext.getThreadId()).thenReturn(threadId);
        when(mockContext.getSignalDepth()).thenReturn(signalDepth);
    }

    public RawTrace extractRawTrace(List<List<ReadonlyEventInterface>> events, long thread, int signalDepth) {
        List<List<ReadonlyEventInterface>> traceEvents = new ArrayList<>();
        for (List<ReadonlyEventInterface> eventList : events) {
            if (eventList.isEmpty()) {
                continue;
            }
            ReadonlyEventInterface firstEvent = eventList.get(0);
            if (firstEvent.getOriginalThreadId() == thread
                    && firstEvent.getSignalDepth() == signalDepth) {
                traceEvents.add(eventList);
            }
        }
        @SuppressWarnings("unchecked")
        List<ReadonlyEventInterface> traceArray[] = new List[traceEvents.size()];
        for (int i = 0; i < traceEvents.size(); i++) {
            traceArray[i] = traceEvents.get(i);
        }
        return createRawTrace(traceArray);
    }

    @SafeVarargs
    public final RawTrace createRawTrace(List<ReadonlyEventInterface>... events) {
        int size = 0;
        for (List<ReadonlyEventInterface> eventList : events) {
            size += eventList.size();
        }
        int paddedSize = 1;
        while (paddedSize <= size) {
            paddedSize = paddedSize * 2;
        }
        ReadonlyEventInterface[] paddedEvents = new ReadonlyEventInterface[paddedSize];
        int pos = 0;
        for (List<ReadonlyEventInterface> eventList : events) {
            for (ReadonlyEventInterface event : eventList) {
                paddedEvents[pos] = event;
                pos++;
            }
        }
        int currentThreadNumber = this.nextThreadNumber;
        this.nextThreadNumber++;
        return new RawTrace(0, pos, paddedEvents, paddedEvents[0].getSignalDepth(), currentThreadNumber);
    }
    @SafeVarargs
    public final List<ReadonlyEventInterface> flatten(List<ReadonlyEventInterface>... events) {
        List<ReadonlyEventInterface> flattened = new ArrayList<>();
        for (List<ReadonlyEventInterface> eventList : events) {
            flattened.addAll(eventList);
        }
        return flattened;
    }
}
