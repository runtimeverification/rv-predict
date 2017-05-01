package com.runtimeverification.rvpredict.log;

public class ReadonlyEventDecorator implements ReadonlyEventInterface {
    private final ReadonlyEventInterface event;

    protected ReadonlyEventDecorator(ReadonlyEventInterface event) {
        this.event = event;
    }

    @Override
    public long getEventId() {
        return event.getEventId();
    }

    @Override
    public long getThreadId() {
        return event.getThreadId();
    }

    @Override
    public int getSignalDepth() {
        return event.getSignalDepth();
    }

    @Override
    public int getLocationId() {
        return event.getLocationId();
    }

    @Override
    public long getDataValue() {
        return event.getDataValue();
    }

    @Override
    public EventType getType() {
        return event.getType();
    }

    @Override
    public long getDataAddress() {
        return event.getDataAddress();
    }

    @Override
    public long getSyncObject() {
        return event.getSyncObject();
    }

    @Override
    public long getSyncedThreadId() {
        return event.getSyncedThreadId();
    }

    @Override
    public long unsafeGetAddress() {
        return event.unsafeGetAddress();
    }

    @Override
    public long unsafeGetDataValue() {
        return event.unsafeGetDataValue();
    }

    @Override
    public String getLockRepresentation() {
        return event.getLockRepresentation();
    }

    @Override
    public ReadonlyEventInterface copy() {
        return event.copy();
    }

    @Override
    public ReadonlyEventInterface destructiveWithLocationId(int locationId) {
        return new ReadonlyEventDecorator(this) {
            @Override
            public int getLocationId() {
                return locationId;
            }
        };
    }

    @Override
    public ReadonlyEventInterface destructiveWithEventId(long eventId) {
        return new ReadonlyEventDecorator(this) {
            @Override
            public long getEventId() {
                return eventId;
            }
        };
    }

    @Override
    public boolean isRead() {
        return event.isRead();
    }

    @Override
    public boolean isWrite() {
        return event.isWrite();
    }

    @Override
    public boolean isReadOrWrite() {
        return event.isReadOrWrite();
    }

    @Override
    public boolean isStart() {
        return event.isStart();
    }

    @Override
    public boolean isJoin() {
        return event.isJoin();
    }

    @Override
    public boolean isLock() {
        return event.isLock();
    }

    @Override
    public boolean isUnlock() {
        return event.isUnlock();
    }

    @Override
    public boolean isPreLock() {
        return event.isPreLock();
    }

    @Override
    public boolean isReadLock() {
        return event.isReadLock();
    }

    @Override
    public boolean isWriteLock() {
        return event.isWriteLock();
    }

    @Override
    public boolean isWaitAcq() {
        return event.isWaitAcq();
    }

    @Override
    public boolean isReadUnlock() {
        return event.isWaitAcq();
    }

    @Override
    public boolean isWriteUnlock() {
        return event.isWriteUnlock();
    }

    @Override
    public boolean isWaitRel() {
        return event.isWaitRel();
    }

    @Override
    public boolean isSyncEvent() {
        return event.isSyncEvent();
    }

    @Override
    public boolean isMetaEvent() {
        return event.isMetaEvent();
    }

    @Override
    public boolean isCallStackEvent() {
        return event.isCallStackEvent();
    }

    @Override
    public boolean isInvokeMethod() {
        return event.isInvokeMethod();
    }

    @Override
    public int getObjectHashCode() {
        return event.getObjectHashCode();
    }

    @Override
    public int getFieldIdOrArrayIndex() {
        return event.getFieldIdOrArrayIndex();
    }

    @Override
    public long getLockId() {
        return event.getLockId();
    }

    @Override
    public boolean isSimilarTo(ReadonlyEventInterface event) {
        return this.event.isSimilarTo(event);
    }

    @Override
    public int compareTo(ReadonlyEventInterface event) {
        return this.event.compareTo(event);
    }
}
