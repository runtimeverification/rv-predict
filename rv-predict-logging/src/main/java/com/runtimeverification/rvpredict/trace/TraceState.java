package com.runtimeverification.rvpredict.trace;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;


// TODO(YilongL): think about the thread-safety about this class
public class TraceState {

    /**
     * Map from memory address to its value.
     */
    private final Map<MemoryAddr, Long> addrToValue = Maps.newHashMap();

    /**
     * Map form thread ID to the current level of class initialization.
     */
    private final Map<Long, MutableInt> threadIdToClinitDepth = Maps.newHashMap();

    /**
     * Map from thread ID to the current stack trace elements.
     */
    private final Map<Long, List<Integer>> threadIdToStacktrace = Maps.newHashMap();

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

    public void invokeMethod(Event event) {
        assert event.getType() == EventType.INVOKE_METHOD;
        Trace.getOrInitEmptyList(threadIdToStacktrace, event.getTID()).add(event.getLocId());
    }

    public void finishMethod(Event event) {
        assert event.getType() == EventType.FINISH_METHOD;
        List<Integer> stacktrace = threadIdToStacktrace.get(event.getTID());
        int locId = stacktrace.remove(stacktrace.size() - 1);
        assert locId == event.getLocId();
    }

    public void acquireLock(Event lock) {
        assert lock.getType().isLockType();
        long tid = lock.getTID();
        long lockId = lock.getSyncObject();
        LockState lockState = lockTable.get(tid, lockId);
        if (lockState == null) {
            lockState = new LockState(lock);
            lockTable.put(tid, lockId, lockState);
        }
        lockState.incLevel();
    }

    public void releaseLock(Event unlock) {
        assert unlock.getType().isUnlockType();
        lockTable.get(unlock.getTID(), unlock.getSyncObject()).decLevel();
    }

    public int getLockEntranceLevel(long tid, long lockId) {
        LockState lockState = lockTable.get(tid, lockId);
        return lockState == null ? 0 : lockState.level();
    }

    public boolean isInsideClassInitializer(long tid) {
        MutableInt level = threadIdToClinitDepth.get(tid);
        return level != null && level.intValue() > 0;
    }

    public void incClinitDepth(long tid) {
        MutableInt depth = threadIdToClinitDepth.get(tid);
        if (depth == null) {
            depth = new MutableInt();
        }
        depth.increment();
        threadIdToClinitDepth.put(tid, depth);
    }

    public void decClinitDepth(long tid) {
        MutableInt depth = threadIdToClinitDepth.get(tid);
        assert depth != null && depth.intValue() > 0;
        depth.decrement();
        threadIdToClinitDepth.put(tid, depth);
    }

    public void writeValueAt(MemoryAddr addr, long value) {
        addrToValue.put(addr, value);
    }

    public long getValueAt(MemoryAddr addr) {
        return addrToValue.getOrDefault(addr, 0L);
    }

    public Map<Long, LockState> getLockStatusSnapshot(long threadId) {
        ImmutableMap.Builder<Long, LockState> builder = ImmutableMap.builder();
        for (Map.Entry<Long, LockState> e : lockTable.row(threadId).entrySet()) {
            builder.put(e.getKey(), e.getValue().copy());
        }
        return builder.build();
    }

    public List<Integer> getStacktraceSnapshot(long threadId) {
        List<Integer> stacktrace = threadIdToStacktrace.get(threadId);
        return stacktrace == null ? ImmutableList.<Integer>of() : ImmutableList.copyOf(stacktrace);
    }

}