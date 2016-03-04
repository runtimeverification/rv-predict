package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.util.Constants;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class Event implements Comparable<Event> {
    private long GID;
    private long TID;
    private int ID;
    private long ADDR;
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
    public Event(long gid, long tid, int id, long addr, long value, EventType type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = id;
        this.ADDR = addr;
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

    public void setAddr(long addr) {
        ADDR = addr;
    }

    public long getAddr() {
        assert isReadOrWrite();
        return ADDR;
    }

    public int getObjectHashCode() {
        return (int) (getAddr() >> 32);
    }

    public int getFieldIdOrArrayIndex() {
        return (int) getAddr();
    }

    public long getSyncObject() {
        assert getType().isSyncType();
        return ADDR;
    }

    public long getSyncedThreadId() {
        assert isStart() || isJoin();
        return ADDR;
    }

    public long getLockId() {
        assert isPreLock() || isLock() ||  isUnlock();
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
     * Returns {@code true} if this event has type {@link EventType#WRITE_LOCK},
     * {@link EventType#READ_LOCK}, or {@link EventType#WAIT_ACQ}; otherwise,
     * {@code false}.
     */
    public boolean isLock() {
        return TYPE == EventType.READ_LOCK || TYPE == EventType.WRITE_LOCK
                || TYPE == EventType.WAIT_ACQ;
    }

    public boolean isPreLock() {
        return TYPE == EventType.PRE_LOCK;
    }

    public boolean isReadLock() {
        return TYPE == EventType.READ_LOCK;
    }

    public boolean isWriteLock() {
        return TYPE == EventType.WRITE_LOCK;
    }

    public boolean isWaitAcq() {
        return TYPE == EventType.WAIT_ACQ;
    }

    /**
     * Returns {@code true} if this event has type
     * {@link EventType#WRITE_UNLOCK}, {@link EventType#READ_UNLOCK}, or
     * {@link EventType#WAIT_REL}; otherwise, {@code false}.
     */
    public boolean isUnlock() {
        return TYPE == EventType.READ_UNLOCK || TYPE == EventType.WRITE_UNLOCK
                || TYPE == EventType.WAIT_REL;
    }

    public boolean isReadUnlock() {
        return TYPE == EventType.READ_UNLOCK;
    }

    public boolean isWriteUnlock() {
        return TYPE == EventType.WRITE_UNLOCK;
    }

    public boolean isWaitRel() {
        return TYPE == EventType.WAIT_REL;
    }

    public boolean isSyncEvent() {
        return TYPE.isSyncType();
    }

    public boolean isMetaEvent() {
        return TYPE.isMetaType();
    }

    public boolean isFork() {
        return TYPE == EventType.FORK;
    }

    public boolean isCallStackEvent() {
        return TYPE == EventType.INVOKE_METHOD || TYPE == EventType.FINISH_METHOD;
    }

    public boolean isInvokeMethod() {
        return TYPE == EventType.INVOKE_METHOD;
    }

    public boolean isSimilarTo(Event event) {
        return TYPE == event.TYPE && ID == event.ID && ADDR == event.ADDR && VALUE == event.VALUE;
    }

    public String getLockRepresentation() {
        assert isPreLock() || isLock();
        long lockId = getLockId();
        int upper32 = (int)(lockId >> 32);
        String lower32 = Integer.toHexString((int) lockId);
        if (getType() == EventType.READ_LOCK) {
            assert upper32 == 0;
            return "ReadLock@" + lower32;
        } else {
            switch (upper32) {
                case Constants.MONITOR_C:
                    return "Monitor@" + lower32;
                case Constants.ATOMIC_LOCK_C:
                    return "AtomicLock@" + lower32;
                default:
                    assert upper32 == 0;
                    return "WriteLock@" + lower32;
            }
        }
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
        int result = Long.compare(getGID(), e.getGID());
        if (result == 0) {
            // YilongL: dirty hack to deal with the imprecise GID of call stack event
            if (isCallStackEvent()) {
                return e.isCallStackEvent() ? 0 : -1;
            } else {
                return e.isCallStackEvent() ? 1 : 0;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return (int) (GID % Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        if (isReadOrWrite()) {
            int addrl = (int) (ADDR >> 32);
            int addrr = getFieldIdOrArrayIndex();
            String addr = addrr < 0 ?
                    Integer.toHexString(addrl) + "." + -addrr :
                    Integer.toHexString(addrl) + "[" + addrr + "]";
            return String.format("(%s, E%s, T%s, L%s, %s, %s)", TYPE, GID, TID, ID, addr,
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
        return new Event(GID, TID, ID, ADDR, VALUE, TYPE);
    }
}
