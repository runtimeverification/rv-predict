package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Class adding a transparency layer between the prediction engine and the
 * filesystem holding the trace log.
 * A trace log consists from a collection of streams, each holding all the events
 * corresponding to a single thread.
 * @author TraianSF
 */
public class TraceCache {

    private final Map<Long, Pair<EventReader, EventItem>> indexes;

    private long nextIdx = 0;

    private int nextLogFileId = 1;

    private final Configuration config;

    private final TraceState crntState;

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     *
     * @param loggingFactory suppling additional information about the nature of the logs.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
        this.indexes = new HashMap<>();
        this.crntState = new TraceState(metadata);
    }

    /**
     * Load trace segment from event {@code fromIndex} to event
     * {@code toIndex-1}. Event number is assumed to start from 0.
     *
     * @see TraceCache#getNextEvent()
     * @param fromIndex
     *            low endpoint (inclusive) of the trace segment
     * @param toIndex
     *            high endpoint (exclusive) of the trace segment
     * @return a {@link Trace} representing the trace segment
     *         read
     */
    public Trace getTrace(long fromIndex, long toIndex) throws IOException {
        Trace trace = new Trace(crntState, (int) (toIndex - fromIndex));
        crntState.setCurrentTraceWindow(trace);
        assert nextIdx == fromIndex;
        for (nextIdx = fromIndex; nextIdx < toIndex; nextIdx++) {
            EventItem eventItem = getNextEvent();
            if (eventItem == null) {
                break;
            }
            trace.addRawEvent(EventUtils.of(eventItem));
        }
        trace.finishedLoading();
        return trace;
    }

    /**
     * Returns the next event in the trace, whose unique identifier in the
     * logged trace is given by {@link #nextIdx}.
     * <p>
     * This method assumes the trace is read in sequential order, hence one of
     * the keys in the {@link #indexes} table is equal to {@code nextIdx}.
     * Moreover, it is assumed that {@code nextIdx < traceSize}.
     *
     * @return the next event in the trace
     */
    private EventItem getNextEvent() throws IOException {
        if (!indexes.containsKey(nextIdx)) {
            try {
                updateIndexes(nextIdx);
            } catch (EOFException e) {
                // EOF is expected
                return null;
            }
        }
        Pair<EventReader, EventItem> entry = indexes.remove(nextIdx);
        if (entry == null) {
            return null;
        }

        EventItem nextEvent = entry.getValue();
        try {
            EventItem event = entry.getKey().readEvent();
            entry.setValue(event);
            indexes.put(event.GID, entry);
        } catch (EOFException e) {
            // EOF is expected.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nextEvent;
    }

    private void updateIndexes(long index) throws IOException {
        EventItem event;
        do {
            Path path = config.getTraceFilePath(nextLogFileId++);
            if (!path.toFile().exists()) {
                return;
            }
            EventReader reader = new EventReader(path);
            event = reader.readEvent();
            indexes.put(event.GID, MutablePair.of(reader, event));
        } while (event.GID != index);
    }

}
