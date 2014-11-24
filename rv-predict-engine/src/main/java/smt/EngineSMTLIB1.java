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
package smt;

import rvpredict.trace.AbstractEvent;
import rvpredict.trace.EventType;
import rvpredict.trace.MemoryAccessEvent;
import rvpredict.trace.SyncEvent;
import rvpredict.trace.LockPair;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.WriteEvent;
import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.List;
import java.util.Map.Entry;

import rvpredict.config.Configuration;

public class EngineSMTLIB1 extends Engine {
    String CONS_BENCHNAME;
    final String BRACKET_LEFT = "(";
    final String BRACKET_RIGHT = ")";

    public EngineSMTLIB1(Configuration config) {
        super(config);
        CONS_BENCHNAME = "(benchmark " + config.tableName + ".smt\n";
    }

    @Override
    public void declareVariables(List<AbstractEvent> trace) {
        CONS_SETLOGIC = ":logic QF_IDL\n";

        CONS_DECLARE = new StringBuilder(":extrafuns (\n");

        CONS_ASSERT = new StringBuilder(":formula (and \n");

        // CONS_ASSERT = "(assert (distinct ";
        int size = trace.size();
        for (int i = 0; i < size; i++) {
            AbstractEvent node = trace.get(i);
            long GID = node.getGID();
            String var = makeVariable(GID);

            CONS_DECLARE.append(BRACKET_LEFT).append(var).append(" Int)\n");

            // CONS_ASSERT.append(var).append(" ");

            // CONS_ASSERT.append("(assert (and (> ").append(var).append(" 0) (< ").append(var)
            // .append(" ").append(size+1).append(")))\n");
        }

        CONS_DECLARE.append(BRACKET_RIGHT).append("\n");

    }

    @Override
    public void addIntraThreadConstraints(HashMap<Long, List<AbstractEvent>> map) {
        // create reachability engine
        reachEngine = new ReachabilityEngine();

        Iterator<List<AbstractEvent>> mapIt = map.values().iterator();
        while (mapIt.hasNext()) {
            List<AbstractEvent> nodes = mapIt.next();
            long lastGID = nodes.get(0).getGID();
            String lastVar = makeVariable(lastGID);
            for (int i = 1; i < nodes.size(); i++) {
                long thisGID = nodes.get(i).getGID();
                String var = makeVariable(thisGID);
                CONS_ASSERT.append("(< ").append(lastVar).append(" ").append(var).append(")\n");

                reachEngine.addEdge(lastGID, thisGID);

                lastGID = thisGID;
                lastVar = var;

            }
        }
    }

    @Override
    public void addPSOIntraThreadConstraints(
            HashMap<String, HashMap<Long, List<MemoryAccessEvent>>> indexedMap) {

        Iterator<HashMap<Long, List<MemoryAccessEvent>>> mapIt1 = indexedMap.values().iterator();
        while (mapIt1.hasNext()) {
            HashMap<Long, List<MemoryAccessEvent>> map = mapIt1.next();

            Iterator<List<MemoryAccessEvent>> mapIt2 = map.values().iterator();
            while (mapIt2.hasNext()) {
                List<MemoryAccessEvent> nodes = mapIt2.next();
                long lastGID = nodes.get(0).getGID();
                String lastVar = makeVariable(lastGID);
                for (int i = 1; i < nodes.size(); i++) {
                    long thisGID = nodes.get(i).getGID();
                    String var = makeVariable(thisGID);
                    CONS_ASSERT.append("(< ").append(lastVar).append(" ").append(var).append(")\n");

                    reachEngine.addEdge(lastGID, thisGID);

                    lastGID = thisGID;
                    lastVar = var;

                }
            }
        }

    }

