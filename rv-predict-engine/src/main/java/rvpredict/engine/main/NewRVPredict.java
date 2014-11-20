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
import java.util.*;
import java.util.Map.Entry;

import property.EREProperty;
import rvpredict.config.Configuration;
import rvpredict.util.Logger;
import smt.EngineSMTLIB1;
import smt.Engine;
import rvpredict.trace.AbstractEvent;
import rvpredict.trace.MemoryAccessEvent;
import rvpredict.trace.SyncEvent;
import rvpredict.trace.LockPair;
import rvpredict.trace.PropertyNode;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.TraceInfo;
import rvpredict.trace.WriteEvent;
import rvpredict.trace.EventType;
import violation.AtomicityViolation;
import violation.Deadlock;
import violation.ExactRace;
import violation.IViolation;
import violation.PropertyViolation;
import violation.Race;
import rvpredict.db.DBEngine;

/**
 * The NewRVPredict class implements our new race detection algorithm based on
 * constraint solving. The events in the trace are loaded and processed window
 * by window with a configurable window size.
 *
 * @author jeffhuang
 *
 */
public class NewRVPredict {

    private HashSet<IViolation> violations = new HashSet<IViolation>();
    private HashSet<IViolation> potentialviolations = new HashSet<IViolation>();
    private Configuration config;
    private static boolean detectRace = true;
    private static boolean detectAtomicityViolation = false;
    private static boolean detectDeadlock = false;
    private static boolean detectProperty = false;
    private Logger logger;
    private HashMap<Integer, String> sharedVarIdSigMap = new HashMap<>();
    private HashMap<Integer, String> volatileAddresses = new HashMap<>();
    private HashMap<Integer, String> stmtIdSigMap = new HashMap<>();
    private HashMap<Long, String> threadIdNameMap = new HashMap<>();
    private long totalTraceLength;
    private DBEngine dbEngine;
    private TraceInfo traceInfo;
    private long startTime;

