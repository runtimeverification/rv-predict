package com.runtimeverification.rvpredict.trace;

import com.google.common.annotations.VisibleForTesting;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Class adding a transparency layer between the prediction engine and the
 * filesystem holding the trace log.
 *
 * @author TraianSF
 * @author YilongL
 */
public abstract class TraceCache {

    private final LockGraph lockGraph;

    protected final Configuration config;

    private final TraceState crntState;

    private long lastGID = 0;
    protected final int capacity;
    private final ArrayList<ReadonlyEventInterface> eventsBuffer;

    protected final List<IEventReader> readers = new ArrayList<>();

    /**
     * Creates a new {@code TraceCache} structure for a trace log.
     */
    TraceCache(Configuration config, MetadataInterface metadata) {
        this(config, new TraceState(config, metadata), new LockGraph(config, metadata), Collections.emptyList());
    }

    @VisibleForTesting
    TraceCache(
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

    public abstract void setup() throws IOException;
    public abstract long getFileSize();

    public LockGraph getLockGraph() {
        return lockGraph;
    }

    /**
     * Returns the power of two that is greater than the given integer.
     */
    private static int getNextPowerOfTwo(int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x));
    }

    private List<RawTrace> readEventWindow() throws IOException {
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
        if (nextGenStart == maxEvents + 1 && !config.withoutGeneration()) {
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

        for (int i = 0; i < eventCount; i++) {
            ReadonlyEventInterface event = events.get(i);
            if (event.getOriginalThreadId() == prevOTID
                    && event.getSignalDepth() == prevSignalDepth
                    && event.getType() != EventType.EXIT_SIGNAL) {
                continue;
            }

            if (event.getType() == EventType.EXIT_SIGNAL) {
                i++;
                if (i < eventCount) {
                    event = events.get(i);
                }
            }

            if (tidStart < i) {
                rawTraces.add(tidSpanToRawTrace(events, tidStart, i, prevSignalDepth, prevOTID));
            }
            prevOTID = event.getOriginalThreadId();
            prevSignalDepth = event.getSignalDepth();
            tidStart = i;
        }

        if (tidStart < eventCount) {
            rawTraces.add(tidSpanToRawTrace(events, tidStart, eventCount, prevSignalDepth, prevOTID));
        }
    }

    private RawTrace tidSpanToRawTrace(List<? extends ReadonlyEventInterface> events,
            int tidStart, int tidEnd, int signalDepth, long otid) {
        boolean threadStartsInTheCurrentWindow;
        List<? extends ReadonlyEventInterface> tidEvents = events.subList(tidStart, tidEnd);
        int n = tidEvents.size(), length = getNextPowerOfTwo(n);
        tidEvents.sort(ReadonlyEventInterface::compareTo);
        int threadId;
        if (signalDepth == 0) {
            OptionalInt maybeThreadId = crntState.getUnfinishedThreadId(signalDepth, otid);
            threadId = maybeThreadId.orElseGet(() -> crntState.getNewThreadId(otid));
            threadStartsInTheCurrentWindow = !maybeThreadId.isPresent();
        } else {
            boolean signalEnds = signalEndsNow(tidEvents);
            if (!signalStartsNow(tidEvents)) {
                threadStartsInTheCurrentWindow = false;
                OptionalInt maybeThreadId = crntState.getUnfinishedThreadId(signalDepth, otid);
                if (!maybeThreadId.isPresent()) {
                    throw new IllegalStateException("No thread id for existing signal.");
                }
                threadId = maybeThreadId.getAsInt();
                if (signalEnds) {
                    crntState.exitSignal(signalDepth, otid);
                }
            } else if (!signalEnds) {
                threadStartsInTheCurrentWindow = true;
                threadId = crntState.enterSignal(signalDepth, otid);
            } else {
                threadStartsInTheCurrentWindow = true;
                threadId = crntState.getNewThreadId();
            }
        }
        return new RawTrace(
                0, n, tidEvents.toArray(new ReadonlyEventInterface[length]),
                signalDepth, threadId, threadStartsInTheCurrentWindow);
    }

    private boolean signalStartsNow(List<? extends ReadonlyEventInterface> events) {
        return events.stream().anyMatch(event -> event.getType() == EventType.ENTER_SIGNAL);
    }

    private boolean signalEndsNow(List<? extends ReadonlyEventInterface> events) {
        return events.stream().anyMatch(event -> event.getType() == EventType.EXIT_SIGNAL);
    }

    public Trace getTraceWindow() throws IOException {
        List<RawTrace> rawTraces = readEventWindow();

        /* finish reading events and create the Trace object */
        return rawTraces.isEmpty() ? null : crntState.initNextTraceWindow(rawTraces);
    }

    public long getTotalRead() throws IOException {
        long bytesRead = 0;
        for (IEventReader reader : readers) {
            bytesRead += reader.bytesRead();
        }
        return bytesRead;
    }
}
