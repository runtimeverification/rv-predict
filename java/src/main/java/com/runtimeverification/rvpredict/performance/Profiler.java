package com.runtimeverification.rvpredict.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helps with measuring code performance.
 *
 * The only way one can get an instance of this class is through the static {@link #instance()} method. The default
 * instance does not measure performance in any way, so calls to its methods are cheap. To get a profiler instance
 * which measures performance one must first call the static {@link #enableProfiler()} method.
 *
 * Measuring the time for a given piece of code usually looks like this:
 * try (ProfilerToken ignored = Profiler.instance().start("text identifying what is being measured")) {
 *     [code that is being measured]
 * }
 * or
 * Profiler.instance().count();
 *
 * Of course, one can manually call the {@link ProfilerToken#close()} method, but using try-with-resources
 * is safer. Also, the default instance start method returns null tokens, so one must be able to handle that.
 *
 * At the end of the application, or whenever one is interested in the profiler stats, one would call
 * {@link #toString()} to get a string representation of the counters
 *
 * Normally this class is not extended outside of this file.
 */
public abstract class Profiler {
    private static final Profiler ENABLED_PROFILER = new EnabledProfiler();
    private static final Profiler DISABLED_PROFILER = new DisabledProfiler();
    private static Profiler instance = DISABLED_PROFILER;

    private Profiler() {}

    /**
     * After calling this method, the {@link #instance()} method will return a profiler which
     * measures performance
     */
    public static void enableProfiler() {
        instance = ENABLED_PROFILER;
    }

    /**
     * {@see #enableProfiler()}.
     *
     * @return the current profiler instance.
     */
    public static Profiler instance() {
        return instance;
    }

    /**
     * Starts a performance measurement.
     *
     * The measurement ends when the .close() method of the returned token is called.
     *
     * @param tag identifies what is being measured.
     *
     * @return A measurement token.
     */
    public abstract ProfilerToken start(String tag);

    /**
     * Notifies the profiler that one instance of whatever is denoted by tag happened, but without assigning
     * any special meaning to the time tag took. A profiler is allowed to assign a very small amount of time to this
     * tag.
     *
     * @param tag identifies what is being counted.
     */
    public abstract void count(String tag);

    private static class DisabledProfiler extends Profiler {
        @Override
        public ProfilerToken start(String tag) {
            return null;
        }

        @Override
        public void count(String tag) {
        }
    }

    private static class EnabledProfiler extends Profiler {
        private final Map<String, Integer> tagToIndex;
        private final List<ItemData> items;

        private EnabledProfiler() {
            this.tagToIndex = new HashMap<>();
            this.items = new ArrayList<>();
        }

        @Override
        public synchronized ProfilerToken start(String tag) {
            Integer indexObj = tagToIndex.get(tag);
            ItemData itemData;
            if (indexObj == null) {
                tagToIndex.put(tag, items.size());
                itemData = new ItemData(tag);
                items.add(itemData);
            } else {
                itemData = items.get(indexObj);
            }
            itemData.start();
            return new ProfilerToken(itemData);
        }

        @Override
        public synchronized void count(String tag) {
            start(tag).close();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String endOfLine = String.format("%n");
            for (ItemData itemData : items) {
                sb.append(itemData);
                sb.append(endOfLine);
            }
            return sb.toString();
        }
    }

}
