package rvpredict.trace;

/**
 * A memory address involved in {@link InitOrAccessEvent} representing either an
 * object field or an array slot.
 *
 * @author YilongL
 *
 */
public class MemoryAddr {

    private final int objectHashCode;
    private final int fieldIdOrArrayIndex;

    public MemoryAddr(int objectHashCode, int index) {
        this.objectHashCode = objectHashCode;
        this.fieldIdOrArrayIndex = index;
    }

    public int objectHashCode() {
        return objectHashCode;
    }

    /**
     * Gets the field identifier or array index involved in the event.
     *
     * @return a negative integer representing field identifier or a
     *         non-negative integer representing array index
     */
    public int fieldIdOrArrayIndex() {
        return fieldIdOrArrayIndex;
    }

    @Override
    public int hashCode() {
        return objectHashCode ^ fieldIdOrArrayIndex;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MemoryAddr) {
            MemoryAddr addr = (MemoryAddr) object;
            return objectHashCode == addr.objectHashCode
                    && fieldIdOrArrayIndex == addr.fieldIdOrArrayIndex;
        }
        return false;
    }

    @Override
    public String toString() {
        return fieldIdOrArrayIndex < 0 ?
            Integer.toHexString(objectHashCode) + "." + -fieldIdOrArrayIndex :
            Integer.toHexString(objectHashCode) + "[" + fieldIdOrArrayIndex + "]";
    }
}