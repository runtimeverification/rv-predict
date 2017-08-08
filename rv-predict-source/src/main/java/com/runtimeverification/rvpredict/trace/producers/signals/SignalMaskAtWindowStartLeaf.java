package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.Map;

public class SignalMaskAtWindowStartLeaf extends LeafProducer<Map<Integer, SignalMask>> {
    Map<Integer, SignalMask> getMasks() {
        return get();
    }
}
