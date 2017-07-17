package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * For each iterator position, the signal can run between the previous event and the current one.
 *
 * If the previous event is absent, then one should consider the START event on the parent thread as
 * the previous event. Similarly, an absent current event is equivalent to the JOIN event on the parent
 * thread.
 */
public class EventsEnabledForSignalIterator {
    private final long signalNumber;
    private final Iterator<ReadonlyEventInterface> eventsIterator;
    private final boolean stopAtFirstMaskChangeEvent;
    private final Predicate<ReadonlyEventInterface> shouldNotCrossOver;
    private final Predicate<ReadonlyEventInterface> canIncludeOnlyOne;

    private Optional<ReadonlyEventInterface> previousEvent;
    private TwoConsecutiveEvents currentAndPrecedentEvent;

    /**
     * Whether the signal is enabled after the {@link #currentAndPrecedentEvent} event.
     */
    private boolean enabled;
    private boolean firstStep;
    private boolean reuseLastEvent;

    static EventsEnabledForSignalIterator createWithNoInterruptedThreadRaceDetectionStrictMode(
            Collection<ReadonlyEventInterface> events,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        return new EventsEnabledForSignalIterator(
                events,
                x -> true,
                x -> false,
                signalNumber,
                enabledAtStart,
                stopAtFirstMaskChangeEvent);
    }

    public static EventsEnabledForSignalIterator createWithNoInterruptedThreadRaceDetectionFastMode(
            Collection<ReadonlyEventInterface> events,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        return new EventsEnabledForSignalIterator(
                events,
                x -> eventCannotBeCrossed(x) || eventCanHaveRace(x),
                x -> false,
                signalNumber,
                enabledAtStart,
                stopAtFirstMaskChangeEvent);
    }

    static EventsEnabledForSignalIterator createWithInterruptedThreadRaceDetectionStrictMode(
            Collection<ReadonlyEventInterface> events,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        return new EventsEnabledForSignalIterator(
                events,
                x -> !eventCanHaveRace(x),
                EventsEnabledForSignalIterator::eventCanHaveRace,
                signalNumber,
                enabledAtStart,
                stopAtFirstMaskChangeEvent);
    }

    public static EventsEnabledForSignalIterator createWithInterruptedThreadRaceDetectionFastMode(
            Collection<ReadonlyEventInterface> events,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        return new EventsEnabledForSignalIterator(
                events,
                EventsEnabledForSignalIterator::eventCannotBeCrossed,
                EventsEnabledForSignalIterator::eventCanHaveRace,
                signalNumber,
                enabledAtStart,
                stopAtFirstMaskChangeEvent);
    }

    public static EventsEnabledForSignalIterator createWithInterruptedThreadRaceDetectionFastUnsoundMode(
            Collection<ReadonlyEventInterface> events,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        return new EventsEnabledForSignalIterator(
                events,
                EventsEnabledForSignalIterator::eventCannotBeCrossed,
                e -> false,
                signalNumber,
                enabledAtStart,
                stopAtFirstMaskChangeEvent);
    }

    private EventsEnabledForSignalIterator(
            Collection<ReadonlyEventInterface> events,
            Predicate<ReadonlyEventInterface> shouldNotCrossOver,
            Predicate<ReadonlyEventInterface> canIncludeOnlyOne,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        this.signalNumber = signalNumber;
        this.stopAtFirstMaskChangeEvent = stopAtFirstMaskChangeEvent;
        this.shouldNotCrossOver = shouldNotCrossOver;
        this.canIncludeOnlyOne = canIncludeOnlyOne;
        previousEvent = Optional.empty();
        currentAndPrecedentEvent = new TwoConsecutiveEvents();
        enabled = enabledAtStart;
        eventsIterator = events.iterator();
        firstStep = true;
        reuseLastEvent = false;
    }

    public Optional<ReadonlyEventInterface> getPreviousEventWithDefault(Optional<ReadonlyEventInterface> defaultValue) {
        return eventWithDefault(previousEvent, defaultValue);
    }

