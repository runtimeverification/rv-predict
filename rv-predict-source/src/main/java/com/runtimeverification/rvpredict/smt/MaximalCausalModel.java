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

import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.signals.Signals;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;
import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.ConcretePhiVariable;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.OrderVariable;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
import com.runtimeverification.rvpredict.trace.LockRegion;
import com.runtimeverification.rvpredict.trace.MemoryAccessBlock;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.AND;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.BOOL_EQUAL;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.INT_EQUAL;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.LESS_THAN;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.OR;

public class MaximalCausalModel {

    private final Trace trace;

    /**
     * Keeps track of the must-happen-before (MHB) relations in paper.
     * <p>
     * The must-happen-before (MHB) relations form a special DAG where only a
     * few nodes have more than one outgoing edge. To speed up the reachability
     * query between two nodes, we first collapsed the DAG as much as possible.
     */
    private TransitiveClosure mhbClosure;

    private final LockSetEngine locksetEngine = new LockSetEngine();

    /**
     * Map from read events to the corresponding concrete feasibility formulas.
     */
    private final Map<ReadonlyEventInterface, BoolFormula> readToPhiConc = new HashMap<>();

    /**
     * The formula that describes the maximal causal model of the trace tau.
     */
    private final FormulaTerm.Builder phiTau = FormulaTerm.andBuilder();

    private final Map<String, ReadonlyEventInterface> nameToEvent = new HashMap<>();

    private final Z3Filter z3filter;

    private final com.microsoft.z3.Solver solver;

    private final boolean detectInterruptedThreadRace;

    public static MaximalCausalModel create(
            Trace trace, Z3Filter z3filter, Solver solver, boolean detectInterruptedThreadRace) {
        MaximalCausalModel model = new MaximalCausalModel(trace, z3filter, solver, detectInterruptedThreadRace);
        model.addPhiMHB();
        model.addPhiLock();
        model.addSignalInterruptsRestricts();
        return model;
    }

    private MaximalCausalModel(Trace trace, Z3Filter z3filter, Solver solver, boolean detectInterruptedThreadRace) {
        this.trace = trace;
        this.z3filter = z3filter;
        this.solver = solver;
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
    }

    private BoolFormula HB(ReadonlyEventInterface event1, ReadonlyEventInterface event2) {
        return LESS_THAN(OrderVariable.get(event1), OrderVariable.get(event2));
    }

    private BoolFormula HB(LockRegion lockRegion1, LockRegion lockRegion2) {
        ReadonlyEventInterface unlock = lockRegion1.getUnlock();
        ReadonlyEventInterface lock = lockRegion2.getLock();
        return (unlock == null || lock == null) ? BooleanConstant.FALSE : HB(unlock, lock);
    }

    private BoolFormula MUTEX(LockRegion lockRegion1, LockRegion lockRegion2) {
        return OR(HB(lockRegion1, lockRegion2), HB(lockRegion2, lockRegion1));
    }

    private void fillEnabledAtStartIfEnabledAtEventId(
            Long signalNumber, Long otid, Long eventId,
            Set<Long> otidWhereEnabledAtStart,
            Map<Long, EventWithIndex> otidToItsStartEventWithIndex) {
        if (otidWhereEnabledAtStart.contains(otid)) {
            return;
        }
        if (trace.getEvents(trace.getMainTraceThreadForOriginalThread(otid)).stream()
                .anyMatch(event -> event.getEventId() < eventId
                        && Signals.signalEnableChange(event, signalNumber) != null)) {
            return;
        }
        otidWhereEnabledAtStart.add(otid);
        EventWithIndex start = otidToItsStartEventWithIndex.get(otid);
        if (start == null) {
            return;
        }
        fillEnabledAtStartIfEnabledAtEventId(
                signalNumber, start.getEvent().getOriginalThreadId(), start.getEvent().getEventId(),
                otidWhereEnabledAtStart, otidToItsStartEventWithIndex);
    }

