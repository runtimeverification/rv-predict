package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;


/**
 * A {@see ReadonlyEventInterface} with an additional {@see VectorClock}
 */
public class ReadonlyOrderedEvent {
    private final ReadonlyEventInterface event;
    private final VectorClock clock;

    public ReadonlyOrderedEvent(ReadonlyEventInterface event, VectorClock clock) {
        this.event = event;
        this.clock = clock;
    }

    public ReadonlyEventInterface getEvent() {
        return event;
    }

    public VectorClock getVectorClock() {
        return clock;
    }

    @Override
    public String toString() {
        return String.format("{ event=%-50s; timestamp=%-20s }",
                event.toString(),
                clock.toString());
    }
}
