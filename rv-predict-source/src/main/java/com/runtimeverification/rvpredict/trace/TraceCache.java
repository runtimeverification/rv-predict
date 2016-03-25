package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Class adding a transparency layer between the prediction engine and the
 * filesystem holding the trace log.
 *
 * @author TraianSF
 * @author YilongL
 */
public class TraceCache {

    protected final Configuration config;

    private final TraceState crntState;

    protected final List<IEventReader> readers = new ArrayList<>();

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
        this.crntState = new TraceState(config, metadata);
    }

    public TraceCache(Configuration config, Metadata metadata, ForkPoint forkPoint) {
        this.config = config;
        if(forkPoint != null)
            this.crntState = forkPoint.getTraceState();
        else
            this.crntState = new TraceState(config, metadata);
    }

    public TraceState getCrntState() {
        return crntState;
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

    public Map<Long, ForkPoint> getForks() {
        return this.crntState.getPidToForkPoint();
    }

    /**
     * Returns the power of two that is greater than the given integer.
     */
    protected final int getNextPowerOfTwo(int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x));
    }

    /**
     * Load trace segment starting from event {@code fromIndex}.
     *
     * @param fromIndex
     *            low endpoint (inclusive) of the trace segment
     * @return a {@link Trace} representing the trace segment read or
     *         {@code null} if the end of file is reached
     */
    public Trace getTrace(long fromIndex) throws IOException {
        long toIndex = fromIndex + config.windowSize;
        List<RawTrace> rawTraces = readEvents(fromIndex, toIndex);

        /* finish reading events and create the Trace object */
        return rawTraces.isEmpty() ? null : crntState.initNextTraceWindow(rawTraces);
    }

    protected final List<RawTrace> readEvents(long fromIndex, long toIndex) throws IOException {
        List<RawTrace> rawTraces =  new ArrayList<>();
        /* sort readers by their last read events */
        readers.sort((r1, r2) -> r1.lastReadEvent().compareTo(r2.lastReadEvent()));
        Iterator<IEventReader> iter = readers.iterator();
        Event event;
        while (iter.hasNext()) {
            IEventReader reader = iter.next();
            if ((event = reader.lastReadEvent()).getGID() >= toIndex) {
                break;
            }

            assert event.getGID() >= fromIndex;
            int capacity = getNextPowerOfTwo(config.windowSize - 1);
            if (config.stacks()) {
                capacity <<= 1;
            }
            List<Event> events = new ArrayList<>(capacity);
            do {
                events.add(event);
                try {
                    event = reader.readEvent();
                } catch (EOFException e) {
                    iter.remove();
                    break;
                }
            } while (event.getGID() < toIndex);
            int length = getNextPowerOfTwo(events.size());
            rawTraces.add(new RawTrace(0, events.size(), events.toArray(new Event[length])));
        }
        return rawTraces;
    }

}
