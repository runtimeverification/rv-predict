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

    private final Event[] events;

    public RawTrace(int start, int end, Event[] events) {
        this.tid = events[start].getTID();
        this.start = start;
        this.size = end >= start ? end - start : end - start + events.length;
        this.events = events;
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
        n += start;
        return n >= events.length ? n - events.length : n;
    }

    /**
     * Returns the {@code n}-th event in the trace.
     */
    public Event event(int n) {
        return events[getIndex(n)];
    }

    public int find(Event key) {
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Event midVal = events[getIndex(mid)];
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        throw new IllegalArgumentException("Event not found!");
    }

}
