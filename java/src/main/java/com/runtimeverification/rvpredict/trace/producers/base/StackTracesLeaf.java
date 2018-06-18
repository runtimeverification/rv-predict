package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.Deque;
import java.util.Map;

public class StackTracesLeaf extends LeafProducer<Map<Integer, Deque<ReadonlyEventInterface>>> {
    Map<Integer, Deque<ReadonlyEventInterface>> getStackTraces() {
        return get();
    }
}
