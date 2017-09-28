package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.producers.base.TtidsForCurrentWindow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SignalMaskAtWindowOrThreadStartWithInferences
        extends SignalMaskAtWindowStart<SignalMaskAtWindowOrThreadStartWithInferences.State> {
    private final SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart;
    private final SignalEnabledAtStartInferenceTransitiveClosure signalEnabledAtStartInferenceTransitiveClosure;
    private final TtidsForCurrentWindow ttidsForCurrentWindow;

    protected static class State implements ProducerState {
        private final Map<Integer, SignalMask> signalMasks = new HashMap<>();

        @Override
        public void reset() {
            signalMasks.clear();
        }
    }

    public SignalMaskAtWindowOrThreadStartWithInferences(
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart<? extends ProducerState>>
                    signalMaskAtWindowStart,
            ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure>
                    signalEnabledAtStartInferenceTransitiveClosure,
            ComputingProducerWrapper<TtidsForCurrentWindow> ttidsForCurrentWindow) {
        super(new State());
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
        this.signalEnabledAtStartInferenceTransitiveClosure =
                signalEnabledAtStartInferenceTransitiveClosure.getAndRegister(this);
        this.ttidsForCurrentWindow = ttidsForCurrentWindow.getAndRegister(this);
    }

    @Override
    protected void compute() {
        ttidsForCurrentWindow.getTtids().forEach(ttid -> {
            SignalMask mask = signalMaskAtWindowStart.getMask(ttid).orElse(SignalMask.UNKNOWN_MASK);
            mask = setSignalMaskBits(
                    ttid, mask,
                    signalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart(),
                    true);
            mask = setSignalMaskBits(
                    ttid, mask,
                    signalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart(),
                    false);
            getState().signalMasks.put(ttid, mask);
        });
    }

    private SignalMask setSignalMaskBits(
            int ttid, SignalMask mask, Map<Long, Set<Integer>> signalNumberToTtids, boolean enable) {
        for (Map.Entry<Long, Set<Integer>> entry : signalNumberToTtids.entrySet()) {
            if (!entry.getValue().contains(ttid)) {
                continue;
            }
            long signalNumber = entry.getKey();
            SignalMask.SignalMaskBit maskBit = mask.getMaskBit(signalNumber);
            if (maskBit == SignalMask.SignalMaskBit.UNKNOWN) {
                mask = enable ? mask.enable(signalNumber) : mask.disable(signalNumber);
                continue;
            }
            assert maskBit == (enable ? SignalMask.SignalMaskBit.ENABLED : SignalMask.SignalMaskBit.DISABLED);
        }
        return mask;
    }

    @Override
    public Map<Integer, SignalMask> getSignalMasks() {
        return getState().signalMasks;
    }
}
