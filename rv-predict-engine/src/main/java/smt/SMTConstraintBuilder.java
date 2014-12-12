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
        assert event.getType() != EventType.WAIT;
        return "o" + event.getGID();
    }

    private static String makeOrderVariable(Event event, int subscript) {
        return "o" + event.getGID() + "_" + subscript;
    }

    private static String makeMatchVariable(Event event) {
        return "m" + event.getGID();
    }

    /**
     * Declares an order variable for each event.
     */
    public void declareVariables() {
        for (Event e : trace.getAllEvents()) {
            if (e.getType() == EventType.WAIT) {
                String wait0 = makeOrderVariable(e, 0);
                String wait1 = makeOrderVariable(e, 1);
                smtlibDecl.append(String.format("(%s Int)\n", wait0));
                smtlibDecl.append(String.format("(%s Int)\n", wait1));
                smtlibAssertion.append(String.format("(< %s %s)\n", wait0, wait1));
            } else if (e.getType() == EventType.NOTIFY) {
                smtlibDecl.append(String.format("(%s Int)\n", makeOrderVariable(e)));
                smtlibDecl.append(String.format("(%s Int)\n", makeMatchVariable(e)));
            } else {
                smtlibDecl.append(String.format("(%s Int)\n", makeOrderVariable(e)));
            }
        }
        smtlibDecl.append(")\n");
    }

    private void assertHappensBefore(Event e1, Event e2) {
        String ordVar1 = e1.getType() == EventType.WAIT ? makeOrderVariable(e1, 0)
                : makeOrderVariable(e1);
        String ordVar2 = e2.getType() == EventType.WAIT ? makeOrderVariable(e2, 1)
                : makeOrderVariable(e2);
        smtlibAssertion.append(String.format("(< %s %s)\n", ordVar1, ordVar2));
        reachEngine.addEdge(e1, e2);
    }

    private String getAsstLockRegionHappensBefore(LockRegion lockRegion1, LockRegion lockRegion2) {
        String ordVar1;
        String ordVar2;

        SyncEvent unlock = lockRegion1.getUnlock();
        SyncEvent lock = lockRegion2.getLock();
        if (unlock != null) {
            ordVar1 = unlock.getType() == EventType.WAIT ? makeOrderVariable(unlock, 0)
                    : makeOrderVariable(unlock);
        } else {
            ordVar1 = makeOrderVariable(trace.getLastThreadEvent(lockRegion1.getThreadId()));
        }
        if (lock != null) {
            ordVar2 = lock.getType() == EventType.WAIT ? makeOrderVariable(lock, 1)
                    : makeOrderVariable(lock);
        } else {
            ordVar2 = makeOrderVariable(trace.getFirstThreadEvent(lockRegion2.getThreadId()));
        }

        return String.format("(< %s %s)", ordVar1, ordVar2);
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
            Map<Long, Deque<SyncEvent>> threadIdToLockStack = Maps.newHashMap();
            Map<Long, Deque<SyncEvent>> threadIdToNotifyQueue = Maps.newHashMap();
            List<LockRegion> lockRegions = Lists.newArrayList();

            for (SyncEvent syncEvent : syncEvents) {
                long tid = syncEvent.getTID();

                EventType eventType = syncEvent.getType();
                if (eventType == EventType.LOCK) {
                    safeDequeMapGet(threadIdToLockStack, tid).push(syncEvent);
                } else if (eventType == EventType.UNLOCK || eventType == EventType.WAIT) {
                    Deque<SyncEvent> locks = safeDequeMapGet(threadIdToLockStack, tid);
                    if (locks.size() <= 1) {
                        SyncEvent lock = locks.isEmpty() ? null : locks.pop();
                        Deque<SyncEvent> notifyEvents = safeDequeMapGet(threadIdToNotifyQueue, tid);
                        lockRegions.add(new LockRegion(lock, syncEvent, notifyEvents));
                        notifyEvents.clear();
                    } else {
                        /* discard reentrant lock region */
                        locks.pop();
                    }

                    if (eventType == EventType.WAIT) {
                        if (!locks.isEmpty()) {
                            /* calling wait() in a reentrant lock region results in deadlock */
                            System.err.println("Found dead lock in trace!");
                        }
                        /* wait event is modeled as two consecutive unlock-lock events */
                        locks.push(syncEvent);
                    }
                } else if (eventType == EventType.NOTIFY || eventType == EventType.NOTIFY_ALL) {
                    safeDequeMapGet(threadIdToNotifyQueue, tid).add(syncEvent);
                } else {
                    assert false : "dead code";
                }
            }

            for (Deque<SyncEvent> locks : threadIdToLockStack.values()) {
                /* the corresponding unlock events are missing in the current trace window */
                if (!locks.isEmpty()) {
                    SyncEvent lock = locks.peek();
                    if (lock.getType() == EventType.WAIT && trace.getNextThreadEvent(lock) == null) {
                        /* YilongL: do not create a new lock region in this case
                         * because we don't know if the thread has been notified
                         * from the wait */
                        continue;
                    }

                    Deque<SyncEvent> notifyEvents = safeDequeMapGet(threadIdToNotifyQueue, lock.getTID());
                    lockRegions.add(new LockRegion(lock, null, notifyEvents));
                    notifyEvents.clear();
                }
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
            /* YilongL: we check the lock event (instead of unlock) to be
             * wait because we don't want to add constraint for un-notified
             * wait */
            if (lockRegion1.getLock() != null
                    && lockRegion1.getLock().getType() == EventType.WAIT) {
                SyncEvent wait = lockRegion1.getLock();

                /* assert that the wait event must be matched with a notify */
                StringBuilder matchWaitNotify = new StringBuilder("(or ");

                /* enumerate all notify in the current window */
                String wait0 = makeOrderVariable(wait, 0);
                String wait1 = makeOrderVariable(wait, 1);
                for (LockRegion lockRegion2 : lockRegions) {
                    if (lockRegion1.getThreadId() != lockRegion2.getThreadId()) {
                        for (SyncEvent notify : lockRegion2.getNotifyEvents()) {
                            /* the matched notify event must happen between
                             * the unlock-lock pair of the wait event */
                            StringBuilder sb = new StringBuilder("(and ");
                            sb.append(String.format("(< %s %s %s)",
                                    wait0, makeOrderVariable(notify), wait1));
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

                /* YilongL: we don't need to consider the case where the notify
                 * is not in the current window because it is unsound to guess
                 * outside the current window */

                matchWaitNotify.append(")\n");
                smtlibAssertion.append(matchWaitNotify);
            }
        }
    }

    /**
     * Adds read-write consistency constraints.
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

    /**
     * return true if the solver return a solution to the constraints
     *
     * @param e1
     * @param e2
     * @param casualConstraint
     * @return
     */
    public boolean isRace(Event e1, Event e2, StringBuilder casualConstraint) {
        String var1 = makeOrderVariable(e1);
        String var2 = makeOrderVariable(e2);

        // String QUERY = "\n(assert (= "+var1+" "+var2+"))\n\n";

        id++;
        task = new SMTTaskRun(config, id);

        String cons_assert = smtlibAssertion.toString() + casualConstraint.toString() + ")\n";
        cons_assert = cons_assert.replace(var2 + " ", var1 + " ");
        cons_assert = cons_assert.replace(var2 + ")", var1 + ")");
        StringBuilder msg = new StringBuilder(benchname).append(CONS_SETLOGIC)
                .append(smtlibDecl).append(cons_assert).append(")");
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
