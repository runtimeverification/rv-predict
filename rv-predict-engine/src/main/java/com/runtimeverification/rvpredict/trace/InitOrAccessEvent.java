package com.runtimeverification.rvpredict.trace;

public abstract class InitOrAccessEvent extends AbstractEvent {

    protected final long value;
    protected final long objectHashCode;
    protected final int index;

    protected InitOrAccessEvent(long GID, long TID, int ID, EventType type, long objectHashCode,
            int index, long value) {
        super(GID, TID, ID, type);
        this.objectHashCode = objectHashCode;
        this.index = index;
        this.value = value;
    }

    /**
     * Returns {@code String} representation of the memory address involved in
     * the event.
     */
    public final String getAddr() {
        if (index < 0) {
            return Long.toHexString(objectHashCode) + "." + -index;
        } else {
            return Long.toHexString(objectHashCode) + "[" + index + "]";
        }
    }

    /**
     * Gets the field identifier or array index involved in the event.
     *
     * @return a negative integer representing field identifier or a
     *         non-negative integer representing array index
     */
    public final int getFieldIdOrArrayIndex() {
        return index;
    }

    /**
     * Returns the value read or written in the event.
     */
    public final long getValue() {
        return value;
    }

    @Override
    public final String toString() {
        return String.format("(%s, E%s, T%s, L%s, %s, %s)", type, GID, TID, ID, getAddr(),
                Long.toHexString(value));
    }

}
