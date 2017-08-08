package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.Lists;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.producers.base.SortedTtidsWithParentFirst;
import com.runtimeverification.rvpredict.trace.producers.base.StartAndJoinEventsForWindow;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

public class SignalEnabledAtStartInferenceTransitiveClosure extends ComputingProducer {
    private final SignalEnabledAtStartInferenceFromReads signalEnabledAtStartInferenceFromReads;
    private final SignalEnabledAtStartInferenceFromInterruptions signalEnabledAtStartInferenceFromInterruptions;
    private final SortedTtidsWithParentFirst sortedTtidsWithParentFirst;
    private final ThreadInfosComponent threadInfosComponent;
    private final InterruptedEvents interruptedEvents;
    private final SignalMaskForEvents signalMaskForEvents;
    private final SignalMaskAtWindowStart signalMaskAtWindowStart;
    private final StartAndJoinEventsForWindow startAndJoinEventsForWindow;

    private final Map<Long, Set<Integer>> signalToTtidWhereEnabledAtStart = new HashMap<>();
    private final Map<Long, Set<Integer>> signalToTtidWhereDisabledAtStart = new HashMap<>();

    public SignalEnabledAtStartInferenceTransitiveClosure(
            ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> signalEnabledAtStartInferenceFromReads,
            ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> signalEnabledAtStartInferenceFromInterruptions,
            ComputingProducerWrapper<SortedTtidsWithParentFirst> sortedTtidsWithParentFirst,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent,
            ComputingProducerWrapper<InterruptedEvents> interruptedEvents,
            ComputingProducerWrapper<SignalMaskForEvents> signalMaskForEvents,
            ComputingProducerWrapper<SignalMaskAtWindowStart> signalMaskAtWindowStart,
            ComputingProducerWrapper<StartAndJoinEventsForWindow> startAndJoinEventsForWindow) {
        this.signalEnabledAtStartInferenceFromReads =
                signalEnabledAtStartInferenceFromReads.getAndRegister(this);
        this.signalEnabledAtStartInferenceFromInterruptions =
                signalEnabledAtStartInferenceFromInterruptions.getAndRegister(this);
        this.sortedTtidsWithParentFirst = sortedTtidsWithParentFirst.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
        this.interruptedEvents = interruptedEvents.getAndRegister(this);
        this.signalMaskForEvents = signalMaskForEvents.getAndRegister(this);
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
        this.startAndJoinEventsForWindow = startAndJoinEventsForWindow.getAndRegister(this);
    }

    @Override
    protected void compute() {
        signalToTtidWhereEnabledAtStart.clear();
        signalToTtidWhereDisabledAtStart.clear();

        mergeToFormer(
                signalToTtidWhereEnabledAtStart,
                signalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart());
        mergeToFormer(
                signalToTtidWhereEnabledAtStart,
                signalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart());
        mergeToFormer(
                signalToTtidWhereDisabledAtStart,
                signalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart());
        transitiveInferrences(signalToTtidWhereEnabledAtStart, SignalMask.SignalMaskBit.ENABLED);
        transitiveInferrences(signalToTtidWhereDisabledAtStart, SignalMask.SignalMaskBit.DISABLED);
    }

    private void transitiveInferrences(
            Map<Long, Set<Integer>> signalToTtids, SignalMask.SignalMaskBit expectedMaskBit) {
        signalToTtids.forEach((signalNumber, ttids) -> {
            for (int ttid : Lists.reverse(sortedTtidsWithParentFirst.getTtids())) {
                if (!ttids.contains(ttid)) {
                    continue;
                }
                ThreadType threadType = threadInfosComponent.getThreadType(ttid);
                OptionalLong maybeInterruptedEventId;
                int parentTtid;
                switch (threadType) {
                    case THREAD:
                        OptionalInt maybeParentTtid = threadInfosComponent.getParentThread(ttid);
                        if (!maybeParentTtid.isPresent()) {
                            continue;
                        }
                        parentTtid = maybeParentTtid.getAsInt();
                        Optional<ReadonlyEventInterface> maybeStartEvent =
                                startAndJoinEventsForWindow.getStartEvent(ttid);
                        if (!maybeStartEvent.isPresent()) {
                            continue;
                        }
                        maybeInterruptedEventId = OptionalLong.of(maybeStartEvent.get().getEventId());
                        break;
                    case SIGNAL:
                        if (expectedMaskBit == SignalMask.SignalMaskBit.DISABLED) {
                            // Can't infer anything about the interrupted thread, it may have been disabled by the
                            // establish signal mask.
                            continue;
                        }
                        parentTtid = interruptedEvents.getInterruptedTtid(ttid);
                        maybeInterruptedEventId = interruptedEvents.getInterruptedEventId(ttid);
                        break;
                    default:
                        throw new IllegalStateException("Unknown thread type: " + threadType);
                }
                SignalMask signalMask;
                if (maybeInterruptedEventId.isPresent()) {
                    signalMask = signalMaskForEvents.getSignalMaskBeforeEvent(
                            parentTtid, maybeInterruptedEventId.getAsLong());
                } else {
                    signalMask = signalMaskAtWindowStart.getMask(parentTtid).orElse(SignalMask.UNKNOWN_MASK);
                }
                SignalMask.SignalMaskBit maskBit = signalMask.getMaskBit(signalNumber);
                switch (maskBit) {
                    case ENABLED:
                    case DISABLED:
                        assert maskBit == expectedMaskBit;
                        break;
                    case UNKNOWN:
                        // TODO(virgil): We should always know the enable bit for a normal thread, but this
                        // is not true in tests.
                        // assert threadInfosComponent.getThreadType(parentTtid) == ThreadType.SIGNAL;
                        ttids.add(parentTtid);
                        break;
                    default:
                        throw new IllegalStateException("Unknown mask bit type: " + maskBit);
                }
            }
        });
    }

    private void mergeToFormer(Map<Long, Set<Integer>> former, Map<Long, Set<Integer>> latter) {
        latter.forEach((signalNumber, ttids) ->
                former.computeIfAbsent(signalNumber, k -> new HashSet<>()).addAll(ttids));
    }

    Map<Long, Set<Integer>> getSignalToTtidWhereEnabledAtStart() {
        return signalToTtidWhereEnabledAtStart;
    }

    Map<Long, Set<Integer>> getSignalToTtidWhereDisabledAtStart() {
        return signalToTtidWhereDisabledAtStart;
    }
}
