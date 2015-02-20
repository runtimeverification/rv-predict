package com.runtimeverification.rvpredict.log;

import java.util.concurrent.BlockingQueue;

import com.runtimeverification.rvpredict.util.LinkedBlockingQueueCopy;

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

    private boolean isWritingEvent = false;
    private long lastGID = 0;

    public BufferedEventPipe() {
        this.pipe = new LinkedBlockingQueueCopy<>();
    }

    /**
     * Puts an event in the pipe input buffer. When the buffer limit
     * {@link BufferedEventPipe#BUFFER_SIZE} is reached,
     * the buffer is made available for reading through a blocking queue.
     *
     * @param event  the event to be sent through the pipe
     * @throws InterruptedException
     */
    @Override
    public void writeEvent(EventItem event) throws InterruptedException {
        try {
            if (isWritingEvent) {
                throw new RuntimeException("This method is not supposed to be reentrant!");
            }
            isWritingEvent = true;

            assert lastGID < event.GID : "Events from the same thread are logged out of order!";
            lastGID = event.GID;

            inBuffer[inIndex++] = event;
            if (inIndex == BUFFER_SIZE) {
                flush();
            }
        } finally {
            isWritingEvent = false;
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
         * (1) only non-null events are written into the buffer in {@link LoggingEngine#saveEvent(EventType, int, long, long, long)}
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
     * Flushes the pipe input buffer through the pipe.
     * <p>
     * This method needs to be synchronized because it can be called by the
     * cleanup thread from {@link #close()} as well.
     * @throws InterruptedException
     */
    private synchronized void flush() throws InterruptedException {
        if (inIndex != 0) {
            pipe.put(inBuffer);
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
