package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.trace.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModelTrace {
    private final List<Long> threads;  // TODO: Make it local.
    private final Map<Long, Integer> threadToIndex;
    private final List<Long> variables;
    private final Map<Long, Integer> variableToIndex;
    private final List<List<Event>> eventsForThread;
    private final List<List<Set<Long>>> writeLocksPerInstruction;
    private final List<List<Set<Long>>> readLocksPerInstruction;
    private final List<ThreadLimits> threadLimits;

    static class ThreadLimits {
        // TODO(virgil): make these final.
        private final int startThreadIndex;
        private final int startEventIndex;

        private ThreadLimits(int startThreadIndex, int startEventIndex) {
            this.startThreadIndex = startThreadIndex;
            this.startEventIndex = startEventIndex;
        }

        private ThreadLimits() {
            this.startThreadIndex = -1;
            this.startEventIndex = -1;
        }

        @Override
        public String toString() {
            return "{thread=" + startThreadIndex + ", event=" + startEventIndex + "}";
        }

        int getStartThreadIndex() {
            return startThreadIndex;
        }

        int getStartEventIndex() {
            return startEventIndex;
        }
    }

    public ModelTrace(Trace trace) {
        threads = new ArrayList<>(trace.eventsByThreadID().keySet());
        threadToIndex = new HashMap<>();
        for (int i = 0; i < threads.size(); i++) {  // TODO: Refactor.
            threadToIndex.put(threads.get(i), i);
        }
        variables = extractVariables(trace);
        variableToIndex = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            variableToIndex.put(variables.get(i), i);
        }
        eventsForThread = new ArrayList<>();
        writeLocksPerInstruction = new ArrayList<>();
        readLocksPerInstruction = new ArrayList<>();
        threads.forEach(threadId -> {
            List<Event> threadEvents = trace.getEvents(threadId);
            threadEvents = threadEvents.stream()
                    .filter(event -> !event.isReadOrWrite()
                            || variables.contains(event.getAddr()))
                    .collect(Collectors.toList());
            eventsForThread.add(threadEvents);
            writeLocksPerInstruction.add(
                    computeWriteLocksPerInstruction(threadEvents, trace.getHeldLocksAt(threadEvents.get(0))));
            readLocksPerInstruction.add(
                    computeReadLocksPerInstruction(threadEvents, trace.getHeldLocksAt(threadEvents.get(0))));
        });
        threadLimits = computeThreadLimits(eventsForThread, threadToIndex);
    }

    Integer getThreadIndex(long threadId) {
        return threadToIndex.get(threadId);
    }

    int getEventCount(Integer threadIndex) {
        return eventsForThread.get(threadIndex).size();
    }

    Integer getVariableIndexOrNull(long addr) {
        return variableToIndex.getOrDefault(addr, null);
    }

    boolean isReadLocked(int threadIndex, int eventIndex, long addr) {
        return readLocksPerInstruction.get(threadIndex).get(eventIndex).contains(addr);
    }

    boolean isWriteLocked(int threadIndex, int eventIndex, long addr) {
        return writeLocksPerInstruction.get(threadIndex).get(eventIndex).contains(addr);
    }

    int getVariableCount() {
        return variables.size();
    }

    int getThreadCount() {
        return threads.size();
    }

    Event getEvent(int threadIndex, int eventIndex) {
        return eventsForThread.get(threadIndex).get(eventIndex);
    }

    ThreadLimits getThreadLimits(int threadIndex) {
        return threadLimits.get(threadIndex);
    }

    Event[] computeFirstEventPerVariable() {
        Event[] firstEvents = new Event[variables.size()];
        eventsForThread.forEach(events ->
                events.stream()
                        .filter(Event::isReadOrWrite)
                        .forEach(event -> {
                            Integer variableIndex = variableToIndex.getOrDefault(event.getAddr(), null);
                            if (variableIndex == null) {
                                return;
                            }
                            Event first = firstEvents[variableIndex];
                            if (first == null || event.getGID() < first.getGID()) {
                                firstEvents[variableIndex] = event;
                            }
                        }));
        return firstEvents;
    }

    List<Set<Long>> computeAllValuesPerVariable() {
        List<Set<Long>> allValuesForVariable = new ArrayList<>();
        variables.forEach(events -> allValuesForVariable.add(new HashSet<>()));
        eventsForThread.forEach(events ->
                events.stream()
                        .filter(Event::isReadOrWrite)
                        .forEach(event -> {
                            Integer variableIndex = variableToIndex.getOrDefault(event.getAddr(), null);
                            if (variableIndex == null) {
                                return;
                            }
                            allValuesForVariable.get(variableIndex).add(event.getValue());
                        }));
        return allValuesForVariable;
    }

    private static List<Long> extractVariables(Trace trace) {
        Map<Long, Integer> variableToThreadCount = new HashMap<>();
        trace.eventsByThreadID().values().forEach(events -> {
            Set<Long> variablesInThread = new HashSet<>();
            events.forEach(event -> {
                if (event.isReadOrWrite()) {
                    variablesInThread.add(event.getAddr());
                }
            });
            variablesInThread.forEach(addr -> variableToThreadCount.merge(addr, 1, (v1, v2) -> v1 + v2));
        });
        return variableToThreadCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static List<Set<Long>> computeWriteLocksPerInstruction(List<Event> events, List<Event> heldLocks) {
        return computeLocksPerInstruction(events, Event::isWriteLock, Event::isWriteUnlock, heldLocks);
    }

    private static List<Set<Long>> computeReadLocksPerInstruction(List<Event> events, List<Event> heldLocks) {
        return computeLocksPerInstruction(events, Event::isReadLock, Event::isReadUnlock, heldLocks);
    }

    private static List<Set<Long>> computeLocksPerInstruction(
            List<Event> events, Predicate<Event> isLock, Predicate<Event> isUnlock, List<Event> heldLocks) {
        Set<Long> locks = new HashSet<>();
        for (Event event : heldLocks) {
            if (isLock.test(event)) {
                locks.add(event.getSyncObject());
            }
        }
        List<Set<Long>> locksPerInstruction = new ArrayList<>(events.size());
        for (Event event : events) {
            locksPerInstruction.add(locks);
            if (isLock.test(event)) {
                locks = new HashSet<>(locks);
                locks.add(event.getSyncObject());
            }
            if (isUnlock.test(event)) {
                locks = new HashSet<>(locks);
                locks.remove(event.getSyncObject());
            }
        }
        locksPerInstruction.add(locks);
        return locksPerInstruction;
    }

    private static List<ThreadLimits> computeThreadLimits(
            List<List<Event>> eventsForThread, Map<Long, Integer> threadToIndex) {
        List<ThreadLimits> limits = new ArrayList<>();
        eventsForThread.forEach(e -> limits.add(new ThreadLimits()));
        for (int i = 0; i < eventsForThread.size(); i++) {
            List<Event> currentEvents = eventsForThread.get(i);
            for (int j = 0; j < currentEvents.size(); j++) {
                Event event = currentEvents.get(j);
                if (event.isStart()) {
                    Integer threadIndex = threadToIndex.get(event.getSyncedThreadId());
                    if (threadIndex == null) {
                        // TODO: Why does this happen?
                        continue;
                    }
                    limits.set(threadIndex, new ThreadLimits(i, j));
                }
            }
        }
        return limits;
    }
}
