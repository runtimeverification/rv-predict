package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.ThreadInfos;
import com.runtimeverification.rvpredict.trace.TraceState;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import static com.runtimeverification.rvpredict.log.compact.Constants.LONG_SIZE_IN_BYTES;
import static org.mockito.Mockito.when;

public class TraceUtils {
    private final Context mockContext;
    private final CompactEventFactory compactEventFactory;
    private final Map<Long, Map<Integer, ThreadData>> threadIdToSignalDepthToThreadData;

    private long nextPc;
    private long threadId;
    private int signalDepth;
    private int nextThreadNumber;
    private Optional<TraceState> traceState;

    public TraceUtils(Context context, long initialThreadId, int initialSignalDepth, long nextPc) {
        this.mockContext = context;
        this.compactEventFactory = new CompactEventFactory();
        this.threadId = initialThreadId;
        this.signalDepth = initialSignalDepth;
        this.nextPc = nextPc;
        this.nextThreadNumber = 1;
        this.threadIdToSignalDepthToThreadData = new HashMap<>();
        this.traceState = Optional.empty();
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

    public List<ReadonlyEventInterface> enterFunction(long canonicalFrameAddress, OptionalLong callSiteAddress) {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.enterFunction(mockContext, canonicalFrameAddress, callSiteAddress);
    }

    public static ReadonlyEventInterface extractSingleEvent(List<ReadonlyEventInterface> events) {
        Assert.assertEquals(1, events.size());
        return events.get(0);
    }

    public static ReadonlyEventInterface extractFirstEvent(List<ReadonlyEventInterface> events) {
        Assert.assertFalse(events.isEmpty());
        return events.get(0);
    }

    public static ReadonlyEventInterface extractLastEvent(List<ReadonlyEventInterface> events) {
        Assert.assertFalse(events.isEmpty());
        return events.get(events.size() - 1);
    }

    public static ReadonlyEventInterface extractEventByType(List<ReadonlyEventInterface> events, EventType type) {
        List<ReadonlyEventInterface> matchingEvents =
                events.stream().filter(event -> event.getType() == type).collect(Collectors.toList());
        Assert.assertEquals(1, matchingEvents.size());
        return matchingEvents.get(0);
    }

    private void prepareContextForEvent(long threadId, int signalDepth) {
        when(mockContext.getPC()).thenReturn(nextPc);
        nextPc++;
        when(mockContext.getThreadId()).thenReturn(threadId);
        when(mockContext.getSignalDepth()).thenReturn(signalDepth);
    }

    public RawTrace extractRawTrace(List<List<ReadonlyEventInterface>> events, long thread, int signalDepth) {
        return extractRawTrace(true, events, thread, signalDepth, OptionalLong.empty(), OptionalLong.empty());
    }

    public RawTrace extractRawTrace(
            boolean threadStartsInTheCurrentWindow,
            List<List<ReadonlyEventInterface>> events, long thread, int signalDepth) {
        return extractRawTrace(
                threadStartsInTheCurrentWindow,
                events, thread, signalDepth, OptionalLong.empty(), OptionalLong.empty());
    }

    public RawTrace extractRawTrace(
            List<List<ReadonlyEventInterface>> events, long thread, int signalDepth,
            long firstEventId, long lastEventId) {
        return extractRawTrace(
                true,
                events, thread, signalDepth, OptionalLong.of(firstEventId), OptionalLong.of(lastEventId));
    }

    public RawTrace extractRawTrace(
            boolean threadStartsInTheCurrentWindow,
            List<List<ReadonlyEventInterface>> events, long thread, int signalDepth,
            long firstEventId, long lastEventId) {
        return extractRawTrace(
                threadStartsInTheCurrentWindow,
                events, thread, signalDepth, OptionalLong.of(firstEventId), OptionalLong.of(lastEventId));
    }

    private RawTrace extractRawTrace(
            boolean threadStartsInTheCurrentWindow,
            List<List<ReadonlyEventInterface>> events, long thread, int signalDepth,
            OptionalLong firstEventId, OptionalLong lastEventId) {
        List<List<ReadonlyEventInterface>> traceEvents = new ArrayList<>();
        for (List<ReadonlyEventInterface> eventList : events) {
            if (eventList.isEmpty()) {
                continue;
            }
            ReadonlyEventInterface firstEvent = eventList.get(0);
            if (firstEvent.getOriginalThreadId() == thread
                    && firstEvent.getSignalDepth() == signalDepth
                    && (!firstEventId.isPresent() || firstEventId.getAsLong() <= firstEvent.getEventId())
                    && (!lastEventId.isPresent() || lastEventId.getAsLong() >= firstEvent.getEventId())) {
                traceEvents.add(eventList);
            }
        }
        @SuppressWarnings("unchecked")
        List<ReadonlyEventInterface> traceArray[] = new List[traceEvents.size()];
        for (int i = 0; i < traceEvents.size(); i++) {
            traceArray[i] = traceEvents.get(i);
        }
        return createRawTrace(threadStartsInTheCurrentWindow, traceArray);
    }

    @SafeVarargs
    public final RawTrace createRawTrace(List<ReadonlyEventInterface>... events) {
        return createRawTrace(true, events);
    }

    @SafeVarargs
    public final RawTrace createRawTrace(
            boolean threadStartsInTheCurrentWindow, List<ReadonlyEventInterface>... events) {
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
        OptionalLong signalNumber = OptionalLong.empty();
        OptionalLong signalHandler = OptionalLong.empty();
        for (List<ReadonlyEventInterface> eventList : events) {
            for (ReadonlyEventInterface event : eventList) {
                if (event.getType() == EventType.ENTER_SIGNAL) {
                    signalNumber = OptionalLong.of(event.getSignalNumber());
                    signalHandler = OptionalLong.of(event.getSignalHandlerAddress());
                }
                paddedEvents[pos] = event;
                pos++;
            }
        }
        int currentThreadNumber;
        if (threadStartsInTheCurrentWindow) {
            currentThreadNumber = this.nextThreadNumber;
            threadIdToSignalDepthToThreadData
                    .computeIfAbsent(paddedEvents[0].getOriginalThreadId(), k -> new HashMap<>())
                    .put(
                            paddedEvents[0].getSignalDepth(),
                            new ThreadData(currentThreadNumber, signalNumber, signalHandler));
            this.nextThreadNumber++;
        } else {
            ThreadData threadData = threadIdToSignalDepthToThreadData
                    .computeIfAbsent(paddedEvents[0].getOriginalThreadId(), k -> new HashMap<>())
                    .get(paddedEvents[0].getSignalDepth());
            currentThreadNumber = threadData.getTtid();
            signalNumber = threadData.getSignalNumber();
            signalHandler = threadData.getSignalHandler();
        }
        ThreadInfo threadInfo;
        Optional<TraceState> localTraceState = traceState;
        if (localTraceState.isPresent()) {
            if (paddedEvents[0].getSignalDepth() == 0) {
                threadInfo = localTraceState.get().createAndRegisterThreadInfo(paddedEvents[0].getOriginalThreadId(), OptionalInt.empty());
            } else {
                assert signalNumber.isPresent();
                assert signalHandler.isPresent();
                threadInfo = localTraceState.get().createAndRegisterSignalInfo(
                        paddedEvents[0].getOriginalThreadId(),
                        signalNumber.getAsLong(),
                        signalHandler.getAsLong(),
                        paddedEvents[0].getSignalDepth());
            }
        } else {
            if (paddedEvents[0].getSignalDepth() == 0) {
                threadInfo = ThreadInfo.createThreadInfo(
                        currentThreadNumber, paddedEvents[0].getOriginalThreadId(), OptionalInt.empty());
            } else {
                assert signalNumber.isPresent();
                assert signalHandler.isPresent();
                threadInfo = ThreadInfo.createSignalInfo(
                        currentThreadNumber,
                        paddedEvents[0].getOriginalThreadId(),
                        signalNumber.getAsLong(),
                        signalHandler.getAsLong(),
                        paddedEvents[0].getSignalDepth());
            }
        }
        return new RawTrace(0, pos, paddedEvents, threadInfo);
    }

    public void setNextThreadNumber(int threadNumber) {
        this.nextThreadNumber = threadNumber;
    }

    @SafeVarargs
    public final List<ReadonlyEventInterface> flatten(List<ReadonlyEventInterface>... events) {
        List<ReadonlyEventInterface> flattened = new ArrayList<>();
        for (List<ReadonlyEventInterface> eventList : events) {
            flattened.addAll(eventList);
        }
        return flattened;
    }

    public void setTraceState(TraceState traceState) {
        this.traceState = Optional.of(traceState);
    }

    public static void addThreadInfoToMocks(
            ThreadInfos mockThreadInfos, TraceState mockTraceState, ThreadInfo... threadInfos) {
        Set<Integer> ttids = new HashSet<>();
        for (ThreadInfo threadInfo : threadInfos) {
            Assert.assertFalse(ttids.contains(threadInfo.getId()));
            ttids.add(threadInfo.getId());
            when(mockThreadInfos.getThreadInfo(threadInfo.getId())).thenReturn(threadInfo);
            when(
                    mockTraceState.getTtidForThreadOngoingAtWindowStart(
                            threadInfo.getOriginalThreadId(), threadInfo.getSignalDepth()))
                    .thenReturn(OptionalInt.of(threadInfo.getId()));
        }
        when(mockTraceState.getThreadsForCurrentWindow()).thenReturn(ttids);
    }

    private static class ThreadData {
        private final int ttid;
        private final OptionalLong signalNumber;
        private final OptionalLong signalHandler;

        private ThreadData(int ttid, OptionalLong signalNumber, OptionalLong signalHandler) {
            this.ttid = ttid;
            this.signalNumber = signalNumber;
            this.signalHandler = signalHandler;
        }

        private int getTtid() {
            return ttid;
        }

        private OptionalLong getSignalNumber() {
            return signalNumber;
        }

        private OptionalLong getSignalHandler() {
            return signalHandler;
        }
    }
}
