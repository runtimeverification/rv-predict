package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RawTracesByTtid extends ComputingProducer {
    private final RawTraces rawTraces;

    private final Map<Integer, RawTrace> ttidToRawTrace;

    public RawTracesByTtid(ComputingProducerWrapper<RawTraces> rawTraces) {
        this.rawTraces = rawTraces.getAndRegister(this);
        ttidToRawTrace = new HashMap<>();
    }

    @Override
    protected void compute() {
        ttidToRawTrace.clear();
        rawTraces.getTraces().forEach(trace -> ttidToRawTrace.put(trace.getThreadInfo().getId(), trace));
    }

    public Optional<RawTrace> getRawTrace(int ttid) {
        return Optional.ofNullable(ttidToRawTrace.get(ttid));
    }
}
