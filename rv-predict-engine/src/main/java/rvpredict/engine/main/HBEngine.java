package rvpredict.engine.main;

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
import rvpredict.trace.AbstractEvent;
import rvpredict.trace.Event;
import rvpredict.trace.EventType;
import rvpredict.trace.SyncEvent;
import rvpredict.trace.LockPair;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.WriteEvent;
import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.List;

/**
 * The engine class for happens-before (HB) based race detection. It maintains
 * the HB edge between events and answers reachability inquiries between
 * conflicting events.
 *
 */
public class HBEngine {

    private final ReachabilityEngine reachEngine = new ReachabilityEngine();

    private final LockSetEngine lockEngine = new LockSetEngine();// construct a new
                                                           // lockset for this
                                                           // segment

    public HBEngine(Trace trace) {
        initIntraThreadHBEdges(trace.getAllThreadEvents());

        // TODO: ensure lockset algorithm??
        initInterThreadHBEdges(trace);
    }

    /**
     * Adds program order HB edges.
     */
    private void initIntraThreadHBEdges(Collection<List<Event>> allThreadEvents) {
        for (List<Event> events : allThreadEvents) {
            long prevGID = -1;
            for (Event e : events) {
                if (prevGID != -1) {
                    reachEngine.addEdge(prevGID, e.getGID());
                }
                prevGID = e.getGID();
            }
        }
    }

