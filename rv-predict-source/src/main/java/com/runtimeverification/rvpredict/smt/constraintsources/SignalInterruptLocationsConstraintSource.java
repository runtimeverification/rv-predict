package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.signals.EventsEnabledForSignalIterator;
import com.runtimeverification.rvpredict.signals.Signals;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.Before;
import com.runtimeverification.rvpredict.smt.constraints.EventCanOccurBetweenGoodEventAndBadEvent;
import com.runtimeverification.rvpredict.smt.constraints.False;
import com.runtimeverification.rvpredict.smt.constraints.Or;
import com.runtimeverification.rvpredict.smt.constraints.SectionOccursBetweenEvents;
import com.runtimeverification.rvpredict.smt.constraints.SignalInterruptsThread;
import com.runtimeverification.rvpredict.trace.ThreadType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Adds constraints that specify where a signal can interrupt a thread. In order for a signal to interrupt a thread,
 * the following things must happen:
 *
 * 1. The signal handler must be set to the observed signal's handler. The signal handler is global, so it can
 *    be handled as a variable read, so it is NOT checked here.
 * 2. The signal mask must be set to the right value. Masks are per thread, which means that we can analyse them
 *    statically. The easy case is when we have enable/disable/mask set events for
 *    the interrupting signal, when we just use that. However, it's a bit more complex to interrupt between
 *    the first thread event and the first enable/disable/mask event. For that we do the following
 * 2.a. A thread inherits its caller mask, so we must check what was the mask at the beginning of each thread
 *      and if the current thread changed it. If there is no mask change for a thread or its parents, but a signal
 *      started, we will assume that the mask was enabled at the beginning of the thread.
 *      TODO(virgil): keep signal enabling state between windows and just use that instead of inferring the enable
 *      state.
 * 2.b. A signal uses the mask configured by the last sigset event. This is global, but can't be handled as a normal
 *      read, so we add a condition that the
 */
public class SignalInterruptLocationsConstraintSource implements ConstraintSource {
    private final Map<Integer, List<ReadonlyEventInterface>> eventsByThreadID;
    private final Function<Integer, ThreadType> ttidToThreadType;
    private final Function<Integer, Long> ttidToSignalNumber;
    private final Function<Integer, Long> ttidToSignalHandler;
    private final BiFunction<Long, Long, Collection<ReadonlyEventInterface>> signalNumberAndHandlerToEstablishEvents;
    private final Function<Integer, Optional<ReadonlyEventInterface>> ttidToStartEvent;
    private final Function<Integer, Optional<ReadonlyEventInterface>> ttidToJoinEvent;
    private final Function<Long, Set<Integer>> signalNumberToTtidsWhereEnabledAtStart;
    private final Function<Long, Set<Integer>> signalNumberToTtidsWhereDisabledAtStart;
    private final boolean detectInterruptedThreadRace;
    private final EventCanOccurBetweenGoodEventAndBadEvent.HappensBefore happensBefore;

    public SignalInterruptLocationsConstraintSource(
            Map<Integer, List<ReadonlyEventInterface>> eventsByThreadID,
            Function<Integer, ThreadType> ttidToThreadType,
            Function<Integer, Long> ttidToSignalNumber,
            Function<Integer, Long> ttidToSignalHandler,
            BiFunction<Long, Long, Collection<ReadonlyEventInterface>> signalNumberAndHandlerToEstablishEvents,
            Function<Integer, Optional<ReadonlyEventInterface>> ttidToStartEvent,
            Function<Integer, Optional<ReadonlyEventInterface>> ttidToJoinEvent,
            Function<Long, Set<Integer>> signalNumberToTtidsWhereEnabledAtStart,
            Function<Long, Set<Integer>> signalNumberToTtidsWhereDisabledAtStart,
            boolean detectInterruptedThreadRace,
            EventCanOccurBetweenGoodEventAndBadEvent.HappensBefore happensBefore) {
        this.eventsByThreadID = eventsByThreadID;
        this.ttidToThreadType = ttidToThreadType;
        this.ttidToSignalNumber = ttidToSignalNumber;
        this.ttidToSignalHandler = ttidToSignalHandler;
        this.signalNumberAndHandlerToEstablishEvents = signalNumberAndHandlerToEstablishEvents;
        this.ttidToStartEvent = ttidToStartEvent;
        this.ttidToJoinEvent = ttidToJoinEvent;
        this.signalNumberToTtidsWhereEnabledAtStart = signalNumberToTtidsWhereEnabledAtStart;
        this.signalNumberToTtidsWhereDisabledAtStart = signalNumberToTtidsWhereDisabledAtStart;
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
        this.happensBefore = happensBefore;
    }

