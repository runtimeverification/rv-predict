package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

public class SignalMaskAtWindowStartWithoutInferrences extends SignalMaskAtWindowStart {
    private final SignalMaskAtWindowStartLeaf signalMaskAtWindowStartLeaf;

    public SignalMaskAtWindowStartWithoutInferrences(
            ComputingProducerWrapper<SignalMaskAtWindowStartLeaf> signalMaskAtWindowStartLeaf) {
        this.signalMaskAtWindowStartLeaf = signalMaskAtWindowStartLeaf.getAndRegister(this);
    }

    @Override
    protected void compute() {
        signalMasks.clear();
        signalMasks.putAll(signalMaskAtWindowStartLeaf.getMasks());
    }
}
