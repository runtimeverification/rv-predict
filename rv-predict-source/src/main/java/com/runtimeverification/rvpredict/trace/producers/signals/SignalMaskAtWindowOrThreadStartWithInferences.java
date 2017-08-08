package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.producers.base.TtidsForCurrentWindow;

import java.util.Map;
import java.util.Set;

public class SignalMaskAtWindowOrThreadStartWithInferences extends SignalMaskAtWindowStart {
    private final SignalMaskAtWindowStart signalMaskAtWindowStart;
    private final SignalEnabledAtStartInferenceTransitiveClosure signalEnabledAtStartInferenceTransitiveClosure;
    private final TtidsForCurrentWindow ttidsForCurrentWindow;

    public SignalMaskAtWindowOrThreadStartWithInferences(
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart> signalMaskAtWindowStart,
            ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure>
                    signalEnabledAtStartInferenceTransitiveClosure,
            ComputingProducerWrapper<TtidsForCurrentWindow> ttidsForCurrentWindow) {
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
            this.signalMasks.put(ttid, mask);
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
}
