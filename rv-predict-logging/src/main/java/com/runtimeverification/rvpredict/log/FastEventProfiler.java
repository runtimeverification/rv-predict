package com.runtimeverification.rvpredict.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.runtimeverification.rvpredict.metadata.Metadata;

/**
 * Fast (but inaccurate) event profiler with a typical slowdown of 10x.
 *
 * @author YilongL
 */
public class FastEventProfiler {

    // this should be enough for more than 1 million lines of code
    private static final int MAX_NUM_OF_LOCATIONS = 1024 * 1024;

    private final long[] counter = new long[MAX_NUM_OF_LOCATIONS]; // 8MB

    private final Metadata metadata;

    public FastEventProfiler() {
        this.metadata = Metadata.instance();
    }

    public void update(int locId) {
        counter[locId]++;
    }

    public void printProfilingResult() {
        Map<String, Long> map = new HashMap<>();
        for (int locId = 0; locId < counter.length; locId++) {
            String className = metadata.getLocationClass(locId);
            Long val = map.get(className);
            val = val == null ? counter[locId] : val + counter[locId];
            map.put(className, val);
        }

        List<Entry<String, Long>> entries = new ArrayList<>(map.entrySet());
        Collections.sort(entries, new Comparator<Entry<String, Long>>() {
            @Override
            public int compare(Entry<String, Long> e1, Entry<String, Long> e2) {
                return -Long.compare(e1.getValue(), e2.getValue());
            }
        });
        long total = 0;
        for (Map.Entry<String, Long> e : entries) {
            total += e.getValue();
        }
        System.err
                .println("-----------------------Events profiling statistics-------------------------");
        System.err.println("Total number of events: " + total);
        System.err.printf("%-10s %-10s %-10s %-50s%n", "#events", "pct.", "cpct.", "class");

        long c = 0;
        for (Map.Entry<String, Long> e : entries) {
            c += e.getValue();
            System.err.printf("%-10s %-10s %-10s %-50s%n", e.getValue(),
                    String.format("%.3f%%", e.getValue() * 100.0f / total),
                    String.format("%.3f%%", c * 100.0f / total), e.getKey());
            if (c * 1.0f / total > 0.9f) {
                break;
            }
        }
    }

}
