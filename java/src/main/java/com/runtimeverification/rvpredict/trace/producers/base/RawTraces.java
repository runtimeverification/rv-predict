package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducer;
import com.runtimeverification.rvpredict.trace.RawTrace;

import java.util.List;

public class RawTraces extends LeafProducer<List<RawTrace>> implements RawTracesCollection {
    public List<RawTrace> getTraces() {
        return get();
    }
}
