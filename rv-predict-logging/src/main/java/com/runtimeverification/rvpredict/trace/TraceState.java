package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;

// TODO(YilongL): think about the thread-safety about this class
public class TraceState {

    private static final int DEFAULT_NUM_OF_THREADS = 32;

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
    private final Map<Long, MutableInt> tidToClinitDepth = new HashMap<>(DEFAULT_NUM_OF_THREADS);

    /**
     * Map from thread ID to the current stack trace elements.
     */
    private final Map<Long, Deque<Integer>> tidToStacktrace = new HashMap<>(DEFAULT_NUM_OF_THREADS);

    /**
     * Table indexed by thread ID and lock object respectively. This table
     * records the current lock status of each thread.
     */
    private final Table<Long, Long, LockState> lockTable = HashBasedTable.create();

    private final Metadata metadata;

    private final Map<Long, List<Event>> t_tidToEvents;

    private final Map<Long, List<MemoryAccessBlock>> t_tidToMemoryAccessBlocks;

    private final Map<Long, ThreadState> t_tidToThreadState;

    private final MemoryAddrToStateMap t_addrToState;

    private final Map<Long, List<Event>> t_addrToWriteEvents;

    private final Map<Long, List<LockRegion>> t_lockIdToLockRegions;

    private final Set<Event> t_clinitEvents;

    public TraceState(Configuration config, Metadata metadata) {
        this.metadata = metadata;
        this.t_tidToEvents             = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToMemoryAccessBlocks = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_tidToThreadState        = new HashMap<>(DEFAULT_NUM_OF_THREADS);
        this.t_addrToState             = new MemoryAddrToStateMap(config.windowSize);
        this.t_addrToWriteEvents       = new HashMap<>(config.windowSize);
        this.t_lockIdToLockRegions     = new HashMap<>(config.windowSize >> 1);
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

    public LockState acquireLock(Event lock) {
        assert lock.isLock();
        LockState st = lockTable.row(lock.getTID()).computeIfAbsent(lock.getSyncObject(),
                p -> new LockState());
        st.acquire(lock);
        return st;
    }

    public LockState releaseLock(Event unlock) {
        assert unlock.isUnlock();
        LockState st = lockTable.get(unlock.getTID(), unlock.getSyncObject());
        st.release();
        return st;
    }

    public void onMetaEvent(Event event) {
        long tid = event.getTID();
        switch (event.getType()) {
        case CLINIT_ENTER:
            tidToClinitDepth.computeIfAbsent(tid, p -> new MutableInt()).increment();
            break;
        case CLINIT_EXIT:
            tidToClinitDepth.get(tid).decrement();
            break;
        case INVOKE_METHOD:
            tidToStacktrace.computeIfAbsent(tid, p -> new ArrayDeque<>())
                .add(event.getLocId());
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
        return tidToClinitDepth.computeIfAbsent(tid, p -> new MutableInt(0)).intValue() > 0;
    }

    public void writeValueAt(long addr, long value) {
        addrToValue.put(addr, value);
    }

    public long getValueAt(long addr) {
        // the default return value is 0
        return addrToValue.get(addr);
    }

    public ThreadState getThreadState(long tid) {
        return new ThreadState(tidToStacktrace.getOrDefault(tid, new ArrayDeque<>()),
                lockTable.row(tid).values());
    }

    public ThreadState getThreadStateSnapshot(long tid) {
        /* copy stack trace */
        Deque<Integer> stacktrace = tidToStacktrace.get(tid);
        stacktrace = stacktrace == null ? new ArrayDeque<>() : new ArrayDeque<>(stacktrace);
        /* copy each lock state */
        List<LockState> lockStates = new ArrayList<>();
        lockTable.row(tid).values().forEach(st -> lockStates.add(st.copy()));
        return new ThreadState(stacktrace, lockStates);
    }

}