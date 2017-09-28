package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;

import java.util.Map;
import java.util.Optional;

public abstract class SignalMaskAtWindowStart<T extends ProducerState> extends ComputingProducer<T> {

    protected SignalMaskAtWindowStart(T state) {
        super(state);
    }

    public Optional<SignalMask> getMask(int ttid) {
        return Optional.ofNullable(getSignalMasks().get(ttid));
    }
    public abstract Map<Integer, SignalMask> getSignalMasks();
}
