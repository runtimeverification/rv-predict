package com.runtimeverification.rvpredict.engine.main;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.smt.SMTConstraintBuilder;
import com.runtimeverification.rvpredict.smt.formula.Formula;
import com.runtimeverification.rvpredict.trace.MemoryAddr;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.violation.Race;
import com.runtimeverification.rvpredict.violation.Violation;

/**
 * Detects data races from a given {@link Trace} object.
 * <p>
 * We analyze memory access events on each shared memory address in the
 * trace separately. For each shared memory address, enumerate all memory
 * access pairs on this address and build the data-abstract feasibility for
 * each of them. Then for each memory access pair, send to the SMT solver
 * its data-abstract feasibility together with the already built must
 * happen-before (MHB) constraints and locking constraints. The pair is
 * reported as a real data race if the solver returns sat.
 * <p>
 * To reduce the expensive calls to the SMT solver, we apply two
 * optimizations:
 * <li>Use Lockset + Weak HB algorithm to filter out those memory access
 * pairs that are obviously not data races.
 * <li>Group "equivalent" memory access events to a block and consider them
 * as a single memory access. In short, such block has the property that all
 * memory access events in it have the same causal HB relation with the
 * outside events. Therefore, it is sufficient to consider only one event
 * from each block.
 *
 * @author YilongL
 */
public class RaceDetectorTask implements Runnable {

    private final Configuration config;

    private final Metadata metadata;

    private final Trace trace;

    private final Set<Violation> violations;

    public RaceDetectorTask(Configuration config, Metadata metadata, Trace trace,
            Set<Violation> violations) {
        this.config = config;
        this.metadata = metadata;
        this.trace = trace;
        this.violations = violations;
    }

    @Override
    public void run() {
        SMTConstraintBuilder cnstrBuilder = new SMTConstraintBuilder(config, trace);

        cnstrBuilder.addIntraThreadConstraints();
        cnstrBuilder.addThreadStartJoinConstraints();
        cnstrBuilder.addLockingConstraints();
        cnstrBuilder.finish();
        /* enumerate each shared memory address in the trace */
        for (MemoryAddr addr : trace.getMemAccessEventsTable().rowKeySet()) {
            /* exclude unsafe address */
            if (trace.isUnsafeAddress(addr)) {
                continue;
            }

            /* exclude volatile variable */
            if (!config.checkVolatile && trace.isVolatileField(addr)) {
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
            Map<EventItem, List<EventItem>> equivAccBlk = new LinkedHashMap<>();
            for (Map.Entry<Long, List<EventItem>> entry : trace
                    .getMemAccessEventsTable().row(addr).entrySet()) {
                // TODO(YilongL): the extensive use of List#indexOf could be a performance problem later

                long crntTID = entry.getKey();
                List<EventItem> memAccEvents = entry.getValue();

                List<EventItem> crntThrdEvents = trace.getThreadEvents(crntTID);

                ListIterator<EventItem> iter = memAccEvents.listIterator();
                while (iter.hasNext()) {
                    EventItem memAcc = iter.next();
                    equivAccBlk.put(memAcc, Lists.newArrayList(memAcc));
                    if (memAcc.isWrite()) {
                        int prevMemAccIdx = crntThrdEvents.indexOf(memAcc);

                        while (iter.hasNext()) {
                            EventItem crntMemAcc = iter.next();
                            int crntMemAccIdx = crntThrdEvents.indexOf(crntMemAcc);

                            /* ends the block if there is sync/branch-event in between */
                            boolean memAccOnly = true;
                            for (EventItem e : crntThrdEvents.subList(prevMemAccIdx + 1, crntMemAccIdx)) {
                                memAccOnly = memAccOnly && e.isReadOrWrite();
                            }
                            if (!memAccOnly) {
                                iter.previous();
                                break;
                            }

                            equivAccBlk.get(memAcc).add(crntMemAcc);
                            /* YilongL: without logging branch events, we
                             * have to be conservative and end the block
                             * when a read event is encountered */
                            if (crntMemAcc.isRead()) {
                                break;
                            }

                            prevMemAccIdx = crntMemAccIdx;
                        }
                    }
                }
            }

            /* check memory access pairs */
            for (EventItem fst : equivAccBlk.keySet()) {
                for (EventItem snd : equivAccBlk.keySet()) {
                    if (fst.getTID() >= snd.getTID()) {
                        continue;
                    }

                    /* skip if all potential data races are already known */
                    Set<Race> potentialRaces = Sets.newHashSet();
                    for (EventItem e1 : equivAccBlk.get(fst)) {
                        for (EventItem e2 : equivAccBlk.get(snd)) {
                            if ((e1.isWrite() || e2.isWrite())
                                    && !trace.isClinitMemoryAccess(e1)
                                    && !trace.isClinitMemoryAccess(e2)) {
                                potentialRaces.add(new Race(e1, e2, trace, metadata));
                            }
                        }
                    }
                    boolean hasFreshRace = false;
                    for (Race potentialRace : potentialRaces) {
                        hasFreshRace = !violations.contains(potentialRace);
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
                    if (cnstrBuilder.happensBefore(fst, snd)
                            || cnstrBuilder.happensBefore(snd, fst)) {
                        continue;
                    }

                    /* start building constraints for MCM */
                    Formula[] causalConstraints = new Formula[]{
                            cnstrBuilder.getAbstractFeasibilityConstraint(fst),
                            cnstrBuilder.getAbstractFeasibilityConstraint(snd)
                    };

                    if (cnstrBuilder.isRace(fst, snd, causalConstraints)) {
                        for (Race race : potentialRaces) {
                            if (violations.add(race)) {
                                String report = config.simple_report ?
                                        race.toString() : race.generateRaceReport();
                                config.logger.report(report, Logger.MSGTYPE.REAL);
                            }
                        }
                    }
                }
            }
        }
    }

}
