package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.Deque;
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
    private final Table<Long, Long, Deque<Event>> lockTable = HashBasedTable.create();

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
        Deque<Event> locks = lockTable.get(tid, lockId);
        if (locks == null) {
            locks = new ArrayDeque<>();
            lockTable.put(tid, lockId, locks);
        }
        locks.add(lock);
    }

    public void releaseLock(Event unlock) {
        assert unlock.getType().isUnlockType();
        long tid = unlock.getTID();
        long lockId = unlock.getSyncObject();
        Event lock = lockTable.get(tid, lockId).removeLast();
        assert lock.getType().isLockType();
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

    public int getLockCount(long threadId, long lockId) {
        Deque<Event> deque = lockTable.get(threadId, lockId);
        return deque == null ? 0 : deque.size();
    }

    public Map<Long, List<Event>> getLockStatusSnapshot(long threadId) {
        ImmutableMap.Builder<Long, List<Event>> builder = ImmutableMap.builder();
        for (Map.Entry<Long, Deque<Event>> e : lockTable.row(threadId).entrySet()) {
            builder.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        return builder.build();
    }

    public List<Integer> getStacktraceSnapshot(long threadId) {
        List<Integer> stacktrace = threadIdToStacktrace.get(threadId);
        return stacktrace == null ? ImmutableList.<Integer>of() : ImmutableList.copyOf(stacktrace);
    }

}