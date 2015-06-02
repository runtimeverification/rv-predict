package com.runtimeverification.rvpredict.trace;

import java.util.Arrays;

/**
 * A custom implementation of a long-to-long hash map which has a maximum size
 * and uses a Least Recently Used algorithm to remove stale entries.
 *
 * @author Yilong
 */
public class MemoryAddrToValueMap {

    /**
     * We know for sure that memory address location cannot be 0 (which
     * represents {@code null}).
     */
    private static final long NOT_USED = 0L;

    private static final long DEFAULT_VALUE = 0L;

    private static final int NULL_PTR = -1;

    private static final int PURGE_THRESHOLD = 32;

    /**
     * The maximum number of keys to store in this hash table.
     */
    private final int maxNumOfKeys;

    /**
     * The number of slots in the hash table.
     */
    private final int size;

    private final int mask;

    private int numOfEntries;

    private final long[] keys;

    private final long[] values;

    private int head = NULL_PTR;

    private int tail = NULL_PTR;

    private final int[] prev;

    private final int[] next;

    public MemoryAddrToValueMap(int expected) {
        this.maxNumOfKeys = expected;
        size = 1 << (32 - Integer.numberOfLeadingZeros(expected * 2 - 1));
        mask = size - 1;
        keys = new long[size];
        values = new long[size];
        prev = new int[size];
        next = new int[size];
        Arrays.fill(prev, NULL_PTR);
        Arrays.fill(next, NULL_PTR);
    }

    private int hash(long key) {
        return ((int) (key >> 32) ^ (int) key) & mask;
    }

    public long get(long key) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (keys[p] == NOT_USED) {
                return DEFAULT_VALUE;
            } else if (key == keys[p]) {
                moveIndexToFirst(p);
                return values[p];
            }
            p = (p + 1) & mask;
        }
        // should never happen
        throw new IllegalStateException("The hash table is full!");
    }

    public void put(long key, long value) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (keys[p] == NOT_USED) {
                keys[p] = key;
                values[p] = value;
                addIndexAsFirst(p);
                if (++numOfEntries > maxNumOfKeys + PURGE_THRESHOLD) {
                    while (numOfEntries > maxNumOfKeys) {
                        removeLastEntry();
                    }
                }
                return;
            } else if (key == keys[p]) {
                values[p] = value;
                moveIndexToFirst(p);
                return;
            }
            p = (p + 1) & mask;
        }
        // should never happen
        throw new IllegalStateException("The hash table is full!");
    }

    /**
     * Appends the given index before the head of the doubly-linked list.
     *
     * @param p
     *            the index
     */
    private void addIndexAsFirst(int p) {
        if (head == NULL_PTR) {
            head = tail = p;
        } else {
            next[p] = head;
            prev[head] = p;
            head = p;
        }
    }

    /**
     * Moves the given index to the head of the doubly-linked list.
     *
     * @param p
     *            the index
     */
    private void moveIndexToFirst(int p) {
        if (p != head) {
            int p0 = prev[p];
            int p1 = next[p];
            next[p0] = p1;
            if (p1 == NULL_PTR) {
                tail = p0;
            } else {
                prev[p1] = p0;
            }
            next[p] = head;
            prev[head] = p;
            head = p;
        }
    }

    /**
     * Removes the tail key from the hash table.
     */
    private void removeLastEntry() {
        if (tail != NULL_PTR) {
            numOfEntries--;
            keys[tail] = NOT_USED;
            int p0 = prev[tail];
            if (p0 == NULL_PTR) {
                head = tail = NULL_PTR;
            } else {
                next[p0] = NULL_PTR;
                prev[tail] = NULL_PTR;
                tail = p0;
            }
        } else {
            throw new IllegalStateException("No element to remove");
        }
    }

}
