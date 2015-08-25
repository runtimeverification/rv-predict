package com.runtimeverification.rvpredict.engine.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.MaximalCausalModel;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.violation.Race;

/**
 * Detects data races from a given {@link Trace} object.
 *
 * @author YilongL
 */
public class RaceDetector implements Constants {

    private final Configuration config;

    private final Map<String, Race> sigToRealRace = new HashMap<>();

    private final List<String> reports = new ArrayList<>();

    private final Z3Filter z3filter;

    private final com.microsoft.z3.Solver solver;

    public RaceDetector(Configuration config) {
        this.config = config;
        Context z3Context = Configuration.getZ3Context();
        this.z3filter = new Z3Filter(z3Context, config.windowSize);
        try {
            /* setup the solver */
            // mkSimpleSolver < mkSolver < mkSolver("QF_IDL")
            this.solver = z3Context.mkSimpleSolver();
            Params params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 1000);
            solver.setParameters(params);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getRaceReports() {
        return reports;
    }

    private boolean isThreadSafeLocation(Trace trace, int locId) {
        String locationSig = trace.metadata().getLocationSig(locId);
        return locationSig.startsWith("java.util.concurrent")
            || locationSig.startsWith("java.util.stream")
            || locationSig.substring(locationSig.lastIndexOf('.')).startsWith(".class$");
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
                                  && !trace.metadata().isVolatile(e1.getAddr())
                                  && !isThreadSafeLocation(trace, e1.getLocId())
                                  && !trace.isInsideClassInitializer(e1)
                                  && !trace.isInsideClassInitializer(e2)) {
                              Race race = new Race(e1, e2, trace);
                              if (!config.suppressPattern.matcher(race.getRaceLocationSig())
                                      .matches()) {
                                  String raceSig = race.toString();
                                  if (!sigToRealRace.containsKey(raceSig)) {
                                      sigToRaceCandidates.computeIfAbsent(raceSig,
                                              x -> new ArrayList<>()).add(race);
                                  }
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

        Map<String, Race> result = MaximalCausalModel.create(trace, z3filter, solver)
                .checkRaceSuspects(sigToRaceSuspects);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = race.generateRaceReport();
            reports.add(report);
            config.logger().reportRace(report);
        });
    }
}
