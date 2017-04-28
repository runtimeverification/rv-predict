package com.runtimeverification.rvpredict.profiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by virgil on 3/10/17.
 */
public class Profiler {
    private static final Map<String, Long> lineToTimeNano = new HashMap<>();
    private static final Map<String, Long> stackToTimeNano = new HashMap<>();
    private static final Stack<StackElement> timeStackNano = new Stack<>();
    private static final long startTimeNano = System.nanoTime();
    private static long currentTimeNano = System.nanoTime();

    public static void push() {
        synchronized (Profiler.class) {
            long ctNano = System.nanoTime();
            timeStackNano.add(new StackElement(getTag(), ctNano, currentTimeNano));
            currentTimeNano = ctNano;
        }
    }
    public static void pop() {
        synchronized (Profiler.class) {
            long ctNano = System.nanoTime();
            StackElement se = timeStackNano.pop();
            long deltaNano = ctNano - se.startTimeNano;
            stackToTimeNano.compute(se.tag, (k, v) -> (v == null) ? deltaNano : deltaNano + v);
            currentTimeNano = se.formerCurrentTimeNano;
        }
    }
    public static void log() {
        synchronized (Profiler.class) {
            long ctNano = System.nanoTime();
            long deltaNano = ctNano - currentTimeNano;
            currentTimeNano = ctNano;
            String tag = getTag();
            lineToTimeNano.compute(tag, (k, v) -> (v == null) ? deltaNano : deltaNano + v);
        }
    }
    public static void dump() {
        long ctNano = System.nanoTime();
        System.out.println("---------------------------");
        System.out.println("Log times:");
        dumpSortedMap(lineToTimeNano, ctNano);

        System.out.println("---------------------------");
        System.out.println("Stack times:");
        dumpSortedMap(stackToTimeNano, ctNano);

        System.out.println("---------------------------");
        if (!timeStackNano.empty()) {
            System.err.println("Profiler stack not empty! The profiling data may be wrong.");
        }
    }

    private static void dumpSortedMap(Map<String, Long> map, Long ctNano) {
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(map.entrySet());
        Collections.sort(sorted, (a, b) -> b.getValue().compareTo(a.getValue()));
        sorted.forEach(a -> System.out.println(formatTime(a.getValue(), ctNano) + " : " + a.getKey()));
    }

    private static String formatTime(long deltaNano, long currentTimeNano) {
        long totalDeltaNano = currentTimeNano - startTimeNano;
        double percentage = deltaNano * 100 / (double)totalDeltaNano;
        double length = deltaNano;
        if (length < 1000) {
            return String.format("%1$8.3fns (%2$5.2f%%)", length, percentage);
        }
        length /= 1000;
        if (length < 1000) {
            return String.format("%1$8.3fus (%2$5.2f%%)", length, percentage);
        }
        length /= 1000;
        if (length < 1000) {
            return String.format("%1$8.3fms (%2$5.2f%%)", length, percentage);
        }
        length /= 1000;
        return String.format("%1$8.3fs (%2$5.2f%%)", length, percentage);
    }

    private static String getTag() {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        return ste.getFileName() + ":" + ste.getLineNumber();
    }
    private static class StackElement {
        private final String tag;
        private final long startTimeNano;
        private final long formerCurrentTimeNano;

        StackElement(String tag, long startTimeNano, long formerCurrentTimeNano) {
            this.tag = tag;
            this.startTimeNano = startTimeNano;
            this.formerCurrentTimeNano = formerCurrentTimeNano;
        }
    }
}
