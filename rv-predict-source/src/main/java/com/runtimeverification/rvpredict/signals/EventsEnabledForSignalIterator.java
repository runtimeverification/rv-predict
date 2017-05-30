package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.Iterator;

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

    private ReadonlyEventInterface previousEvent;
    private ReadonlyEventInterface currentEvent;

    /**
     * Whether the signal is enabled after the {@link #currentEvent} event.
     */
    private boolean enabled;
    private boolean firstStep;

    public EventsEnabledForSignalIterator(
            Collection<ReadonlyEventInterface> events,
            boolean detectInterruptedThreadRace,
            long signalNumber, boolean enabledAtStart) {
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
        this.signalNumber = signalNumber;
        previousEvent = null;
        currentEvent = null;
        enabled = enabledAtStart;
        eventsIterator = events.iterator();
        firstStep = true;
    }

    public ReadonlyEventInterface getPreviousEventWithDefault(ReadonlyEventInterface defaultValue) {
        return eventWithDefault(previousEvent, defaultValue);
    }

    public ReadonlyEventInterface getCurrentEventWithDefault(ReadonlyEventInterface defaultValue) {
        return eventWithDefault(currentEvent, defaultValue);
    }

    public boolean advance() {
        if (!enabled) {
            if (!findNextEnabledEvent()) {
                previousEvent = null;
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
        return previousEvent != null || currentEvent != null;
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
            if (currentEvent.isLock()) {
                return;
            }
        } while (enabled);
    }

    private boolean advanceOneStep() {
        if (!eventsIterator.hasNext()) {
            currentEvent = null;
            return false;
        }
        currentEvent = eventsIterator.next();
        enabled = Signals.updateEnabledWithEvent(enabled, signalNumber, currentEvent);
        return true;
    }

    private static ReadonlyEventInterface eventWithDefault(
            ReadonlyEventInterface event, ReadonlyEventInterface defaultValue) {
        if (event == null) {
            return defaultValue;
        }
        return event;
    }
}
