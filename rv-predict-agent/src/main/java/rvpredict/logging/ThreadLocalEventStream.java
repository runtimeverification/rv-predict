package rvpredict.logging;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * to {@link EventOutputStream} in a given directory.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {

    static final EventOutputStream END_REGISTRY = new EventOutputStream();
    private BlockingQueue<EventOutputStream> registry;

    public ThreadLocalEventStream(BlockingQueue<EventOutputStream> registry) {
        super();
        this.registry = registry;
    }

    @Override
    protected synchronized EventOutputStream initialValue() {
        final EventOutputStream stream = new EventOutputStream();
        registry.add(stream);
        return stream;
   }

    public synchronized void close() {
        registry.add(END_REGISTRY);
    }
}
