package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.Iterator;

public class Signals {
    public static Boolean signalEnableChange(ReadonlyEventInterface event, long signalNumber) {
        switch (event.getType()) {
            case WRITE_SIGNAL_MASK:
            case READ_WRITE_SIGNAL_MASK:
                return signalInMask(signalNumber, event.getFullWriteSignalMask());
            case BLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return false;
                }
                return null;
            case UNBLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return true;
                }
                return null;
        }
        return null;
    }

    public static boolean updateEnabledWithEvent(boolean eventIsEnabled, long signalNumber, ReadonlyEventInterface event) {
        Boolean change = Signals.signalEnableChange(event, signalNumber);
        if (change == null) {
            return eventIsEnabled;
        }
        return change;
    }

    private static boolean signalInMask(long signalNumber, long mask) {
        return (mask & (1 << signalNumber)) != 0;
    }

    /**
     * For each iterator position, the signal can run between the previous event and the current one.
     *
     * If the previous event is absent, then one should consider the START event on the parent thread as
     * the previous event. Similarly, an absent current event is equivalent to the JOIN event on the parent
     * thread.
     */
    public static class EnabledEventsIterator {
        private final Collection<ReadonlyEventInterface> events;
        private final boolean detectInterruptedThreadRace;
        private final long signalNumber;

        ReadonlyEventInterface previousPreviousEvent;
        ReadonlyEventInterface previousEvent;
        ReadonlyEventInterface currentEvent;
        Iterator<ReadonlyEventInterface> iterator;
        /**
         * Whether the signal is enabled after the {@link #previousPreviousEvent} event.
         */
        boolean previousPreviousEnabled;
        /**
         * Whether the signal is enabled after the {@link #previousEvent} event.
         */
        boolean previousEnabled;
        /**
         * Whether the signal is enabled after the {@link #currentEvent} event.
         */
        boolean enabled;

        public EnabledEventsIterator(
                Collection<ReadonlyEventInterface> events,
                boolean detectInterruptedThreadRace,
                long signalNumber, boolean enabledAtStart) {
            this.events = events;
            this.detectInterruptedThreadRace = detectInterruptedThreadRace;
            this.signalNumber = signalNumber;
            previousPreviousEvent = null;
            previousEvent = null;
            currentEvent = null;
            previousPreviousEnabled = false;
            previousEnabled = false;
            enabled = enabledAtStart;
            iterator = events.iterator();
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
            if (!iterator.hasNext()) {
                currentEvent = null;
                return detectInterruptedThreadRace ? previousPreviousEvent != null : previousEvent != null;
            }
            currentEvent = iterator.next();
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
}
