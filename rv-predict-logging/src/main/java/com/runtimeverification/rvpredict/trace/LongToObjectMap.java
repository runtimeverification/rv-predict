package com.runtimeverification.rvpredict.trace;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Base class of our various specialized long-to-Object map implementations.
 * <p>
 * This class and its subclasses are not meant to be general-purpose map
 * implementation and, thus, do not implement the {@link Map} interface.
 * <p>
 * There are several benefits to have our own custom map implementation:
 * <li>primitive {@code long} as key type</li>
 * <li>specialized hash function based on the source of the keys (e.g., memory
 * location, thread ID)</li>
 * <li>easy to profile collision rate</li>
 * <li>very fast (since only need to support a few operations)</li>
 * <p>
 *
 * @author YilongL
 *
 * @param <T>
 *            the type of mapped values
 */
public abstract class LongToObjectMap<T> {

    private final int size;

    protected final int mask;

    private final long[] keys;

    private final T[] values;

    private int numOfEntries;

    private final int[] entryIndexes;

    private final Supplier<T> newValue;

    @SuppressWarnings("unchecked")
    public LongToObjectMap(int expected, Supplier<T> newValue) {
        size = 1 << (32 - Integer.numberOfLeadingZeros(expected * 2 - 1));
        mask = size - 1;
        keys = new long[size];
        values = (T[]) new Object[size];
        entryIndexes = new int[size];
        this.newValue = newValue;
    }

    protected abstract int hash(long key);

    protected T newValue() {
        if (newValue == null) {
            throw new UnsupportedOperationException("No supplier function available!");
        }
        return newValue.get();
    }

    public T computeIfAbsent(long key) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (values[p] == null) {
                keys[p] = key;
                entryIndexes[numOfEntries++] = p;
                return values[p] = newValue();
            } else if (key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        // should never happen
        throw new IllegalStateException();
    }

    public T get(long key) {
        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (values[p] == null || key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        // should never happen
        throw new IllegalStateException();
    }

    public T put(long key, T value) {
        if (value == null) {
            throw new NullPointerException();
        }

        int p = hash(key);
        for (int i = 0; i < size; i++) {
            if (values[p] == null) {
                keys[p] = key;
                values[p] = value;
                entryIndexes[numOfEntries++] = p;
                return null;
            } else if (key == keys[p]) {
                T oldValue = values[p];
                values[p] = value;
                return oldValue;
            }
            p = (p + 1) & mask;
        }
        // should never happen
        throw new IllegalStateException();
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

        public T getNextValue() {
            return values[entryIndexes[p]];
        }

    }

}
