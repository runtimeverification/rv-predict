package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

/**
 * A {@see ReadonlyEventInterface} with an additional {@see VectorClock}
 */
public interface ReadonlyOrderedEventInterface extends ReadonlyEventInterface {
    VectorClock getVectorClock();
}
