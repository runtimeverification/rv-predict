package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.trace.maps.ThreadIDToObjectMap;

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
    private ThreadIDToObjectMap<Deque<Event>> tidToStacktrace = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, ArrayDeque::new);

    /**
     * Map from (thread ID, lock ID) to lock state.
     */
    public final Table<Long, Long, LockState> tidToLockIdToLockState = HashBasedTable.create(
            DEFAULT_NUM_OF_THREADS, DEFAULT_NUM_OF_LOCKS);

    private LockGraph lockGraph;

    private final Configuration config;

    private final Metadata metadata;

    private final Map<Long, List<Event>> t_tidToEvents;

    private final Map<Long, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Long, ThreadState> t_tidToThreadState;

    private final MemoryAddrToStateMap t_addrToState;

    private final Table<Long, Long, List<Event>> t_tidToAddrToEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<Event> t_clinitEvents;

    private Map<Long, ForkState> forks;

    public LockGraph getLockGraph() {
        return lockGraph;
    }

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
        this.forks                     = new HashedMap<>();
        this.lockGraph                 = new LockGraph(config, metadata);
    }

    public TraceState(Configuration config, Metadata metadata, LockGraph lockGraph) {
        this(config, metadata);
        this.lockGraph                 = lockGraph;
    }

    public ThreadIDToObjectMap<Deque<Event>> getTidToStacktrace() {
        return tidToStacktrace;
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

    public int acquireLock(Event lock) {
        lock = lock.copy();
        LockState st = tidToLockIdToLockState.row(lock.getTID())
                .computeIfAbsent(lock.getLockId(), LockState::new);
        st.acquire(lock);
        return lock.isReadLock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public int releaseLock(Event unlock) {
        LockState st = tidToLockIdToLockState.get(unlock.getTID(), unlock.getLockId());
        if (st == null) return -1;

        st.release(unlock);
        return unlock.isReadUnlock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public void onMetaEvent(Event event) {
        long tid = event.getTID();
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
            int locId = tidToStacktrace.get(tid).removeLast().getLocId();
            if (locId != event.getLocId()) {
                throw new IllegalStateException("Unmatched method entry/exit events!" +
                        (Configuration.debug ?
                                "\n\tENTRY:" + metadata.getLocationSig(locId) +
                                        "\n\tEXIT:" + metadata.getLocationSig(event.getLocId()) : ""));
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
        Deque<Event> stacktrace = tidToStacktrace.get(tid);
        stacktrace = stacktrace == null ? new ArrayDeque<>() : new ArrayDeque<>(stacktrace);
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        tidToLockIdToLockState.row(tid).values().forEach(st -> lockStates.add(st.copy()));
        return new ThreadState(stacktrace, lockStates);
    }

    public TraceState makeCopy() {
        TraceState ret = new TraceState(this.config, this.metadata, lockGraph.makeCopy());
        LongToObjectMap.EntryIterator it = getTidToStacktrace().iterator();
        while (it.hasNext()) {
            ret.getTidToStacktrace().put(it.getNextKey(), new ArrayDeque<>((Deque) it.getNextValue()));
            it.incCursor();
        }

        for (Table.Cell<Long, Long, LockState> cell : tidToLockIdToLockState.cellSet()) {
            ret.tidToLockIdToLockState.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue().copy());
        }

        return ret;
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
            Event event = rawTrace.event(i);
            if (event.isLock() && !event.isWaitAcq()) {
                updateLockLocToUserLoc(event);
                acquireLock(event);
            } else if (event.isUnlock() && !event.isWaitRel()) {
                releaseLock(event);
            } else if (event.isMetaEvent()) {
                onMetaEvent(event);
            } else if (event.isStart()) {
                updateThreadLocToUserLoc(event);
            } else if (event.isFork()) {
                forks.put(event.getPID(), new ForkState(this, event.getGID()));
            }
            if (event.isPreLock() || event.isLock() || event.isUnlock()) {
                if (config().isLLVMPrediction()) {
                    //TODO(TraianSF): remove above condition once instrumentation works for Java
                    getLockGraph().handle(event);
                }
            }
        }
    }

    /**
     * Updates the location at which a lock was acquired to the most recent reportable location on the call stack.
     * @param event a lock acquiring event.  Assumed to be the latest in the current trace window.
     */
    protected void updateLockLocToUserLoc(Event event) {
        int locId = findUserCallLocation(event);
        if (locId != event.getLocId()) {
            event.setLocId(locId);
        }
    }

    /**
     * Updates the location about thread creation to the most recent reportable location on the call stack.
     * @param event an event creating a new thread.  Assumed to be the latest in the current trace window.
     */
    protected void updateThreadLocToUserLoc(Event event) {
        int locId = findUserCallLocation(event);
        if (locId != metadata.getThreadCreationLocId(event.getSyncedThreadId())) {
            metadata().addThreadCreationInfo(event.getSyncedThreadId(), event.getTID(), locId);
        }
    }

    /**
     * Retrieves the most recent non-library call location from the stack trace associated to an event.
     */
    private int findUserCallLocation(Event e) {
        int locId = e.getLocId();
        if (locId >= 0 && !config().isExcludedLibrary(metadata().getLocationSig(locId))) {
            return locId;
        }
        long tid = e.getTID();
        Deque<Event> stacktrace = tidToStacktrace.get(tid);
        String sig;
        for (Event event : stacktrace) {
            locId = event.getLocId();
            if (locId != -1) {
                sig = metadata().getLocationSig(locId);
                if (!config().isExcludedLibrary(sig)) {
                    return locId;
                }
            }
        }
        return -1;
    }

    public Map<Long, ForkState> getForks() {
        return forks;
    }
}