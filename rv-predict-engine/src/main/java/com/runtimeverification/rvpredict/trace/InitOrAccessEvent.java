package com.runtimeverification.rvpredict.trace;

public abstract class InitOrAccessEvent extends AbstractEvent {

    protected final long value;
    protected final MemoryAddr addr;

    protected InitOrAccessEvent(long GID, long TID, int ID, EventType type, int objectHashCode,
            int index, long value) {
        super(GID, TID, ID, type);
        this.addr = new MemoryAddr(objectHashCode, index);
        this.value = value;
    }

    /**
     * Returns the memory address involved in the event.
     */
    public final MemoryAddr getAddr() {
        return addr;
    }

    /**
     * Returns the value read or written in the event.
     */
    public final long getValue() {
        return value;
    }

    @Override
    public final String toString() {
        return String.format("(%s, E%s, T%s, L%s, %s, %s)", type, GID, TID, ID, addr,
                Long.toHexString(value));
    }

}
