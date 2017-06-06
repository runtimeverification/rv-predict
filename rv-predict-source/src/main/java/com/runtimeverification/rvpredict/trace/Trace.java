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

import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToObjectMap;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import com.runtimeverification.rvpredict.util.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    private final long baseGID;

    private final int size;

    private final List<RawTrace> rawTraces;

    /**
     * Map from an event's ID to its thread information.
     */
    private final Map<Long, Integer> eventIdToTtid;

    /**
     * Map from a thread ID to the information about that thread.
     */
    private final Map<Integer, ThreadInfo> ttidToThreadInfo;

    /**
     * Map from a thread's ID to the internal id assigned when preparing the trace.
     *
     * This is needed because a signal interrupting a thread has the same
     * original thread ID as the interrupted thread.
     */
    private final Map<Long, Integer> originalTidToTraceTid;

    /**
     * Map from thread ID to critical events.
     */
    private final Map<Integer, List<ReadonlyEventInterface>> tidToEvents;

    /**
     * Map from thread ID to critical memory access events grouped into blocks.
     */
    private final Map<Integer, List<MemoryAccessBlock>> tidToMemoryAccessBlocks;

    /**
     * The initial states for all threads referenced in this trace segment.
     * It is computed as the value in the {@link #state} before the first
     * event of that thread occurs in this trace segment.
     */
    private final Map<Integer, ThreadState> tidToThreadState;

    /**
     * Map from memory addresses referenced in this trace segment to their states.
     */
    private final MemoryAddrToStateMap addrToState;

    /**
     * Map from (thread ID, memory address) to write events.
     */
    private final Table<Integer, Long, List<ReadonlyEventInterface>>
            ttidToAddrToWriteEvents;

    /**
     * Map from lock ID to critical lock pairs.
     */
    private final Map<Long, List<LockRegion>> lockIdToLockRegions;

    /**
     * Set of {@code MemoryAccessEvent}'s that happen during class initialization.
     */
    private final Set<ReadonlyEventInterface> clinitEvents;

    /**
     * Maintains the current values for every location, as recorded into the trace
     */
    private final TraceState state;

    public Trace(TraceState state, List<RawTrace> rawTraces,
            Map<Long, Integer> eventIdToTtid,
            Map<Integer, ThreadInfo> ttidToThreadInfo,
            Map<Integer, List<ReadonlyEventInterface>> tidToEvents,
            Map<Integer, List<MemoryAccessBlock>> tidToMemoryAccessBlocks,
            Map<Integer, ThreadState> tidToThreadState,
            MemoryAddrToStateMap addrToState,
            Table<Integer, Long, List<ReadonlyEventInterface>> tidToAddrToEvents,
            Map<Long, List<LockRegion>> lockIdToLockRegions,
            Set<ReadonlyEventInterface> clinitEvents,
            Map<Long, Integer> originalTidToTraceTid) {
        this.state = state;
        this.rawTraces = rawTraces;
        this.eventIdToTtid = eventIdToTtid;
        this.ttidToThreadInfo = ttidToThreadInfo;
        this.tidToEvents = tidToEvents;
        this.tidToMemoryAccessBlocks = tidToMemoryAccessBlocks;
        this.tidToThreadState = tidToThreadState;
        this.addrToState = addrToState;
        this.ttidToAddrToWriteEvents = tidToAddrToEvents;
        this.lockIdToLockRegions = lockIdToLockRegions;
        this.clinitEvents = clinitEvents;
        this.originalTidToTraceTid = originalTidToTraceTid;

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
        this.size = tidToEvents.values().stream().collect(Collectors.summingInt(List::size));
        if (rawTraces.size() != rawTraces.stream().map(rawTrace -> rawTrace.getThreadInfo().getId())
                .collect(Collectors.toSet()).size()) {
            throw new IllegalStateException();
        }
    }

    public MetadataInterface metadata() {
        return state.metadata();
    }

    public Logger logger() {
        return state.config().logger();
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
        return !tidToEvents.isEmpty();
    }

    public ReadonlyEventInterface getFirstEvent(int ttid) {
        List<ReadonlyEventInterface> events = tidToEvents.get(ttid);
        return events == null ? null : events.get(0);
    }

    public ReadonlyEventInterface getLastEvent(int ttid) {
        List<ReadonlyEventInterface> events = tidToEvents.get(ttid);
        return events == null ? null : events.get(events.size() - 1);
    }

    public List<ReadonlyEventInterface> getEvents(int ttid) {
        return tidToEvents.getOrDefault(ttid, Collections.emptyList());
    }

    public Map<Integer, List<ReadonlyEventInterface>> eventsByThreadID() {
        return tidToEvents;
    }

    public Map<Integer, List<MemoryAccessBlock>> memoryAccessBlocksByThreadID() {
        return tidToMemoryAccessBlocks;
    }

    /**
     * Returns the {@link MemoryAccessBlock} that {@code event} belongs to.
     */
    public MemoryAccessBlock getMemoryAccessBlock(ReadonlyEventInterface event) {
        List<MemoryAccessBlock> l = tidToMemoryAccessBlocks.get(getTraceThreadId(event));
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

    public int getTraceThreadId(ReadonlyEventInterface event) {
        return eventIdToTtid.get(event.getEventId());
    }

    public ThreadType getThreadType(ReadonlyEventInterface event) {
        return ttidToThreadInfo.get(eventIdToTtid.get(event.getEventId())).getThreadType();
    }

    public ThreadType getThreadType(Integer traceThreadId) {
        return ttidToThreadInfo.get(traceThreadId).getThreadType();
    }

    public int getSignalDepth(Integer traceThreadId) {
        return ttidToThreadInfo.get(traceThreadId).getSignalDepth();
    }

    public long getSignalNumber(Integer traceThreadId) {
        return ttidToThreadInfo.get(traceThreadId).getSignalNumber();
    }

    public long getOriginalThreadIdForTraceThreadId(Integer traceThreadId) {
        return ttidToThreadInfo.get(traceThreadId).getOriginalThreadId();
    }

    public List<ReadonlyEventInterface> getInterThreadSyncEvents() {
        List<ReadonlyEventInterface> events = new ArrayList<>();
        tidToEvents.values().forEach(l -> {
            l.stream().filter(e -> e.isStart() || e.isJoin()).forEach(events::add);
        });
        return events;
    }

    public Iterable<ReadonlyEventInterface> getWriteEvents(Long addr) {
        return Iterables.concat(ttidToAddrToWriteEvents.column(addr).values());
    }

    private ReadonlyEventInterface getPrevWrite(long gid, int ttid, Long addr) {
        List<ReadonlyEventInterface> list = ttidToAddrToWriteEvents.get(ttid, addr);
        if (list == null || list.isEmpty() || list.get(0).getEventId() >= gid) {
            return null;
        }

        /* binary-searching the latest write before gid */
        ReadonlyEventInterface e = null;
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            ReadonlyEventInterface midVal = list.get(mid);
            int cmp = Long.compare(midVal.getEventId(), gid);

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

    public ReadonlyEventInterface getSameThreadPrevWrite(ReadonlyEventInterface read) {
        return getPrevWrite(read.getEventId(), getTraceThreadId(read), read.getDataInternalIdentifier());
    }

    public ReadonlyEventInterface getAllThreadsPrevWrite(ReadonlyEventInterface read) {
        ReadonlyEventInterface prevWrite = null;
        for (int ttid : ttidToAddrToWriteEvents.rowKeySet()) {
           ReadonlyEventInterface e = getPrevWrite(read.getEventId(), ttid, read.getDataInternalIdentifier());
           if (prevWrite == null || e != null && e.getEventId() < prevWrite.getEventId()) {
               prevWrite = e;
           }
        }
        return prevWrite;
    }

    public Map<Long, List<LockRegion>> getLockIdToLockRegions() {
        return lockIdToLockRegions;
    }

    public boolean isInsideClassInitializer(ReadonlyEventInterface event) {
        return clinitEvents.contains(event);
    }

    /**
     * Returns the {@code String} representation of the stack trace at the point
     * when a given {@code event} happened.
     *
     * @param event
     *            the event
     * @return a {@code Deque} of call stack events
     */
    public Deque<ReadonlyEventInterface> getStacktraceAt(ReadonlyEventInterface event) {
        int ttid = getTraceThreadId(event);
        long gid = event.getEventId();
        Deque<ReadonlyEventInterface> stacktrace = new ArrayDeque<>();
        if (!state.config().stacks()) {
            stacktrace.add(event);
        } else if (gid >= baseGID) {
            /* event is in the current window; reassemble its stack trace */
            tidToThreadState.getOrDefault(ttid, new ThreadState()).getStacktrace()
                    .forEach(stacktrace::addFirst);
            RawTrace t = rawTraces.stream().filter(p -> p.getThreadInfo().getId() == ttid).findAny().get();
            for (int i = 0; i < t.size(); i++) {
                ReadonlyEventInterface e = t.event(i);
                if (e.getEventId() > gid) break;
                if (e.getType() == EventType.INVOKE_METHOD) {
                    stacktrace.addFirst(e);
                } else if (e.getType() == EventType.FINISH_METHOD) {
                    stacktrace.removeFirst();
                }
            }
            stacktrace.addFirst(event);
        } else {
            /* event is from previous windows */
            stacktrace.add(event);
            stacktrace.add(new Event(0, 0, -1, 0, 0, EventType.INVOKE_METHOD));
        }
        return stacktrace;
    }

    /**
     * Returns the locks held by the owner thread when a given {@code ReadonlyEventInterface} occurs.
     */
    public List<ReadonlyEventInterface> getHeldLocksAt(ReadonlyEventInterface event) {
        int ttid = getTraceThreadId(event);
        Map<Long, LockState> lockIdToLockState = tidToThreadState
                .computeIfAbsent(ttid, key -> new ThreadState()).getLockStates().stream()
                .collect(Collectors.toMap(LockState::lockId, LockState::copy));
        RawTrace t = rawTraces.stream().filter(p -> p.getThreadInfo().getId() == ttid).findAny().get();
        for (int i = 0; i < t.size(); i++) {
            ReadonlyEventInterface e = t.event(i);
            if (e.getEventId() >= event.getEventId()) break;
            if (e.isLock() && !e.isWaitAcq()) {
                lockIdToLockState.computeIfAbsent(e.getLockId(), LockState::new)
                        .acquire(e);
            } else if (e.isUnlock() && !e.isWaitRel()) {
                lockIdToLockState.get(e.getLockId()).release(e);
            }
        }

        List<ReadonlyEventInterface> lockEvents = lockIdToLockState.values().stream()
                .filter(LockState::isAcquired)
                .map(LockState::lock).collect(Collectors.toList());
        Collections.sort(lockEvents);
        return lockEvents;
    }

    private void processEvents() {
        if (rawTraces.size() == 1) {
            state.fastProcess(rawTraces.iterator().next());
            return;
        }

        /// PHASE 1
        Set<ReadonlyEventInterface> outermostLockEvents = new HashSet<>();
        for (RawTrace rawTrace : rawTraces) {
            ThreadInfo threadInfo = rawTrace.getThreadInfo();
            int ttid = threadInfo.getId();
            ttidToThreadInfo.put(ttid, threadInfo);
            if (threadInfo.getThreadType() == ThreadType.THREAD && rawTrace.size() > 0) {
                ReadonlyEventInterface event = rawTrace.event(0);
                originalTidToTraceTid.put(event.getOriginalThreadId(), ttid);
            }
            tidToThreadState.put(ttid, state.getThreadStateSnapshot(ttid));
            boolean isInsideClinit = state.isInsideClassInitializer(ttid);

            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (isInsideClinit) {
                    clinitEvents.add(event);
                }

                if (event.isReadOrWrite()) {
                    /* update memory address state */
                    MemoryAddrState st = addrToState.computeIfAbsent(event.getDataInternalIdentifier());
                    st.touch(event, ttid);
                } else if (event.isSyncEvent()) {
                    if (event.isLock()) {
                        event = state.updateLockLocToUserLoc(event, ttid);
                        if (event.isWaitAcq()) {
                            outermostLockEvents.add(event);
                        } else if (state.acquireLock(event, ttid) == 1) {
                            outermostLockEvents.add(event);
                        }
                    } else if (event.isUnlock()) {
                        if (event.isWaitRel()) {
                            // a WAIT_RELEASE event can be matched with one or more
                            // lock events because locks can be reentrant
                            outermostLockEvents.add(event);
                        } else if (state.releaseLock(event, ttid) == 0) {
                            outermostLockEvents.add(event);
                        }
                    } else if (event.isStart()) {
                        state.updateThreadLocToUserLoc(event, ttid);
                    }
                } else if (event.isMetaEvent()) {
                    state.onMetaEvent(event, ttid);
                    EventType type = event.getType();
                    if (type == EventType.CLINIT_ENTER) {
                        isInsideClinit = true;
                    } else if (type == EventType.CLINIT_EXIT) {
                        isInsideClinit = state.isInsideClassInitializer(ttid);
                    }
                } else if (event.isSignalEvent()) {
                    // Do nothing for now.
                } else {
		    if (Configuration.debug)
		        System.err.println(event.getType());
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
                int ttid = rawTrace.getThreadInfo().getId();
                /* step 1: remove thread-local events and nested lock events */
                int tmp_size = 0;
                ReadonlyEventInterface[] tmp_events = new ReadonlyEventInterface[rawTrace.size()];
                for (int i = 0; i < rawTrace.size(); i++) {
                    ReadonlyEventInterface event = rawTrace.event(i);
                    if (event.isReadOrWrite()) {
                        if (sharedAddr.contains(event.getDataInternalIdentifier())) {
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
                    } else if (event.isSignalEvent()) {
                        tmp_events[tmp_size++] = event;
                    } else {
                        // MetaEvents are thrown away
                    }
                }

                /* step 2: remove recurrent patterns and empty lock regions */
                MemoryAddrToObjectMap<Integer> addrToLastReadIdx = new MemoryAddrToObjectMap<>(
                        sharedAddr.size());
                Map<Long, Integer> lockIdToLastLockIdx = new HashMap<>();
                Map<Long, Integer> lockIdToOpenReadLockIdx = new HashMap<>();
                Map<Long, Integer> lockIdToOpenWriteLockIdx = new HashMap<>();
                Set<Integer> pendingLockIndexes = new HashSet<>();
                boolean[] critical = new boolean[tmp_size];
                int numOfCriticalEvents = 0;
                for (int i = 0; i < tmp_size; i++) {
                    ReadonlyEventInterface event = tmp_events[i];
                    if (event.isRead()) {
                        Integer lastReadIdx = addrToLastReadIdx.put(event.getDataInternalIdentifier(), i);
                        if (lastReadIdx != null) {
                            /* attempts to skip recurrent pattern */
                            int nextIdx = skipRecurrentPatterns(tmp_events, tmp_size, lastReadIdx, i);
                            if (nextIdx != i) {
                                i = nextIdx - 1;
                                continue;
                            }
                        }
                        critical[i] = true;
                    } else if (event.isWrite()) {
                        critical[i] = true;
                    } else if (event.isLock()) {
                        /* attempts to skip recurrent pattern */
                        Integer lastLockIdx = lockIdToLastLockIdx.put(event.getLockId(), i);
                        if (lastLockIdx != null) {
                            int nextIdx = skipRecurrentPatterns(tmp_events, tmp_size, lastLockIdx, i);
                            if (nextIdx != i) {
                                i = nextIdx - 1;
                                continue;
                            }
                        }
                        /* whether a lock event is critical cannot be determined immediately */
                        (event.isReadLock() ? lockIdToOpenReadLockIdx : lockIdToOpenWriteLockIdx)
                                .put(event.getLockId(), i);
                        pendingLockIndexes.add(i);
                    } else if (event.isUnlock()) {
                        Integer idx = (event.isReadUnlock() ?
                                lockIdToOpenReadLockIdx : lockIdToOpenWriteLockIdx)
                                .remove(event.getLockId());
                        pendingLockIndexes.remove(idx);

                        critical[i] = idx == null ? numOfCriticalEvents > 0 : critical[idx];
                        if (critical[i]) {
                            lockIdToLockRegions
                                .computeIfAbsent(event.getLockId(), p -> new ArrayList<>())
                                .add(new LockRegion(idx == null ? null : tmp_events[idx], event, ttid));
                        }
                    } else {
                        critical[i] = true;
                    }

                    if (critical[i]) {
                        numOfCriticalEvents++;
                        numOfCriticalEvents += pendingLockIndexes.size();
                        pendingLockIndexes.forEach(idx -> critical[idx] = true);
                        pendingLockIndexes.clear();
                    }
                }
                Iterables.concat(lockIdToOpenReadLockIdx.values(),
                        lockIdToOpenWriteLockIdx.values()).forEach(idx -> {
                   if (critical[idx]) {
                       lockIdToLockRegions
                           .computeIfAbsent(tmp_events[idx].getLockId(), p -> new ArrayList<>())
                           .add(new LockRegion(tmp_events[idx], null, ttid));
                   }
                });

                /* commit all critical events into this window */
                ReadonlyEventInterface[] events = new ReadonlyEventInterface[numOfCriticalEvents];
                for (int i = 0, c = 0; i < tmp_size; i++) {
                    if (critical[i]) {
                        ReadonlyEventInterface event = tmp_events[i];
//                        logger().debug(event + " at " + metadata().getLocationSig(event.getLocationId()));

                        /* update tidToEvents & tidToAddrToWriteEvents */
                        events[c++] = event;
                        eventIdToTtid.put(event.getEventId(), ttid);
                        if (event.isWrite()) {
                            ttidToAddrToWriteEvents.row(ttid)
                                    .computeIfAbsent(event.getDataInternalIdentifier(), p -> new ArrayList<>())
                                    .add(event);
                        }
                    }
                }
                if (numOfCriticalEvents > 0) {
                    List<ReadonlyEventInterface> list = Arrays.asList(events);
                    tidToEvents.put(ttid, list);
                    tidToMemoryAccessBlocks.put(ttid, divideMemoryAccessBlocks(list));
                }
            }

            /* sort lock regions for better performance of constraint solving */
            lockIdToLockRegions.values().forEach(regions -> Collections.sort(regions));
        }

        /* debugging code: print out events in order */
//        printEvents();
    }

    /**
     * Fast forward the event index to skip recurrent patterns generated by
     * wait-notify or busy-wait loop.
     * <p>
     * TODO(YilongL): formal proof of this optimization
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
     * @return the new event index
     */
    private static int skipRecurrentPatterns(ReadonlyEventInterface[] events, int size, int idx0, int idx1) {
        int len = idx1 - idx0;
        int nextIdx = idx1;
        while (testRecurrentPattern(events, size, idx0, nextIdx, len)) {
            nextIdx += len;
        }
        return nextIdx;
    }

    private static boolean testRecurrentPattern(ReadonlyEventInterface[] events, int size, int idx0, int idx1,
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

    private List<MemoryAccessBlock> divideMemoryAccessBlocks(List<ReadonlyEventInterface> events) {
        List<MemoryAccessBlock> blocks = new ArrayList<>();
        MemoryAccessBlock lastBlock = null;
        List<ReadonlyEventInterface> crntBlock = new ArrayList<>();
        ReadonlyEventInterface lastEvent = null;
        for (ReadonlyEventInterface event : events) {
            /* update memory access blocks */
            boolean endCrntBlock;
            if (event.isSyncEvent()) {
                endCrntBlock = true;
            } else if (event.isReadOrWrite()) {
                if (event.isRead()) {
                    /* Do not end the block if the previous event is a write or
                     * a duplicate read (i.e., a read event that differs only in
                     * global ID and location ID).
                     */
                    if (lastEvent != null) {
                        boolean readsTheSameThing = lastEvent.isRead()
                                && lastEvent.getDataInternalIdentifier() == event.getDataInternalIdentifier()
                                && lastEvent.getDataValue() == event.getDataValue();
                        endCrntBlock = !(lastEvent.isWrite() || readsTheSameThing);
                    } else {
                        endCrntBlock = false;
                    }
                } else {
                    endCrntBlock = lastEvent != null && lastEvent.isRead();
                }
            } else if (event.isSignalEvent()) {
                // Do nothing for now, since signal events themselves are not involved with r/w.
                endCrntBlock = false;
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

    /**
     * Useful for debugging.
     */
    public void printEvents() {
        tidToEvents.values().stream().flatMap(List::stream).sorted().forEach(event -> logger()
                .debug((event + " at " + metadata().getLocationSig(event.getLocationId()))));
    }

    public Integer getMainTraceThreadForOriginalThread(long originalThreadId) {
        return originalTidToTraceTid.get(originalThreadId);
    }
}
