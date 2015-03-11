package com.runtimeverification.rvpredict.log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.util.juc.Executors;

public class EventDisruptor {

    private static final int RING_BUFFER_SIZE = 1024;

    private static final int BATCH_SIZE = 128;

    private final Disruptor<EventItem> disruptor;

    private long cursor = -1;

    private long claimed = -1;

    private volatile boolean isPublishing;

    private volatile boolean shutdown = false;

    @SuppressWarnings("unchecked")
    public static EventDisruptor create(LoggingFactory loggingFactory) {
        /* create the executor */
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Logger-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        });

        /* create the event handler */
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
        try {
            if (shutdown && gid > 0) {
                return;
            }

            RingBuffer<EventItem> ringBuffer = disruptor.getRingBuffer();
            if (cursor == claimed) {
                if (claimed >= 0) {
                    /* publish the current batch of events */
                    ringBuffer.publish(claimed - BATCH_SIZE + 1, claimed);
                }
                /* claim the slots for the next batch of events */
                claimed = ringBuffer.next(BATCH_SIZE);
            }

            /* commit one more event */
            cursor++;
            EventItem event = ringBuffer.get(cursor);
            event.GID = gid;
            event.TID = tid;
            event.ID  = locId;
            event.ADDRL = addrl;
            event.ADDRR = addrr;
            event.VALUE = value;
            event.TYPE  = eventType;

            /* on shutdown signal */
            if (gid < 0) {
                /* flush the current batch of events and shutdown the disruptor */
                ringBuffer.publish(claimed - BATCH_SIZE + 1, claimed);
                disruptor.shutdown();
            }
        } finally {
            isPublishing = false;
        }
    }

    public void shutdown() {
        shutdown = true;
        while (isPublishing) {
            LockSupport.parkNanos(10000000);
        }
        publishEvent(-1, 0, 0, 0, 0, 0, null);
    }

}
