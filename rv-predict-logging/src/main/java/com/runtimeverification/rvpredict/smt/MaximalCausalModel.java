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

import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;
import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.ConcretePhiVariable;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.OrderVariable;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
import com.runtimeverification.rvpredict.trace.LockRegion;
import com.runtimeverification.rvpredict.trace.MemoryAccessBlock;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.violation.Race;

public class MaximalCausalModel {

    /**
     * Inlining M as a large integer is faster than declaring it as an order
     * variable with fixed value, which is again much faster than declaring it
     * as an order variable with unknown value.
     */
    private static final IntConstant M = new IntConstant(Long.MAX_VALUE / 1024);

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
    private final Map<Event, BoolFormula> readToPhiConc = new HashMap<>();

    /**
     * The formula that describes the maximal causal model of the trace tau.
     */
    private final FormulaTerm.Builder phiTau = FormulaTerm.andBuilder();

    private static Context z3Context; {
        try {
            z3Context = new Context();
        } catch (Z3Exception e) {
            throw new RuntimeException();
        }
    }

    public static MaximalCausalModel create(Trace trace) {
        MaximalCausalModel model = new MaximalCausalModel(trace);
        model.addPhiMHB();
        model.addPhiLock();
        model.addPhiE();
        return model;
    }

    private MaximalCausalModel(Trace trace) {
        this.trace = trace;
    }

    private FormulaTerm getAsstHappensBefore(Event event1, Event event2) {
        return LESS_THAN(OrderVariable.get(event1), OrderVariable.get(event2));
    }

    private FormulaTerm getAsstLockRegionHappensBefore(LockRegion lockRegion1, LockRegion lockRegion2) {
        Event unlock = lockRegion1.getUnlock();
        Event lock = lockRegion2.getLock();
        return getAsstHappensBefore(
                unlock != null ? unlock : trace.getLastEvent(lockRegion1.getTID()),
                lock != null ? lock : trace.getFirstEvent(lockRegion2.getTID()));
    }

    private void assertMutex(LockRegion lockRegion1, LockRegion lockRegion2) {
        phiTau.add(OR(
                getAsstLockRegionHappensBefore(lockRegion1, lockRegion2),
                getAsstLockRegionHappensBefore(lockRegion2, lockRegion1)));
    }

    private int getRelativeIdx(Event event) {
        return (int) (event.getGID() - trace.getBaseGID());
    }

