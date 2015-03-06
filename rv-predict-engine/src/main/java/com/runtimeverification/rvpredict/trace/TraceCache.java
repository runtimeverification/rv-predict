package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventInputStream;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.LoggingFactory;

import java.io.EOFException;
import java.io.IOException;
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

    private final Map<Long, Pair<EventInputStream, EventItem>> indexes;

    private long nextIdx = 1;

    private final LoggingFactory loggingFactory;

    private final TraceState crntState;

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     * @param config
     *
     * @param loggingFactory suppling additional information about the nature of the logs.
     */
    public TraceCache(Configuration config, LoggingFactory loggingFactory) {
        this.loggingFactory = loggingFactory;
        this.indexes = new HashMap<>();
        this.crntState = new TraceState(config, loggingFactory);
    }

    /**
     * Load trace segment from event {@code fromIndex} to event
     * {@code toIndex-1}. Event number is assumed to start from 1.
     *
     * @see TraceCache#getNextEvent()
     * @param fromIndex
     *            low endpoint (inclusive) of the trace segment
     * @param toIndex
     *            high endpoint (exclusive) of the trace segment
     * @return a {@link Trace} representing the trace segment
     *         read
     */
    public Trace getTrace(long fromIndex, long toIndex) throws IOException,
            InterruptedException {
        Trace trace = new Trace(crntState);
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
    private EventItem getNextEvent() throws IOException, InterruptedException {
        if (!indexes.containsKey(nextIdx)) {
            try {
                updateIndexes(nextIdx);
            } catch (EOFException e) {
                // EOF is expected
                return null;
            }
        }
        Pair<EventInputStream, EventItem> entry = indexes.remove(nextIdx);
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

    private void updateIndexes(long index) throws IOException, InterruptedException {
        EventItem event;
        do {
            EventInputStream inputStream = loggingFactory.getInputStream();
            if (inputStream == null) return;
            event = inputStream.readEvent();
            indexes.put(event.GID, MutablePair.of(inputStream, event));
        } while (event.GID != index);
    }

}
