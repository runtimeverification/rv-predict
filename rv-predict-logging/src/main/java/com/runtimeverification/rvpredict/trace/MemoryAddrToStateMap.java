package com.runtimeverification.rvpredict.trace;

import java.util.Arrays;

public class MemoryAddrToStateMap {

    private final int size;

    private final int mask;

    private final long[] keys;

    private final MemoryAddrState[] values;

    private int numOfEntries;

    private final int[] entryIndexes;

    public MemoryAddrToStateMap(int expected) {
        size = 1 << (32 - Integer.numberOfLeadingZeros(expected * 2 - 1));
        mask = size - 1;
        keys = new long[size];
        values = new MemoryAddrState[size];
        entryIndexes = new int[size];
    }

    private int hash(long key) {
        return ((int) (key >> 32) ^ (int) key) & mask;
    }

    public MemoryAddrState computeIfAbsent(long key) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (values[p] == null) {
                keys[p] = key;
                entryIndexes[numOfEntries++] = p;
                return values[p] = new MemoryAddrState();
            } else if (key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        // should never happen
        return null;
    }

    public MemoryAddrState get(long key) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (values[p] == null || key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        // should never happen
        return null;
    }

    public void clear() {
        // only need to clear the values array
        numOfEntries = 0;
        Arrays.fill(values, null);
    }

    public EntryIterator iterator() {
        return new EntryIterator();
    }

    public class EntryIterator {

        private int p;

        public void incCursor() {
            p++;
        }

        public boolean hasNext() {
            return p != numOfEntries;
        }

        public long getNextKey() {
            return keys[entryIndexes[p]];
        }

        public MemoryAddrState getNextValue() {
            return values[entryIndexes[p]];
        }

    }

}