    @Override
    public ModelConstraint createConstraint() {
        ImmutableList.Builder<ModelConstraint> allSignalConstraints = new ImmutableList.Builder<>();
        eventsByThreadID.keySet().stream()
                .filter(interruptingTtid -> ttidToThreadType.apply(interruptingTtid) == ThreadType.SIGNAL)
                .forEach(interruptingTtid -> {
                    Optional<ReadonlyEventInterface> maybefirstEvent = getFirstEvent(interruptingTtid);
                    Optional<ReadonlyEventInterface> maybeLastEvent = getLastEvent(interruptingTtid);
                    assert maybefirstEvent.isPresent();
                    assert maybeLastEvent.isPresent();

                    allSignalConstraints.add(computeSignalInterruptionConstraint(
                            interruptingTtid,
                            maybefirstEvent.get(), maybeLastEvent.get(),
                            ttidToSignalNumber.apply(interruptingTtid)));
                });
        return new And(allSignalConstraints.build());
    }

    private ModelConstraint computeSignalInterruptionConstraint(
            Integer interruptingTtid,
            ReadonlyEventInterface firstEvent, ReadonlyEventInterface lastEvent,
            long signalNumber) {
        Set<Integer> ttidWhereEnabledAtStart = signalNumberToTtidsWhereEnabledAtStart.apply(signalNumber);
        Set<Integer> ttidWhereDisabledAtStart = signalNumberToTtidsWhereDisabledAtStart.apply(signalNumber);
        ImmutableList.Builder<ModelConstraint> possibleSignalInterruptions = new ImmutableList.Builder<>();
        eventsByThreadID.forEach((interruptedTtid, interruptedThreadEvents) -> {
            if (interruptedTtid.equals(interruptingTtid)) {
                return;
            }
            if (!ttidWhereDisabledAtStart.contains(interruptedTtid)) {
                possibleSignalInterruptions.add(signalInterruptsAtStart(
                        interruptingTtid, signalNumber,
                        firstEvent, lastEvent,
                        interruptedThreadEvents,
                        interruptedTtid,
                        ttidWhereEnabledAtStart.contains(interruptedTtid)));
            }
            if (interruptedThreadEvents.isEmpty()) {
                return;
            }

            boolean isSignal = ttidToThreadType.apply(interruptedTtid) == ThreadType.SIGNAL;
            Optional<ReadonlyEventInterface> joinThreadEvent =
                    isSignal
                            ? getLastEvent(interruptedTtid)
                            : ttidToJoinEvent.apply(interruptedTtid);
            EventsEnabledForSignalIterator iterator =
                    new EventsEnabledForSignalIterator(
                            interruptedThreadEvents, detectInterruptedThreadRace, signalNumber,
                            false,  // enabledAtStart
                            false  // stopAtFirstMaskChangeEvent
                    );
            while (iterator.advance()) {
                Optional<ReadonlyEventInterface> previousEvent = iterator.getPreviousEventWithDefault(Optional.empty());
                assert previousEvent.isPresent();
                possibleSignalInterruptions.add(signalInterruptsBetweenEvents(
                        previousEvent,
                        iterator.getCurrentEventWithDefault(joinThreadEvent),
                        firstEvent,
                        lastEvent,
                        interruptingTtid,
                        interruptedTtid));
            }
        });
        return new Or(possibleSignalInterruptions.build());
    }

    private ModelConstraint signalInterruptsAtStart(
            int interruptingSignalTtid, long interruptingSignalNumber,
            ReadonlyEventInterface firstInterruptingSignalEvent, ReadonlyEventInterface lastInterruptingSignalEvent,
            Collection<ReadonlyEventInterface> interruptedThreadEvents,
            int interruptedTtid,
            boolean enabledAtStart) {
        if (ttidToThreadType.apply(interruptedTtid) == ThreadType.SIGNAL) {
            return signalInterruptsSignalAtStartWhenSigsetMaskAllowsIt(
                    interruptingSignalTtid, interruptingSignalNumber,
                    firstInterruptingSignalEvent, lastInterruptingSignalEvent,
                    interruptedThreadEvents,
                    enabledAtStart,
                    interruptedTtid);
        }
        if (enabledAtStart) {
            return signalInterruptsThreadAtStart(
                    interruptingSignalTtid, interruptingSignalNumber,
                    firstInterruptingSignalEvent, lastInterruptingSignalEvent,
                    interruptedThreadEvents,
                    interruptedTtid);
        }
        return new False();
    }

