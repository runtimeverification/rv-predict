package rvpredict.logging;

import rvpredict.db.EventItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A pipe for passing {@link rvpredict.db.EventItem}s from one thread to another.
 *
 * @author TraianSF
 *
 */
public class EventPipe {
    private static final int BUFFER_SIZE=10;
    static final EventItem[] END_BUFFER = new EventItem[1];

    private final BlockingQueue<EventItem[]> queue;
    private EventItem[] inBuffer = new EventItem[BUFFER_SIZE+1];
    int inIndex = 0;
    private EventItem[] outBuffer = null;
    int outIndex = 0;

    public EventPipe() {
        this.queue = new LinkedBlockingQueue<>();
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
     * a new buffer is requested from the queue.
     *
     * @return the event read through the pipe or null if the pipe was closed.
     */
    public EventItem readEvent() {
        if (outBuffer == null) {
            try {
                outBuffer = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (outBuffer == END_BUFFER) return null;
        }
        EventItem event = outBuffer[outIndex++];
        assert event != null : "Each buffer should at least contain one element";
        if (outBuffer[outIndex] == null) {
            outBuffer = null;
            outIndex = 0;
        }
        return event;
    }

    /**
     * Flushes the pipe input buffer through the pipe
     */
    public void flush() {
        try {
            if (inIndex != 0) {
                inBuffer[inIndex] = null;
                queue.put(inBuffer);
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
            queue.put(END_BUFFER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
