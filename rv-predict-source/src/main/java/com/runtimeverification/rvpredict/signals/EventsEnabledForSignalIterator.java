package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * For each iterator position, the signal can run between the previous event and the current one.
 *
 * If the previous event is absent, then one should consider the START event on the parent thread as
 * the previous event. Similarly, an absent current event is equivalent to the JOIN event on the parent
 * thread.
 */
public class EventsEnabledForSignalIterator {
    private final boolean detectInterruptedThreadRace;
    private final long signalNumber;
    private final Iterator<ReadonlyEventInterface> eventsIterator;
    private final boolean stopAtFirstMaskChangeEvent;

    private Optional<ReadonlyEventInterface> previousEvent;
    private Optional<ReadonlyEventInterface> currentEvent;

    /**
     * Whether the signal is enabled after the {@link #currentEvent} event.
     */
    private boolean enabled;
    private boolean firstStep;

    public EventsEnabledForSignalIterator(
            Collection<ReadonlyEventInterface> events,
            boolean detectInterruptedThreadRace,
            long signalNumber,
            boolean enabledAtStart,
            boolean stopAtFirstMaskChangeEvent) {
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
        this.signalNumber = signalNumber;
        this.stopAtFirstMaskChangeEvent = stopAtFirstMaskChangeEvent;
        previousEvent = Optional.empty();
        currentEvent = Optional.empty();
        enabled = enabledAtStart;
        eventsIterator = events.iterator();
        firstStep = true;
    }

    public Optional<ReadonlyEventInterface> getPreviousEventWithDefault(Optional<ReadonlyEventInterface> defaultValue) {
        return eventWithDefault(previousEvent, defaultValue);
    }

    public Optional<ReadonlyEventInterface> getCurrentEventWithDefault(Optional<ReadonlyEventInterface> defaultValue) {
        return eventWithDefault(currentEvent, defaultValue);
    }

    public boolean advance() {
        if (!enabled) {
            if (stopAtFirstMaskChangeEvent ||!findNextEnabledEvent()) {
                previousEvent = Optional.empty();
                return false;
            }
        }
        previousEvent = currentEvent;
        if (detectInterruptedThreadRace) {
            findNextDisabledOrLockEvent();
        } else {
            advanceOneStep();
        }
        if (firstStep) {
            firstStep = false;
            return true;
        }
        return previousEvent.isPresent() || currentEvent.isPresent();
    }

    private boolean findNextEnabledEvent() {
        do {
            if (!advanceOneStep()) {
                return false;
            }
        } while (!enabled);
        return true;
    }

    private void findNextDisabledOrLockEvent() {
        do {
            if (!advanceOneStep()) {
                return;
            }
            Optional<ReadonlyEventInterface> event = currentEvent;
            if (event.isPresent() && event.get().isLock()) {
                return;
            }
        } while (enabled);
    }

    private boolean advanceOneStep() {
        if (!eventsIterator.hasNext()) {
            currentEvent = Optional.empty();
            return false;
        }
        ReadonlyEventInterface event = eventsIterator.next();
        if (stopAtFirstMaskChangeEvent && Signals.signalEnableChange(event, signalNumber).isPresent()) {
            enabled = false;
            currentEvent = Optional.of(event);
            return true;
        }
        enabled = Signals.updateEnabledWithEvent(enabled, signalNumber, event);
        currentEvent = Optional.of(event);
        return true;
    }

    private static Optional<ReadonlyEventInterface> eventWithDefault(
            Optional<ReadonlyEventInterface> event, Optional<ReadonlyEventInterface> defaultValue) {
        return event.isPresent() ? event : defaultValue;
    }
}
