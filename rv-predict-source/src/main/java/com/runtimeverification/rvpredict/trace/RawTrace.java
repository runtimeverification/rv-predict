package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.log.ReadonlyEvent;

/**
 * Unprocessed trace of events, implemented as a thin wrapper around the array
 * of events obtained from an {@link ILoggingEngine}.
 *
 * @author YilongL
 */
public class RawTrace {

    private final long tid;

    private final int start;

    private final int size;

    private final int mask;

    private final ReadonlyEvent[] events;

    public RawTrace(int start, int end, ReadonlyEvent[] events) {
        this.tid = events[start].getThreadId();
        this.start = start;
        this.mask = events.length - 1;
        this.size = (end - start + events.length) & mask;
        this.events = events;
        if ((events.length & mask) != 0) {
            throw new IllegalArgumentException("The length of events must be a power of two!");
        }
    }

    public long getTID() {
        return tid;
    }

    public long getMinGID() {
        return events[start].getEventId();
    }

    public int size() {
        return size;
    }

    /**
     * Returns the index of the {@code n}-th event.
     */
    private int getIndex(int n) {
        return (n + start) & mask;
    }

    /**
     * Returns the {@code n}-th event in the trace.
     */
    public ReadonlyEvent event(int n) {
        return events[getIndex(n)];
    }

}
