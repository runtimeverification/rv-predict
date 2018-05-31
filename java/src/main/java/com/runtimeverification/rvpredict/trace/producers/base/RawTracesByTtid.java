package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.Producer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RawTracesByTtid extends ComputingProducer<RawTracesByTtid.State> {
    private final RawTracesCollection rawTraces;

    protected static class State implements ProducerState {
        private final Map<Integer, RawTrace> ttidToRawTrace = new HashMap<>();

        @Override
        public void reset() {
            ttidToRawTrace.clear();
        }
    }

    public <T extends Producer & RawTracesCollection> RawTracesByTtid(ComputingProducerWrapper<T> rawTraces) {
        super(new State());
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    @Override
    protected void compute() {
        rawTraces.getTraces().forEach(trace -> getState().ttidToRawTrace.put(trace.getThreadInfo().getId(), trace));
    }

    public Optional<RawTrace> getRawTrace(int ttid) {
        return Optional.ofNullable(getState().ttidToRawTrace.get(ttid));
    }
}