    private Boolean fillEnabledDisabledAtStartIfEnabledByTheParentThread(
            long signalNumber,
            long otid,
            Map<Long, EventWithIndex> otidToItsStartEventWithIndex,
            Set<Long> otidWhereEnabledAtStart, Set<Long> otidWhereDisabledAtStart) {
        if (otidWhereEnabledAtStart.contains(otid)) {
            return Boolean.TRUE;
        }
        if (otidWhereDisabledAtStart.contains(otid)) {
            return Boolean.FALSE;
        }
        EventWithIndex startEvent = otidToItsStartEventWithIndex.get(otid);
        if (startEvent == null) {
            return null;
        }
        Optional<Boolean> maybeLastWrittenValue =
                trace.getEvents(trace.getMainTraceThreadForOriginalThread(startEvent.getEvent().getOriginalThreadId()))
                        // TODO: This is the only place where I use getEventIndex, maybe I should just remove it.
                        // If I do that, I may be able to refactor this.
                        .subList(0, startEvent.getEventIndex()).stream()
                        .map(event -> Signals.signalEnableChange(event, signalNumber))
                        .filter(Objects::nonNull)
                        .reduce((e1, e2) -> e2);
        if (maybeLastWrittenValue.isPresent()) {
            boolean isEnabled = maybeLastWrittenValue.get();
            if (isEnabled) {
                otidWhereEnabledAtStart.add(otid);
                return Boolean.TRUE;
            } else {
                otidWhereDisabledAtStart.add(otid);
                return Boolean.FALSE;
            }
        } else {
            Boolean enabled = fillEnabledDisabledAtStartIfEnabledByTheParentThread(
                    signalNumber,
                    startEvent.event.getOriginalThreadId(),
                    otidToItsStartEventWithIndex,
                    otidWhereEnabledAtStart, otidWhereDisabledAtStart);
            if (enabled == null) {
                return null;
            }
            if (enabled) {
                otidWhereEnabledAtStart.add(otid);
            } else {
                otidWhereDisabledAtStart.add(otid);
            }
            return enabled;
        }
    }

