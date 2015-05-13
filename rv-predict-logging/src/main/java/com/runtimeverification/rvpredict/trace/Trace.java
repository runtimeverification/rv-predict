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
     * Map from thread ID to critical events.
     */
    private final Map<Long, List<Event>> tidToEvents = Maps.newHashMap();

    /**
     * Map from memory addresses to write events ordered by global ID.
     */
    private final Map<MemoryAddr, List<Event>> addrToWriteEvents = Maps.newHashMap();

    /**
     * List of memory access blocks.
     */
    private final List<MemoryAccessBlock> memoryAccessBlocks = Lists.newArrayList();

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
     * Map from lock ID to critical lock pairs.
     */
    private final Map<Long, List<LockRegion>> lockIdToLockRegions = Maps.newHashMap();

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

    public long getBaseGID() {
        return baseGID;
    }

    public int getSize() {
        return numOfEvents;
    }

    public boolean mayContainRaces() {
        // This method can be further improved to skip an entire window ASAP
        // For example, if this window contains no race candidate determined
        // by some static analysis then we can safely skip it
        return !memoryAccessBlocks.isEmpty();
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
        List<Event> events = tidToEvents.get(tid);
        return events == null ? null : events.get(0);
    }

    public Event getLastEvent(long tid) {
        List<Event> events = tidToEvents.get(tid);
        return events == null ? null : events.get(events.size() - 1);
    }

    public List<Event> getEvents(long tid) {
        return tidToEvents.getOrDefault(tid, Collections.emptyList());
    }

    public Collection<List<Event>> perThreadView() {
        return tidToEvents.values();
    }

    public Map<Long, List<LockRegion>> getLockIdToLockRegions() {
        return lockIdToLockRegions;
    }

    public List<Event> getWriteEvents(MemoryAddr addr) {
        return addrToWriteEvents.getOrDefault(addr, Collections.emptyList());
    }

    public List<MemoryAccessBlock> getMemoryAccessBlocks() {
        return memoryAccessBlocks;
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

    private static final Function<Object, ? extends List<Event>> NEW_EVENT_LIST = p -> new ArrayList<>();

    private void processEvents() {
        /// PHASE 1
        Map<Long, Map<Event, Event>> tidToLockPairs = Maps.newHashMap();
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
                    st.readBy(tid, i);
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
            int lastAccess = Math.max(st.lastRead, st.lastWrite);
            if (lastAccess >= 0) {
                /* use the value of the last access to update state, instead of
                 * that of the last write, to recover from potential missing
                 * write events */
                state.writeValueAt(addr, events[lastAccess].getValue());
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
                            Event lock, unlock;
                            if (l >= 0) {
                                critical[l] = true;
                                lock = events[l];
                            } else {
                                lock = null;
                            }
                            if (r != null) {
                                critical[r] = true;
                                unlock = events[r];
                            } else {
                                unlock = null;
                            }
                            lockIdToLockRegions.computeIfAbsent(
                                    lock != null ? lock.getLockId() : unlock.getLockId(),
                                    p -> new ArrayList<>()).add(new LockRegion(lock, unlock));
                        });
                        indices.clear();
                    }
                }
            }

            /* commit all critical events into this window */
            Map<Long, List<Event>> tidToCrntBlock = new HashMap<>();
            for (int i = 0; i < numOfEvents; i++) {
                if (critical[i]) {
                    Event event = events[i];
//                    System.err.println(event + " at " + metadata.getLocationSig(event.getLocId()));
                    long tid = event.getTID();

                    /* update memory access blocks */
                    boolean endCrntBlock;
                    if (event.isSyncEvent()) {
                        endCrntBlock = true;
                    } else if (event.isReadOrWrite()) {
                        Event lastEvent = Iterables.getLast(
                                tidToEvents.getOrDefault(tid, Collections.emptyList()), null);
                        if (event.isRead()) {
                            /* Optimization: merge consecutive read events that are equivalent */
                            endCrntBlock = !(lastEvent != null && lastEvent.isRead()
                                    && lastEvent.getAddr().equals(event.getAddr())
                                    && lastEvent.getValue() == event.getValue());
                        } else {
                            endCrntBlock = lastEvent != null && lastEvent.isRead();
                        }
                    } else {
                        throw new IllegalStateException("Unexpected critical event: " + event);
                    }
                    if (endCrntBlock) {
                        /* end the current block and then start a new one */
                        List<Event> oldBlk = tidToCrntBlock.get(tid);
                        if (oldBlk != null && !oldBlk.isEmpty()) {
                            memoryAccessBlocks.add(new MemoryAccessBlock(oldBlk));
                            tidToCrntBlock.put(tid, new ArrayList<>());
                        }
                    }
                    if (event.isReadOrWrite()) {
                        /* append to the current block */
                        tidToCrntBlock.computeIfAbsent(tid, NEW_EVENT_LIST).add(event);
                    }

                    /* update tidToEvents & addrToWriteEvents */
                    tidToEvents.computeIfAbsent(tid, NEW_EVENT_LIST).add(event);
                    if (event.isWrite()) {
                        addrToWriteEvents.computeIfAbsent(event.getAddr(), NEW_EVENT_LIST).add(event);
                    }
                }
            }
            tidToCrntBlock.values().forEach(blk -> {
                if (!blk.isEmpty()) {
                    memoryAccessBlocks.add(new MemoryAccessBlock(blk));
                }
            });
        }
    }

    private int getEventOffset(Event event) {
        return (int) (event.getGID() - baseGID);
    }

    private static class MemoryAddrState {
        int lastRead = -1;
        int lastWrite = -1;
        final long initVal;
        long reader1, reader2;
        long writer1, writer2;

        MemoryAddrState(long value) {
            initVal = value;
        }

        void readBy(long tid, int idx) {
            lastRead = idx;
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

}
