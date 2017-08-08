package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class SignalMaskAtWindowStart extends ComputingProducer {
    protected final Map<Integer, SignalMask> signalMasks = new HashMap<>();

    public Optional<SignalMask> getMask(int ttid) {
        return Optional.ofNullable(signalMasks.get(ttid));
    }
    public Map<Integer, SignalMask> getSignalMasks() {
        return signalMasks;
    }
}
