package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
    protected final ArrayList<Event> eventsBuffer;

    protected final List<IEventReader> readers = new ArrayList<>();

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
        // Used to be config.windowSize - 1, but I try to read 1 more
	// than the window size, now.
	capacity = getNextPowerOfTwo(config.windowSize) *
	    (config.stacks() ? 2 : 1);
	eventsBuffer = new ArrayList<>(capacity);
        this.crntState = new TraceState(config, metadata);
        lockGraph = new LockGraph(config, metadata);
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

    public LockGraph getLockGraph() {
        return lockGraph;
    }

    /**
     * Returns the power of two that is greater than the given integer.
     */
    protected static final int getNextPowerOfTwo(int x) {
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
        ReadonlyEventInterface event;
        while (iter.hasNext()) {
            IEventReader reader = iter.next();
            if ((event = reader.lastReadEvent()).getEventId() >= toIndex) {
                break;
            }

            assert event.getEventId() >= fromIndex;
            List<ReadonlyEventInterface> events = new ArrayList<>(capacity);
            do {
                events.add(event);
                //TODO(TraianSF): the following conditional does not belong here. Consider moving it.
                if (event.isPreLock() || event.isLock() || event.isUnlock()) {
                    if (config.isLLVMPrediction()) {
                        //TODO(TraianSF): remove above condition once instrumentation works for Java
                        lockGraph.handle(event);
                    }
                }
                try {
                    event = reader.readEvent();
                } catch (EOFException e) {
                    iter.remove();
                    break;
                }
            } while (event.getEventId() < toIndex);
            int length = getNextPowerOfTwo(events.size());
            rawTraces.add(new RawTrace(0, events.size(), events.toArray(new ReadonlyEventInterface[length])));
        }
        return rawTraces;
    }

    protected final List<RawTrace> readEventWindow() throws IOException {
        List<RawTrace> rawTraces =  new ArrayList<>();
	final int maxEvents = config.windowSize;
	if (Configuration.debug)
	    System.err.println(readers.size() + " readers");
        ArrayList<Event> events = (ArrayList<Event>)eventsBuffer.clone();
	eventsBuffer.clear();
	events.ensureCapacity(capacity);
	for (int i = events.size(); i < maxEvents + 1; i++) {
		Event event;
		long leastGID = Long.MAX_VALUE;
		IEventReader leastReader = null;
		Iterator<IEventReader> iter = readers.iterator();
		while (iter.hasNext()) {
			IEventReader reader = iter.next();
			event = reader.lastReadEvent();
			if (event != null && event.getEventId() < leastGID) {
//				System.err.println("choosing new reader because gid " + event.getEventId() + " < " + leastGID);
				leastReader = reader;
				leastGID = event.getEventId();
			}
		}
		if (leastReader == null)
			break;
		event = leastReader.lastReadEvent();
		assert event != null;
		events.add(event);
//		System.err.println("adding event " + event.getEventId());
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
		return null;		// XXX
	}
	if (Configuration.debug) {
		System.err.println("buffering " + (n - nextGenStart) +
		    " events after window boundary");
	}
	eventsBuffer.addAll(events.subList(nextGenStart, n));
	events.subList(nextGenStart, n).clear();
	/* Make GIDs compact. */
	for (int i = 0; i < nextGenStart; i++)
		events.get(i).setEventId(lastGID + i);
	lastGID += maxEvents;
	int tidStart = 0;
	events.sort((l, r) -> {
		long lt = l.getThreadId(), rt = r.getThreadId();
		return (lt < rt) ? -1 : ((lt > rt) ? 1 : 0);
	});
	long prevTID = events.get(0).getThreadId();

	for (int i = 1; i < nextGenStart; i++) {
		if (events.get(i).getThreadId() == prevTID)
			continue;

		rawTraces.add(tidSpanToRawTrace(events, tidStart, i));
		prevTID = events.get(i).getThreadId();
		tidStart = i;
	}

	rawTraces.add(tidSpanToRawTrace(events, tidStart, nextGenStart));
        return rawTraces;
    }
    private static RawTrace tidSpanToRawTrace(List<? extends ReadonlyEventInterface> events,
	    int tidStart, int tidEnd) {
	List<? extends ReadonlyEventInterface> tidEvents = events.subList(tidStart, tidEnd);
	int n = tidEvents.size(), length = getNextPowerOfTwo(n);
	tidEvents.sort(ReadonlyEventInterface::compareTo);
	return new RawTrace(0, n, tidEvents.toArray(new ReadonlyEventInterface[length]));
    }
    public Trace getTraceWindow() throws IOException {
        List<RawTrace> rawTraces = readEventWindow();

        /* finish reading events and create the Trace object */
        return rawTraces.isEmpty() ? null : crntState.initNextTraceWindow(rawTraces);
    }
}
