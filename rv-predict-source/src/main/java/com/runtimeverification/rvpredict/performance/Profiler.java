package com.runtimeverification.rvpredict.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Profiler {
    private static final Profiler ENABLED_PROFILER = new EnabledProfiler();
    private static final Profiler DISABLED_PROFILER = new DisabledProfiler();
    private static Profiler instance = DISABLED_PROFILER;

    public static void enableProfiler() {
        instance = ENABLED_PROFILER;
    }

    public static Profiler instance() {
        return instance;
    }

    public abstract ProfilerToken start(String tag);

    private static class DisabledProfiler extends  Profiler {
        @Override
        public ProfilerToken start(String tag) {
            return null;
        }
    }

    private static class EnabledProfiler extends Profiler {
        private final Map<String, Integer> tagToIndex;
        private final List<ItemData> items;

        private EnabledProfiler() {
            this.tagToIndex = new HashMap<>();
            this.items = new ArrayList<>();
        }

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
