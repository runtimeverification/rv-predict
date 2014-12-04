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
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 *
 * @author jeffhuang
 *
 */
public class Trace {

    // rawfulltrace represents all the raw events in the global order
    private final List<Event> rawfulltrace = new ArrayList<>();

    // indexed by address, the set of read/write threads
    // used to prune away local data accesses
    private final Map<String, Set<Long>> indexedReadThreads = new HashMap<>();
    private final Map<String, Set<Long>> indexedWriteThreads = new HashMap<>();

    // the set of shared memory locations
    private final Set<String> sharedAddresses = new HashSet<>();

    private final Set<Long> threadIds = new HashSet<>();

    // fulltrace represents all the critical events in the global order
    private final List<Event> fulltrace = new ArrayList<>();

    // per thread node map
    private final Map<Long, List<Event>> threadIdToEvents = new HashMap<>();

    // per thread per lock lock/unlock pair
    private final Map<Long, Map<Long, List<LockRegion>>> threadIndexedLockPairs = new HashMap<>();
    private final Map<Long, Stack<SyncEvent>> threadSyncStack = new HashMap<>();

    // per thread branch nodes
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

    /**
     * return true if sharedAddresses is not empty
     *
     * @return
     */
    public boolean mayRace() {
        return !sharedAddresses.isEmpty();
    }

    public List<Event> getFullTrace() {
        return fulltrace;
    }

    public Long getInitValueOf(String addr) {
        return initValues.get(addr);
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
     * Gets dependent nodes of a given {@code MemoryAccessEvent}. Without
     * logging {@code BranchNode}, all read events that happen-before the given
     * event have to be included conservatively. Otherwise, only the read events
     * that happen-before the latest branch event are included.
     */
    // TODO: NEED to include the dependent nodes from other threads
    public List<ReadEvent> getDependentReadEvents(MemoryAccessEvent memAccEvent, boolean branch) {
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
            if (e.getGID() > event.getGID()) {
                break;
            }

            if (e instanceof ReadEvent) {
                readEvents.add((ReadEvent) e);
            }
        }

        return readEvents;
    }

    /**
     * add a new event to the trace in the order of its appearance
     *
     * @param node
     */
    public void addRawNode(AbstractEvent node) {
        rawfulltrace.add(node);
        if (node instanceof MemoryAccessEvent) {
            String addr = ((MemoryAccessEvent) node).getAddr();
            Long tid = node.getTID();

            if (node instanceof ReadEvent) {
                Set<Long> set = indexedReadThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    indexedReadThreads.put(addr, set);
                }
                set.add(tid);
            } else {
                Set<Long> set = indexedWriteThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    indexedWriteThreads.put(addr, set);
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
    private void addNode(Event node) {
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

            fulltrace.add(node);

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

                    if (syncEvent.getType().equals(EventType.LOCK)) {
                        Stack<SyncEvent> stack = threadSyncStack.get(tid);
                        if (stack == null) {
                            stack = new Stack<SyncEvent>();
                            threadSyncStack.put(tid, stack);
                        }

                        stack.push(syncEvent);
                    } else if (syncEvent.getType().equals(EventType.UNLOCK)) {
                        Map<Long, List<LockRegion>> indexedLockpairs = threadIndexedLockPairs
                                .get(tid);
                        if (indexedLockpairs == null) {
                            indexedLockpairs = new HashMap<>();
                            threadIndexedLockPairs.put(tid, indexedLockpairs);
                        }
                        List<LockRegion> lockpairs = indexedLockpairs.get(syncObj);
                        if (lockpairs == null) {
                            lockpairs = new ArrayList<>();
                            indexedLockpairs.put(syncObj, lockpairs);
                        }

                        Stack<SyncEvent> stack = threadSyncStack.get(tid);
                        if (stack == null) {
                            stack = new Stack<SyncEvent>();
                            threadSyncStack.put(tid, stack);
                        }
                        // assert(stack.size()>0); //this is possible when segmented
                        if (stack.size() == 0)
                            lockpairs.add(new LockRegion(null, syncEvent));
                        else if (stack.size() == 1)
                            lockpairs.add(new LockRegion(stack.pop(), syncEvent));
                        else
                            stack.pop();// handle reentrant lock
                    }
                }
            }
        }
    }

    /**
     * once trace is completely loaded, do two things: 1. prune away local data
     * accesses 2. process the remaining trace
     */
    public void finishedLoading() {
        HashSet<String> addrs = new HashSet<String>();
        addrs.addAll(indexedReadThreads.keySet());
        addrs.addAll(indexedWriteThreads.keySet());
        for (Iterator<String> addrIt = addrs.iterator(); addrIt.hasNext();) {
            String addr = addrIt.next();
            Set<Long> wtids = indexedWriteThreads.get(addr);
            if (wtids != null && wtids.size() > 0) {
                if (wtids.size() > 1) {
                    sharedAddresses.add(addr);

                } else {
                    Set<Long> rtids = indexedReadThreads.get(addr);
                    if (rtids != null) {
                        HashSet<Long> set = new HashSet<>(rtids);
                        set.addAll(wtids);
                        if (set.size() > 1)
                            sharedAddresses.add(addr);
                    }
                }
            }
        }

        // add trace
        for (int i = 0; i < rawfulltrace.size(); i++) {
            Event node = rawfulltrace.get(i);
            if (node instanceof MemoryAccessEvent) {
                String addr = ((MemoryAccessEvent) node).getAddr();
                if (sharedAddresses.contains(addr))
                    addNode(node);
                else
                    info.incrementLocalReadWriteNumber();

            } else
                addNode(node);
        }

        // process sync stack to handle windowing
        checkSyncStack();

        // clear rawfulltrace
        rawfulltrace.clear();

        // add info
        info.addSharedAddresses(sharedAddresses);
        info.addThreads(threadIds);

    }

    /**
     * compute the lock/unlock pairs because we analyze the trace window by
     * window, lock/unlock may not be in the same window, so we may have null
     * lock or unlock node in the pair.
     */
    private void checkSyncStack() {
        // check threadSyncStack - only to handle when segmented
        Iterator<Entry<Long, Stack<SyncEvent>>> entryIt = threadSyncStack.entrySet().iterator();
        while (entryIt.hasNext()) {
            Entry<Long, Stack<SyncEvent>> entry = entryIt.next();
            Long tid = entry.getKey();
            Stack<SyncEvent> stack = entry.getValue();

            if (!stack.isEmpty()) {
                Map<Long, List<LockRegion>> indexedLockpairs = threadIndexedLockPairs
                        .get(tid);
                if (indexedLockpairs == null) {
                    indexedLockpairs = new HashMap<>();
                    threadIndexedLockPairs.put(tid, indexedLockpairs);
                }

                while (!stack.isEmpty()) {
                    SyncEvent syncnode = stack.pop();// lock or wait

                    List<LockRegion> lockpairs = indexedLockpairs.get(syncnode.getSyncObject());
                    if (lockpairs == null) {
                        lockpairs = new ArrayList<>();
                        indexedLockpairs.put(syncnode.getSyncObject(), lockpairs);
                    }

                    lockpairs.add(new LockRegion(syncnode, null));
                }
            }
        }
    }

    // TODO(YilongL): add javadoc; addr seems to be some abstract address, e.g.
    // "_.1", built when reading the trace; figure out what happens and improve it
    public boolean isVolatileAddr(String addr) {
        // all field addr should contain ".", not true for array access
        int dotPos = addr.indexOf(".");
        return dotPos != -1 && info.isVolatileAddr(Integer.valueOf(addr.substring(dotPos + 1)));
    }

}
