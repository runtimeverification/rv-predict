package com.runtimeverification.rvpredict.engine.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.trace.*;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.smt.SMTConstraintBuilder;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.*;

/**
 * Detects data races from a given {@link Trace} object
 * <p/>
 * <p/>
 * We analyze memory access events on each shared memory address in the
 * trace separately. For each shared memory address, enumerate all memory
 * access pairs on this address and build the data-abstract feasibility for
 * each of them. Then for each memory access pair, send to the SMT solver
 * its data-abstract feasibility together with the already built must
 * happen-before (MHB) constraints and locking constraints. The pair is
 * reported as a real data race if the solver returns sat.
 * <p/>
 * <p/>
 * To reduce the expensive calls to the SMT solver, we apply three
 * optimizations:
 * <li>Use Lockset + Weak HB algorithm to filter out those memory access
 * pairs that are obviously not data races.
 * <li>Group "equivalent" memory access events to a block and consider them
 * as a single memory access. In short, such block has the property that all
 * memory access events in it have the same causal HB relation with the
 * outside events. Therefore, it is sufficient to consider only one event
 * from each block.
 *
 */
public class RaceDetectorTask implements Runnable {
    private final RVPredict RVPredict;
    private final Trace trace;

    public RaceDetectorTask(RVPredict RVPredict, Trace trace) {
        this.RVPredict = RVPredict;
        this.trace = trace;
    }


    @Override
    public void run() {
        SMTConstraintBuilder cnstrBuilder = new SMTConstraintBuilder(RVPredict.getConfig(), trace);

        cnstrBuilder.declareVariables();
        if (RVPredict.getConfig().rmm_pso) {
            cnstrBuilder.addPSOIntraThreadConstraints();
        } else {
            cnstrBuilder.addIntraThreadConstraints();
        }
        cnstrBuilder.addProgramOrderAndThreadStartJoinConstraints();
        cnstrBuilder.addLockingConstraints();
        /* enumerate each shared memory address in the trace */
        for (MemoryAddr addr : trace.getMemAccessEventsTable().rowKeySet()) {
            /* exclude volatile variable */
            if (!RVPredict.getConfig().checkVolatile && trace.isVolatileField(addr)) {
                continue;
            }

            /* skip if there is no write event */
            if (trace.getWriteEventsOn(addr).isEmpty()) {
                continue;
            }

            /* skip if there is only one thread */
            if (trace.getMemAccessEventsTable().row(addr).size() == 1) {
                continue;
            }

            /* group equivalent reads and writes into memory access blocks */
            Map<MemoryAccessEvent, List<MemoryAccessEvent>> equivAccBlk = new LinkedHashMap<MemoryAccessEvent, List<MemoryAccessEvent>>();
            for (Map.Entry<Long, List<MemoryAccessEvent>> entry : trace
                    .getMemAccessEventsTable().row(addr).entrySet()) {
                // TODO(YilongL): the extensive use of List#indexOf could be a performance problem later

                long crntTID = entry.getKey();
                List<MemoryAccessEvent> memAccEvents = entry.getValue();

                List<Event> crntThrdEvents = trace.getThreadEvents(crntTID);

                ListIterator<MemoryAccessEvent> iter = memAccEvents.listIterator();
                while (iter.hasNext()) {
                    MemoryAccessEvent memAcc = iter.next();
                    equivAccBlk.put(memAcc, Lists.newArrayList(memAcc));
                    if (memAcc instanceof WriteEvent) {
                        int prevMemAccIdx = crntThrdEvents.indexOf(memAcc);

                        while (iter.hasNext()) {
                            MemoryAccessEvent crntMemAcc = iter.next();
                            int crntMemAccIdx = crntThrdEvents.indexOf(crntMemAcc);

                            /* ends the block if there is sync/branch-event in between */
                            boolean memAccOnly = true;
                            for (Event e : crntThrdEvents.subList(prevMemAccIdx + 1, crntMemAccIdx)) {
                                memAccOnly = memAccOnly && (e instanceof MemoryAccessEvent);
                            }
                            if (!memAccOnly) {
                                iter.previous();
                                break;
                            }

                            equivAccBlk.get(memAcc).add(crntMemAcc);
                            if (!RVPredict.getConfig().branch) {
                                /* YilongL: without logging branch events, we
                                 * have to be conservative and end the block
                                 * when a read event is encountered */
                                if (crntMemAcc instanceof ReadEvent) {
                                    break;
                                }
                            }

                            prevMemAccIdx = crntMemAccIdx;
                        }
                    }
                }
            }

            /* check memory access pairs */
            for (MemoryAccessEvent fst : equivAccBlk.keySet()) {
                for (MemoryAccessEvent snd : equivAccBlk.keySet()) {
                    if (fst.getTID() >= snd.getTID()) {
                        continue;
                    }

                    /* skip if all potential data races are already known */
                    Set<Race> potentialRaces = Sets.newHashSet();
                    for (MemoryAccessEvent e1 : equivAccBlk.get(fst)) {
                        for (MemoryAccessEvent e2 : equivAccBlk.get(snd)) {
                            if ((e1 instanceof WriteEvent || e2 instanceof WriteEvent)
                                    && !trace.isClinitMemoryAccess(e1)
                                    && !trace.isClinitMemoryAccess(e2)) {
                                potentialRaces.add(new Race(e1, e2, trace,
                                        RVPredict.getLoggingFactory()));
                            }
                        }
                    }
                    boolean hasFreshRace = false;
                    for (Race potentialRace : potentialRaces) {
                        hasFreshRace = !RVPredict.getViolations().contains(potentialRace);
                        if (hasFreshRace) break;
                    }
                    if (!hasFreshRace) {
                        /* YilongL: note that this could lead to miss of data
                         * races if their signatures are the same */
                        continue;
                    }

                    /* not a race if the two events hold a common lock */
                    if (cnstrBuilder.hasCommonLock(fst, snd)) {
                        continue;
                    }

                    /* not a race if one event happens-before the other */
                    if (fst.getGID() < snd.getGID()
                            && cnstrBuilder.happensBefore(fst, snd)
                            || fst.getGID() > snd.getGID()
                            && cnstrBuilder.happensBefore(snd, fst)) {
                        continue;
                    }

                    /* start building constraints for MCM */
                    StringBuilder sb = new StringBuilder()
                            .append(cnstrBuilder.getAbstractFeasibilityConstraint(fst)).append(" ")
                            .append(cnstrBuilder.getAbstractFeasibilityConstraint(snd));

                    if (cnstrBuilder.isRace(fst, snd, sb)) {
                        for (Race race : potentialRaces) {
                            if (RVPredict.getViolations().add(race)) {
                                String report = RVPredict.getConfig().simple_report ?
                                        race.toString() : race.generateRaceReport();
                                RVPredict.getLogger().report(report, Logger.MSGTYPE.REAL);
                            }
                        }
                    }
                }
            }
        }
    }

}