    /**
     * Adds restricts that specify that a signal must interrupt a thread. It also specifies when a signal
     * can interrupt a thread. In order for a signal to interrupt a thread, the following things must happen:
     *
     * 1. The signal handler must be set to the signal's handler. The signal handler is global, so it can
     *    be handled as a variable read, so it is NOT checked here.
     * 2. The signal mask must be set to the right value. This is a bit more complex, since the mask is per-thread.
     *    A thread inherits its caller mask, so we must check what was the mask at the beginning of each thread
     *    and if the current thread changed it. If there is no mask change for a thread or its parents, but a signal
     *    started, we will assume that the mask was enabled at the beginning of the thread.
     */
    private void addSignalInterruptsRestricts() {
        Map<Long, EventWithIndex> otidToItsStartEventWithIndex = new HashMap<>();
        trace.eventsByThreadID().values().forEach(events -> {
            for (int i = 0; i < events.size(); i++) {
                ReadonlyEventInterface event = events.get(i);
                if (!event.isStart()) {
                    continue;
                }
                otidToItsStartEventWithIndex.put(event.getSyncedThreadId(), new EventWithIndex(event, i));
            }
        });
        Map<Long, Map<Long, List<Long>>> signalNumberToOtidToInterruptedEventIds = new HashMap<>();
        trace.eventsByThreadID().entrySet().stream()
                .filter(entry -> trace.getThreadType(entry.getKey()) == ThreadType.SIGNAL)
                .forEach(entry -> {
                    long signalNumber = trace.getSignalNumber(entry.getKey());
                    long originalThreadId = trace.getOriginalThreadIdForTraceThreadId(entry.getKey());
                    // TODO: Make it work with empty lists.
                    long firstEventId = entry.getValue().get(0).getEventId();
                    signalNumberToOtidToInterruptedEventIds
                            .computeIfAbsent(signalNumber, k -> new HashMap<>())
                            .computeIfAbsent(originalThreadId, k -> new ArrayList<>())
                            .add(firstEventId);
                });
        Map<Long, Set<Long>> signalToOtidWhereEnabledAtStart = new HashMap<>();
        signalNumberToOtidToInterruptedEventIds.forEach((signalNumber, otidToInterruptedEventIds) -> {
            Set<Long> otidWhereEnabledAtStart = new HashSet<>();
            signalToOtidWhereEnabledAtStart.put(signalNumber, otidWhereEnabledAtStart);
            otidToInterruptedEventIds.forEach((otid, interruptedEventIds) -> {
                OptionalLong maybeMinSignalStart = interruptedEventIds.stream().mapToLong(l -> l).min();
                if (!maybeMinSignalStart.isPresent()) {
                    return;
                }
                fillEnabledAtStartIfEnabledAtEventId(
                        signalNumber, otid, maybeMinSignalStart.getAsLong(),
                        otidWhereEnabledAtStart, otidToItsStartEventWithIndex);
            });
        });
        // TODO: Signal mask reads also work, so I should do something similar to the above for reads.
        signalToOtidWhereEnabledAtStart.forEach((signalNumber, otidWhereEnabledAtStart) -> {
            Set<Long> otidWhereDisabledAtStart = new HashSet<>();
            otidToItsStartEventWithIndex.forEach(
                    (otid, startEvent) -> fillEnabledDisabledAtStartIfEnabledByTheParentThread(
                            signalNumber,
                            otid,
                            otidToItsStartEventWithIndex,
                            otidWhereEnabledAtStart,
                            otidWhereDisabledAtStart));
        });

        Map<Integer, ReadonlyEventInterface> ttidToStartEvent = new HashMap<>();
        Map<Integer, ReadonlyEventInterface> ttidToJoinEvent = new HashMap<>();
        /* build inter-thread synchronization constraint */
        trace.getInterThreadSyncEvents().forEach(event -> {
            if (event.isStart()) {
                Integer ttid = trace.getMainTraceThreadForOriginalThread(event.getSyncedThreadId());
                if (ttid != null) {
                    ttidToStartEvent.put(ttid, event);
                }
            } else if (event.isJoin()) {
                Integer ttid = trace.getMainTraceThreadForOriginalThread(event.getSyncedThreadId());
                if (ttid != null) {
                    ttidToJoinEvent.put(ttid, event);
                }
            }
        });


        FormulaTerm.Builder and = FormulaTerm.andBuilder();
        and.add(BooleanConstant.TRUE);
        trace.eventsByThreadID().keySet().stream()
                .filter(ttid -> trace.getThreadType(trace.getFirstEvent(ttid)) == ThreadType.SIGNAL)
                .forEach(ttid -> {
                    ReadonlyEventInterface firstEvent = trace.getFirstEvent(ttid);
                    ReadonlyEventInterface lastEvent = trace.getLastEvent(ttid);
                    long signalNumber = trace.getSignalNumber(ttid);
                    Set<Long> otidWhereEnabledAtStart = signalToOtidWhereEnabledAtStart.get(signalNumber);

                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    trace.eventsByThreadID().entrySet().stream()
                            .filter(entry -> trace.getThreadType(entry.getKey()) == ThreadType.THREAD)
                            .forEach(entry -> {
                                List<ReadonlyEventInterface> events = entry.getValue();
                                int entryTtid = entry.getKey();
                                long entryOtid = trace.getOriginalThreadIdForTraceThreadId(entryTtid);
                                boolean enabled = otidWhereEnabledAtStart.contains(entryOtid);
                                ReadonlyEventInterface startThreadEvent = ttidToStartEvent.get(entryTtid);
                                ReadonlyEventInterface joinThreadEvent = ttidToJoinEvent.get(entryTtid);
                                if (events.isEmpty() && enabled) {

                                    // TODO: The signal can simply interrupt this thread. I should add its start/join
                                    // constraints. I can probably add them directly to the top AND.

                                    or.add(signalInterruption(
                                            startThreadEvent,
                                            joinThreadEvent,
                                            firstEvent,
                                            lastEvent));
                                    return;
                                }
                                Signals.EnabledEventsIterator iterator =
                                        new Signals.EnabledEventsIterator(
                                                events, detectInterruptedThreadRace, signalNumber, enabled);
                                while (iterator.advance()) {
                                    or.add(signalInterruption(
                                            iterator.getPreviousEventWithDefault(startThreadEvent),
                                            iterator.getCurrentEventWithDefault(joinThreadEvent),
                                            firstEvent,
                                            lastEvent));
                                }
                            });
                    and.add(or.build());
                });
        phiTau.add(and.build());
    }

