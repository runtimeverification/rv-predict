package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.Set;

public class TtidSetLeaf extends LeafProducer<Set<Integer>> {
    public boolean contains(int ttid) {
        return get().contains(ttid);
    }

    public Set<Integer> getTtids() {
        return get();
    }
}
