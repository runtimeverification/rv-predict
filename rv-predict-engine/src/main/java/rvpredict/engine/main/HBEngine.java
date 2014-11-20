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
import rvpredict.trace.SyncEvent;
import rvpredict.trace.JoinNode;
import rvpredict.trace.LockNode;
import rvpredict.trace.LockPair;
import rvpredict.trace.NotifyNode;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.StartNode;
import rvpredict.trace.Trace;
import rvpredict.trace.UnlockNode;
import rvpredict.trace.WaitNode;
import rvpredict.trace.WriteEvent;
import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.List;

/**
 * The engine class for happens-before (HB) based race detection. It maintains
 * the HB edge between events and answers reachability inquiries between
 * conflicting events.
 * 
 * @author jeffhuang
 *
 */
public class HBEngine {

    private ReachabilityEngine reachEngine = new ReachabilityEngine();// TODO:
                                                                      // do
                                                                      // segmentation
                                                                      // on this
    private LockSetEngine lockEngine = new LockSetEngine();// construct a new
                                                           // lockset for this
                                                           // segment

    HBEngine(Trace trace) {
        addIntraThreadEdge(trace.getThreadNodesMap());

        // TODO: ensure lockset algorithm??
        addHBEdges(trace, trace.getThreadFirstNodeMap(), trace.getThreadLastNodeMap());
    }

