package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.producers.signals.SignalMaskForEvents;

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
    private final Map<Integer, SignalMask> signalMasks = new HashMap<>();
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
        signalMasks.clear();
        signalMasks.putAll(other.signalMasks);
        minEventIdForWindow = other.minEventIdForWindow;
    }

    void initializeForNextWindow() {
        threadsForCurrentWindow.clear();
        threadsForCurrentWindow.addAll(ongoingThreadsCache);
        threadsStarted.removeAll(threadsJoined);
        threadsJoined.clear();
        minEventIdForWindow = OptionalLong.empty();
    }

    Collection<Integer> getUnfinishedTtids() {
        return ongoingThreadsCache;
    }

    Optional<ReadonlyEventInterface> getLastEstablishEvent(long signalNumber) {
        return Optional.ofNullable(
                signalNumberToLastWindowEstablishEvents.get(signalNumber));
    }

    void registerThread(int ttid) {
        startThread(ttid);
    }

    void registerSignal(int ttid) {
        startThread(ttid);
    }

    void threadEvent(int ttid) {
        startThread(ttid);
    }

    private void startThread(int ttid) {
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

    void processEvent(ReadonlyEventInterface event, int ttid) {
        if (event.isSignalEvent()) {
            if (event.getType() == EventType.ESTABLISH_SIGNAL) {
                establishSignal(event);
            } else if (event.getType() == EventType.ENTER_SIGNAL) {
                registerSignal(ttid);
            } else if (event.getType() == EventType.EXIT_SIGNAL) {
                joinThread(ttid);
            }
        }
        processEvent(event.getEventId());
    }

    void processSignalMasks(SignalMaskForEvents signalMasks) {
        this.signalMasks.putAll(signalMasks.extractTtidToLastEventMap());
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

    private void processEvent(long eventId) {
        if (!minEventIdForWindow.isPresent()) {
            minEventIdForWindow = OptionalLong.of(eventId);
        }
        minEventIdForWindow = OptionalLong.of(Math.min(eventId, minEventIdForWindow.getAsLong()));
    }

    private void establishSignal(ReadonlyEventInterface event) {
        Optional<ReadonlyEventInterface> previousEvent = getLastEstablishEvent(event.getSignalNumber());
        if (!previousEvent.isPresent() || previousEvent.get().getEventId() < event.getEventId()) {
            signalNumberToLastWindowEstablishEvents.put(event.getSignalNumber(), event);
        }
    }

    Map<Integer,SignalMask> getSignalMasks() {
        return signalMasks;
    }

    Set<Integer> getStartedThreads() {
        return threadsStarted;
    }

    Set<Integer> getFinishedThreads() {
        return threadsJoined;
    }
}
