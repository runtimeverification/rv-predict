package rvpredict.trace;

/**
 * Interface for trace events.
 *
 */
public interface Event {

    public long getGID();

    public long getTID();

    public int getID();

    public EventType getType();

}
