package rvpredict.logging;

import rvpredict.db.EventItem;

/**
 * A pipe for passing {@link rvpredict.db.EventItem}s from one thread to another.
 * The class is meant for a single-producer-single-consumer design with an
 * instance object of this class being created by the
 * {@link rvpredict.logging.ThreadLocalEventStream} for each logged thread
 * and being written to only by that thread through the
 * {@link rvpredict.logging.LoggingEngine#saveEvent(rvpredict.trace.EventType, int, long, int, long)},
 * while being only read from a {@link rvpredict.logging.LoggerThread} object
 * created for this purpose.
 *
 * The {@link BufferedEventPipe#close()} method, which flushes the buffers and send the
 * {@link BufferedEventPipe#END_BUFFER} marker to close the pipe, is called only as part of the
 * {@link rvpredict.engine.main.Main.CleanupAgent#cleanup()} method added as a shutdown hook
 * to the logging process.
 *
 * @author TraianSF
 */
public interface EventPipe {
    /**
     * Puts an event in the pipe input buffer. When the buffer limit
     * 
     * @param event  the event to be sent through the pipe
     */
    void writeEvent(EventItem event);

    /**
     * Reads an event from the pipe output buffer.
     *
     * @return the event read through the pipe or <code>null</code> if the pipe was closed.
     */
     EventItem readEvent() throws InterruptedException;

    /**
     * Closes the pipe.  All further writes to it would be discarded.
     * *
     * @throws InterruptedException
     */
    void close() throws InterruptedException;
}
