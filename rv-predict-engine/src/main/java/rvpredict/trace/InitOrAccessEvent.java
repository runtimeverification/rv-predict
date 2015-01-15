package rvpredict.trace;

public abstract class InitOrAccessEvent extends AbstractEvent {

    protected final long value;
    protected final long objectHashCode;
    protected final long index;

    protected InitOrAccessEvent(long GID, long TID, int ID, EventType type, long objectHashCode,
            long index, long value) {
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

    public final int getIndex() {
        // TODO(YilongL): index should be an integer instead of long
        // in fact, objecthashcode should also be an integer!!!
        return (int) index;
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
