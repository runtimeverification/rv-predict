package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class EventWithOrder {
    private final ReadonlyEventInterface event;
    private final long orderId;
    EventWithOrder(ReadonlyEventInterface event, long orderId) {
        this.event = event;
        this.orderId = orderId;
    }
    ReadonlyEventInterface getEvent() {
        return event;
    }
    long getOrderId() {
        return orderId;
    }
    @Override
    public String toString() {
        return String.format("(Id:%s Order:%s)", event.getEventId(), orderId);
    }
}
