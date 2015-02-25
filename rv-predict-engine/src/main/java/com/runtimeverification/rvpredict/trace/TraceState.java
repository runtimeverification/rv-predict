package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
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

    /**
     * Map from currently held lock to the stack trace at the time it is first acquired.
     */
    private final Map<SyncEvent, List<String>> lockHeldToStacktrace = Maps.newHashMap();

    private Trace crntTraceWindow;

    private final Configuration config;
    private final LoggingFactory loggingFactory;

    TraceState(Configuration config, LoggingFactory loggingFactory) {
        this.config = config;
        this.loggingFactory = loggingFactory;
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    public void setCurrentTraceWindow(Trace traceWindow) {
        assert crntTraceWindow == null;
        this.crntTraceWindow = traceWindow;
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
        long lockId = lock.getSyncObject();
        Deque<SyncEvent> locks = lockTable.get(tid, lockId);
        if (locks == null) {
            locks = new ArrayDeque<>();
            lockTable.put(tid, lockId, locks);
        }
        locks.add(lock);
    }

    public void releaseLock(Event event) {
        assert EventType.isUnlock(event.getType());
        SyncEvent unlock = (SyncEvent) event;
        long tid = unlock.getTID();
        long lockId = unlock.getSyncObject();
        Event lock = lockTable.get(tid, lockId).removeLast();
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

    public void updateValueAt(MemoryAccessEvent memAcc) {
        MemoryAddr addr = memAcc.getAddr();
        long value = memAcc.getValue();
        if (memAcc instanceof ReadEvent) {
            long oldVal = getValueAt(addr);
            if (config.debug) {
                if (value != Constants._0X_DEADBEEFL && value != oldVal) {
                    System.err.printf(
                        String.format("[Warning] logged trace not sequential consistent:%n"
                                + "  event %s reads a different value than the currently stored value %s%n"
                                + "    at %s%n",
                                memAcc, oldVal, loggingFactory.getStmtSig(memAcc.getLocId())));
                }
            }
        }
        addrToValue.put(addr, value);
    }

    public long getValueAt(MemoryAddr addr) {
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

    public Map<SyncEvent, List<String>> getHeldLockStacktraceSnapshot() {
        ImmutableMap.Builder<SyncEvent, List<String>> builder = ImmutableMap.builder();
        for (Map.Entry<SyncEvent, List<String>> entry : lockHeldToStacktrace.entrySet()) {
            builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        return builder.build();
    }

    private Set<SyncEvent> getHeldLocks() {
        Set<SyncEvent> locksHeld = Sets.newHashSet();
        for (Table.Cell<Long, Long, Deque<SyncEvent>> cell : lockTable.cellSet()) {
            Deque<SyncEvent> deque = cell.getValue();
            if (!deque.isEmpty()) {
                locksHeld.add(deque.peek());
            }
        }
        return locksHeld;
    }

    public void finishLoading() {
        Set<SyncEvent> locksHeld = getHeldLocks();
        lockHeldToStacktrace.keySet().retainAll(locksHeld);
        for (SyncEvent lock : locksHeld) {
            lockHeldToStacktrace.putIfAbsent(lock, crntTraceWindow.getStacktraceAt(lock));
        }

        /* detach the state from the trace window */
        crntTraceWindow = null;
    }

}