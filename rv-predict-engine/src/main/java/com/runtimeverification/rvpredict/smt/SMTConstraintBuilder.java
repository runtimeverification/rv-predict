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
package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;
import com.runtimeverification.rvpredict.trace.SyncEvent;
import com.runtimeverification.rvpredict.trace.LockRegion;
import com.runtimeverification.rvpredict.trace.ReadEvent;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.WriteEvent;
import com.runtimeverification.rvpredict.graph.LockSetEngine;
import com.runtimeverification.rvpredict.graph.ReachabilityEngine;

import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.util.Constants;

public class SMTConstraintBuilder {

    private static AtomicInteger id = new AtomicInteger();// constraint id
    private SMTTaskRun task;

    private final Configuration config;

    private final Trace trace;

    private final ReachabilityEngine reachEngine = new ReachabilityEngine();
    private final LockSetEngine lockEngine = new LockSetEngine();

    private final Map<MemoryAccessEvent, StringBuilder> abstractPhi = Maps.newHashMap();
    private final Map<MemoryAccessEvent, StringBuilder> concretePhi = Maps.newHashMap();

    /**
     * Avoids infinite recursion when building the abstract feasibility
     * constraint of a {@link MemoryAccessEvent}.
     */
    private final Set<MemoryAccessEvent> computedAbstractPhi = Sets.newHashSet();

    /**
     * Avoids infinite recursion when building the concrete feasibility
     * constraint of a {@link MemoryAccessEvent}.
     */
    private final Set<MemoryAccessEvent> computedConcretePhi = Sets.newHashSet();

    // constraints below
    private final StringBuilder smtlibDecl = new StringBuilder(":extrafuns (\n");
    private final StringBuilder smtlibAssertion = new StringBuilder(":formula (and \n");
    private static final String CONS_SETLOGIC = ":logic QF_IDL\n";

    private final String benchname;

    public SMTConstraintBuilder(Configuration config, Trace trace) {
        this.config = config;
        this.trace = trace;
        benchname = "(benchmark " + config.tableName + ".smt\n";
    }

    private static String makeOrderVariable(Event event) {
        return "o" + event.getGID();
    }

    private String makeConcretePhiVariable(MemoryAccessEvent event) {
        return "phi_c" + event.getGID();
    }

    private String makeAbstractPhiVariable(MemoryAccessEvent event) {
        return "phi_a" + event.getGID();
    }

    /**
     * Declares an order variable for each event.
     */
    public void declareVariables() {
        for (Event e : trace.getAllEvents()) {
            smtlibDecl.append(String.format("(%s Int)\n", makeOrderVariable(e)));
            if (e instanceof MemoryAccessEvent) {
                smtlibDecl.append(String.format("(%s Bool)\n",
                        makeAbstractPhiVariable((MemoryAccessEvent) e)));
                smtlibDecl.append(String.format("(%s Bool)\n",
                        makeConcretePhiVariable((MemoryAccessEvent) e)));
            }
        }
        smtlibDecl.append(")\n");
    }

    private void assertHappensBefore(Event e1, Event e2) {
        smtlibAssertion.append(getAsstHappensBefore(e1, e2));
        reachEngine.addEdge(e1, e2);
    }

    private String getAsstHappensBefore(Event... events) {
        StringBuilder sb = new StringBuilder("(<");
        for (Event event : events) {
            sb.append(" ");
            sb.append(makeOrderVariable(event));
        }
        sb.append(")");
        return sb.toString();
    }

    private String getAsstLockRegionHappensBefore(LockRegion lockRegion1, LockRegion lockRegion2) {
        SyncEvent unlock = lockRegion1.getUnlock();
        SyncEvent lock = lockRegion2.getLock();
        return getAsstHappensBefore(
                unlock != null ? unlock : trace.getLastThreadEvent(lockRegion1.getThreadId()),
                lock != null ? lock : trace.getFirstThreadEvent(lockRegion2.getThreadId()));
    }

    private void assertMutualExclusion(LockRegion lockRegion1, LockRegion lockRegion2) {
        smtlibAssertion.append(String.format("(or %s %s)\n",
                getAsstLockRegionHappensBefore(lockRegion1, lockRegion2),
                getAsstLockRegionHappensBefore(lockRegion2, lockRegion1)));
    }

    /**
     * Adds intra-thread must happens-before (MHB) constraints of sequential
     * consistent memory model.
     */
    public void addIntraThreadConstraints() {
        for (List<Event> events : trace.getThreadIdToEventsMap().values()) {
            Event prevEvent = events.get(0);
            for (Event crntEvent : events.subList(1, events.size())) {
                assertHappensBefore(prevEvent, crntEvent);
                prevEvent = crntEvent;
            }
        }
    }

    /**
     * Adds intra-thread must happens-before (MHB) constraints of relaxed PSO
     * memory model.
     */
    public void addPSOIntraThreadConstraints() {
        for (List<MemoryAccessEvent> nodes : trace.getMemAccessEventsTable().values()) {
            MemoryAccessEvent prevEvent = nodes.get(0);
            for (MemoryAccessEvent crntEvent : nodes.subList(1, nodes.size())) {
                assertHappensBefore(prevEvent, crntEvent);
                prevEvent = crntEvent;
            }
        }
    }

