package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.io.Closeable;
import java.io.IOException;

/**
 * A wrapper over an {@see IEventReader} which annotates events with vector clocks given a
 * {@see VectorClockOrderInterface} ordering.
 *
 * @author TraianSF
 */
public class VectorClockTraceReader implements Closeable {
    private final IEventReader reader;
    private final VectorClockOrderInterface order;
    private ReadonlyOrderedEvent lastEvent;

    public VectorClockTraceReader(
            IEventReader reader, VectorClockOrderInterface order) {
        this.reader = reader;
        this.order = order;
    }

    public ReadonlyOrderedEvent readEvent() throws IOException {
        ReadonlyEventInterface event = reader.readEvent();
        lastEvent = new ReadonlyOrderedEvent(event, order.updateVectorClockForEvent(event));
        return lastEvent;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
