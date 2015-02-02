package rvpredict.logging;

import rvpredict.db.EventItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A buffer based implementation of the EventPipe interface.
 *
 * @author TraianSF
 *
 */
public class BufferedEventPipe implements EventPipe {
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

    public BufferedEventPipe() {
        this.pipe = new LinkedBlockingQueue<>();
    }

    /**
     * Puts an event in the pipe input buffer. When the buffer limit
     * {@link BufferedEventPipe#BUFFER_SIZE} is reached,
     * the buffer is made available for reading through a blocking queue.
     *
     * @param event  the event to be sent through the pipe
     */
    @Override
    public void writeEvent(EventItem event) {
        inBuffer[inIndex++] = event;
        if (inIndex == BUFFER_SIZE) {
            flush();
        }
    }

    /**
     * Reads an event from the pipe output buffer.  If the buffer is empty,
     * a new buffer is requested from the blocking queue.
     *
     *
     * @return the event read through the pipe or null if the pipe was closed.
     */
    @Override
    public EventItem readEvent() throws InterruptedException {
        if (outBuffer == null) {
            outBuffer = pipe.take();
            if (outBuffer == END_BUFFER) return null;
        }
        EventItem event = outBuffer[outIndex++];
        assert event != null : "Each buffer should at least contain one element";
        /**
         * The above assertion should always be hold, in normal operating conditions, as
         * (1) only non-null events are written into the buffer in {@link rvpredict.logging.LoggingEngine#saveEvent(rvpredict.trace.EventType, int, long, long, long)}
         * (2) null can be written during {@link BufferedEventPipe#flush()}, but never on an empty buffer.
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
            if (inIndex != 0) {
                inBuffer[inIndex] = null;
                try {
                    pipe.put(inBuffer);
                } catch (InterruptedException e) {
                    System.out.println("Process forcefully ending. All data in current buffer (" + inIndex + " events) lost.");
                }
                inBuffer = new EventItem[BUFFER_SIZE+1];
                inIndex = 0;
            }
    }

    /**
     * Flushes the pipe input buffer and then puts the
     * {@link BufferedEventPipe#BUFFER_SIZE} in the pipe
     * to signify its closing.
     */
    @Override
    public void close() throws InterruptedException {
        flush();
        pipe.put(END_BUFFER);
    }
}
