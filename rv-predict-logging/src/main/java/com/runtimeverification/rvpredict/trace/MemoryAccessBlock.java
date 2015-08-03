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
public class MemoryAccessBlock implements Iterable<Event>, Comparable<MemoryAccessBlock> {

    private final List<Event> events;

    private final MemoryAccessBlock prev;

    private final Event firstRead;

    public MemoryAccessBlock(List<Event> events, MemoryAccessBlock prev) {
        this.events = events;
        this.prev = prev;
        if (events.get(0).isRead()) {
            firstRead = events.get(0);
//            if (!events.stream().allMatch(Event::isRead)) {
//                throw new IllegalArgumentException();
//            }
        } else {
            int lastIdx = events.size() - 1;
            Event lastEvent = events.get(lastIdx);
            firstRead = lastEvent.isRead() ? lastEvent : null;
//            if (lastIdx > 0 && events.subList(0, lastIdx - 1).stream().anyMatch(Event::isRead)) {
//                throw new IllegalArgumentException();
//            }
        }
    }

    public long getTID() {
        return events.get(0).getTID();
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

    public Event getFirst() {
        return events.get(0);
    }

    public Event getLast() {
        return events.get(events.size() - 1);
    }

    public MemoryAccessBlock prev() {
        return prev;
    }

    public Event getFirstRead() {
        return firstRead;
    }

    @Override
    public int compareTo(MemoryAccessBlock blk) {
        return getFirst().compareTo(blk.getFirst());
    }

}
