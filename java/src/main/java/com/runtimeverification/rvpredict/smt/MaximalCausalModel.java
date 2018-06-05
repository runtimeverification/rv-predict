/* ******************************************************************************
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
 * *****************************************************************************/
package com.runtimeverification.rvpredict.smt;

import com.google.common.collect.ImmutableList;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.performance.AnalysisLimit;
import com.runtimeverification.rvpredict.performance.Profiler;
import com.runtimeverification.rvpredict.performance.ProfilerToken;
import com.runtimeverification.rvpredict.smt.constraintsources.DisjointLocks;
import com.runtimeverification.rvpredict.smt.constraintsources.InterThreadOrdering;
import com.runtimeverification.rvpredict.smt.constraintsources.IntraThreadOrdering;
import com.runtimeverification.rvpredict.smt.constraintsources.NonSignalsHaveDepth0ConstraintSource;
import com.runtimeverification.rvpredict.smt.constraintsources.SignalDepthLimit;
import com.runtimeverification.rvpredict.smt.constraintsources.SignalInterruptLocationsConstraintSource;
import com.runtimeverification.rvpredict.smt.constraintsources.SignalStartMask;
import com.runtimeverification.rvpredict.smt.constraintsources.SignalsDoNotOverlapWhenInterruptingTheSameThread;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;
import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.ConcretePhiVariable;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;
import com.runtimeverification.rvpredict.smt.formula.OrderVariable;
import com.runtimeverification.rvpredict.trace.MemoryAccessBlock;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.violation.Race;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

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
    private final FormulaTerm.Builder unsoundButFastPhiTau = FormulaTerm.andBuilder();
    private final FormulaTerm.Builder soundPhiTau = FormulaTerm.andBuilder();

    private final Map<String, ReadonlyEventInterface> nameToEvent = new HashMap<>();

    private final RaceSolver raceSolver;

    private final boolean detectInterruptedThreadRace;
    private final int maxSignalDepth;

    public static MaximalCausalModel create(
            Trace trace, RaceSolver raceSolver, boolean detectInterruptedThreadRace, int maxSignalDepth) {
        MaximalCausalModel model = new MaximalCausalModel(
                trace, raceSolver, detectInterruptedThreadRace, maxSignalDepth);

        model.addConstraints();
        return model;
    }

    private MaximalCausalModel(
            Trace trace, RaceSolver raceSolver, boolean detectInterruptedThreadRace, int maxSignalDepth) {
        this.trace = trace;
        this.raceSolver = raceSolver;
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
        this.maxSignalDepth = maxSignalDepth;
        trace.eventsByThreadID().forEach((tid, events) ->
                events.forEach(event -> nameToEvent.put(OrderVariable.get(event).toString(), event)));
    }

    private BoolFormula HB(ReadonlyEventInterface event1, ReadonlyEventInterface event2) {
        return LESS_THAN(OrderVariable.get(event1), OrderVariable.get(event2));
    }

    private void addConstraints() {
        ImmutableList<ConstraintSourceWithHappensBefore> happensBeforeConstraints =
                new ImmutableList.Builder<ConstraintSourceWithHappensBefore>()
                        .add(new IntraThreadOrdering(trace.eventsByThreadID()))
                        .add(new InterThreadOrdering(
                                trace.getInterThreadSyncEvents(),
                                trace::getMainTraceThreadForOriginalThread,
                                trace::getFirstEvent,
                                trace::getLastEvent))
                        .build();

        TransitiveClosure.Builder mhbClosureBuilder = TransitiveClosure.builder(trace.getSize());
        happensBeforeConstraints.forEach(source -> source.addToMhbClosure(mhbClosureBuilder));
        mhbClosure = mhbClosureBuilder.build();

        trace.getLockIdToLockRegions().forEach((lockId, lockRegions) -> lockRegions.forEach(locksetEngine::add));

        List<Integer> allSignalTtids = trace.getThreadsForCurrentWindow().stream()
                .filter(ttid -> trace.getThreadType(ttid) == ThreadType.SIGNAL)
                .collect(Collectors.toList());

        List<ConstraintSource> constraintSources = new ImmutableList.Builder<ConstraintSource>()
                .addAll(happensBeforeConstraints)
                .add(new DisjointLocks(
                        trace.getLockIdToLockRegions().values(),
                        trace::threadsCanOverlap))
                .add(new SignalInterruptLocationsConstraintSource(
                        trace.eventsByThreadID(),
                        trace::getThreadType,
                        trace::getSignalNumber,
                        trace::getStartEventForTtid,
                        trace::getJoinEventForTtid,
                        trace::getTtidsWhereSignalIsEnabledAtStart,
                        trace::getTtidsWhereSignalIsDisabledAtStart,
                        detectInterruptedThreadRace))
                .add(new NonSignalsHaveDepth0ConstraintSource(
                        trace.getThreadsForCurrentWindow(),
                        trace::getThreadType))
                .add(new SignalDepthLimit(
                        maxSignalDepth,
                        trace.eventsByThreadID().keySet(),
                        trace::getThreadType,
                        trace::getSignalDepth))
                .add(new SignalStartMask(
                        trace.eventsByThreadID(),
                        trace::getThreadType,
                        trace::getSignalNumber,
                        trace::getSignalHandler,
                        trace::getThreadStartsInTheCurrentWindow,
                        trace::getSignalEndsInTheCurrentWindow,
                        trace::getSignalEnabledAtStart,
                        trace::getEstablishSignalEvents,
                        trace::getPreviousWindowEstablishEvent))
                .add(new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                        allSignalTtids,
                        trace::getFirstEvent,
                        trace::getLastEvent,
                        trace::getThreadStartsInTheCurrentWindow))
                .build();

        constraintSources.forEach(source -> unsoundButFastPhiTau.add(source
                .createConstraint(ConstraintType.UNSOUND_BUT_FAST)
                .createSmtFormula()));
        constraintSources.forEach(source -> soundPhiTau.add(source
                .createConstraint(ConstraintType.SOUND)
                .createSmtFormula()));
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
        Map<Integer, ReadonlyEventInterface> diffThreadSameAddrFirstDiffValReadPrefix = new HashMap<>();
        trace.getWriteEvents(read.getDataInternalIdentifier()).forEach(write -> {
            OptionalInt maybeWriteId = trace.getTraceThreadId(write);
            OptionalInt maybeReadId = trace.getTraceThreadId(read);
            assert maybeWriteId.isPresent();
            assert maybeReadId.isPresent();
            if (maybeWriteId.getAsInt() != maybeReadId.getAsInt() && !happensBefore(read, write)) {
                if (write.getDataValue() == read.getDataValue()) {
                    diffThreadSameAddrSameValWrites.add(write);
                } else {
                    diffThreadSameAddrDiffValWrites.add(write);
                }
            }
        });
        trace.getPrefixReadEvents(read.getDataInternalIdentifier()).forEach(otherRead -> {
            OptionalInt maybeOtherId = trace.getTraceThreadId(otherRead);
            OptionalInt maybeReadId = trace.getTraceThreadId(read);
            assert maybeOtherId.isPresent();
            assert maybeReadId.isPresent();
            if (maybeOtherId.getAsInt() != maybeReadId.getAsInt() && !happensBefore(read, otherRead)) {
                if (otherRead.getDataValue() != read.getDataValue()) {
                    OptionalInt maybeTtid = trace.getTraceThreadId(otherRead);
                    assert maybeTtid.isPresent();
                    diffThreadSameAddrFirstDiffValReadPrefix
                            .compute(
                                    maybeTtid.getAsInt(),
                                    (k, v) -> v == null || v.getEventId() > otherRead.getEventId() ? otherRead : v);
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
                            .stream()
                            .filter(w -> !happensBefore(w, sameThreadPrevWrite))
                            .filter(w -> !happensBefore(read, w))
                            .forEach(w -> and.add(OR(HB(w, sameThreadPrevWrite), HB(read, w))));
                    or.add(and.build());
                }

                /* case 2: read the value written in another thread  */
                diffThreadSameAddrSameValWrites
                        .stream()
                        .filter(w1 -> !happensBefore(w1, sameThreadPrevWrite))
                        .filter(w1 -> !happensBefore(read, w1))
                        .forEach(w1 -> {
                            FormulaTerm.Builder and = FormulaTerm.andBuilder();
                            and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                            and.add(HB(w1, read));
                            // This is also possible, but not needed: and.add(HB(sameThreadPrevWrite, w1));
                            diffThreadSameAddrDiffValWrites
                                    .stream()
                                    .filter(w2 -> !happensBefore(w2, w1))
                                    .filter(w2 -> !happensBefore(w2, sameThreadPrevWrite))
                                    .filter(w2 -> !happensBefore(read, w2))
                                    .forEach(w2 -> and.add(OR(HB(w2, w1), HB(read, w2))));
                            or.add(and.build());
                        });
                return or.build();
            } else {
                /* the read value is different from sameThreadPrevWrite */
                if (!diffThreadSameAddrSameValWrites.isEmpty()) {
                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    diffThreadSameAddrSameValWrites
                            .stream()
                            .filter(w1 -> !happensBefore(w1, sameThreadPrevWrite))
                            .filter(w1 -> !happensBefore(read, w1))
                            .forEach(w1 -> {
                                FormulaTerm.Builder and = FormulaTerm.andBuilder();
                                and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                                and.add(HB(sameThreadPrevWrite, w1), HB(w1, read));
                                diffThreadSameAddrDiffValWrites
                                        .stream()
                                        .filter(w2 -> !happensBefore(w2, w1))
                                        .filter(w2 -> !happensBefore(w2, sameThreadPrevWrite))
                                        .filter(w2 -> !happensBefore(read, w2))
                                        .forEach(w2 -> and.add(OR(HB(w2, w1), HB(read, w2))));
                                or.add(and.build());
                            });
                    return or.build();
                } else {
                    /* the read-write consistency constraint is UNSAT */
                    trace.logger().debug("Missing write events on " + read.getDataInternalIdentifier());
                    return BooleanConstant.TRUE;
                }
            }
        } else {
            /* sameThreadPrevWrite is unavailable in the current window.*/
            ReadonlyEventInterface sameThreadPrevReadDiffValue =
                    trace.getSameThreadPrevReadSameAddrDiffValue(read);
            if (sameThreadPrevReadDiffValue != null) {
                if (!diffThreadSameAddrSameValWrites.isEmpty()) {
                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    diffThreadSameAddrSameValWrites
                            .stream()
                            .filter(w1 -> !happensBefore(w1, sameThreadPrevReadDiffValue))
                            .filter(w1 -> !happensBefore(read, w1))
                            .forEach(w1 -> {
                                FormulaTerm.Builder and = FormulaTerm.andBuilder();
                                and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                                and.add(HB(sameThreadPrevReadDiffValue, w1), HB(w1, read));
                                diffThreadSameAddrDiffValWrites
                                        .stream()
                                        .filter(w2 -> !happensBefore(w2, w1))
                                        .filter(w2 -> !happensBefore(w2, sameThreadPrevReadDiffValue))
                                        .filter(w2 -> !happensBefore(read, w2))
                                        .forEach(w2 -> and.add(OR(HB(w2, w1), HB(read, w2))));
                                or.add(and.build());
                            });
                    return or.build();
                } else {
                    /* the read-write consistency constraint is UNSAT */
                    trace.logger().debug("Missing write events on " + read.getDataInternalIdentifier());
                    return BooleanConstant.TRUE;
                }
            }
            /* sameThreadPrevReadDiffValue is unavailable in the current window.*/
            ReadonlyEventInterface diffThreadPrevWrite = trace.getAllThreadsPrevWrite(read);
            if (diffThreadPrevWrite == null) {
                /* the initial value of this address must be read.getDataValue() */
                FormulaTerm.Builder and = FormulaTerm.andBuilder();
                diffThreadSameAddrDiffValWrites.forEach(w -> and.add(HB(read, w)));
                diffThreadSameAddrFirstDiffValReadPrefix.values().forEach(r -> and.add(HB(read, r)));
                return and.build();
            } else {
                /* the initial value of this address is unknown */
                if (!diffThreadSameAddrSameValWrites.isEmpty()) {
                    FormulaTerm.Builder or = FormulaTerm.orBuilder();
                    diffThreadSameAddrSameValWrites
                            .stream()
                            .filter(w1 -> !happensBefore(read, w1))
                            .forEach(w1 -> {
                                FormulaTerm.Builder and = FormulaTerm.andBuilder();
                                and.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
                                and.add(HB(w1, read));
                                diffThreadSameAddrDiffValWrites
                                        .stream()
                                        .filter(w2 -> !happensBefore(w2, w1))
                                        .filter(w2 -> !happensBefore(read, w2))
                                        .forEach(w2 -> and.add(OR(HB(w2, w1), HB(read, w2))));
                                or.add(and.build());
                            });
                    return or.build();
                } else {
                    /* the read-write consistency constraint is UNSAT */
                    trace.logger().debug("Missing write events on " + read.getDataInternalIdentifier());
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
        OptionalInt maybeThread1 = trace.getTraceThreadId(e1);
        OptionalInt maybeThread2 = trace.getTraceThreadId(e2);
        assert maybeThread1.isPresent();
        assert maybeThread2.isPresent();
        return locksetEngine.hasCommonLock(e1, e2, maybeThread1.getAsInt(), maybeThread2.getAsInt())
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

    /**
     * Checks if the given race suspects are real. Race suspects are grouped by
     * their signatures.
     *
     * @param sigToRaceSuspects The race suspects to check.
     * @return a map from race signatures to real race instances
     */
    public Map<String, Race> checkRaceSuspects(Map<String, List<Race>> sigToRaceSuspects, AnalysisLimit analysisLimit) {
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

        List<RaceBucket> raceBuckets = new ArrayList<>();
        for (Map.Entry<String, List<Race>> entry : sigToRaceSuspects.entrySet()) {
            raceBuckets.add(new RaceBucket(entry.getKey(), entry.getValue()));
        }

        RaceSolver.WindowData windowData =
            new RaceSolver.WindowData(unsoundButFastPhiTau.build(), soundPhiTau.build(), buildPhiConc());
        Map<String, Race> result = new HashMap<>();
        try (ProfilerToken ignored = Profiler.instance().start("All solver stuff")) {
            MutableBoolean atLeastOneRace = new MutableBoolean(false);
            try {
                boolean hadPossibleRace;
                /* check race suspects */
                do {
                    hadPossibleRace = false;
                    for (RaceBucket bucket : raceBuckets) {
                        Optional<Race> maybeRace = bucket.nextRace();
                        if (!maybeRace.isPresent()) {
                            continue;
                        }
                        hadPossibleRace = true;
                        Race race = maybeRace.get();
                        analysisLimit.runWithException( () ->
                                raceSolver.checkRace(
                                        windowData,
                                        suspectToAsst.get(race),
                                        model -> {
                                            result.put(bucket.getNameAndMarkAsSolved(), race);
                                            atLeastOneRace.setTrue();
                                            Map<Integer, List<EventWithOrder>> threadToExecution =
                                                    extractExecution(model, nameToEvent);
                                            Map<Integer, Integer> signalParents = extractSignalParents(model);
                                            fillSignalStack(threadToExecution, signalParents, race);
                                            if (Configuration.debug) {
                                                dumpOrderingWithLessThreadSwitches(
                                                        threadToExecution,
                                                        Optional.of(race.firstEvent()), Optional.of(race.secondEvent()),
                                                        signalParents);
                                            }
                                        }));
                    }
                } while (hadPossibleRace);
            } finally {
                analysisLimit.runWithException(raceSolver::finishAllWork);
            }
            if (!atLeastOneRace.booleanValue() && Configuration.debug) {
                analysisLimit.runWithException( () ->
                    raceSolver.generateSolution(
                            windowData,
                            model ->
                                    dumpOrderingWithLessThreadSwitches(
                                            extractExecution(model, nameToEvent),
                                            Optional.empty(), Optional.empty(),
                                            extractSignalParents(model))));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private Collection<BoolFormula> buildPhiConc() {
        return readToPhiConc.entrySet()
                .stream()
                .map(entry -> BOOL_EQUAL(new ConcretePhiVariable(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<Integer, Integer> extractSignalParents(Model model) {
        Map<Integer, Integer> signalParents = new HashMap<>();
        for (FuncDecl f : model.getConstDecls()) {
            String name = f.getName().toString();
            OptionalInt maybeSignalTtid = InterruptedThreadVariable.extractSignalTtidIfPossible(name);
            if (!maybeSignalTtid.isPresent()) {
                continue;
            }
            int parentTid = Integer.parseInt(model.getConstInterp(f).toString());
            signalParents.put(maybeSignalTtid.getAsInt(), parentTid);
        }
        return signalParents;
    }

    private void fillSignalStack(
            Map<Integer, List<EventWithOrder>> threadToExecution,
            Map<Integer, Integer> signalParents,
            Race race) {
        race.setFirstSignalStack(computeSignalStack(threadToExecution, signalParents, race.firstEvent()));
        race.setSecondSignalStack(computeSignalStack(threadToExecution, signalParents, race.secondEvent()));
    }

    private OptionalLong findEventOrder(
            int ttid,
            Map<Integer, List<EventWithOrder>> threadToExecution,
            ReadonlyEventInterface event) {
        for (EventWithOrder eventWithOrder : threadToExecution.get(ttid)) {
            if (eventWithOrder.getEvent().getEventId() == event.getEventId()) {
                return OptionalLong.of(eventWithOrder.getOrderId());
            }
        }
        return OptionalLong.empty();
    }

    private List<Race.SignalStackEvent> computeSignalStack(
            Map<Integer, List<EventWithOrder>> threadToExecution,
            Map<Integer, Integer> signalParents,
            ReadonlyEventInterface event) {
        List<Race.SignalStackEvent> signalStack = new ArrayList<>();
        OptionalInt maybeTtid = trace.getTraceThreadId(event);
        assert maybeTtid.isPresent();
        int ttid = maybeTtid.getAsInt();
        signalStack.add(Race.SignalStackEvent.fromEvent(event, ttid));

        OptionalLong maybeFirstEventOrder = findEventOrder(ttid, threadToExecution, event);
        assert maybeFirstEventOrder.isPresent();
        long currentEventOrder = maybeFirstEventOrder.getAsLong();

        while (trace.getThreadType(ttid) != ThreadType.THREAD) {
            int parentTtid = signalParents.get(ttid);
            List<EventWithOrder> parentThreadEvents =
                    threadToExecution.getOrDefault(parentTtid, Collections.emptyList());
            EventWithOrder parentEvent = null;
            for (EventWithOrder maybeParentEvent : parentThreadEvents) {
                if (maybeParentEvent.getOrderId() > currentEventOrder) {
                    break;
                }
                parentEvent = maybeParentEvent;
            }
            ttid = parentTtid;
            if (parentEvent != null) {
                currentEventOrder = parentEvent.getOrderId();
                signalStack.add(Race.SignalStackEvent.fromEvent(parentEvent.getEvent(), parentTtid));
            } else {
                signalStack.add(Race.SignalStackEvent.fromBeforeFirstEvent(parentTtid));
            }
        }
        return signalStack;
    }

    private Map<Integer, List<EventWithOrder>> extractExecution(
            Model model,
            Map<String, ReadonlyEventInterface> nameToEvent) {
        Map<Integer, List<EventWithOrder>> threadToExecution = new HashMap<>();
        for (FuncDecl f : model.getConstDecls()) {
            String name = f.getName().toString();
            if (nameToEvent.containsKey(name)) {
                ReadonlyEventInterface event = nameToEvent.get(name);
                EventWithOrder eventWithOrder =
                        new EventWithOrder(event, Long.parseLong(model.getConstInterp(f).toString()));
                OptionalInt maybeTtid = trace.getTraceThreadId(event);
                assert maybeTtid.isPresent();
                threadToExecution
                        .computeIfAbsent(maybeTtid.getAsInt(), a -> new ArrayList<>())
                        .add(eventWithOrder);
            }
        }
        threadToExecution.values().forEach(events ->
                events.sort(Comparator.comparingLong(EventWithOrder::getOrderId)));
        return threadToExecution;
    }

    private void dumpOrderingWithLessThreadSwitches(
            Map<Integer, List<EventWithOrder>> threadToExecution,
            Optional<ReadonlyEventInterface> firstRaceEvent, Optional<ReadonlyEventInterface> secondRaceEvent,
            Map<Integer, Integer> signalParents) {
        Optional<OrderedEventWithThread> firstRaceOrderedEvent = Optional.empty();
        Optional<OrderedEventWithThread> secondRaceOrderedEvent = Optional.empty();
        if (firstRaceEvent.isPresent()) {
            firstRaceOrderedEvent = findEventForId(threadToExecution, firstRaceEvent.get().getEventId());
        }
        if (secondRaceEvent.isPresent()) {
            secondRaceOrderedEvent = findEventForId(threadToExecution, secondRaceEvent.get().getEventId());
        }
        assert(firstRaceEvent.isPresent() == secondRaceEvent.isPresent());
        assert(firstRaceEvent.isPresent() == firstRaceOrderedEvent.isPresent());
        assert(secondRaceEvent.isPresent() == secondRaceOrderedEvent.isPresent());

        Map<OrderedEventWithThread, Set<OrderedEventWithThread>> dependencyGraph =
                buildDependencyGraph(threadToExecution);

        if (firstRaceOrderedEvent.isPresent()) {
            removeRaceEventsAndEventsNotInvolvedInRace(
                    dependencyGraph, firstRaceOrderedEvent.get(), secondRaceOrderedEvent.get());
        }

        List<OrderedEventWithThread> sortedEvents = topologicalSortingWithFewerThreadSwitches(dependencyGraph);
        int previousThread = Constants.INVALID_TTID;
        for (OrderedEventWithThread oewt : sortedEvents) {
            EventWithOrder eventWithOrder = getOrderedEvent(threadToExecution, oewt);
            if (oewt.thread != previousThread) {
                System.out.println(
                        "-- Switching to thread "
                                + threadDescription(oewt.thread, eventWithOrder, signalParents) + " --");
                previousThread = oewt.thread;
            }
            System.out.println(prettyPrint(eventWithOrder.getEvent()));
        }
        if (firstRaceOrderedEvent.isPresent()) {
            EventWithOrder first = getOrderedEvent(threadToExecution, firstRaceOrderedEvent.get());
            EventWithOrder second = getOrderedEvent(threadToExecution, secondRaceOrderedEvent.get());
            System.out.println("-- Found race for threads "
                    + threadDescription(firstRaceOrderedEvent.get().thread, first, signalParents)
                    + " and "
                    + threadDescription(secondRaceOrderedEvent.get().thread, second, signalParents)
                    + " --");
            System.out.println(prettyPrint(first.getEvent()));
            System.out.println(" -- vs");
            System.out.println(prettyPrint(second.getEvent()));
        }
        System.out.println("------------------------------------------");
    }

    private Map<OrderedEventWithThread, Set<OrderedEventWithThread>> buildDependencyGraph(
            Map<Integer, List<EventWithOrder>> threadToExecution) {
        Map<OrderedEventWithThread, Set<OrderedEventWithThread>> dependencies = new HashMap<>();
        threadToExecution.keySet().forEach(ttid -> {
            Optional<ReadonlyEventInterface> maybeStartEvent = trace.getStartEventForTtid(ttid);
            if (!maybeStartEvent.isPresent()) {
                return;
            }
            ReadonlyEventInterface startEvent = maybeStartEvent.get();
            OptionalInt maybeParentThreadTtid = trace.getTraceThreadId(startEvent);
            assert maybeParentThreadTtid.isPresent();
            int parentThreadTtid = maybeParentThreadTtid.getAsInt();
            List<EventWithOrder> parentExecution = threadToExecution.get(parentThreadTtid);
            OptionalInt maybeStartEventIndex = OptionalInt.empty();
            for (int i = 0; i < parentExecution.size(); i++) {
                if (parentExecution.get(i).getEvent().getEventId() == startEvent.getEventId()) {
                    maybeStartEventIndex = OptionalInt.of(i);
                    break;
                }
            }
            assert maybeStartEventIndex.isPresent();
            dependencies
                    .computeIfAbsent(new OrderedEventWithThread(ttid, 0), key -> new HashSet<>())
                    .add(new OrderedEventWithThread(parentThreadTtid, maybeStartEventIndex.getAsInt()));
        });
        Map<Long, OrderedEventWithThread> lastReadForVariable = new HashMap<>();
        Map<Long, OrderedEventWithThread> lastWriteForVariable = new HashMap<>();
        // TODO(virgil): signal control can be more fine-grained.
        Optional<OrderedEventWithThread> lastSignalEvent = Optional.empty();
        Map<Long, OrderedEventWithThread> lastLockEvent = new HashMap<>();
        Optional<OrderedEventWithThread> lastAtomicEvent = Optional.empty();
        Map<Integer, OrderedEventWithThread> lastEventForThread = new HashMap<>();
        Map<Integer, Integer> ttidToIndex = new HashMap<>();
        threadToExecution.keySet().forEach(ttid -> ttidToIndex.put(ttid, 0));
        Optional<OrderedEventWithThread> maybeNextEvent = goToNextEvent(threadToExecution, ttidToIndex);
        while (maybeNextEvent.isPresent()) {
            OrderedEventWithThread nextEvent = maybeNextEvent.get();
            EventWithOrder eventWithOrder = getOrderedEvent(threadToExecution, nextEvent);
            Optional<OrderedEventWithThread> maybePreviousThreadEvent =
                    Optional.ofNullable(lastEventForThread.get(nextEvent.thread));
            maybePreviousThreadEvent.ifPresent(previousEvent ->
                    addDependency(dependencies, nextEvent, previousEvent));
            ReadonlyEventInterface event = eventWithOrder.getEvent();
            lastEventForThread.put(nextEvent.thread, nextEvent);
            if (event.isRead()) {
                Optional<OrderedEventWithThread> maybePreviousWrite =
                        Optional.ofNullable(lastWriteForVariable.get(event.getDataInternalIdentifier()));
                maybePreviousWrite.ifPresent(previousWrite -> addDependency(dependencies, nextEvent, previousWrite));
                lastReadForVariable.put(event.getDataInternalIdentifier(), nextEvent);
            } else if (event.isWrite()) {
                Optional<OrderedEventWithThread> maybePreviousRead =
                        Optional.ofNullable(lastReadForVariable.get(event.getDataInternalIdentifier()));
                maybePreviousRead.ifPresent(previousRead -> addDependency(dependencies, nextEvent, previousRead));
                lastWriteForVariable.put(event.getDataInternalIdentifier(), nextEvent);
            } else if (event.isLock() || event.isUnlock()) {
                Optional<OrderedEventWithThread> maybePreviousLock =
                        Optional.ofNullable(lastLockEvent.get(event.getLockId()));
                maybePreviousLock.ifPresent(previousLock -> addDependency(dependencies, nextEvent, previousLock));
                lastLockEvent.put(event.getLockId(), nextEvent);
                if (event.isAtomic()) {
                    lastSignalEvent.ifPresent(signalEvent -> addDependency(dependencies, nextEvent, signalEvent));
                    lastAtomicEvent = Optional.of(nextEvent);
                }
            } else if (event.isSignalEvent()) {
                lastAtomicEvent.ifPresent(atomicEvent -> addDependency(dependencies, nextEvent, atomicEvent));
                lastSignalEvent.ifPresent(signalEvent -> addDependency(dependencies, nextEvent, signalEvent));
                lastSignalEvent = Optional.of(nextEvent);
            }

            maybeNextEvent = goToNextEvent(threadToExecution, ttidToIndex);
        }
        return dependencies;
    }

    private Optional<OrderedEventWithThread> goToNextEvent(
            Map<Integer, List<EventWithOrder>> threadToExecution,
            Map<Integer, Integer> ttidToIndex) {
        OptionalLong minOrder = OptionalLong.empty();
        OptionalInt minThread = OptionalInt.empty();
        for (Map.Entry<Integer, Integer> entry : ttidToIndex.entrySet()) {
            int ttid = entry.getKey();
            int index = entry.getValue();
            List<EventWithOrder> events = threadToExecution.get(ttid);
            if (index >= events.size()) {
                continue;
            }
            EventWithOrder eventWithOrder = events.get(index);
            if (!minOrder.isPresent()) {
                minOrder = OptionalLong.of(eventWithOrder.getOrderId());
                minThread = OptionalInt.of(ttid);
                continue;
            }
            if (minOrder.getAsLong() > eventWithOrder.getOrderId()) {
                minOrder = OptionalLong.of(eventWithOrder.getOrderId());
                minThread = OptionalInt.of(ttid);
            }
        }
        if (!minThread.isPresent()) {
            return Optional.empty();
        }
        int thread = minThread.getAsInt();
        int eventIndex = ttidToIndex.get(thread);
        ttidToIndex.put(thread, eventIndex + 1);
        return Optional.of(new OrderedEventWithThread(minThread.getAsInt(), eventIndex));
    }

    private boolean addDependency(
            Map<OrderedEventWithThread, Set<OrderedEventWithThread>> dependencies,
            OrderedEventWithThread dependent, OrderedEventWithThread dependency) {
        return dependencies.computeIfAbsent(dependent, k -> new HashSet<>()).add(dependency);
    }

    private List<OrderedEventWithThread> topologicalSortingWithFewerThreadSwitches(
            Map<OrderedEventWithThread, Set<OrderedEventWithThread>> dependencyGraph) {
        Map<OrderedEventWithThread, List<OrderedEventWithThread>> eventToEventsThatDependOnIt = new HashMap<>();
        dependencyGraph.forEach((dependent, dependencies) ->
                dependencies.forEach(dependency ->
                        eventToEventsThatDependOnIt
                                .computeIfAbsent(dependency, k -> new ArrayList<>())
                                .add(dependent)));
        Map<Integer, OrderedEventWithThread> threadToEventWithoutDependencies = new HashMap<>();
        dependencyGraph.forEach((dependent, dependencies) ->
                dependencies.stream()
                        .filter(dependency -> !dependencyGraph.containsKey(dependency))
                        .forEach(dependency -> threadToEventWithoutDependencies.put(dependency.thread, dependency)));
        Map<OrderedEventWithThread, Integer> dependencyCount = new HashMap<>();
        dependencyGraph.forEach((dependent, dependencies) -> dependencyCount.put(dependent, dependencies.size()));
        int currentThread = Constants.INVALID_TTID;
        List<OrderedEventWithThread> sorted = new ArrayList<>();
        while (!threadToEventWithoutDependencies.isEmpty()) {
            if (!threadToEventWithoutDependencies.containsKey(currentThread)) {
                currentThread = threadToEventWithoutDependencies.keySet().iterator().next();
            }
            OrderedEventWithThread oetw = threadToEventWithoutDependencies.get(currentThread);
            sorted.add(oetw);
            threadToEventWithoutDependencies.remove(currentThread);
            eventToEventsThatDependOnIt.getOrDefault(oetw, Collections.emptyList()).forEach(dependent ->
                    dependencyCount.compute(dependent, (key, value) -> {
                        value = value - 1;
                        if (value == 0) {
                            assert !threadToEventWithoutDependencies.containsKey(dependent.thread);
                            threadToEventWithoutDependencies.put(dependent.thread, dependent);
                        }
                        return value;
                    }));
        }
        return sorted;
    }

    private void removeRaceEventsAndEventsNotInvolvedInRace(
            Map<OrderedEventWithThread, Set<OrderedEventWithThread>> dependencyGraph,
            OrderedEventWithThread firstRaceEvent,
            OrderedEventWithThread secondRaceEvent) {
        Set<OrderedEventWithThread> validEvents = new HashSet<>();
        Stack<OrderedEventWithThread> toProcess = new Stack<>();
        toProcess.push(firstRaceEvent);
        toProcess.push(secondRaceEvent);
        while (!toProcess.isEmpty()) {
            OrderedEventWithThread oetw = toProcess.pop();
            for (OrderedEventWithThread dependency : dependencyGraph.getOrDefault(oetw, Collections.emptySet())) {
                if (validEvents.contains(dependency)) {
                    continue;
                }
                validEvents.add(dependency);
                toProcess.push(dependency);
            }
        }
        dependencyGraph.keySet().removeIf(oetw -> !validEvents.contains(oetw));
        dependencyGraph.values().forEach(oetws -> oetws.removeIf(oetw -> !validEvents.contains(oetw)));
    }

    private EventWithOrder getOrderedEvent(
            Map<Integer, List<EventWithOrder>> threadToExecution, OrderedEventWithThread oewt) {
        return threadToExecution.get(oewt.thread).get(oewt.eventIndex);
    }

    private Optional<OrderedEventWithThread> findEventForId(
            Map<Integer, List<EventWithOrder>> threadToExecution,
            long eventId) {
        for (Map.Entry<Integer, List<EventWithOrder>> entry : threadToExecution.entrySet()) {
            Integer ttid = entry.getKey();
            List<EventWithOrder> events = entry.getValue();
            for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
                ReadonlyEventInterface event = events.get(eventIndex).getEvent();
                if (event.getEventId() == eventId) {
                    return Optional.of(new OrderedEventWithThread(ttid, eventIndex));
                }
            }
        }
        return Optional.empty();
    }

    private class OrderedEventWithThread {
        private final Integer thread;
        private final int eventIndex;

        OrderedEventWithThread(Integer thread, int eventIndex) {
            this.thread = thread;
            this.eventIndex = eventIndex;
        }

        @Override
        public int hashCode() {
            return thread.hashCode() ^ Integer.hashCode(eventIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OrderedEventWithThread)) {
                return false;
            }
            OrderedEventWithThread other = (OrderedEventWithThread) obj;
            return other.thread.equals(thread) && other.eventIndex == eventIndex;
        }

        @Override
        public String toString() {
            return thread + " " + eventIndex;
        }
    }

    private String prettyPrint(ReadonlyEventInterface event) {
        return event.getType().getPrinter().print(event);
    }

    private String threadDescription(int threadId, EventWithOrder event, Map<Integer, Integer> signalParents) {
        if (trace.getThreadType(threadId) == ThreadType.THREAD) {
            return "T" + event.getEvent().getOriginalThreadId();
        }
        return "(<S" + trace.getSignalNumber(threadId) + "> "
                    + getInterruptionDescription(threadId, signalParents) + ")";
    }

    private String getInterruptionDescription(int threadId, Map<Integer, Integer> signalParents) {
        StringBuilder description = new StringBuilder();
        int currentThreadId = threadId;
        while (true) {
            Optional<Integer> maybeNextThreadId = Optional.ofNullable(signalParents.get(currentThreadId));
            assert maybeNextThreadId.isPresent();
            int nextThreadId = maybeNextThreadId.get();
            description.append(currentThreadId == threadId ? "interrupting " : "which interrupts ");
            if (trace.getThreadType(nextThreadId) == ThreadType.THREAD) {
                description.append("T");
                description.append(trace.getOriginalThreadIdForTraceThreadId(nextThreadId));
                break;
            }
            description.append("<S");
            description.append(trace.getSignalNumber(nextThreadId));
            description.append(">");
            currentThreadId = nextThreadId;
        }
        return description.toString();
    }
}
