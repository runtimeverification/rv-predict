package com.runtimeverification.rvpredict.trace;

import com.google.common.annotations.VisibleForTesting;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

    private final LockGraph lockGraph;

    protected final Configuration config;

    private final TraceState crntState;

    protected long lastGID = 0;
    protected final int capacity;
    private final ArrayList<ReadonlyEventInterface> eventsBuffer;

    protected final List<IEventReader> readers = new ArrayList<>();

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this(config, new TraceState(config, metadata), new LockGraph(config, metadata), Collections.emptyList());
    }

    private TraceCache(
            Configuration config, TraceState traceState, LockGraph lockGraph, List<IEventReader> defaultReaders) {
        this.config = config;
        this.crntState = traceState;
        this.lockGraph = lockGraph;
        this.readers.addAll(defaultReaders);

        // Used to be config.windowSize - 1, but I try to read 1 more
        // than the window size, now.
        capacity = getNextPowerOfTwo(config.windowSize) *
                (config.stacks() ? 2 : 1);
        eventsBuffer = new ArrayList<>(capacity);
    }

    @VisibleForTesting
    static TraceCache createForTesting(
            Configuration config, TraceState traceState, LockGraph lockGraph, List<IEventReader> readers) {
        return new TraceCache(config, traceState, lockGraph, readers);
    }

    public void setup() throws IOException {
        int logFileId = 0;
        if (config.isCompactTrace()) {
            try {
                readers.add(new CompactEventReader(config.getCompactTraceFilePath()));
            } catch (InvalidTraceDataException e) {
                throw new IOException(e);
            }
            return;
        }
        while (true) {
            Path path = config.getTraceFilePath(logFileId++);
            if (!path.toFile().exists()) {
                break;
            }
            readers.add(new EventReader(path));
        }
    }

    public LockGraph getLockGraph() {
        return lockGraph;
    }

    /**
     * Returns the power of two that is greater than the given integer.
     */
    protected static final int getNextPowerOfTwo(int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x));
    }

    protected final List<RawTrace> readEventWindow() throws IOException {
        List<RawTrace> rawTraces =  new ArrayList<>();
	final int maxEvents = config.windowSize;
	if (Configuration.debug)
	    System.err.println(readers.size() + " readers");
        ArrayList<ReadonlyEventInterface> events = new ArrayList<>(eventsBuffer);
	eventsBuffer.clear();
	events.ensureCapacity(capacity);
	for (int i = events.size(); i < maxEvents + 1; i++) {
            ReadonlyEventInterface event;
            long leastGID = Long.MAX_VALUE;
            IEventReader leastReader = null;
            Iterator<IEventReader> iter = readers.iterator();
            while (iter.hasNext()) {
                IEventReader reader = iter.next();
                event = reader.lastReadEvent();
                if (event != null && event.getEventId() < leastGID) {
//                      System.err.println("choosing new reader because gid " + event.getEventId() + " < " + leastGID);
                        leastReader = reader;
                        leastGID = event.getEventId();
                }
            }
            if (leastReader == null)
                break;
            event = leastReader.lastReadEvent();
            assert event != null;
            events.add(event);
//          System.err.println("adding event " + event.getEventId());
            try {
		leastReader.readEvent();
            } catch (EOFException e) {
		readers.remove(leastReader);
            }
        }
        if (Configuration.debug)
            System.err.println("got " + events.size() + " events out of " + maxEvents);
        final int n = events.size();
        if (n <= 0)
                return rawTraces;
        int nextGenStart = maxEvents + 1;
        final long genMask = (long)0xffff << 48;
        if (n < nextGenStart)
                nextGenStart = n;
        else for (int i = n - 1; i > 0; i--) {
                if ((events.get(i - 1).getEventId() & genMask) !=
                    (events.get(i).getEventId() & genMask)) {
                        nextGenStart = i;
                        break;
                }
        }
        if (nextGenStart == maxEvents + 1) {
                System.err.println("no change of generation in " +
                    (maxEvents + 1) + " events");
                return rawTraces;                // XXX
        }
        if (Configuration.debug) {
                System.err.println("buffering " + (n - nextGenStart) +
                    " events after window boundary");
        }
        eventsBuffer.addAll(events.subList(nextGenStart, n));
        events.subList(nextGenStart, n).clear();
        /* Make GIDs compact. */
        for (int i = 0; i < nextGenStart; i++)
                events.set(i, events.get(i).destructiveWithEventId(lastGID + i));
        lastGID += maxEvents;
        splitTracesIntoThreads(rawTraces, events, nextGenStart);
        return rawTraces;
    }

    private void splitTracesIntoThreads(
            List<RawTrace> rawTraces, ArrayList<ReadonlyEventInterface> events, int eventCount) {
        int tidStart = 0;
        events.sort((l, r) -> {
            long lt = l.getOriginalThreadId(), rt = r.getOriginalThreadId();
            if (lt < rt)
                return -1;
            if (lt > rt)
                return 1;
            int lsd = l.getSignalDepth(), rsd = r.getSignalDepth();
            if (lsd < rsd)
                return -1;
            if (lsd > rsd)
                return 1;
            long lid = l.getEventId(), rid = r.getEventId();
            if (lid < rid)
                return -1;
            if (lid > rid)
                return 1;
            return 0;
        });
        long prevOTID = events.get(0).getOriginalThreadId();
        int prevSignalDepth = events.get(0).getSignalDepth();
        int currentTraceThreadId = 1;

        for (int i = 1; i < eventCount; i++) {
            ReadonlyEventInterface event = events.get(i);
            if (event.getOriginalThreadId() == prevOTID
                    && event.getSignalDepth() == prevSignalDepth
                    && (event.getType() != EventType.ENTER_SIGNAL || tidStart == i-1)) {
                continue;
            }

            rawTraces.add(tidSpanToRawTrace(events, tidStart, i, prevSignalDepth, currentTraceThreadId));
            currentTraceThreadId++;
            prevOTID = event.getOriginalThreadId();
            prevSignalDepth = event.getSignalDepth();
            tidStart = i;
        }

        rawTraces.add(tidSpanToRawTrace(events, tidStart, eventCount, prevSignalDepth, currentTraceThreadId));
    }

    private static RawTrace tidSpanToRawTrace(List<? extends ReadonlyEventInterface> events,
	    int tidStart, int tidEnd, int signalDepth, int threadId) {
	List<? extends ReadonlyEventInterface> tidEvents = events.subList(tidStart, tidEnd);
	int n = tidEvents.size(), length = getNextPowerOfTwo(n);
	tidEvents.sort(ReadonlyEventInterface::compareTo);
	return new RawTrace(0, n, tidEvents.toArray(new ReadonlyEventInterface[length]), signalDepth, threadId);
    }
    public Trace getTraceWindow() throws IOException {
        List<RawTrace> rawTraces = readEventWindow();

        /* finish reading events and create the Trace object */
        return rawTraces.isEmpty() ? null : crntState.initNextTraceWindow(rawTraces);
    }
}
