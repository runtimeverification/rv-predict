package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class MaximalCausalModel {
    private final Trace trace;
    private final com.runtimeverification.rvpredict.config.Configuration globalConfiguration;
    private final List<Long> threads;  // TODO: Make it local.
    private final Map<Long, Integer> threadToIndex;
    private final List<Long> variables;
    private final Map<Long, Integer> variableToIndex;
    private final List<List<Event>> eventsForThread;
    private final List<List<Set<Long>>> writeLocksPerInstruction;
    private final List<List<Set<Long>>> readLocksPerInstruction;
    private final List<ThreadLimits> threadLimits;

    public static MaximalCausalModel create(
            Trace trace, com.runtimeverification.rvpredict.config.Configuration globalConfiguration) {
        return new MaximalCausalModel(trace, globalConfiguration);
    }

    private MaximalCausalModel(
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
        eventsForThread = new ArrayList<>();
        writeLocksPerInstruction = new ArrayList<>();
        readLocksPerInstruction = new ArrayList<>();
        threads.forEach(threadId -> {
            List<Event> threadEvents = trace.getEvents(threadId);
            eventsForThread.add(threadEvents);
            writeLocksPerInstruction.add(
                    computeWriteLocksPerInstruction(threadEvents, trace.getHeldLocksAt(threadEvents.get(0))));
            readLocksPerInstruction.add(
                    computeReadLocksPerInstruction(threadEvents, trace.getHeldLocksAt(threadEvents.get(0))));
        });
        threadLimits = computeThreadLimits();
    }

    public Map<String, Race> findRaces() {
        Queue<Configuration> toProcess = new ArrayDeque<>();
        Set<Configuration> configurations = new HashSet<>();
        Map<String, Race> races = new HashMap<>();

        toProcess.add(new Configuration(threads.size(), computeInitialVariableValues()));
        long count = 0;
        while (!toProcess.isEmpty()) {
            count++;
            Configuration configuration = toProcess.remove();
            addRaces(configuration, races);

            List<ConfigurationWithEvent> expandedConfigurations = expand(configuration);
            expandedConfigurations.forEach(expanded -> {
                Configuration expandedConfiguration = expanded.getConfiguration();
                if (configurations.contains(expandedConfiguration)) {
                    return;
                }
                configurations.add(expandedConfiguration);
                toProcess.add(expandedConfiguration);
            });
        }
        System.out.println("Count=" + count);
        return races;
    }

    private long[] computeInitialVariableValues() {
        Event[] firstEventForVariable = new Event[variables.size()];
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
                        Event first = firstEventForVariable[variableIndex];
                        if (first == null || event.getGID() < first.getGID()) {
                            firstEventForVariable[variableIndex] = event;
                        }
                    }));
        long [] initialValues = new long[variables.size()];
        for (int i = 0; i < initialValues.length; i++) {
            Event first = firstEventForVariable[i];
            if (first.isWrite()) {
                initialValues[i] = getUnusedValue(allValuesForVariable.get(i));
            } else {
                initialValues[i] = first.getValue();
            }
        }
        return initialValues;
    }

    private long getUnusedValue(Set<Long> usedValues) {
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (usedValues.contains(value));
        return value;
    }

    // TODO: This should be static.
    private List<ThreadLimits> computeThreadLimits() {
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
                        System.out.println("Thread " + event.getSyncedThreadId() + " not found.");
                        continue;
                    }
                    limits.get(threadIndex).setStart(i, j);
                }
            }
        }
        return limits;
    }

    private void addRaces(Configuration configuration, Map<String, Race> races) {
        List<Event> accesibleEvents = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < threads.size(); threadIndex++) {
            int eventIndex = configuration.getEventIndex(threadIndex);
            if (eventIndex == 0 && !threadCanStart(threadIndex, configuration)) {
                continue;
            }
            if (eventIndex >= eventsForThread.get(threadIndex).size()) {
                continue;
            }
            Event event = eventsForThread.get(threadIndex).get(eventIndex);
            if (!event.isReadOrWrite()) {
                continue;
            }
            accesibleEvents.add(event);
        }
        for (int i = 0; i < accesibleEvents.size(); i++) {
            Event e1 = accesibleEvents.get(i);
            for (int j = i + 1; j < accesibleEvents.size(); j++) {
                Event e2 = accesibleEvents.get(j);
                if (e1.getTID() == e2.getTID()
                        || e1.getAddr() != e2.getAddr()
                        || (!e1.isWrite() && !e2.isWrite())) {
                    continue;
                }
                String signature = e1.getGID() + ":" + e2.getGID();
                if (races.containsKey(signature)) {
                    continue;
                }
                races.computeIfAbsent(signature, s -> new Race(e1, e2, trace, globalConfiguration));
            }
        }
    }
    /*
    private void addRaces(List<ConfigurationWithEvent> configurations, Map<String, Race> races) {
        List<Event> newEvents = configurations.stream()
                .map(ConfigurationWithEvent::getEvent)
                .filter(Event::isReadOrWrite)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (int i = 0; i < newEvents.size(); i++) {
            Event e1 = newEvents.get(i);
            for (int j = i + 1; j < newEvents.size(); j++) {
                Event e2 = newEvents.get(j);
                if (e1.getTID() == e2.getTID()
                        || e1.getAddr() != e2.getAddr()
                        || (!e1.isWrite() && !e2.isWrite())) {
                    continue;
                }
                String signature = e1.getGID() + ":" + e2.getGID();
                if (races.containsKey(signature)) {
                    continue;
                }
                System.out.println(signature + " -> " + e1 + " vs " + e2);
                races.computeIfAbsent(signature, s -> new Race(e1, e2, trace, globalConfiguration));
            }
        }
    }
    */

    private List<ConfigurationWithEvent> expand(Configuration configuration) {
        if (globalConfiguration.debug) {
            System.out.println("Expanding: " + configuration);
        }
        List<ConfigurationWithEvent> expanded = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < eventsForThread.size(); threadIndex++) {
            if (globalConfiguration.debug) {
                System.out.print("Thread: " + threadIndex + " ");
            }
            int eventIndex = configuration.getEventIndex(threadIndex);
            if (eventIndex >= eventsForThread.get(threadIndex).size()) {
                if (globalConfiguration.debug) {
                    System.out.println("No more events.");
                }
                continue;
            }
            if (eventIndex == 0 && !threadCanStart(threadIndex, configuration)) {
                if (globalConfiguration.debug) {
                    System.out.println("Thread can't start.");
                }
                continue;
            }
            Event event = eventsForThread.get(threadIndex).get(eventIndex);
            if (globalConfiguration.debug) {
                System.out.print(event + " ");
            }
            Configuration expandedConfiguration = expandWithEvent(configuration, threadIndex, event);
            if (expandedConfiguration == null) {
                if (globalConfiguration.debug) {
                    System.out.println("Can't expand configuration.");
                }
                continue;
            }
            if (globalConfiguration.debug) {
                System.out.println("Expanded as " + new ConfigurationWithEvent(expandedConfiguration, event) + ".");
            }
            expanded.add(new ConfigurationWithEvent(expandedConfiguration, event));
        }
        if (globalConfiguration.debug) {
            System.out.println("expanded=" + expanded);
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
            return expandWithWriteLock(configuration, threadIndex, event.getSyncObject());
        }
        if (event.isReadLock()) {
            return expandWithReadLock(configuration, threadIndex, event.getSyncObject());
        }
        if (event.isJoin()) {
            return expandWithJoin(configuration, threadIndex, event.getSyncedThreadId());
        }
        return expandWithGenericEvent(configuration, threadIndex);
    }

    private Configuration expandWithJoin(Configuration configuration, int threadIndex, long joinedThread) {
        Integer joinedThreadIndex = threadToIndex.get(joinedThread);
        // The thread may not be found if all its events were in a previous window.
        //
        // I can't tell for sure right now if it can happen that the last instruction of a thread
        // is after the join for that thread. If that happens, then this last instruction may be in a
        // different window, which can't be handled correctly here.
        if (joinedThreadIndex == null ||
                configuration.getEventIndex(joinedThreadIndex) >= eventsForThread.get(joinedThreadIndex).size()) {
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

    private static class ConfigurationWithEvent {
        private final Configuration configuration;
        private final Event event;

        private ConfigurationWithEvent(Configuration configuration, Event event) {
            this.configuration = configuration;
            this.event = event;
        }

        private Configuration getConfiguration() {
            return configuration;
        }

        private Event getEvent() {
            return event;
        }

        @Override
        public String toString() {
            return "[" + configuration.toString() + "," + event.toString() + "]";
        }
    }

    private static class Configuration {
        private final int [] eventIndexes;
        private final long [] variableValues;

        private Configuration(int threadCount, long[] variableValues) {
            eventIndexes = new int[threadCount];
            this.variableValues = variableValues;
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

        private int getEventIndex(int threadIndex) {
            return eventIndexes[threadIndex];
        }

        private long getValue(int variableIndex) {
            return variableValues[variableIndex];
        }

        private Configuration setValue(int variableIndex, long value) {
            variableValues[variableIndex] = value;
            return this;
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

        @Override
        public String toString() {
            return "[eventIndexes=" + Arrays.toString(eventIndexes)
                    + ",variableValues=" + Arrays.toString(variableValues) + "]";
        }
    }

    private static class ThreadLimits {
        private int startThreadIndex = -1;
        private int startEventIndex = -1;

        private void setStart(int threadIndex, int eventIndex) {
            startThreadIndex = threadIndex;
            startEventIndex = eventIndex;
        }
    }
}
