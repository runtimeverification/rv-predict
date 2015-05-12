package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.trace.MemoryAddr;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class Event implements Comparable<Event> {
    private long GID;
    private long TID;
    private int ID;
    private int ADDRL;
    private int ADDRR;
    private long VALUE;
    private EventType TYPE;

    private static final int SIZEOF_LONG = 8;
    private static final int SIZEOF_INT = 4;
    private static final int SIZEOF_EVENT_TYPE = 1;

    /**
     * constant representing the size of the event item on disk (no. of bytes).
     * This should be updated whenever structure of the class is changed.
     */
    public static final int SIZEOF
            = SIZEOF_LONG       //GID
            + SIZEOF_LONG       //TID
            + SIZEOF_INT        //ID
            + SIZEOF_INT        //ADDRL
            + SIZEOF_INT        //ADDRR
            + SIZEOF_LONG       //VALUE
            + SIZEOF_EVENT_TYPE //TYPE
            ;

    public Event() { }

    /**
     * @param gid global identifier / primary key of the event
     * @param tid thread identifier primary key
     * @param id statement location identifier
     * @param addrl object identifier
     * @param addrr index (for arrays)
     * @param value value for events carrying a value
     * @param type type of event
     */
    public Event(long gid, long tid, int id, int addrl, int addrr, long value, EventType type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = id;
        this.ADDRL = addrl;
        this.ADDRR = addrr;
        this.VALUE = value;
        this.TYPE = type;
    }

    public long getGID() {
        return GID;
    }

    public void setGID(long gid) {
        GID = gid;
    }

    public long getTID() {
        return TID;
    }

    public void setTID(long tid) {
        TID = tid;
    }

    public int getLocId() {
        return ID;
    }

    public void setLocId(int locId) {
        ID = locId;
    }

    public long getValue() {
        return VALUE;
    }

    public void setValue(long value) {
        VALUE = value;
    }

    public EventType getType() {
        return TYPE;
    }

    public void setType(EventType type) {
        TYPE = type;
    }

    public void setAddrl(int addrl) {
        ADDRL = addrl;
    }

    public void setAddrr(int addrr) {
        ADDRR = addrr;
    }

    public MemoryAddr getAddr() {
        assert isReadOrWrite();
        return new MemoryAddr(ADDRL, ADDRR);
    }

    public long getSyncObject() {
        assert getType().isSyncType();
        return (long)ADDRL << 32 | ADDRR & 0xFFFFFFFFL;
    }

    public long getLockId() {
        assert isLock() || isUnlock();
        return getSyncObject();
    }

    public boolean isRead() {
        return TYPE == EventType.READ;
    }

    public boolean isWrite() {
        return TYPE == EventType.WRITE;
    }

    public boolean isReadOrWrite() {
        return isRead() || isWrite();
    }

    public boolean isStart() {
        return TYPE == EventType.START;
    }

    public boolean isJoin() {
        return TYPE == EventType.JOIN;
    }

    /**
     * Returns {@code true} if this event has type {@link EventType#WRITE_LOCK}
     * or {@link EventType#READ_LOCK}; otherwise, {@code false}.
     */
    public boolean isLock() {
        return TYPE == EventType.READ_LOCK || TYPE == EventType.WRITE_LOCK;
    }

    /**
     * Returns {@code true} if this event has type
     * {@link EventType#WRITE_UNLOCK} or {@link EventType#READ_UNLOCK};
     * otherwise, {@code false}.
     */
    public boolean isUnlock() {
        return TYPE == EventType.READ_UNLOCK || TYPE == EventType.WRITE_UNLOCK;
    }

    public boolean isSyncEvent() {
        return TYPE.isSyncType();
    }

    public boolean isMetaEvent() {
        return TYPE.isMetaType();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Event) {
            return GID == ((Event) object).GID;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Event e) {
        return Long.compare(getGID(), e.getGID());
    }

    @Override
    public int hashCode() {
        return (int) (GID % Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        if (isReadOrWrite()) {
            return String.format("(%s, E%s, T%s, L%s, %s, %s)", TYPE, GID, TID, ID, getAddr(),
                    Long.toHexString(VALUE));
        } else if (isSyncEvent()) {
            return String.format("(%s, E%s, T%s, L%s, %s)", TYPE, GID, TID, ID,
                    Long.toHexString(getSyncObject()));
        } else if (isMetaEvent()) {
            return String.format("(%s, E%s, T%s, L%s)", TYPE, GID, TID, ID);
        } else {
            return "UNKNOWN EVENT";
        }
    }

    public Event copy() {
        return new Event(GID, TID, ID, ADDRL, ADDRR, VALUE, TYPE);
    }

}
