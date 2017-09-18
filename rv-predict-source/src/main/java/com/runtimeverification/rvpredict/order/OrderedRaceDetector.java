package com.runtimeverification.rvpredict.order;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.OrderedTraceReader;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OrderedRaceDetector implements RaceDetector {
    protected final List<String> reports;
    protected final Configuration config;
    private final MetadataInterface metadata;
    protected final OrderedTraceReader traceReader;
    protected final VectorClockTraceReader vectorClockReader;
    final Map<String, Race> races;
    Map<Long,ReadonlyOrderedEventInterface> lastWrites = new HashMap<>();
    Map<Long,Collection<ReadonlyOrderedEventInterface>> lastReads = new HashMap<>();

    protected OrderedRaceDetector(Configuration config, MetadataInterface metadata, VectorClockOrderInterface happensBefore) {
        this.config = config;
        this.metadata = metadata;
        reports = new ArrayList<>();
        traceReader = new OrderedTraceReader();
        vectorClockReader = new VectorClockTraceReader(traceReader, happensBefore, OrderedEvent::new);
        races = new HashMap<>();
    }

    private Collection<ReadonlyOrderedEventInterface> findEventsUnorderedWith(ReadonlyOrderedEventInterface event) {
        assert event.isReadOrWrite();
        ImmutableList.Builder<ReadonlyOrderedEventInterface> builder = new ImmutableList.Builder<>();
        long address = event.getDataInternalIdentifier();
        ReadonlyOrderedEventInterface lastWrite = lastWrites.get(address);
        if (lastWrite != null
                && lastWrite.getVectorClock().compareTo(event.getVectorClock()) != VectorClock.Comparison.BEFORE) {
            builder.add(lastWrite);
        }
        Collection<ReadonlyOrderedEventInterface> lastRead = lastReads.computeIfAbsent(address, k -> new ArrayList<>());
        if (event.isRead()) {
            lastRead.add(event);
        } else { // event.isWrite()
            lastWrites.put(address, event);
            builder.addAll(lastRead.stream()
                    .filter((read) -> read.getVectorClock().compareTo(event.getVectorClock())
                            != VectorClock.Comparison.BEFORE)
                    .collect(Collectors.toList()));
            lastRead.clear();
        }
        return builder.build();
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
                if (!event.isReadOrWrite()) {
                    continue;
                }
                if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                    // Races between volatiles are not considered as per
                    // http://cs.oswego.edu/pipermail/concurrency-interest/2012-January/008927.html
                    continue;
                }

                Collection<ReadonlyOrderedEventInterface> unorderedWithEvents = findEventsUnorderedWith(event);
                unorderedWithEvents.forEach((rEvent) -> {
                    Race race = new Race(event, rEvent, trace, config);
                    String raceSig = race.toString();
                    if (!races.containsKey(raceSig)) {
                        races.put(raceSig, race);
                        String report = race.generateRaceReport();
                        reports.add(report);
                        config.logger().reportRace(report);
                    }
                });
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
