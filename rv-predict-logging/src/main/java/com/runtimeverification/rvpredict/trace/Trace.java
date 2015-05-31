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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

    private final long baseGID;

    private final int size;

    private final List<RawTrace> rawTraces;

    private boolean hasCriticalWrite;

    /**
     * Map from thread ID to critical events.
     */
    private final Map<Long, List<Event>> tidToEvents = Maps.newHashMap();

    /**
     * Map from thread ID to critical memory access events grouped into blocks.
     */
    private final Map<Long, List<MemoryAccessBlock>> tidToMemoryAccessBlocks = new HashMap<>();

    /**
     * Map from memory addresses to write events ordered by global ID.
     */
    private final Long2ObjectMap<List<Event>> addrToWriteEvents;

    /**
     * Map from memory addresses referenced in this trace segment to their states.
     */
    private final Long2ObjectMap<MemoryAddrState> addrToState;

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

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState state;

    public Trace(TraceState state, List<RawTrace> rawTraces) {
        this.state = state;
        this.size = rawTraces.stream().collect(Collectors.summingInt(RawTrace::size));
        this.rawTraces = rawTraces;
        if (rawTraces.isEmpty()) {
            baseGID = -1;
        } else {
            long min = Long.MAX_VALUE;
            for (RawTrace t : rawTraces) {
                min = Math.min(min, t.getMinGID());
            }
            baseGID = min;
        }
        addrToState = new Long2ObjectLinkedOpenHashMap<>(size);
        addrToWriteEvents = new Long2ObjectLinkedOpenHashMap<>();
        processEvents();
    }

    public Metadata metadata() {
        return state.metadata();
    }

    public long getBaseGID() {
        return baseGID;
    }

    public int getSize() {
        return size;
    }

    public boolean mayContainRaces() {
        // This method can be further improved to skip an entire window ASAP
        // For example, if this window contains no race candidate determined
        // by some static analysis then we can safely skip it
        return hasCriticalWrite;
    }

    /**
     * Gets the initial value of a memory address.
     *
     * @param addr
     *            the address
     * @return the initial value
     */
    public long getInitValueOf(long addr) {
        return addrToState.get(addr).initialValue();
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

    public Map<Long, List<Event>> eventsByThreadID() {
        return tidToEvents;
    }

    public Map<Long, List<MemoryAccessBlock>> memoryAccessBlocksByThreadID() {
        return tidToMemoryAccessBlocks;
    }

    /**
     * Returns the {@link MemoryAccessBlock} that {@code event} belongs to.
     */
    public MemoryAccessBlock getMemoryAccessBlock(Event event) {
        List<MemoryAccessBlock> l = tidToMemoryAccessBlocks.get(event.getTID());
        /* doing binary search on l */
        int low = 0;
        int high = l.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            MemoryAccessBlock midVal = l.get(mid);
            if (midVal.getLast().compareTo(event) < 0) {
                low = mid + 1;
            } else if (midVal.getFirst().compareTo(event) > 0) {
                high = mid - 1;
            } else {
                return l.get(mid); // key found
            }
        }
        throw new IllegalArgumentException("No such block!");
    }

    public List<Event> getInterThreadSyncEvents() {
        List<Event> events = new ArrayList<>();
        tidToEvents.values().forEach(l -> {
            l.stream().filter(e -> e.isStart() || e.isJoin()).forEach(events::add);
        });
        return events;
    }

    public Map<Long, List<LockRegion>> getLockIdToLockRegions() {
        return lockIdToLockRegions;
    }

    public List<Event> getWriteEvents(long addr) {
        return addrToWriteEvents.getOrDefault(addr, Collections.emptyList());
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
            RawTrace t = rawTraces.stream().filter(p -> p.getTID() == tid).findAny().get();
            for (int i = 0; i < t.size(); i++) {
                Event e = t.event(i);
                if (e.getGID() >= gid) break;
                if (e.getType() == EventType.INVOKE_METHOD) {
                    stacktrace.addFirst(e.getLocId());
                } else if (e.getType() == EventType.FINISH_METHOD) {
                    stacktrace.removeFirst();
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

    private static final Function<Object, ? extends List<Event>> NEW_EVENT_LIST = p -> new ArrayList<>();

    private void processEvents() {
        /// PHASE 1
        Map<Long, Map<Event, Event>> tidToLockPairs = Maps.newHashMap();
        for (RawTrace rawTrace : rawTraces) {
            long tid = rawTrace.getTID();
            Map<Event, Event> lockPairs = tidToLockPairs.computeIfAbsent(tid, p -> new HashMap<>());
            tidToThreadState.put(tid, state.getThreadStateSnapshot(tid));
            boolean isInsideClinit = state.isInsideClassInitializer(tid);

            for (int i = 0; i < rawTrace.size(); i++) {
                Event event = rawTrace.event(i);
                if (isInsideClinit) {
                    clinitEvents.add(event);
                }

                if (event.isReadOrWrite()) {
                    /* update memory address state */
                    long addr = event.getAddr();
                    // use the primitive type api instead of computeIfAbsent
                    MemoryAddrState st = addrToState.get(addr);
                    if (st == null) {
                        // TODO(YilongL): revisit the use of object pool here
                        // revise SingleThreadWriteTest to introduce more addresses
                        addrToState.put(addr, st = new MemoryAddrState(state.getValueAt(addr)));
                    }
                    st.touch(event);
                } else if (event.isSyncEvent()) {
                    if (event.isLock()) {
                        event = event.copy();
                        if (state.acquireLock(event).level() == 1) {
                            lockPairs.put(event, null);
                        }
                    } else if (event.isUnlock()) {
                        LockState st = state.releaseLock(event);
                        if (st.level() == 0) {
                            lockPairs.put(st.lock(), event);
                        }
                    }
                } else if (event.isMetaEvent()) {
                    state.onMetaEvent(event);
                    EventType type = event.getType();
                    if (type == EventType.CLINIT_ENTER) {
                        isInsideClinit = true;
                    } else if (type == EventType.CLINIT_EXIT) {
                        isInsideClinit = state.isInsideClassInitializer(tid);
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        /* update memory address value */
        addrToState.forEach((addr, st) -> {
            state.writeValueAt(addr, st.finalValue());
        });

        /* compute shared memory addresses */
        Set<Long> sharedAddr = new HashSet<>();
        addrToState.forEach((addr, state) -> {
            if (state.isWriteShared()) {
                sharedAddr.add(addr);
            }
        });

        /// PHASE 2
        if (!sharedAddr.isEmpty()) {
            for (RawTrace rawTrace : rawTraces) {
                long tid = rawTrace.getTID();
                boolean[] critical = new boolean[rawTrace.size()];
                Map<Event, Event> openLockPairs = new HashMap<>();
                Map<Event, Event> lockPairs = tidToLockPairs.getOrDefault(tid,
                        Collections.emptyMap());
                lockPairs.forEach((l, r) -> {
                    if (l.getGID() < baseGID) {
                        openLockPairs.put(l, r);
                    }
                });

                for (int i = 0; i < rawTrace.size(); i++) {
                    Event event = rawTrace.event(i);
                    if (event.isReadOrWrite()) {
                        critical[i] = sharedAddr.contains(event.getAddr());
                    } else if (event.isSyncEvent()) {
                        if (event.isLock()) {
                            if (lockPairs.containsKey(event)) {
                                Event unlock = lockPairs.get(event);
                                openLockPairs.put(event, unlock);
                            }
                        } else if (event.isUnlock()) {
                            openLockPairs.values().remove(event);
                        } else {
                            critical[i] = true;
                        }
                    } else {
                        // MetaEvents are not critical
                    }

                    if (critical[i]) {
                        openLockPairs.forEach((l, r) -> {
                            Event lock, unlock;
                            if (l.getGID() >= baseGID) {
                                critical[rawTrace.find(l)] = true;
                                lock = l;
                            } else {
                                lock = null;
                            }
                            if (r != null) {
                                critical[rawTrace.find(r)] = true;
                                unlock = r;
                            } else {
                                unlock = null;
                            }
                            lockIdToLockRegions.computeIfAbsent(
                                    lock != null ? lock.getLockId() : unlock.getLockId(),
                                    p -> new ArrayList<>()).add(new LockRegion(lock, unlock));
                        });
                        openLockPairs.clear();
                    }
                }

                /* commit all critical events into this window */
                List<Event> events = new ArrayList<>();
                for (int i = 0; i < rawTrace.size(); i++) {
                    if (critical[i]) {
                        Event event = rawTrace.event(i);
//                        System.err.println(event + " at " + metadata().getLocationSig(event.getLocId()));

                        /* update tidToEvents & addrToWriteEvents */
                        events.add(event);
                        if (event.isWrite()) {
                            hasCriticalWrite = true;
                            addrToWriteEvents.computeIfAbsent(event.getAddr(), NEW_EVENT_LIST).add(event);
                        }
                    }
                    if (!events.isEmpty()) {
                        tidToEvents.put(tid, events);
                    }
                }
            }

            /* compute more derivative information */
            if (hasCriticalWrite) {
                tidToEvents.forEach((tid, events) -> tidToMemoryAccessBlocks.put(tid,
                        divideMemoryAccessBlocks(events)));
            }
        }
    }

    private List<MemoryAccessBlock> divideMemoryAccessBlocks(List<Event> events) {
        List<MemoryAccessBlock> blocks = new ArrayList<>();
        MemoryAccessBlock lastBlock = null;
        List<Event> crntBlock = new ArrayList<>();
        Event lastEvent = null;
        for (Event event : events) {
            /* update memory access blocks */
            boolean endCrntBlock;
            if (event.isSyncEvent()) {
                endCrntBlock = true;
            } else if (event.isReadOrWrite()) {
                if (event.isRead()) {
                    /* do not end the block if the previous event is a write or
                     * a duplicate read (i.e., a read event that differs only in
                     * global ID) */
                    endCrntBlock = lastEvent != null &&
                            !(lastEvent.isWrite() || lastEvent.isRead()
                            && lastEvent.getAddr() == event.getAddr()
                            && lastEvent.getValue() == event.getValue());
                } else {
                    endCrntBlock = lastEvent != null && lastEvent.isRead();
                }
            } else {
                throw new IllegalStateException("Unexpected critical event: " + event);
            }
            if (endCrntBlock && !crntBlock.isEmpty()) {
                /* end the current block and then start a new one */
                blocks.add(lastBlock = new MemoryAccessBlock(crntBlock, lastBlock));
                crntBlock = new ArrayList<>();
            }
            if (event.isReadOrWrite()) {
                /* append to the current block */
                crntBlock.add(event);
            }
            lastEvent = event;
        }

        if (!crntBlock.isEmpty()) {
            blocks.add(new MemoryAccessBlock(crntBlock, lastBlock));
        }

        return blocks;
    }

}
