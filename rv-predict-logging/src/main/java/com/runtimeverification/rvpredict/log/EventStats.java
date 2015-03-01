package com.runtimeverification.rvpredict.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.metadata.Metadata;

/**
 * Class used for profiling the logging process
 */
public class EventStats {

    private static final ConcurrentHashMap<String, ImmutablePair<AtomicLong, Set<Long>>> eventStats = new ConcurrentHashMap<>();

    public static void updateEventStats(int locId) {
        // TODO(YilongL): improve this method to record how many threads are
        // accessing a certain object and the percentage of each type of events?
        String className = Metadata.getLocationClass(locId);
        ImmutablePair<AtomicLong, Set<Long>> pair = eventStats.get(className);
        if (pair == null) {
            eventStats.putIfAbsent(className,
                    ImmutablePair.of(new AtomicLong(0), Sets.<Long> newConcurrentHashSet()));
            pair = eventStats.get(className);
        }
        pair.left.incrementAndGet();
        pair.right.add(Thread.currentThread().getId());
    }

    public static void printEventStats() {
        List<Entry<String, ImmutablePair<AtomicLong, Set<Long>>>> entries = new ArrayList<>(
                eventStats.entrySet());
        Collections.sort(entries,
                new Comparator<Entry<String, ImmutablePair<AtomicLong, Set<Long>>>>() {
                    @Override
                    public int compare(Entry<String, ImmutablePair<AtomicLong, Set<Long>>> e1,
                            Entry<String, ImmutablePair<AtomicLong, Set<Long>>> e2) {
                        return -Long.compare(e1.getValue().left.longValue(),
                                e2.getValue().left.longValue());
                    }
                });
        long total = 0;
        for (Map.Entry<String, ImmutablePair<AtomicLong, Set<Long>>> e : entries) {
            total += e.getValue().left.longValue();
        }
        System.err.println("-----------------------Events profiling statistics-------------------------");
        System.err.println("Total number of events: " + total);
        System.err.printf("%-10s %-10s %-10s %-10s %-50s%n",
                "#events",
                "pct.",
                "cpct.",
                "#threads",
                "Class");

        long c = 0;
        for (Map.Entry<String, ImmutablePair<AtomicLong, Set<Long>>> e : entries) {
            c += e.getValue().left.longValue();
            System.err.printf("%-10s %-10s %-10s %-10s %-50s%n",
                e.getValue().left,
                String.format("%.3f%%", e.getValue().left.longValue() * 100.0f / total),
                String.format("%.3f%%", c * 100.0f / total),
                e.getValue().right.size(),
                e.getKey());
            if (c * 1.0f / total > 0.9f) {
                break;
            }
        }
    }
}