    /**
     * Adds must-happen-before (MHB) constraints.
     */
    private void addPhiMHB() {
        TransitiveClosure.Builder mhbClosureBuilder = TransitiveClosure.builder(trace.getSize());

        /* build intra-thread program order constraint */
        trace.eventsByThreadID().forEach((tid, events) -> {
            mhbClosureBuilder.createNewGroup(getRelativeIdx(events.get(0)));
            for (int i = 1; i < events.size(); i++) {
                Event e1 = events.get(i - 1);
                Event e2 = events.get(i);
                phiTau.add(getAsstHappensBefore(e1, e2));
                /* every group should start with a join event and end with a start event */
                if (e1.isStart() || e2.isJoin()) {
                    mhbClosureBuilder.createNewGroup(getRelativeIdx(e2));
                    mhbClosureBuilder.addRelation(getRelativeIdx(e1), getRelativeIdx(e2));
                } else {
                    mhbClosureBuilder.addToGroup(getRelativeIdx(e2), getRelativeIdx(e1));
                }
            }
        });

        /* build inter-thread synchronization constraint */
        trace.getInterThreadSyncEvents().forEach(event -> {
            if (event.isStart()) {
                Event fst = trace.getFirstEvent(event.getSyncObject());
                if (fst != null) {
                    phiTau.add(getAsstHappensBefore(event, fst));
                    mhbClosureBuilder.addRelation(getRelativeIdx(event), getRelativeIdx(fst));
                }
            } else if (event.isJoin()) {
                Event last = trace.getLastEvent(event.getSyncObject());
                if (last != null) {
                    phiTau.add(getAsstHappensBefore(last, event));
                    mhbClosureBuilder.addRelation(getRelativeIdx(last), getRelativeIdx(event));
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
                    if (lr1.getTID() < lr2.getTID()
                            && (lr1.isWriteLocked() || lr2.isWriteLocked())) {
                        assertMutex(lr1, lr2);
                    }
                });
            });
        });
    }

    private void addPhiE() {
        trace.memoryAccessBlocksByThreadID().forEach((tid, blocks) -> {
            for (int i = 0; i < blocks.size(); i++) {
                MemoryAccessBlock block = blocks.get(i);
                Event read = block.getFirstRead();
                if (read != null) {
                    FormulaTerm.Builder phiE = FormulaTerm.orBuilder();
                    OrderVariable O_r = OrderVariable.get(read);
                    /* case 1: read is infeasible */
                    phiE.add(LESS_THAN(M, O_r));
                    /* case 2: read is feasible */
                    phiE.add(getPhiConc(block));
                    /* case 3: read is data-abstract feasible */
                    FormulaTerm.Builder case3 = FormulaTerm.andBuilder();
                    if (i > 0) {
                        case3.add(getPhiAbs(block));
                    }
                    if (i + 1 < blocks.size()) {
                        Event nextEvent = blocks.get(i + 1).getFirst();
                        case3.add(LESS_THAN(M, OrderVariable.get(nextEvent)));
                    }
                    phiE.add(case3.build());
                    /* add Phi_e to Phi_tau */
                    phiTau.add(phiE.build());
                }
            }
        });

        readToPhiConc.forEach((e, phi) -> phiTau.add(BOOL_EQUAL(new ConcretePhiVariable(e), phi)));
    }

    private BoolFormula getPhiConc(MemoryAccessBlock block) {
        Event read = block.getFirstRead();
        if (read == null) {
            return getPhiAbs(block);
        } else if (read.getValue() == Constants._0X_DEADBEEFL) {
            return BooleanConstant.TRUE;
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

    private BoolFormula getPhiSC(Event read) {
        FormulaTerm.Builder phiSC = FormulaTerm.orBuilder();

        // all write events that could interfere with the read event
        List<Event> predWrites = new ArrayList<>();
        Event sameThreadPredWrite = null;
        for (Event write : trace.getWriteEvents(read.getAddr())) {
            if (write.getTID() == read.getTID()) {
                if (write.getGID() < read.getGID()) {
                    sameThreadPredWrite = write;
                }
            } else if (!happensBefore(read, write)) {
                predWrites.add(write);
            }
        }
        if (sameThreadPredWrite != null) {
            predWrites.add(sameThreadPredWrite);
        }

        // all write events whose values could be read by the read event
        List<Event> sameValPredWrites = predWrites.stream()
                .filter(w -> w.getValue() == read.getValue()).collect(Collectors.toList());

        /* case 1: the read event reads the initial value */
        if (sameThreadPredWrite == null &&
                trace.getInitValueOf(read.getAddr()) == read.getValue()) {
            FormulaTerm.Builder case1 = FormulaTerm.andBuilder();
            predWrites.forEach(w -> case1.add(getAsstHappensBefore(read, w)));
            phiSC.add(case1.build());
        }

        /* case 2: the read event reads a previously written value */
        sameValPredWrites.forEach(w1 -> {
            FormulaTerm.Builder case2 = FormulaTerm.andBuilder();
            case2.add(getPhiAbs(trace.getMemoryAccessBlock(w1)));
            case2.add(getAsstHappensBefore(w1, read));
            predWrites.forEach(w2 -> {
                if (w2.getValue() != w1.getValue() && !happensBefore(w2, w1)) {
                    case2.add(OR(getAsstHappensBefore(w2, w1),
                            getAsstHappensBefore(read, w2)));
                }
            });
            phiSC.add(case2.build());
        });
        return phiSC.build();
    }

    /**
     * Checks if one event happens before another.
     */
    private boolean happensBefore(Event e1, Event e2) {
        return mhbClosure.inRelation(getRelativeIdx(e1), getRelativeIdx(e2));
    }

    private boolean checkPecanCondition(Race race) {
        Event e1 = race.firstEvent();
        Event e2 = race.secondEvent();
        return !locksetEngine.hasCommonLock(e1, e2) && !happensBefore(e1, e2)
                && !happensBefore(e2, e1);
    }

    private BoolFormula getRaceAssertion(Race race) {
        Event e1 = race.firstEvent();
        Event e2 = race.secondEvent();
        return AND(INT_EQUAL(OrderVariable.get(e1), OrderVariable.get(e2)),
                LESS_THAN(OrderVariable.get(e1), M));
    }

    /**
     * Checks if the given race suspects are real. Race suspects are grouped by
     * their signatures.
     *
     * @param sigToRaceSuspects
     * @param timeout
     *            solver timeout in seconds
     * @return a map from race signatures to real race instances
     */
    public Map<String, Race> checkRaceSuspects(Map<String, List<Race>> sigToRaceSuspects, int timeout) {
        /* build phiRace */
        FormulaTerm.Builder phiRace = FormulaTerm.orBuilder();
        Map<Race, BoolFormula> suspectToAsst = new HashMap<>();
        sigToRaceSuspects.values().forEach(candidates -> {
           candidates.removeIf(p -> !checkPecanCondition(p));
                    candidates.forEach(p -> phiRace.add(suspectToAsst.computeIfAbsent(p,
                            this::getRaceAssertion)));
        });
        sigToRaceSuspects.entrySet().removeIf(e -> e.getValue().isEmpty());
//        sigToRaceSuspects.forEach((sig, l) -> System.err.println(sig + ": " + l.size()));

        Map<String, Race> result = new HashMap<>();
        Z3Filter z3filter = new Z3Filter(z3Context);
        com.microsoft.z3.Solver solver;
        try {
            // mkSimpleSolver < mkSolver < mkSolver("QF_IDL")
            solver = z3Context.mkSimpleSolver();
            Params params = z3Context.mkParams();
            params.add("timeout", timeout * 1000);
            solver.setParameters(params);
            solver.add(z3filter.filter(phiTau.build()));
            solver.add(z3filter.filter(phiRace.build()));
//            System.err.println(z3filter.filter(phiTau.build()));
//            System.err.println(z3filter.filter(phiRace.build()));
            Status status = solver.check();
//            System.err.println(status);
            if (status == Status.SATISFIABLE) {
                for (Map.Entry<String, List<Race>> entry : sigToRaceSuspects.entrySet()) {
                    for (Race race : entry.getValue()) {
                        solver.push();
                        solver.add(z3filter.filter(suspectToAsst.get(race)));
                        boolean isRace = solver.check() == Status.SATISFIABLE;
                        solver.pop();
                        if (isRace) {
                            result.put(entry.getKey(), race);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
