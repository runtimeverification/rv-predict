package com.runtimeverification.rvpredict.trace;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;


// TODO(YilongL): think about the thread-safety about this class
public class TraceState {

    /**
     * Map from memory address to its value.
     */
    private final Long2LongMap addrToValue = new Long2LongOpenHashMap();

    /**
     * Map form thread ID to the current level of class initialization.
     */
    private final Map<Long, MutableInt> tidToClinitDepth = Maps.newHashMap();

    /**
     * Map from thread ID to the current stack trace elements.
     */
    private final Map<Long, Deque<Integer>> tidToStacktrace = Maps.newHashMap();

    /**
     * Table indexed by thread ID and lock object respectively. This table
     * records the current lock status of each thread.
     */
    private final Table<Long, Long, LockState> lockTable = HashBasedTable.create();

    private final Metadata metadata;

    public TraceState(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata metadata() {
        return metadata;
    }

    public Trace initNextTraceWindow(Event[] events, int numOfEvents) {
        return new Trace(this, events, numOfEvents);
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
//            if (locId != event.getLocId()) {
//                throw new IllegalStateException("Unmatched method entry/exit events!");
//            }
            break;
        default:
            throw new IllegalArgumentException("Unexpected event type: " + event.getType());
        }
    }

    public boolean isInsideClassInitializer(long tid) {
        return tidToClinitDepth.computeIfAbsent(tid, p -> new MutableInt(0)).intValue() > 0;
    }

    public boolean hasThreadInsideClinit() {
        return tidToClinitDepth.values().stream().anyMatch(d -> d.intValue() > 0);
    }

    public void writeValueAt(long addr, long value) {
        addrToValue.put(addr, value);
    }

    public long getValueAt(long addr) {
        // the default value of Long2LongMap is 0
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