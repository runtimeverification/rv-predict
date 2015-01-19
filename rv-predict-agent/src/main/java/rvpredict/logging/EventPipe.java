package rvpredict.logging;

import rvpredict.db.EventItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A pipe for passing {@link rvpredict.db.EventItem}s from one thread to another.
 * The class is meant for a single-producer-single-consumer design with an
 * instance object of this class being created by the
 * {@link rvpredict.logging.ThreadLocalEventStream} for each logged thread
 * and being written to only by that thread through the
 * {@link rvpredict.logging.LoggingEngine#saveEvent(rvpredict.trace.EventType, int, long, long, long)},
 * while being only read from a {@link rvpredict.logging.LoggerThread} object
 * created for this purpose.
 *
 * The {@link EventPipe#close()} method, which flushes the buffers and send the
 * {@link EventPipe#END_BUFFER} marker to close the pipe, is called only as part of the
 * {@link rvpredict.engine.main.Main.CleanupAgent#cleanup()} method added as a shutdown hook
 * to the logging {@link rvpredict.instrumentation.Agent#premain(String, java.lang.instrument.Instrumentation)} process.
 *
 * @author TraianSF
 *
 */
public class EventPipe {
    private static final int BUFFER_SIZE=10;
    /**
     * constant to indicate that the current pipe is closing.
     */
    private static final EventItem[] END_BUFFER = new EventItem[0];

    private final BlockingQueue<EventItem[]> pipe;
    private EventItem[] inBuffer = new EventItem[BUFFER_SIZE+1];
    private int inIndex = 0;
    private EventItem[] outBuffer = null;
    private int outIndex = 0;

    public EventPipe() {
        this.pipe = new LinkedBlockingQueue<>();
    }

    /**
     * Puts an event in the pipe input buffer. When the buffer limit
     * {@link rvpredict.logging.EventPipe#BUFFER_SIZE} is reached,
     * the buffer is made available for reading through a blocking queue.
     *
     * @param event  the event to be sent through the pipe
     */
    public void writeEvent(EventItem event) {
        inBuffer[inIndex++] = event;
        if (inIndex == BUFFER_SIZE) {
            flush();
        }
    }

    /**
     * Reads an event from the pipe output buffer.  If the buffer is empty,
     * a new buffer is requested from the pipe.
     *
     *
     * @return the event read through the pipe or null if the pipe was closed.
     */
    public EventItem readEvent() {
        if (outBuffer == null) {
            try {
                outBuffer = pipe.take();
            } catch (InterruptedException e) {
                System.err.println("Pipe broken at rvpredict.logging.EventPipe#readEvent()." +
                                "\n\tAssuming end of stream");
                return null;
            }
            if (outBuffer == END_BUFFER) return null;
        }
        EventItem event = outBuffer[outIndex++];
        assert event != null : "Each buffer should at least contain one element";
        /**
         * The above assertion should always be hold, in normal operating conditions, as
         * (1) only non-null events are written into the buffer in {@link rvpredict.logging.LoggingEngine#saveEvent(rvpredict.trace.EventType, int, long, long, long)}
         * (2) null can be written during {@link EventPipe#flush()}, but never on an empty buffer.
         * (3) following code checks for null so next read would not read from the same buffer if so.
         */
        if (outBuffer[outIndex] == null) {
            outBuffer = null;
            outIndex = 0;
        }
        return event;
    }

    /**
     * Flushes the pipe input buffer through the pipe
     */
    private void flush() {
        try {
            if (inIndex != 0) {
                inBuffer[inIndex] = null;
                pipe.put(inBuffer);
                inBuffer = new EventItem[BUFFER_SIZE+1];
                inIndex = 0;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flushes the pipe input buffer and then puts the
     * {@link rvpredict.logging.EventPipe#BUFFER_SIZE} in the pipe
     * to signify its closing.
     */
    public void close() {
        try {
            flush();
            pipe.put(END_BUFFER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