    private FormulaTerm signalInterruption(
            ReadonlyEventInterface before, ReadonlyEventInterface after,
            ReadonlyEventInterface firstSignalEvent, ReadonlyEventInterface lastSignalEvent) {
        FormulaTerm.Builder threadInterruptionAtPoint = FormulaTerm.andBuilder();
        if (before != null) {
            threadInterruptionAtPoint.add(HB(before, firstSignalEvent));
        }
        if (after != null) {
            threadInterruptionAtPoint.add(HB(lastSignalEvent, after));
        }
        return threadInterruptionAtPoint.build();
    }

    private class EventWithIndex {
        private final ReadonlyEventInterface event;
        private final int eventIndex;

        private EventWithIndex(ReadonlyEventInterface event, int eventIndex) {
            this.event = event;
            this.eventIndex = eventIndex;
        }

        private ReadonlyEventInterface getEvent() {
            return event;
        }

        private int getEventIndex() {
            return eventIndex;
        }
    }

    /**
     * Adds must-happen-before (MHB) constraints.
     */
    private void addPhiMHB() {
        TransitiveClosure.Builder mhbClosureBuilder = TransitiveClosure.builder(trace.getSize());

        /* build intra-thread program order constraint */
        trace.eventsByThreadID().forEach((tid, events) -> {
            mhbClosureBuilder.createNewGroup(events.get(0));
            events.forEach(event -> nameToEvent.put(OrderVariable.get(event).toString(), event));
            for (int i = 1; i < events.size(); i++) {
                ReadonlyEventInterface e1 = events.get(i - 1);
                ReadonlyEventInterface e2 = events.get(i);
                phiTau.add(HB(e1, e2));
                /* every group should start with a join event and end with a start event */
                if (e1.isStart() || e2.isJoin()) {
                    mhbClosureBuilder.createNewGroup(e2);
                    mhbClosureBuilder.addRelation(e1, e2);
                } else {
                    mhbClosureBuilder.addToGroup(e2, e1);
                }
            }
        });

        /* build inter-thread synchronization constraint */
        trace.getInterThreadSyncEvents().forEach(event -> {
            if (event.isStart()) {
                Integer ttid = trace.getMainTraceThreadForOriginalThread(event.getSyncedThreadId());
                if (ttid != null) {
                    ReadonlyEventInterface fst = trace.getFirstEvent(ttid);
                    if (fst != null) {
                        phiTau.add(HB(event, fst));
                        mhbClosureBuilder.addRelation(event, fst);
                    }
                }
            } else if (event.isJoin()) {
                Integer ttid = trace.getMainTraceThreadForOriginalThread(event.getSyncedThreadId());
                if (ttid != null) {
                    ReadonlyEventInterface last = trace.getLastEvent(ttid);
                    if (last != null) {
                        phiTau.add(HB(last, event));
                        mhbClosureBuilder.addRelation(last, event);
                    }
                }
            }
        });

        mhbClosure = mhbClosureBuilder.build();
    }

