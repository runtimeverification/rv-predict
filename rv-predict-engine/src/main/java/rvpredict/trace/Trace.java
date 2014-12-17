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
package rvpredict.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 */
public class Trace {

    /**
     * Unprocessed raw events reading from the logging phase.
     */
    private final List<Event> rawEvents = new ArrayList<>();

    /**
     * Read threads on each address.
     */
    private final Map<String, Set<Long>> addrToReadThreads = new HashMap<>();

    /**
     * Write threads on each address.
     */
    private final Map<String, Set<Long>> addrToWriteThreads = new HashMap<>();

    /**
     * Shared memory locations.
     */
    private final Set<String> sharedMemAddr = new HashSet<>();

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
     * Start/Join events indexed by the ID of its owner Thread object.
     */
    private final Map<Long, List<SyncEvent>> threadIdToStartJoinEvents = new HashMap<>();

    /**
     * Wait/Notify/Lock/Unlock events indexed by the involved intrinsic lock object.
     */
    private final Map<Long, List<SyncEvent>> lockObjToSyncEvents = new HashMap<>();

    /**
     * Read events on each address.
     */
    private final Map<String, List<ReadEvent>> addrToReadEvents = new HashMap<>();

    /**
     * Write events on each address.
     */
    private final Map<String, List<WriteEvent>> addrToWriteEvents = new HashMap<>();

    /**
     * Lists of {@code MemoryAccessEvent}'s indexed by address and thread ID.
     */
    private final Table<String, Long, List<MemoryAccessEvent>> memAccessEventsTbl = HashBasedTable.create();

    /**
     * Initial value of each address.
     */
    private final Map<String, Long> initValues;

    private List<ReadEvent> allReadNodes;

    private final TraceInfo info;

    public Trace(Map<String, Long> initValues, TraceInfo info) {
        assert initValues != null && info != null;
        this.initValues = initValues;
        this.info = info;
    }

    public boolean hasSharedMemAddr() {
        return !sharedMemAddr.isEmpty();
    }

    public List<Event> getAllEvents() {
        return allEvents;
    }

    public Long getInitValueOf(String addr) {
        Long initValue = initValues.get(addr);
        // TODO(YilongL): assuming that every variable is initialized is very Java-specific
        return initValue == null ? 0 : initValue;
    }

    public Map<Integer, String> getLocIdToStmtSigMap() {
        return info.getLocIdToStmtSigMap();
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
        return lockObjToSyncEvents;
    }

    public List<ReadEvent> getReadEventsOn(String addr) {
        List<ReadEvent> events = addrToReadEvents.get(addr);
        return events == null ? Lists.<ReadEvent>newArrayList() : events;
    }

    public List<WriteEvent> getWriteEventsOn(String addr) {
        List<WriteEvent> events = addrToWriteEvents.get(addr);
        return events == null ? Lists.<WriteEvent>newArrayList() : events;
    }

    public Table<String, Long, List<MemoryAccessEvent>> getMemAccessEventsTable() {
        return memAccessEventsTbl;
    }

    public Map<String, Long> getFinalValues() {
        Map<String, Long> finalValues = new HashMap<>(initValues);
        for (String addr : addrToWriteEvents.keySet()) {
            List<WriteEvent> writeEvents = addrToWriteEvents.get(addr);
            finalValues.put(addr, writeEvents.get(writeEvents.size() - 1).getValue());
        }
        return finalValues;
    }

    public List<ReadEvent> getAllReadNodes() {
        if (allReadNodes == null) {
            allReadNodes = new ArrayList<>();
            Iterator<List<ReadEvent>> it = addrToReadEvents.values().iterator();
            while (it.hasNext()) {
                allReadNodes.addAll(it.next());
            }
        }

        return allReadNodes;
    }

    /**
     * Gets control-flow dependent events of a given {@code MemoryAccessEvent}.
     * Without logging {@code BranchNode}, all read events that happen-before
     * the given event have to be included conservatively. Otherwise, only the
     * read events that happen-before the latest branch event are included.
     */
    // TODO: NEED to include the dependent nodes from other threads
    public List<ReadEvent> getCtrlFlowDependentEvents(MemoryAccessEvent memAccEvent) {
        // TODO(YilongL): optimize this method when it becomes a bottleneck
        List<ReadEvent> readEvents = new ArrayList<>();

        long threadId = memAccEvent.getTID();

        BranchEvent prevBranchEvent = null;
        for (BranchEvent branchEvent : getThreadBranchEvents(threadId)) {
            if (branchEvent.getGID() < memAccEvent.getGID()) {
                prevBranchEvent = branchEvent;
            } else {
                break;
            }
        }

        Event event = prevBranchEvent == null ? memAccEvent : prevBranchEvent;
        for (Event e : getThreadEvents(threadId)) {
            if (e.getGID() >= event.getGID()) {
                break;
            }

            if (e instanceof ReadEvent) {
                readEvents.add((ReadEvent) e);
            }
        }

        return readEvents;
    }

