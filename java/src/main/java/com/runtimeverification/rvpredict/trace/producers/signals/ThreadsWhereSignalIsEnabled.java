package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.Producer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSet;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ThreadsWhereSignalIsEnabled extends ComputingProducer<ThreadsWhereSignalIsEnabled.State> {
    private final SignalMaskForEvents signalMaskForEvents;
    private final TtidSet allTtids;
    private final RawTracesByTtid rawTracesByTtid;
    private final SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart;

    public <T extends Producer & TtidSet> ThreadsWhereSignalIsEnabled(
            ComputingProducerWrapper<SignalMaskForEvents> signalMaskForEvents,
            ComputingProducerWrapper<T> allTtids,
            ComputingProducerWrapper<RawTracesByTtid> rawTracesByTtid,
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart<? extends ProducerState>>
                    signalMaskAtWindowStart) {
        super(new State());
        this.signalMaskForEvents = signalMaskForEvents.getAndRegister(this);
        this.allTtids = allTtids.getAndRegister(this);
        this.rawTracesByTtid = rawTracesByTtid.getAndRegister(this);
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
    }

    @Override
    protected void compute() {
        for (long i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            final long signalNumber = i;
            Set<Integer> enabledThreads = new HashSet<>();
            allTtids.getTtids().forEach(ttid -> {
                Optional<RawTrace> maybeTrace = rawTracesByTtid.getRawTrace(ttid);
                if (!maybeTrace.isPresent() || maybeTrace.get().size() == 0) {
                    Optional<SignalMask> mask = signalMaskAtWindowStart.getMask(ttid);
                    if (mask.isPresent() && mask.get().getMaskBit(signalNumber) == SignalMask.SignalMaskBit.ENABLED) {
                        // TODO: I should exclude these when merging previous window signals because it's likely that
                        // the signal will not have any place to actually interrupt the thread (i.e. it's likely that
                        // the thread is already interrupted, and will be so until the end of the window).
                        enabledThreads.add(ttid);
                    }
                    return;
                }
                RawTrace trace = maybeTrace.get();
                for (int j = 0; j < trace.size(); j++) {
                    long eventId = trace.event(j).getEventId();
                    if (signalMaskForEvents.getSignalMaskBeforeEvent(ttid, eventId).getMaskBit(signalNumber)
                            == SignalMask.SignalMaskBit.ENABLED) {
                        enabledThreads.add(ttid);
                        break;
                    }
                }
                long lastEventId = trace.event(trace.size() - 1).getEventId();
                if (signalMaskForEvents.getSignalMaskAfterEvent(ttid, lastEventId).getMaskBit(signalNumber)
                        == SignalMask.SignalMaskBit.ENABLED) {
                    enabledThreads.add(ttid);
                }
            });
            getState().signalNumberToEnabledThreads.put(signalNumber, ImmutableList.copyOf(enabledThreads));
        }
    }

    public List<Integer> threadsForSignal(long signalNumber) {
        return getState().signalNumberToEnabledThreads.getOrDefault(signalNumber, ImmutableList.of());
    }

    protected static class State implements ProducerState {
        Map<Long, List<Integer>> signalNumberToEnabledThreads = new HashMap<>();

        @Override
        public void reset() {
            signalNumberToEnabledThreads.clear();
        }
    }
}
