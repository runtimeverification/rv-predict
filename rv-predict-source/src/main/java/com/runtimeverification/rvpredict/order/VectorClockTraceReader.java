package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.io.IOException;

public class VectorClockTraceReader implements IEventReader {
    private final IEventReader reader;
    private final VectorClockOrderInterface order;
    private final ReadonlyOrderedEventFactory factory;
    private ReadonlyOrderedEventInterface lastEvent;

    public VectorClockTraceReader(IEventReader reader, VectorClockOrderInterface order, ReadonlyOrderedEventFactory factory) {
        this.reader = reader;
        this.order = order;
        this.factory = factory;
    }

    @Override
    public ReadonlyOrderedEventInterface readEvent() throws IOException {
        ReadonlyEventInterface event = reader.readEvent();
        lastEvent = factory.create(event, order.log(event));
        return lastEvent;
    }

    @Override
    public ReadonlyOrderedEventInterface lastReadEvent() {
        return lastEvent;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
