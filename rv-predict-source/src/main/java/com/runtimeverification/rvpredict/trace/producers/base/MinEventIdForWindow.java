package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.RawTrace;

import java.util.OptionalLong;

public class MinEventIdForWindow extends ComputingProducer {
    private final RawTraces rawTraces;

    private OptionalLong maybeMinId = OptionalLong.empty();

    public MinEventIdForWindow(ComputingProducerWrapper<RawTraces> rawTraces) {
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    public OptionalLong getId() {
        return maybeMinId;
    }

    @Override
    protected void compute() {
        maybeMinId = rawTraces.getTraces().stream()
                .filter(trace -> trace.size() > 0)
                .mapToLong(RawTrace::getMinGID)
                .min();
    }
}
