package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.producers.base.OtidToSignalDepthToTtidAtWindowStart;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSetDifference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

public class SignalEnabledAtStartInferenceFromInterruptions extends
        ComputingProducer<SignalEnabledAtStartInferenceFromInterruptions.State> {
    private final InterruptedEvents interruptedEvents;
    private final SignalMaskForEvents signalMaskForEvents;
    private final TtidSetDifference unfinishedTtidsAtWindowStart;
    private final ThreadInfosComponent threadInfosComponent;
    private final OtidToSignalDepthToTtidAtWindowStart otidToSignalDepthToTtidAtWindowStart;

    protected static class State implements ProducerState {
        private Map<Long, Set<Integer>> signalToTtidWhereEnabledAtStart = new HashMap<>();

        @Override
        public void reset() {
            signalToTtidWhereEnabledAtStart.clear();
        }

    }

    public SignalEnabledAtStartInferenceFromInterruptions(
            ComputingProducerWrapper<InterruptedEvents> interruptedEvents,
            ComputingProducerWrapper<SignalMaskForEvents> signalMaskForEvents,
            ComputingProducerWrapper<TtidSetDifference> unfinishedTtidsAtWindowStart,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent,
            ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart>
                    otidToSignalDepthToTtidAtWindowStart) {
        super(new State());
        this.interruptedEvents = interruptedEvents.getAndRegister(this);
        this.signalMaskForEvents = signalMaskForEvents.getAndRegister(this);
        this.unfinishedTtidsAtWindowStart = unfinishedTtidsAtWindowStart.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
        this.otidToSignalDepthToTtidAtWindowStart =
                otidToSignalDepthToTtidAtWindowStart.getAndRegister(this);
    }

    @Override
    protected void compute() {
        interruptedEvents.getSignalNumberToTtidToNextMinInterruptedEventId().forEach(
                (signalNumber, ttidToNextMinInterruptedEventId) ->
                        ttidToNextMinInterruptedEventId.forEach((ttid, minInterruptedEventId) -> {
                            SignalMask signalMask =
                                    signalMaskForEvents.getSignalMaskBeforeEvent(ttid, minInterruptedEventId);
                            SignalMask.SignalMaskBit signalMaskBit = signalMask.getMaskBit(signalNumber);
                            switch (signalMaskBit) {
                                case UNKNOWN:
                                    // If we don't know the signal mask bit value at the interruption point then we
                                    // didn't know it when the thread/signal started. However, because of the
                                    // interruption, we can infer that it was enabled at the beginning.
                                    getState().signalToTtidWhereEnabledAtStart
                                            .computeIfAbsent(signalNumber, k -> new HashSet<>())
                                            .add(ttid);
                                    break;
                                case ENABLED:
                                    // We can't learn anything new.
                                    break;
                                case DISABLED:
                                    // We made a broken inference, but we can't reliably infer interruption points
                                    // right now. In the future we should throw an InvalidStateException here or
                                    // something, but for now, we just ignore this.
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown mask bit " + signalMaskBit);
                            }
                        })
        );

        unfinishedTtidsAtWindowStart.getTtids().stream()
                .filter(this::isSignal)
                .forEach(ttid -> {
                    OptionalInt maybeParentTtid = otidToSignalDepthToTtidAtWindowStart.getTtid(
                            threadInfosComponent.getOriginalThreadIdForTraceThreadId(ttid),
                            threadInfosComponent.getSignalDepth(ttid) - 1);
                    if (!maybeParentTtid.isPresent()) {
                        // TODO(virgil): Find a way to handle signals that are running at the beginning of the
                        // current window, but the interrupted signal either ended in the previous window, or
                        // will start in the current window.
                        // assert false;
                        return;
                    }
                    int parentTtid = maybeParentTtid.getAsInt();
                    OptionalLong signalNumber = threadInfosComponent.getSignalNumber(ttid);
                    assert signalNumber.isPresent();
                    // TODO(virgil): Maybe it would be worth checking that it is not already enabled.
                    // The semantics would be cleaner, but it shouldn't make a difference in practice.
                    getState().signalToTtidWhereEnabledAtStart
                            .computeIfAbsent(signalNumber.getAsLong(), k -> new HashSet<>())
                            .add(parentTtid);
                });
    }

    private boolean isSignal(int ttid) {
        return threadInfosComponent.getThreadType(ttid) == ThreadType.SIGNAL;
    }

    Map<Long, Set<Integer>> getSignalToTtidWhereEnabledAtStart() {
        return getState().signalToTtidWhereEnabledAtStart;
    }
}