    /**
     * Deadlock detection method. Not used in race detection.
     *
     * @param engine
     * @param trace
     * @param schedule_prefix
     */
    private void detectDeadlock(Engine engine, Trace trace, List<String> schedule_prefix) {
        HashMap<Long, HashMap<String, List<LockPair>>> threadIndexedLockPairs = trace
                .getThreadIndexedLockPairs();
        Object[] threads = threadIndexedLockPairs.keySet().toArray();
        for (int i = 0; i < threads.length - 1; i++) {
            Set<String> lockset1 = threadIndexedLockPairs.get(threads[i]).keySet();
            if (lockset1.size() > 1)
                for (int j = 1; j < threads.length; j++) {
                    Set<String> lockset2 = new HashSet<>(threadIndexedLockPairs.get(threads[j])
                            .keySet());
                    lockset2.retainAll(lockset1);
                    if (lockset2.size() > 1) {
                        HashMap<String, List<LockPair>> indexedLockpairs1 = threadIndexedLockPairs
                                .get(threads[i]);
                        HashMap<String, List<LockPair>> indexedLockpairs2 = threadIndexedLockPairs
                                .get(threads[j]);
                        Object[] addrs = lockset2.toArray();
                        for (int k1 = 0; k1 < addrs.length - 1; k1++) {
                            List<LockPair> vlp1a = indexedLockpairs1.get(addrs[k1]);
                            List<LockPair> vlp2a = indexedLockpairs2.get(addrs[k1]);

                            for (int k2 = 1; k2 < addrs.length; k2++) {
                                List<LockPair> vlp1b = indexedLockpairs1.get(addrs[k2]);
                                List<LockPair> vlp2b = indexedLockpairs2.get(addrs[k2]);

                                for (int i1 = 0; i1 < vlp1a.size(); i1++) {
                                    LockPair lp1a = vlp1a.get(i1);
                                    for (int i2 = 0; i2 < vlp2a.size(); i2++) {
                                        LockPair lp2a = vlp2a.get(i2);
                                        for (int i3 = 0; i3 < vlp1b.size(); i3++) {
                                            LockPair lp1b = vlp1b.get(i3);
                                            for (int i4 = 0; i4 < vlp2b.size(); i4++) {
                                                LockPair lp2b = vlp2b.get(i4);

                                                if (lp1a.lock == null
                                                        || (lp1b.lock != null && lp1a.lock.getGID() > lp1b.lock
                                                                .getGID())) {
                                                    LockPair templp = lp1b;
                                                    lp1b = lp1a;
                                                    lp1a = templp;
                                                }

                                                if (lp2b.lock == null
                                                        || (lp2a.lock != null && lp2b.lock.getGID() > lp2a.lock
                                                                .getGID())) {
                                                    LockPair templp = lp2a;
                                                    lp2a = lp2b;
                                                    lp2b = templp;
                                                }

                                                Deadlock deadlock = new Deadlock(trace
                                                        .getStmtSigIdMap().get(lp1a.lock.getID()),
                                                        trace.getStmtSigIdMap().get(
                                                                lp2b.lock.getID()), trace
                                                                .getStmtSigIdMap().get(
                                                                        lp1b.lock.getID()), trace
                                                                .getStmtSigIdMap().get(
                                                                        lp2a.lock.getID()));

                                                if (!violations.contains(deadlock)) {
                                                    if (engine.isDeadlock(lp1a, lp2b, lp1b, lp2a)) {
                                                        List<String> schedule = engine
                                                                .getSchedule(lp1b.lock.getGID(),
                                                                        trace.getNodeGIDTIdMap(),
                                                                        trace.getThreadIdNameMap());

                                                        schedule.addAll(0, schedule_prefix);

                                                        deadlock.addSchedule(schedule);

                                                        violations.add(deadlock);

                                                        logger.report("Deadlock : " + deadlock,
                                                                Logger.MSGTYPE.REAL);

                                                        logger.report("Schedule: " + trim(schedule)
                                                                + "\n", Logger.MSGTYPE.REAL);
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }

                            }

                        }

                    }
                }
        }
    }

    /**
     * Trim the schedule to show the last 100 only entries
     *
     * @param schedule
     * @return
     */
    private static List<String> trim(List<String> schedule) {
        if (schedule.size() > 100) {
            List<String> s = new ArrayList<>();
            s.add("...");
            for (int i = schedule.size() - 100; i < schedule.size(); i++)
                s.add(schedule.get(i));
            return s;
        } else
            return schedule;
    }

    private void detectDeadlockProperty(Engine engine, Trace trace, EREProperty property,
            List<String> schedule_prefix) {
        List<ReadEvent> readNodes_rw = trace.getAllReadNodes();
        StringBuilder sb_rw = engine.constructCausalReadWriteConstraintsOptimized(-1, readNodes_rw,
                trace.getIndexedWriteNodes(), trace.getInitialWriteValueMap());

        HashMap<String, HashMap<Integer, List<PropertyNode>>> propertyMonitors = trace
                .getPropertyMonitors();

        HashMap<Integer, List<PropertyNode>> indexedPropertyNodeMap = new HashMap<Integer, List<PropertyNode>>();

        Iterator<String> monitorIt = propertyMonitors.keySet().iterator();
        while (monitorIt.hasNext()) {
            String addr = monitorIt.next();

            HashMap<Integer, List<PropertyNode>> m = propertyMonitors.get(addr);
            Iterator<Integer> mIt = m.keySet().iterator();
            while (mIt.hasNext()) {
                Integer i = mIt.next();

                List<PropertyNode> v = indexedPropertyNodeMap.get(i);
                if (v == null) {
                    v = new ArrayList<>();
                    indexedPropertyNodeMap.put(i, v);
                }
                v.addAll(m.get(i));
            }

            // make sure the monitor is complete
        }

        if (indexedPropertyNodeMap.keySet().size() < property.getSize())
            return;

        // HashSet<ArrayList<PropertyNode>> piset =
        // engine.constructPropertyInstances(property,indexedPropertyNodeMap);

        // Iterator<ArrayList<PropertyNode>> piIt = piset.iterator();

        // while(piIt.hasNext())
        {
            // ArrayList<PropertyNode> pi = piIt.next();

            // String cons =
            // engine.constructPropertyConstraint(pi,property.isParallel());

            String cons = "(and (< x333 x452) " + "(and (< x452 342) " + "(< x342 x458)))";

            StringBuilder sb = new StringBuilder("(assert\n" + cons + ")\n");
            PropertyViolation pv = new PropertyViolation(cons);
            if (engine.isPropertySatisfied(sb.append(sb_rw))) {
                violations.add(pv);
                logger.report("Property " + pv + " Satisfied!", Logger.MSGTYPE.REAL);
            } else {
                potentialviolations.add(pv);
            }

        }

    }

    private void detectProperty(Engine engine, Trace trace, EREProperty property,
            List<String> schedule_prefix) {
        List<ReadEvent> readNodes_rw = trace.getAllReadNodes();
        StringBuilder sb_rw = engine.constructCausalReadWriteConstraintsOptimized(-1, readNodes_rw,
                trace.getIndexedWriteNodes(), trace.getInitialWriteValueMap());

        HashMap<String, HashMap<Integer, List<PropertyNode>>> propertyMonitors = trace
                .getPropertyMonitors();

        Iterator<String> monitorIt = propertyMonitors.keySet().iterator();
        while (monitorIt.hasNext()) {
            String addr = monitorIt.next();
            HashMap<Integer, List<PropertyNode>> indexedPropertyNodeMap = propertyMonitors
                    .get(addr);

            // make sure the monitor is complete

            if (indexedPropertyNodeMap.keySet().size() < property.getSize())
                continue;

            HashSet<ArrayList<PropertyNode>> piset = engine.constructPropertyInstances(property,
                    indexedPropertyNodeMap);

            Iterator<ArrayList<PropertyNode>> piIt = piset.iterator();

            while (piIt.hasNext()) {
                ArrayList<PropertyNode> pi = piIt.next();
                String cons = engine.constructPropertyConstraint(pi, property.isParallel());

                StringBuilder sb = new StringBuilder("(assert\n" + cons + ")\n");
                PropertyViolation pv = new PropertyViolation(pi.toString());
                if (engine.isPropertySatisfied(sb.append(sb_rw))) {
                    violations.add(pv);
                    logger.report("Property " + pv + " Satisfied!", Logger.MSGTYPE.REAL);
                } else {
                    potentialviolations.add(pv);
                }

            }

        }

    }

    /**
     * Race detection method. For every pair of conflicting data accesses, the
     * corresponding race constraint is generated and solved by a solver. If the
     * solver returns a solution, we report a real data race and generate a racy
     * schedule. Otherwise, a potential race is reported. We call it a potential
     * race but not a false race because it might be a real data race in another
     * trace.
     *
     * @param engine
     * @param trace
     * @param schedule_prefix
     */
    private void detectRace(Engine engine, Trace trace, List<String> schedule_prefix) {
        // implement potentialraces to be exact match

        // sometimes we choose an un-optimized way to implement things faster,
        // easier
        // e.g., here we use check, but still enumerate read/write

        Iterator<String> addrIt = trace.getIndexedThreadReadWriteNodes().keySet().iterator();
        while (addrIt.hasNext()) {
            // the dynamic memory location
            String addr = addrIt.next();

            if (config.novolatile) {
                // all field addr should contain ".", not true for array access
                int dotPos = addr.indexOf(".");
                // continue if volatile
                if (dotPos > -1 && trace.isAddressVolatile(addr.substring(dotPos + 1)))
                    continue;
            }

            // get all read nodes on the address
            List<ReadEvent> readnodes = trace.getIndexedReadNodes().get(addr);

            // get all write nodes on the address
            List<WriteEvent> writenodes = trace.getIndexedWriteNodes().get(addr);

            // skip if there is no write events to the address
            if (writenodes == null || writenodes.size() < 1)
                continue;

            {
                // check if local variable
                int size_all = trace.getIndexedThreadReadWriteNodes().get(addr)
                        .get(writenodes.get(0).getTID()).size();
                int size_write = writenodes.size();
                int size_read = 0;
                if (readnodes != null)
                    size_read = readnodes.size();
                if (size_all == size_write + size_read)
                    continue;
            }
            // find equivalent reads and writes by the same thread
            HashMap<MemoryAccessEvent, HashSet<MemoryAccessEvent>> equiMap = new HashMap<MemoryAccessEvent, HashSet<MemoryAccessEvent>>();
            // skip non-primitive and array variables?
            // because we add branch operations before their operations
            if (config.optrace && !addr.contains("_")) {
                // read/write-> set of read/write
                HashMap<Long, List<MemoryAccessEvent>> threadrwnodes = trace
                        .getIndexedThreadReadWriteNodes().get(addr);
                Iterator<Long> tidIt = threadrwnodes.keySet().iterator();
                while (tidIt.hasNext()) {
                    Long tid = tidIt.next();
                    List<MemoryAccessEvent> mnodes = threadrwnodes.get(tid);
                    if (mnodes.size() < 2)
                        continue;
                    MemoryAccessEvent mnode_cur = mnodes.get(0);
                    HashSet<MemoryAccessEvent> equiset = null;

                    int index_cur = trace.getThreadNodesMap().get(tid).indexOf(mnode_cur);

                    for (int k = 1; k < mnodes.size(); k++) {
                        MemoryAccessEvent mnode = mnodes.get(k);
                        if (mnode.getPrevBranchId() < mnode_cur.getGID()) {
                            // check sync id
                            List<AbstractEvent> nodes = trace.getThreadNodesMap().get(tid);
                            int index_end = nodes.indexOf(mnode);
                            int index = index_end - 1;
                            boolean shouldAdd = true;
                            for (; index > index_cur; index--) {
                                AbstractEvent node = nodes.get(index);
                                if (node instanceof SyncEvent) {
                                    shouldAdd = false;
                                    break;
                                }
                            }
                            if (shouldAdd) {
                                if (equiset == null)
                                    equiset = new HashSet<MemoryAccessEvent>();

                                equiset.add(mnode);

                                if (!equiMap.containsKey(mnode_cur))
                                    equiMap.put(mnode_cur, equiset);

                            } else {
                                if (k < mnodes.size() - 1) {
                                    index_cur = index;
                                    mnode_cur = mnode;
                                    equiset = null;
                                }
                            }
                        } else {
                            if (k < mnodes.size() - 1) {
                                index_cur = trace.getThreadNodesMap().get(tid).indexOf(mnode);
                                mnode_cur = mnode;
                                equiset = null;
                            }
                        }
                    }
                }
            }

            // check read-write conflict
            if (readnodes != null)
                for (int i = 0; i < readnodes.size(); i++) {
                    ReadEvent rnode = readnodes.get(i);// read
                    // if(rnode.getGID()==3105224)//3101799
                    // System.out.println("");

                    for (int j = 0; j < writenodes.size(); j++) {
                        WriteEvent wnode = writenodes.get(j);// write

                        // check read and write are by different threads
                        if (rnode.getTID() != wnode.getTID()) {
                            // create a potential race
                            Race race = new Race(trace.getStmtSigIdMap().get(rnode.getID()), trace
                                    .getStmtSigIdMap().get(wnode.getID()), rnode.getID(),
                                    wnode.getID());
                            ExactRace race2 = new ExactRace(race, (int) rnode.getGID(),
                                    (int) wnode.getGID());
                            // skip redundant races with the same signature,
                            // i.e., from same program locations
                            if (config.allrace || !violations.contains(race)
                                    && !potentialviolations.contains(race2))// may
                                                                            // miss
                                                                            // real
                                                                            // violation
                                                                            // with
                                                                            // the
                                                                            // same
                                                                            // signature
                            {

                                // Quick check first: lockset algorithm + weak
                                // HB

                                // lockset algorithm
                                if (engine.hasCommonLock(rnode, wnode))
                                    continue;

                                // weak HB check
                                // a simple reachability analysis to reduce the
                                // solver invocations
                                if (rnode.getGID() < wnode.getGID()) {
                                    if (engine.canReach(rnode, wnode))
                                        continue;
                                } else {
                                    if (engine.canReach(wnode, rnode))
                                        continue;
                                }

                                // if(race.toString().equals("<mergesort.MSort: void DecreaseThreadCounter()>|$i0 = <mergesort.MSort: int m_iCurrentThreadsAlive>|41 - <mergesort.MSort: void DecreaseThreadCounter()>|<mergesort.MSort: int m_iCurrentThreadsAlive> = $i1|41"))
                                // System.out.print("");

                                // If the race passes the quick check, we build
                                // constraints
                                // for it and determine if it is race by solving
                                // the constraints

                                StringBuilder sb;
                                if (config.allconsistent)// all read-write
                                                         // consistency used by
                                                         // the Said approach
                                {
                                    List<ReadEvent> readNodes_rw = trace.getAllReadNodes();
                                    sb = engine.constructCausalReadWriteConstraintsOptimized(
                                            rnode.getGID(), readNodes_rw,
                                            trace.getIndexedWriteNodes(),
                                            trace.getInitialWriteValueMap());
                                } else {

                                    // the following builds the constraints for
                                    // maximal causal model

                                    // get dependent nodes of rnode and wnode
                                    // if w/o branch information, then all read
                                    // nodes that happen-before rnode/wnode are
                                    // considered
                                    // otherwise, only the read nodes that
                                    // before the most recent branch nodes
                                    // before rnode/wnode are considered
                                    List<ReadEvent> readNodes_r = trace.getDependentReadNodes(
                                            rnode, config.branch);
                                    List<ReadEvent> readNodes_w = trace.getDependentReadNodes(
                                            wnode, config.branch);

                                    // construct the optimized read-write
                                    // constraints ensuring the feasibility of
                                    // rnode and wnode
                                    StringBuilder sb1 = engine
                                            .constructCausalReadWriteConstraintsOptimized(
                                                    rnode.getGID(), readNodes_r,
                                                    trace.getIndexedWriteNodes(),
                                                    trace.getInitialWriteValueMap());
                                    StringBuilder sb2 = engine
                                            .constructCausalReadWriteConstraintsOptimized(-1,
                                                    readNodes_w, trace.getIndexedWriteNodes(),
                                                    trace.getInitialWriteValueMap());
                                    // conjunct them
                                    sb = sb1.append(sb2);
                                }

                                // if(race.toString().equals("<benchmarks.raytracer.TournamentBarrier: void DoBarrier(int)>|$z3 = $r2[$i7]|65 - <benchmarks.raytracer.TournamentBarrier: void DoBarrier(int)>|$r3[i0] = z0|76"))
                                // System.out.print("");

                                // query the engine to check rnode/wnode forms a
                                // race or not
                                if (engine.isRace(rnode, wnode, sb)) {
                                    // real race found

                                    logger.report(race.toString(), Logger.MSGTYPE.REAL);// report
                                                                                        // it
                                    if (config.allrace)
                                        violations.add(race2);// save it to
                                                              // violations
                                    else
                                        violations.add(race);

                                    if (equiMap.containsKey(rnode) || equiMap.containsKey(wnode)) {
                                        HashSet<MemoryAccessEvent> nodes1 = new HashSet<MemoryAccessEvent>();
                                        nodes1.add(rnode);
                                        if (equiMap.get(rnode) != null)
                                            nodes1.addAll(equiMap.get(rnode));
                                        HashSet<MemoryAccessEvent> nodes2 = new HashSet<MemoryAccessEvent>();
                                        nodes2.add(wnode);
                                        if (equiMap.get(wnode) != null)
                                            nodes2.addAll(equiMap.get(wnode));

                                        for (Iterator<MemoryAccessEvent> nodesIt1 = nodes1.iterator(); nodesIt1
                                                .hasNext();) {
                                            MemoryAccessEvent node1 = nodesIt1.next();
                                            for (Iterator<MemoryAccessEvent> nodesIt2 = nodes2.iterator(); nodesIt2
                                                    .hasNext();) {
                                                MemoryAccessEvent node2 = nodesIt2.next();
                                                Race r = new Race(trace.getStmtSigIdMap().get(
                                                        node1.getID()), trace.getStmtSigIdMap()
                                                        .get(node2.getID()), node1.getID(),
                                                        node2.getID());
                                                if (violations.add(r))
                                                    logger.report(r.toString(), Logger.MSGTYPE.REAL);

                                            }
                                        }
                                    }

                                    if (config.noschedule)
                                        continue;

                                    // generate the corresponding racey schedule

                                    // for race, there are two schedules:
                                    // rnode before or after wnode
                                    List<String> schedule_a = engine.getSchedule(rnode.getGID(),
                                            trace.getNodeGIDTIdMap(), trace.getThreadIdNameMap());
                                    schedule_a.add(trace.getThreadIdNameMap().get(
                                            trace.getNodeGIDTIdMap().get(wnode.getGID())));

                                    List<String> schedule_b = new ArrayList<String>(schedule_a);

                                    String str1 = schedule_b.remove(schedule_b.size() - 1);

                                    // Due to identical solution to events by
                                    // other threads
                                    // rnode may not be immediately before
                                    // wnode,
                                    // in such a case, we find rnode first and
                                    // then move it to after wnode
                                    int pos = schedule_b.size() - 1;
                                    String str2 = schedule_b.remove(pos);
                                    while (str1 == str2) {
                                        pos--;
                                        schedule_b.add(str2);
                                        str2 = schedule_b.remove(pos);
                                    }

                                    schedule_b.add(str1);
                                    schedule_b.add(str2);

                                    schedule_a.addAll(0, schedule_prefix);
                                    schedule_b.addAll(0, schedule_prefix);

                                    // add the schedules to the race
                                    if (rnode.getGID() < wnode.getGID()) {
                                        race.addSchedule(schedule_a);
                                        race.addSchedule(schedule_b);
                                    } else {
                                        race.addSchedule(schedule_b);
                                        race.addSchedule(schedule_a);
                                    }

                                    // report the schedules
                                    logger.report("Schedule_a: " + trim(schedule_a),
                                            Logger.MSGTYPE.REAL);
                                    logger.report("Schedule_b: " + trim(schedule_b) + "\n",
                                            Logger.MSGTYPE.REAL);

                                } else {
                                    // report potential races

                                    // if we arrive here, it means we find a
                                    // case where
                                    // lockset+happens-before could produce
                                    // false positive
                                    if (potentialviolations.add(race2))
                                        logger.report("Potential " + race2,
                                                Logger.MSGTYPE.POTENTIAL);

                                    if (equiMap.containsKey(rnode) || equiMap.containsKey(wnode)) {
                                        HashSet<MemoryAccessEvent> nodes1 = new HashSet<MemoryAccessEvent>();
                                        nodes1.add(rnode);
                                        if (equiMap.get(rnode) != null)
                                            nodes1.addAll(equiMap.get(rnode));
                                        HashSet<MemoryAccessEvent> nodes2 = new HashSet<MemoryAccessEvent>();
                                        nodes2.add(wnode);
                                        if (equiMap.get(wnode) != null)
                                            nodes2.addAll(equiMap.get(wnode));

                                        for (Iterator<MemoryAccessEvent> nodesIt1 = nodes1.iterator(); nodesIt1
                                                .hasNext();) {
                                            MemoryAccessEvent node1 = nodesIt1.next();
                                            for (Iterator<MemoryAccessEvent> nodesIt2 = nodes2.iterator(); nodesIt2
                                                    .hasNext();) {
                                                MemoryAccessEvent node2 = nodesIt2.next();

                                                ExactRace r = new ExactRace(trace.getStmtSigIdMap()
                                                        .get(node1.getID()), trace
                                                        .getStmtSigIdMap().get(node2.getID()),
                                                        (int) node1.getGID(), (int) node2.getGID());
                                                if (potentialviolations.add(r))
                                                    logger.report("Potential " + r,
                                                            Logger.MSGTYPE.POTENTIAL);

                                            }
                                        }
                                    }

                                }

                            }
                        }
                    }
                }
            // check race write-write
            if (writenodes.size() > 1)
                for (int i = 0; i < writenodes.size(); i++)// skip the initial
                                                           // write node
                {
                    WriteEvent wnode1 = writenodes.get(i);

                    for (int j = 0; j != i && j < writenodes.size(); j++) {
                        WriteEvent wnode2 = writenodes.get(j);
                        if (wnode1.getTID() != wnode2.getTID()) {
                            Race race = new Race(trace.getStmtSigIdMap().get(wnode1.getID()), trace
                                    .getStmtSigIdMap().get(wnode2.getID()), wnode1.getID(),
                                    wnode2.getID());
                            ExactRace race2 = new ExactRace(race, (int) wnode1.getGID(),
                                    (int) wnode2.getGID());

                            if (config.allrace || !violations.contains(race)
                                    && !potentialviolations.contains(race2))//
                            {
                                if (engine.hasCommonLock(wnode1, wnode2))
                                    continue;

                                if (wnode1.getGID() < wnode2.getGID()) {
                                    if (engine.canReach(wnode1, wnode2))
                                        continue;
                                } else {
                                    if (engine.canReach(wnode2, wnode1))
                                        continue;
                                }

                                StringBuilder sb;
                                if (config.allconsistent) {
                                    List<ReadEvent> readNodes_ww = trace.getAllReadNodes();
                                    sb = engine.constructCausalReadWriteConstraintsOptimized(-1,
                                            readNodes_ww, trace.getIndexedWriteNodes(),
                                            trace.getInitialWriteValueMap());
                                } else {
                                    // get dependent nodes of rnode and wnode
                                    List<ReadEvent> readNodes_w1 = trace.getDependentReadNodes(
                                            wnode1, config.branch);
                                    List<ReadEvent> readNodes_w2 = trace.getDependentReadNodes(
                                            wnode2, config.branch);

                                    StringBuilder sb1 = engine
                                            .constructCausalReadWriteConstraintsOptimized(-1,
                                                    readNodes_w1, trace.getIndexedWriteNodes(),
                                                    trace.getInitialWriteValueMap());
                                    StringBuilder sb2 = engine
                                            .constructCausalReadWriteConstraintsOptimized(-1,
                                                    readNodes_w2, trace.getIndexedWriteNodes(),
                                                    trace.getInitialWriteValueMap());
                                    sb = sb1.append(sb2);
                                }
                                // TODO: NEED to ensure that the other
                                // non-dependent nodes by other threads are not
                                // included
                                if (engine.isRace(wnode1, wnode2, sb)) {
                                    logger.report(race.toString(), Logger.MSGTYPE.REAL);

                                    if (config.allrace)
                                        violations.add(race2);// save it to
                                                              // violations
                                    else
                                        violations.add(race);

                                    if (equiMap.containsKey(wnode1) || equiMap.containsKey(wnode2)) {
                                        HashSet<MemoryAccessEvent> nodes1 = new HashSet<MemoryAccessEvent>();
                                        nodes1.add(wnode1);
                                        if (equiMap.get(wnode1) != null)
                                            nodes1.addAll(equiMap.get(wnode1));
                                        HashSet<MemoryAccessEvent> nodes2 = new HashSet<MemoryAccessEvent>();
                                        nodes2.add(wnode2);
                                        if (equiMap.get(wnode2) != null)
                                            nodes2.addAll(equiMap.get(wnode2));

                                        for (Iterator<MemoryAccessEvent> nodesIt1 = nodes1.iterator(); nodesIt1
                                                .hasNext();) {
                                            MemoryAccessEvent node1 = nodesIt1.next();
                                            for (Iterator<MemoryAccessEvent> nodesIt2 = nodes2.iterator(); nodesIt2
                                                    .hasNext();) {
                                                MemoryAccessEvent node2 = nodesIt2.next();
                                                Race r = new Race(trace.getStmtSigIdMap().get(
                                                        node1.getID()), trace.getStmtSigIdMap()
                                                        .get(node2.getID()), node1.getID(),
                                                        node2.getID());
                                                if (violations.add(r))
                                                    logger.report(r.toString(), Logger.MSGTYPE.REAL);

                                            }
                                        }
                                    }

                                    if (config.noschedule)
                                        continue;

                                    List<String> schedule_a = engine.getSchedule(wnode1.getGID(),
                                            trace.getNodeGIDTIdMap(), trace.getThreadIdNameMap());
                                    schedule_a.add(trace.getThreadIdNameMap().get(
                                            trace.getNodeGIDTIdMap().get(wnode2.getGID())));

                                    List<String> schedule_b = new ArrayList<String>(schedule_a);

                                    String str1 = schedule_b.remove(schedule_b.size() - 1);
                                    int pos = schedule_b.size() - 1;
                                    String str2 = schedule_b.remove(pos);
                                    while (str1 == str2) {
                                        pos--;
                                        schedule_b.add(str2);
                                        str2 = schedule_b.remove(pos);
                                    }

                                    schedule_b.add(str1);
                                    schedule_b.add(str2);

                                    schedule_a.addAll(0, schedule_prefix);
                                    schedule_b.addAll(0, schedule_prefix);

                                    if (wnode1.getGID() < wnode2.getGID()) {
                                        race.addSchedule(schedule_a);
                                        race.addSchedule(schedule_b);
                                    } else {
                                        race.addSchedule(schedule_b);
                                        race.addSchedule(schedule_a);
                                    }

                                    logger.report("Schedule_a: " + trim(schedule_a),
                                            Logger.MSGTYPE.REAL);
                                    logger.report("Schedule_b: " + trim(schedule_b) + "\n",
                                            Logger.MSGTYPE.REAL);

                                } else {
                                    // if we arrive here, it means we find a
                                    // case where lockset+happens-before could
                                    // produce false positive
                                    if (potentialviolations.add(race2))
                                        logger.report("Potential " + race2,
                                                Logger.MSGTYPE.POTENTIAL);

                                    if (equiMap.containsKey(wnode1) || equiMap.containsKey(wnode2)) {
                                        HashSet<MemoryAccessEvent> nodes1 = new HashSet<MemoryAccessEvent>();
                                        nodes1.add(wnode1);
                                        if (equiMap.get(wnode1) != null)
                                            nodes1.addAll(equiMap.get(wnode1));
                                        HashSet<MemoryAccessEvent> nodes2 = new HashSet<MemoryAccessEvent>();
                                        nodes2.add(wnode2);
                                        if (equiMap.get(wnode2) != null)
                                            nodes2.addAll(equiMap.get(wnode2));

                                        for (Iterator<MemoryAccessEvent> nodesIt1 = nodes1.iterator(); nodesIt1
                                                .hasNext();) {
                                            MemoryAccessEvent node1 = nodesIt1.next();
                                            for (Iterator<MemoryAccessEvent> nodesIt2 = nodes2.iterator(); nodesIt2
                                                    .hasNext();) {
                                                MemoryAccessEvent node2 = nodesIt2.next();
                                                ExactRace r = new ExactRace(trace.getStmtSigIdMap()
                                                        .get(node1.getID()), trace
                                                        .getStmtSigIdMap().get(node2.getID()),
                                                        (int) node1.getGID(), (int) node2.getGID());
                                                if (potentialviolations.add(r))
                                                    logger.report("Potential " + r,
                                                            Logger.MSGTYPE.POTENTIAL);

                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Detect atomicty violations. Not used for race detection.
     *
     * @param engine
     * @param trace
     * @param schedule_prefix
     */
    private void detectAtomicityViolation(Engine engine, Trace trace, List<String> schedule_prefix) {

        HashMap<String, HashMap<Long, List<MemoryAccessEvent>>> indexedThreadReadWriteNodes = trace
                .getIndexedThreadReadWriteNodes();
        Iterator<Entry<String, List<ReadEvent>>> entryIt = trace.getIndexedReadNodes().entrySet()
                .iterator();
        while (entryIt.hasNext()) {
            Entry<String, List<ReadEvent>> entry = entryIt.next();
            String addr = entry.getKey();

            // get all write nodes on the address
            List<WriteEvent> writenodes = trace.getIndexedWriteNodes().get(addr);
            if (writenodes == null || writenodes.size() < 1)
                continue;

            // check atomicity-violation all nodes
            HashMap<Long, List<MemoryAccessEvent>> threadReadWriteNodes = indexedThreadReadWriteNodes
                    .get(addr);

            Object[] threads = threadReadWriteNodes.keySet().toArray();

            for (int i = 0; i < threads.length - 1; i++)
                for (int j = i + 1; j < threads.length; j++) {
                    List<MemoryAccessEvent> rwnodes1 = threadReadWriteNodes.get(threads[i]);

                    List<MemoryAccessEvent> rwnodes2 = threadReadWriteNodes.get(threads[j]);

                    if (rwnodes1 != null & rwnodes2 != null && rwnodes1.size() > 1) {
                        for (int k = 0; k < rwnodes1.size() - 1; k++) {
                            // require atomic region specification
                            MemoryAccessEvent node1 = rwnodes1.get(k);
                            MemoryAccessEvent node2 = rwnodes1.get(k + 1);

                            for (int m = 0; m < rwnodes2.size(); m++) {
                                MemoryAccessEvent node3 = rwnodes2.get(m);

                                if (node1.getType() == EventType.WRITE || node2.getType() == EventType.WRITE
                                        || node3.getType() == EventType.WRITE) {
                                    AtomicityViolation av = new AtomicityViolation(trace
                                            .getStmtSigIdMap().get(node1.getID()), trace
                                            .getStmtSigIdMap().get(node3.getID()), trace
                                            .getStmtSigIdMap().get(node2.getID()), node1.getID(),
                                            node2.getID(), node3.getID());

                                    if (!violations.contains(av))// &&!potentialviolations.contains(av)
                                    {

                                        // it's possible that av is true in the
                                        // original execution
                                        // no need to model its constraints
                                        // TODO: optimize it!

                                        // TODO: Need to check lock-history
                                        if (engine.isAtomic(node1, node2, node3))
                                            continue;

                                        if (node3.getGID() < node1.getGID()) {
                                            if (engine.canReach((AbstractEvent) node3,
                                                    (AbstractEvent) node1))
                                                continue;
                                        } else if (node3.getGID() > node2.getGID()) {
                                            if (engine.canReach((AbstractEvent) node2,
                                                    (AbstractEvent) node3))
                                                continue;
                                        }

                                        // get dependent read nodes
                                        List<ReadEvent> readNodes_1 = trace.getDependentReadNodes(
                                                node1, config.branch);
                                        List<ReadEvent> readNodes_2 = trace.getDependentReadNodes(
                                                node2, config.branch);
                                        List<ReadEvent> readNodes_3 = trace.getDependentReadNodes(
                                                node3, config.branch);

                                        StringBuilder sb1 = engine
                                                .constructCausalReadWriteConstraintsOptimized(
                                                        node1.getGID(), readNodes_1,
                                                        trace.getIndexedWriteNodes(),
                                                        trace.getInitialWriteValueMap());
                                        StringBuilder sb2 = engine
                                                .constructCausalReadWriteConstraintsOptimized(
                                                        node2.getGID(), readNodes_2,
                                                        trace.getIndexedWriteNodes(),
                                                        trace.getInitialWriteValueMap());
                                        StringBuilder sb3 = engine
                                                .constructCausalReadWriteConstraintsOptimized(
                                                        node3.getGID(), readNodes_3,
                                                        trace.getIndexedWriteNodes(),
                                                        trace.getInitialWriteValueMap());

                                        if (engine.isAtomicityViolation(node1, node2, node3, sb1,
                                                sb2, sb3)) {

                                            logger.report("Atomicity Violation: " + av,
                                                    Logger.MSGTYPE.REAL);

                                            violations.add(av);

                                            if (config.noschedule)
                                                continue;

                                            List<String> schedule = engine.getSchedule(
                                                    node3.getGID(), trace.getNodeGIDTIdMap(),
                                                    trace.getThreadIdNameMap());

                                            schedule.addAll(0, schedule_prefix);

                                            av.addSchedule(schedule);
                                            logger.report("Schedule: " + trim(schedule) + "\n",
                                                    Logger.MSGTYPE.REAL);
                                        } else {
                                            // if we arrive here, it means we
                                            // find a case where
                                            // lockset+happens-before could
                                            // produce false positive
                                            logger.report("Potential Atomicity Violation: " + av
                                                    + "\n", Logger.MSGTYPE.POTENTIAL);
                                            potentialviolations.add(av);

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * The input is the application name and the optional options
     *
     * @param args
     */
    public static void main(String[] args) {
        Configuration config = new Configuration();
        config.parseArguments(args, true);
        config.outdir = "./log";
        NewRVPredict predictor = new NewRVPredict();
        predictor.initPredict(config);
        predictor.addHooks();
        predictor.run();
    }

    public void run() {
        try {

            // this is used to maintain the schedule in the previous windows
            List<String> schedule_prefix = new ArrayList<>();

            // z3 engine is used for interacting with constraints
            Engine engine = new EngineSMTLIB1(config);

            // map from memory address to the initial value
            HashMap<String, Long> initialWriteValueMap = new HashMap<>();

            // process the trace window by window
            for (int round = 0; round * config.window_size < totalTraceLength; round++) {
                long index_start = round * config.window_size + 1;
                long index_end = (round + 1) * config.window_size;
                // if(totalTraceLength>rvpredict.config.window_size)System.out.println("***************** Round "+(round+1)+": "+index_start+"-"+index_end+"/"+totalTraceLength+" ******************\n");

                // load trace
                Trace trace = dbEngine.getTrace(index_start, index_end, traceInfo);

                // starting from the second window, the initial value map
                // becomes
                // the last write map in the last window
                if (round > 0)
                    trace.setInitialWriteValueMap(initialWriteValueMap);

                // OPT: if #sv==0 or #shared rw ==0 continue
                if (trace.mayRace()) {
                    // Now, construct the constraints

                    // 1. declare all variables
                    engine.declareVariables(trace.getFullTrace());
                    // 2. intra-thread order for all nodes, excluding branches
                    // and basic block transitions
                    if (config.rmm_pso)// TODO: add intra order between sync
                        engine.addPSOIntraThreadConstraints(trace.getIndexedThreadReadWriteNodes());
                    else
                        engine.addIntraThreadConstraints(trace.getThreadNodesMap());

                    // 3. order for locks, signals, fork/joins
                    engine.addSynchronizationConstraints(trace, trace.getSyncNodesMap(),
                            trace.getThreadFirstNodeMap(), trace.getThreadLastNodeMap());

                    // 4. match read-write
                    // This is only used for constructing all read-write
                    // consistency constraints

                    // engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());
                    // engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());

                    if (detectRace)// detecting races
                    {
                        detectRace(engine, trace, schedule_prefix);
                    }
                    if (detectAtomicityViolation)// detecting atomicity
                                                 // violations
                    {
                        detectAtomicityViolation(engine, trace, schedule_prefix);
                    }
                    if (detectDeadlock)// when detecting deadlocks, make sure
                                       // the lock constraints of the rest
                                       // unlock nodes are disabled
                    {
                        detectDeadlock(engine, trace, schedule_prefix);
                    }
                    if (detectProperty && trace.getPropertyMonitors().size() > 0) {

                        EREProperty property = engine.getProperty();
                        if (property == null) {
                            HashMap<String, Integer> map = dbEngine.getProperty();
                            property = engine.initProperty(map, trace);
                        }
                        if (property.getPropertySize() == 8)
                            detectDeadlockProperty(engine, trace, property, schedule_prefix);
                        else
                            detectProperty(engine, trace, property, schedule_prefix);

                    }
                }
                // get last write value from the current trace
                // as the initial value for the next round
                initialWriteValueMap = trace.getInitialWriteValueMap();
                trace.saveLastWriteValues(initialWriteValueMap);

                // append the schedule in the current trace to schedule_prefix
                // which maybe used in the next window for generating racey
                // schedules
                if (!config.noschedule && (round + 1) * config.window_size < totalTraceLength)
                    schedule_prefix.addAll(getTraceSchedule(trace));
            }

            if (!config.noschedule) {
                // save schedules to db
                int size = violations.size();
                if (size > 0) {
                    dbEngine.createScheduleTable();
                    dbEngine.saveSchedulesToDB(violations);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // terminate
            System.exit(0);
        }

    }

    public void initPredict(Configuration conf) {
        config = conf;
        logger = config.logger;

        // Now let's start predict analysis
        startTime = System.currentTimeMillis();

        // db engine is used for interacting with database
        dbEngine = new DBEngine(config.outdir, config.tableName);

        // load all the metadata in the application
        dbEngine.getMetadata(threadIdNameMap, sharedVarIdSigMap, volatileAddresses, stmtIdSigMap);

        // the total number of events in the trace
        totalTraceLength = 0;
        try {
            totalTraceLength = dbEngine.getTraceSize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        traceInfo = new TraceInfo(sharedVarIdSigMap, volatileAddresses, stmtIdSigMap,
                threadIdNameMap);
    }

    public void addHooks() {
        ExecutionInfoTask task = new ExecutionInfoTask(startTime, traceInfo, totalTraceLength);
        // register a shutdown hook to store runtime statistics
        Runtime.getRuntime().addShutdownHook(task);

        // set a timer to timeout in a configured period
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.report("\n******* Timeout " + config.timeout + " seconds ******",
                        Logger.MSGTYPE.REAL);// report it
                System.exit(0);
            }
        }, config.timeout * 1000);
    }

    /**
     * Return the schedule, i.e., the thread execution order, of the trace
     *
     * @param trace
     * @return
     */
    private static List<String> getTraceSchedule(Trace trace) {

        List<String> fullschedule = new ArrayList<>();

        for (int k = 0; k < trace.getFullTrace().size(); k++)
            fullschedule.add(trace.getThreadIdNameMap().get(trace.getFullTrace().get(k).getTID()));

        return fullschedule;
    }

    class ExecutionInfoTask extends Thread {
        TraceInfo info;
        long start_time;
        long TOTAL_TRACE_LENGTH;

        ExecutionInfoTask(long st, TraceInfo info, long size) {
            this.info = info;
            this.start_time = st;
            this.TOTAL_TRACE_LENGTH = size;
        }

        @Override
        public void run() {

            // Report statistics about the trace and race detection

            // TODO: query the following information from DB may be expensive

            int TOTAL_THREAD_NUMBER = info.getTraceThreadNumber();
            int TOTAL_SHAREDVARIABLE_NUMBER = info.getTraceSharedVariableNumber();
            int TOTAL_BRANCH_NUMBER = info.getTraceBranchNumber();
            int TOTAL_SHAREDREADWRITE_NUMBER = info.getTraceSharedReadWriteNumber();
            int TOTAL_LOCALREADWRITE_NUMBER = info.getTraceLocalReadWriteNumber();
            int TOTAL_INITWRITE_NUMBER = info.getTraceInitWriteNumber();

            int TOTAL_SYNC_NUMBER = info.getTraceSyncNumber();
            int TOTAL_PROPERTY_NUMBER = info.getTracePropertyNumber();

            if (violations.size() == 0)
                logger.report("No races found.", Logger.MSGTYPE.INFO);
            else {
                logger.report("Trace Size: " + TOTAL_TRACE_LENGTH, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Threads: " + TOTAL_THREAD_NUMBER, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #SharedVariables: " + TOTAL_SHAREDVARIABLE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Shared Read-Writes: " + TOTAL_SHAREDREADWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Local Read-Writes: " + TOTAL_LOCALREADWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Initial Writes: " + TOTAL_INITWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Synchronizations: " + TOTAL_SYNC_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Branches: " + TOTAL_BRANCH_NUMBER, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Property Events: " + TOTAL_PROPERTY_NUMBER,
                        Logger.MSGTYPE.STATISTICS);

                logger.report("Total #Potential Violations: "
                        + (potentialviolations.size() + violations.size()),
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Real Violations: " + violations.size(),
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total Time: " + (System.currentTimeMillis() - start_time) + "ms",
                        Logger.MSGTYPE.STATISTICS);
                // System.out.println("Total #Schedules: "+size_schedule);
            }

            logger.closePrinter();

        }

    }

}
