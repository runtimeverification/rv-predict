/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.google.common.collect.*;
import com.runtimeverification.rvpredict.log.LoggingFactory;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    /**
     * Unprocessed raw events reading from the logging phase.
     */
    private ImmutableList<Event> rawEvents = null;
    private final ImmutableList.Builder<Event> rawEventsBuilder = ImmutableList.builder();

    /**
     * Read threads on each address. Only used in filtering thread-local
     * {@link MemoryAccessEvent}s.
     */
    private final Map<MemoryAddr, Set<Long>> addrToReadThreads = new HashMap<>();

    /**
     * Write threads on each address. Only used in filtering thread-local
     * {@link MemoryAccessEvent}s.
     */
    private final Map<MemoryAddr, Set<Long>> addrToWriteThreads = new HashMap<>();

    /**
     * Shared memory locations.
     */
    private final Set<MemoryAddr> sharedMemAddr = new HashSet<>();

    private final Set<Long> threadIds = new HashSet<>();

    // fulltrace represents all the critical events in the global order
    private final List<Event> allEvents = new ArrayList<>();

    /**
     * Events of each thread.
     */
    private final Map<Long, List<Event>> threadIdToEvents = new HashMap<>();

    /**
     * Branch events of each thread.
     */
    private final Map<Long, List<BranchEvent>> threadIdToBranchEvents = new HashMap<>();

    /**
     * Start/Join events indexed by the ID of its target thread to join/start.
     */
    private final Map<Long, List<SyncEvent>> threadIdToStartJoinEvents = new HashMap<>();

    /**
     * Wait/Notify/Lock/Unlock events indexed by the involved lock object.
     */
    private final Map<Long, List<SyncEvent>> lockIdToSyncEvents = new HashMap<>();

    /**
     * Read events on each address.
     */
    private final Map<MemoryAddr, List<ReadEvent>> addrToReadEvents = new HashMap<>();

    /**
     * Write events on each address.
     */
    private final Map<MemoryAddr, List<WriteEvent>> addrToWriteEvents = new HashMap<>();

    /**
     * Lists of {@code MemoryAccessEvent}'s indexed by address and thread ID.
     */
    private final Table<MemoryAddr, Long, List<MemoryAccessEvent>> memAccessEventsTbl = HashBasedTable.create();

    /**
     * The initial value at all addresses referenced in this trace segment. It
     * is computed as the value in the {@link #crntState} before the first
     * access occurring in this trace segment.
     */
    private final Map<MemoryAddr, Long> addrToInitValue = Maps.newHashMap();

    /**
     * The initial stack trace for all threads referenced in this trace segment.
     * It is computed as the value in the {@link #crntState} before the first
     * event of that thread occurring in this trace segment.
     */
    private final Map<Long, List<Integer>> threadIdToInitStacktrace = Maps.newHashMap();

    private final Map<Long, Map<Long, List<SyncEvent>>> threadIdToInitLockStatus = Maps.newHashMap();

    private final Map<SyncEvent, List<String>> initHeldLockToStacktrace;

    /**
     * Set of {@code MemoryAccessEvent}'s that happen during class initialization.
     */
    private final Set<MemoryAccessEvent> clinitMemAccEvents = Sets.newHashSet();

    /**
     * Set of outermost LOCK/UNLOCK events.
     */
    private final Set<SyncEvent> outermostLockingEvents = Sets.newHashSet();

    /**
     * Map from thread Id to {@link EventType#INVOKE_METHOD} and
     * {@link EventType#FINISH_METHOD} events.
     */
    private final Map<Long, List<MetaEvent>> threadIdToCallStackEvents = Maps.newHashMap();

    private final LoggingFactory loggingFactory;

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState crntState;

    public Trace(TraceState crntState) {
        this.crntState = crntState;
        this.loggingFactory = crntState.getLoggingFactory();
        this.initHeldLockToStacktrace = crntState.getHeldLockStacktraceSnapshot();
    }

    public boolean hasSharedMemAddr() {
        return !sharedMemAddr.isEmpty();
    }

    public List<Event> getAllEvents() {
        return allEvents;
    }

    /**
     * Gets the initial value of a memory address.
     *
     * @param addr
     *            the address
     * @return the initial value
     */
    public Long getInitValueOf(MemoryAddr addr) {
        Long initValue = addrToInitValue.get(addr);
        assert initValue != null : "All values in the trace should have been set in addRawEvent";
        return initValue;
    }

    public Event getFirstThreadEvent(long threadId) {
        List<Event> events = getThreadEvents(threadId);
        return events.isEmpty() ? null : events.get(0);
    }

    public Event getLastThreadEvent(long threadId) {
        List<Event> events = getThreadEvents(threadId);
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }

    public Set<Long> getThreadIds() {
        return threadIds;
    }

    public List<Event> getThreadEvents(long threadId) {
        List<Event> events = threadIdToEvents.get(threadId);
        return events == null ? Lists.<Event>newArrayList() : events;
    }

    public Event getNextThreadEvent(Event event) {
        List<Event> events = getThreadEvents(event.getTID());
        int nextThrdEventIdx = events.indexOf(event) + 1;
        assert nextThrdEventIdx > 0;
        return events.size() == nextThrdEventIdx ? null : events.get(nextThrdEventIdx);
    }

    public List<BranchEvent> getThreadBranchEvents(long threadId) {
        List<BranchEvent> events = threadIdToBranchEvents.get(threadId);
        return events == null ? Lists.<BranchEvent>newArrayList() : events;
    }

    public Map<Long, List<Event>> getThreadIdToEventsMap() {
        return threadIdToEvents;
    }

    public Map<Long, List<SyncEvent>> getThreadIdToStartJoinEvents() {
        return threadIdToStartJoinEvents;
    }

    public Map<Long, List<SyncEvent>> getLockObjToSyncEvents() {
        return lockIdToSyncEvents;
    }

    public List<ReadEvent> getReadEventsOn(MemoryAddr addr) {
        List<ReadEvent> events = addrToReadEvents.get(addr);
        return events == null ? Lists.<ReadEvent>newArrayList() : events;
    }

    public List<WriteEvent> getWriteEventsOn(MemoryAddr addr) {
        List<WriteEvent> events = addrToWriteEvents.get(addr);
        return events == null ? Lists.<WriteEvent>newArrayList() : events;
    }

    public Table<MemoryAddr, Long, List<MemoryAccessEvent>> getMemAccessEventsTable() {
        return memAccessEventsTbl;
    }

    public boolean isClinitMemoryAccess(MemoryAccessEvent event) {
        return clinitMemAccEvents.contains(event);
    }

    /**
     * Returns the {@code String} representation of the stack trace at the point
     * when a given {@code event} happened.
     *
     * @param event
     *            the event
     * @return a {@code List} of stack trace element represented in
     *         {@code String}s
     */
    public List<String> getStacktraceAt(Event event) {
        long tid = event.getTID();
        List<String> stacktrace = Lists.newArrayList();
        if (event.getGID() >= rawEvents.get(0).getGID()) {
            /* event is in the current window; reassemble its stack trace */
            for (int locId : threadIdToInitStacktrace.get(tid)) {
                stacktrace.add(loggingFactory.getStmtSig(locId));
            }
            for (MetaEvent e : threadIdToCallStackEvents.getOrDefault(tid,
                    Collections.<MetaEvent> emptyList())) {
                if (e.getGID() >= event.getGID()) {
                    break;
                }

                if (e.getType() == EventType.INVOKE_METHOD) {
                    stacktrace.add(loggingFactory.getStmtSig(e.getLocId()));
                } else {
                    stacktrace.remove(stacktrace.size() - 1);
                }
            }
        } else {
            /* event is from previous windows */
            if (initHeldLockToStacktrace.containsKey(event)) {
                stacktrace.addAll(initHeldLockToStacktrace.get(event));
            } else {
                stacktrace.add("... stack trace not available ...");
            }
        }
        stacktrace.add(loggingFactory.getStmtSig(event.getLocId()));
        return stacktrace;
    }

    public List<LockObject> getHeldLocksAt(MemoryAccessEvent memAcc) {
        long tid = memAcc.getTID();
        Map<Long, Deque<SyncEvent>> map = Maps.newHashMap();
        for (Map.Entry<Long, List<SyncEvent>> entry : threadIdToInitLockStatus.getOrDefault(tid,
                Collections.<Long, List<SyncEvent>> emptyMap()).entrySet()) {
            map.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
        }
        for (Event e : getThreadEvents(tid)) {
            if (e.getGID() >= memAcc.getGID()) {
                break;
            }

            EventType type = e.getType();
            if (EventType.isLock(type)) {
                long lockId = ((SyncEvent) e).getSyncObject();
                map.putIfAbsent(lockId, new ArrayDeque<SyncEvent>());
                map.get(lockId).add((SyncEvent) e);
            } else if (EventType.isUnlock(type)) {
                long lockId = ((SyncEvent) e).getSyncObject();
                SyncEvent lock = map.get(lockId).removeLast();
                assert lock.getTID() == tid && lock.getSyncObject() == lockId;
            }
        }

        List<LockObject> lockObjects = Lists.newArrayList();
        for (Deque<SyncEvent> deque : map.values()) {
            if (!deque.isEmpty()) {
                lockObjects.add(LockObject.create(deque.peek()));
            }
        }
        Collections.sort(lockObjects, new Comparator<LockObject>() {
            @Override
            public int compare(LockObject o1, LockObject o2) {
                return Long.compare(o1.getLockEvent().getGID(), o2.getLockEvent().getGID());
            }
        });
        return lockObjects;
    }

    /**
     * Gets control-flow dependent events of a given {@code MemoryAccessEvent}.
     * Without logging {@code BranchEvent}, all read events that happen-before
     * the given event have to be included conservatively. Otherwise, only the
     * read events that happen-before the latest branch event are included.
     */
    public List<ReadEvent> getCtrlFlowDependentEvents(MemoryAccessEvent memAccEvent) {
        // TODO(YilongL): optimize this method when it becomes a bottleneck
        List<ReadEvent> readEvents = new ArrayList<>();
        BranchEvent prevBranchEvent = getLastBranchEventBefore(memAccEvent);
        Event event = prevBranchEvent == null ? memAccEvent : prevBranchEvent;
        for (Event e : getThreadEvents(memAccEvent.getTID())) {
            if (e.getGID() >= event.getGID()) {
                break;
            }

            if (e instanceof ReadEvent) {
                readEvents.add((ReadEvent) e);
            }
        }

        return readEvents;
    }

    /**
     * Gets all read events that happen-before the given event but not in
     * {@link #getCtrlFlowDependentEvents(MemoryAccessEvent)}.
     */
    public List<ReadEvent> getExtraDataFlowDependentEvents(MemoryAccessEvent memAccEvent) {
        BranchEvent prevBranchEvent = getLastBranchEventBefore(memAccEvent);
        if (prevBranchEvent == null) {
            return ImmutableList.of();
        } else {
            List<ReadEvent> readEvents = new ArrayList<>();
            for (Event e : getThreadEvents(memAccEvent.getTID())) {
                if (e.getGID() >= memAccEvent.getGID()) {
                    break;
                }
                if (e.getGID() > prevBranchEvent.getGID() && e instanceof ReadEvent) {
                    readEvents.add((ReadEvent) e);
                }
            }
            return readEvents;
        }
    }

    /**
     * Given an {@code event}, returns the last branch event that appears before
     * it in the same thread.
     *
     * @param event
     *            the event
     * @return the last branch event before {@code event} if there is one;
     *         otherwise, {@code null}
     */
    private BranchEvent getLastBranchEventBefore(Event event) {
        BranchEvent lastBranchEvent = null;
        for (BranchEvent branchEvent : getThreadBranchEvents(event.getTID())) {
            if (branchEvent.getGID() < event.getGID()) {
                lastBranchEvent = branchEvent;
            } else {
                break;
            }
        }
        return lastBranchEvent;
    }

    public void addRawEvent(Event event) {
//        System.err.println(event + " at " + loggingFactory.getStmtSig(event.getLocId()));
        rawEventsBuilder.add(event);
        updateTraceState(event);

        if (event instanceof MemoryAccessEvent) {
            MemoryAccessEvent memAcc = (MemoryAccessEvent) event;
            MemoryAddr addr = memAcc.getAddr();
            getOrInitEmptySet(event instanceof ReadEvent ?
                    addrToReadThreads : addrToWriteThreads, addr).add(event.getTID());
            if (crntState.isInsideClassInitializer(event.getTID())) {
                clinitMemAccEvents.add(memAcc);
            }
        }
    }

    private void updateTraceState(Event event) {
        long tid = event.getTID();
        threadIdToInitStacktrace.putIfAbsent(tid, crntState.getStacktraceSnapshot(tid));
        if (event instanceof MemoryAccessEvent) {
            MemoryAccessEvent memAcc = (MemoryAccessEvent) event;
            MemoryAddr addr = memAcc.getAddr();
            addrToInitValue.putIfAbsent(addr, crntState.getValueAt(addr));
            crntState.updateValueAt(memAcc);
        } else if (EventType.isLock(event.getType()) || EventType.isUnlock(event.getType())) {
            threadIdToInitLockStatus.putIfAbsent(tid, crntState.getLockStatusSnapshot(tid));

            SyncEvent syncEvent = (SyncEvent) event;
            long lockId = syncEvent.getSyncObject();
            if (EventType.isLock(event.getType())) {
                crntState.acquireLock(event);
                if (crntState.getLockCount(tid, lockId) == 1) {
                    outermostLockingEvents.add(syncEvent);
                }
            } else {
                crntState.releaseLock(event);
                if (crntState.getLockCount(tid, lockId) == 0) {
                    outermostLockingEvents.add(syncEvent);
                }
            }
        } else if (event instanceof MetaEvent) {
            EventType eventType = event.getType();
            if (eventType == EventType.CLINIT_ENTER) {
                crntState.incClinitDepth(tid);
            } else if (eventType == EventType.CLINIT_EXIT) {
                crntState.decClinitDepth(tid);
            } else if (eventType == EventType.INVOKE_METHOD) {
                crntState.invokeMethod(event);
                getOrInitEmptyList(threadIdToCallStackEvents, tid).add((MetaEvent) event);
            } else if (eventType == EventType.FINISH_METHOD) {
                crntState.finishMethod(event);
                getOrInitEmptyList(threadIdToCallStackEvents, tid).add((MetaEvent) event);
            } else {
                assert false : "unreachable";
            }
        }
    }

    /**
     * add a new filtered event to the trace in the order of its appearance
     *
     * @param event
     */
    private void addEvent(Event event) {
//        System.err.println(event + " at " + loggingFactory.getStmtSig(event.getLocId()));
        long tid = event.getTID();
        threadIds.add(tid);

        if (event instanceof BranchEvent) {
            getOrInitEmptyList(threadIdToBranchEvents, tid).add((BranchEvent) event);
        } else if (event instanceof MetaEvent) {
            // do nothing
        } else {
            allEvents.add(event);

            getOrInitEmptyList(threadIdToEvents, tid).add(event);
            if (event instanceof MemoryAccessEvent) {
                MemoryAccessEvent memAcc = (MemoryAccessEvent) event;
                MemoryAddr addr = memAcc.getAddr();

                getOrInitEmptyList(memAccessEventsTbl.row(addr), tid).add(memAcc);

                if (event instanceof ReadEvent) {
                    getOrInitEmptyList(addrToReadEvents, addr).add((ReadEvent) event);
                } else {
                    getOrInitEmptyList(addrToWriteEvents, addr).add((WriteEvent) event);
                }
            } else if (event instanceof SyncEvent) {
                SyncEvent syncEvent = (SyncEvent) event;

                Map<Long, List<SyncEvent>> eventsMap = null;
                switch (syncEvent.getType()) {
                case START:
                case PRE_JOIN:
                case JOIN:
                case JOIN_MAYBE_FAILED:
                    eventsMap = threadIdToStartJoinEvents;
                    break;
                case WRITE_LOCK:
                case WRITE_UNLOCK:
                case READ_LOCK:
                case READ_UNLOCK:
                case WAIT_REL:
                case WAIT_ACQ:
                    eventsMap = lockIdToSyncEvents;
                    break;
                default:
                    assert false : "unexpected event: " + syncEvent;
                }

                getOrInitEmptyList(eventsMap, syncEvent.getSyncObject()).add(syncEvent);
            } else {
                assert false : "unreachable";
            }
        }
    }

    /**
     * Once trace is completely loaded, remove local data accesses and process
     * the remaining trace.
     */
    public void finishedLoading() {
        rawEvents = rawEventsBuilder.build();
        crntState.finishLoading();

        /* compute memory addresses that are accessed by more than one thread
         * and with at least one write access */
        for (MemoryAddr addr : Iterables.concat(addrToReadThreads.keySet(),
                addrToWriteThreads.keySet())) {
            Set<Long> wrtThrdIds = addrToWriteThreads.get(addr);
            if (wrtThrdIds != null && !wrtThrdIds.isEmpty()) {
                if (wrtThrdIds.size() > 1) {
                    sharedMemAddr.add(addr);
                } else {
                    Set<Long> readThrdIds = addrToReadThreads.get(addr);
                    if (readThrdIds != null && Sets.union(wrtThrdIds, readThrdIds).size() > 1) {
                        sharedMemAddr.add(addr);
                    }
                }
            }
        }

        List<Event> reducedEvents = new ArrayList<>();
        for (Event event : rawEvents) {
            if (event instanceof MemoryAccessEvent) {
                MemoryAddr addr = ((MemoryAccessEvent) event).getAddr();
                if (sharedMemAddr.contains(addr)) {
                    reducedEvents.add(event);
                }
            } else if (EventType.isLock(event.getType()) || EventType.isUnlock(event.getType())) {
                if (outermostLockingEvents.contains(event)) {
                    reducedEvents.add(event);
                }
            } else {
                reducedEvents.add(event);
            }
        }

        /* remove empty lock regions */
        Table<Long, Long, Event> lockEventTbl = HashBasedTable.create();
        Set<Long> criticalThreadIds = new HashSet<>();
        Set<Event> criticalLockingEvents = new HashSet<>();
        for (Event event : reducedEvents) {
            long tid = event.getTID();
            if (event.isLockEvent()) {
                long lockId = ((SyncEvent) event).getSyncObject();
                Event prevLock = lockEventTbl.put(tid, lockId, event);
                assert prevLock == null : "Unexpected unmatched lock event: " + prevLock;
            } else if (event.isUnlockEvent()) {
                long lockId = ((SyncEvent) event).getSyncObject();
                Event lock = lockEventTbl.remove(tid, lockId);
                if (lock != null) {
                    if (criticalLockingEvents.contains(lock)) {
                        criticalLockingEvents.add(event);
                    }
                } else {
                    if (criticalThreadIds.contains(tid)) {
                        criticalLockingEvents.add(event);
                    }
                }
            } else {
                criticalThreadIds.add(tid);
                for (Event e : lockEventTbl.values()) {
                    if (e.getTID() == tid) {
                        criticalLockingEvents.add(e);
                    }
                }
            }
        }
        for (Event event : reducedEvents) {
            if ((event.isLockEvent() || event.isUnlockEvent())
                    && !criticalLockingEvents.contains(event)) {
                continue;
            }
            addEvent(event);
        }
    }

    public int getSize() {
        return rawEvents.size();
    }

    /**
     * Checks if a memory address is volatile.
     *
     * @param addr
     *            the memory address
     * @return {@code true} if the address is {@code volatile}; otherwise,
     *         {@code false}
     */
    public boolean isVolatileField(MemoryAddr addr) {
        int fieldId = -addr.fieldIdOrArrayIndex();
        return fieldId > 0 && loggingFactory.isVolatile(fieldId);
    }

    static <K,V> List<V> getOrInitEmptyList(Map<K, List<V>> map, K key) {
        List<V> value = map.get(key);
        if (value == null) {
            value = Lists.newArrayList();
        }
        map.put(key, value);
        return value;
    }

    private static <K,V> Set<V> getOrInitEmptySet(Map<K, Set<V>> map, K key) {
        Set<V> value = map.get(key);
        if (value == null) {
            value = Sets.newHashSet();
        }
        map.put(key, value);
        return value;
    }

}
