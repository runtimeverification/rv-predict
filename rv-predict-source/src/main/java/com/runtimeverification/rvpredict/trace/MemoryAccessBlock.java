package com.runtimeverification.rvpredict.trace;

import java.util.Iterator;
import java.util.List;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

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
public class MemoryAccessBlock implements Iterable<ReadonlyEventInterface>, Comparable<MemoryAccessBlock> {

    private final List<ReadonlyEventInterface> events;

    private final MemoryAccessBlock prev;

    private final ReadonlyEventInterface firstRead;

    public MemoryAccessBlock(List<ReadonlyEventInterface> events, MemoryAccessBlock prev) {
        this.events = events;
        this.prev = prev;
        if (events.get(0).isRead()) {
            firstRead = events.get(0);
//            if (!events.stream().allMatch(ReadonlyEventInterface::isRead)) {
//                throw new IllegalArgumentException();
//            }
        } else {
            int lastIdx = events.size() - 1;
            ReadonlyEventInterface lastEvent = events.get(lastIdx);
            firstRead = lastEvent.isRead() ? lastEvent : null;
//            if (lastIdx > 0 && events.subList(0, lastIdx - 1).stream().anyMatch(ReadonlyEventInterface::isRead)) {
//                throw new IllegalArgumentException();
//            }
        }
    }

    public long getTID() {
        return events.get(0).getThreadId();
    }

    @Override
    public Iterator<ReadonlyEventInterface> iterator() {
        return events.iterator();
    }

    public ReadonlyEventInterface getFirst() {
        return events.get(0);
    }

    public ReadonlyEventInterface getLast() {
        return events.get(events.size() - 1);
    }

    public MemoryAccessBlock prev() {
        return prev;
    }

    public ReadonlyEventInterface getFirstRead() {
        return firstRead;
    }

    @Override
    public int compareTo(MemoryAccessBlock blk) {
        return getFirst().compareTo(blk.getFirst());
    }

}
