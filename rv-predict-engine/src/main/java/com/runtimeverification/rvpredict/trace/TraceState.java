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
import com.runtimeverification.rvpredict.log.LoggingFactory;
import com.runtimeverification.rvpredict.util.Constants;

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
    private final Table<Long, Long, Deque<SyncEvent>> lockTable = HashBasedTable.create();

    private final LoggingFactory loggingFactory;

    TraceState(LoggingFactory loggingFactory) {
        this.loggingFactory = loggingFactory;
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
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

    public void acquireLock(Event event) {
        assert EventType.isLock(event.getType());

        SyncEvent lock = (SyncEvent) event;
        long tid = lock.getTID();
        long lockObj = lock.getSyncObject();
        Deque<SyncEvent> locks = lockTable.get(tid, lockObj);
        if (locks == null) {
            locks = new ArrayDeque<>();
            lockTable.put(tid, lockObj, locks);
        }
        locks.add(lock);
    }

    public void releaseLock(Event event) {
        assert EventType.isUnlock(event.getType());
        SyncEvent unlock = (SyncEvent) event;
        long tid = unlock.getTID();
        long lockObj = unlock.getSyncObject();
        Event lock = lockTable.get(tid, lockObj).removeLast();
        assert EventType.isLock(lock.getType());
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

    public void updateAddrValue(MemoryAccessEvent memAcc) {
        MemoryAddr addr = memAcc.getAddr();
        long value = memAcc.getValue();
        if (memAcc instanceof ReadEvent) {
            long oldVal = getAddrValue(addr);
            if (value != Constants._0X_DEADBEEFL && value != oldVal) {
                System.err.printf(
                    String.format("[Warning] logged trace not sequential consistent:%n"
                            + "  event %s reads a different value than the currently stored value %s%n"
                            + "    at %s%n",
                            memAcc, oldVal, loggingFactory.getStmtSig(memAcc.getLocId())));
            }
        }
        addrToValue.put(addr, value);
    }

    public long getAddrValue(MemoryAddr addr) {
        return addrToValue.getOrDefault(addr, 0L);
    }

    public int getLockCount(long threadId, long lockId) {
        Deque<SyncEvent> deque = lockTable.get(threadId, lockId);
        return deque == null ? 0 : deque.size();
    }

    public Map<Long, List<SyncEvent>> getLockStatusSnapshot(long threadId) {
        ImmutableMap.Builder<Long, List<SyncEvent>> builder = ImmutableMap.builder();
        for (Map.Entry<Long, Deque<SyncEvent>> e : lockTable.row(threadId).entrySet()) {
            builder.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        return builder.build();
    }

    public List<Integer> getStacktraceSnapshot(long threadId) {
        List<Integer> stacktrace = threadIdToStacktrace.get(threadId);
        return stacktrace == null ? ImmutableList.<Integer>of() : ImmutableList.copyOf(stacktrace);
    }

}