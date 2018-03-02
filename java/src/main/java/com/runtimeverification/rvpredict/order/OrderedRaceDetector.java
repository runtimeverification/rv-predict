package com.runtimeverification.rvpredict.order;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.OrderedTraceReader;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

/**
 * Race detector based on a sound partial ordering:  If two conflicting accesses are unordered there is a race.
 *
 * @author TraianSF
 */
public class OrderedRaceDetector implements RaceDetector {
    protected final List<String> reports;
    protected final Configuration config;
    private final MetadataInterface metadata;
    private final OrderedTraceReader traceReader;
    private final VectorClockTraceReader vectorClockReader;
    final Map<String, Race> races;
    private Map<Long,ReadonlyOrderedEvent> lastWrites = new HashMap<>();
    private Map<Long,Collection<ReadonlyOrderedEvent>> lastReads = new HashMap<>();

    protected OrderedRaceDetector(
            Configuration config, MetadataInterface metadata, VectorClockOrderInterface happensBefore) {
        this.config = config;
        this.metadata = metadata;
        reports = new ArrayList<>();
        traceReader = new OrderedTraceReader();
        vectorClockReader = new VectorClockTraceReader(traceReader, happensBefore);
        races = new HashMap<>();
    }

    private Collection<ReadonlyOrderedEvent> findEventsUnorderedWith(ReadonlyOrderedEvent orderedEvent) {
        ReadonlyEventInterface event = orderedEvent.getEvent();
        VectorClock clock = orderedEvent.getVectorClock();
        assert event.isReadOrWrite();
        ImmutableList.Builder<ReadonlyOrderedEvent> builder = new ImmutableList.Builder<>();
        long address = event.getDataInternalIdentifier();
        ReadonlyOrderedEvent lastWrite = lastWrites.get(address);
        if (lastWrite != null
                && lastWrite.getVectorClock().compareTo(clock) != VectorClock.Comparison.BEFORE) {
            builder.add(lastWrite);
        }
        Collection<ReadonlyOrderedEvent> lastRead = lastReads.computeIfAbsent(address, k -> new ArrayList<>());
        if (event.isRead()) {
            lastRead.add(orderedEvent);
        } else { // event.isWrite()
            lastWrites.put(address, orderedEvent);
            lastRead.stream()
                    .filter((read) -> read.getVectorClock().compareTo(clock)
                            != VectorClock.Comparison.BEFORE)
                    .forEach(builder::add);
            lastRead.clear();
        }
        return builder.build();
    }

    @Override
    public void close() {}

    @Override
    public List<String> getRaceReports() {
        return reports;
    }

    @Override
    public void run(Trace trace) {
        traceReader.reset(trace);
        try {
            while (true) {
                ReadonlyOrderedEvent orderedEvent = vectorClockReader.readEvent();
                ReadonlyEventInterface event = orderedEvent.getEvent();
                if (!event.isReadOrWrite()) {
                    continue;
                }
                if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                    // Races between volatiles are not considered as per
                    // http://cs.oswego.edu/pipermail/concurrency-interest/2012-January/008927.html
                    continue;
                }

                Collection<ReadonlyOrderedEvent> unorderedWithEvents = findEventsUnorderedWith(orderedEvent);
                unorderedWithEvents.forEach((rEvent) -> {
                    Race race = new Race(event, rEvent.getEvent(), trace, config);
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
