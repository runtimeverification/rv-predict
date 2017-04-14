package com.runtimeverification.rvpredict.log;

public abstract class ReadonlyEvent implements Comparable<ReadonlyEvent> {
    public abstract long getEventId();
    public abstract long getThreadId();
    public abstract int getLocationId();
    public abstract long getDataValue();
    public abstract EventType getType();
    public abstract long getDataAddress();
    public abstract long getSyncObject();
    public abstract long getSyncedThreadId();
    protected abstract long unsafeGetAddress();
    protected abstract long unsafeGetDataValue();

    public abstract String getLockRepresentation();
    public abstract ReadonlyEvent copy();
    public abstract ReadonlyEvent destructiveWithLocationId(int locationId);

    public boolean isRead() {
        return getType() == EventType.READ;
    }

    public boolean isWrite() {
        return getType() == EventType.WRITE;
    }

    public boolean isReadOrWrite() {
        return isRead() || isWrite();
    }

    public boolean isStart() {
        return getType() == EventType.START_THREAD;
    }

    public boolean isJoin() {
        return getType() == EventType.JOIN_THREAD;
    }

    /**
     * Returns {@code true} if this event has type {@link EventType#WRITE_LOCK},
     * {@link EventType#READ_LOCK}, or {@link EventType#WAIT_ACQUIRE}; otherwise,
     * {@code false}.
     */
    public boolean isLock() {
        return getType() == EventType.READ_LOCK || getType() == EventType.WRITE_LOCK
                || getType() == EventType.WAIT_ACQUIRE;
    }
    /**
     * Returns {@code true} if this event has type
     * {@link EventType#WRITE_UNLOCK}, {@link EventType#READ_UNLOCK}, or
     * {@link EventType#WAIT_RELEASE}; otherwise, {@code false}.
     */
    public boolean isUnlock() {
        return getType() == EventType.READ_UNLOCK || getType() == EventType.WRITE_UNLOCK
                || getType() == EventType.WAIT_RELEASE;
    }

    public boolean isPreLock() {
        return getType() == EventType.PRE_LOCK;
    }

    public boolean isReadLock() {
        return getType() == EventType.READ_LOCK;
    }

    public boolean isWriteLock() {
        return getType() == EventType.WRITE_LOCK;
    }

    public boolean isWaitAcq() {
        return getType() == EventType.WAIT_ACQUIRE;
    }


    public boolean isReadUnlock() {
        return getType() == EventType.READ_UNLOCK;
    }

    public boolean isWriteUnlock() {
        return getType() == EventType.WRITE_UNLOCK;
    }

    public boolean isWaitRel() {
        return getType() == EventType.WAIT_RELEASE;
    }

    public boolean isSyncEvent() {
        return getType().isSyncType();
    }

    public boolean isMetaEvent() {
        return getType().isMetaType();
    }

    public boolean isCallStackEvent() {
        return getType() == EventType.INVOKE_METHOD || getType() == EventType.FINISH_METHOD;
    }

    public boolean isInvokeMethod() {
        return getType() == EventType.INVOKE_METHOD;
    }

    public int getObjectHashCode() {
        return (int) (getDataAddress() >> 32);
    }

    public int getFieldIdOrArrayIndex() {
        return (int) getDataAddress();
    }

    public long getLockId() {
        assert isPreLock() || isLock() ||  isUnlock();
        return getSyncObject();
    }

    public boolean isSimilarTo(ReadonlyEvent event) {
        return getType() == event.getType() && getLocationId() == event.getLocationId()
                && unsafeGetAddress() == event.unsafeGetAddress() && unsafeGetDataValue() == event.unsafeGetDataValue();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ReadonlyEvent) {
            return getEventId() == ((ReadonlyEvent) object).getEventId();
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ReadonlyEvent e) {
        int result = Long.compare(getEventId(), e.getEventId());
        if (result == 0) {
            // YilongL: dirty hack to deal with the imprecise eventId of call stack event
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
        return (int) (getEventId() % Integer.MAX_VALUE);
    }
}
