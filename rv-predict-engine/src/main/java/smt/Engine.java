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
import rvpredict.trace.LockNode;
import rvpredict.trace.LockPair;
import rvpredict.trace.PropertyNode;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.UnlockNode;
import rvpredict.trace.WaitNode;
import rvpredict.trace.WriteEvent;
import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.List;

import property.EREProperty;
import rvpredict.config.Configuration;

/**
 * The engine for constraint construction and solving.
 *
 * @author jeffhuang
 *
 */
public class Engine {
    protected int id = 0;// constraint id
    protected SMTTaskRun task;

    protected Configuration config;

    protected ReachabilityEngine reachEngine;// TODO: do segmentation on this
    protected LockSetEngine lockEngine;

    // constraints below
    protected StringBuilder CONS_DECLARE;
    protected StringBuilder CONS_ASSERT;
    protected String CONS_SETLOGIC;
    protected final StringBuilder CONS_GETMODEL = new StringBuilder(
            "(check-sat)\n(get-model)\n(exit)");

    public Engine(Configuration config) {
        this.config = config;
        this.id = 0;
    }

    protected static String makeVariable(long GID) {
        return "x" + GID;
    }

    /**
     * declare an order variable for each event in the trace
     *
     * @param trace
     */
    public void declareVariables(List<AbstractEvent> trace) {
        CONS_SETLOGIC = "(set-logic QF_IDL)\n";// use integer difference logic
        CONS_DECLARE = new StringBuilder("");
        CONS_ASSERT = new StringBuilder("");

        // CONS_ASSERT = "(assert (distinct ";
        int size = trace.size();
        for (int i = 0; i < size; i++) {
            AbstractEvent node = trace.get(i);
            long GID = node.getGID();
            String var = makeVariable(GID);

            CONS_DECLARE.append("(declare-const ").append(var).append(" Int)\n");

            // CONS_ASSERT.append(var).append(" ");

            // CONS_ASSERT.append("(assert (and (> ").append(var).append(" 0) (< ").append(var)
            // .append(" ").append(size+1).append(")))\n");
        }

        // CONS_ASSERT.append("))\n");

    }

    /**
     * add program order constraints
     *
     * @param map
     */
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
                CONS_ASSERT.append("(assert (< ").append(lastVar).append(" ").append(var)
                        .append("))\n");

                // the order is added to reachability engine for quick testing
                reachEngine.addEdge(lastGID, thisGID);

