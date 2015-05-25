package com.runtimeverification.rvpredict.engine.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.MaximalCausalModel;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.violation.Race;

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
public class RaceDetector {

    private final Configuration config;

    private final Map<String, Race> sigToRealRace = new HashMap<>();

    public RaceDetector(Configuration config) {
        this.config = config;
    }

    /**
     * Returns a set of all detected races.
     */
    public Set<Race> getRaces() {
        return new HashSet<>(sigToRealRace.values());
    }

    private Map<String, List<Race>> computeUnknownRaceSuspects(Trace trace) {
        Map<String, List<Race>> sigToRaceCandidates = new HashMap<>();
        trace.eventsByThreadID().forEach((tid1, events1) -> {
           trace.eventsByThreadID().forEach((tid2, events2) -> {
               if (tid1 < tid2) {
                   events1.forEach(e1 -> {
                      events2.forEach(e2 -> {
                          if ((e1.isWrite() && e2.isReadOrWrite() ||
                                  e1.isReadOrWrite() && e2.isWrite())
                                  && e1.getAddr() == e2.getAddr()
                                  && (config.checkVolatile || !trace.metadata().isVolatile(e1.getAddr()))
                                  && !trace.isInsideClassInitializer(e1)
                                  && !trace.isInsideClassInitializer(e2)) {
                              Race race = new Race(e1, e2, trace);
                              String raceSig = race.toString();
                              if (!sigToRealRace.containsKey(raceSig)) {
                                  sigToRaceCandidates.computeIfAbsent(raceSig,
                                          x -> new ArrayList<>()).add(race);
                              }
                          }
                      });
                   });
               }
           });
        });
        return sigToRaceCandidates;
    }

    public void run(Trace trace) {
        if (!trace.mayContainRaces()) {
            return;
        }

        Map<String, List<Race>> sigToRaceSuspects = computeUnknownRaceSuspects(trace);
        if (sigToRaceSuspects.isEmpty()) {
            return;
        }

        Map<String, Race> result = MaximalCausalModel.create(trace)
                .checkRaceSuspects(sigToRaceSuspects, config.solver_timeout);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = config.simple_report ? race.generateSimpleRaceReport() : race
                    .generateDetailedRaceReport();
            config.logger.report(report, Logger.MSGTYPE.REAL);
        });
    }
}
