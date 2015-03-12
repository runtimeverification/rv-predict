package com.runtimeverification.rvpredict.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.EventType;

/**
 * Event profiler.
 *
 * @author YilongL
 */
public class EventProfiler implements EventHandler<EventItem> {

    private static final Map<String, GlobalClassStatistics> classNameToGlobalStat = Maps.newHashMap();

    private final Map<String, PerThreadClassStatistics> classNameToPerThreadStat = Maps.newHashMap();

    private boolean closed;

    @Override
    public void onEvent(EventItem eventItem, long sequence, boolean endOfBatch) {
        if (!closed) {
            if (eventItem.GID > 0) {
                String className = Metadata.getLocationClass(eventItem.ID);
                PerThreadClassStatistics stat = classNameToPerThreadStat.get(className);
                if (stat == null) {
                    stat = new PerThreadClassStatistics();
                    classNameToPerThreadStat.put(className, stat);
                }

                stat.numOfEvents++;
                if ((eventItem.TYPE == EventType.READ || eventItem.TYPE == EventType.WRITE)
                        && eventItem.ADDRR < 0) {
                    stat.objHashCodes.add(eventItem.ADDRL);
                }
            } else {
                closed = true;
                synchronized (classNameToGlobalStat) {
                    for (Entry<String, PerThreadClassStatistics> e : classNameToPerThreadStat
                            .entrySet()) {
                        GlobalClassStatistics globalStat = classNameToGlobalStat.get(e.getKey());
                        if (globalStat == null) {
                            globalStat = new GlobalClassStatistics();
                            classNameToGlobalStat.put(e.getKey(), globalStat);
                        }

                        PerThreadClassStatistics perThreadStat = e.getValue();
                        globalStat.numOfEvents += perThreadStat.numOfEvents;
                        if (!globalStat.isMultithreaded) {
                            int origSize = globalStat.objHashCodes.size();
                            globalStat.objHashCodes.addAll(perThreadStat.objHashCodes);
                            if (origSize + perThreadStat.objHashCodes.size() > globalStat.objHashCodes
                                    .size()) {
                                globalStat.isMultithreaded = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void printEventStats() {
        List<Entry<String, GlobalClassStatistics>> entries = new ArrayList<>(
                classNameToGlobalStat.entrySet());
        Collections.sort(entries, new Comparator<Entry<String, GlobalClassStatistics>>() {
            @Override
            public int compare(Entry<String, GlobalClassStatistics> e1,
                    Entry<String, GlobalClassStatistics> e2) {
                return -Long.compare(e1.getValue().numOfEvents, e2.getValue().numOfEvents);
            }
        });
        long total = 0;
        for (Map.Entry<String, GlobalClassStatistics> e : entries) {
            total += e.getValue().numOfEvents;
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
        for (Map.Entry<String, GlobalClassStatistics> e : entries) {
            c += e.getValue().numOfEvents;
            System.err.printf("%-10s %-10s %-10s %-15s %-50s%n",
                e.getValue().numOfEvents,
                String.format("%.3f%%", e.getValue().numOfEvents * 100.0f / total),
                String.format("%.3f%%", c * 100.0f / total),
                e.getValue().isMultithreaded ? "      x" : "",
                e.getKey());
            if (c * 1.0f / total > 0.9f) {
                break;
            }
        }
    }

    private static class PerThreadClassStatistics {
        private long numOfEvents;
        private final Set<Integer> objHashCodes = Sets.newHashSet();
    }

    private static class GlobalClassStatistics {
        private long numOfEvents;
        private boolean isMultithreaded;
        private final Set<Integer> objHashCodes = Sets.newHashSet();
    }

}
