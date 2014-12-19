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

import rvpredict.trace.Event;
import rvpredict.trace.EventType;
import rvpredict.trace.MemoryAccessEvent;
import rvpredict.trace.SyncEvent;
import rvpredict.trace.LockRegion;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.WriteEvent;
import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.List;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import rvpredict.config.Configuration;

public class SMTConstraintBuilder {

    private int id = 0;// constraint id
    private SMTTaskRun task;

    private final Configuration config;

    private final Trace trace;

    private final ReachabilityEngine reachEngine = new ReachabilityEngine();
    private final LockSetEngine lockEngine = new LockSetEngine();

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

    private static String makeMatchVariable(Event event) {
        return "m" + event.getGID();
    }

    /**
     * Declares an order variable for each event.
     */
    public void declareVariables() {
        for (Event e : trace.getAllEvents()) {
            /* introduce a match variable for each NOTIFY event */
            if (e.getType() == EventType.NOTIFY) {
                smtlibDecl.append(String.format("(%s Int)\n", makeMatchVariable(e)));
            }
            smtlibDecl.append(String.format("(%s Int)\n", makeOrderVariable(e)));
        }

        /* match variables for notify events from previous windows */
        for (Event e : trace.getAllUnmatchedNotifyEvents()) {
            if (e.getType() == EventType.NOTIFY) {
                smtlibDecl.append(String.format("(%s Int)\n", makeMatchVariable(e)));
            }
        }

        smtlibDecl.append(")\n");
    }

    private void assertHappensBefore(Event e1, Event e2) {
        smtlibAssertion.append(String.format("(< %s %s)\n", makeOrderVariable(e1),
                makeOrderVariable(e2)));
        reachEngine.addEdge(e1, e2);
    }

