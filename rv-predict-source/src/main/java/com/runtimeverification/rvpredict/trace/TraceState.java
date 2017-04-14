package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEvent;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.trace.maps.ThreadIDToObjectMap;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private ThreadIDToObjectMap<Deque<ReadonlyEvent>> tidToStacktrace = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, ArrayDeque::new);

    /**
     * Map from (thread ID, lock ID) to lock state.
     */
    private final Table<Long, Long, LockState> tidToLockIdToLockState = HashBasedTable.create(
            DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_LOCKS);

    private final Configuration config;

    private final Metadata metadata;

    private final Map<Long, List<ReadonlyEvent>> t_tidToEvents;

    private final Map<Long, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Long, ThreadState> t_tidToThreadState;

    private final MemoryAddrToStateMap t_addrToState;

    private final Table<Long, Long, List<ReadonlyEvent>> t_tidToAddrToEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<ReadonlyEvent> t_clinitEvents;

    public TraceState(Configuration config, Metadata metadata) {
        this.config = config;
        this.metadata = metadata;
        this.t_tidToEvents             = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToMemoryAccessBlocks = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToThreadState        = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_addrToState             = new MemoryAddrToStateMap(config.windowSize);
        this.t_tidToAddrToEvents       = HashBasedTable.create(DEFAULT_NUM_OF_THREADS,
                                            DEFAULT_NUM_OF_ADDR);
        this.t_lockIdToLockRegions     = new LinkedHashMap<>(config.windowSize >> 1);
        this.t_clinitEvents            = new HashSet<>(config.windowSize >> 1);
    }

    public Configuration config() {
        return config;
    }

    public Metadata metadata() {
        return metadata;
    }

    public Trace initNextTraceWindow(List<RawTrace> rawTraces) {
        t_tidToEvents.clear();
        t_tidToMemoryAccessBlocks.clear();
        t_tidToThreadState.clear();
        t_addrToState.clear();
        t_tidToAddrToEvents.clear();
        t_lockIdToLockRegions.clear();
        t_clinitEvents.clear();
        return new Trace(this, rawTraces,
                t_tidToEvents,
                t_tidToMemoryAccessBlocks,
                t_tidToThreadState,
                t_addrToState,
                t_tidToAddrToEvents,
                t_lockIdToLockRegions,
                t_clinitEvents);
    }

    public int acquireLock(ReadonlyEvent lock) {
        lock = lock.copy();
        LockState st = tidToLockIdToLockState.row(lock.getThreadId())
                .computeIfAbsent(lock.getLockId(), LockState::new);
        st.acquire(lock);
        return lock.isReadLock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public int releaseLock(ReadonlyEvent unlock) {
        LockState st = tidToLockIdToLockState.get(unlock.getThreadId(), unlock.getLockId());
        if (st == null) return -1;

        st.release(unlock);
        return unlock.isReadUnlock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public void onMetaEvent(ReadonlyEvent event) {
        long tid = event.getThreadId();
        switch (event.getType()) {
        case CLINIT_ENTER:
            tidToClinitDepth.computeIfAbsent(tid).increment();
            tidToClinitDepth = ThreadIDToObjectMap.growOnFull(tidToClinitDepth);
            break;
        case CLINIT_EXIT:
            tidToClinitDepth.get(tid).decrement();
            break;
        case INVOKE_METHOD:
            tidToStacktrace.computeIfAbsent(tid).add(event.copy());
            tidToStacktrace = ThreadIDToObjectMap.growOnFull(tidToStacktrace);
            break;
        case FINISH_METHOD:
	    ReadonlyEvent lastEvent = tidToStacktrace.get(tid).removeLast();
            int locId = lastEvent.getLocationId();
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

    public boolean isInsideClassInitializer(long tid) {
        try {
            return tidToClinitDepth.computeIfAbsent(tid).intValue() > 0;
        } finally {
            tidToClinitDepth = ThreadIDToObjectMap.growOnFull(tidToClinitDepth);
        }
    }

    public ThreadState getThreadStateSnapshot(long tid) {
        /* copy stack trace */
        Deque<ReadonlyEvent> stacktrace = tidToStacktrace.get(tid);
        stacktrace = stacktrace == null ? new ArrayDeque<>() : new ArrayDeque<>(stacktrace);
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        tidToLockIdToLockState.row(tid).values().forEach(st -> lockStates.add(st.copy()));
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
        for (int i = 0; i < rawTrace.size(); i++) {
            ReadonlyEvent event = rawTrace.event(i);
            if (event.isLock() && !event.isWaitAcq()) {
                event = updateLockLocToUserLoc(event);
                acquireLock(event);
            } else if (event.isUnlock() && !event.isWaitRel()) {
                releaseLock(event);
            } else if (event.isMetaEvent()) {
                onMetaEvent(event);
            } else if (event.isStart()) {
                updateThreadLocToUserLoc(event);
            }
        }
    }

    /**
     * Updates the location at which a lock was acquired to the most recent reportable location on the call stack.
     * @param event a lock acquiring event.  Assumed to be the latest in the current trace window.
     */
    protected ReadonlyEvent updateLockLocToUserLoc(ReadonlyEvent event) {
        int locId = findUserCallLocation(event);
        if (locId != event.getLocationId()) {
            event = event.destructiveWithLocationId(locId);
        }
        return event;
    }

    /**
     * Updates the location about thread creation to the most recent reportable location on the call stack.
     * @param event an event creating a new thread.  Assumed to be the latest in the current trace window.
     */
    protected void updateThreadLocToUserLoc(ReadonlyEvent event) {
        int locId = findUserCallLocation(event);
        if (locId != metadata.getThreadCreationLocId(event.getSyncedThreadId())) {
            metadata().addThreadCreationInfo(event.getSyncedThreadId(), event.getThreadId(), locId);
        }
    }

    /**
     * Retrieves the most recent non-library call location from the stack trace associated to an event.
     */
    private int findUserCallLocation(ReadonlyEvent e) {
        int locId = e.getLocationId();
        if (locId >= 0 && !config().isExcludedLibrary(metadata().getLocationSig(locId))) {
            return locId;
        }
        long tid = e.getThreadId();
        Deque<ReadonlyEvent> stacktrace = tidToStacktrace.get(tid);
        String sig;
        for (ReadonlyEvent event : stacktrace) {
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

}
