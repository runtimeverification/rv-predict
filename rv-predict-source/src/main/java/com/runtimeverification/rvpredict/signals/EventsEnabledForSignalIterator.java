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

    private ReadonlyEventInterface previousPreviousEvent;
    private ReadonlyEventInterface previousEvent;
    private ReadonlyEventInterface currentEvent;
    /**
     * Whether the signal is enabled after the {@link #previousPreviousEvent} event.
     */
    private boolean previousPreviousEnabled;
    /**
     * Whether the signal is enabled after the {@link #previousEvent} event.
     */
    private boolean previousEnabled;
    /**
     * Whether the signal is enabled after the {@link #currentEvent} event.
     */
    private boolean enabled;

    public EventsEnabledForSignalIterator(
            Collection<ReadonlyEventInterface> events,
            boolean detectInterruptedThreadRace,
            long signalNumber, boolean enabledAtStart) {
        this.detectInterruptedThreadRace = detectInterruptedThreadRace;
        this.signalNumber = signalNumber;
        previousPreviousEvent = null;
        previousEvent = null;
        currentEvent = null;
        previousPreviousEnabled = false;
        previousEnabled = false;
        enabled = enabledAtStart;
        eventsIterator = events.iterator();
    }

    public ReadonlyEventInterface getPreviousEventWithDefault(ReadonlyEventInterface defaultValue) {
        assert previousEnabled;
        if (detectInterruptedThreadRace && previousPreviousEnabled) {
            return eventWithDefault(previousPreviousEvent, defaultValue);
        }
        return eventWithDefault(previousEvent, defaultValue);
    }

    public ReadonlyEventInterface getCurrentEventWithDefault(ReadonlyEventInterface defaultValue) {
        return eventWithDefault(currentEvent, defaultValue);
    }

    public boolean advance() {
        while (advanceOneStep()) {
            if (previousEnabled) {
                return true;
            }
        }
        return false;
    }

    private boolean advanceOneStep() {
        previousPreviousEnabled = previousEnabled;
        previousEnabled = enabled;
        previousPreviousEvent = previousEvent;
        previousEvent = currentEvent;
        if (!eventsIterator.hasNext()) {
            currentEvent = null;
            return detectInterruptedThreadRace ? previousPreviousEvent != null : previousEvent != null;
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
