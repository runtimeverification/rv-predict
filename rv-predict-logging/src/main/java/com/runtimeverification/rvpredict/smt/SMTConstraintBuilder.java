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

import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.BOOL_EQUAL;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.LESS_THAN;
import static com.runtimeverification.rvpredict.smt.formula.FormulaTerm.OR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.smt.formula.AbstractPhiVariable;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;
import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.ConcretePhiVariable;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.OrderVariable;
import com.runtimeverification.rvpredict.trace.LockRegion;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;

public class SMTConstraintBuilder {

    private final Trace trace;

    /**
     * Keeps track of the must-happen-before (MHB) relations in paper.
     * <p>
     * The must-happen-before (MHB) relations form a special DAG where only a
     * few nodes have more than one outgoing edge. To speed up the reachability
     * query between two nodes, we first collapsed the DAG as much as possible.
     */
    private TransitiveClosure mhbClosure;

    private final LockSetEngine lockEngine = new LockSetEngine();

    private final Solver solver;

    private final Map<Event, BoolFormula> abstractPhi = Maps.newHashMap();
    private final Map<Event, BoolFormula> concretePhi = Maps.newHashMap();

    /**
     * Avoids infinite recursion when building the abstract feasibility
     * constraint of a {@link MemoryAccessEvent}.
     */
    private final Set<Event> computedAbstractPhi = Sets.newHashSet();

    /**
     * Avoids infinite recursion when building the concrete feasibility
     * constraint of a {@link MemoryAccessEvent}.
     */
    private final Set<Event> computedConcretePhi = Sets.newHashSet();

    private final Map<Event, OrderVariable> eventToOrderVar = new HashMap<>();

    // constraints below
    private final FormulaTerm.Builder formulaBuilder = FormulaTerm.andBuilder();

    public SMTConstraintBuilder(Configuration config, Trace trace) {
        this.trace = trace;
        this.solver = new Z3Wrapper(config);
    }

    private OrderVariable getOrderVariable(Event event) {
        return eventToOrderVar.computeIfAbsent(event, p -> new OrderVariable(event));
    }

    private FormulaTerm getAsstHappensBefore(Event event1, Event event2) {
        return LESS_THAN(getOrderVariable(event1), getOrderVariable(event2));
    }

    private FormulaTerm getAsstLockRegionHappensBefore(LockRegion lockRegion1, LockRegion lockRegion2) {
        Event unlock = lockRegion1.getUnlock();
        Event lock = lockRegion2.getLock();
        return getAsstHappensBefore(
                unlock != null ? unlock : trace.getLastEvent(lockRegion1.getTID()),
                lock != null ? lock : trace.getFirstEvent(lockRegion2.getTID()));
    }

    private void assertMutex(LockRegion lockRegion1, LockRegion lockRegion2) {
        formulaBuilder.add(OR(
                getAsstLockRegionHappensBefore(lockRegion1, lockRegion2),
                getAsstLockRegionHappensBefore(lockRegion2, lockRegion1)));
    }

    private int getRelativeIdx(Event event) {
        return (int) (event.getGID() - trace.getBaseGID());
    }

    /**
     * Adds must-happen-before (MHB) constraints.
     */
    public void addPhiMHB() {
        TransitiveClosure.Builder mhbClosureBuilder = TransitiveClosure.builder(trace.getSize());

        /* build intra-thread program order constraint */
        trace.threadViews().forEach(events -> {
            mhbClosureBuilder.createNewGroup(getRelativeIdx(events.get(0)));
            for (int i = 1; i < events.size(); i++) {
                Event e1 = events.get(i - 1);
                Event e2 = events.get(i);
                formulaBuilder.add(getAsstHappensBefore(e1, e2));
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
                    formulaBuilder.add(getAsstHappensBefore(event, fst));
                    mhbClosureBuilder.addRelation(getRelativeIdx(event), getRelativeIdx(fst));
                }
            } else if (event.isJoin()) {
                Event last = trace.getLastEvent(event.getSyncObject());
                if (last != null) {
                    formulaBuilder.add(getAsstHappensBefore(last, event));
                    mhbClosureBuilder.addRelation(getRelativeIdx(last), getRelativeIdx(event));
                }
            }
        });