    /**
     * Adds program order and thread start/join constraints, that is, the must
     * happens-before constraints (MHB) in the paper.
     */
    public void addProgramOrderAndThreadStartJoinConstraints() {
        for (List<SyncEvent> startOrJoinEvents : trace.getThreadIdToStartJoinEvents().values()) {
            for (SyncEvent startOrJoin : startOrJoinEvents) {
                long tid = startOrJoin.getSyncObject();
                switch (startOrJoin.getType()) {
                case START:
                    Event fstThrdEvent = trace.getFirstThreadEvent(tid);
                    /* YilongL: it's possible that the first event of the new
                     * thread is not in the current trace */
                    if (fstThrdEvent != null) {
                        assertHappensBefore(startOrJoin, fstThrdEvent);
                    }
                    break;
                case JOIN:
                    if (startOrJoin.getType() == EventType.JOIN) {
                        Event lastThrdEvent = trace.getLastThreadEvent(tid);
                        /* YilongL: it's possible that the last event of the thread
                         * to join is not in the current trace */
                        if (lastThrdEvent != null) {
                            assertHappensBefore(lastThrdEvent, startOrJoin);
                        }
                    }
                    break;
                case PRE_JOIN:
                case JOIN_MAYBE_FAILED:
                    break;
                default:
                    assert false : "unexpected event: " + startOrJoin;
                }
            }
        }
    }

    /**
     * Adds lock mutual exclusion constraints.
     */
    public void addLockingConstraints() {
        /* enumerate the locking events on each lock */
        for (List<SyncEvent> syncEvents : trace.getLockObjToSyncEvents().values()) {
            Map<Long, SyncEvent> threadIdToPrevLockOrUnlock = Maps.newHashMap();
            List<LockRegion> lockRegions = Lists.newArrayList();

            for (SyncEvent syncEvent : syncEvents) {
                long tid = syncEvent.getTID();
                SyncEvent prevLockOrUnlock = threadIdToPrevLockOrUnlock.get(tid);
                assert prevLockOrUnlock == null
                    || !(prevLockOrUnlock.isLockEvent() && syncEvent.isLockEvent())
                    || !(prevLockOrUnlock.isUnlockEvent() && syncEvent.isUnlockEvent()) :
                    "Unexpected consecutive lock/unlock events:\n" + prevLockOrUnlock + ", " + syncEvent;

                switch (syncEvent.getType()) {
                case WRITE_LOCK:
                case READ_LOCK:
                case WAIT_ACQ:
                    threadIdToPrevLockOrUnlock.put(tid, syncEvent);
                    break;

                case WRITE_UNLOCK:
                case READ_UNLOCK:
                case WAIT_REL:
                    lockRegions.add(new LockRegion(threadIdToPrevLockOrUnlock.put(tid, syncEvent),
                            syncEvent));
                    break;
                default:
                    assert false : "Unexpected synchronization event: " + syncEvent;
                }
            }

            for (SyncEvent lockOrUnlock : threadIdToPrevLockOrUnlock.values()) {
                if (lockOrUnlock.isLockEvent()) {
                    SyncEvent lock = lockOrUnlock;
                    lockRegions.add(new LockRegion(lock, null));
                }
            }

            lockEngine.addAll(lockRegions);

            /* assert lock regions mutual exclusion */
            assertLockMutex(lockRegions);
        }
    }

    private void assertLockMutex(List<LockRegion> lockRegions) {
        for (LockRegion lockRegion1 : lockRegions) {
            for (LockRegion lockRegion2 : lockRegions) {
                if (lockRegion1.getThreadId() < lockRegion2.getThreadId()
                        && (lockRegion1.isWriteLocked() || lockRegion2.isWriteLocked())) {
                    assertMutualExclusion(lockRegion1, lockRegion2);
                }
            }
        }
    }

    /**
     * Generates a formula ensuring that all read events that {@code event}
     * depends on read the same value as in the original trace, to guarantee
     * {@code event} will be generated in the predicted trace. Note that,
     * however, this {@code event} is allowed to read or write a different value
     * than in the original trace.
     */
    public String getAbstractFeasibilityConstraint(MemoryAccessEvent event) {
        if (computedAbstractPhi.contains(event)) {
            return makeAbstractPhiVariable(event);
        }
        computedAbstractPhi.add(event);

        StringBuilder phi = new StringBuilder("(and true ");
        /* make sure that every dependent read event reads the same value as in the original trace */
        for (ReadEvent depRead : trace.getCtrlFlowDependentEvents(event)) {
            phi.append(getConcreteFeasibilityConstraint(depRead)).append(" ");
        }
        phi.append(")");
        abstractPhi.put(event, phi);
        return makeAbstractPhiVariable(event);
    }