    public void addRawEvent(Event node) {
        rawEvents.add(node);
        if (node instanceof MemoryAccessEvent) {
            String addr = ((MemoryAccessEvent) node).getAddr();
            Long tid = node.getTID();

            if (node instanceof ReadEvent) {
                Set<Long> set = addrToReadThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    addrToReadThreads.put(addr, set);
                }
                set.add(tid);
            } else {
                Set<Long> set = addrToWriteThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    addrToWriteThreads.put(addr, set);
                }
                set.add(tid);
            }
        }
    }

    /**
     * add a new filtered event to the trace in the order of its appearance
     *
     * @param node
     */
    private void addEvent(Event node) {
        Long tid = node.getTID();
        threadIds.add(tid);

        if (node instanceof BranchEvent) {
            // branch node
            info.incrementBranchNumber();

            List<BranchEvent> branchnodes = threadIdToBranchEvents.get(tid);
            if (branchnodes == null) {
                branchnodes = new ArrayList<>();
                threadIdToBranchEvents.put(tid, branchnodes);
            }
            branchnodes.add((BranchEvent) node);
        } else if (node instanceof InitEvent) {
            // initial write node
            initValues.put(((InitEvent) node).getAddr(), ((InitEvent) node).getValue());
            info.incrementInitWriteNumber();
        } else {
            // all critical nodes -- read/write/synchronization events

            allEvents.add(node);

            List<Event> threadNodes = threadIdToEvents.get(tid);
            if (threadNodes == null) {
                threadNodes = new ArrayList<>();
                threadIdToEvents.put(tid, threadNodes);
            }

            threadNodes.add(node);
            // TODO: Optimize it -- no need to update it every time
            if (node instanceof MemoryAccessEvent) {
                info.incrementSharedReadWriteNumber();

                MemoryAccessEvent mnode = (MemoryAccessEvent) node;
                String addr = mnode.getAddr();

                List<MemoryAccessEvent> memAccessEvents = memAccessEventsTbl.get(addr, tid);
                if (memAccessEvents == null) {
                    memAccessEvents = Lists.newArrayList();
                    memAccessEventsTbl.put(addr, tid, memAccessEvents);
                }
                memAccessEvents.add(mnode);

                if (node instanceof ReadEvent) {
                    List<ReadEvent> readNodes = addrToReadEvents.get(addr);
                    if (readNodes == null) {
                        readNodes = new ArrayList<>();
                        addrToReadEvents.put(addr, readNodes);
                    }
                    readNodes.add((ReadEvent) node);

                } else {
                    List<WriteEvent> writeNodes = addrToWriteEvents.get(addr);
                    if (writeNodes == null) {
                        writeNodes = new ArrayList<>();
                        addrToWriteEvents.put(addr, writeNodes);
                    }
                    writeNodes.add((WriteEvent) node);
                }
            } else if (node instanceof SyncEvent) {
                // synchronization nodes
                info.incrementSyncNumber();
                SyncEvent syncEvent = (SyncEvent) node;

                if (syncEvent.getType().equals(EventType.START)
                        || syncEvent.getType().equals(EventType.JOIN)) {
                    long threadObj = syncEvent.getSyncObject();
                    List<SyncEvent> events = threadIdToStartJoinEvents.get(threadObj);
                    if (events == null) {
                        events = Lists.newArrayList();
                        threadIdToStartJoinEvents.put(threadObj, events);
                    }
                    events.add(syncEvent);
                } else {
                    long syncObj = syncEvent.getSyncObject();
                    List<SyncEvent> syncEvents = lockObjToSyncEvents.get(syncObj);
                    if (syncEvents == null) {
                        syncEvents = new ArrayList<>();
                        lockObjToSyncEvents.put(syncObj, syncEvents);
                    }

                    syncEvents.add(syncEvent);
                }
            }
        }
    }

    /**
     * Once trace is completely loaded, remove local data accesses and process
     * the remaining trace.
     */
    public void finishedLoading() {
        for (String addr : Iterables.concat(addrToReadThreads.keySet(),
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

        for (Event event : rawEvents) {
            if (event instanceof MemoryAccessEvent) {
                String addr = ((MemoryAccessEvent) event).getAddr();
                if (sharedMemAddr.contains(addr)) {
                    addEvent(event);
                } else {
                    info.incrementLocalReadWriteNumber();
                }
            } else {
                addEvent(event);
            }
        }

        rawEvents.clear();

        info.addSharedAddresses(sharedMemAddr);
        info.addThreads(threadIds);
    }

    // TODO(YilongL): add javadoc; addr seems to be some abstract address, e.g.
    // "_.1", built when reading the trace; figure out what happens and improve it
    public boolean isVolatileAddr(String addr) {
        // all field addr should contain ".", not true for array access
        int dotPos = addr.indexOf(".");
        return dotPos != -1 && info.isVolatileAddr(Integer.valueOf(addr.substring(dotPos + 1)));
    }

}
