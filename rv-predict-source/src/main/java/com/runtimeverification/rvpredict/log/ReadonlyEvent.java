package com.runtimeverification.rvpredict.log;

public abstract class ReadonlyEvent implements ReadonlyEventInterface {
    @Override
    public boolean isRead() {
        return getType() == EventType.READ;
    }

    @Override
    public boolean isWrite() {
        return getType() == EventType.WRITE;
    }

    @Override
    public boolean isReadOrWrite() {
        return isRead() || isWrite();
    }

    @Override
    public boolean isStart() {
        return getType() == EventType.START_THREAD;
    }

    @Override
    public boolean isJoin() {
        return getType() == EventType.JOIN_THREAD;
    }

    @Override
    public boolean isLock() {
        return getType() == EventType.READ_LOCK || getType() == EventType.WRITE_LOCK
                || getType() == EventType.WAIT_ACQUIRE;
    }

    @Override
    public boolean isUnlock() {
        return getType() == EventType.READ_UNLOCK || getType() == EventType.WRITE_UNLOCK
                || getType() == EventType.WAIT_RELEASE;
    }

    @Override
    public boolean isPreLock() {
        return getType() == EventType.PRE_LOCK;
    }

    @Override
    public boolean isReadLock() {
        return getType() == EventType.READ_LOCK;
    }

    @Override
    public boolean isWriteLock() {
        return getType() == EventType.WRITE_LOCK;
    }

    @Override
    public boolean isWaitAcq() {
        return getType() == EventType.WAIT_ACQUIRE;
    }


    @Override
    public boolean isReadUnlock() {
        return getType() == EventType.READ_UNLOCK;
    }

    @Override
    public boolean isWriteUnlock() {
        return getType() == EventType.WRITE_UNLOCK;
    }

    @Override
    public boolean isWaitRel() {
        return getType() == EventType.WAIT_RELEASE;
    }

    @Override
    public boolean isSyncEvent() {
        return getType().isSyncType();
    }

    @Override
    public boolean isSignalEvent() {
        return getType().isSignalType();
    }

    @Override
    public boolean isMetaEvent() {
        return getType().isMetaType();
    }

    @Override
    public boolean isCallStackEvent() {
        return getType() == EventType.INVOKE_METHOD || getType() == EventType.FINISH_METHOD;
    }

    @Override
    public boolean isInvokeMethod() {
        return getType() == EventType.INVOKE_METHOD;
    }

    @Override
    public long getLockId() {
        assert isPreLock() || isLock() ||  isUnlock();
        return getSyncObject();
    }

    @Override
    public boolean isSimilarTo(ReadonlyEventInterface event) {
        return getType() == event.getType() && getLocationId() == event.getLocationId()
                && unsafeGetAddress() == event.unsafeGetAddress() && unsafeGetDataValue() == event.unsafeGetDataValue();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ReadonlyEventInterface) {
            return getEventId() == ((ReadonlyEventInterface) object).getEventId();
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ReadonlyEventInterface e) {
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
