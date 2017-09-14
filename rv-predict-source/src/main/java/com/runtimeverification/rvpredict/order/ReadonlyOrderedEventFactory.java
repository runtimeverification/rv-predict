package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public interface ReadonlyOrderedEventFactory {
    public ReadonlyOrderedEventInterface create(ReadonlyEventInterface event, VectorClock timestamp);
}
