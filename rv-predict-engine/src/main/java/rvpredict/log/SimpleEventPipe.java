package rvpredict.log;

import rvpredict.trace.EventType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A simple implementation of the {@link EventPipe} interface.
 *
 * @author TraianSF
 */
public class SimpleEventPipe implements EventPipe {
    private final static EventItem END_EVENT = new EventItem(0,0,0,0,0,0, EventType.INIT);
    private final BlockingQueue<EventItem> pipe;

    public SimpleEventPipe() {
        this.pipe = new LinkedBlockingDeque<>();
    }

    @Override
    public void writeEvent(EventItem event) {
        pipe.add(event);
    }

    @Override
    public EventItem readEvent() throws InterruptedException {
        EventItem event = pipe.take();
        if (event == END_EVENT)
            return null;
        return event;
    }

    @Override
    public void close() throws InterruptedException {
        pipe.put(END_EVENT);
    }
}
