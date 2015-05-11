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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.*;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    private final int numOfEvents;

    private final long baseGID;

    private final Event[] events;

    /**
     * Events of each thread.
     */
    private final Map<Long, List<Event>> threadIdToEvents = new HashMap<>();

    /**
     * List of START/JOIN events in this window.
     */
    private final List<Event> startJoinEvents = new ArrayList<>();

    /**
     * Wait/Lock/Unlock events indexed by the involved lock object.
     */
    private final Map<Long, List<Event>> lockIdToLockEvents = new HashMap<>();

    /**
     * Write events on each address.
     */
    private final Map<MemoryAddr, List<Event>> addrToWriteEvents = new HashMap<>();

    /**
     * Lists of {@code MemoryAccessEvent}'s indexed by address and thread ID.
     */
    private final Table<MemoryAddr, Long, List<Event>> memAccessEventsTbl = HashBasedTable.create();

    /**
     * Map from memory addresses referenced in this trace segment to their states.
     */
    private final Map<MemoryAddr, MemoryAddrState> addrToState = Maps.newHashMap();

    /**
     * The initial states for all threads referenced in this trace segment.
     * It is computed as the value in the {@link #state} before the first
     * event of that thread occurs in this trace segment.
     */
    private final Map<Long, ThreadState> tidToThreadState = Maps.newHashMap();

    /**
     * Map from thread ID to outermost lock pairs.
     */
    private final Map<Long, Map<Event, Event>> tidToLockPairs = Maps.newHashMap();

    /**
     * Set of {@code MemoryAccessEvent}'s that happen during class initialization.
     */
    private final Set<Event> clinitEvents = Sets.newHashSet();

    private final Metadata metadata;

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState state;

    public Trace(TraceState crntState, Event[] events, int numOfEvents) {
        this.state = crntState;
        this.metadata = crntState.metadata();
        this.numOfEvents = numOfEvents;
        this.events = events;
        this.baseGID = numOfEvents > 0 ? events[0].getGID() : -1;
        processEvents();
    }

    public Metadata metadata() {
        return metadata;
    }

    public int getSize() {
        return numOfEvents;
    }

    public boolean mayContainRaces() {
        // This method can be further improved to skip an entire window ASAP
        // For example, if this window contains no race candidate determined
        // by some static analysis then we can safely skip it
        return !memAccessEventsTbl.isEmpty();
    }

    /**
     * Gets the initial value of a memory address.
     *
     * @param addr
     *            the address
     * @return the initial value
     */
    public long getInitValueOf(MemoryAddr addr) {
        return addrToState.get(addr).initVal;
    }

    public Event getFirstEvent(long tid) {
        List<Event> events = threadIdToEvents.get(tid);
        return events == null ? null : events.get(0);
    }

    public Event getLastEvent(long tid) {
        List<Event> events = threadIdToEvents.get(tid);
        return events == null ? null : events.get(events.size() - 1);
    }

    public List<Event> getEvents(long tid) {
        return threadIdToEvents.getOrDefault(tid, Collections.emptyList());
    }

    public Collection<List<Event>> perThreadView() {
        return threadIdToEvents.values();
    }

    public List<Event> getStartJoinEvents() {
        return startJoinEvents;
    }

    public Collection<List<Event>> getLockEventsCollection() {
        return lockIdToLockEvents.values();
    }

    public List<Event> getWriteEventsOn(MemoryAddr addr) {
        // TODO(YilongL): consider index write events based on both memory address and value
        return addrToWriteEvents.getOrDefault(addr, Collections.emptyList());
    }

    public Set<MemoryAddr> getMemoryAddresses() {
        return memAccessEventsTbl.rowKeySet();
    }

    public Table<MemoryAddr, Long, List<Event>> getMemAccessEventsTable() {
        // TODO(YilongL): the API of Trace should not expose the memory access events table
        return memAccessEventsTbl;
    }

    public boolean isInsideClassInitializer(Event event) {
        return clinitEvents.contains(event);
    }

    /**
     * Returns the {@code String} representation of the stack trace at the point
     * when a given {@code event} happened.
     *
     * @param event
     *            the event
     * @return a {@code Deque} of stack trace element represented as location
     *         ID's; {@code -1} represents missing stack trace elements
     */
    public Deque<Integer> getStacktraceAt(Event event) {
        long tid = event.getTID();
        long gid = event.getGID();
        Deque<Integer> stacktrace = new ArrayDeque<>();
        if (gid >= baseGID) {
            /* event is in the current window; reassemble its stack trace */
            tidToThreadState.getOrDefault(tid, state.getThreadState(tid)).getStacktrace()
                    .forEach(stacktrace::addFirst);
            for (int i = 0; i < numOfEvents; i++) {
                Event e = events[i];
                if (e.getGID() >= gid) break;
                if (e.getTID() == tid) {
                    if (e.getType() == EventType.INVOKE_METHOD) {
                        stacktrace.addFirst(e.getLocId());
                    } else if (e.getType() == EventType.FINISH_METHOD) {
                        stacktrace.removeFirst();
                    }
                }
            }
            stacktrace.addFirst(event.getLocId());
        } else {
            /* event is from previous windows */
            stacktrace.add(event.getLocId());
            stacktrace.add(-1);
        }
        return stacktrace;
    }

    /**
     * Returns the locks held by the owner thread when a given {@code Event} occurs.
     */
    public List<Event> getHeldLocksAt(Event event) {
        long tid = event.getTID();
        Map<Long, LockState> lockIdToLockState = tidToThreadState
                .getOrDefault(tid, state.getThreadState(tid)).getLockStates().stream()
                .collect(Collectors.toMap(ls -> ls.lock().getSyncObject(), LockState::copy));
        for (Event e : getEvents(tid)) {
            if (e.getGID() >= event.getGID()) break;
            if (e.isLock()) {
                lockIdToLockState.computeIfAbsent(e.getSyncObject(), p -> new LockState())
                        .acquire(e);
            } else if (e.isUnlock()) {
                lockIdToLockState.get(e.getSyncObject()).release();
            }
        }

        List<Event> lockEvents = lockIdToLockState.values().stream()
                .filter(lockState -> lockState.level() > 0)
                .map(LockState::lock).collect(Collectors.toList());
        Collections.sort(lockEvents, (e1, e2) -> e1.compareTo(e2));
        return lockEvents;
    }

    /**
     * Gets control-flow dependent events of a given {@code Event}. Without any
     * knowledge about the control flow of the program, all read events that
     * happen-before the given event have to be included conservatively.
     */
    public List<Event> getCtrlFlowDependentEvents(Event event) {
        // TODO(YilongL): optimize this!
        List<Event> readEvents = new ArrayList<>();
        for (Event e : getEvents(event.getTID())) {
            if (e.getGID() >= event.getGID()) break;
            if (e.isRead()) {
                readEvents.add(e);
            }
        }

        return readEvents;
    }

    private void processEvents() {
        /// PHASE 1
        boolean hasThreadInsideClinit = state.hasThreadInsideClinit();
        for (int i = 0; i < numOfEvents; i++) {
            Event event = events[i];
            long tid = event.getTID();
            if (hasThreadInsideClinit) {
                if (state.isInsideClassInitializer(tid)) {
                    clinitEvents.add(event);
                }
            }

            if (event.isReadOrWrite()) {
                /* update memory address state */
                MemoryAddrState st = addrToState.computeIfAbsent(event.getAddr(),
                        addr -> new MemoryAddrState(state.getValueAt(addr)));
                if (event.isRead()) {
                    st.readBy(tid);
                } else {
                    st.writtenBy(tid, i);
                }
            } else if (event.isSyncEvent()) {
                if (event.isLock()) {
                    tidToThreadState.computeIfAbsent(tid, state::getThreadStateSnapshot);
                    event = event.copy();
                    if (state.acquireLock(event).level() == 1) {
                        tidToLockPairs.computeIfAbsent(tid, p -> new HashMap<>())
                            .put(event, null);
                    }
                } else if (event.isUnlock()) {
                    tidToThreadState.computeIfAbsent(tid, state::getThreadStateSnapshot);
                    LockState st = state.releaseLock(event);
                    if (st.level() == 0) {
                        tidToLockPairs.computeIfAbsent(tid, p -> new HashMap<>())
                            .put(st.lock(), event);
                    }
                }
            } else if (event.isMetaEvent()) {
                EventType type = event.getType();
                if (type == EventType.INVOKE_METHOD || type == EventType.FINISH_METHOD) {
                    tidToThreadState.computeIfAbsent(tid, state::getThreadStateSnapshot);
                }
                state.onMetaEvent(event);
                if (type == EventType.CLINIT_ENTER) {
                    hasThreadInsideClinit = true;
                } else if (type == EventType.CLINIT_EXIT) {
                    hasThreadInsideClinit = state.hasThreadInsideClinit();
                }
            } else {
                throw new IllegalStateException();
            }
        }

        /* update memory address value */
        addrToState.forEach((addr, st) -> {
            if (st.lastWrite >= 0) {
                state.writeValueAt(addr, events[st.lastWrite].getValue());
            }
        });

        /* compute shared memory addresses */
        Set<MemoryAddr> sharedAddr = new HashSet<>();
        addrToState.forEach((addr, state) -> {
            if (state.isWriteShared()) {
                sharedAddr.add(addr);
            }
        });

        /// PHASE 2
        if (!sharedAddr.isEmpty()) {
            long baseGID = events[0].getGID();
            boolean[] critical = new boolean[numOfEvents];
            Map<Long, Map<Integer, Integer>> tidToOpenLockIndices = new HashMap<>();
            tidToLockPairs.forEach((tid, pairs) -> {
                Map<Integer, Integer> indices = new HashMap<>();
                tidToOpenLockIndices.put(tid, indices);
                pairs.forEach((l, r) -> {
                    if (l.getGID() < baseGID) {
                        indices.put(getEventOffset(l), r == null ? null : getEventOffset(r));
                    }
                });
            });

            for (int i = 0; i < numOfEvents; i++) {
                Event event = events[i];
                long tid = event.getTID();
                if (event.isReadOrWrite()) {
                    critical[i] = sharedAddr.contains(event.getAddr());
                } else if (event.isSyncEvent()) {
                    if (event.isLock()) {
                        if (tidToLockPairs.get(tid).containsKey(event)) {
                            Event unlock = tidToLockPairs.get(tid).get(event);
                            tidToOpenLockIndices.get(tid).put(i,
                                    unlock == null ? null : getEventOffset(unlock));
                        }
                    } else if (event.isUnlock()) {
                        tidToOpenLockIndices.get(tid).values().remove(i);
                    } else {
                        critical[i] = true;
                    }
                } else {
                    // MetaEvents are not critical
                }

                if (critical[i]) {
                    Map<Integer, Integer> indices = tidToOpenLockIndices.get(tid);
                    if (indices != null) {
                        indices.forEach((l, r) -> {
                            if (l >= 0) {
                                critical[l] = true;
                            }
                            if (r != null) {
                                critical[r] = true;
                            }
                        });
                        indices.clear();
                    }
                }
            }

            /* finally commit all critical events into this window */
            for (int i = 0; i < numOfEvents; i++) {
                if (critical[i]) {
                    addEvent(events[i]);
                }
            }
        }
    }

    private int getEventOffset(Event event) {
        return (int) (event.getGID() - baseGID);
    }

    private static class MemoryAddrState {
        int lastWrite = -1;
        final long initVal;
        long reader1, reader2;
        long writer1, writer2;

        MemoryAddrState(long value) {
            initVal = value;
        }

        void readBy(long tid) {
            if (reader1 == 0) {
                reader1 = tid;
            } else if (reader1 != tid && reader2 == 0) {
                reader2 = tid;
            }
        }

        void writtenBy(long tid, int idx) {
            lastWrite = idx;
            if (writer1 == 0) {
                writer1 = tid;
            } else if (writer1 != tid && writer2 == 0) {
                writer2 = tid;
            }
        }

        boolean isWriteShared() {
            return writer2 != 0 || writer1 != 0
                    && (reader1 != 0 && reader1 != writer1 || reader2 != 0 && reader2 != writer1);
        }
    }

    private static final Function<Object, ? extends List<Event>> NEW_EVENT_LIST = p -> new ArrayList<>();

    /**
     * add a new filtered event to the trace in the order of its appearance
     *
     * @param event
     */
    private void addEvent(Event event) {
//        System.err.println(event + " at " + metadata.getLocationSig(event.getLocId()));
        long tid = event.getTID();

        threadIdToEvents.computeIfAbsent(tid, NEW_EVENT_LIST).add(event);
        if (event.isReadOrWrite()) {
            MemoryAddr addr = event.getAddr();

            memAccessEventsTbl.row(addr).computeIfAbsent(tid, NEW_EVENT_LIST).add(event);

            if (event.isWrite()) {
                addrToWriteEvents.computeIfAbsent(addr, NEW_EVENT_LIST).add(event);
            }
        } else {
            switch (event.getType()) {
            case START:
            case JOIN:
                startJoinEvents.add(event);
                break;
            case WRITE_LOCK:
            case WRITE_UNLOCK:
            case READ_LOCK:
            case READ_UNLOCK:
                lockIdToLockEvents.computeIfAbsent(event.getSyncObject(), NEW_EVENT_LIST).add(event);
                break;
            default:
                assert false : "unexpected event: " + event;
            }
        }
    }

}