    /**
     * Generates a formula ensuring that {@code event} will be generated exactly
     * the same as in the original trace <b>if</b> the corresponding data-abstract
     * feasibility constraint is also satisfied.
     */
    private String getConcreteFeasibilityConstraint(MemoryAccessEvent event) {
        if (computedConcretePhi.contains(event)) {
            return makeConcretePhiVariable(event);
        } else if (event.getValue() == Constants._0X_DEADBEEFL) {
            return "true";
        }
        computedConcretePhi.add(event);

        StringBuilder phi;
        if (event instanceof ReadEvent) {
            List<WriteEvent> writeEvents = trace.getWriteEventsOn(event.getAddr());

            /* thread immediate write predecessor */
            WriteEvent thrdImdWrtPred = null;
            /* predecessor write set: all write events whose values could be read by `depRead' */
            List<WriteEvent> predWriteSet = Lists.newArrayList();
            for (WriteEvent write : writeEvents) {
                if (write.getTID() == event.getTID()) {
                    if (write.getGID() < event.getGID()) {
                        thrdImdWrtPred = write;
                    }
                } else if (!happensBefore(event, write)) {
                    predWriteSet.add(write);
                }
            }
            if (thrdImdWrtPred != null) {
                predWriteSet.add(thrdImdWrtPred);
            }

            /* predecessor write set of same value */
            List<WriteEvent> sameValPredWriteSet = Lists.newArrayList();
            for (WriteEvent write : predWriteSet) {
                if (write.getValue() == event.getValue()) {
                    sameValPredWriteSet.add(write);
                }
            }

            /* case 1: the dependent read reads the initial value */
            StringBuilder case1 = new StringBuilder("false");
            if (thrdImdWrtPred == null &&
                    trace.getInitValueOf(event.getAddr()) == event.getValue()) {
                case1 = new StringBuilder("(and true ");
                for (WriteEvent write : predWriteSet) {
                    case1.append(getAsstHappensBefore(event, write));
                }
                case1.append(")");
            }

            /* case 2: the dependent read reads a previously written value */
            StringBuilder case2 = new StringBuilder("(or false ");
            for (WriteEvent write : sameValPredWriteSet) {
                case2.append("(and ");
                case2.append(getAbstractFeasibilityConstraint(write)).append(" ")
                     .append(getConcreteFeasibilityConstraint(write)).append(" ");
                case2.append(getAsstHappensBefore(write, event));
                for (WriteEvent otherWrite : writeEvents) {
                    if (write != otherWrite && !happensBefore(otherWrite, write)
                            && !happensBefore(event, otherWrite)) {
                        case2.append(String.format("(or %s %s)",
                                getAsstHappensBefore(otherWrite, write),
                                getAsstHappensBefore(event, otherWrite)));
                    }
                }
                case2.append(")");
            }
            case2.append(")");
            phi = new StringBuilder("(or ").append(case1).append(" ").append(case2)
                    .append(")");
        } else {
            phi = new StringBuilder("(and true ");
            for (ReadEvent e : trace.getExtraDataFlowDependentEvents(event)) {
                phi.append(getAbstractFeasibilityConstraint(e)).append(" ")
                   .append(getConcreteFeasibilityConstraint(e)).append(" ");
            }
            phi.append(")");
        }
        concretePhi.put(event, phi);

        return makeConcretePhiVariable(event);
    }

    /**
     * Checks if two {@code MemoryAccessEvent} hold a common lock.
     */
    public boolean hasCommonLock(MemoryAccessEvent e1, MemoryAccessEvent e2) {
        return lockEngine.hasCommonLock(e1, e2);
    }

    /**
     * Checks if one event happens before another.
     */
    public boolean happensBefore(Event e1, Event e2) {
        return reachEngine.canReach(e1.getGID(), e2.getGID());

    }

    public boolean isSat() {
        int id = SMTConstraintBuilder.id.incrementAndGet();
        task = new SMTTaskRun(config, id);

        StringBuilder msg = new StringBuilder(benchname).append(CONS_SETLOGIC)
                .append(smtlibDecl).append(smtlibAssertion).append(")");
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public boolean isRace(Event e1, Event e2, CharSequence casualConstraint) {
        int id = SMTConstraintBuilder.id.incrementAndGet();
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(benchname).append(CONS_SETLOGIC).append(smtlibDecl);
        msg.append(smtlibAssertion);
        for (Entry<MemoryAccessEvent, StringBuilder> entry : abstractPhi.entrySet()) {
            msg.append("(= ").append(makeAbstractPhiVariable(entry.getKey())).append(" ")
                    .append(entry.getValue()).append(")");
        }
        for (Entry<MemoryAccessEvent, StringBuilder> entry : concretePhi.entrySet()) {
            msg.append("(= ").append(makeConcretePhiVariable(entry.getKey())).append(" ")
                    .append(entry.getValue()).append(")");
        }
        msg.append(String.format("(= %s %s)", makeOrderVariable(e1), makeOrderVariable(e2)))
           .append(casualConstraint).append("))");
        task.sendMessage(msg.toString());
        return task.sat;
    }

}