    /**
     * Adds lock mutual exclusion constraints.
     */
    private void addPhiLock() {
        trace.getLockIdToLockRegions().forEach((lockId, lockRegions) -> {
            lockRegions.forEach(locksetEngine::add);

            /* assert lock regions mutual exclusion */
            lockRegions.forEach(lr1 -> {
                lockRegions.forEach(lr2 -> {
                    if (lr1.getTTID() < lr2.getTTID()
                            && (lr1.isWriteLocked() || lr2.isWriteLocked())) {
                        phiTau.add(MUTEX(lr1, lr2));
                    }
                });
            });
        });
    }

    private BoolFormula getPhiConc(MemoryAccessBlock block) {
        ReadonlyEventInterface read = block.getFirstRead();
        if (read == null) {
            return getPhiAbs(block);
        } else {
            if (!readToPhiConc.containsKey(read)) {
                readToPhiConc.put(read, null);
                readToPhiConc.put(read, AND(getPhiAbs(block), getPhiSC(read)));
            }
            return new ConcretePhiVariable(read);
        }
    }

    private BoolFormula getPhiAbs(MemoryAccessBlock block) {
        return block.prev() != null ? getPhiConc(block.prev()) : BooleanConstant.TRUE;
    }

    private BoolFormula getPhiSC(ReadonlyEventInterface read) {
        /* compute all the write events that could interfere with the read event */
        List<ReadonlyEventInterface> diffThreadSameAddrSameValWrites = new ArrayList<>();
        List<ReadonlyEventInterface> diffThreadSameAddrDiffValWrites = new ArrayList<>();
        trace.getWriteEvents(read.getDataAddress()).forEach(write -> {
            if (trace.getTraceThreadId(write) != trace.getTraceThreadId(read) && !happensBefore(read, write)) {
                if (write.getDataValue() == read.getDataValue()) {
                    diffThreadSameAddrSameValWrites.add(write);
                } else {
                    diffThreadSameAddrDiffValWrites.add(write);
                }
            }
        });

        ReadonlyEventInterface sameThreadPrevWrite = trace.getSameThreadPrevWrite(read);
        if (sameThreadPrevWrite != null) {
            /* sameThreadPrevWrite is available in the current window */
            if (read.getDataValue() == sameThreadPrevWrite.getDataValue()) {
                /* the read value is the same as sameThreadPrevWrite */
                FormulaTerm.Builder or = FormulaTerm.orBuilder();

                { /* case 1: read the value written in the same thread */
                    FormulaTerm.Builder and = FormulaTerm.andBuilder();
                    diffThreadSameAddrDiffValWrites
                            .forEach(w -> and.add(OR(HB(w, sameThreadPrevWrite), HB(read, w))));
                    or.add(and.build());
                }

                /* case 2: read the value written in another thread  */
                diffThreadSameAddrSameValWrites.forEach(w1 -> {
                    if (!happensBefore(w1, sameThreadPrevWrite)) {
                        FormulaTerm.Builder and = FormulaTerm.andBuilder();
                        and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                        diffThreadSameAddrDiffValWrites.forEach(w2 -> {
                            if (!happensBefore(w2, w1) && !happensBefore(w2, sameThreadPrevWrite)) {
                                and.add(OR(HB(w2, w1), HB(read, w2)));
                            }
                        });
                        or.add(and.build());
                    }
                });
                return or.build();
            } else {
                /* the read value is different from sameThreadPrevWrite */
                if (!diffThreadSameAddrSameValWrites.isEmpty()) {
                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    diffThreadSameAddrSameValWrites.forEach(w1 -> {
                        FormulaTerm.Builder and = FormulaTerm.andBuilder();
                        and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                        and.add(HB(sameThreadPrevWrite, w1), HB(w1, read));
                        diffThreadSameAddrDiffValWrites.forEach(w2 -> {
                            if (!happensBefore(w2, w1)) {
                                and.add(OR(HB(w2, w1), HB(read, w2)));
                            }
                        });
                        or.add(and.build());
                    });
                    return or.build();
                } else {
                    /* the read-write consistency constraint is UNSAT */
                    trace.logger().debug("Missing write events on " + read.getDataAddress());
                    return BooleanConstant.TRUE;
                }
            }
        } else {
            /* sameThreadPrevWrite is unavailable in the current window */
            ReadonlyEventInterface diffThreadPrevWrite = trace.getAllThreadsPrevWrite(read);
            if (diffThreadPrevWrite == null) {
                /* the initial value of this address must be read.getDataValue() */
                FormulaTerm.Builder and = FormulaTerm.andBuilder();
                diffThreadSameAddrDiffValWrites.forEach(w -> and.add(HB(read, w)));
                return and.build();
            } else {
                /* the initial value of this address is unknown */
                if (!diffThreadSameAddrSameValWrites.isEmpty()) {
                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    diffThreadSameAddrSameValWrites.forEach(w1 -> {
                        FormulaTerm.Builder and = FormulaTerm.andBuilder();
                        and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                        and.add(HB(w1, read));
                        diffThreadSameAddrDiffValWrites.forEach(w2 -> {
                            if (!happensBefore(w2, w1)) {
                                and.add(OR(HB(w2, w1), HB(read, w2)));
                            }
                        });
                        or.add(and.build());
                    });
                    return or.build();
                } else {
                    /* the read-write consistency constraint is UNSAT */
                    trace.logger().debug("Missing write events on " + read.getDataAddress());
                    return BooleanConstant.TRUE;
                }
            }
        }
    }

