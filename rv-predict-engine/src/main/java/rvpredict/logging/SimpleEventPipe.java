package rvpredict.logging;

import rvpredict.db.EventItem;
import rvpredict.trace.EventType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A buffer based implementation of the EventPipe interface.
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
        EventItem event = pipe.remove();
        if (event == END_EVENT)
            return null;
        return event;
    }

    @Override
    public void close() throws InterruptedException {

    }
}
