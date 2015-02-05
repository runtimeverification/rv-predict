package rvpredict.logging;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * of events.  It associates an {@link EventPipe}
 * to each thread.  Current implementation adds these to a registry used by the
 * {@link rvpredict.logging.LoggingServer} thread to associate a
 * {@link rvpredict.logging.LoggerThread} to each of them for saving their contents
 * to disk.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventPipe> {

    static final EventPipe END_REGISTRY = new EventPipe();
    private final BlockingQueue<EventPipe> registry;

    private static final ThreadLocalEventStream instance = new ThreadLocalEventStream();

    public static ThreadLocalEventStream getInstance() {
        return instance;
    }

    private ThreadLocalEventStream() {
        super();
        this.registry = new LinkedBlockingQueue<>();
    }

    public EventPipe takeEventPipe() throws InterruptedException {
        return registry.take();
    }

    @Override
    protected EventPipe initialValue() {
        EventPipe pipe = new EventPipe();
        registry.add(pipe);
        return pipe;
   }

    /**
     * Adds the END_REGISTRY marker to the registry to signal end of activity.
     */
    public void close() {
        registry.add(END_REGISTRY);
    }
}
