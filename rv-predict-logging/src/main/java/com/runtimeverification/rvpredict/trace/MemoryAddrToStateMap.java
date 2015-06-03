package com.runtimeverification.rvpredict.trace;

/**
 * Specializes the {@link MemoryAddrToObjectMap} implementation to the specific
 * {@link MemoryAddrState} value type to bypass the supplier function.
 *
 * @author YilongL
 */
public final class MemoryAddrToStateMap extends MemoryAddrToObjectMap<MemoryAddrState> {

    public MemoryAddrToStateMap(int expected) {
        super(expected, null);
    }

    @Override
    protected final MemoryAddrState newValue() {
        return new MemoryAddrState();
    }

}
