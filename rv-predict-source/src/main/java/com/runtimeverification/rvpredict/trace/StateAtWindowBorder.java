package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Handles the trace state near window borders
 *
 * Note that, since we're processing threads in a random order, and threads are split between windows, the following
 * may happen:
 *
 * 1. A thread started in the current window, but did not finish.
 * 2. A thread was joined, but did not start (because we didn't process yet the thread containing the start event).
 * 3. A thread was joined and was previously started.
 * 4. A thread was joined multiple times.
 *
 * Also note that a signal is treated as a normal thread most of the time.
 */
class StateAtWindowBorder {
    private final Set<Integer> ongoingThreadsCache = new HashSet<>();
    private final Set<Integer> threadsForCurrentWindow = new HashSet<>();
    private final Set<Integer> threadsStarted = new HashSet<>();
    private final Set<Integer> threadsJoined = new HashSet<>();
    private final Map<Long, ReadonlyEventInterface> signalNumberToLastWindowEstablishEvents
            = new HashMap<>();
    private OptionalLong minEventIdForWindow = OptionalLong.empty();

    void copyFrom(StateAtWindowBorder other) {
        ongoingThreadsCache.clear();
        ongoingThreadsCache.addAll(other.ongoingThreadsCache);
        threadsStarted.clear();
        threadsStarted.addAll(other.threadsStarted);
        threadsJoined.clear();
        threadsJoined.addAll(other.threadsJoined);
        signalNumberToLastWindowEstablishEvents.clear();
        signalNumberToLastWindowEstablishEvents.putAll(other.signalNumberToLastWindowEstablishEvents);
        threadsForCurrentWindow.clear();
        threadsForCurrentWindow.addAll(other.threadsForCurrentWindow);
        minEventIdForWindow = other.minEventIdForWindow;
    }

    void initializeForNextWindow() {
        threadsForCurrentWindow.clear();
        threadsForCurrentWindow.addAll(ongoingThreadsCache);
        minEventIdForWindow = OptionalLong.empty();
    }

    Collection<Integer> getUnfinishedTtids() {
        return ongoingThreadsCache;
    }

    void establishSignal(ReadonlyEventInterface event) {
        Optional<ReadonlyEventInterface> previousEvent = getLastEstablishEvent(event.getSignalNumber());
        if (!previousEvent.isPresent() || previousEvent.get().getEventId() < event.getEventId()) {
            signalNumberToLastWindowEstablishEvents.put(event.getSignalNumber(), event);
        }
    }

    Optional<ReadonlyEventInterface> getLastEstablishEvent(long signalNumber) {
        return Optional.ofNullable(
                signalNumberToLastWindowEstablishEvents.get(signalNumber));
    }

    void registerThread(int ttid) {
        startThread(ttid);
    }

    void registerSignal(int ttid) {
        registerThread(ttid);
    }

    void threadEvent(int ttid) {
        startThread(ttid);
    }

    void startThread(int ttid) {
        threadsStarted.add(ttid);
        threadsForCurrentWindow.add(ttid);
        if (!threadsJoined.contains(ttid)) {
            ongoingThreadsCache.add(ttid);
        }
    }

    void joinThread(int ttid) {
        threadsJoined.add(ttid);
        ongoingThreadsCache.remove(ttid);
    }

    void processEvent(long eventId) {
        if (!minEventIdForWindow.isPresent()) {
            minEventIdForWindow = OptionalLong.of(eventId);
        }
        minEventIdForWindow = OptionalLong.of(Math.min(eventId, minEventIdForWindow.getAsLong()));
    }

    boolean threadWasStarted(int ttid) {
        return threadsStarted.contains(ttid);
    }

    boolean threadEnded(int ttid) {
        return threadsJoined.contains(ttid);
    }

    Collection<Integer> getThreadsForCurrentWindow() {
        return threadsForCurrentWindow;
    }

    long getMinEventIdForWindow() {
        OptionalLong localMinEventIdForWindow = minEventIdForWindow;
        assert localMinEventIdForWindow.isPresent();
        return localMinEventIdForWindow.getAsLong();
    }
}
