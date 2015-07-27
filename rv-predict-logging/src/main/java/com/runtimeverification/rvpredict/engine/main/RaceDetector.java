package com.runtimeverification.rvpredict.engine.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.MaximalCausalModel;
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

    public RaceDetector(Configuration config) {
        this.config = config;
    }

    public List<String> getRaceReports() {
        return reports;
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
                                  && !trace.metadata().getLocationSig(e1.getLocId())
                                      .startsWith("java.util.concurrent")
                                  && !trace.isInsideClassInitializer(e1)
                                  && !trace.isInsideClassInitializer(e2)) {
                              Race race = new Race(e1, e2, trace);
                              if (!config.suppressList.contains(race.getRaceLocationSig())) {
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

        Map<String, Race> result = MaximalCausalModel.create(trace)
                .checkRaceSuspects(sigToRaceSuspects, config.solver_timeout);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = race.generateRaceReport();
            reports.add(report);
            config.logger().reportRace(report);
        });
    }
}
