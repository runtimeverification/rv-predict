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
 * <em>Limitation: the maximum number of keys of this map is now fixed.</em>
 * <p>
 *
 * @author YilongL
 *
 * @param <T>
 *            the type of mapped values
 */
public abstract class LongToObjectMap<T> {

    protected final int capacity;

    protected final int mask;

    private final long[] keys;

    private final T[] values;

    protected int size;

    private final int[] entryIndexes;

    protected final Supplier<T> newValue;

    @SuppressWarnings("unchecked")
    public LongToObjectMap(int expected, Supplier<T> newValue) {
        capacity = 1 << (32 - Integer.numberOfLeadingZeros(expected * 2 - 1));
        mask = capacity - 1;
        keys = new long[capacity];
        values = (T[]) new Object[capacity];
        entryIndexes = new int[capacity];
        this.newValue = newValue;
    }

    protected abstract int hash(long key);

    protected T newValue() {
        if (newValue == null) {
            throw new UnsupportedOperationException("No supplier function available!");
        }
        return newValue.get();
    }

    public final boolean isFull() {
        return capacity == size;
    }

    public final T computeIfAbsent(long key) {
        int p = hash(key);
        for (int i = 0; i < capacity; i++) {
            if (values[p] == null) {
                keys[p] = key;
                entryIndexes[size++] = p;
                return values[p] = newValue();
            } else if (key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        throw new UnsupportedOperationException("Automatic grow operation not implemented!");
    }

    public final T get(long key) {
        int p = hash(key);
        for (int i = 0; i < capacity; i++) {
            if (values[p] == null || key == keys[p]) {
                return values[p];
            }
            p = (p + 1) & mask;
        }
        return null;
    }

    public final T put(long key, T value) {
        if (value == null) {
            throw new NullPointerException();
        }

        int p = hash(key);
        for (int i = 0; i < capacity; i++) {
            if (values[p] == null) {
                keys[p] = key;
                values[p] = value;
                entryIndexes[size++] = p;
                return null;
            } else if (key == keys[p]) {
                T oldValue = values[p];
                values[p] = value;
                return oldValue;
            }
            p = (p + 1) & mask;
        }
        throw new UnsupportedOperationException("Automatic grow operation not implemented!");
    }

    protected final void putAll(LongToObjectMap<T> m) {
        for (int i = 0; i < m.size; i++) {
            put(m.keys[m.entryIndexes[i]], m.values[m.entryIndexes[i]]);
        }
    }

    public final void clear() {
        // only need to clear the values array
        size = 0;
        Arrays.fill(values, null);
    }

    public final EntryIterator iterator() {
        return new EntryIterator();
    }

    public final class EntryIterator {

        private int p;

        public void incCursor() {
            p++;
        }

        public boolean hasNext() {
            return p != size;
        }

        public long getNextKey() {
            return keys[entryIndexes[p]];
        }

        public T getNextValue() {
            return values[entryIndexes[p]];
        }

    }

}
