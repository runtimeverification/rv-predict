package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.trace.maps.ThreadIDToObjectMap;
import com.runtimeverification.rvpredict.trace.producers.TraceProducers;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
     * Map from (thread ID, lock ID) to lock state.
     */
    private final Table<Integer, Long, LockState> tidToLockIdToLockState = HashBasedTable.create(
            DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_LOCKS);

    private final TraceProducers traceProducers = new TraceProducers();

    private final StateAtWindowBorder stateAtCurrentWindowStart;
    private final StateAtWindowBorder stateAtCurrentWindowEnd;

    private final ThreadInfos threadInfos = new ThreadInfos();

    private final Map<Long, Map<Integer, Integer>> otidToSignalDepthToTtidAtWindowStartCache = new HashMap<>();

    private final Configuration config;

    private final MetadataInterface metadata;

    private final Map<Long, Integer> t_eventIdToTtid;

    private final Map<Integer, List<ReadonlyEventInterface>> t_tidToEvents;

    private final Map<Integer, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Integer, Collection<LockState>> t_tidToLockState;

    private final MemoryAddrToStateMap t_addrToState;

    private final Table<Integer, Long, List<ReadonlyEventInterface>> t_tidToAddrToEvents;

    private final Table<Integer, Long, List<ReadonlyEventInterface>> t_tidToAddrToPrefixReadEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<ReadonlyEventInterface> t_clinitEvents;

    private final Map<Integer, Set<Integer>> t_ttidsThatCanOverlap;

    private final Map<Long, Map<Integer, Boolean>> t_signalIsEnabledForThreadCache;

    private final Map<Long, Map<Long, Boolean>> t_atLeastOneSigsetAllowsSignalCache;

    private final Map<Long, Map<Long, List<ReadonlyEventInterface>>> t_signalNumberToSignalHandlerToEstablishSignalEvents;

    private int t_threadId;

    public TraceState(Configuration config, MetadataInterface metadata) {
        this.config = config;
        this.metadata = metadata;

        this.t_eventIdToTtid           = new LinkedHashMap<>();
        this.t_tidToEvents             = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToMemoryAccessBlocks = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToLockState          = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_addrToState             = new MemoryAddrToStateMap(config.windowSize);
        this.t_tidToAddrToEvents       = HashBasedTable.create(DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_ADDR);
        this.t_tidToAddrToPrefixReadEvents = HashBasedTable.create(DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_ADDR);
        this.t_lockIdToLockRegions     = new LinkedHashMap<>(config.windowSize >> 1);
        this.t_clinitEvents            = new HashSet<>(config.windowSize >> 1);
        this.t_ttidsThatCanOverlap     = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_signalIsEnabledForThreadCache = new HashMap<>();
        this.t_atLeastOneSigsetAllowsSignalCache = new HashMap<>();
        this.t_signalNumberToSignalHandlerToEstablishSignalEvents = new HashMap<>();
        this.t_threadId                = 1;
        this.stateAtCurrentWindowStart =
                new StateAtWindowBorder(config.desiredInterruptsPerSignalAndWindow(), metadata);
        this.stateAtCurrentWindowEnd =
                new StateAtWindowBorder(config.desiredInterruptsPerSignalAndWindow(), metadata);
    }

    public Configuration config() {
        return config;
    }

    public MetadataInterface metadata() {
        return metadata;
    }

    public Trace initNextTraceWindow(List<RawTrace> rawTraces) {
        processWindow(rawTraces);
        rawTraces = traceProducers.mergedRawTraces.getComputed().getTraces();
        if (rawTraces.size() == 1) {
            fastProcessWithoutStateAtCurrentWindowEnd(rawTraces.get(0));
        }

        t_eventIdToTtid.clear();
        t_tidToEvents.clear();
        t_tidToMemoryAccessBlocks.clear();
        t_tidToLockState.clear();
        t_addrToState.clear();
        t_tidToAddrToEvents.clear();
        t_tidToAddrToPrefixReadEvents.clear();
        t_lockIdToLockRegions.clear();
        t_clinitEvents.clear();
        t_ttidsThatCanOverlap.clear();
        t_signalIsEnabledForThreadCache.clear();
        t_atLeastOneSigsetAllowsSignalCache.clear();
        t_signalNumberToSignalHandlerToEstablishSignalEvents.clear();
        return new Trace(this, rawTraces,
                t_eventIdToTtid,
                t_tidToEvents,
                t_tidToMemoryAccessBlocks,
                t_tidToLockState,
                t_addrToState,
                t_tidToAddrToEvents,
                t_tidToAddrToPrefixReadEvents,
                t_lockIdToLockRegions,
                t_clinitEvents,
                t_ttidsThatCanOverlap,
                t_signalIsEnabledForThreadCache,
                t_atLeastOneSigsetAllowsSignalCache,
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
            break;
        case FINISH_METHOD:
            break;
        default:
            throw new IllegalArgumentException("Unexpected event type: " + event.getType());
        }
    }

    public boolean isInsideClassInitializer(int ttid) {
        try {
            return tidToClinitDepth.computeIfAbsent(ttid).intValue() > 0;
        } finally {
            tidToClinitDepth = ThreadIDToObjectMap.growOnFull(tidToClinitDepth);
        }
    }

    Collection<LockState> getLockStateSnapshot(int ttid) {
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        tidToLockIdToLockState.row(ttid).values().forEach(st -> lockStates.add(st.copy()));
        return lockStates;
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
        fastProcessWithoutStateAtCurrentWindowEnd(rawTrace);
        maintainStateAtCurrentWindowEnd(rawTrace);
    }

    private void fastProcessWithoutStateAtCurrentWindowEnd(RawTrace rawTrace) {
        int ttid = rawTrace.getThreadInfo().getId();
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
            }
        }
    }

    private void maintainStateAtCurrentWindowEnd(RawTrace rawTrace) {
        ThreadInfo threadInfo = rawTrace.getThreadInfo();
        if (threadInfo.getSignalDepth() > 0) {
            stateAtCurrentWindowEnd.onSignalThread(rawTrace);
        }
        int ttid = threadInfo.getId();
        if (rawTrace.size() > 0) {
            stateAtCurrentWindowEnd.threadEvent(ttid);
        }
        for (int i = 0; i < rawTrace.size(); i++) {
            ReadonlyEventInterface event = rawTrace.event(i);
            stateAtCurrentWindowEnd.processEvent(event, ttid);
            if (event.isStart()) {
                onStartThread(event.getSyncedThreadId());
            } else if (event.isJoin()) {
                onJoinThread(event.getSyncedThreadId());
            }
        }
    }

    private void processWindow(List<RawTrace> traces) {
        traces.forEach(this::maintainStateAtCurrentWindowEnd);
        traceProducers.startWindow(
                stateAtCurrentWindowStart.getFormerSignalTraces(),
                traces, stateAtCurrentWindowEnd.getThreadsForCurrentWindow(), threadInfos,
                stateAtCurrentWindowStart.getSignalMasks(),
                stateAtCurrentWindowStart.getStartedThreads(),
                stateAtCurrentWindowEnd.getStartedThreads(),
                stateAtCurrentWindowStart.getFinishedThreads(),
                stateAtCurrentWindowEnd.getFinishedThreads(),
                stateAtCurrentWindowStart.getStackTraces(),
                config.desiredInterruptsPerSignalAndWindow());
        stateAtCurrentWindowEnd.processSignalMasks(traceProducers.signalMaskForEvents.getComputed());
    }

    /**
     * Updates the location at which a lock was acquired to the most recent reportable location on the call stack.
     * @param event a lock acquiring event.  Assumed to be the latest in the current trace window.
     */
    ReadonlyEventInterface updateLockLocToUserLoc(ReadonlyEventInterface event, int ttid) {
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
    void updateThreadLocToUserLoc(ReadonlyEventInterface event, int ttid) {
        long locId = findUserCallLocation(event, ttid);
        OptionalLong maybeThreadCreation = metadata.getOriginalThreadCreationLocId(event.getSyncedThreadId());
        if (!maybeThreadCreation.isPresent() || locId != maybeThreadCreation.getAsLong()) {
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
        Collection<ReadonlyEventInterface> stacktrace =
                traceProducers.stackTraces.getComputed().getStackTraceAfterEventBuilder(ttid, e.getEventId()).build();
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

    public ThreadInfo createAndRegisterThreadInfo(long originalThreadId, OptionalInt parentTtid) {
        ThreadInfo info = ThreadInfo.createThreadInfo(t_threadId++, originalThreadId, parentTtid);
        threadInfos.registerThreadInfo(info);
        stateAtCurrentWindowEnd.registerThread(info.getId());
        return info;
    }

    public ThreadInfo createAndRegisterSignalInfo(
            long originalThreadId, long signalNumber, long signalHandler, int signalDepth) {
        ThreadInfo info = ThreadInfo.createSignalInfo(
                t_threadId++,
                originalThreadId,
                signalNumber,
                signalHandler,
                signalDepth);
        threadInfos.registerThreadInfo(info);
        stateAtCurrentWindowEnd.registerSignal(info.getId());
        return info;
    }

    private void onStartThread(long otid) {
        OptionalInt maybeTtid = threadInfos.getTtidFromOtid(otid);
        // Should have been registered by the TraceCache.
        assert maybeTtid.isPresent();
        stateAtCurrentWindowEnd.registerThread(maybeTtid.getAsInt());
    }

    private void onJoinThread(long otid) {
        OptionalInt maybeTtid = threadInfos.getTtidFromOtid(otid);
        // Joins should be in the same window or later than starts,
        // and we should have already processed the start event even if it was on a different thread.
        assert maybeTtid.isPresent();
        stateAtCurrentWindowEnd.joinThread(maybeTtid.getAsInt());
    }

    public void preStartWindow() {
        stateAtCurrentWindowStart.copyFrom(stateAtCurrentWindowEnd);
        stateAtCurrentWindowEnd.initializeForNextWindow();

        otidToSignalDepthToTtidAtWindowStartCache.clear();
        for (int ttid : stateAtCurrentWindowStart.getUnfinishedTtids()) {
            ThreadInfo info = threadInfos.getThreadInfo(ttid);
            otidToSignalDepthToTtidAtWindowStartCache
                    .computeIfAbsent(info.getOriginalThreadId(), k -> new HashMap<>())
                    .put(info.getSignalDepth(), ttid);
        }
    }

    public ThreadInfos getThreadInfos() {
        return threadInfos;
    }

    OptionalInt getTtidForThreadOngoingAtWindowStart(long otid, int signalDepth) {
        Integer ttid = otidToSignalDepthToTtidAtWindowStartCache
                .getOrDefault(otid, Collections.emptyMap())
                .get(signalDepth);
        if (ttid == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ttid);
    }

    boolean getThreadStartsInTheCurrentWindow(Integer ttid) {
        return !stateAtCurrentWindowStart.threadWasStarted(ttid) && stateAtCurrentWindowEnd.threadWasStarted(ttid);
    }

    boolean getThreadEndsInTheCurrentWindow(Integer ttid) {
        return !stateAtCurrentWindowStart.threadEnded(ttid) && stateAtCurrentWindowEnd.threadEnded(ttid);
    }

    Optional<ReadonlyEventInterface> getPreviousWindowEstablishEvents(long signalNumber, long signalHandler) {
        Optional<ReadonlyEventInterface> maybeEvent = stateAtCurrentWindowStart.getLastEstablishEvent(signalNumber);
        if (maybeEvent.isPresent() && maybeEvent.get().getSignalHandlerAddress() == signalHandler) {
            return maybeEvent;
        }
        return Optional.empty();
    }

    Collection<Integer> getThreadsForCurrentWindow() {
        return stateAtCurrentWindowEnd.getThreadsForCurrentWindow();
    }

    TraceProducers getTraceProducers() {
        return traceProducers;
    }
}