    /**
     * Checks if one event happens before another.
     */
    private boolean happensBefore(ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        return mhbClosure.inRelation(e1, e2);
    }

    private boolean failPecanCheck(Race race) {
        ReadonlyEventInterface e1 = race.firstEvent();
        ReadonlyEventInterface e2 = race.secondEvent();
        return locksetEngine.hasCommonLock(e1, e2, trace.getTraceThreadId(e1), trace.getTraceThreadId(e2))
                || happensBefore(e1, e2)
                || happensBefore(e2, e1);
    }

    private BoolFormula getRaceAssertion(Race race) {
        ReadonlyEventInterface e1 = race.firstEvent();
        ReadonlyEventInterface e2 = race.secondEvent();
        FormulaTerm.Builder raceAsst = FormulaTerm.andBuilder();
        raceAsst.add(INT_EQUAL(OrderVariable.get(e1), OrderVariable.get(e2)),
                getPhiAbs(trace.getMemoryAccessBlock(e1)),
                getPhiAbs(trace.getMemoryAccessBlock(e2)));
        return raceAsst.build();
    }

    private class EventWithOrder {
        private final ReadonlyEventInterface event;
        private final long orderId;
        private EventWithOrder(ReadonlyEventInterface event, long orderId) {
            this.event = event;
            this.orderId = orderId;
        }
        private ReadonlyEventInterface getEvent() {
            return event;
        }
        private long getOrderId() {
            return orderId;
        }
        @Override
        public String toString() {
            return String.format("(Id:%s Order:%s)", event.getEventId(), orderId);
        }
    }

