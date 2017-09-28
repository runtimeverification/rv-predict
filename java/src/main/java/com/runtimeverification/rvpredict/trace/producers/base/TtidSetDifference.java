package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;

import java.util.HashSet;
import java.util.Set;

public class TtidSetDifference extends ComputingProducer<TtidSetDifference.State> {
    private final TtidSetLeaf ttidsToBeRemoved;
    private final TtidSetLeaf allTtids;

    protected static class State implements ProducerState {
        private final Set<Integer> difference = new HashSet<>();

        @Override
        public void reset() {
            difference.clear();
        }
    }

    public TtidSetDifference(
            ComputingProducerWrapper<TtidSetLeaf> allTtids,
            ComputingProducerWrapper<TtidSetLeaf> ttidsToBeRemoved) {
        super(new State());
        this.ttidsToBeRemoved = ttidsToBeRemoved.getAndRegister(this);
        this.allTtids = allTtids.getAndRegister(this);
    }

    @Override
    protected void compute() {
        getState().difference.addAll(allTtids.getTtids());
        getState().difference.removeAll(ttidsToBeRemoved.getTtids());
    }

    public boolean contains(int ttid) {
        return getState().difference.contains(ttid);
    }

    public Set<Integer> getTtids() {
        return getState().difference;
    }
}
