package com.runtimeverification.rvpredict.log;

import java.io.IOException;

import com.lmax.disruptor.EventHandler;

/**
 *
 * @author YilongL
 */
public class EventWriter implements EventHandler<EventItem> {

    private final EventOutputStream outputStream;

    private boolean closed = false;

    public EventWriter(EventOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void onEvent(EventItem eventItem, long sequence, boolean endOfBatch)
            throws IOException {
        if (!closed) {
            if (eventItem.GID > 0) {
                outputStream.writeEvent(eventItem);
            } else {
                outputStream.close();
                closed = true;
            }
        }
    }

}
