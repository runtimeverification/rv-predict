package com.runtimeverification.rvpredict.log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.util.juc.Executors;

public class EventDisruptor {

    private static final int RING_BUFFER_SIZE = 1024;

    private final Disruptor<EventItem> disruptor;

    private boolean isPublishing;

    @SuppressWarnings("unchecked")
    public static EventDisruptor create(LoggingFactory loggingFactory) {
        /* create the executor to be used by disruptor */
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Logger-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        });

        EventOutputStream outputStream = null;
        try {
            outputStream = loggingFactory.createEventOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        EventWriter eventWriter = new EventWriter(outputStream);

        Disruptor<EventItem> disruptor = new Disruptor<>(EventItem.FACTORY, RING_BUFFER_SIZE,
                executor, ProducerType.SINGLE, new SleepingWaitStrategy());
        disruptor.handleEventsWith(eventWriter);
        disruptor.start();

        return new EventDisruptor(disruptor);
    }

    private EventDisruptor(Disruptor<EventItem> disruptor) {
        this.disruptor = disruptor;
    }

    public void publishEvent(long gid, long tid, int locId,
            int addrl, int addrr, long value, EventType eventType) {
        if (isPublishing) {
            throw new RuntimeException("This method is not supposed to be reentrant!");
        }
        isPublishing = true;

        RingBuffer<EventItem> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            EventItem event = ringBuffer.get(sequence);
            event.GID = gid;
            event.TID = tid;
            event.ID  = locId;
            event.ADDRL = addrl;
            event.ADDRR = addrr;
            event.VALUE = value;
            event.TYPE  = eventType;
        } finally {
            ringBuffer.publish(sequence);
            isPublishing = false;
        }
    }

    public void shutdown() {
        /* signal the EventWriter to close the output stream */
        publishEvent(-1, 0, 0, 0, 0, 0, null);
        disruptor.shutdown();
    }

}
