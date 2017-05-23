package com.runtimeverification.rvpredict.log;

import java.util.concurrent.locks.Condition;

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
     * Event generated before calling {@link Object#wait()} or
     * {@link Condition#await()}.
     */
    WAIT_RELEASE,

    /**
     * Event generated after a thread is awakened from {@link Object#wait()} or
     * {@link Condition#await()} for whatever reason (e.g., spurious wakeup,
     * being notified, or being interrupted).
     */
    WAIT_ACQUIRE,

    /**
     * Event generated before calling {@code Thread#start()}.
     */
    START_THREAD,

    /**
     * Event generated after a thread is awakened from {@code Thread#join()}
     * because the joining thread finishes.
     */
    JOIN_THREAD,

    BEGIN_THREAD,
    END_THREAD,

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

    FINISH_METHOD,

    /**
     * Event generated before acquiring of any type of lock is attempted.
     * Required by, and only used for, deadlock detection, where the intention
     * to acquire a lock is more relevant than actually the acquisition itself.
     */
    //TODO(TraianSF): Consider moving this with the other SYNC events
    PRE_LOCK,

    ESTABLISH_SIGNAL,
    DISESTABLISH_SIGNAL,
    WRITE_SIGNAL_MASK,
    READ_SIGNAL_MASK,
    READ_WRITE_SIGNAL_MASK,
    BLOCK_SIGNALS,
    UNBLOCK_SIGNALS,

    ENTER_SIGNAL,
    EXIT_SIGNAL;


    public boolean isSyncType() {
        return WRITE_LOCK.ordinal() <= this.ordinal() && this.ordinal() <= END_THREAD.ordinal() || this == PRE_LOCK;
    }

    public boolean isMetaType() {
        return CLINIT_ENTER.ordinal() <= this.ordinal() && this.ordinal() <= FINISH_METHOD.ordinal();
    }

    public boolean isSignalType() {
        return ESTABLISH_SIGNAL.ordinal() <= this.ordinal() && this.ordinal() <= EXIT_SIGNAL.ordinal();
    }
}
