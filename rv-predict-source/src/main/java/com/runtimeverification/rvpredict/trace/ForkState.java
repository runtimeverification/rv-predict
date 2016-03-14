package com.runtimeverification.rvpredict.trace;

/**
 * Encapsulates all the necessary information when a fork is made
 * @author EricPts
 */
public class ForkState {

    public TraceState getCrntState() {
        return crntState;
    }

    public long getFromIndex() {
        return fromIndex;
    }

    private final TraceState crntState;

    private long fromIndex;

    public ForkState(TraceState parentState, long parentIndex) {
        this.crntState = parentState.makeCopy();
        this.fromIndex = parentIndex;
    }
}