                lastGID = thisGID;
                lastVar = var;

            }
        }
    }

    /**
     * add intra-thread order constraint for POS memory model
     *
     * @param indexedMap
     */
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
                    CONS_ASSERT.append("(assert (< ").append(lastVar).append(" ").append(var)
                            .append("))\n");

                    reachEngine.addEdge(lastGID, thisGID);

                    lastGID = thisGID;
                    lastVar = var;

                }
            }
        }

    }

    /**
     * the order constraints between wait/notify/fork/join/lock/unlock
     *
     * @param trace
     * @param syncNodesMap
     * @param firstNodes
     * @param lastNodes
     */
    public void addSynchronizationConstraints(Trace trace,
            HashMap<Long, List<SyncEvent>> syncNodesMap,
            HashMap<Long, AbstractEvent> firstNodes, HashMap<Long, AbstractEvent> lastNodes) {
        // construct a new lockset for this segment
        lockEngine = new LockSetEngine();

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

                        // start-begin ordering
                        CONS_ASSERT.append("(assert (< ").append(var).append(" ").append(fvar)
                                .append("))\n");

                        reachEngine.addEdge(thisGID, fGID);

                    }
                } else if (node.getType().equals(EventType.JOIN)) {
                    long tid = Long.valueOf(node.getSyncObject());
                    AbstractEvent lnode = lastNodes.get(tid);
                    if (lnode != null) {
                        long lGID = lnode.getGID();
                        String lvar = makeVariable(lGID);

                        // end-join ordering
                        CONS_ASSERT.append("(assert (< ").append(lvar).append(" ").append(var)
                                .append("))\n");
                        reachEngine.addEdge(lGID, thisGID);

                    }

                } else if (node instanceof LockNode) {
                    long tid = node.getTID();

                    Stack<SyncEvent> stack = threadSyncStack.get(tid);
                    if (stack == null) {
                        stack = new Stack<SyncEvent>();
                        threadSyncStack.put(tid, stack);
                    }

                    stack.push(node);
                } else if (node instanceof UnlockNode) {
                    long tid = node.getTID();
                    Stack<SyncEvent> stack = threadSyncStack.get(tid);

                    // assert(stack.size()>0);//this is possible when segmented
                    if (stack == null) {
                        stack = new Stack<SyncEvent>();
                        threadSyncStack.put(tid, stack);
                    }

                    // pair lock/unlock nodes
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

                } else if (node instanceof WaitNode) {
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

                            // notify-wait ordering

                            CONS_ASSERT.append("(assert (< ").append(notifyVar).append(" ")
                                    .append(var).append("))\n");

                            reachEngine.addEdge(notifyGID, waitNextGID);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // clear notifyNode
                        matchNotifyNode = null;
                    }

                    // wait is interpreted as unlock-wait-lock
                    // so we first pair wait with previos lock
                    // and then push it into stack as a lock node

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

            // Now construct the lock/unlock constraints
            CONS_ASSERT.append(constructLockConstraintsOptimized(lockPairs));
        }

    }

    /**
     * lock/unlock constraints Make sure no two regions on the same lock can
     * interleave.
     *
     * @param lockPairs
     * @return
     */
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
            if (flexLockPairs.size() > 0) {

                // for each lock pair lp2 in flexLockPairs
                // it is either before lp1 or after lp1
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
                        cons_b = "(> " + var_lp1_a + " " + var_lp2_b + ")";
                        // the trace may not be well-formed due to segmentation
                        if (lp1.lock.getGID() < lp2.unlock.getGID())
                            cons_b = "";
                    } else {
                        cons_b = "(or (> " + var_lp1_a + " " + var_lp2_b + ") (> " + var_lp2_a
                                + " " + var_lp1_b + "))";
                    }
                    if (!cons_b.isEmpty())
                        CONS_LOCK += "(assert " + cons_b + ")\n";

                }

            }
            lastLockPairMap.put(lp1.lock.getTID(), lp1);

        }

        return CONS_LOCK;

    }

    /**
     * return the read-write constraints
     *
     * @param readNodes
     * @param indexedWriteNodes
     * @param initValueMap
     * @return
     */
    // TODO: NEED to handle the feasibility of new added write nodes
    public StringBuilder constructCausalReadWriteConstraintsOptimized(long rgid,
            List<ReadEvent> readNodes, HashMap<String, List<WriteEvent>> indexedWriteNodes,
            HashMap<String, Long> initValueMap) {
        StringBuilder CONS_CAUSAL_RW = new StringBuilder("");

        // for every read node in the set
        // make sure it is matched with a write written the same value
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

                long rValue = rnode.getValue();
                long initValue = initValueMap.get(rnode.getAddr());

                // it's possible that we don't have initial value for static
                // variable
                // so we allow init value to be zero or null? -- null is turned
                // into 0 by System.identityHashCode
                boolean allowMatchInit = true;
                if (initValue == 0) {
                    for (int j = 0; j < writenodes_value_match.size(); j++) {
                        if (writenodes_value_match.get(j).getGID() < rnode.getGID()) {
                            allowMatchInit = false;
                            break;
                        }
                    }
                }

                if (initValue == 0 && allowMatchInit || initValue != 0
                        && rValue == initValue) {
                    if (cons_a.length() > 0) {
                        cons_a += cons_a_end + "\n";
                        CONS_CAUSAL_RW
                                .append("(assert \n(or \n" + cons_a + " " + cons_b + "))\n\n");
                    }
                } else {
                    CONS_CAUSAL_RW.append("(assert \n" + cons_b + ")\n\n");
                }

            } else {
                // make sure it reads the initial write
                long rValue = rnode.getValue();
                long initValue = initValueMap.get(rnode.getAddr());

                if (initValue != 0 && rValue == initValue) {
                    String var_r = makeVariable(rnode.getGID());

                    for (int k = 0; k < writenodes.size(); k++) {
                        WriteEvent wnode3 = writenodes.get(k);
                        if (wnode3.getTID() != rnode.getTID() && !canReach(rnode, wnode3)) {
                            String var_w3 = makeVariable(wnode3.getGID());

                            String cons_e = "(> " + var_w3 + " " + var_r + ")";
                            CONS_CAUSAL_RW.append("(assert \n" + cons_e + ")\n");
                        }
                    }

                }

            }
        }

        return CONS_CAUSAL_RW;
    }

    /**
     * return true if the lockset of node1 and node2 overlaps with that of node3
     *
     * @param node1
     * @param node2
     * @param node3
     * @return
     */
    public boolean isAtomic(MemoryAccessEvent node1, MemoryAccessEvent node2, MemoryAccessEvent node3) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();
        long gid3 = node3.getGID();

        return lockEngine.isAtomic(node1.getTID(), gid1, gid2, node3.getTID(), gid3);

    }

    /**
     * return true if node1 and node2 has a common lock
     *
     * @param node1
     * @param node2
     * @return
     */
    public boolean hasCommonLock(MemoryAccessEvent node1, MemoryAccessEvent node2) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();

        return lockEngine.hasCommonLock(node1.getTID(), gid1, node2.getTID(), gid2);

    }

    /**
     * return true if node1 can reach node2 from the ordering relation
     *
     * @param node1
     * @param node2
     * @return
     */
    public boolean canReach(AbstractEvent node1, AbstractEvent node2) {
        long gid1 = node1.getGID();
        long gid2 = node2.getGID();

        return reachEngine.canReach(gid1, gid2);

    }

    /**
     * return true if the solver return a solution to the constraints
     *
     * @param node1
     * @param node2
     * @param casualConstraint
     * @return
     */
    public boolean isPropertySatisfied(StringBuilder propertyConstraint) {

        id++;
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(CONS_SETLOGIC).append(CONS_DECLARE)
                .append(CONS_ASSERT).append(propertyConstraint).append(CONS_GETMODEL);
        task.sendMessage(msg.toString());

        return task.sat;
    }

    /**
     * return true if the solver return a solution to the constraints
     *
     * @param node1
     * @param node2
     * @param casualConstraint
     * @return
     */
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
        casualConstraint.append(CONS_ASSERT);
        String cons_assert = casualConstraint.toString();
        cons_assert = cons_assert.replace(var2 + " ", var1 + " ");
        cons_assert = cons_assert.replace(var2 + ")", var1 + ")");
        StringBuilder msg = new StringBuilder(CONS_SETLOGIC).append(CONS_DECLARE)
                .append(cons_assert).append(CONS_GETMODEL);
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public boolean isAtomicityViolation(MemoryAccessEvent node1, MemoryAccessEvent node2, MemoryAccessEvent node3,
            StringBuilder casualConstraint1, StringBuilder casualConstraint2,
            StringBuilder casualConstraint3) {

        long gid1 = node1.getGID();
        long gid2 = node2.getGID();
        long gid3 = node3.getGID();

        // if(gid2<gid1)
        // {
        // if(reachEngine.canReach(gid2, gid1))
        // return false;
        // }
        // else if(gid2>gid3)
        // {
        // if(reachEngine.canReach(gid3, gid2))
        // return false;
        // }

        String var1 = makeVariable(gid1);
        String var2 = makeVariable(gid2);
        String var3 = makeVariable(gid3);

        // not global order
        String QUERY = "(assert (and (<= " + var1 + " " + var3 + ")\n" + "(<= " + var3 + " " + var2
                + ")" + "))\n\n";

        id++;
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT)
                .append(casualConstraint1).append(casualConstraint2).append(casualConstraint3)
                .append(QUERY).append(CONS_GETMODEL);
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public boolean isAtomicityViolation(MemoryAccessEvent node1, MemoryAccessEvent node2, MemoryAccessEvent node3) {

        String var1 = makeVariable(node1.getGID());
        String var2 = makeVariable(node2.getGID());
        String var3 = makeVariable(node3.getGID());

        // not global order
        String QUERY = "(assert (and (<= " + var1 + " " + var3 + ")\n" + "(<= " + var3 + " " + var2
                + ")" + "))\n\n";

        id++;
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT).append(QUERY)
                .append(CONS_GETMODEL);
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public boolean isDeadlock(LockPair lp1, LockPair lp2, LockPair lp3, LockPair lp4) {

        String var1a = "";
        String var1b = "";
        String var2a = "";
        String var2b = "";
        String var3a = "";
        String var3b = "";
        String var4a = "";
        String var4b = "";

        if (lp1.lock != null)
            var1a = makeVariable(lp1.lock.getGID());
        if (lp1.unlock != null)
            var1b = makeVariable(lp1.unlock.getGID());

        if (lp2.lock != null)
            var2a = makeVariable(lp2.lock.getGID());
        else
            return false;// lp2.lock must be here

        if (lp2.unlock != null)
            var2b = makeVariable(lp2.unlock.getGID());

        if (lp3.lock != null)
            var3a = makeVariable(lp3.lock.getGID());
        else
            return false;// lp3.lock must be here
        if (lp3.unlock != null)
            var3b = makeVariable(lp3.unlock.getGID());
        else
            return false;// lp3.unlock must be here

        if (lp4.lock != null)
            var4a = makeVariable(lp4.lock.getGID());
        else
            return false;// lp4.lock must be here
        if (lp4.unlock != null)
            var4b = makeVariable(lp4.unlock.getGID());
        else
            return false;// lp4.unlock must be here

        String QUERY = "";

        if (lp1.lock != null)
            QUERY += "(assert (< " + var1a + " " + var2a + "))\n";

        QUERY += "(assert (< " + var2a + " " + var3a + "))\n";

        if (lp1.unlock != null)
            QUERY += "(assert (< " + var3b + " " + var1b + "))\n";

        if (lp2.unlock != null)
            QUERY += "(assert (< " + var4b + " " + var2b + "))\n";

        id++;
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT).append(QUERY)
                .append(CONS_GETMODEL);
        task.sendMessage(msg.toString());

        return task.sat;

    }

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

    public String constructPropertyConstraint(ArrayList<PropertyNode> pi, boolean isParallel) {
        String CONS = "";
        String CONS_END = "";
        String CMP = "< ";
        if (isParallel)
            CMP = "= ";

        String lastVar = makeVariable(pi.get(0).getGID());
        for (int i = 1; i < pi.size(); i++) {
            PropertyNode node = pi.get(i);
            String var = makeVariable(node.getGID());
            if (i + 1 < pi.size()) {
                CONS += "(and (" + CMP + lastVar + " " + var + ")\n";
                CONS_END += ")";
            } else {
                CONS += "(" + CMP + lastVar + " " + var + ")";
            }
            lastVar = var;
        }

        return CONS + CONS_END + "\n";
    }

    /**
     * Construct the property constraints. The constraints for different
     * monitors are disjuncted.
     *
     * @param properties
     * @param trace
     * @return
     */
    public HashSet<ArrayList<PropertyNode>> constructPropertyInstances(EREProperty property,
            HashMap<Integer, List<PropertyNode>> indexedPropertyNodeMap) {

        int SIZE = property.getPropertySize();

        ArrayList<Integer> properties = property.getPropertyList();

        HashSet<ArrayList<PropertyNode>> nodelist_ = null;

        for (int k = 0; k < SIZE; k++) {
            Integer o = properties.get(k);
            HashSet<ArrayList<PropertyNode>> nodelist_now = new HashSet<ArrayList<PropertyNode>>();

            if (property.isPaired(k)) {
                Integer index = property.getPairedID(k);

                Iterator<ArrayList<PropertyNode>> prevIt = nodelist_.iterator();
                while (prevIt.hasNext()) {
                    ArrayList<PropertyNode> list = prevIt.next();
                    PropertyNode node = property.getPairedNode(list.get(index));
                    if (node != null) {

                        ArrayList<PropertyNode> newlist = new ArrayList<PropertyNode>(list);
                        newlist.add(node);
                        nodelist_now.add(newlist);
                    }
                }

            } else {

                List<PropertyNode> pnodes = indexedPropertyNodeMap.get(o);

                for (int i = 0; i < pnodes.size(); i++) {
                    PropertyNode node = pnodes.get(i);
                    if (nodelist_ == null) {
                        ArrayList<PropertyNode> list = new ArrayList<PropertyNode>();
                        list.add(node);

                        nodelist_now.add(list);
                    } else {
                        Iterator<ArrayList<PropertyNode>> prevIt = nodelist_.iterator();
                        while (prevIt.hasNext()) {
                            ArrayList<PropertyNode> list = prevIt.next();
                            if (!list.contains(node)
                                    && (!property.hasThreadBinding(k) || property
                                            .hasCorrectThreadBinding(k, node.getTID(), list))) {
                                ArrayList<PropertyNode> newlist = new ArrayList<PropertyNode>(list);
                                newlist.add(node);
                                nodelist_now.add(newlist);
                            }
                        }
                    }
                }

            }
            nodelist_ = nodelist_now;

        }

        return nodelist_;

    }

    private EREProperty property;

    public EREProperty getProperty() {
        return property;
    }

    public EREProperty initProperty(HashMap<String, Integer> map, Trace trace) {
        String name = "";
        Iterator<String> nameIt = map.keySet().iterator();
        while (nameIt.hasNext()) {
            name = nameIt.next();
            Integer ID = map.get(name);
            if (ID == -1) {
                property = new EREProperty(name, map, trace);
                break;
            }
        }

        return property;
    }

}
