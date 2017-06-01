package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.util.Constants;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class Event extends ReadonlyEvent {
    private long eventId;
    private long originalThreadId;
    private long locationId;
    private long address;
    private long dataValue;
    private EventType type;

    private static final int SIZEOF_LONG = 8;
    private static final int SIZEOF_INT = 4;
    private static final int SIZEOF_EVENT_TYPE = 1;

    /**
     * constant representing the size of the event item on disk (no. of bytes).
     * This should be updated whenever structure of the class is changed.
     */
    static final int SIZEOF
            = SIZEOF_LONG       //eventId
            + SIZEOF_LONG       //TID
            + SIZEOF_INT        //locationId
            + SIZEOF_LONG       //address
            + SIZEOF_LONG       //dataValue
            + SIZEOF_EVENT_TYPE //type
            ;

    public Event() { }

    /**
     * @param eventId global identifier / primary key of the event
     * @param tid thread identifier primary key
     * @param locationId statement location identifier
     * @param address object identifier. For read/write events, the higher 32 bits form an object identifier,
     *                the lower 32 bytes form a field or array index identifier.
     * @param dataValue data for read/write events.
     * @param type type of event
     */
    public Event(long eventId, long tid, long locationId, long address, long dataValue, EventType type) {
        this.eventId = eventId;
        this.originalThreadId = tid;
        this.locationId = locationId;
        this.address = address;
        this.dataValue = dataValue;
        this.type = type;
    }

    @Override
    public long getEventId() {
        return eventId;
    }

    public void setEventId(long gid) {
        eventId = gid;
    }

    @Override
    public long getOriginalThreadId() {
        return originalThreadId;
    }

    @Override
    public int getSignalDepth() {
        return 0;
    }

    @Override
    public long getSignalNumber() {
        assert false;
        return Constants.INVALID_SIGNAL;
    }

    @Override
    public long getPartialSignalMask() {
        assert false;
        return 0;
    }

    @Override
    public long getFullWriteSignalMask() {
        assert false;
        return 0;
    }

    @Override
    public long getFullReadSignalMask() {
        assert false;
        return 0;
    }

    @Override
    public long getSignalHandlerAddress() {
        assert false;
        return 0;
    }

    public void setOriginalThreadId(long tid) {
        originalThreadId = tid;
    }

    @Override
    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    @Override
    public long getDataValue() {
        assert isReadOrWrite();
        return dataValue;
    }

    public void setDataValue(long dataValue) {
        this.dataValue = dataValue;
    }

    @Override
    public long unsafeGetDataValue() { return dataValue; };

    @Override
    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public void setAddress(long addr) {
        address = addr;
    }

    @Override
    public long getDataAddress() {
        assert isReadOrWrite();
        return address;
    }

    @Override
    public long unsafeGetAddress() { return  address; }

    @Override
    public long getSyncObject() {
        assert getType().isSyncType();
        return address;
    }

    @Override
    public long getSyncedThreadId() {
        assert isStart() || isJoin();
        return address;
    }

    @Override
    public long getCanonicalFrameAddress() {
        assert false;
        return 0;
    }

    @Override
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
    public String toString() {
        int signalDepth = getSignalDepth();
        if (isReadOrWrite()) {
            int addrl = Math.toIntExact(getObjectHashCode());
            int addrr = getFieldIdOrArrayIndex();
            String addr = addrr < 0 ?
                    Integer.toHexString(addrl) + "." + -addrr :
                    Integer.toHexString(addrl) + "[" + addrr + "]";
            return String.format("(%s, E%s, T%s, D%s, L%s, %s, %s)",
                    type, eventId, originalThreadId, signalDepth, locationId, addr,
                    Long.toHexString(dataValue));
        } else if (isSyncEvent()) {
            return String.format("(%s, E%s, T%s, D%s, L%s, %s)",
                    type, eventId, originalThreadId, signalDepth, locationId,
                    Long.toHexString(getSyncObject()));
        } else if (isMetaEvent()) {
            return String.format("(%s, E%s, T%s, D%s, L%s)",
                    type, eventId, originalThreadId, signalDepth, locationId);
        } else {
            return "UNKNOWN EVENT";
        }
    }

    @Override
    public long getObjectHashCode() {
        return (int) (address >> 32);
    }

    @Override
    public int getFieldIdOrArrayIndex() {
        return (int) address;
    }

    @Override
    public Event copy() {
        return new Event(eventId, originalThreadId, locationId, address, dataValue, type);
    }

    @Override
    public ReadonlyEventInterface destructiveWithLocationId(long locationId) {
        this.locationId = locationId;
        return this;
    }

    @Override
    public ReadonlyEventInterface destructiveWithEventId(long eventId) {
        this.eventId = eventId;
        return this;
    }
}