    /**
     * add program order CP edges
     * 
     * @param map
     */
    private void addIntraThreadEdge(HashMap<Long, List<AbstractEvent>> map) {
        Iterator<List<AbstractEvent>> mapIt = map.values().iterator();
        while (mapIt.hasNext()) {
            List<AbstractEvent> nodes = mapIt.next();
            long lastGID = nodes.get(0).getGID();
            for (int i = 1; i < nodes.size(); i++) {
                long thisGID = nodes.get(i).getGID();

                reachEngine.addEdge(lastGID, thisGID);

                lastGID = thisGID;

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
     *
     * @param trace
     * @param firstNodes
     * @param lastNodes
     */
    private void addHBEdges(Trace trace, HashMap<Long, AbstractEvent> firstNodes,
            HashMap<Long, AbstractEvent> lastNodes) {
        HashMap<String, WriteEvent> addressLastWriteMap = new HashMap<String, WriteEvent>();
        HashMap<String, ReadEvent> addressLastReadMap = new HashMap<String, ReadEvent>();

        HashMap<String, ArrayList<LockPair>> lockAddrNodes = new HashMap<String, ArrayList<LockPair>>();
        HashMap<Long, Stack<SyncEvent>> threadSyncStack = new HashMap<Long, Stack<SyncEvent>>();

        NotifyNode matchNotifyNode = null;

        // during recording
        // should after wait, before notify
        // after lock, before unlock
        List<AbstractEvent> nodes = trace.getFullTrace();
        for (int i = 0; i < nodes.size(); i++) {
            AbstractEvent node = nodes.get(i);
            long thisGID = node.getGID();

            // add first node

            if (node instanceof StartNode) {
                long tid = Long.valueOf(((StartNode) node).getAddr());

                AbstractEvent fnode = firstNodes.get(tid);
                if (fnode != null) {
                    long fGID = fnode.getGID();
                    reachEngine.addEdge(thisGID, fGID);

                }
            } else if (node instanceof JoinNode) {
                long tid = Long.valueOf(((JoinNode) node).getAddr());
                AbstractEvent lnode = lastNodes.get(tid);
                if (lnode != null) {
                    long lGID = lnode.getGID();
                    reachEngine.addEdge(lGID, thisGID);

                }

            } else if (node instanceof ReadEvent) {

                String addr = ((ReadEvent) node).getAddr();
                WriteEvent wnode = addressLastWriteMap.get(addr);
                if (wnode != null) {
                    reachEngine.addEdge(wnode.getGID(), node.getGID());
                }
                addressLastReadMap.put(addr, (ReadEvent) node);

            } else if (node instanceof WriteEvent) {
                String addr = ((WriteEvent) node).getAddr();
                WriteEvent wnode = addressLastWriteMap.get(addr);
                if (wnode != null) {
                    reachEngine.addEdge(wnode.getGID(), node.getGID());
                }
                ReadEvent rnode = addressLastReadMap.get(addr);
                if (rnode != null) {
                    reachEngine.addEdge(rnode.getGID(), node.getGID());
                }
                addressLastWriteMap.put(addr, (WriteEvent) node);

            } else if (node instanceof LockNode) {
                long tid = node.getTID();

                Stack<SyncEvent> syncstack = threadSyncStack.get(tid);
                if (syncstack == null) {
                    syncstack = new Stack<SyncEvent>();
                    threadSyncStack.put(tid, syncstack);
                }
                syncstack.push(((LockNode) node));

            } else if (node instanceof UnlockNode) {
                long tid = node.getTID();

                Stack<SyncEvent> syncstack = threadSyncStack.get(tid);

                // assert(stack.size()>0);//this is possible when segmented
                if (syncstack == null) {
                    syncstack = new Stack<SyncEvent>();
                    threadSyncStack.put(tid, syncstack);
                }
                LockPair lp = null;
                if (syncstack.isEmpty()) {
                    // lp = new LockPair(null,(ISyncNode)node);
                    // make it non-null
                    AbstractEvent firstnode = firstNodes.get(tid);
                    long fake_gid = firstnode.getGID();
                    LockNode fake_node = new LockNode(fake_gid, tid, firstnode.getID(),
                            ((UnlockNode) node).getLockAddr());
                    lp = new LockPair(fake_node, (SyncEvent) node);
                } else {
                    lp = new LockPair(syncstack.pop(), (SyncEvent) node);

                    // filter out re-entrant locks
                    if (syncstack.size() > 0)
                        if (((UnlockNode) node).getAddr().equals(syncstack.get(0).getAddr())) {
                            continue;
                        }
                }

                String addr = ((UnlockNode) node).getAddr();

                ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);

                }

                syncNodeList.add(lp);
                lockEngine.add(((SyncEvent) node).getAddr(), tid, lp);

            } else if (node instanceof WaitNode) {
                long tid = node.getTID();

                // assert(matchNotifyNode!=null);this is also possible when
                // segmented
                if (matchNotifyNode != null) {
                    long notifyGID = matchNotifyNode.getGID();

                    int nodeIndex = trace.getFullTrace().indexOf(node) + 1;

                    try {
                        // TODO: handle OutofBounds
                        try {
                            while (trace.getFullTrace().get(nodeIndex).getTID() != tid)
                                nodeIndex++;
                        } catch (Exception e) {
                            // if we arrive here, it means the wait node is the
                            // last node of the corresponding thread
                            // so add an order from notify to wait instead
                            nodeIndex = trace.getFullTrace().indexOf(node);
                        }
                        long waitNextGID = trace.getFullTrace().get(nodeIndex).getGID();

                        reachEngine.addEdge(notifyGID, waitNextGID);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // clear notifyNode
                    matchNotifyNode = null;
                }

                Stack<SyncEvent> syncstack = threadSyncStack.get(tid);
                // assert(stack.size()>0);
                if (syncstack == null) {
                    syncstack = new Stack<SyncEvent>();
                    threadSyncStack.put(tid, syncstack);
                }
                LockPair lp = null;
                if (syncstack.isEmpty()) {
                    // lp = new LockPair(null,((WaitNode) node));

                    AbstractEvent firstnode = firstNodes.get(tid);
                    long fake_gid = firstnode.getGID();
                    LockNode fake_node = new LockNode(fake_gid, tid, firstnode.getID(),
                            ((WaitNode) node).getSigAddr());
                    lp = new LockPair(fake_node, (SyncEvent) node);
                } else
                    lp = new LockPair(syncstack.pop(), ((WaitNode) node));

                String addr = ((WaitNode) node).getAddr();

                ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);

                }

                syncNodeList.add(lp);
                lockEngine.add(((SyncEvent) node).getAddr(), tid, lp);

                syncstack.push(((WaitNode) node));

            } else if (node instanceof NotifyNode) {
                matchNotifyNode = (NotifyNode) node;
            }
        }

        // process the last lock node
        Iterator<Long> tidIt = threadSyncStack.keySet().iterator();
        while (tidIt.hasNext()) {
            Long tid = tidIt.next();

            Stack<SyncEvent> stack = threadSyncStack.get(tid);

            AbstractEvent lastnode = lastNodes.get(tid);
            long fake_gid = lastnode.getGID();

            while (stack.size() > 0) {
                SyncEvent node = stack.remove(0);
                UnlockNode fake_node = new UnlockNode(fake_gid, tid, lastnode.getID(),
                        node.getAddr());

                LockPair lp = new LockPair(node, fake_node);

                String addr = node.getAddr();

                ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);

                if (syncNodeList == null) {
                    syncNodeList = new ArrayList<LockPair>();
                    lockAddrNodes.put(addr, syncNodeList);
                }
                syncNodeList.add(lp);
                lockEngine.add(node.getAddr(), tid, lp);

            }
        }

        // add HB edge between lock regions

        Iterator<String> addrIt = lockAddrNodes.keySet().iterator();
        while (addrIt.hasNext()) {
            String addr = addrIt.next();

            ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
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
     * @param node1
     * @param node2
     * @return
     */
    public boolean isRace(AbstractEvent node1, AbstractEvent node2) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();

        // lockset algorithm
        if (lockEngine.hasCommonLock(node1.getTID(), gid1, node2.getTID(), gid2))
            return false;

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
