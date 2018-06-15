package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.Producer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.RawTrace;

import java.util.OptionalLong;

public class MinEventIdForWindow extends ComputingProducer<MinEventIdForWindow.State> {
    private final RawTracesCollection rawTraces;

    protected static class State implements ProducerState {
        private OptionalLong maybeMinId = OptionalLong.empty();

        @Override
        public void reset() {
            maybeMinId = OptionalLong.empty();
        }
    }

    public <T extends Producer & RawTracesCollection> MinEventIdForWindow(ComputingProducerWrapper<T> rawTraces) {
        super(new State());
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    public OptionalLong getId() {
        return getState().maybeMinId;
    }


    @Override
    protected void compute() {
        getState().maybeMinId = rawTraces.getTraces().stream()
                .filter(trace -> trace.size() > 0)
                .mapToLong(RawTrace::getMinGID)
                .min();
    }
}