    /**
     * Checks if the given race suspects are real. Race suspects are grouped by
     * their signatures.
     *
     * @param sigToRaceSuspects
     * @return a map from race signatures to real race instances
     */
    public Map<String, Race> checkRaceSuspects(Map<String, List<Race>> sigToRaceSuspects) {
        /* specialize the maximal causal model based on race queries */
        Map<Race, BoolFormula> suspectToAsst = new HashMap<>();
        sigToRaceSuspects.values().forEach(suspects -> {
            suspects.removeIf(this::failPecanCheck);
            suspects.forEach(p -> suspectToAsst.computeIfAbsent(p, this::getRaceAssertion));
        });
        sigToRaceSuspects.entrySet().removeIf(e -> e.getValue().isEmpty());
        if (sigToRaceSuspects.isEmpty()) {
            return Collections.emptyMap();
        }

//        trace.logger().debug().println("start analyzing: " + trace.getBaseGID());
//        sigToRaceSuspects.forEach((sig, l) -> trace.logger().debug().println(sig + ": " + l.size()));

        Map<String, Race> result = new HashMap<>();
        try {
            solver.push();
            /* translate our formula into Z3 AST format */
            solver.add(z3filter.filter(phiTau.build()));
            for (Map.Entry<ReadonlyEventInterface, BoolFormula> entry : readToPhiConc.entrySet()) {
                solver.add(z3filter.filter(BOOL_EQUAL(new ConcretePhiVariable(entry.getKey()),
                        entry.getValue())));
            }
//            checkTraceConsistency(z3filter, solver);

            if (Configuration.debug) {
                findAndDumpOrdering();
            }
            /* check race suspects */

            for (Map.Entry<String, List<Race>> entry : sigToRaceSuspects.entrySet()) {
                for (Race race : entry.getValue()) {
                    solver.push();
                    solver.add(z3filter.filter(suspectToAsst.get(race)));
                    boolean isRace = solver.check() == Status.SATISFIABLE;
                    if (isRace && Configuration.debug) {
                        dumpOrdering();
                    }
                    solver.pop();
                    if (isRace) {
                        result.put(entry.getKey(), race);
                        break;
                    }
                }
            }
            solver.pop();
            z3filter.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private void findAndDumpOrdering() {
        solver.push();
        if (solver.check() == Status.SATISFIABLE) {
            dumpOrdering();
        }
        solver.pop();
    }

    private void dumpOrdering() {
        Model model = solver.getModel();
        Map<Integer, List<EventWithOrder>> threadToExecution = new HashMap<>();
        for (FuncDecl f : model.getConstDecls()) {
            String name = f.getName().toString();
            if (nameToEvent.containsKey(name)) {
                ReadonlyEventInterface event = nameToEvent.get(name);
                EventWithOrder eventWithOrder =
                        new EventWithOrder(event, Long.parseLong(model.getConstInterp(f).toString()));
                threadToExecution
                        .computeIfAbsent(trace.getTraceThreadId(event), a -> new ArrayList<>())
                        .add(eventWithOrder);
            }
        }
        threadToExecution.values().forEach(events ->
                events.sort(Comparator.comparingLong(EventWithOrder::getOrderId)));

        System.out.println("Possible ordering of events, per thread ..........");
        threadToExecution.forEach((tid, events) -> {
            ArrayList<String> description = new ArrayList<>();
            events.forEach(e -> description.add(e.getEvent().getEventId() + ":" + e.getOrderId()));
            System.out.print("  Thread:" + tid);
            System.out.print(" -> ");
            System.out.println(String.join(" ", description));
        });
        System.out.println(".......... That's all folks!");
    }

    /**
     * Checks if the logged trace is in a consistent state.
     */
    @SuppressWarnings("unused")
    private void checkTraceConsistency(Z3Filter z3filter, com.microsoft.z3.Solver solver)
            throws Exception {
        List<MemoryAccessBlock> blks = new ArrayList<>();
        trace.memoryAccessBlocksByThreadID().values().forEach(l -> blks.addAll(l));
        Collections.sort(blks);

        solver.push();
        /* simply assign the GID of an event to its order variable */
        for (List<ReadonlyEventInterface> l : trace.eventsByThreadID().values()) {
            for (ReadonlyEventInterface event : l) {
                solver.add(z3filter.filter(
                        INT_EQUAL(OrderVariable.get(event), new IntConstant(event.getEventId()))));
            }
        }

        /* assert that all events should be concretely feasible */
        for (MemoryAccessBlock blk : blks) {
            solver.add(z3filter.filter(getPhiConc(blk)));
        }

        if (solver.check() != Status.SATISFIABLE) {
            throw new RuntimeException("Inconsistent trace!");
        }
        solver.pop();
    }

}