    // the order constraints between wait/notify/fork/join/lock/unlock
    @Override
    public void addSynchronizationConstraints(Trace trace,
            HashMap<Long, List<SyncEvent>> syncNodesMap,
            HashMap<Long, AbstractEvent> firstNodes, HashMap<Long, AbstractEvent> lastNodes) {
        lockEngine = new LockSetEngine();// construct a new lockset for this
                                         // segment

        // thread first node - last node
        Iterator<List<SyncEvent>> mapIt = syncNodesMap.values().iterator();
        while (mapIt.hasNext()) {
            List<SyncEvent> nodes = mapIt.next();

            List<LockPair> lockPairs = new ArrayList<>();

            HashMap<Long, Stack<SyncEvent>> threadSyncStack = new HashMap<Long, Stack<SyncEvent>>();
            SyncEvent matchNotifyNode = null;

            // during recording
            // should after wait, before notify
            // after lock, before unlock

            for (int i = 0; i < nodes.size(); i++) {
                SyncEvent node = nodes.get(i);
                long thisGID = node.getGID();
                String var = makeVariable(thisGID);
                if (node.getType().equals(EventType.START)) {
                    long tid = Long.valueOf(node.getSyncObject());
                    AbstractEvent fnode = firstNodes.get(tid);
                    if (fnode != null) {
                        long fGID = fnode.getGID();
                        String fvar = makeVariable(fGID);

                        CONS_ASSERT.append("(< ").append(var).append(" ").append(fvar)
                                .append(")\n");

                        reachEngine.addEdge(thisGID, fGID);

                    }
                } else if (node.getType().equals(EventType.JOIN)) {
                    long tid = Long.valueOf(node.getSyncObject());
                    AbstractEvent lnode = lastNodes.get(tid);
                    if (lnode != null) {
                        long lGID = lnode.getGID();
                        String lvar = makeVariable(lGID);
                        CONS_ASSERT.append("(< ").append(lvar).append(" ").append(var)
                                .append(")\n");
                        reachEngine.addEdge(lGID, thisGID);

                    }

                } else if (node.getType().equals(EventType.LOCK)) {
                    long tid = node.getTID();

                    Stack<SyncEvent> stack = threadSyncStack.get(tid);
                    if (stack == null) {
                        stack = new Stack<SyncEvent>();
                        threadSyncStack.put(tid, stack);
                    }

                    stack.push(node);
                } else if (node.getType().equals(EventType.UNLOCK)) {
                    long tid = node.getTID();
                    Stack<SyncEvent> stack = threadSyncStack.get(tid);

                    // assert(stack.size()>0);//this is possible when segmented
                    if (stack == null) {
                        stack = new Stack<SyncEvent>();
                        threadSyncStack.put(tid, stack);
                    }

                    // TODO: make sure no nested locks?

                    if (stack.isEmpty()) {
                        LockPair lp = new LockPair(null, node);
                        lockPairs.add(lp);
                        lockEngine.add(node.getSyncObject(), tid, lp);
                    } else if (stack.size() == 1) {
                        LockPair lp = new LockPair(stack.pop(), node);
                        lockPairs.add(lp);

                        lockEngine.add(node.getSyncObject(), tid, lp);
                    } else
                        stack.pop();// handle reentrant lock here

                } else if (node.getType().equals(EventType.WAIT)) {
                    long tid = node.getTID();

                    // assert(matchNotifyNode!=null);this is also possible when
                    // segmented
                    if (matchNotifyNode != null) {
                        long notifyGID = matchNotifyNode.getGID();
                        String notifyVar = makeVariable(notifyGID);

                        int nodeIndex = trace.getFullTrace().indexOf(node) + 1;

                        try {
                            // TODO: handle OutofBounds
                            try {
                                while (trace.getFullTrace().get(nodeIndex).getTID() != tid)
                                    nodeIndex++;
                            } catch (Exception e) {
                                // if we arrive here, it means the wait node is
                                // the last node of the corresponding thread
                                // so add an order from notify to wait instead
                                nodeIndex = trace.getFullTrace().indexOf(node);
                            }
                            long waitNextGID = trace.getFullTrace().get(nodeIndex).getGID();
                            var = makeVariable(waitNextGID);

                            CONS_ASSERT.append("(< ").append(notifyVar).append(" ").append(var)
                                    .append(")\n");

                            reachEngine.addEdge(notifyGID, waitNextGID);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // clear notifyNode
                        matchNotifyNode = null;
                    }

                    Stack<SyncEvent> stack = threadSyncStack.get(tid);
                    // assert(stack.size()>0);
                    if (stack == null) {
                        stack = new Stack<SyncEvent>();
                        threadSyncStack.put(tid, stack);
                    }
                    if (stack.isEmpty())
                        lockPairs.add(new LockPair(null, node));
                    else if (stack.size() == 1)
                        lockPairs.add(new LockPair(stack.pop(), node));
                    else
                        stack.pop();// handle reentrant lock here

                    stack.push(node);

                } else if (node.getType().equals(EventType.NOTIFY)) {
                    matchNotifyNode = node;
                }
            }

            // check threadSyncStack
            Iterator<Stack<SyncEvent>> stackIt = threadSyncStack.values().iterator();
            while (stackIt.hasNext()) {
                Stack<SyncEvent> stack = stackIt.next();
                if (stack.size() > 0)// handle reentrant lock here, only pop the
                                     // first locking node
                {
                    SyncEvent node = stack.firstElement();
                    LockPair lp = new LockPair(node, null);
                    lockPairs.add(lp);
                    lockEngine.add(node.getSyncObject(), node.getTID(), lp);
                }
            }

            CONS_ASSERT.append(constructLockConstraintsOptimized(lockPairs));
        }

    }

    private String constructLockConstraintsOptimized(List<LockPair> lockPairs) {
        String CONS_LOCK = "";

        // obtain each thread's last lockpair
        HashMap<Long, LockPair> lastLockPairMap = new HashMap<Long, LockPair>();

        for (int i = 0; i < lockPairs.size(); i++) {
            LockPair lp1 = lockPairs.get(i);
            String var_lp1_a = "";
            String var_lp1_b = "";

            if (lp1.lock == null)//
                continue;
            else
                var_lp1_a = makeVariable(lp1.lock.getGID());

            if (lp1.unlock != null)
                var_lp1_b = makeVariable(lp1.unlock.getGID());

            long lp1_tid = lp1.lock.getTID();
            LockPair lp1_pre = lastLockPairMap.get(lp1_tid);

            /*
             * String var_lp1_pre_b = ""; if(lp1_pre!=null) var_lp1_pre_b =
             * makeVariable(lp1_pre.unlock.getGID());;
             */

            ArrayList<LockPair> flexLockPairs = new ArrayList<LockPair>();

            // find all lps that are from a different thread, and have no
            // happens-after relation with lp1
            // could further optimize by consider lock regions per thread
            for (int j = 0; j < lockPairs.size(); j++) {
                LockPair lp = lockPairs.get(j);
                if (lp.lock != null) {
                    if (lp.lock.getTID() != lp1_tid
                            && !canReach(lp1.lock, lp.lock)) {
                        flexLockPairs.add(lp);
                    }
                } else if (lp.unlock != null) {
                    if (lp.unlock.getTID() != lp1_tid
                            && !canReach(lp1.lock, lp.unlock)) {
                        flexLockPairs.add(lp);
                    }
                }
            }

            for (int j = 0; j < flexLockPairs.size(); j++) {
                LockPair lp2 = flexLockPairs.get(j);

                if (lp2.unlock == null || lp2.lock == null && lp1_pre != null)// impossible
                                                                              // to
                                                                              // match
                                                                              // lp2
                    continue;

                String var_lp2_b = "";
                String var_lp2_a = "";

                var_lp2_b = makeVariable(lp2.unlock.getGID());

                if (lp2.lock != null)
                    var_lp2_a = makeVariable(lp2.lock.getGID());

                String cons_b;

                // lp1_b==null, lp2_a=null
                if (lp1.unlock == null || lp2.lock == null) {
                    cons_b = "(> " + var_lp1_a + " " + var_lp2_b + ")\n";
                    // the trace may not be well-formed due to segmentation
                    if (lp1.lock.getGID() < lp2.unlock.getGID())
                        cons_b = "";

                } else {
                    cons_b = "(or (> " + var_lp1_a + " " + var_lp2_b + ") (> " + var_lp2_a + " "
                            + var_lp1_b + "))\n";
                }

                CONS_LOCK += cons_b;

            }
            lastLockPairMap.put(lp1.lock.getTID(), lp1);

        }

        return CONS_LOCK;

    }

    public void addReadWriteConstraints(HashMap<String, List<ReadEvent>> indexedReadNodes,
            HashMap<String, List<WriteEvent>> indexedWriteNodes) {
        CONS_ASSERT.append(constructReadWriteConstraints(indexedReadNodes, indexedWriteNodes));
    }

    // TODO: NEED to handle the feasibility of new added write nodes
    @Override
    public StringBuilder constructCausalReadWriteConstraintsOptimized(long rgid,
            List<ReadEvent> readNodes, HashMap<String, List<WriteEvent>> indexedWriteNodes,
            HashMap<String, Long> initValueMap) {
        StringBuilder CONS_CAUSAL_RW = new StringBuilder("");

        for (int i = 0; i < readNodes.size(); i++) {

            ReadEvent rnode = readNodes.get(i);
            // filter out itself --
            if (rgid == rnode.getGID())
                continue;

            // get all write nodes on the address
            List<WriteEvent> writenodes = indexedWriteNodes.get(rnode.getAddr());
            // no write to array field?
            // Yes, it could be: java.io.PrintStream out
            if (writenodes == null || writenodes.size() < 1)//
                continue;

            WriteEvent preNode = null;//

            // get all write nodes on the address & write the same value
            List<WriteEvent> writenodes_value_match = new ArrayList<>();
            for (int j = 0; j < writenodes.size(); j++) {
                WriteEvent wnode = writenodes.get(j);
                if (wnode.getValue() == rnode.getValue() && !canReach(rnode, wnode)) {
                    if (wnode.getTID() != rnode.getTID())
                        writenodes_value_match.add(wnode);
                    else {
                        if (preNode == null
                                || (preNode.getGID() < wnode.getGID() && wnode.getGID() < rnode
                                        .getGID()))
                            preNode = wnode;

                    }
                }
            }
            if (writenodes_value_match.size() > 0) {
                if (preNode != null)
                    writenodes_value_match.add(preNode);

                // TODO: consider the case when preNode is not null

                String var_r = makeVariable(rnode.getGID());

                String cons_a = "";
                String cons_a_end = "";

                String cons_b = "";
                String cons_b_end = "";

                // make sure all the nodes that x depends on read the same value

                for (int j = 0; j < writenodes_value_match.size(); j++) {
                    WriteEvent wnode1 = writenodes_value_match.get(j);
                    String var_w1 = makeVariable(wnode1.getGID());

                    String cons_b_ = "(> " + var_r + " " + var_w1 + ")\n";

                    String cons_c = "";
                    String cons_c_end = "";
                    String last_cons_d = null;
                    for (int k = 0; k < writenodes.size(); k++) {
                        WriteEvent wnode2 = writenodes.get(k);
                        if (!writenodes_value_match.contains(wnode2) && !canReach(wnode2, wnode1)
                                && !canReach(rnode, wnode2)) {
                            String var_w2 = makeVariable(wnode2.getGID());

                            if (last_cons_d != null) {
                                cons_c += "(and " + last_cons_d;
                                cons_c_end += ")";

                            }
                            last_cons_d = "(or (> " + var_w2 + " " + var_r + ")" + " (> " + var_w1
                                    + " " + var_w2 + "))\n";

                        }
                    }
                    if (last_cons_d != null) {
                        cons_c += last_cons_d;
                    }
                    cons_c = cons_c + cons_c_end;

                    if (cons_c.length() > 0)
                        cons_b_ = "(and " + cons_b_ + " " + cons_c + ")\n";

                    if (j + 1 < writenodes_value_match.size()) {
                        cons_b += "(or " + cons_b_;
                        cons_b_end += ")";

                        cons_a += "(and (> " + var_w1 + " " + var_r + ")\n";
                        cons_a_end += ")";
                    } else {
                        cons_b += cons_b_;
                        cons_a += "(> " + var_w1 + " " + var_r + ")\n";
                    }
                }

                cons_b += cons_b_end;

                Long rValue = rnode.getValue();
                Long initValue = initValueMap.get(rnode.getAddr());

                // it's possible that we don't have initial value for static
                // variable
                // so we allow init value to be zero or null? -- null is turned
                // into 0 by System.identityHashCode
                boolean allowMatchInit = true;
                if (initValue == null) {
                    // it's possible for the read node to match with the init
                    // write node
                    // if preNode is null
                    if (preNode != null) {
                        allowMatchInit = false;
                    }
                }

                if (initValue == null && allowMatchInit || initValue != null
                        && rValue.equals(initValue)) {
                    if (cons_a.length() > 0) {
                        cons_a += cons_a_end + "\n";
                        CONS_CAUSAL_RW.append("(or \n" + cons_a + " " + cons_b + ")\n\n");
                    }
                } else {
                    CONS_CAUSAL_RW.append(cons_b + "\n");
                }
            } else {
                // make sure it reads the initial write
                Long rValue = rnode.getValue();
                Long initValue = initValueMap.get(rnode.getAddr());

                if (initValue != null && rValue.equals(initValue)) {
                    String var_r = makeVariable(rnode.getGID());

                    for (int k = 0; k < writenodes.size(); k++) {
                        WriteEvent wnode3 = writenodes.get(k);
                        if (wnode3.getTID() != rnode.getTID() && !canReach(rnode, wnode3)) {
                            String var_w3 = makeVariable(wnode3.getGID());

                            String cons_e = "(> " + var_w3 + " " + var_r + ")\n";
                            CONS_CAUSAL_RW.append(cons_e);
                        }

                    }

                }

            }

        }

        return CONS_CAUSAL_RW;
    }

    // does not consider value and causal dependence
    private static String constructReadWriteConstraints(
            HashMap<String, List<ReadEvent>> indexedReadNodes,
            HashMap<String, List<WriteEvent>> indexedWriteNodes) {

        String CONS_RW = "";

        Iterator<Entry<String, List<ReadEvent>>> entryIt = indexedReadNodes.entrySet().iterator();
        while (entryIt.hasNext()) {
            Entry<String, List<ReadEvent>> entry = entryIt.next();
            String addr = entry.getKey();

            // get all read nodes on the address
            List<ReadEvent> readnodes = entry.getValue();

            // get all write nodes on the address
            List<WriteEvent> writenodes = indexedWriteNodes.get(addr);

            // no write to array field?
            // Yes, it could be: java.io.PrintStream out
            if (writenodes == null || writenodes.size() < 2)
                continue;

            for (int i = 0; i < readnodes.size(); i++) {
                ReadEvent rnode = readnodes.get(i);
                String var_r = makeVariable(rnode.getGID());

                String cons_a = "";

                String cons_a_end = "true";

                String cons_b = "";
                String cons_b_end = "false";

                // we start from j=1 to exclude the initial write
                for (int j = 1; j < writenodes.size(); j++) {
                    WriteEvent wnode1 = writenodes.get(j);
                    {
                        String var_w1 = makeVariable(wnode1.getGID());

                        cons_a = "(and (> " + var_w1 + " " + var_r + ")\n" + cons_a;
                        cons_a_end += ")";

                        String cons_b_ = "(and (> " + var_r + " " + var_w1 + ")\n";

                        String cons_c = "";
                        String cons_c_end = "true";

                        for (int k = 1; k < writenodes.size(); k++) {
                            if (j != k) {
                                WriteEvent wnode2 = writenodes.get(k);
                                String var_w2 = makeVariable(wnode2.getGID());

                                String cons_d = "(and " + "(or (> " + var_w2 + " " + var_r + ")"
                                        + " (> " + var_w1 + " " + var_w2 + "))\n";

                                cons_c = cons_d + cons_c;
                                cons_c_end += ")";
                            }
                        }

                        cons_c += cons_c_end;

                        cons_b_ = cons_b_ + cons_c + ")\n";

                        cons_b += "(or " + cons_b_;

                        cons_b_end += ")";
                    }
                }

                cons_b += cons_b_end;

                cons_a += cons_a_end + "\n";

                CONS_RW += "(assert \n(or \n" + cons_a + " " + cons_b + "))\n\n";
            }

        }

        return CONS_RW;
    }

    @Override
    public boolean isAtomic(MemoryAccessEvent node1, MemoryAccessEvent node2, MemoryAccessEvent node3) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();
        long gid3 = node3.getGID();

        return lockEngine.isAtomic(node1.getTID(), gid1, gid2, node3.getTID(), gid3);

    }

