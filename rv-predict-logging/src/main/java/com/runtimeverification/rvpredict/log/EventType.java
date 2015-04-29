package com.runtimeverification.rvpredict.log;

/**
 * Enumeration of all types of events considered during logging and prediction.
 *
 * @author TraianSF
 */
public enum EventType {
    READ,
    WRITE,

    /**
     * Atomic events that are used only in the front-end.
     */
    ATOMIC_READ,
    ATOMIC_WRITE,
    ATOMIC_READ_THEN_WRITE,

    /**
     * Event generated after acquiring an intrinsic lock or write lock.
     */
    WRITE_LOCK,

    /**
     * Event generated before releasing an intrinsic lock or write lock.
     */
    WRITE_UNLOCK,

    /**
     * Event generated after acquiring a read lock, i.e.,
     * {@code ReadWriteLock#readLock()#lock()}.
     */
    READ_LOCK,

    /**
     * Event generated before releasing a read lock, i.e.,
     * {@code ReadWriteLock#readLock()#unlock()}.
     */
    READ_UNLOCK,

    /**
     * Event generated before calling {@code Object#wait}.
     */
    WAIT_REL,

    /**
     * Event generated after a thread is awakened from {@code Object#wait} for
     * whatever reason (e.g., spurious wakeup, being notified, or being
     * interrupted).
     */
    WAIT_ACQ,

    /**
     * Event generated before calling {@code Thread#start()}.
     */
    START,

    /**
     * Event generated after a thread is awakened from {@code Thread#join()}
     * because the joining thread finishes.
     */
    JOIN,

    /**
     * Event generated after entering the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_ENTER,

    /**
     * Event generated right before exiting the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_EXIT,

    INVOKE_METHOD,

    FINISH_METHOD;

    public boolean isLockType() {
        return this == WRITE_LOCK || this == READ_LOCK;
    }

    public boolean isUnlockType() {
        return this == WRITE_UNLOCK || this == READ_UNLOCK;
    }

    public boolean isSyncType() {
        return WRITE_LOCK.ordinal() <= this.ordinal() && this.ordinal() <= JOIN.ordinal();
    }

    public boolean isMetaType() {
        return CLINIT_ENTER.ordinal() <= this.ordinal() && this.ordinal() <= FINISH_METHOD.ordinal();
    }
}
