package com.runtimeverification.rvpredict.order;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.stream.Collectors;

public class OrderedRaceDetector {
    Map<Long,ReadonlyOrderedEventInterface> lastWrites = new HashMap<>();
    Map<Long,Collection<ReadonlyOrderedEventInterface>> lastReads = new HashMap<>();

    public Collection<ReadonlyOrderedEventInterface> process(ReadonlyOrderedEventInterface event) {
        if (event.isReadOrWrite())
            return handleEvent(event);
       return Collections.emptyList();
    }

    private Collection<ReadonlyOrderedEventInterface> handleEvent(ReadonlyOrderedEventInterface event) {
        assert event.isReadOrWrite();
        ImmutableList.Builder<ReadonlyOrderedEventInterface> builder = new ImmutableList.Builder<>();
        long address = event.getDataInternalIdentifier();
        ReadonlyOrderedEventInterface lastWrite = lastWrites.get(address);
        if (lastWrite != null && lastWrite.getVectorClock().compareTo(event.getVectorClock()) != VectorClock.Comparison.BEFORE) {
            builder.add(lastWrite);
        }
        Collection<ReadonlyOrderedEventInterface> lastRead = lastReads.get(address);
        if (lastRead == null) {
            lastRead = new ArrayList<>();
            lastReads.put(address, lastRead);
        }
        if (event.isRead()) {
            lastRead.add(event);
        } else { // event.isWrite()
            lastWrites.put(address, event);
            builder.addAll(lastRead.stream()
                    .filter((read) -> read.getVectorClock().compareTo(event.getVectorClock()) != VectorClock.Comparison.BEFORE)
                    .collect(Collectors.toList()));
            lastRead.clear();
        }
        return builder.build();
    }

}
