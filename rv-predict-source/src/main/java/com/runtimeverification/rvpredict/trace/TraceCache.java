package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.Event;
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

    protected final List<IEventReader> readers = new ArrayList<>();

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     */
    public TraceCache(Configuration config, Metadata metadata) {
        this.config = config;
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
            } while (event.getGID() < toIndex);
            int length = getNextPowerOfTwo(events.size());
            rawTraces.add(new RawTrace(0, events.size(), events.toArray(new Event[length])));
        }
        return rawTraces;
    }

    protected final List<RawTrace> readEventWindow() throws IOException {
        List<RawTrace> rawTraces =  new ArrayList<>();
	int maxEvents = config.windowSize;
        int capacity = getNextPowerOfTwo(maxEvents - 1);
        if (config.stacks()) {
            capacity <<= 1;
        }
        List<Event> events = new ArrayList<>(capacity);
	for (int i = 0; i < maxEvents; i++) {
		Event event;
		long leastGID = Long.MAX_VALUE;
		IEventReader leastReader = null;
		Iterator<IEventReader> iter = readers.iterator();
		while (iter.hasNext()) {
			IEventReader reader = iter.next();
			event = reader.lastReadEvent();
			if (event != null && event.getGID() < leastGID) {
//				System.err.println("choosing new reader because gid " + event.getGID() + " < " + leastGID);
				leastReader = reader;
				leastGID = event.getGID();
			}
		}
		if (leastReader == null)
			break;
		event = leastReader.lastReadEvent();
		assert event != null;
		events.add(event);
//		System.err.println("adding event " + event.getGID());
                try {
			leastReader.readEvent();
                } catch (EOFException e) {
			readers.remove(leastReader);
                }
	}
	System.err.println("got " + events.size() + " events out of " + maxEvents);
	/* Make GIDs compact. */
	final int n = events.size();
	if (n > 0 && events.get(n - 1).getGID() - events.get(0).getGID() >= config.windowSize) {
		System.err.println("Compacting GIDs");
		long gid = 0;
		for (int i = 0; i < n; i++)
			events.get(i).setGID(gid++);
	}
	if (n > 0) {
		int i, tidStart = 0;
		events.sort((l, r) -> {
			long lt = l.getTID(), rt = r.getTID();
			return (lt < rt) ? -1 : ((lt > rt) ? 1 : 0);
		});
		long lastTID = events.get(0).getTID();

		for (i = 1; i < n; i++) {
/*
			assert evarray[i - 1].getGID() < evarray[i].getGID();
*/
			if (events.get(i).getTID() == lastTID)
				continue;

			rawTraces.add(tidSpanToRawTrace(events, tidStart, i));
			lastTID = events.get(i).getTID();
			tidStart = i;
		}

		rawTraces.add(tidSpanToRawTrace(events, tidStart, n));
	}
        return rawTraces;
    }
    private static RawTrace tidSpanToRawTrace(List<Event> events,
	    int tidStart, int tidEnd) {
	List<Event> tidEvents = events.subList(tidStart, tidEnd);
	int n = tidEvents.size(), length = getNextPowerOfTwo(n);
	tidEvents.sort((l, r) -> l.compareTo(r));
	return new RawTrace(0, n, tidEvents.toArray(new Event[length]));
    }
    public Trace getTraceWindow() throws IOException {
        List<RawTrace> rawTraces = readEventWindow();

        /* finish reading events and create the Trace object */
        return rawTraces.isEmpty() ? null : crntState.initNextTraceWindow(rawTraces);
    }
}
