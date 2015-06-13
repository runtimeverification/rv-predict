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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToObjectMap;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.util.Logger;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    private final long baseGID;

    private final int size;

    private final List<RawTrace> rawTraces;

    private boolean hasCriticalEvent;

    /**
     * Map from thread ID to critical events.
     */
    private final Map<Long, List<Event>> tidToEvents;

    /**
     * Map from thread ID to critical memory access events grouped into blocks.
     */
    private final Map<Long, List<MemoryAccessBlock>> tidToMemoryAccessBlocks;

    /**
     * The initial states for all threads referenced in this trace segment.
     * It is computed as the value in the {@link #state} before the first
     * event of that thread occurs in this trace segment.
     */
    private final Map<Long, ThreadState> tidToThreadState;

    /**
     * Map from memory addresses referenced in this trace segment to their states.
     */
    private final MemoryAddrToStateMap addrToState;

    /**
     * Map from (thread ID, memory address) to write events.
     */
    private final Table<Long, Long, List<Event>> tidToAddrToWriteEvents;

    /**
     * Map from lock ID to critical lock pairs.
     */
    private final Map<Long, List<LockRegion>> lockIdToLockRegions;

    /**
     * Set of {@code MemoryAccessEvent}'s that happen during class initialization.
     */
    private final Set<Event> clinitEvents;

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState state;

    public Trace(TraceState state, List<RawTrace> rawTraces,
            Map<Long, List<Event>> tidToEvents,
            Map<Long, List<MemoryAccessBlock>> tidToMemoryAccessBlocks,
            Map<Long, ThreadState> tidToThreadState,
            MemoryAddrToStateMap addrToState,
            Table<Long, Long, List<Event>> tidToAddrToEvents,
            Map<Long, List<LockRegion>> lockIdToLockRegions,
            Set<Event> clinitEvents) {
        this.state = state;
        this.size = rawTraces.stream().collect(Collectors.summingInt(RawTrace::size));
        this.rawTraces = rawTraces;
        this.tidToEvents = tidToEvents;
        this.tidToMemoryAccessBlocks = tidToMemoryAccessBlocks;
        this.tidToThreadState = tidToThreadState;
        this.addrToState = addrToState;
        this.tidToAddrToWriteEvents = tidToAddrToEvents;
        this.lockIdToLockRegions = lockIdToLockRegions;
        this.clinitEvents = clinitEvents;

        if (rawTraces.isEmpty()) {
            baseGID = -1;
        } else {
            long min = Long.MAX_VALUE;
            for (RawTrace t : rawTraces) {
                min = Math.min(min, t.getMinGID());
            }
            baseGID = min;
        }
        processEvents();
    }

    public Logger logger() {
        return state.logger();
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
        return hasCriticalEvent;
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

    public Iterable<Event> getWriteEvents(long addr) {
        return Iterables.concat(tidToAddrToWriteEvents.column(addr).values());
    }

    private Event getPrevWrite(long gid, long tid, long addr) {
        List<Event> list = tidToAddrToWriteEvents.get(tid, addr);
        if (list == null || list.isEmpty() || list.get(0).getGID() >= gid) {
            return null;
        }

        /* binary-searching the latest write before gid */
        Event e = null;
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Event midVal = list.get(mid);
            int cmp = Long.compare(midVal.getGID(), gid);

            if (cmp < 0) {
                low = mid + 1;
                e = midVal;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                break;
            }
        }

        return e;
    }

    public Event getSameThreadPrevWrite(Event read) {
        return getPrevWrite(read.getGID(), read.getTID(), read.getAddr());
    }

    public Event getAllThreadsPrevWrite(Event read) {
        Event prevWrite = null;
        for (long tid : tidToAddrToWriteEvents.rowKeySet()) {
           Event e = getPrevWrite(read.getGID(), tid, read.getAddr());
           if (prevWrite == null || e != null && e.getGID() < prevWrite.getGID()) {
               prevWrite = e;
           }
        }
        return prevWrite;
    }

    public Map<Long, List<LockRegion>> getLockIdToLockRegions() {
        return lockIdToLockRegions;
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
                .collect(Collectors.toMap(ls -> ls.lockId(), LockState::copy));
        for (Event e : getEvents(tid)) {
            if (e.getGID() >= event.getGID()) break;
            if (e.isLock()) {
                lockIdToLockState.computeIfAbsent(e.getLockId(), LockState::new)
                        .acquire(e);
            } else if (e.isUnlock()) {
                lockIdToLockState.get(e.getLockId()).release(e);
            }
        }

        List<Event> lockEvents = lockIdToLockState.values().stream()
                .filter(LockState::isAcquired)
                .map(LockState::lock).collect(Collectors.toList());
        Collections.sort(lockEvents, (e1, e2) -> e1.compareTo(e2));
        return lockEvents;
    }

    private void processEvents() {
        boolean isSingleThreaded = rawTraces.size() < 2;

        /// PHASE 1
        Set<Event> outermostLockEvents = new HashSet<>(getSize() / 10);
        for (RawTrace rawTrace : rawTraces) {
            long tid = rawTrace.getTID();
            tidToThreadState.put(tid, state.getThreadStateSnapshot(tid));
            boolean isInsideClinit = state.isInsideClassInitializer(tid);

            for (int i = 0; i < rawTrace.size(); i++) {
                Event event = rawTrace.event(i);
                if (isInsideClinit) {
                    clinitEvents.add(event);
                }

                if (event.isReadOrWrite()) {
                    /* update memory address state */
                    if (!isSingleThreaded) {
                        MemoryAddrState st = addrToState.computeIfAbsent(event.getAddr());
                        st.touch(event);
                    }
                } else if (event.isSyncEvent()) {
                    if (event.isLock()) {
                        event = event.copy();
                        if (state.acquireLock(event) == 1) {
                            outermostLockEvents.add(event);
                        }
                    } else if (event.isUnlock()) {
                        if (state.releaseLock(event) == 0) {
                            outermostLockEvents.add(event);
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

        Set<Long> sharedAddr = new HashSet<>();
        for (LongToObjectMap<MemoryAddrState>.EntryIterator iter = addrToState.iterator();
                iter.hasNext(); iter.incCursor()) {
            long addr = iter.getNextKey();
            MemoryAddrState st = iter.getNextValue();

            /* compute shared memory addresses */
            if (st.isWriteShared()) {
                sharedAddr.add(addr);
            }
        }

        /// PHASE 2
        if (!sharedAddr.isEmpty()) {
            for (RawTrace rawTrace : rawTraces) {
                /* step 1: remove thread-local events and nested lock events */
                int tmp_size = 0;
                Event[] tmp_events = new Event[rawTrace.size()];
                for (int i = 0; i < rawTrace.size(); i++) {
                    Event event = rawTrace.event(i);
                    if (event.isReadOrWrite()) {
                        if (sharedAddr.contains(event.getAddr())) {
                            tmp_events[tmp_size++] = event;
                        }
                    } else if (event.isSyncEvent()) {
                        if (event.isLock() || event.isUnlock()) {
                            if (outermostLockEvents.contains(event)) {
                                tmp_events[tmp_size++] = event;
                            }
                        } else {
                            tmp_events[tmp_size++] = event;
                        }
                    } else {
                        // MetaEvents are thrown away
                    }
                }

                /* step 2: remove recurrent patterns and empty lock regions */
                MemoryAddrToObjectMap<Integer> addrToLastReadIdx = new MemoryAddrToObjectMap<>(
                        sharedAddr.size());
                Map<Long, Integer> lockIdToOpenReadLockIdx = new HashMap<>();
                Map<Long, Integer> lockIdToOpenWriteLockIdx = new HashMap<>();
                Set<Integer> pendingLockIndexes = new HashSet<>();
                boolean[] critical = new boolean[tmp_size];
                boolean hasCritical = false;
                for (int i = 0; i < tmp_size; i++) {
                    Event event = tmp_events[i];
                    if (event.isRead()) {
                        Integer lastReadIdx = addrToLastReadIdx.put(event.getAddr(), i);
                        if (lastReadIdx != null) {
                            // TODO(YilongL): formal proof of this optimization
                            int nextIdx = skipRecurrentPatterns(tmp_events, tmp_size, lastReadIdx, i);
                            if (nextIdx != i) {
                                i = nextIdx - 1;
                                continue;
                            } else {
                                critical[i] = true;
                            }
                        } else {
                            critical[i] = true;
                        }
                    } else if (event.isWrite()) {
                        critical[i] = true;
                    } else if (event.isLock()) {
                        /* whether a lock event is critical cannot be determined immediately */
                        (event.isReadLock() ? lockIdToOpenReadLockIdx : lockIdToOpenWriteLockIdx)
                                .put(event.getLockId(), i);
                        pendingLockIndexes.add(i);
                    } else if (event.isUnlock()) {
                        Integer idx = (event.isReadUnlock() ?
                                lockIdToOpenReadLockIdx : lockIdToOpenWriteLockIdx)
                                .remove(event.getLockId());
                        pendingLockIndexes.remove(idx);

                        critical[i] = idx == null ? hasCritical : critical[idx];
                        if (critical[i]) {
                            lockIdToLockRegions
                                .computeIfAbsent(event.getLockId(), p -> new ArrayList<>())
                                .add(new LockRegion(idx == null ? null : tmp_events[idx], event));
                        }
                    } else {
                        critical[i] = true;
                    }

                    if (critical[i]) {
                        hasCritical = true;
                        pendingLockIndexes.forEach(idx -> critical[idx] = true);
                        pendingLockIndexes.clear();
                    }
                }
                Iterables.concat(lockIdToOpenReadLockIdx.values(),
                        lockIdToOpenWriteLockIdx.values()).forEach(idx -> {
                   if (critical[idx]) {
                       lockIdToLockRegions
                           .computeIfAbsent(tmp_events[idx].getLockId(), p -> new ArrayList<>())
                           .add(new LockRegion(tmp_events[idx], null));
                   }
                });

                /* commit all critical events into this window */
                List<Event> events = new ArrayList<>();
                for (int i = 0; i < tmp_size; i++) {
                    if (critical[i]) {
                        Event event = tmp_events[i];
//                        logger().debug().println(event + " at " + metadata().getLocationSig(event.getLocId()));

                        /* update tidToEvents & tidToAddrToWriteEvents */
                        events.add(event);
                        if (event.isWrite()) {
                            tidToAddrToWriteEvents.row(event.getTID())
                                    .computeIfAbsent(event.getAddr(), p -> new ArrayList<>())
                                    .add(event);
                        }
                    }
                }
                if (!events.isEmpty()) {
                    hasCriticalEvent = true;
                    tidToEvents.put(rawTrace.getTID(), events);
                    tidToMemoryAccessBlocks.put(rawTrace.getTID(), divideMemoryAccessBlocks(events));
                }
            }

            /* sort lock regions for better performance of constraint solving */
            lockIdToLockRegions.values().forEach(regions -> Collections.sort(regions));
        }
    }

    /**
     * Fast forward the event index to skip recurrent patterns generated by
     * wait-notify or busy-wait loop.
     *
     * @param events
     *            the events array
     * @param size
     *            the number of events in the array
     * @param idx0
     *            the initial index of the first occurrence of the potential
     *            pattern
     * @param idx1
     *            the initial index of the (consecutive) second occurrence of
     *            the potential pattern
     * @param i
     * @return the new event index
     */
    private static int skipRecurrentPatterns(Event[] events, int size, int idx0, int idx1) {
        int len = idx1 - idx0;
        int nextIdx = idx1;
        while (testRecurrentPattern(events, size, idx0, nextIdx, len)) {
            nextIdx += len;
        }
        return nextIdx;
    }

    private static boolean testRecurrentPattern(Event[] events, int size, int idx0, int idx1,
            int len) {
        if (idx1 + len >= size) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!events[idx0 + i].isSimilarTo(events[idx1 + i])) {
                return false;
            }
        }
        return true;
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
                     * global ID and location ID) */
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
