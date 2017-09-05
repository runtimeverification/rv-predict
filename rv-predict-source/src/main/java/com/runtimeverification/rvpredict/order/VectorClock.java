package com.runtimeverification.rvpredict.order;

import java.util.HashMap;
import java.util.Map;

public class VectorClock {
    public enum Comparison {
        BEFORE,
        AFTER,
        EQUAL,
        NOT_COMPARABLE;

        public Comparison reverse() {
            switch (ordinal()) {
                case 0: return AFTER;
                case 1: return BEFORE;
                case 2: return EQUAL;
            }
            return NOT_COMPARABLE;
        }

        public Comparison and(Comparison c) {
            if (c == EQUAL) return this;
            if (this == EQUAL) return c;
            if (c != this) return NOT_COMPARABLE;
            return this;
        }
    }

    Map<Long, Long> clocks;
    public VectorClock() {
        clocks = new HashMap<>();
    }
    public VectorClock(VectorClock c) {
        if (c == null) {
            clocks = new HashMap<>();
        } else {
            clocks = new HashMap<>(c.clocks);
        }
    }

    public void increment(long clock) {
        clocks.put(clock, clocks.getOrDefault(clock, 0L) + 1);
    }

    public void update(VectorClock c) {
        if (c != null) {
            c.clocks.forEach((clock, value) -> clocks.put(clock, Long.max(value, clocks.getOrDefault(clock, 0L))));
        }
    }

    public Comparison compareTo(VectorClock to) {
        if (to == null) return Comparison.NOT_COMPARABLE;
        if (clocks.size() > to.clocks.size()) return to.compareTo(this).reverse();
        Comparison aggregate = clocks.entrySet().stream().reduce(
                Comparison.EQUAL,
                (c, entry) -> {
                    if (c == Comparison.NOT_COMPARABLE) return Comparison.NOT_COMPARABLE;
                    Long toValue = to.clocks.get(entry.getKey());
                    if (toValue == null) return Comparison.NOT_COMPARABLE;
                    switch (Long.signum(entry.getValue().compareTo(toValue))) {
                        case -1: return c.and(Comparison.BEFORE);
                        case 1: return c.and(Comparison.AFTER);
                        default: return c;
                    }
                },
                Comparison::and);
        if (clocks.size() < to.clocks.size()) return aggregate.and(Comparison.BEFORE);
        return aggregate;
    }
}
