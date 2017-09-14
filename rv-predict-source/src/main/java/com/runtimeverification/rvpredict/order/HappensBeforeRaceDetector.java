package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.OrderedTraceReader;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HappensBeforeRaceDetector extends OrderedRaceDetector implements RaceDetector {
    final List<Race> races;
    private final List<String> reports;
    private final Configuration config;
    private final OrderedTraceReader traceReader;
    private final VectorClockTraceReader vectorClockReader;

    public HappensBeforeRaceDetector(Configuration config, MetadataInterface metadata) {
        this.config = config;
        races = new ArrayList<>();
        reports = new ArrayList<>();
        traceReader = new OrderedTraceReader();
        vectorClockReader = new VectorClockTraceReader(traceReader, new HappensBefore(metadata), OrderedEvent::new);
    }

    @Override
    public List<String> getRaceReports() {
        return reports;
    }

    @Override
    public void run(Trace trace) {
        traceReader.reset(trace);
        try {
            while (true) {
                ReadonlyOrderedEventInterface event = vectorClockReader.readEvent();
                Collection<ReadonlyOrderedEventInterface> racingEvents = process(event);
                racingEvents.forEach((rEvent) -> {
                    Race race = new Race(event, rEvent, trace, config);
                    races.add(race);
                    String report = race.generateRaceReport();
                    reports.add(report);
                    config.logger().reportRace(report);
                });
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
