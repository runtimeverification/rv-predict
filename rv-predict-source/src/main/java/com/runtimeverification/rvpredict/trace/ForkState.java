package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;

/**
 * Encapsulates all the necessary information when a fork is made
 * Created by ericpts on 3/10/16.
 */
public class ForkState {
    public LockGraph getLockGraph() {
        return lockGraph;
    }

    public TraceState getCrntState() {
        return crntState;
    }

    public long getFromIndex() {
        return fromIndex;
    }

    private final LockGraph lockGraph;
    private final TraceState crntState;

    private long fromIndex;

    public ForkState(LockGraph parentGraph, TraceState parentState, long parentIndex) {
        this.lockGraph = parentGraph.makeCopy();
        this.crntState = parentState.makeCopy();
        this.fromIndex = parentIndex;
    }
}
