package com.runtimeverification.rvpredict.log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.runtimeverification.rvpredict.metadata.Metadata;

/**
 * Class used for profiling the logging process
 */
public class EventStats {

    private static final ConcurrentHashMap<String, AtomicLong> eventStats = new ConcurrentHashMap<>();

    public static void updateEventStats(int locId) {
        // TODO(YilongL): improve this method to record how many threads are
        // accessing a certain object and the percentage of each type of events?
        String className = Metadata.getLocationClass(locId);
        AtomicLong atoml = eventStats.get(className);
        if (atoml == null) {
            eventStats.putIfAbsent(className, new AtomicLong(0));
            atoml = eventStats.get(className);
        }
        atoml.incrementAndGet();
    }

    public static void printEventStats() {
        List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(eventStats.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, AtomicLong>>() {
            @Override
            public int compare(Map.Entry<String, AtomicLong> e1,
                               Map.Entry<String, AtomicLong> e2) {
                if (e1.getValue().longValue() < e2.getValue().longValue()) {
                    return 1;
                } else if (e1.getValue().equals(e2.getValue())) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        int total = 0;
        for (Map.Entry<String, AtomicLong> e : entries) {
            total += e.getValue().longValue();
        }
        System.err.println("-----------------------Events profiling statistics-------------------------");
        System.err.println("Total number of events: " + total);
        int c = 0;
        for (Map.Entry<String, AtomicLong> e : entries) {
            c += e.getValue().longValue();
            System.err.printf("%-10s %-10s %-10s %-50s%n",
                e.getValue(),
                String.format("%.3f%%", e.getValue().longValue() * 100.0f / total),
                String.format("%.3f%%", c * 100.0f / total),
                e.getKey());
            if (c * 1.0f / total > 0.9f) {
                break;
            }
        }
    }
}
