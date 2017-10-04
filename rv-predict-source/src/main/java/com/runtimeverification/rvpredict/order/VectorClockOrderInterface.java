package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public interface VectorClockOrderInterface {
    /**
     * Incorporates the given event into the ordering.
     *
     * Note: Assumes events are sent in increasing order of their gid.
     *
     * @param event      the event being handled
     * @return           the current {@see VectorClock} for the thread upon processing the event
     */
    VectorClock updateVectorClockForEvent(ReadonlyEventInterface event);
}
