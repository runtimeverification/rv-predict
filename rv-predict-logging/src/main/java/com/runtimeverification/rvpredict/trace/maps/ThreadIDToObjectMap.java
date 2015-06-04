package com.runtimeverification.rvpredict.trace.maps;

import java.util.function.Supplier;

import com.runtimeverification.rvpredict.trace.LongToObjectMap;

public final class ThreadIDToObjectMap<T> extends LongToObjectMap<T> {

    public ThreadIDToObjectMap(int expected, Supplier<T> newValue) {
        super(expected, newValue);
    }

    @Override
    protected final int hash(long key) {
        return (int) (key & mask);
    }

}
