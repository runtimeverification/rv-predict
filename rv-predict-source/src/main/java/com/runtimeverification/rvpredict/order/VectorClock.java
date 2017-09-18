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

    Map<Integer, Integer> clocks;
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

    public void increment(int clock) {
        clocks.merge(clock, 1, (oldValue, newValue) -> oldValue + 1);
    }

    public void update(VectorClock c) {
        if (c != null) {
            c.clocks.forEach((clock, value) -> clocks.merge(clock, value, Integer::max));
        }
    }

    public Comparison compareTo(VectorClock to) {
        if (to == null) return Comparison.NOT_COMPARABLE;
        if (clocks.size() > to.clocks.size()) return to.compareTo(this).reverse();
        Comparison c = Comparison.EQUAL;
        for (Map.Entry<Integer, Integer> entry : clocks.entrySet()) {
            Integer toValue = to.clocks.get(entry.getKey());
            if (toValue == null) {
                return Comparison.NOT_COMPARABLE;
            }
            switch (Long.signum(entry.getValue().compareTo(toValue))) {
                case -1:
                    c = c.and(Comparison.BEFORE);
                    break;
                case 1:
                    c = c.and(Comparison.AFTER);
                    break;
            }
            if (c == Comparison.NOT_COMPARABLE) return Comparison.NOT_COMPARABLE;
        }
        if (clocks.size() < to.clocks.size()) return c.and(Comparison.BEFORE);
        return c;
    }

    @Override
    public String toString() {
        return clocks.toString();
    }
}
