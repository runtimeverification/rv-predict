package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
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

public class SignalMaskForEvents extends ComputingProducer {
    private final RawTracesByTtid rawTracesByTtid;
    private final SortedTtidsWithParentFirst sortedTtidsWithParentFirst;
    private final SignalMaskAtWindowStart signalMaskAtWindowStart;
    private final OtidToMainTtid otidToMainTtid;
    private final InterruptedEvents interruptedEvents;
    private final ThreadInfosComponent threadInfosComponent;

    // The right thing to do may be to remove the ttidToStartMask map and add the start SignalMask in the
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
    private final Map<Integer, SignalMask> ttidToStartMask;
    private final Map<Integer, List<MaskChangeEvent>> ttidToMaskAfterEvent;

    public SignalMaskForEvents(
            ComputingProducerWrapper<RawTracesByTtid> rawTracesByTtid,
            ComputingProducerWrapper<SortedTtidsWithParentFirst> sortedTtidsWithParentFirst,
            ComputingProducerWrapper<? extends SignalMaskAtWindowStart> signalMaskAtWindowStart,
            ComputingProducerWrapper<OtidToMainTtid> otidToMainTtid,
            ComputingProducerWrapper<InterruptedEvents> interruptedEvents,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent) {
        this.rawTracesByTtid = rawTracesByTtid.getAndRegister(this);
        this.sortedTtidsWithParentFirst = sortedTtidsWithParentFirst.getAndRegister(this);
        this.signalMaskAtWindowStart = signalMaskAtWindowStart.getAndRegister(this);
        this.otidToMainTtid = otidToMainTtid.getAndRegister(this);
        this.interruptedEvents = interruptedEvents.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);

        this.ttidToStartMask = new HashMap<>();
        ttidToMaskAfterEvent = new HashMap<>();
    }

    @Override
    public void compute() {
        ttidToMaskAfterEvent.clear();

        for (int ttid : sortedTtidsWithParentFirst.getTtids()) {
            SignalMask threadMask = getStartSignalMask(ttid);
            SignalMask threadMaskForLambda = threadMask;
            List<MaskChangeEvent> maskAfterEvent =
                    ttidToMaskAfterEvent.computeIfAbsent(ttid, k -> initialMaskList(threadMaskForLambda));
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
                    ttidToStartMask.put(maybeTtid.getAsInt(), threadMask);
                } else if (event.getType() == EventType.ENTER_SIGNAL) {
                    ttidToStartMask.put(ttid, threadMask);
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
        List<MaskChangeEvent> maskAfterEventList = ttidToMaskAfterEvent.get(ttid);
        // The maskAfterEventList list is assumed to be small.
        Optional<SignalMask> lastMask = Optional.empty();
        for (MaskChangeEvent maskChangeEvent : maskAfterEventList) {
            OptionalLong maskChangeId = maskChangeEvent.getEventId();
            if (maskChangeId.isPresent() && maskChangeId.getAsLong() >= eventId) {
                break;
            }
            lastMask = Optional.of(maskChangeEvent.getSignalMask());
        }
        assert lastMask.isPresent();
        return lastMask.get();
    }

    public Map<Integer, SignalMask> extractTtidToLastEventMap() {
        return ttidToMaskAfterEvent.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get(entry.getValue().size() - 1).getSignalMask()));
    }

    private List<MaskChangeEvent> initialMaskList(SignalMask initialSignalMask) {
        List<MaskChangeEvent> initialMask = new ArrayList<>();
        initialMask.add(new MaskChangeEvent(initialSignalMask));
        return initialMask;
    }

    private SignalMask getStartSignalMask(int ttid) {
        Optional<SignalMask> maybeLastMask = signalMaskAtWindowStart.getMask(ttid);
        return maybeLastMask
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
        return getSignalMaskBeforeEvent(interruptedTtid, interruptedEventId.getAsLong()).enabledToUnknown();
    }

    private Optional<SignalMask> getStartEventMask(int ttid) {
        return Optional.ofNullable(ttidToStartMask.get(ttid));
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
