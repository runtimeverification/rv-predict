package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.trace.producers.base.MinEventIdForWindow;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSetDifference;
import com.runtimeverification.rvpredict.trace.producers.base.TtidsForCurrentWindow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Computes various information related to signal interruption points.
 *
 * 1. Computes a map from signal numbers to a map from threads interrupted by signals with those numbers to the
 *    minimum ID of an interrupted event. This "minimum ID" term is not what would one normally expect.
 *    In the default case, the minimum id of an interrupted event is the id of the last
 *    event on a given thread before any interruption by a signal with a certain number, plus 1. If there is no such
 *    event, then the minimum ID is the minimum ID of an event on that thread. If the interrupted thread has no event,
 *    the minimum ID is the minimum of all event IDs in the current window.
 *    TODO(virgil): integration test in which the first event in the interrupted thread is a disable.
 * 2. Computes a map from threads to interrupted threads.
 * 3. Computes a map from threads to the event on the interrupted thread just before the interruption (if any).
 */
public class InterruptedEvents extends ComputingProducer<InterruptedEvents.State> {
    private final RawTracesByTtid rawTracesByTtid;
    private final TtidsForCurrentWindow ttidsForCurrentWindow;
    private final ThreadInfosComponent threadInfosComponent;
    private final TtidSetDifference threadStartsInTheCurrentWindow;
    private final TtidSetDifference threadEndsInTheCurrentWindow;
    private final MinEventIdForWindow minEventIdForWindow;

    protected static class State implements ProducerState {
        private final Map<Long, Map<Integer, Long>> signalNumberToTtidToNextMinInterruptedEventId = new HashMap<>();
        private final Map<Integer, Long> ttidToInterruptedEventId = new HashMap<>();
        private final Map<Integer, Integer> ttidToInterruptedTtid = new HashMap<>();

        @Override
        public void reset() {
            signalNumberToTtidToNextMinInterruptedEventId.clear();
            ttidToInterruptedEventId.clear();
            ttidToInterruptedTtid.clear();
        }
    }

    public InterruptedEvents(
            ComputingProducerWrapper<RawTracesByTtid> rawTracesByTtid,
            ComputingProducerWrapper<TtidsForCurrentWindow> ttidsForCurrentWindow,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent,
            ComputingProducerWrapper<TtidSetDifference> threadStartsInTheCurrentWindow,
            ComputingProducerWrapper<TtidSetDifference> threadEndsInTheCurrentWindow,
            ComputingProducerWrapper<MinEventIdForWindow> minEventIdForWindow) {
        super(new State());
        this.rawTracesByTtid = rawTracesByTtid.getAndRegister(this);
        this.ttidsForCurrentWindow = ttidsForCurrentWindow.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
        this.threadStartsInTheCurrentWindow = threadStartsInTheCurrentWindow.getAndRegister(this);
        this.threadEndsInTheCurrentWindow = threadEndsInTheCurrentWindow.getAndRegister(this);
        this.minEventIdForWindow = minEventIdForWindow.getAndRegister(this);
    }

    public Map<Long, Map<Integer, Long>> getSignalNumberToTtidToNextMinInterruptedEventId() {
        return getState().signalNumberToTtidToNextMinInterruptedEventId;
    }

    public OptionalLong getInterruptedEventId(int interruptingTtid) {
        Long interruptedEventId = getState().ttidToInterruptedEventId.get(interruptingTtid);
        return interruptedEventId == null ? OptionalLong.empty() : OptionalLong.of(interruptedEventId);
    }

    public int getInterruptedTtid(int interruptingTtid) {
        return getState().ttidToInterruptedTtid.get(interruptingTtid);
    }

    @Override
    protected void compute() {
        ttidsForCurrentWindow.getTtids().stream()
                .filter(ttid -> threadInfosComponent.getThreadType(ttid) == ThreadType.SIGNAL)
                .forEach(ttid -> {
                    ThreadInfo threadInfo = threadInfosComponent.getThreadInfo(ttid);
                    Optional<RawTrace> trace = rawTracesByTtid.getRawTrace(ttid);
                    Optional<ReadonlyEventInterface> firstSignalEvent =
                            (trace.isPresent() && trace.get().size() > 0)
                                    ? Optional.of(trace.get().event(0)) : Optional.empty();
                    int signalDepth = threadInfo.getSignalDepth();
                    long otid = threadInfo.getOriginalThreadId();
                    Optional<Integer> maybeInterruptedThread =
                            findInterruptedThread(firstSignalEvent, signalDepth, otid);
                    if (!maybeInterruptedThread.isPresent()) {
                        // TODO(virgil): Enable this assert after David ensures that a signal's parent can't
                        // start after it.
                        //assert false;
                        return;
                    }
                    int interruptedTtid = maybeInterruptedThread.get();
                    getState().ttidToInterruptedTtid.put(threadInfo.getId(), interruptedTtid);
                    Optional<RawTrace> maybeInterruptedTrace = rawTracesByTtid.getRawTrace(interruptedTtid);
                    Optional<ReadonlyEventInterface> maybeInterruptedEvent =
                            findInterruptedEvent(firstSignalEvent, maybeInterruptedTrace);
                    assert threadInfo.getSignalNumber().isPresent();
                    Map<Integer, Long> ttidToNextMinEventId =
                            getState().signalNumberToTtidToNextMinInterruptedEventId
                                    .computeIfAbsent(threadInfo.getSignalNumber().getAsLong(), k -> new HashMap<>());
                    if (!maybeInterruptedEvent.isPresent()) {
                        assert !threadStartsInTheCurrentWindow.contains(interruptedTtid) || signalDepth == 1;

                        long eventId;
                        if (maybeInterruptedTrace.isPresent() && maybeInterruptedTrace.get().size() > 0) {
                            eventId = maybeInterruptedTrace.get().event(0).getEventId();
                        } else {
                            OptionalLong minEventId = minEventIdForWindow.getId();
                            assert minEventId.isPresent();
                            eventId = minEventId.getAsLong();
                        }
                        ttidToNextMinEventId.put(interruptedTtid, eventId);
                    } else {
                        ReadonlyEventInterface interruptedEvent = maybeInterruptedEvent.get();
                        getState().ttidToInterruptedEventId.put(threadInfo.getId(), interruptedEvent.getEventId());
                        long nextInterruptedEventId = interruptedEvent.getEventId() + 1;
                        ttidToNextMinEventId.compute(
                                interruptedTtid,
                                (k, v) -> v == null ? nextInterruptedEventId : Math.min(nextInterruptedEventId, v));
                    }
                });
    }

