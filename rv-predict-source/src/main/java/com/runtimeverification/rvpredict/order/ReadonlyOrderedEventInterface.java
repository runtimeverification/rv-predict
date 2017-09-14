package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public interface ReadonlyOrderedEventInterface extends ReadonlyEventInterface {
    public VectorClock getVectorClock();
}
