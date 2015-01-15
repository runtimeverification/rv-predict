package rvpredict.db;

import rvpredict.config.Configuration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Extension of the DataOutputStream class specialized for {@link rvpredict.db.EventItem}
 * @author TraianSF
 *
 */
public class EventOutputStream {
    private static final int BUFFER_SIZE=1024*1024;
    private static final List<EventItem> END_MARKER = new LinkedList<>();

    private final BlockingQueue<List<EventItem>> queue;
    private List<EventItem> buffer = new ArrayList<>(BUFFER_SIZE);

    /**
     * Creates a new event output stream.
     *
     * @param queue
     */
    public EventOutputStream(BlockingQueue<List<EventItem>> queue) {
        this.queue = queue;
    }

    public final void writeEvent(EventItem event) {
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

    public final void flush() {
        try {
            if (!buffer.isEmpty())
                queue.put(buffer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public final void close() {
        try {
            flush();
            queue.put(END_MARKER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
