package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.*;

public class OrderedEvent extends Event implements ReadonlyOrderedEventInterface {
    private final VectorClock timestamp;

    public OrderedEvent(ReadonlyEventInterface event, VectorClock timestamp) {
        super(event.getEventId(),
                event.getOriginalThreadId(),
                event.getLocationId(),
                event.unsafeGetDataInternalIdentifier(),
                event.unsafeGetDataValue(),
                event.getType());
        this.timestamp = new VectorClock(timestamp);
    }

    @Override
    public VectorClock getVectorClock() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("{ event=%-50s; timestamp=%-20s }",
                super.toString(),
                timestamp);
    }
}
