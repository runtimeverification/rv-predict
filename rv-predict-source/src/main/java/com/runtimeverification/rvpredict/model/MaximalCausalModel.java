package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.*;
import java.util.function.Predicate;

/**
 * Created by virgil on 14.03.2017.
 */
public class MaximalCausalModel {
    private final Trace trace;
    private final com.runtimeverification.rvpredict.config.Configuration globalConfiguration;
    private final List<Long> threads;  // TODO: Make it local.
    private final Map<Long, Integer> threadToIndex;
    private final List<Long> variables;
    private final Map<Long, Integer> variableToIndex;
    private final List<List<Event>> events;
    private final List<List<Set<Long>>> writeLocksPerInstruction;
    private final List<List<Set<Long>>> readLocksPerInstruction;
    private final List<ThreadLimits> threadLimits;

    public static MaximalCausalModel create(
            Trace trace, com.runtimeverification.rvpredict.config.Configuration globalConfiguration) {
        MaximalCausalModel model = new MaximalCausalModel(trace, globalConfiguration);
        return model;
    }

    public MaximalCausalModel(
            Trace trace, com.runtimeverification.rvpredict.config.Configuration globalConfiguration) {
        this.trace = trace;
        this.globalConfiguration = globalConfiguration;
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
        events = new ArrayList<>();
        writeLocksPerInstruction = new ArrayList<>();
        readLocksPerInstruction = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < threads.size(); threadIndex++) {
            List<Event> threadEvents = trace.getEvents(threads.get(threadIndex));
            events.add(threadEvents);
            writeLocksPerInstruction.add(computeWriteLocksPerInstruction(threadEvents));
            readLocksPerInstruction.add(computeReadLocksPerInstruction(threadEvents));
        }
        threadLimits = computeThreadLimits();
    }

    public Map<String, Race> findRaces() {
        Queue<Configuration> toProcess = new ArrayDeque<>();
        Set<Configuration> configurations = new HashSet<>();
        Map<String, Race> races = new HashMap<>();

        toProcess.add(new Configuration(threads.size(), variables.size()));
        long count = 0;
        while (!toProcess.isEmpty()) {
            count++;
            Configuration configuration = toProcess.remove();
            addRaceIfNeeded(configuration, races);
            expand(configuration).forEach(expanded -> {
                if (configurations.contains(expanded)) {
                    return;
                }
                configurations.add(expanded);
                toProcess.add(expanded);
            });
        }
        System.out.println("Count=" + count);
        return races;
    }

    // TODO: This should be static.
    private List<ThreadLimits> computeThreadLimits() {
        List<ThreadLimits> limits = new ArrayList<>();
        events.forEach(e -> limits.add(new ThreadLimits()));
        for (int i = 0; i < events.size(); i++) {
            List<Event> currentEvents = events.get(i);
            for (int j = 0; j < currentEvents.size(); j++) {
                Event event = currentEvents.get(j);
                if (event.isStart()) {
                    limits.get(threadToIndex.get(event.getSyncedThreadId())).setStart(i, j);
                    System.out.println("tti[" + threadToIndex.get(event.getSyncedThreadId()) + "]=" + i + ", "+ j);
                }
            }
        }
        return limits;
    }

    private void addRaceIfNeeded(Configuration configuration, Map<String, Race> races) {
        for (int i = 0; i < configuration.getEventIndexes().length; i++) {
            int eventIndex1 = configuration.getEventIndexes()[i];
            if (eventIndex1 == 0 || eventIndex1 >= events.get(i).size()) {
                continue;
            }
            Event e1 = events.get(i).get(eventIndex1 - 1);
            for (int j = i + 1; j < configuration.getEventIndexes().length; j++) {
                int eventIndex2 = configuration.getEventIndexes()[j];
                if (eventIndex2 == 0 || eventIndex2 > events.get(j).size()) {
                    continue;
                }
                Event e2 = events.get(j).get(eventIndex2 - 1);
                if (!e1.isWrite() && !e2.isWrite()) {
                    continue;
                }
                if (!e1.isReadOrWrite() || !e2.isReadOrWrite()) {
                    continue;
                }
                if (e1.getAddr() != e2.getAddr()) {
                    continue;
                }
                String signature = i + ":" + eventIndex1 + ":" + j + ":" + eventIndex2;
                if (races.containsKey(signature)) {
                    continue;
                }
                System.out.println(signature + " -> " + e1 + " vs " + e2);
                races.computeIfAbsent(signature, s -> new Race(e1, e2, trace, globalConfiguration));
            }
        }
    }

    private List<Configuration> expand(Configuration configuration) {
        List<Configuration> expanded = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            int eventIndex = configuration.eventIndexes[i];
            if (eventIndex >= events.get(i).size()) {
                continue;
            }
            if (eventIndex == 0 && !threadCanStart(i, configuration)) {
                continue;
            }
            Configuration expandedConfiguration = expandWithEvent(configuration, i, events.get(i).get(eventIndex));
            if (expandedConfiguration == null) {
                continue;
            }
            expanded.add(expandedConfiguration);
        }
        return expanded;
    }

    private boolean threadCanStart(int threadIndex, Configuration configuration) {
        ThreadLimits limits = threadLimits.get(threadIndex);
        if (limits.startThreadIndex < 0) {
            return true;
        }
        return limits.startEventIndex < configuration.getEventIndex(limits.startThreadIndex);
    }

    private Configuration expandWithEvent(Configuration configuration, int threadIndex, Event event) {
        if (event.isRead()) {
            return expandWithRead(configuration, threadIndex, event.getAddr(), event.getValue());
        }
        if (event.isWrite()) {
            return expandWithWrite(configuration, threadIndex, event.getAddr(), event.getValue());
        }
        if (event.isWriteLock()) {
            return expandWithWriteLock(configuration, threadIndex, event.getAddr());
        }
        if (event.isReadLock()) {
            return expandWithReadLock(configuration, threadIndex, event.getAddr());
        }
        if (event.isJoin()) {
            return expandWithJoin(configuration, threadIndex, event.getSyncedThreadId());
        }
        return expandWithGenericEvent(configuration, threadIndex);
    }

    private Configuration expandWithJoin(Configuration configuration, int threadIndex, long joinedThread) {
        int joinedThreadIndex = threadToIndex.get(joinedThread);
        if (configuration.getEventIndex(joinedThreadIndex) >= events.get(joinedThreadIndex).size()) {
            return expandWithGenericEvent(configuration, threadIndex);
        }
        return null;
    }

    private Configuration expandWithWriteLock(Configuration configuration, int threadIndex, long addr) {
        for (int i = 0; i < threads.size(); i++) {
            if (i == threadIndex) {
                continue;
            }
            if (isReadLocked(i, configuration.getEventIndex(i), addr) ||
                    isWriteLocked(i, configuration.getEventIndex(i), addr)) {
                return null;
            }
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithReadLock(Configuration configuration, int threadIndex, long addr) {
        for (int i = 0; i < threads.size(); i++) {
            if (i == threadIndex) {
                continue;
            }
            if (isWriteLocked(i, configuration.getEventIndex(i), addr)) {
                return null;
            }
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithRead(Configuration configuration, int threadIndex, long addr, long value) {
        Integer variableIndex = variableToIndex.getOrDefault(addr, null);  // TODO: replace addresses with indexes.
        if (variableIndex == null || configuration.getValue(variableIndex) != value) {
            return null;
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithWrite(Configuration configuration, int threadIndex, long addr, long value) {
        Integer variableIndex = variableToIndex.getOrDefault(addr, null);  // TODO: replace addresses with indexes.
        if (variableIndex == null) {
            return null;
        }
        return configuration.clone().advanceThread(threadIndex).setValue(variableIndex, value);
    }

    private Configuration expandWithGenericEvent(Configuration configuration, int threadIndex) {
        return configuration.clone().advanceThread(threadIndex);
    }

    private boolean isReadLocked(int threadIndex, int eventIndex, long addr) {
        return readLocksPerInstruction.get(threadIndex).get(eventIndex).contains(addr);
    }

    private boolean isWriteLocked(int threadIndex, int eventIndex, long addr) {
        return writeLocksPerInstruction.get(threadIndex).get(eventIndex).contains(addr);
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
            variablesInThread.forEach(addr -> variableToThreadCount.compute(addr, (k, v) -> v == null ? 1 : v + 1));
        });
        return variableToThreadCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static List<Set<Long>> computeWriteLocksPerInstruction(List<Event> events) {
        return computeLocksPerInstruction(events, event -> event.isWriteLock(), event -> event.isWriteUnlock());
    }

    private static List<Set<Long>> computeReadLocksPerInstruction(List<Event> events) {
        return computeLocksPerInstruction(events, event -> event.isReadLock(), event -> event.isReadUnlock());
    }

    private static List<Set<Long>> computeLocksPerInstruction(
            List<Event> events, Predicate<Event> isLock, Predicate<Event> isUnlock) {
        Set<Long> locks = new HashSet<>();
        List<Set<Long>> locksPerInstruction = new ArrayList<>(events.size());
        for (Event event : events) {
            if (isLock.test(event)) {
                locks = new HashSet<>(locks);
                locks.add(event.getAddr());
            }
            if (isUnlock.test(event)) {
                locks = new HashSet<>(locks);
                locks.remove(event.getAddr());
            }
            locksPerInstruction.add(locks);
        }
        return locksPerInstruction;
    }

    private static class Configuration {
        private final int [] eventIndexes;
        private final long [] variableValues;

        private Configuration(int threadCount, int variableCount) {
            eventIndexes = new int[threadCount];
            variableValues = new long[variableCount];
        }

        private Configuration(int[] threadIndexes, long[] variableValues) {
            this.eventIndexes = threadIndexes;
            this.variableValues = variableValues;
        }

        protected Configuration clone() {
            return new Configuration(eventIndexes.clone(), variableValues.clone());
        }

        private Configuration advanceThread(int threadIndex) {
            this.eventIndexes[threadIndex]++;
            return this;
        }

        public int getEventIndex(int threadIndex) {
            return eventIndexes[threadIndex];
        }

        public long getValue(int variableIndex) {
            return variableValues[variableIndex];
        }

        public Configuration setValue(int variableIndex, long value) {
            variableValues[variableIndex] = value;
            return this;
        }

        public int[] getEventIndexes() {
            return eventIndexes;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(eventIndexes) ^ Arrays.hashCode(variableValues);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Configuration)) {
                return false;
            }
            Configuration configuration = (Configuration)o;

            return Arrays.equals(eventIndexes, configuration.eventIndexes)
                    && Arrays.equals(variableValues, configuration.variableValues);
        }
    }

    private static class ThreadLimits {
        private int startThreadIndex = -1;
        private int startEventIndex = -1;
        private int endThreadIndex = -1;  // TODO: Delete.
        private int endEventIndex = -1;  // TODO: Delete.

        private void setStart(int threadIndex, int eventIndex) {
            startThreadIndex = threadIndex;
            startEventIndex = eventIndex;
        }
        private void setEnd(int threadIndex, int eventIndex) {
            endThreadIndex = threadIndex;
            endEventIndex = eventIndex;
        }
    }
}