    private String getAsstLockRegionHappensBefore(LockRegion lockRegion1, LockRegion lockRegion2) {
        SyncEvent unlock = lockRegion1.getUnlock();
        SyncEvent lock = lockRegion2.getLock();
        return String.format("(< %s %s)",
                unlock != null ? makeOrderVariable(unlock) : makeOrderVariable(
                        trace.getLastThreadEvent(lockRegion1.getThreadId())),
                lock != null ? makeOrderVariable(lock) : makeOrderVariable(
                        trace.getFirstThreadEvent(lockRegion2.getThreadId())));
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
     * Adds must happens-before constraints (MHB).
     */
    public void addMHBConstraints() {
        for (List<SyncEvent> startOrJoinEvents : trace.getThreadIdToStartJoinEvents().values()) {
            for (SyncEvent startOrJoinEvent : startOrJoinEvents) {
                long threadId = startOrJoinEvent.getSyncObject();
                if (startOrJoinEvent.getType().equals(EventType.START)) {
                    Event fstThrdEvent = trace.getFirstThreadEvent(threadId);
                    /* YilongL: it's possible that the first event of the new
                     * thread is not in the current trace */
                    if (fstThrdEvent != null) {
                        assertHappensBefore(startOrJoinEvent, fstThrdEvent);
                    }
                } else if (startOrJoinEvent.getType().equals(EventType.JOIN)) {
                    Event lastThrdEvent = trace.getLastThreadEvent(threadId);
                    /* YilongL: it's possible that the last event of the thread
                     * to join is not in the current trace */
                    if (lastThrdEvent != null) {
                        assertHappensBefore(lastThrdEvent, startOrJoinEvent);
                    }
                } else {
                    assert false : "unexpected event: " + startOrJoinEvent;
                }
            }
        }
    }

    /**
     * Adds lock mutual exclusion constraints.
     */
    public void addLockingConstraints() {
        /* enumerate the locking events on each intrinsic lock */
        for (List<SyncEvent> syncEvents : trace.getLockObjToSyncEvents().values()) {
            Map<Long, SyncEvent> threadIdToPrevLock = Maps.newHashMap();
            Map<Long, SyncEvent> threadIdToPreWait = Maps.newHashMap();
            Map<Long, Deque<SyncEvent>> threadIdToNotifyQueue = Maps.newHashMap();
            List<LockRegion> lockRegions = Lists.newArrayList();

            for (SyncEvent syncEvent : syncEvents) {
                long tid = syncEvent.getTID();

                SyncEvent prewait = null;
                switch (syncEvent.getType()) {
                case LOCK:
                case WAIT:
                    assert !threadIdToPrevLock.containsKey(tid) : "Unexpected nested locking events:\n"
                            + threadIdToPrevLock.get(tid) + ", " + syncEvent;
                    threadIdToPrevLock.put(tid, syncEvent);
                    break;
                case PRE_WAIT:
                    prewait = threadIdToPreWait.put(tid, syncEvent);
                case UNLOCK:
                    Deque<SyncEvent> notifyEvents = safeDequeMapGet(threadIdToNotifyQueue, tid);
                    lockRegions.add(new LockRegion(threadIdToPrevLock.remove(tid), syncEvent,
                            prewait, notifyEvents));
                    notifyEvents.clear();
                    break;
                case NOTIFY:
                case NOTIFY_ALL:
                    safeDequeMapGet(threadIdToNotifyQueue, tid).add(syncEvent);
                    break;
                default:
                    assert false : "Unexpected synchronization event: " + syncEvent;
                }
            }

            for (SyncEvent lock : threadIdToPrevLock.values()) {
                Deque<SyncEvent> notifyEvents = safeDequeMapGet(threadIdToNotifyQueue, lock.getTID());
                lockRegions.add(new LockRegion(lock, null, threadIdToPreWait.remove(lock.getTID()),
                        notifyEvents));
                notifyEvents.clear();
            }

            lockEngine.addAll(lockRegions);

            /* assert lock regions mutual exclusion */
            assertLockMutex(lockRegions);

            matchWaitNotifyPair(lockRegions);
        }
    }

    private void assertLockMutex(List<LockRegion> lockRegions) {
        for (LockRegion lockRegion1 : lockRegions) {
            for (LockRegion lockRegion2 : lockRegions) {
                if (lockRegion1.getThreadId() < lockRegion2.getThreadId()) {
                    assertMutualExclusion(lockRegion1, lockRegion2);
                }
            }
        }
    }

    private void matchWaitNotifyPair(List<LockRegion> lockRegions) {
        for (LockRegion lockRegion1 : lockRegions) {
            if (lockRegion1.getLock() != null
                    && lockRegion1.getLock().getType() == EventType.WAIT) {
                /* assert that the wait event must be matched with a notify */
                StringBuilder matchWaitNotify = new StringBuilder("(or false ");
                SyncEvent wait = lockRegion1.getLock();

                /* enumerate unmatched notify from previous windows */
                if (lockRegion1.getPreWait() == null) {
                    for (SyncEvent notify : trace.getUnmatchedNotifyEvents(wait.getSyncObject())) {
                        if (notify.getType() == EventType.NOTIFY) {
                            matchWaitNotify.append(String.format("(= %s %s)",
                                    makeMatchVariable(notify), wait.getGID()));
                        }
                    }
                }

                /* enumerate all notify in the current window */
                for (LockRegion lockRegion2 : lockRegions) {
                    if (lockRegion1.getThreadId() != lockRegion2.getThreadId()) {
                        for (SyncEvent notify : lockRegion2.getNotifyEvents()) {
                            /* the matched notify event must happen between
                             * the unlock-lock pair of the wait event */
                            StringBuilder sb = new StringBuilder("(and ");
                            if (lockRegion1.getPreWait() != null) {
                                sb.append(String.format("(< %s %s)",
                                        makeOrderVariable(lockRegion1.getPreWait()),
                                        makeOrderVariable(notify)));
                            }
                            sb.append(String.format("(< %s %s)", makeOrderVariable(notify),
                                    makeOrderVariable(wait)));
                            if (notify.getType() == EventType.NOTIFY) {
                                /* make sure NOTIFY can be used only once */
                                sb.append(String.format("(= %s %s)", makeMatchVariable(notify),
                                        wait.getGID()));
                            }
                            sb.append(")");

                            matchWaitNotify.append(sb);
                        }
                    }
                }

                matchWaitNotify.append(")\n");
                smtlibAssertion.append(matchWaitNotify);
            }
        }
    }

    /**
     * Generates a formula ensuring that all read events that {@code event}
     * depends on read the same value as in the original trace, to guarantee
     * {@code event} will be generated in the predicted trace.
     */
    public StringBuilder addReadWriteConsistencyConstraints(MemoryAccessEvent event) {
        StringBuilder cnstr = new StringBuilder("(and true ");

        /* make sure that every dependent read event reads the same value as in the original trace */
        for (ReadEvent depRead : trace.getCtrlFlowDependentEvents(event)) {
            List<WriteEvent> writeEvents = trace.getWriteEventsOn(depRead.getAddr());

            /* thread immediate write predecessor */
            WriteEvent thrdImdWrtPred = null;
            /* predecessor write set: all write events whose values could be read by `depRead' */
            List<WriteEvent> predWriteSet = Lists.newArrayList();
            for (WriteEvent write : writeEvents) {
                if (write.getTID() == depRead.getTID()) {
                    if (write.getGID() < depRead.getGID()) {
                        thrdImdWrtPred = write;
                    }
                } else if (!happensBefore(depRead, write)) {
                    predWriteSet.add(write);
                }
            }
            if (thrdImdWrtPred != null) {
                predWriteSet.add(thrdImdWrtPred);
            }

            /* predecessor write set of same value */
            List<WriteEvent> sameValPredWriteSet = Lists.newArrayList();
            for (WriteEvent write : predWriteSet) {
                if (write.getValue() == depRead.getValue()) {
                    sameValPredWriteSet.add(write);
                }
            }

            /* case 1: the dependent read reads the initial value */
            StringBuilder case1 = new StringBuilder("false");
            if (thrdImdWrtPred == null
                    && trace.getInitValueOf(depRead.getAddr()) == depRead.getValue()) {
                case1 = new StringBuilder("(and true ");
                for (WriteEvent write : predWriteSet) {
                    case1.append(String.format("(< %s %s)", makeOrderVariable(depRead),
                            makeOrderVariable(write)));
                }
                case1.append(")");
            }

            /* case 2: the dependent read reads a previously written value */
            StringBuilder case2 = new StringBuilder("(or false ");
            for (WriteEvent write : sameValPredWriteSet) {
                case2.append("(and ");
                case2.append(String.format("(< %s %s)", makeOrderVariable(write), makeOrderVariable(depRead)));
                for (WriteEvent otherWrite : writeEvents) {
                    if (write != otherWrite && !happensBefore(otherWrite, write)
                            && !happensBefore(depRead, otherWrite)) {
                        case2.append(String.format("(or (< %s %s) (< %s %s))",
                                makeOrderVariable(otherWrite), makeOrderVariable(write),
                                makeOrderVariable(depRead), makeOrderVariable(otherWrite)));
                    }
                }
                case2.append(")");
            }
            case2.append(")");

            cnstr.append(String.format("(or %s %s)", case1, case2));
        }

        cnstr.append(")");
        return cnstr;
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
        id++;
        task = new SMTTaskRun(config, id);

        StringBuilder msg = new StringBuilder(benchname).append(CONS_SETLOGIC)
                .append(smtlibDecl).append(smtlibAssertion).append(")");
        task.sendMessage(msg.toString());

        return task.sat;
    }

    public boolean isRace(Event e1, Event e2, CharSequence casualConstraint) {
        id++;
        task = new SMTTaskRun(config, id);
        StringBuilder msg = new StringBuilder(benchname).append(CONS_SETLOGIC).append(smtlibDecl)
                .append(smtlibAssertion)
                .append(String.format("(= %s %s)", makeOrderVariable(e1), makeOrderVariable(e2)))
                .append(casualConstraint).append("))");
        task.sendMessage(msg.toString());
        return task.sat;
    }

    private static <K, E> Deque<E> safeDequeMapGet(Map<K, Deque<E>> map, K key) {
        Deque<E> value = map.get(key);
        if (value == null) {
            value = new ArrayDeque<>();
            map.put(key, value);
        }
        return value;
    }

}
