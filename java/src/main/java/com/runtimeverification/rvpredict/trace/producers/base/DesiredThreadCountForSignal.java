package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducer;

public class DesiredThreadCountForSignal extends LeafProducer<Integer> {
    public int getCount() {
        return get();
    }
}
