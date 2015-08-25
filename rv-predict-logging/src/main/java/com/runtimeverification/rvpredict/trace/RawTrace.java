package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.ILoggingEngine;

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

    private final Event[] events;

    public RawTrace(int start, int end, Event[] events) {
        this.tid = events[start].getTID();
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
        return events[start].getGID();
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
    public Event event(int n) {
        return events[getIndex(n)];
    }

}