    private ModelConstraint signalInterruptsThreadAtStart(
            int interruptingSignalTtid, long interruptingSignalNumber,
            ReadonlyEventInterface firstInterruptingSignalEvent, ReadonlyEventInterface lastInterruptingSignalEvent,
            Collection<ReadonlyEventInterface> interruptedThreadEvents,
            int interruptedTtid) {
        Optional<ReadonlyEventInterface> joinThreadEvent = ttidToJoinEvent.apply(interruptedTtid);
        Optional<ReadonlyEventInterface> startThreadEvent = ttidToStartEvent.apply(interruptedTtid);
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        interruptedThreadEvents, detectInterruptedThreadRace, interruptingSignalNumber,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                );
        ImmutableList.Builder<ModelConstraint> possibleInterruptions = new ImmutableList.Builder<>();
        while (iterator.advance()) {
            possibleInterruptions.add(signalInterruptsBetweenEvents(
                    iterator.getPreviousEventWithDefault(startThreadEvent),
                    iterator.getCurrentEventWithDefault(joinThreadEvent),
                    firstInterruptingSignalEvent,
                    lastInterruptingSignalEvent,
                    interruptingSignalTtid,
                    interruptedTtid));
        }
        return new Or(possibleInterruptions.build());
    }

    private ModelConstraint signalInterruptsSignalAtStartWhenSigsetMaskAllowsIt(
            int interruptingSignalTtid, long interruptingSignalNumber,
            ReadonlyEventInterface firstInterruptingSignalEvent, ReadonlyEventInterface lastInterruptingSignalEvent,
            Collection<ReadonlyEventInterface> interruptedThreadEvents,
            boolean enabled,
            int interruptedSignalTtid) {
        Optional<ReadonlyEventInterface> firstInterruptedSignalEvent = getFirstEvent(interruptedSignalTtid);
        if (!firstInterruptedSignalEvent.isPresent()) {
            return new False();
        }
        long interruptedSignalNumber = ttidToSignalNumber.apply(interruptedSignalTtid);
        long interruptedSignalHandler = ttidToSignalHandler.apply(interruptedSignalTtid);
        ImmutableList.Builder<ModelConstraint> possibleInterruptionPlaces = new ImmutableList.Builder<>();

        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        interruptedThreadEvents, detectInterruptedThreadRace, interruptingSignalNumber,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                );
        while (iterator.advance()) {
            Optional<ReadonlyEventInterface> previousEvent =
                    iterator.getPreviousEventWithDefault(firstInterruptedSignalEvent);
            assert previousEvent.isPresent();
            possibleInterruptionPlaces.add(
                    new SectionOccursBetweenEvents(
                            firstInterruptingSignalEvent, lastInterruptingSignalEvent,
                            previousEvent, iterator.getCurrentEventWithDefault(getLastEvent(interruptedSignalTtid))
                    )
            );
        }
        return new And(
                new SignalInterruptsThread(interruptingSignalTtid, interruptedSignalTtid),
                new Or(possibleInterruptionPlaces.build()),
                new EventCanOccurBetweenGoodEventAndBadEvent(
                        firstInterruptedSignalEvent.get(),
                        enabled,
                        signalNumberAndHandlerToEstablishEvents.apply(interruptedSignalNumber, interruptedSignalHandler),
                        event -> Signals.signalIsEnabled(interruptingSignalNumber, event.getFullWriteSignalMask()),
                        happensBefore));
    }

    private ModelConstraint signalInterruptsBetweenEvents(
            Optional<ReadonlyEventInterface> before, Optional<ReadonlyEventInterface> after,
            ReadonlyEventInterface firstSignalEvent, ReadonlyEventInterface lastSignalEvent,
            int interruptingSignalTtid, int interruptedTtid) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        before.ifPresent(beforeEvent -> constraints.add(new Before(beforeEvent, firstSignalEvent)));
        after.ifPresent(afterEvent -> constraints.add(new Before(lastSignalEvent, afterEvent)));
        constraints.add(new SignalInterruptsThread(interruptingSignalTtid, interruptedTtid));
        return new And(constraints.build());
    }

    private Optional<ReadonlyEventInterface> getFirstEvent(int ttid) {
        List<ReadonlyEventInterface> events = eventsByThreadID.get(ttid);
        return events == null ? Optional.empty() : Optional.of(events.get(0));
    }

    private Optional<ReadonlyEventInterface> getLastEvent(int ttid) {
        List<ReadonlyEventInterface> events = eventsByThreadID.get(ttid);
        return events == null ? Optional.empty() : Optional.of(events.get(events.size() - 1));
    }
}
