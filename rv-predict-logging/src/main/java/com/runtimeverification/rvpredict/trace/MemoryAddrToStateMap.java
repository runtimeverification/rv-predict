package com.runtimeverification.rvpredict.trace;

public final class MemoryAddrToStateMap extends MemoryAddrToObjectMap<MemoryAddrState> {

    public MemoryAddrToStateMap(int expected) {
        super(expected, null);
    }

    @Override
    protected final MemoryAddrState newValue() {
        return new MemoryAddrState();
    }

}