    @Override
    public boolean hasCommonLock(MemoryAccessEvent node1, MemoryAccessEvent node2) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();

        return lockEngine.hasCommonLock(node1.getTID(), gid1, node2.getTID(), gid2);

    }

    @Override
    public boolean canReach(AbstractEvent node1, AbstractEvent node2) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();

        return reachEngine.canReach(gid1, gid2);

    }

    @Override
    public boolean isRace(AbstractEvent node1, AbstractEvent node2, StringBuilder casualConstraint) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();
        //
        // if(gid1<gid2)
        // { if(reachEngine.canReach(gid1, gid2))
        // return false;
        // }
        // else
        // {
        // if(reachEngine.canReach(gid2, gid1))
        // return false;
        // }

        String var1 = makeVariable(gid1);
        String var2 = makeVariable(gid2);

        // String QUERY = "\n(assert (= "+var1+" "+var2+"))\n\n";

        id++;
        task = new SMTTaskRun(config, id);

        String cons_assert = CONS_ASSERT.toString() + casualConstraint.toString() + ")\n";
        cons_assert = cons_assert.replace(var2 + " ", var1 + " ");
        cons_assert = cons_assert.replace(var2 + ")", var1 + ")");
        StringBuilder msg = new StringBuilder(CONS_BENCHNAME).append(CONS_SETLOGIC)
                .append(CONS_DECLARE).append(cons_assert).append(BRACKET_RIGHT);
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public static void testConstructReadWriteConstraints() {
        HashMap<String, List<ReadEvent>> indexedReadNodes = new HashMap<String, List<ReadEvent>>();

        HashMap<String, List<WriteEvent>> indexedWriteNodes = new HashMap<String, List<WriteEvent>>();

        List<WriteEvent> writeNodes = new ArrayList<>();
        writeNodes.add(new WriteEvent(1, 1, 1, 1, 1, 0));
        writeNodes.add(new WriteEvent(2, 2, 3, 1, 1, 0));
        writeNodes.add(new WriteEvent(3, 3, 5, 1, 1, 1));
        writeNodes.add(new WriteEvent(4, 4, 7, 1, 1, 1));

        List<ReadEvent> readNodes = new ArrayList<>();
        readNodes.add(new ReadEvent(5, 1, 2, 1, 1, 0));
        readNodes.add(new ReadEvent(6, 2, 4, 1, 1, 0));
        readNodes.add(new ReadEvent(7, 3, 6, 1, 1, 1));
        readNodes.add(new ReadEvent(8, 4, 8, 1, 1, 1));

        indexedWriteNodes.put("s", writeNodes);
        indexedReadNodes.put("s", readNodes);

        System.out.println(constructReadWriteConstraints(indexedReadNodes, indexedWriteNodes));
    }

    public static void main(String[] args) throws IOException {
        // testConstructLockConstraints();
        testConstructReadWriteConstraints();
    }

    @Override
    public List<String> getSchedule(long endGID, HashMap<Long, Long> nodeGIDTidMap,
            HashMap<Long, String> threadIdNameMap) {

        List<String> schedule = new ArrayList<>();
        for (int i = 0; i < task.schedule.size(); i++) {
            String xi = task.schedule.get(i);
            long gid = Long.valueOf(xi.substring(1));
            long tid = nodeGIDTidMap.get(gid);
            String name = threadIdNameMap.get(tid);
            schedule.add(name);
            if (gid == endGID)
                break;
        }

        return schedule;
    }

}
