package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;

import java.util.HashMap;
import java.util.Map;

public class SignalMaskAtWindowStartWithoutInferrences
        extends SignalMaskAtWindowStart<SignalMaskAtWindowStartWithoutInferrences.State> {
    private final SignalMaskAtWindowStartLeaf signalMaskAtWindowStartLeaf;

    protected static class State implements ProducerState {
        private final Map<Integer, SignalMask> signalMasks = new HashMap<>();

        @Override
        public void reset() {
            signalMasks.clear();
        }
    }

    public SignalMaskAtWindowStartWithoutInferrences(
            ComputingProducerWrapper<SignalMaskAtWindowStartLeaf> signalMaskAtWindowStartLeaf) {
        super(new State());
        this.signalMaskAtWindowStartLeaf = signalMaskAtWindowStartLeaf.getAndRegister(this);
    }

    @Override
    protected void compute() {
        getState().signalMasks.putAll(signalMaskAtWindowStartLeaf.getMasks());
    }

    @Override
    public Map<Integer, SignalMask> getSignalMasks() {
        return getState().signalMasks;
    }
}
