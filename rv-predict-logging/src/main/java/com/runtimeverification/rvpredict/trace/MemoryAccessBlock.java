package com.runtimeverification.rvpredict.trace;

import java.util.Iterator;
import java.util.List;

import com.runtimeverification.rvpredict.log.Event;

/**
 * Consecutive read and write events that have the same abstract feasibility
 * constraint.
 * <p>
 * Without further control-flow information of the target program, every
 * {@code MemoryAccessBlock} either contains at most one read event which must
 * be last event in the block or solely consists of consecutive read events that
 * differ only in their global ID.
 *
 * @author YilongL
 *
 */
public class MemoryAccessBlock implements Iterable<Event> {

    private final List<Event> events;

    public MemoryAccessBlock(List<Event> events) {
        this.events = events;
    }

    public long getTID() {
        return events.get(0).getTID();
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

}
