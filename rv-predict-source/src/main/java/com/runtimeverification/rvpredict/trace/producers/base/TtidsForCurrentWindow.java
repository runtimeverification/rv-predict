package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.Collection;

public class TtidsForCurrentWindow extends LeafProducer<Collection<Integer>> {
    public Collection<Integer> getTtids() {
        return get();
    }
}
