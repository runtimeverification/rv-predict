package rvpredict.trace;

/**
 * Interface for trace events.
 *
 */
public interface Event {

    /**
     * Returns the global event ID which represents its ordinal in the trace.
     */
    public long getGID();

    /**
     * Returns the thread ID of the event.
     */
    public long getTID();

    /**
     * Returns the program location ID of the event.
     */
    public int getID();

    /**
     * Returns the event type.
     */
    public EventType getType();

}
