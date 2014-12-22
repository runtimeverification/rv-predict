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
    LOCK,
    UNLOCK,

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
     * Event generated before calling {@code Thread#interrupt()}.
     */
    INTERRUPT,

    /**
     * Serves as a guard of a possible control flow change, which
     * determines the next instruction to execute in a thread.
     */
    BRANCH;
}
