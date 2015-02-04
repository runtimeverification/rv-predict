package rvpredict.trace;

/**
 * Interface for trace events.
 *
 */
public interface Event {

    /**
     * Returns the global event ID which represents its ordinal in the trace.
     */
    long getGID();

    /**
     * Returns the thread ID of the event.
     */
    long getTID();

    /**
     * Returns the program location ID of the event.
     */
    int getID();

    /**
     * Returns the event type.
     */
    EventType getType();

    /**
     * Returns {@code true} if this event has type {@link EventType#WRITE_LOCK},
     * {@link EventType#READ_LOCK}, or {@link EventType#WAIT_ACQ}; otherwise,
     * {@code false}.
     */
    boolean isLockEvent();

    /**
     * Returns {@code true} if this event has type
     * {@link EventType#WRITE_UNLOCK}, {@link EventType#READ_UNLOCK}, or
     * {@link EventType#WAIT_REL}; otherwise, {@code false}.
     */
    boolean isUnlockEvent();

}
