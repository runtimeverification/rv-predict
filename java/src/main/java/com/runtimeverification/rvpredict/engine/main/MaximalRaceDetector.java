package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.performance.AnalysisLimit;
import com.runtimeverification.rvpredict.smt.MaximalCausalModel;
import com.runtimeverification.rvpredict.smt.RaceSolver;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects data races from a given {@link Trace} object.
 *
 * @author YilongL
 */
public class MaximalRaceDetector implements RaceDetector {

    private final Configuration config;

    private final Map<String, Race> sigToRealRace = new HashMap<>();

    private final List<String> reports = new ArrayList<>();

    private final RaceSolver raceSolver;

    /**
     * Takes ownership of the race solver.
     */
    public MaximalRaceDetector(Configuration config, RaceSolver raceSolver) {
        this.config = config;
        this.raceSolver = raceSolver;
    }

    @Override
    public List<String> getRaceReports() {
        return reports;
    }

    @Override
    public void close() throws Exception {
        raceSolver.close();
    }

    private boolean isThreadSafeLocation(Trace trace, long locId) {
        String locationSig = trace.metadata().getLocationSig(locId, Optional.of(trace.getSharedLibraries()));
        if (locationSig.startsWith("java.util.concurrent")
            || locationSig.startsWith("java.util.stream")) {
            return true;
        } else {
            int index = locationSig.lastIndexOf('.');
            if (index != -1) {
                return locationSig.substring(index).startsWith(".class$");
            }
        }
        return false;
    }

    private Map<String, List<Race>> computeUnknownRaceSuspects(Trace trace) {
        Map<String, List<Race>> sigToRaceCandidates = new HashMap<>();
        trace.eventsByThreadID().forEach((ttid1, events1) ->
                trace.eventsByThreadID().forEach((ttid2, events2) -> {
                    if (ttid1 >= ttid2 || !trace.threadsCanOverlap(ttid1, ttid2)) {
                        return;
                    }
                    events1.forEach(e1 -> events2.forEach(e2 -> {
                        if ((e1.isWrite() && e2.isReadOrWrite() || e1.isReadOrWrite() && e2.isWrite())
                                && e1.getDataInternalIdentifier() == e2.getDataInternalIdentifier()
                                // TODO(virgil): Metadata should work with external identifiers.
                                // This code works fine for Java events and we don't handle volatile data
                                // for compact events, so this is fine for now.
                                && !trace.metadata().isVolatile(e1.getDataInternalIdentifier())
                                && !isThreadSafeLocation(trace, e1.getLocationId())
                                && !trace.isInsideClassInitializer(e1)
                                && !trace.isInsideClassInitializer(e2)) {
                            Race race = new Race(e1, e2, trace, config);
                            if (!config.suppressPattern.matcher(race.getRaceDataSig())
                                    .matches()) {
                                String raceSig = race.toString();
                                if (!sigToRealRace.containsKey(raceSig)) {
                                    sigToRaceCandidates.computeIfAbsent(raceSig,
                                            x -> new ArrayList<>()).add(race);
                                }
                            }
                        }
                    }));
                }));
        return sigToRaceCandidates;
    }

    @Override
    public void run(Trace trace, AnalysisLimit analysisLimit) {
        if (!trace.mayContainRaces()) {
            return;
        }

        Map<String, List<Race>> sigToRaceSuspects = computeUnknownRaceSuspects(trace);
        if (sigToRaceSuspects.isEmpty()) {
            return;
        }

        Map<String, Race> result =
                MaximalCausalModel.create(trace, raceSolver, config.detectInterruptedThreadRace())
                        .checkRaceSuspects(sigToRaceSuspects, analysisLimit);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = race.generateRaceReport();
            reports.add(report);
            config.logger().reportRace(report);
        });
    }
}
