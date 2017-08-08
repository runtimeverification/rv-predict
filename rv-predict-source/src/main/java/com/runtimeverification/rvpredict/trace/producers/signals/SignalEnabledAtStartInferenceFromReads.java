package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.signals.Signals;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.producers.base.RawTraces;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SignalEnabledAtStartInferenceFromReads extends ComputingProducer {
    private final RawTraces rawTraces;
    private final SignalMaskForEvents signalMaskForEvents;

    private final Map<Long, Set<Integer>> signalToTtidWhereEnabledAtStart = new HashMap<>();
    private final Map<Long, Set<Integer>> signalToTtidWhereDisabledAtStart = new HashMap<>();

    public SignalEnabledAtStartInferenceFromReads(
            ComputingProducerWrapper<RawTraces> rawTraces,
            ComputingProducerWrapper<SignalMaskForEvents> signalMaskForEvents) {
        this.rawTraces = rawTraces.getAndRegister(this);
        this.signalMaskForEvents =  signalMaskForEvents.getAndRegister(this);
    }

    @Override
    protected void compute() {
        signalToTtidWhereEnabledAtStart.clear();
        signalToTtidWhereDisabledAtStart.clear();

        rawTraces.getTraces().forEach(rawTrace -> {
            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (!event.isSignalMaskRead()) {
                    continue;
                }
                int ttid = rawTrace.getThreadInfo().getId();
                SignalMask computedMask = signalMaskForEvents.getSignalMaskBeforeEvent(ttid, event.getEventId());
                long mask = event.getFullReadSignalMask();
                for (long signalNumber = 0; signalNumber < Constants.SIGNAL_NUMBER_COUNT; signalNumber++) {
                    SignalMask.SignalMaskBit maskBit = computedMask.getMaskBit(signalNumber);
                    switch (maskBit) {
                        case ENABLED:
                            assert Signals.signalIsEnabled(signalNumber, mask);
                            break;
                        case DISABLED:
                            assert Signals.signalIsDisabledInFullMask(signalNumber, mask);
                            break;
                        case UNKNOWN:
                            if (Signals.signalIsEnabled(signalNumber, mask)) {
                                signalToTtidWhereEnabledAtStart
                                        .computeIfAbsent(signalNumber, k -> new HashSet<>())
                                        .add(ttid);
                            } else {
                                assert Signals.signalIsDisabledInFullMask(signalNumber, mask);
                                signalToTtidWhereDisabledAtStart
                                        .computeIfAbsent(signalNumber, k -> new HashSet<>())
                                        .add(ttid);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown mask bit: " + maskBit);
                    }
                }
            }
        });
    }

    Map<Long, Set<Integer>> getSignalToTtidWhereEnabledAtStart() {
        return signalToTtidWhereEnabledAtStart;
    }

    Map<Long, Set<Integer>> getSignalToTtidWhereDisabledAtStart() {
        return signalToTtidWhereDisabledAtStart;
    }
}
