package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.List;

public class RawTraces extends LeafProducer<List<RawTrace>> {
    public List<RawTrace> getTraces() {
        return get();
    }
}
