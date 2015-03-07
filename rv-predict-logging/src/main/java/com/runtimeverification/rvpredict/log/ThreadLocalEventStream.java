package com.runtimeverification.rvpredict.log;

import java.util.concurrent.BlockingQueue;

import com.runtimeverification.rvpredict.util.juc.LinkedBlockingQueue;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * of events.  It associates an {@link EventPipe}
 * to each thread.  Current implementation adds these to a registry used by the
 * {@link LoggingServer} thread to associate a
 * {@link Logger} to each of them for saving their contents
 * to disk.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventPipe> {

    static final EventPipe END_REGISTRY = new BufferedEventPipe();
    private final LoggingFactory loggingFactory;
    private final BlockingQueue<EventPipe> registry;

    public ThreadLocalEventStream(LoggingFactory loggingFactory) {
        super();
        this.loggingFactory = loggingFactory;
        this.registry = new LinkedBlockingQueue<>();
    }

    public EventPipe takeEventPipe() throws InterruptedException {
        return registry.take();
    }

    @Override
    protected EventPipe initialValue() {
        EventPipe pipe = loggingFactory.createEventPipe();
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