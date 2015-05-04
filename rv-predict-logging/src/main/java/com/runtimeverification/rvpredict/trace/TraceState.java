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

    /**
     * Map from currently held lock to the stack trace at the time it is first acquired.
     */
    private final Map<Event, List<String>> lockHeldToStacktrace = Maps.newHashMap();

    private final Map<Long, Event> threadIdToStartEvent = Maps.newHashMap();

    private final Metadata metadata;

    public TraceState(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata metadata() {
        return metadata;
    }

    public Trace initNextTraceWindow(Event[] events, int numOfEvents) {
        Trace crntTraceWindow = new Trace(this, events, numOfEvents);
        finishLoading(crntTraceWindow);
        return crntTraceWindow;
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

    public Map<Event, List<String>> getHeldLockStacktraceSnapshot() {
        ImmutableMap.Builder<Event, List<String>> builder = ImmutableMap.builder();
        for (Map.Entry<Event, List<String>> entry : lockHeldToStacktrace.entrySet()) {
            builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        return builder.build();
    }

    private Set<Event> getHeldLocks() {
        Set<Event> locksHeld = Sets.newHashSet();
        for (Table.Cell<Long, Long, Deque<Event>> cell : lockTable.cellSet()) {
            Deque<Event> deque = cell.getValue();
            if (!deque.isEmpty()) {
                locksHeld.add(deque.peek());
            }
        }
        return locksHeld;
    }

    public void finishLoading(Trace crntTraceWindow) {
        Set<Event> locksHeld = getHeldLocks();
        lockHeldToStacktrace.keySet().retainAll(locksHeld);
        for (Event lock : locksHeld) {
            lockHeldToStacktrace.putIfAbsent(lock, crntTraceWindow.getStacktraceAt(lock));
        }
    }

    public void onThreadStart(Event startEvent) {
        assert startEvent.getType() == EventType.START;
        threadIdToStartEvent.put(startEvent.getSyncObject(), startEvent);
    }

    public Event getThreadStartEvent(long threadId) {
        return threadIdToStartEvent.get(threadId);
    }

}