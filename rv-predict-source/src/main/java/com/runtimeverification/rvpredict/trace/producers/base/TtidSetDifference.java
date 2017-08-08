package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.HashSet;
import java.util.Set;

public class TtidSetDifference extends ComputingProducer {
    private final TtidSetLeaf ttidsToBeRemoved;
    private final TtidSetLeaf allTtids;

    private final Set<Integer> difference = new HashSet<>();

    public TtidSetDifference(
            ComputingProducerWrapper<TtidSetLeaf> allTtids,
            ComputingProducerWrapper<TtidSetLeaf> ttidsToBeRemoved) {
        this.ttidsToBeRemoved = ttidsToBeRemoved.getAndRegister(this);
        this.allTtids = allTtids.getAndRegister(this);
    }

    @Override
    protected void compute() {
        difference.clear();

        difference.addAll(allTtids.getTtids());
        difference.removeAll(ttidsToBeRemoved.getTtids());
    }

    public boolean contains(int ttid) {
        return difference.contains(ttid);
    }

    public Set<Integer> getTtids() {
        return difference;
    }
}
