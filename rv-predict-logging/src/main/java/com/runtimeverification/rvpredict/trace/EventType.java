package com.runtimeverification.rvpredict.trace;

/**
 * Enumeration of all types of events considered during logging and prediction.
 *
 * @author TraianSF
 */
public enum EventType {
    READ,
    WRITE,

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
     * Event generated after entering the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_ENTER,

    /**
     * Event generated right before exiting the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_EXIT,

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
     * Event generated before calling {@code Thread#join}.
     */
    PRE_JOIN,

    /**
     * Event generated after a thread is awakened from {@code Thread#join()}
     * because the joining thread finishes.
     */
    JOIN,

    /**
     * Event generated after a thread is awakened from some version of
     * {@code Thread#join} even when the joining thread is not finished.
     */
    JOIN_MAYBE_FAILED,

    /**
     * Serves as a guard of a possible control flow change, which
     * determines the next instruction to execute in a thread.
     */
    BRANCH;

    public static boolean isLock(EventType type) {
        return type == WRITE_LOCK || type == READ_LOCK;
    }

    public static boolean isUnlock(EventType type) {
        return type == WRITE_UNLOCK || type == READ_UNLOCK;
    }
}
