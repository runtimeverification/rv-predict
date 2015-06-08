package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToObjectMap;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToValueMap;
import com.runtimeverification.rvpredict.trace.maps.ThreadIDToObjectMap;

public class TraceState {

    private static final int DEFAULT_NUM_OF_THREADS = 32;

    private static final int DEFAULT_NUM_OF_LOCKS = 32;

    /**
     * Limit the maximum number of entries in the {@link #addrToValue} map in
     * order to avoid {@link OutOfMemoryError}.
     */
    private static final int NUM_OF_ADDR = 32 * 1024;

    /**
     * Map from memory address to its value.
     */
    private final MemoryAddrToValueMap addrToValue = new MemoryAddrToValueMap(NUM_OF_ADDR);

    /**
     * Map form thread ID to the current level of class initialization.
     */
    private final ThreadIDToObjectMap<MutableInt> tidToClinitDepth = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, MutableInt::new);

    /**
     * Map from thread ID to the current stack trace elements.
     */
    private final ThreadIDToObjectMap<Deque<Integer>> tidToStacktrace = new ThreadIDToObjectMap<>(
            DEFAULT_NUM_OF_THREADS, ArrayDeque::new);

    /**
     * Map from (thread ID, lock ID) to lock state.
     */
    private final Map<Long, Map<Long, LockState>> tidToLockIdToLockState = new LinkedHashMap<>(
            DEFAULT_NUM_OF_THREADS);

    private final Metadata metadata;

    private final Map<Long, List<Event>> t_tidToEvents;

    private final Map<Long, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Long, ThreadState> t_tidToThreadState;

    private final MemoryAddrToStateMap t_addrToState;

    private final MemoryAddrToObjectMap<List<Event>> t_addrToWriteEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<Event> t_clinitEvents;

    public TraceState(Configuration config, Metadata metadata) {
        this.metadata = metadata;
        this.t_tidToEvents             = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToMemoryAccessBlocks = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToThreadState        = new LinkedHashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_addrToState             = new MemoryAddrToStateMap(config.windowSize);
        this.t_addrToWriteEvents       = new MemoryAddrToObjectMap<>(config.windowSize, ArrayList::new);
        this.t_lockIdToLockRegions     = new LinkedHashMap<>(config.windowSize >> 1);
        this.t_clinitEvents            = new HashSet<>(config.windowSize >> 1);
    }

    public Metadata metadata() {
        return metadata;
    }

    public Trace initNextTraceWindow(List<RawTrace> rawTraces) {
        t_tidToEvents.clear();
        t_tidToMemoryAccessBlocks.clear();
        t_tidToThreadState.clear();
        t_addrToState.clear();
        t_addrToWriteEvents.clear();
        t_lockIdToLockRegions.clear();
        t_clinitEvents.clear();
        return new Trace(this, rawTraces,
                t_tidToEvents,
                t_tidToMemoryAccessBlocks,
                t_tidToThreadState,
                t_addrToState,
                t_addrToWriteEvents,
                t_lockIdToLockRegions,
                t_clinitEvents);
    }

    public int acquireLock(Event lock) {
        LockState st = tidToLockIdToLockState.computeIfAbsent(lock.getTID(),
                p -> new LinkedHashMap<>(DEFAULT_NUM_OF_LOCKS)).computeIfAbsent(
                lock.getLockId(), LockState::new);
        st.acquire(lock);
        return lock.isReadLock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public int releaseLock(Event unlock) {
        LockState st = tidToLockIdToLockState.get(unlock.getTID()).get(unlock.getLockId());
        st.release(unlock);
        return unlock.isReadUnlock() ? st.readLockLevel() : st.writeLockLevel();
    }

    public void onMetaEvent(Event event) {
        long tid = event.getTID();
        switch (event.getType()) {
        case CLINIT_ENTER:
            tidToClinitDepth.computeIfAbsent(tid).increment();
            break;
        case CLINIT_EXIT:
            tidToClinitDepth.get(tid).decrement();
            break;
        case INVOKE_METHOD:
            tidToStacktrace.computeIfAbsent(tid).add(event.getLocId());
            break;
        case FINISH_METHOD:
            int locId = tidToStacktrace.get(tid).removeLast();
            if (locId != event.getLocId()) {
                throw new IllegalStateException("Unmatched method entry/exit events!");
            }
            break;
        default:
            throw new IllegalArgumentException("Unexpected event type: " + event.getType());
        }
    }

    public boolean isInsideClassInitializer(long tid) {
        return tidToClinitDepth.computeIfAbsent(tid).intValue() > 0;
    }

    public void writeValueAt(long addr, long value) {
        addrToValue.put(addr, value);
    }

    public long getValueAt(long addr) {
        // the default return value is 0
        return addrToValue.get(addr);
    }

    public ThreadState getThreadState(long tid) {
        return new ThreadState(tidToStacktrace.computeIfAbsent(tid),
                tidToLockIdToLockState.getOrDefault(tid, Collections.emptyMap()).values());
    }

    public ThreadState getThreadStateSnapshot(long tid) {
        /* copy stack trace */
        Deque<Integer> stacktrace = tidToStacktrace.get(tid);
        stacktrace = stacktrace == null ? new ArrayDeque<>() : new ArrayDeque<>(stacktrace);
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        tidToLockIdToLockState.getOrDefault(tid, Collections.emptyMap()).values()
                .forEach(st -> lockStates.add(st.copy()));
        return new ThreadState(stacktrace, lockStates);
    }

}