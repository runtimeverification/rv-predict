package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class adding a transparency layer between the prediction engine and the
 * filesystem holding the trace log.
 *
 * @author TraianSF
 * @author YilongL
 */
public class TraceCache {

    private final Configuration config;

    private final TraceState crntState;

    private final List<EventReader> readers = new ArrayList<>();

    private final Event[] events;

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
        this.crntState = new TraceState(metadata);
        this.events = new Event[config.windowSize];
    }

    public void setup() throws IOException {
        int logFileId = 0;
        while (true) {
            Path path = config.getTraceFilePath(logFileId++);
            if (!path.toFile().exists()) {
                break;
            }
            readers.add(new EventReader(path));
        }
    }

    /**
     * Load trace segment starting from event {@code fromIndex}.
     *
     * @param fromIndex
     *            low endpoint (inclusive) of the trace segment
     * @return a {@link Trace} representing the trace segment read
     */
    public Trace getTrace(long fromIndex) throws IOException {
        Arrays.fill(events, null);
        long toIndex = fromIndex + events.length;

        /* sort readers by their last read events */
        readers.sort((r1, r2) -> Long.compare(r1.lastReadEvent().GID, r2.lastReadEvent().GID));
        Iterator<EventReader> iter = readers.iterator();
        Event event;
        while (iter.hasNext()) {
            EventReader reader = iter.next();
            if ((event = reader.lastReadEvent()).GID >= toIndex) {
                break;
            }

            assert event.GID >= fromIndex;
            do {
                events[(int) (event.GID % events.length)] = event;
                try {
                    event = reader.readEvent();
                } catch (EOFException e) {
                    iter.remove();
                    break;
                }
            } while (event.GID < toIndex);
        }

        /* finish reading events and create the Trace object */
        Trace trace = new Trace(crntState, config.windowSize);
        crntState.setCurrentTraceWindow(trace);
        for (int i = 0; i < events.length; i++) {
            if (events[i] == null) {
                break;
            }
            trace.addRawEvent(events[i]);
        }
        trace.finishedLoading();
        return trace;
    }

}
