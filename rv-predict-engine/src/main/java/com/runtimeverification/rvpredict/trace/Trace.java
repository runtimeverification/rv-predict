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
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Constants;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    private final int capacity;

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
    private final Map<Long, ThreadStatus> threadIdToInitThreadStatus = Maps.newHashMap();

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

    private final Set<MemoryAddr> unsafeMemoryAddresses = Sets.newHashSet();

    private final Metadata metadata;

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState crntState;

    public Trace(TraceState crntState, int capacity) {
        this.crntState = crntState;
        this.capacity = capacity;
        this.metadata = crntState.metadata();
        this.initHeldLockToStacktrace = crntState.getHeldLockStacktraceSnapshot();
    }

    public Metadata metadata() {
        return metadata;
    }

    public int capacity() {
        return capacity;
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

    public boolean isUnsafeAddress(MemoryAddr addr) {
        return unsafeMemoryAddresses.contains(addr);
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
            for (int locId : threadIdToInitThreadStatus.get(tid).stacktrace) {
                stacktrace.add(metadata.getLocationSig(locId));
            }
            for (MetaEvent e : threadIdToCallStackEvents.getOrDefault(tid,
                    Collections.<MetaEvent> emptyList())) {
                if (e.getGID() >= event.getGID()) {
                    break;
                }

                if (e.getType() == EventType.INVOKE_METHOD) {
                    stacktrace.add(metadata.getLocationSig(e.getLocId()));
                } else {
                    stacktrace.remove(stacktrace.size() - 1);
                }
            }
            stacktrace.add(metadata.getLocationSig(event.getLocId()));
        } else {
            /* event is from previous windows */
            if (initHeldLockToStacktrace.containsKey(event)) {
                stacktrace.addAll(initHeldLockToStacktrace.get(event));
            } else {
                stacktrace.add(metadata.getLocationSig(event.getLocId()));
                stacktrace.add("... stack trace not available ...");
            }
        }
        return stacktrace;
    }

    public List<LockObject> getHeldLocksAt(MemoryAccessEvent memAcc) {
        long tid = memAcc.getTID();
        Map<Long, Deque<SyncEvent>> map = Maps.newHashMap();
        for (Map.Entry<Long, List<SyncEvent>> entry : threadIdToInitThreadStatus.get(tid).lockStatus
                .entrySet()) {
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

    public SyncEvent getStartEventOf(long tid) {
        return threadIdToInitThreadStatus.get(tid).startEvent;
    }

    /**
     * Gets control-flow dependent events of a given {@code MemoryAccessEvent}.
     * Without any knowledge about the control flow of the program, all read
     * events that happen-before the given event have to be included
     * conservatively.
     */
    public List<ReadEvent> getCtrlFlowDependentEvents(MemoryAccessEvent memAccEvent) {
        List<ReadEvent> readEvents = new ArrayList<>();
        for (Event e : getThreadEvents(memAccEvent.getTID())) {
            if (e.getGID() >= memAccEvent.getGID()) {
                break;
            }

            if (e instanceof ReadEvent) {
                readEvents.add((ReadEvent) e);
            }
        }

        return readEvents;
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
        if (!threadIdToInitThreadStatus.containsKey(tid)) {
            ThreadStatus initThreadInfo = getCurrentThreadStatus(tid);
            threadIdToInitThreadStatus.putIfAbsent(tid, initThreadInfo);
        }

        if (event instanceof MemoryAccessEvent) {
            MemoryAccessEvent memAcc = (MemoryAccessEvent) event;
            MemoryAddr addr = memAcc.getAddr();
            long value = memAcc.getValue();
            long crntVal = crntState.getValueAt(addr);
            addrToInitValue.putIfAbsent(addr, crntVal);
            if (event instanceof ReadEvent) {
                if (value != Constants._0X_DEADBEEFL && value != crntVal) {
                    if (Configuration.debug) {
                        System.err.printf(
                            String.format("[Warning] logged trace not sequential consistent:%n"
                                    + "  event %s reads a different value than the currently stored value %s%n"
                                    + "    at %s%n",
                                    memAcc, crntVal, metadata.getLocationSig(memAcc.getLocId())));
                    }
                    crntState.writeValueAt(addr, crntVal);
                    unsafeMemoryAddresses.add(addr);
                }
            } else {
                crntState.writeValueAt(addr, memAcc.getValue());
            }
        } else if (event.getType() == EventType.START) {
            crntState.onThreadStart((SyncEvent) event);
        } else if (EventType.isLock(event.getType()) || EventType.isUnlock(event.getType())) {
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

    private ThreadStatus getCurrentThreadStatus(long tid) {
        return new ThreadStatus(crntState.getStacktraceSnapshot(tid),
                crntState.getThreadStartEvent(tid), crntState.getLockStatusSnapshot(tid));
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
            case JOIN:
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
            } else if (!(event instanceof MetaEvent)) {
                reducedEvents.add(event);
            }
        }

        /* remove complete yet empty lock regions */
        Table<Long, Long, Event> lockEventTbl = HashBasedTable.create();
        Set<Event> criticalLockingEvents = new HashSet<>();
        for (Event event : reducedEvents) {
            long tid = event.getTID();
            Map<Long, Event> lockStatus = lockEventTbl.row(tid);

            if (event.isLockEvent()) {
                long lockId = ((SyncEvent) event).getSyncObject();
                Event prevLock = lockStatus.put(lockId, event);
                assert prevLock == null : "Unexpected unmatched lock event: " + prevLock;
            } else if (event.isUnlockEvent()) {
                long lockId = ((SyncEvent) event).getSyncObject();
                Event lock = lockStatus.remove(lockId);
                if (lock == null || criticalLockingEvents.contains(lock)) {
                    criticalLockingEvents.add(event);
                }
            } else {
                for (Event e : lockStatus.values()) {
                    criticalLockingEvents.add(e);
                }
            }
        }
        criticalLockingEvents.addAll(lockEventTbl.values());
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
        return fieldId > 0 && metadata.isVolatile(fieldId);
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

    private static class ThreadStatus {

        private final List<Integer> stacktrace;
        private final SyncEvent startEvent;
        private final Map<Long, List<SyncEvent>> lockStatus;

        private ThreadStatus(List<Integer> stacktrace, SyncEvent startEvent,
                Map<Long, List<SyncEvent>> lockStatus) {
            this.stacktrace = stacktrace;
            this.startEvent = startEvent;
            this.lockStatus = lockStatus;
        }
    }

}
