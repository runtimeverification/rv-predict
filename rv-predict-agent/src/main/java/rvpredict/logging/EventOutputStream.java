package rvpredict.logging;

import rvpredict.db.EventItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Extension of the DataOutputStream class specialized for {@link rvpredict.db.EventItem}
 * @author TraianSF
 *
 */
public class EventOutputStream {
    private static final int BUFFER_SIZE=1024*1024;
    static final EventItem[] END_BUFFER = new EventItem[1];

    private final BlockingQueue<EventItem[]> queue;
    private EventItem[] buffer = new EventItem[BUFFER_SIZE+1];
    int index = 0;

    /**
     * Creates a new event output stream.
     *
     */
    public EventOutputStream() {
        this.queue = new LinkedBlockingQueue<>();
    }

    public void writeEvent(EventItem event) {
        buffer[index++] = event;
        if (index == BUFFER_SIZE) {
            flush();
        }
    }

    public EventItem[] take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void flush() {
        try {
            if (index != 0) {
                buffer[index] = null;
                queue.put(buffer);
                buffer = new EventItem[BUFFER_SIZE+1];
                index = 0;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            flush();
            queue.put(END_BUFFER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
