package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TtidsToSignalEnabling extends ComputingProducer<TtidsToSignalEnabling.State> {
    private final SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart;

    protected static class State implements ProducerState {
        private final Map<Long, Set<Integer>> signalToTtidWhereEnabledAtStart = new HashMap<>();
        private final Map<Long, Set<Integer>> signalToTtidWhereDisabledAtStart = new HashMap<>();

        @Override
        public void reset() {
            signalToTtidWhereEnabledAtStart.clear();
            signalToTtidWhereDisabledAtStart.clear();
        }
    }

    public TtidsToSignalEnabling(
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart<? extends ProducerState>>
                    signalMaskAtWindowStart) {
        super(new State());
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
    }

    @Override
    protected void compute() {
        signalMaskAtWindowStart.getSignalMasks().forEach((ttid, signalMask) -> {
            for (long signalNumber = 0; signalNumber < Constants.SIGNAL_NUMBER_COUNT; signalNumber++) {
                SignalMask.SignalMaskBit maskBit = signalMask.getMaskBit(signalNumber);
                switch (maskBit) {
                    case ENABLED:
                        getState().signalToTtidWhereEnabledAtStart
                                .computeIfAbsent(signalNumber, k -> new HashSet<>()).add(ttid);
                        break;
                    case DISABLED:
                        getState().signalToTtidWhereDisabledAtStart
                                .computeIfAbsent(signalNumber, k -> new HashSet<>()).add(ttid);
                        break;
                    case UNKNOWN:
                        break;
                    default:
                        throw new IllegalStateException("Unknown signal mask bit: " + maskBit);
                }
            }
        });
    }

    public Map<Long, Set<Integer>> getSignalToTtidWhereEnabledAtStart() {
        return getState().signalToTtidWhereEnabledAtStart;
    }

    public Map<Long, Set<Integer>> getSignalToTtidWhereDisabledAtStart() {
        return getState().signalToTtidWhereDisabledAtStart;
    }
}
