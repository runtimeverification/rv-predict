package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.signals.Signals;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.producers.base.OtidToMainTtid;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.SortedTtidsWithParentFirst;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public class SignalMaskForEvents extends ComputingProducer<SignalMaskForEvents.State> {
    private final RawTracesByTtid rawTracesByTtid;
    private final SortedTtidsWithParentFirst sortedTtidsWithParentFirst;
    private final SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart;
    private final OtidToMainTtid otidToMainTtid;
    private final InterruptedEvents interruptedEvents;
    private final ThreadInfosComponent threadInfosComponent;

    // TODO(virgil): The right thing to do may be to remove the ttidToStartMask map and add the start SignalMask in the
    // ThreadInfo object. This way it would be obvious to everyone where to find this kind of data, if needed.
    // However, to fill the map with each window's data, one needs to do much of the same computation as
    // ttidToMaskAfterEvent, and doing this in a clean way is not easy.
    //
    // One option would be to duplicate the computation for ttidToMaskAfterEvent, but that is inefficient and
    // it would be harder to follow what is computed and where.
    //
    // Another option would be to create the ttidToMaskAfterEvent map when creating the ThreadInfo objects and
    // attach it somehow to the RawTrace objects. But then it would not be clear where the raw trace preprocessing
    // belongs.
    protected static class State implements ProducerState {
        private final Map<Integer, SignalMask> ttidToStartMask = new HashMap<>();
        private final Map<Integer, List<MaskChangeEvent>> ttidToMaskAfterEvent = new HashMap<>();

        @Override
        public void reset() {
            ttidToMaskAfterEvent.clear();
            // As explained in the comment above, the ttidToStartMask map should not live here. However, it does, for
            // now, so it is not cleared.
        }
    }

    public SignalMaskForEvents(
            ComputingProducerWrapper<RawTracesByTtid> rawTracesByTtid,
            ComputingProducerWrapper<SortedTtidsWithParentFirst> sortedTtidsWithParentFirst,
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart<? extends ProducerState>>
                    signalMaskAtWindowStart,
            ComputingProducerWrapper<OtidToMainTtid> otidToMainTtid,
            ComputingProducerWrapper<InterruptedEvents> interruptedEvents,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent) {
        super(new State());
        this.rawTracesByTtid = rawTracesByTtid.getAndRegister(this);
        this.sortedTtidsWithParentFirst = sortedTtidsWithParentFirst.getAndRegister(this);
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
        this.otidToMainTtid = otidToMainTtid.getAndRegister(this);
        this.interruptedEvents = interruptedEvents.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
    }

    @Override
    public void compute() {
        for (int ttid : sortedTtidsWithParentFirst.getTtids()) {
            SignalMask threadMask = getStartSignalOrThreadMask(ttid);
            SignalMask threadMaskForLambda = threadMask;
            List<MaskChangeEvent> maskAfterEvent =
                    getState().ttidToMaskAfterEvent.computeIfAbsent(ttid, k -> initialMaskList(threadMaskForLambda));
            Optional<RawTrace> maybeTrace = rawTracesByTtid.getRawTrace(ttid);
            if (!maybeTrace.isPresent()) {
                continue;
            }
            RawTrace trace = maybeTrace.get();
            for (int i = 0; i < trace.size(); i++) {
                ReadonlyEventInterface event = trace.event(i);
                if (event.isStart()) {
                    OptionalInt maybeTtid = otidToMainTtid.getTtid(event.getSyncedThreadId());
                    assert maybeTtid.isPresent();
                    getState().ttidToStartMask.put(maybeTtid.getAsInt(), threadMask);
                } else if (event.getType() == EventType.ENTER_SIGNAL) {
                    getState().ttidToStartMask.put(ttid, threadMask);
                }
                Optional<SignalMask> changedMask = Signals.changedSignalMaskAfterEvent(event, threadMask);
                if (changedMask.isPresent()) {
                    threadMask = changedMask.get();
                    maskAfterEvent.add(new MaskChangeEvent(threadMask, event.getEventId()));
                }
            }
        }
    }

    public SignalMask getSignalMaskBeforeEvent(int ttid, long eventId) {
        Optional<List<MaskChangeEvent>> maybeMaskAfterEventList =
                Optional.ofNullable(getState().ttidToMaskAfterEvent.get(ttid));
        if (!maybeMaskAfterEventList.isPresent()) {
            throw new IllegalArgumentException("Could not find any mask for thread " + ttid);
        }
        List<MaskChangeEvent> maskAfterEventList = maybeMaskAfterEventList.get();
        // The maskAfterEventList list is assumed to be small.
        Optional<SignalMask> lastMask = Optional.empty();
        for (MaskChangeEvent maskChangeEvent : maskAfterEventList) {
            OptionalLong maskChangeId = maskChangeEvent.getEventId();
            if (maskChangeId.isPresent() && maskChangeId.getAsLong() >= eventId) {
                break;
            }
            lastMask = Optional.of(maskChangeEvent.getSignalMask());
        }
        if (!lastMask.isPresent()) {
            throw new IllegalArgumentException("Could not find mask for thread " + ttid + " and event " + eventId);
        }
        return lastMask.get();
    }

    public SignalMask getSignalMaskAfterEvent(int interruptedTtid, long eventId) {
        return getSignalMaskBeforeEvent(interruptedTtid, eventId + 1);
    }

    public Map<Integer, SignalMask> extractTtidToLastEventMap() {
        return getState().ttidToMaskAfterEvent.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get(entry.getValue().size() - 1).getSignalMask()));
    }

    private List<MaskChangeEvent> initialMaskList(SignalMask initialSignalMask) {
        List<MaskChangeEvent> initialMask = new ArrayList<>();
        initialMask.add(new MaskChangeEvent(initialSignalMask));
        return initialMask;
    }

    private SignalMask getStartSignalOrThreadMask(int ttid) {
        return signalMaskAtWindowStart.getMask(ttid)
                .orElseGet(() -> getStartEventMask(ttid)
                        .orElseGet(() -> getInterruptedThreadMaskOrUnknown(ttid)));
    }

    private SignalMask getInterruptedThreadMaskOrUnknown(int interruptingTtid) {
        if (threadInfosComponent.getThreadType(interruptingTtid) == ThreadType.THREAD) {
            return SignalMask.UNKNOWN_MASK;
        }
        OptionalLong interruptedEventId = interruptedEvents.getInterruptedEventId(interruptingTtid);
        int interruptedTtid = interruptedEvents.getInterruptedTtid(interruptingTtid);
        if (!interruptedEventId.isPresent()) {
            return signalMaskAtWindowStart.getMask(interruptedTtid)
                    .map(SignalMask::enabledToUnknown)
                    .orElse(SignalMask.UNKNOWN_MASK);
        }
        return getSignalMaskAfterEvent(interruptedTtid, interruptedEventId.getAsLong()).enabledToUnknown();
    }

    private Optional<SignalMask> getStartEventMask(int ttid) {
        return Optional.ofNullable(getState().ttidToStartMask.get(ttid));
    }

    private static class MaskChangeEvent {
        private final SignalMask signalMask;
        private final OptionalLong eventId;

        private MaskChangeEvent(SignalMask signalMask, long eventId) {
            this.signalMask = signalMask;
            this.eventId = OptionalLong.of(eventId);
        }

        private MaskChangeEvent(SignalMask signalMask) {
            this.signalMask = signalMask;
            this.eventId = OptionalLong.empty();
        }

        public OptionalLong getEventId() {
            return eventId;
        }

        public SignalMask getSignalMask() {
            return signalMask;
        }
    }
}
