package com.runtimeverification.rvpredict.algorithm;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

public class BinarySearch {
    public static <T, K extends Comparable<K>>
    OptionalInt getIndexLessOrEqual(List<T> elements, Function<T, K> elementToKey, K key) {
        if (elements.isEmpty()) {
            return OptionalInt.empty();
        }
        return getIndexLessOrEqualRec(
                elements, elementToKey, key, 0, elements.size() - 1, OptionalInt.empty());
    }

    /**
     * Searches between start and end inclusive. Returns empty() if key > elementToKey.apply(elements.get(start)).
     *
     * Invariant: all elements before start are strictly less than the key, all elements after end are strictly
     * larger.
     */
    private static <T, K extends Comparable<K>>
    OptionalInt getIndexLessOrEqualRec(
            List<T> elements, Function<T, K> elementToKey, K key, int start, int end, OptionalInt lastIndexLessOrEqual) {
        assert start <= end;
        if (start == end) {
            K startKey = elementToKey.apply(elements.get(start));
            if (startKey.compareTo(key) > 0) {
                return lastIndexLessOrEqual;
            }
            return OptionalInt.of(start);
        }

        if (start == end - 1) {
            K endKey = elementToKey.apply(elements.get(end));
            if (endKey.compareTo(key) > 0) {
                return getIndexLessOrEqualRec(elements, elementToKey, key, start, start, lastIndexLessOrEqual);
            }
            return OptionalInt.of(end);
        }

        int mid = (start + end) / 2;
        assert start < mid;
        assert mid < end;

        K midKey = elementToKey.apply(elements.get(mid));
        int cmp = midKey.compareTo(key);
        if (cmp > 0) {
            return getIndexLessOrEqualRec(elements, elementToKey, key, start, mid - 1, lastIndexLessOrEqual);
        }
        if (cmp < 0) {
            return getIndexLessOrEqualRec(elements, elementToKey, key, mid + 1, end, OptionalInt.of(mid));
        }
        // Note that, statistically speaking, this line is reached very rarely, so it would be more
        // efficient to merge this case with one of the previous ones. However, the code is easier
        // to read this way, and the performance difference is not that large.
        return OptionalInt.of(mid);
    }
}
