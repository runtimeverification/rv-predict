package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class LockState {

    private final long lockId;

    private Event readLock;
    private Event writeLock;

    private int readLockLevel = 0;
    private int writeLockLevel = 0;

    public LockState(long lockId) {
        this.lockId = lockId;
    }

    public long lockId() {
        return lockId;
    }

    public Event lock() {
        if (writeLockLevel > 0) {
            return writeLock;
        } else if (readLockLevel > 0) {
            return readLock;
        } else {
            return null;
        }
    }

    public boolean isAcquired() {
        return readLockLevel > 0 || writeLockLevel > 0;
    }

    public int readLockLevel() {
        return readLockLevel;
    }

    public int writeLockLevel() {
        return writeLockLevel;
    }

    public void acquire(Event lock) {
        if (lock.isReadLock()) {
            if (readLockLevel++ == 0) {
                this.readLock = lock;
            }
        } else if (lock.isWriteLock()) {
            if (writeLockLevel++ == 0) {
                this.writeLock = lock;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void release(Event unlock) {
        if (unlock.isReadUnlock()) {
            if (--readLockLevel < 0) {
                throw new IllegalStateException("Lock entrance level cannot be less than 0!");
            }
        } else if (unlock.isWriteUnlock()) {
            if (--writeLockLevel < 0) {
                throw new IllegalStateException("Lock entrance level cannot be less than 0!");
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public LockState copy() {
        LockState copy = new LockState(lockId);
        copy.readLock = readLock;
        copy.writeLock = writeLock;
        copy.readLockLevel = readLockLevel;
        copy.writeLockLevel = writeLockLevel;
        return copy;
    }
}