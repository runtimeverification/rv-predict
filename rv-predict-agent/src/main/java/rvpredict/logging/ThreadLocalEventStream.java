package rvpredict.logging;

import rvpredict.db.EventItem;
import rvpredict.db.EventOutputStream;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * to {@link rvpredict.db.EventOutputStream} in a given directory.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {

    private BlockingQueue<BlockingQueue<List<EventItem>>> registry;
    private final List<EventOutputStream> streams = new LinkedList<>();

    public ThreadLocalEventStream(BlockingQueue<BlockingQueue<List<EventItem>>> registry) {
        super();
        this.registry = registry;
    }

    @Override
    protected EventOutputStream initialValue() {
        BlockingQueue<List<EventItem>> threadQueue = new LinkedBlockingQueue<>();
        registry.add(threadQueue);
        final EventOutputStream stream = new EventOutputStream(threadQueue);
        streams.add(stream);
        return stream;
   }

   /**
     * Accessor to the map of streams indexed by thread identifier
     * @return  a map containing all thread-local streams as values indexed by thread id.
     */
    public List<EventOutputStream> getStreams() {
        return streams;
    }
}
