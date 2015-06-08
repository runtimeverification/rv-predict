package com.runtimeverification.rvpredict.trace.maps;

import java.util.function.Supplier;

import com.runtimeverification.rvpredict.trace.LongToObjectMap;

/**
 * Map from long value that represents memory address to Object.
 *
 * @author YilongL
 *
 * @param <T>
 */
public class MemoryAddrToObjectMap<T> extends LongToObjectMap<T> {

    public MemoryAddrToObjectMap(int expected, Supplier<T> newValue) {
        super(expected, newValue);
    }

    public MemoryAddrToObjectMap(int expected) {
        super(expected, null);
    }

    /**
     * The high 32 bit value that obtains from {@code System.identityHashCode}
     * is already random. The bitwise {@code xor} operation is then used to
     * scatter the likely consecutive low 32 bit value.
     */
    @Override
    protected final int hash(long key) {
        return ((int) (key >> 32) ^ (int) key) & mask;
    }

}