        mhbClosure = mhbClosureBuilder.build();
    }

    /**
     * Adds lock mutual exclusion constraints.
     */
    public void addPhiLock() {
        trace.getLockIdToLockRegions().forEach((lockId, lockRegions) -> {
            lockRegions.forEach(lockEngine::add);

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

    /**
     * Generates a formula ensuring that all read events that {@code event}
     * depends on read the same value as in the original trace, to guarantee
     * {@code event} will be generated in the predicted trace. Note that,
     * however, this {@code event} is allowed to read or write a different value
     * than in the original trace.
     */
    private BoolFormula getPhiAbs(Event event) {
        if (computedAbstractPhi.contains(event)) {
            return new AbstractPhiVariable(event);
        }
        computedAbstractPhi.add(event);

        FormulaTerm.Builder phiBuilder = FormulaTerm.andBuilder();
        /* make sure that every dependent read event reads the same value as in the original trace */
        for (Event depRead : trace.getCtrlFlowDependentEvents(event)) {
            phiBuilder.add(getPhiConc(depRead));
        }
        abstractPhi.put(event, phiBuilder.build());
        return new AbstractPhiVariable(event);
    }

    /**
     * Generates a formula ensuring that {@code event} will be generated exactly
     * the same as in the original trace <b>if</b> the corresponding data-abstract
     * feasibility constraint is also satisfied.
     */
    private BoolFormula getPhiConc(Event event) {
        if (computedConcretePhi.contains(event)) {
            return new ConcretePhiVariable(event);
        } else if (event.getValue() == Constants._0X_DEADBEEFL) {
            return BooleanConstant.TRUE;
        }
        computedConcretePhi.add(event);

        BoolFormula phi;
        if (event.isRead()) {
            List<Event> writeEvents = trace.getWriteEvents(event.getAddr());

            // all write events that could interfere with the read event
            List<Event> predWrites = Lists.newArrayList();
            Event sameThreadPredWrite = null;
            for (Event write : writeEvents) {
                if (write.getTID() == event.getTID()) {
                    if (write.getGID() < event.getGID()) {
                        sameThreadPredWrite = write;
                    }
                } else if (!happensBefore(event, write)) {
                    predWrites.add(write);
                }
            }
            if (sameThreadPredWrite != null) {
                predWrites.add(sameThreadPredWrite);
            }

            // all write events whose values could be read by the read event
            List<Event> sameValPredWrites = predWrites.stream()
                    .filter(w -> w.getValue() == event.getValue()).collect(Collectors.toList());

            /* case 1: the read event reads the initial value */
            BoolFormula case1 = BooleanConstant.FALSE;
            if (sameThreadPredWrite == null &&
                    trace.getInitValueOf(event.getAddr()) == event.getValue()) {
                FormulaTerm.Builder builder = FormulaTerm.andBuilder();
                predWrites.forEach(w -> builder.add(getAsstHappensBefore(event, w)));
                case1 = builder.build();
            }

            /* case 2: the read event reads a previously written value */
            FormulaTerm.Builder case2Builder = FormulaTerm.orBuilder();
            sameValPredWrites.forEach(w1 -> {
                FormulaTerm.Builder builder = FormulaTerm.andBuilder();
                builder.add(getPhiAbs(w1), getPhiConc(w1));
                builder.add(getAsstHappensBefore(w1, event));
                predWrites.forEach(w2 -> {
                    if (w2.getValue() != w1.getValue() && !happensBefore(w2, w1)) {
                        builder.add(OR(getAsstHappensBefore(w2, w1),
                                getAsstHappensBefore(event, w2)));
                    }
                });
                case2Builder.add(builder.build());
            });
            phi = OR(case1, case2Builder.build());
        } else {
            phi = BooleanConstant.TRUE;
        }
        concretePhi.put(event, phi);

        return new ConcretePhiVariable(event);
    }

    /**
     * Checks if one event happens before another.
     */
    private boolean happensBefore(Event e1, Event e2) {
        return mhbClosure.inRelation(getRelativeIdx(e1), getRelativeIdx(e2));
    }

    public boolean isRace(Event e1, Event e2) {
        /* not a race if the two events hold a common lock or one event
         * happens-before the other */
        if (lockEngine.hasCommonLock(e1, e2) || happensBefore(e1, e2) || happensBefore(e2, e1)) {
            return false;
        }

        FormulaTerm.Builder raceAsstBuilder = FormulaTerm.andBuilder();
        raceAsstBuilder.add(formulaBuilder.build());
        raceAsstBuilder.add(getPhiAbs(e1));
        raceAsstBuilder.add(getPhiAbs(e2));
        abstractPhi.forEach((e, phi) -> {
            raceAsstBuilder.add(BOOL_EQUAL(new AbstractPhiVariable(e), phi));
        });
        concretePhi.forEach((e, phi) -> {
            raceAsstBuilder.add(BOOL_EQUAL(new ConcretePhiVariable(e), phi));
        });
        raceAsstBuilder.add(FormulaTerm
                .INT_EQUAL(getOrderVariable(e1), getOrderVariable(e2)));

        return solver.isSat(raceAsstBuilder.build());
    }

}