    private Optional<ReadonlyEventInterface> findInterruptedEvent(
            Optional<ReadonlyEventInterface> firstSignalEvent, Optional<RawTrace> maybeInterruptedTrace) {
        Optional<ReadonlyEventInterface> maybeInterruptedEvent = Optional.empty();
        if (maybeInterruptedTrace.isPresent()) {
            assert firstSignalEvent.isPresent();
            RawTrace interruptedTrace = maybeInterruptedTrace.get();
            for (int i = interruptedTrace.size() - 1; i >= 0; i--) {
                ReadonlyEventInterface event = interruptedTrace.event(i);
                if (event.getEventId() <= firstSignalEvent.get().getEventId()) {
                    maybeInterruptedEvent = Optional.of(event);
                    break;
                }
            }
        }
        return maybeInterruptedEvent;
    }

    private Optional<Integer> findInterruptedThread(
            Optional<ReadonlyEventInterface> firstSignalEvent, int signalDepth, long otid) {
        List<Integer> candidateThreads = ttidsForCurrentWindow.getTtids().stream()
                .filter(candidateTtid ->
                        threadInfosComponent.getOriginalThreadIdForTraceThreadId(candidateTtid) == otid
                                && threadInfosComponent.getSignalDepth(candidateTtid) == signalDepth - 1)
                .collect(Collectors.toList());
        assert !candidateThreads.isEmpty();
        return candidateThreads.stream()
                .filter(candidateTtid -> {
                    Optional<RawTrace> maybeCandidateTrace = rawTracesByTtid.getRawTrace(candidateTtid);
                    if (!maybeCandidateTrace.isPresent() || maybeCandidateTrace.get().size() == 0) {
                        // This must be an ongoing thread that does not have any events in
                        // the current window, or a thread without any important events. However,
                        // signal events are important, so either this thread is a normal thread,
                        // or it is a signal without any start or end events, which means that it
                        // started before the current window and will end after it. Because of that,
                        // there should be exactly one such candidate which is the actual
                        // interrupted thread/signal.
                        assert candidateThreads.size() == 1;
                        return true;
                    }
                    if (signalDepth == 1) {
                        assert candidateThreads.size() == 1;
                        return true;
                    }
                    // If the parent thread has at least one event, then the current signal must have
                    // either its start or its end in the current window.
                    assert firstSignalEvent.isPresent();
                    RawTrace candidateTrace = maybeCandidateTrace.get();
                    ReadonlyEventInterface firstCandidateEvent = candidateTrace.event(0);
                    return firstCandidateEvent.getEventId() <= firstSignalEvent.get().getEventId()
                            || !threadStartsInTheCurrentWindow.contains(candidateTtid);
                })
                .filter(candidateTtid -> {
                    Optional<RawTrace> maybeCandidateTrace = rawTracesByTtid.getRawTrace(candidateTtid);
                    if (!maybeCandidateTrace.isPresent() || maybeCandidateTrace.get().size() == 0) {
                        // We must have also entered the similar if above.
                        assert candidateThreads.size() == 1;
                        return true;
                    }
                    if (signalDepth == 1) {
                        assert candidateThreads.size() == 1;
                        return true;
                    }
                    // See the similar assert above.
                    assert firstSignalEvent.isPresent();
                    RawTrace candidateTrace = maybeCandidateTrace.get();
                    ReadonlyEventInterface lastCandidateEvent =
                            candidateTrace.event(candidateTrace.size() - 1);
                    return lastCandidateEvent.getEventId() >= firstSignalEvent.get().getEventId()
                            || !threadEndsInTheCurrentWindow.contains(candidateTtid);
                })
                .findAny();
    }
}
