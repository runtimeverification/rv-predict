package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.util.Constants;

public class LockObject {

    private enum Type {
        Monitor, ReadLock, WriteLock
    };

    private final Type type;

    private final int objectHashCode;

    private final SyncEvent lockEvent;

    public static LockObject create(SyncEvent lockEvent) {
        assert EventType.isLock(lockEvent.getType());

        long lockId = lockEvent.getSyncObject();
        int upper32 = (int)(lockId >> 32);
        int lower32 = (int) lockId;
        Type type;
        if (lockEvent.getType() == EventType.READ_LOCK) {
            assert upper32 == 0;
            type = Type.ReadLock;
        } else {
            if (upper32 == 0) {
                type = Type.WriteLock;
            } else {
                assert upper32 == Constants.MONITOR_C;
                type = Type.Monitor;
            }
        }

        return new LockObject(type, lower32, lockEvent);
    }

    private LockObject(Type type, int objectHashCode, SyncEvent lockEvent) {
        this.type = type;
        this.objectHashCode = objectHashCode;
        this.lockEvent = lockEvent;
    }

    public SyncEvent getLockEvent() {
        return lockEvent;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 17 + objectHashCode;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LockObject) {
            LockObject otherLockObj = (LockObject) object;
            return type == otherLockObj.type && objectHashCode == otherLockObj.objectHashCode;
        }
        return false;
    }

    @Override
    public String toString() {
        return type.toString() + "@" + Integer.toHexString(objectHashCode);
    }

}
