package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.EventItem;
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

    private final EventItem[] items;

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
        this.crntState = new TraceState(metadata);
        this.items = new EventItem[config.windowSize];
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
        Arrays.fill(items, null);
        long toIndex = fromIndex + items.length;

        /* sort readers by their last read events */
        readers.sort((r1, r2) -> Long.compare(r1.lastReadEvent().GID, r2.lastReadEvent().GID));
        Iterator<EventReader> iter = readers.iterator();
        EventItem item;
        while (iter.hasNext()) {
            EventReader reader = iter.next();
            if ((item = reader.lastReadEvent()).GID >= toIndex) {
                break;
            }

            assert item.GID >= fromIndex;
            do {
                items[(int) (item.GID % items.length)] = item;
                try {
                    item = reader.readEvent();
                } catch (EOFException e) {
                    iter.remove();
                    break;
                }
            } while (item.GID < toIndex);
        }

        /* finish reading events and create the Trace object */
        Trace trace = new Trace(crntState, config.windowSize);
        crntState.setCurrentTraceWindow(trace);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                break;
            }
            trace.addRawEvent(EventUtils.of(items[i]));
        }
        trace.finishedLoading();
        return trace;
    }

}
