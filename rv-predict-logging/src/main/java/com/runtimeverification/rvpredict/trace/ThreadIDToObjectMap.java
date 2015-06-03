package com.runtimeverification.rvpredict.trace;

import java.util.function.Supplier;

public final class ThreadIDToObjectMap<T> extends LongToObjectMap<T> {

    public ThreadIDToObjectMap(int expected, Supplier<T> newValue) {
        super(expected, newValue);
    }

    @Override
    protected final int hash(long key) {
        return (int) (key & mask);
    }

}
