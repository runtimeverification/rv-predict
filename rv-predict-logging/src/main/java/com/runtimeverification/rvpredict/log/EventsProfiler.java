package com.runtimeverification.rvpredict.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.EventType;

/**
 * Class used for profiling the logging process
 *
 * @author YilongL
 */
public class EventsProfiler {

    private static class ClassStat {
        final AtomicLong numOfEvents = new AtomicLong(0);
        final ConcurrentMap<Integer, Set<Long>> addrToThreadIds = Maps.newConcurrentMap();
    }

    private static final ConcurrentMap<String, ClassStat> classNameToStat = Maps.newConcurrentMap();

    public static void updateEventStats(EventType eventType, int locId, int addrl, int addrr,
            long value) {
        String className = Metadata.getLocationClass(locId);
        ClassStat stat = classNameToStat.get(className);
        if (stat == null) {
            classNameToStat.putIfAbsent(className, new ClassStat());
            stat = classNameToStat.get(className);
        }
        stat.numOfEvents.incrementAndGet();
        if ((eventType == EventType.READ || eventType == EventType.WRITE) && addrr < 0) {
            stat.addrToThreadIds.putIfAbsent(addrl, Sets.<Long>newConcurrentHashSet());
            stat.addrToThreadIds.get(addrl).add(Thread.currentThread().getId());
        }
    }

    public static void printEventStats() {
        List<Entry<String, ClassStat>> entries = new ArrayList<>(classNameToStat.entrySet());
        Collections.sort(entries, new Comparator<Entry<String, ClassStat>>() {
            @Override
            public int compare(Entry<String, ClassStat> e1, Entry<String, ClassStat> e2) {
                return -Long.compare(e1.getValue().numOfEvents.longValue(),
                        e2.getValue().numOfEvents.longValue());
            }
        });
        long total = 0;
        for (Map.Entry<String, ClassStat> e : entries) {
            total += e.getValue().numOfEvents.longValue();
        }
        System.err.println("-----------------------Events profiling statistics-------------------------");
        System.err.println("Total number of events: " + total);
        System.err.printf("%-10s %-10s %-10s %-15s %-50s%n",
                "#events",
                "pct.",
                "cpct.",
                "multithreaded?",
                "class");

        long c = 0;
        for (Map.Entry<String, ClassStat> e : entries) {
            c += e.getValue().numOfEvents.longValue();
            boolean isMultiThreaded = false;
            for (Set<Long> threadIds : e.getValue().addrToThreadIds.values()) {
                isMultiThreaded = isMultiThreaded || threadIds.size() > 1;
            }
            System.err.printf("%-10s %-10s %-10s %-15s %-50s%n",
                e.getValue().numOfEvents,
                String.format("%.3f%%", e.getValue().numOfEvents.longValue() * 100.0f / total),
                String.format("%.3f%%", c * 100.0f / total),
                isMultiThreaded ? "      x" : "",
                e.getKey());
            if (c * 1.0f / total > 0.9f) {
                break;
            }
        }
    }
}