    /**
     * add inter-thread HB edges. HB edges for fork/begin, end/join, and lock
     * regions are simply added as it is. To ensure the soundness for all
     * detected races, we also include the HB edges for write/write, write/read,
     * read/write.
     *
     * TODO: need to distinguish reads and writes for checking conflicting lock
     * regions
     */
    private void initInterThreadHBEdges(Trace trace) {
        Map<String, Long> addrToLastWrtGID = new HashMap<>();

        Map<Long, List<LockPair>> lockAddrNodes = new HashMap<>();
        Map<Long, Stack<SyncEvent>> threadIdToLockStack = new HashMap<>();

        SyncEvent matchNotifyNode = null;

        // during recording
        // should after wait, before notify
        // after lock, before unlock
        List<Event> nodes = trace.getFullTrace();
        for (Event crntEvent : nodes) {
            long crntGID = crntEvent.getGID();
            long crntTID = crntEvent.getTID();

            // add first node
            switch (crntEvent.getType()) {
            case START: {
                long tid = ((SyncEvent) crntEvent).getSyncObject();
                Event fstEvent = trace.getFirstEvent(tid);
                if (fstEvent != null) {
                    reachEngine.addEdge(crntGID, fstEvent.getGID());
                }
                break;
            }
            case JOIN: {
                long tid = ((SyncEvent) crntEvent).getSyncObject();
                Event lstEvent = trace.getLastEvent(tid);
                if (lstEvent != null) {
                    reachEngine.addEdge(lstEvent.getGID(), crntGID);
                }
                break;
            }
            case READ: {
                String addr = ((ReadEvent) crntEvent).getAddr();
                Long lastWrtGID = addrToLastWrtGID.get(addr);
                if (lastWrtGID != null) {
                    reachEngine.addEdge(lastWrtGID, crntGID);
                }
                break;
            }
            case WRITE: {
                String addr = ((WriteEvent) crntEvent).getAddr();
                Long lastWrtGID = addrToLastWrtGID.get(addr);
                if (lastWrtGID != null) {
                    reachEngine.addEdge(lastWrtGID, crntGID);
                }
                addrToLastWrtGID.put(addr, ((WriteEvent) crntEvent).getGID());
                break;
            }
            case LOCK: {
                Stack<SyncEvent> lockStack = threadIdToLockStack.get(crntTID);
                if (lockStack == null) {
                    lockStack = new Stack<>();
                    threadIdToLockStack.put(crntTID, lockStack);
                }
                lockStack.push(((SyncEvent) crntEvent));
                break;
            }
            case UNLOCK: {
                Stack<SyncEvent> syncstack = threadIdToLockStack.get(crntTID);

                if (syncstack == null) {
                    syncstack = new Stack<SyncEvent>();
                    threadIdToLockStack.put(crntTID, syncstack);
                }
                LockPair lp = null;
                if (syncstack.isEmpty()) {
                    // lp = new LockPair(null,(ISyncNode)node);
                    // make it non-null
                    AbstractEvent firstnode = trace.getThreadFirstNodeMap().get(crntTID);
                    long fake_gid = firstnode.getGID();
                    SyncEvent fake_node = new SyncEvent(fake_gid, crntTID, firstnode.getID(), EventType.LOCK,
                            ((SyncEvent) crntEvent).getSyncObject());
                    lp = new LockPair(fake_node, (SyncEvent) crntEvent);
                } else {
                    lp = new LockPair(syncstack.pop(), (SyncEvent) crntEvent);

                    // filter out re-entrant locks
                    if (syncstack.size() > 0)
                        if (((SyncEvent) crntEvent).getSyncObject() == syncstack.get(0).getSyncObject()) {
                            continue;
                        }
                }

                long addr = ((SyncEvent) crntEvent).getSyncObject();

                List<LockPair> syncNodeList = lockAddrNodes.get(addr);
                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);

                }

                syncNodeList.add(lp);
                lockEngine.add(((SyncEvent) crntEvent).getSyncObject(), crntTID, lp);
                break;
            }
            case WAIT: {
                // assert(matchNotifyNode!=null);this is also possible when
                // segmented
                if (matchNotifyNode != null) {
                    long notifyGID = matchNotifyNode.getGID();

                    int nodeIndex = trace.getFullTrace().indexOf(crntEvent) + 1;

                    try {
                        // TODO: handle OutofBounds
                        try {
                            while (trace.getFullTrace().get(nodeIndex).getTID() != crntTID)
                                nodeIndex++;
                        } catch (Exception e) {
                            // if we arrive here, it means the wait node is the
                            // last node of the corresponding thread
                            // so add an order from notify to wait instead
                            nodeIndex = trace.getFullTrace().indexOf(crntEvent);
                        }
                        long waitNextGID = trace.getFullTrace().get(nodeIndex).getGID();

                        reachEngine.addEdge(notifyGID, waitNextGID);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // clear notifyNode
                    matchNotifyNode = null;
                }

                Stack<SyncEvent> syncstack = threadIdToLockStack.get(crntTID);
                // assert(stack.size()>0);
                if (syncstack == null) {
                    syncstack = new Stack<SyncEvent>();
                    threadIdToLockStack.put(crntTID, syncstack);
                }
                LockPair lp = null;
                if (syncstack.isEmpty()) {
                    // lp = new LockPair(null,((WaitNode) node));

                    AbstractEvent firstnode = trace.getThreadFirstNodeMap().get(crntTID);
                    long fake_gid = firstnode.getGID();
                    SyncEvent fake_node = new SyncEvent(fake_gid, crntTID, firstnode.getID(), EventType.LOCK,
                            ((SyncEvent) crntEvent).getSyncObject());
                    lp = new LockPair(fake_node, (SyncEvent) crntEvent);
                } else
                    lp = new LockPair(syncstack.pop(), ((SyncEvent) crntEvent));

                long addr = ((SyncEvent) crntEvent).getSyncObject();

                List<LockPair> syncNodeList = lockAddrNodes.get(addr);
                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);

                }

                syncNodeList.add(lp);
                lockEngine.add(((SyncEvent) crntEvent).getSyncObject(), crntTID, lp);

                syncstack.push(((SyncEvent) crntEvent));
                break;
            }
            case NOTIFY: {
                matchNotifyNode = (SyncEvent) crntEvent;
                break;
            }
            default:
                break;
            }
        }

        // process the last lock node
        Iterator<Long> tidIt = threadIdToLockStack.keySet().iterator();
        while (tidIt.hasNext()) {
            Long tid = tidIt.next();

            Stack<SyncEvent> stack = threadIdToLockStack.get(tid);

            AbstractEvent lastnode = trace.getThreadLastNodeMap().get(tid);
            long fake_gid = lastnode.getGID();

            while (stack.size() > 0) {
                SyncEvent node = stack.remove(0);
                SyncEvent fake_node = new SyncEvent(fake_gid, tid, lastnode.getID(), EventType.UNLOCK,
                        node.getSyncObject());

                LockPair lp = new LockPair(node, fake_node);

                long addr = node.getSyncObject();

                List<LockPair> syncNodeList = lockAddrNodes.get(addr);

                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);
                }
                syncNodeList.add(lp);
                lockEngine.add(node.getSyncObject(), tid, lp);

            }
        }

        // add HB edge between lock regions

        Iterator<Long> addrIt = lockAddrNodes.keySet().iterator();
        while (addrIt.hasNext()) {
            long addr = addrIt.next();

            List<LockPair> syncNodeList = lockAddrNodes.get(addr);
            for (int k = 0; k < syncNodeList.size() - 1; k++) {
                LockPair lp = syncNodeList.get(k);
                LockPair lp2 = syncNodeList.get(k + 1);
                if (lp.unlock == null || lp2.lock == null) {
                    // how come this is true!!!
                } else {
                    long gid1 = lp.unlock.getGID();
                    long gid2 = lp2.lock.getGID();
                    reachEngine.addEdge(gid1, gid2);
                }
            }
        }

    }

    /**
     * return true if node1 and node2 have no common lock, and are not reachable
     * by all the indirect HB edges excluding the possible direct HB edge
     * between node1 and node2
     *
     * @param event1
     * @param event2
     * @return
     */
    public boolean isRace(Event event1, Event event2) {
        long gid1 = event1.getGID();
        long gid2 = event2.getGID();

        // lockset algorithm
        if (lockEngine.hasCommonLock(event1.getTID(), gid1, event2.getTID(), gid2)) {
            return false;
        }

        if (gid1 > gid2) {
            long t = gid1;
            gid1 = gid2;
            gid2 = t;
        }

        // exclude this edge first
        boolean e12 = reachEngine.deleteEdge(gid1, gid2);
        boolean e21 = reachEngine.deleteEdge(gid2, gid1);

        boolean race = !reachEngine.canReach(gid1, gid2) && !reachEngine.canReach(gid2, gid1);

        // add back
        if (e12)
            reachEngine.addEdge(gid1, gid2);
        if (e21)
            reachEngine.addEdge(gid2, gid1);

        return race;
    }
}
