package com.runtimeverification.rvpredict.trace;

import java.util.function.Supplier;

public class MemoryAddrToObjectMap<T> extends LongToObjectMap<T> {

    public MemoryAddrToObjectMap(int expected, Supplier<T> newValue) {
        super(expected, newValue);
    }

    @Override
    protected final int hash(long key) {
        return ((int) (key >> 32) ^ (int) key) & mask;
    }

}
