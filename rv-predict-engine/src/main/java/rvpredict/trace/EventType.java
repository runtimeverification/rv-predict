package rvpredict.trace;

/**
 * Enumeration of all types of events considered during logging and prediction.
 *
 * @author TraianSF
 */
public enum EventType {
    INIT,
    READ,
    WRITE,

    /**
     * Event generated after acquiring an intrinsic lock or write lock.
     */
    LOCK,

    /**
     * Event generated before releasing an intrinsic lock or write lock.
     */
    UNLOCK,

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
    PRE_WAIT,

    /**
     * Event generated after a thread is awakened from {@code Object#wait} by
     * {@code Object#notify()/notifyAll()}.
     */
    WAIT,

    /**
     * Event generated after a thread is awakened from {@code Object#wait}
     * because of {@code Object#notify()/notifyAll()} or timeout.
     */
    WAIT_MAYBE_TIMEOUT,

    /**
     * Event generated after a thread is awakened from {@code Object#wait} by
     * {@code Thread#interrupt()}.
     */
    WAIT_INTERRUPTED,

    /**
     * Event generated before calling {@code Object#notify()}.
     */
    NOTIFY,

    /**
     * Event generated before calling {@code Object#notifyAll()}.
     */
    NOTIFY_ALL,

    /**
     * Event generated before calling {@code Thread#start()}.
     */
    START,

    /**
     * Event generated before calling {@code Thread#join}.
     */
    PRE_JOIN,

    /**
     * Event generated after a thread is awakened from {@code Thread#join} by
     * {@code Thread#interrupt()}.
     */
    JOIN,

    /**
     * Event generated after a thread is awakened from {@code Thread#join}
     * because of the thread to join finishes or timeout.
     */
    JOIN_MAYBE_TIMEOUT,

    /**
     * Event generated after a thread is awakened from {@code Thread#join} by
     * {@code Thread#interrupt()}.
     */
    JOIN_INTERRUPTED,

    /**
     * Serves as a guard of a possible control flow change, which
     * determines the next instruction to execute in a thread.
     */
    BRANCH;

    public static boolean isLock(EventType type) {
        return type == LOCK || type == READ_LOCK;
    }

    public static boolean isUnlock(EventType type) {
        return type == UNLOCK || type == READ_UNLOCK;
    }
}