    public Optional<ReadonlyEventInterface> getCurrentEventWithDefault(Optional<ReadonlyEventInterface> defaultValue) {
        return eventWithDefault(currentAndPrecedentEvent.getEvent(), defaultValue);
    }

    public boolean advance() {
        if (!enabled) {
            if (stopAtFirstMaskChangeEvent || !findNextEnabledEvent()) {
                previousEvent = Optional.empty();
                return false;
            }
        }
        if (reuseLastEvent) {
            previousEvent = currentAndPrecedentEvent.getPreviousEvent();
        } else {
            previousEvent = currentAndPrecedentEvent.getEvent();
        }
        findNextBorderEvent();
        if (firstStep) {
            firstStep = false;
            return true;
        }
        return previousEvent.isPresent() || currentAndPrecedentEvent.getEvent().isPresent();
    }

    private boolean findNextEnabledEvent() {
        do {
            if (!advanceOneStep()) {
                return false;
            }
        } while (!enabled);
        return true;
    }

    private void findNextBorderEvent() {
        boolean alreadyIncludedOneLonelyElement =
                reuseLastEvent
                        && currentAndPrecedentEvent.getEvent().isPresent()
                        && canIncludeOnlyOne.test(currentAndPrecedentEvent.getEvent().get());
        reuseLastEvent = false;
        do {
            if (!advanceOneStep()) {
                return;
            }
            Optional<ReadonlyEventInterface> event = currentAndPrecedentEvent.getEvent();
            if (event.isPresent()) {
                if (canIncludeOnlyOne.test(event.get())) {
                    if (alreadyIncludedOneLonelyElement) {
                        reuseLastEvent = true;
                        return;
                    }
                    alreadyIncludedOneLonelyElement = true;
                }
                if (shouldNotCrossOver.test(event.get())) {
                    return;
                }
            }
        } while (enabled);
    }

    private boolean advanceOneStep() {
        if (!eventsIterator.hasNext()) {
            currentAndPrecedentEvent.addEvent(Optional.empty());
            return false;
        }
        ReadonlyEventInterface event = eventsIterator.next();
        // TODO(virgil): I should also use atomic locks as enable/disable events. It's not that easy, though
        // since an atomic unlock should just restore the previous state, it should not actually enable.
        if (stopAtFirstMaskChangeEvent && Signals.signalEnableChange(event, signalNumber).isPresent()) {
            enabled = false;
            currentAndPrecedentEvent.addEvent(Optional.of(event));
            return true;
        }
        enabled = Signals.updateEnabledWithEvent(enabled, signalNumber, event);
        currentAndPrecedentEvent.addEvent(Optional.of(event));
        return true;
    }

    private static Optional<ReadonlyEventInterface> eventWithDefault(
            Optional<ReadonlyEventInterface> event, Optional<ReadonlyEventInterface> defaultValue) {
        return event.isPresent() ? event : defaultValue;
    }

    private static boolean eventCannotBeCrossed(ReadonlyEventInterface event) {
        return event.isLock() || event.isSignalEvent();
    }

    private static boolean eventCanHaveRace(ReadonlyEventInterface event) {
        // Note that although it looks like we can also require that the event uses a variable also used by the
        // interrupting signal, we can't simply do that because the current signal can be interrupted by another
        // signal.
        return event.isReadOrWrite();
    }

    private static class TwoConsecutiveEvents {
        private Optional<ReadonlyEventInterface> event;
        private Optional<ReadonlyEventInterface> previousEvent;

        private TwoConsecutiveEvents() {
            reset();
        }

        private void reset() {
            event = Optional.empty();
            previousEvent = Optional.empty();
        }

        private void addEvent(Optional<ReadonlyEventInterface> newEvent) {
            previousEvent = event;
            event = newEvent;
        }

        private Optional<ReadonlyEventInterface> getEvent() {
            return event;
        }

        private Optional<ReadonlyEventInterface> getPreviousEvent() {
            return previousEvent;
        }
    }
}
