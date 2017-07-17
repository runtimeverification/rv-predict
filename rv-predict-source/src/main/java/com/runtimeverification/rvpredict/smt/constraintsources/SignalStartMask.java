package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.signals.Signals;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.Before;
import com.runtimeverification.rvpredict.smt.constraints.DisjointSections;
import com.runtimeverification.rvpredict.smt.constraints.False;
import com.runtimeverification.rvpredict.smt.constraints.Or;
import com.runtimeverification.rvpredict.smt.constraints.DisabledIfDisabledOnInterruptedSignal;
import com.runtimeverification.rvpredict.smt.constraints.SectionOccursBetweenEvents;
import com.runtimeverification.rvpredict.smt.constraints.SignalEnabledOnThreadValue;
import com.runtimeverification.rvpredict.smt.constraints.SignalInterruptsThread;
import com.runtimeverification.rvpredict.smt.constraints.True;
import com.runtimeverification.rvpredict.trace.ThreadType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SignalStartMask implements ConstraintSource {
    private final Map<Integer, List<ReadonlyEventInterface>> ttidToEvents;
    private final Function<Integer, ThreadType> ttidToType;
    private final Function<Integer, Long> ttidToSignalNumber;
    private final Function<Integer, Long> ttidToSignalHandler;
    private final Function<Integer, Boolean> signalStartsInThisWindow;
    private final Function<Integer, Boolean> signalEndsInThisWindow;
    private final BiFunction<Integer, Long, Optional<Boolean>> signalEnabledAtStart;
    private final BiFunction<Long, Long, List<ReadonlyEventInterface>> establishSignalEvents;
    private final BiFunction<Long, Long, Optional<ReadonlyEventInterface>> previousWindowEstablishEvent;

    public SignalStartMask(
            Map<Integer, List<ReadonlyEventInterface>> ttidToEvents,
            Function<Integer, ThreadType> ttidToType,
            Function<Integer, Long> ttidToSignalNumber,
            Function<Integer, Long> ttidToSignalHandler,
            Function<Integer, Boolean> signalStartsInThisWindow,
            Function<Integer, Boolean> signalEndsInThisWindow,
            BiFunction<Integer, Long, Optional<Boolean>> signalEnabledAtStart,
            BiFunction<Long, Long, List<ReadonlyEventInterface>> establishSignalEvents,
            BiFunction<Long, Long, Optional<ReadonlyEventInterface>> previousWindowEstablishEvent) {
        this.ttidToEvents = ttidToEvents;
        this.ttidToType = ttidToType;
        this.ttidToSignalNumber = ttidToSignalNumber;
        this.ttidToSignalHandler = ttidToSignalHandler;
        this.signalStartsInThisWindow = signalStartsInThisWindow;
        this.signalEndsInThisWindow = signalEndsInThisWindow;
        this.signalEnabledAtStart = signalEnabledAtStart;
        this.establishSignalEvents = establishSignalEvents;
        this.previousWindowEstablishEvent = previousWindowEstablishEvent;
    }

    @Override
    public ModelConstraint createConstraint() {
        Set<Long> interestingSignalNumbers = ttidToEvents.keySet().stream()
                .filter(ttid -> ttidToType.apply(ttid) == ThreadType.SIGNAL)
                .map(ttidToSignalNumber)
                .collect(Collectors.toSet());
        return new And(
                signalMaskForSignalsAtWindowStart(interestingSignalNumbers),
                signalMaskFromEstablishEvents(interestingSignalNumbers),
                signalMaskFromInterruptions(interestingSignalNumbers));
    }

    private ModelConstraint signalMaskFromEstablishEvents(Collection<Long> interestingSignalNumbers) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        ttidToEvents.keySet().stream()
                .filter(interruptingTtid -> ttidToType.apply(interruptingTtid) == ThreadType.SIGNAL
                        && signalStartsInThisWindow.apply(interruptingTtid))
                .forEach(interruptingTtid -> {
                    assert !ttidToEvents.get(interruptingTtid).isEmpty();
                    ReadonlyEventInterface firstEvent = ttidToEvents.get(interruptingTtid).get(0);
                    long interruptingSignalHandler = ttidToSignalHandler.apply(interruptingTtid);
                    long interruptingSignalNumber = ttidToSignalNumber.apply(interruptingTtid);
                    Collection<ReadonlyEventInterface> establishEvents =
                            establishSignalEvents.apply(interruptingSignalNumber, interruptingSignalHandler);
                    ImmutableList.Builder<ModelConstraint> positionBetweenEstablishConstraints =
                            new ImmutableList.Builder<>();
                    establishEvents.forEach(justBefore -> {
                        List<ModelConstraint> notBetweenConstraints = establishEvents.stream()
                                // Intentional != comparison.
                                .filter(notBetween -> notBetween != justBefore)
                                .map(notBetween -> new DisjointSections(
                                        Optional.of(justBefore), Optional.of(firstEvent),
                                        Optional.of(notBetween), Optional.of(notBetween)))
                                .collect(Collectors.toList());
                        positionBetweenEstablishConstraints.add(
                                new And(
                                        new Before(justBefore, firstEvent),
                                        new And(ImmutableList.copyOf(notBetweenConstraints)),
                                        signalMaskConstraint(
                                                interruptingTtid, justBefore.getFullWriteSignalMask(),
                                                interestingSignalNumbers)));
                    });
                    Optional<ReadonlyEventInterface> previousEstablishEvent =
                            previousWindowEstablishEvent.apply(interruptingSignalNumber, interruptingSignalHandler);
                    if (!previousEstablishEvent.isPresent()) {
                        positionBetweenEstablishConstraints.add(
                                new And(
                                        new And(ImmutableList.copyOf(establishEvents.stream()
                                                .map(establish -> new Before(firstEvent, establish))
                                                .collect(Collectors.toList()))),
                                        new And(ImmutableList.copyOf(interestingSignalNumbers.stream()
                                                .map(number -> new SignalEnabledOnThreadValue(
                                                        interruptingTtid, number, false))
                                                .collect(Collectors.toList())))));
                    } else {
                        List<ModelConstraint> notBetweenConstraints = establishEvents.stream()
                                .map(notBetween -> new Before(firstEvent, notBetween))
                                .collect(Collectors.toList());
                        positionBetweenEstablishConstraints.add(
                                new And(
                                        new And(ImmutableList.copyOf(notBetweenConstraints)),
                                        signalMaskConstraint(
                                                interruptingTtid,
                                                previousEstablishEvent.get().getFullWriteSignalMask(),
                                                interestingSignalNumbers)));
                    }
                    constraints.add(new Or(positionBetweenEstablishConstraints.build()));
                });
        return new And(constraints.build());
    }

    private ModelConstraint signalMaskConstraint(int ttid, long signalMask, Collection<Long> signalNumbers) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        signalNumbers.stream()
                .filter(signalNumber -> !Signals.signalIsEnabled(signalNumber, signalMask))
                .forEach(signalNumber ->
                        constraints.add(new SignalEnabledOnThreadValue(
                                ttid, signalNumber, false)));
        return new And(constraints.build());
    }

    private ModelConstraint signalMaskForSignalsAtWindowStart(Collection<Long> interestingSignalNumbers) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        ttidToEvents.keySet().stream()
                .filter(ttid -> ttidToType.apply(ttid) == ThreadType.SIGNAL
                        && !signalStartsInThisWindow.apply(ttid))
                .forEach(ttid -> interestingSignalNumbers.forEach(signalNumber -> {
                    Optional<Boolean> maybeEnabled = signalEnabledAtStart.apply(ttid, signalNumber);
                    constraints.add(new SignalEnabledOnThreadValue(
                            ttid, signalNumber, maybeEnabled.orElse(Boolean.FALSE)));
                }));
        return new And(constraints.build());
    }

    private ModelConstraint signalMaskFromInterruptions(Collection<Long> interestingSignalNumbers) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();

        interestingSignalNumbers.forEach(signalNumber -> {
            ImmutableList.Builder<ModelConstraint> signalNumberConstraints = new ImmutableList.Builder<>();
            ttidToEvents.forEach((interruptingTtid, interruptingEvents) -> {
                if (ttidToType.apply(interruptingTtid) != ThreadType.SIGNAL
                        || !signalStartsInThisWindow.apply(interruptingTtid)
                        || interruptingEvents.isEmpty()) {
                    return;
                }
                ImmutableList.Builder<ModelConstraint> interruptingTtidConstraints = new ImmutableList.Builder<>();
                ReadonlyEventInterface firstInterruptingEvent = interruptingEvents.get(0);
                Optional<ReadonlyEventInterface> lastInterruptingEvent =
                        signalEndsInThisWindow.apply(interruptingTtid)
                                ? Optional.of(interruptingEvents.get(interruptingEvents.size() - 1))
                                : Optional.empty();
                ttidToEvents.forEach((interruptedTtid, interruptedEvents) -> {
                    if (interruptedTtid.equals(interruptingTtid)) {
                        return;
                    }
                    ImmutableList.Builder<ModelConstraint> interruptedTtidConstraints = new ImmutableList.Builder<>();
                    // We don't need to get the actual thread start event, that constraint should be added by the
                    // SignalInterruptLocationsConstraintSource.
                    Optional<ReadonlyEventInterface> previousEvent = Optional.empty();
                    Optional<Boolean> previousValue = signalEnabledAtStart.apply(interruptedTtid, signalNumber);
                    for (ReadonlyEventInterface event : interruptedEvents) {
                        if (!event.isSignalEvent()) {
                            continue;
                        }
                        Optional<Boolean> maybeChange = Signals.signalEnableChange(event, signalNumber);
                        if (!maybeChange.isPresent()) {
                            continue;
                        }
                        // One may think that if the lastInterruptingEvent is not present then
                        // the signal can't run here. However, it is not this RestrictSource's duty to
                        // enforce that, which is a good thing because it leaves the flexibility of using
                        // partial signal executions if we ever want that.
                        interruptedTtidConstraints.add(enabledBetweenEvents(
                                interruptingTtid, firstInterruptingEvent, lastInterruptingEvent,
                                signalNumber,
                                interruptedTtid, previousEvent, Optional.of(event),
                                previousValue));
                        previousEvent = Optional.of(event);
                        previousValue = maybeChange;
                    }
                    interruptedTtidConstraints.add(enabledBetweenEvents(
                            interruptingTtid, firstInterruptingEvent, lastInterruptingEvent,
                            signalNumber,
                            interruptedTtid, previousEvent, Optional.empty(),
                            previousValue));
                    interruptingTtidConstraints.add(new And(
                            new Or(interruptedTtidConstraints.build()),
                            new SignalInterruptsThread(interruptingTtid, interruptedTtid)));
                });
                signalNumberConstraints.add(new Or(interruptingTtidConstraints.build()));
            });
            constraints.add(new And(signalNumberConstraints.build()));
        });
        return new And(constraints.build());
    }

    private ModelConstraint enabledBetweenEvents(
            Integer interruptingTtid,
            ReadonlyEventInterface firstInterruptingEvent,
            Optional<ReadonlyEventInterface> lastInterruptingEvent,
            Long signalNumber,
            Integer interruptedTtid,
            Optional<ReadonlyEventInterface> previousEvent,
            Optional<ReadonlyEventInterface> nextEvent,
            Optional<Boolean> enabled) {
        ModelConstraint signalLocation;
        if (lastInterruptingEvent.isPresent()) {
            signalLocation = new SectionOccursBetweenEvents(
                    firstInterruptingEvent, lastInterruptingEvent.get(),
                    previousEvent, nextEvent);
        } else if (nextEvent.isPresent()) {
            // The signal does not end in this window, but the interrupting thread does.
            // However, if the signal start event is somehow in the current section, we
            // should still add a constraint for the enable value.
            signalLocation = new SectionOccursBetweenEvents(
                    firstInterruptingEvent, firstInterruptingEvent,
                    previousEvent, nextEvent);
        } else {
            if (previousEvent.isPresent()) {
                signalLocation = new Before(previousEvent.get(), firstInterruptingEvent);
            } else {
                signalLocation = new True();
            }
        }

        ModelConstraint enabledConstraint;
        if (enabled.isPresent()) {
            if (!enabled.get()) {
                enabledConstraint = new SignalEnabledOnThreadValue(interruptingTtid, signalNumber, false);
            } else {
                enabledConstraint = new True();
            }
        } else if (ttidToType.apply(interruptedTtid) == ThreadType.SIGNAL) {
            enabledConstraint =
                    new DisabledIfDisabledOnInterruptedSignal(interruptingTtid, interruptedTtid, signalNumber);
        } else {
            enabledConstraint = new False();
        }
        return new And(signalLocation, enabledConstraint);
    }
}
