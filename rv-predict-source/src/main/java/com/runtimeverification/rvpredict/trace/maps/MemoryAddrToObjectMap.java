package com.runtimeverification.rvpredict.trace.maps;

import java.util.HashMap;
import java.util.function.Supplier;

import com.runtimeverification.rvpredict.log.DataAddress;

/**
 * Map from long value that represents memory address to Object.
 *
 * @author YilongL
 *
 * @param <T>
 */
// TODO(virgil): This used to extend a LongToObjectMap<T>. I should profile it to see if I need a custom implementation.
public class MemoryAddrToObjectMap<T> extends HashMap<DataAddress, T> {
    private final Supplier<T> newValue;

    MemoryAddrToObjectMap(int expected, Supplier<T> newValue) {
        super(expected);
        this.newValue = newValue;
    }

    public MemoryAddrToObjectMap(int expected) {
        super(expected);
        this.newValue = null;
    }

    public final T computeIfAbsent(DataAddress key) {
        return computeIfAbsent(key, k -> newValue());
    }

    protected T newValue() {
        if (newValue == null) {
            throw new UnsupportedOperationException("No supplier function available!");
        }
        return newValue.get();
    }
}
