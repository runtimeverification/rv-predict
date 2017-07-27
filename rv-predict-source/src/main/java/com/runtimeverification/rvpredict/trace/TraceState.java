package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.trace.maps.ThreadIDToObjectMap;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

public class TraceState {

    private static final int DEFAULT_NUM_OF_THREADS = 1024;

    private static final int DEFAULT_NUM_OF_ADDR = 128;

    private static final int DEFAULT_NUM_OF_LOCKS = 32;

    /**
     * Map form thread ID to the current level of class initialization.
     */
    private ThreadIDToObjectMap<MutableInt> tidToClinitDepth = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, MutableInt::new);

    /**
     * Map from thread ID to the current stack trace elements.
     */
    private ThreadIDToObjectMap<Deque<ReadonlyEventInterface>> tidToStacktrace = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, ArrayDeque::new);

    /**
     * Map from (thread ID, lock ID) to lock state.
     */
    private final Table<Integer, Long, LockState> tidToLockIdToLockState = HashBasedTable.create(
            DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_LOCKS);

    private final Map<Long, Map<Long, ReadonlyEventInterface>> t_signalNumberToHandlerToLastWindowEstablishEvents =
            new HashMap<>();

    private final StateAtWindowBorder stateAtCurrentWindowStart = new StateAtWindowBorder();
    private final StateAtWindowBorder stateAtCurrentWindowEnd = new StateAtWindowBorder();

    /**
     * Map from a thread ID to the information about that thread.
     */
    private final Map<Integer, ThreadInfo> ttidToThreadInfo = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);

    private final Configuration config;

    private final MetadataInterface metadata;

    private final Map<Long, Integer> t_eventIdToTtid;

    private final Map<Long, Integer> t_originalTidToTraceTid;

    private final Map<Integer, List<ReadonlyEventInterface>> t_tidToEvents;

    private final Map<Integer, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Integer, ThreadState> t_tidToThreadState;

    private final MemoryAddrToStateMap t_addrToState;

    private final Table<Integer, Long, List<ReadonlyEventInterface>> t_tidToAddrToEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<ReadonlyEventInterface> t_clinitEvents;

    private final Map<Integer, ReadonlyEventInterface> t_ttidToStartEvent;

    private final Map<Integer, ReadonlyEventInterface> t_ttidToJoinEvent;

    private final Map<Long, Set<Integer>> t_signalToTtidWhereEnabledAtStart;

    private final Map<Long, Set<Integer>> t_signalToTtidWhereDisabledAtStart;

    private final Map<Integer, Set<Integer>> t_ttidsThatCanOverlap;

    private final Map<Long, Map<Integer, Boolean>> t_signalIsEnabledForThreadCache;

    private final Map<Long, Map<Long, Boolean>> t_atLeastOneSigsetAllowsSignalCache;

    private final Map<Long, Map<Long, List<ReadonlyEventInterface>>> t_signalNumberToSignalHandlerToEstablishSignalEvents;

    private int t_threadId;

    public TraceState(Configuration config, MetadataInterface metadata) {
        this.config = config;
        this.metadata = metadata;

        this.t_eventIdToTtid           = new LinkedHashMap<>();
        this.t_originalTidToTraceTid   = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToEvents             = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToMemoryAccessBlocks = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToThreadState        = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_addrToState             = new MemoryAddrToStateMap(config.windowSize);
        this.t_tidToAddrToEvents       = HashBasedTable.create(DEFAULT_NUM_OF_THREADS,
                                            DEFAULT_NUM_OF_ADDR);
        this.t_lockIdToLockRegions     = new LinkedHashMap<>(config.windowSize >> 1);
        this.t_clinitEvents            = new HashSet<>(config.windowSize >> 1);
        this.t_ttidToStartEvent        = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_ttidToJoinEvent         = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_signalToTtidWhereEnabledAtStart = new HashMap<>();
        this.t_signalToTtidWhereDisabledAtStart = new HashMap<>();
        this.t_ttidsThatCanOverlap = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_signalIsEnabledForThreadCache = new HashMap<>();
        this.t_atLeastOneSigsetAllowsSignalCache = new HashMap<>();
        this.t_signalNumberToSignalHandlerToEstablishSignalEvents = new HashMap<>();
        this.t_threadId                = 1;
    }

    public Configuration config() {
        return config;
    }

    public MetadataInterface metadata() {
        return metadata;
    }

    private int tmp = 0;

    public Trace initNextTraceWindow(List<RawTrace> rawTraces) {
        tmp++;
        if (tmp == 13) {
            System.out.println("Lucky you!");
        }
        System.out.println("--- Window ---");
        for (RawTrace rawTrace : rawTraces) {
            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (event.isSignalEvent()) {
                    System.out.println(event);
                }
            }
        }
        t_eventIdToTtid.clear();
        t_originalTidToTraceTid.clear();
        t_tidToEvents.clear();
        t_tidToMemoryAccessBlocks.clear();
        t_tidToThreadState.clear();
        t_addrToState.clear();
        t_tidToAddrToEvents.clear();
        t_lockIdToLockRegions.clear();
        t_clinitEvents.clear();
        t_ttidToStartEvent.clear();
        t_ttidToJoinEvent.clear();
        t_signalToTtidWhereEnabledAtStart.clear();
        t_signalToTtidWhereDisabledAtStart.clear();
        t_ttidsThatCanOverlap.clear();
        t_signalIsEnabledForThreadCache.clear();
        t_atLeastOneSigsetAllowsSignalCache.clear();
        t_originalTidToTraceTid.clear();
        t_signalNumberToSignalHandlerToEstablishSignalEvents.clear();
        return new Trace(this, rawTraces,
                t_eventIdToTtid,
                t_tidToEvents,
                t_tidToMemoryAccessBlocks,
                t_tidToThreadState,
                t_addrToState,
                t_tidToAddrToEvents,
                t_lockIdToLockRegions,
                t_clinitEvents,
                t_ttidToStartEvent,
                t_ttidToJoinEvent,
                t_signalToTtidWhereEnabledAtStart,
                t_signalToTtidWhereDisabledAtStart,
                t_ttidsThatCanOverlap,
                t_signalIsEnabledForThreadCache,
                t_atLeastOneSigsetAllowsSignalCache,
                t_originalTidToTraceTid,
                t_signalNumberToSignalHandlerToEstablishSignalEvents);
    }

    public int acquireLock(ReadonlyEventInterface lock, int ttid) {
        lock = lock.copy();
        LockState st = tidToLockIdToLockState.row(ttid)
                .computeIfAbsent(lock.getLockId(), LockState::new);
        st.acquire(lock);
        return lock.isReadLock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public int releaseLock(ReadonlyEventInterface unlock, int ttid) {
        LockState st = tidToLockIdToLockState.get(ttid, unlock.getLockId());
        if (st == null) return -1;

        st.release(unlock);
        return unlock.isReadUnlock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public void onMetaEvent(ReadonlyEventInterface event, int ttid) {
        switch (event.getType()) {
        case CLINIT_ENTER:
            tidToClinitDepth.computeIfAbsent(ttid).increment();
            tidToClinitDepth = ThreadIDToObjectMap.growOnFull(tidToClinitDepth);
            break;
        case CLINIT_EXIT:
            tidToClinitDepth.get(ttid).decrement();
            break;
        case INVOKE_METHOD:
            tidToStacktrace.computeIfAbsent(ttid).add(event.copy());
            tidToStacktrace = ThreadIDToObjectMap.growOnFull(tidToStacktrace);
            break;
        case FINISH_METHOD:
	    ReadonlyEventInterface lastEvent = tidToStacktrace.get(ttid).removeLast();
            long locId = lastEvent.getLocationId();
            if (locId != event.getLocationId()) {
                throw new IllegalStateException("Unmatched method entry/exit events!" +
                        (Configuration.debug ?
                        "\n\tENTRY:" + metadata.getLocationSig(locId) + " gid " + lastEvent.getEventId() +
                        "\n\tEXIT:" + metadata.getLocationSig(event.getLocationId()) + " gid " + event.getEventId() : ""));
            }
            break;
        default:
            throw new IllegalArgumentException("Unexpected event type: " + event.getType());
        }
    }

    public void onSignalEvent(ReadonlyEventInterface event) {
        if (event.getType() == EventType.ESTABLISH_SIGNAL) {
            Optional<ReadonlyEventInterface> previousEvent =
                    getPreviousEstablishEvent(event.getSignalNumber(), event.getSignalHandlerAddress());
            if (!previousEvent.isPresent() || previousEvent.get().getEventId() < event.getEventId()) {
                t_signalNumberToHandlerToLastWindowEstablishEvents
                        .computeIfAbsent(event.getSignalNumber(), k -> new HashMap<>())
                        .put(event.getSignalHandlerAddress(), event);
            }
        }
    }

    public boolean isInsideClassInitializer(int ttid) {
        try {
            return tidToClinitDepth.computeIfAbsent(ttid).intValue() > 0;
        } finally {
            tidToClinitDepth = ThreadIDToObjectMap.growOnFull(tidToClinitDepth);
        }
    }

    public ThreadState getThreadStateSnapshot(int ttid) {
        /* copy stack trace */
        Deque<ReadonlyEventInterface> stacktrace = tidToStacktrace.get(ttid);
        stacktrace = stacktrace == null ? new ArrayDeque<>() : new ArrayDeque<>(stacktrace);
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        tidToLockIdToLockState.row(ttid).values().forEach(st -> lockStates.add(st.copy()));
        return new ThreadState(stacktrace, lockStates);
    }

    /**
     * Fast-path implementation for event processing that is specialized for the
     * single-threading case.
     * <p>
     * No need to create the {@link Trace} object because there can't be races.
     * The only task is to update this global trace state.
     *
     * @param rawTrace
     */
    public void fastProcess(RawTrace rawTrace) {
        int ttid = rawTrace.getThreadInfo().getId();
        setThreadInfo(ttid, rawTrace.getThreadInfo());
        for (int i = 0; i < rawTrace.size(); i++) {
            ReadonlyEventInterface event = rawTrace.event(i);
            if (event.isLock() && !event.isWaitAcq()) {
                event = updateLockLocToUserLoc(event, ttid);
                acquireLock(event, ttid);
            } else if (event.isUnlock() && !event.isWaitRel()) {
                releaseLock(event, ttid);
            } else if (event.isMetaEvent()) {
                onMetaEvent(event, ttid);
            } else if (event.isStart()) {
                updateThreadLocToUserLoc(event, ttid);
            } else if (event.isSignalEvent()) {
                onSignalEvent(event);
            }
        }
    }

    /**
     * Updates the location at which a lock was acquired to the most recent reportable location on the call stack.
     * @param event a lock acquiring event.  Assumed to be the latest in the current trace window.
     */
    protected ReadonlyEventInterface updateLockLocToUserLoc(ReadonlyEventInterface event, int ttid) {
        long locId = findUserCallLocation(event, ttid);
        if (locId != event.getLocationId()) {
            event = event.destructiveWithLocationId(locId);
        }
        return event;
    }

    /**
     * Updates the location about thread creation to the most recent reportable location on the call stack.
     * @param event an event creating a new thread.  Assumed to be the latest in the current trace window.
     */
    protected void updateThreadLocToUserLoc(ReadonlyEventInterface event, int ttid) {
        long locId = findUserCallLocation(event, ttid);
        if (locId != metadata.getOriginalThreadCreationLocId(event.getSyncedThreadId())) {
            metadata().addOriginalThreadCreationInfo(event.getSyncedThreadId(), ttid, locId);
        }
    }

    /**
     * Retrieves the most recent non-library call location from the stack trace associated to an event.
     */
    private long findUserCallLocation(ReadonlyEventInterface e, int ttid) {
        long locId = e.getLocationId();
        if (locId >= 0 && !config().isExcludedLibrary(metadata().getLocationSig(locId))) {
            return locId;
        }
        Deque<ReadonlyEventInterface> stacktrace = tidToStacktrace.get(ttid);
        if (stacktrace == null) {
            return -1;
        }
        String sig;
        for (ReadonlyEventInterface event : stacktrace) {
            locId = event.getLocationId();
            if (locId != -1) {
                sig = metadata().getLocationSig(locId);
                if (!config().isExcludedLibrary(sig)) {
                    return locId;
                }
            }
        }
        return -1;
    }

    public OptionalInt getUnfinishedThreadId(int signalDepth, long otid) {
        Integer id = stateAtCurrentWindowEnd.unfinishedThreads.get(new SignalThreadId(signalDepth, otid));
        if (id == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(id);
    }

    int enterSignal(int signalDepth, long otid, long signalNumber) {
        int id = getNewThreadId();
        stateAtCurrentWindowEnd.unfinishedThreads.put(new SignalThreadId(signalDepth, otid), id);
        stateAtCurrentWindowEnd.ttidToSignalNumber.put(id, signalNumber);
        return id;
    }

    void exitSignal(int signalDepth, long otid) {
        SignalThreadId key = new SignalThreadId(signalDepth, otid);
        int id = stateAtCurrentWindowEnd.unfinishedThreads.get(key);
        stateAtCurrentWindowEnd.unfinishedThreads.remove(key);
        stateAtCurrentWindowEnd.ttidToSignalNumber.remove(id);
    }

    int getNewThreadId() {
        return t_threadId++;
    }

    public int getNewThreadId(long otid) {
        int id = getNewThreadId();
        stateAtCurrentWindowEnd.unfinishedThreads.put(new SignalThreadId(0, otid), id);
        return id;
    }

    private Optional<ReadonlyEventInterface> getPreviousEstablishEvent(long signalNumber, long signalHandler) {
        return Optional.ofNullable(
                t_signalNumberToHandlerToLastWindowEstablishEvents
                        .getOrDefault(signalNumber, Collections.emptyMap())
                        .get(signalHandler));
    }

    Map<Long, Map<Long, ReadonlyEventInterface>> getSignalNumberToHandlerToPreviousWindowEstablishEvent() {
        return t_signalNumberToHandlerToLastWindowEstablishEvents;
    }

    OptionalLong getSignalNumberForThreadAtWindowStart(int threadId) {
        Long signalNumber = stateAtCurrentWindowStart.ttidToSignalNumber.get(threadId);
        return signalNumber == null ? OptionalLong.empty() : OptionalLong.of(signalNumber);
    }

    Collection<Integer> getUnfinishedSignalTtidsAtWindowStart() {
        return stateAtCurrentWindowStart.ttidToSignalNumber.keySet();
    }

    Integer getTtidFromOtidAndSignalDepthAtStart(long otid, int signalDepth) {
        return stateAtCurrentWindowStart.unfinishedThreads.get(new SignalThreadId(signalDepth, otid));
    }

    void startWindow() {
        stateAtCurrentWindowStart.copyFrom(stateAtCurrentWindowEnd);
    }

    ThreadInfo getThreadInfo(int ttid) {
        return ttidToThreadInfo.get(ttid);
    }

    void setThreadInfo(int ttid, ThreadInfo threadInfo) {
        ttidToThreadInfo.put(ttid, threadInfo);
    }

    private static class StateAtWindowBorder {
        private final Map<Integer, Long> ttidToSignalNumber = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        private final Map<SignalThreadId, Integer> unfinishedThreads = new HashMap<>(DEFAULT_NUM_OF_THREADS);

        private void copyFrom(StateAtWindowBorder other) {
            ttidToSignalNumber.clear();
            ttidToSignalNumber.putAll(other.ttidToSignalNumber);
            unfinishedThreads.clear();
            unfinishedThreads.putAll(other.unfinishedThreads);
        }
    }

    private class SignalThreadId {
        private final int signalDepth;
        private final long otid;

        private SignalThreadId(int signalDepth, long otid) {
            this.signalDepth = signalDepth;
            this.otid = otid;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(signalDepth) ^ Long.hashCode(otid);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SignalThreadId)) {
                return false;
            }
            SignalThreadId sti = (SignalThreadId)obj;
            return signalDepth == sti.signalDepth && otid == sti.otid;
        }
    }
 }
