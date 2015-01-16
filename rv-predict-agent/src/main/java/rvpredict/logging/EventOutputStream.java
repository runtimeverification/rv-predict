package rvpredict.logging;

import rvpredict.db.EventItem;
import rvpredict.trace.Event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Extension of the DataOutputStream class specialized for {@link rvpredict.db.EventItem}
 * @author TraianSF
 *
 */
public class EventOutputStream {
    private static final int BUFFER_SIZE=1024*1024;
    static final List<EventItem> END_BUFFER = new LinkedList<>();

    private final BlockingQueue<List<EventItem>> queue;
    private List<EventItem> buffer = new ArrayList<>(BUFFER_SIZE);

    /**
     * Creates a new event output stream.
     *
     */
    public EventOutputStream() {
        this.queue = new LinkedBlockingQueue<>();
    }

    public void writeEvent(EventItem event) {
        buffer.add(event);
        if (buffer.size() >= BUFFER_SIZE) {
            try {
                queue.put(buffer);
                buffer = new ArrayList<>(BUFFER_SIZE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<EventItem> take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void flush() {
        try {
            if (!buffer.isEmpty())
                queue.put(buffer);
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
