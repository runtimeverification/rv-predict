package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MaximalCausalModel {
    private final Trace trace;
    private final com.runtimeverification.rvpredict.config.Configuration globalConfiguration;
    private final boolean[] noReadOrWrite;
    private final EventStepper eventStepper;
    private final ModelTrace modelTrace;

    public static MaximalCausalModel create(
            Trace trace,
            com.runtimeverification.rvpredict.config.Configuration globalConfiguration,
            EventStepper eventStepper,
            ModelTrace modelTrace) {
        return new MaximalCausalModel(trace, globalConfiguration, eventStepper, modelTrace);
    }

    private MaximalCausalModel(
            Trace trace,
            com.runtimeverification.rvpredict.config.Configuration globalConfiguration,
            EventStepper eventStepper,
            ModelTrace modelTrace) {
        this.trace = trace;
        this.globalConfiguration = globalConfiguration;
        this.eventStepper = eventStepper;
        this.modelTrace = modelTrace;
        // System.out.println(threadLimits);
        noReadOrWrite = new boolean[modelTrace.getVariableCount()];
        // System.out.println(eventsForThread);
    }

    private class ProcessingQueue {
        private final Queue<Configuration> toProcess = new ArrayDeque<>();
        private final Set<Configuration> existingConfigurations = ConcurrentHashMap.newKeySet();
        private volatile int activeProducerConsumers = 0;

        private void registerProducerConsumer() {
            synchronized (toProcess) {
                activeProducerConsumers++;
            }
        }

        private void add(Configuration configuration) {
            if (existingConfigurations.add(configuration)) {
                synchronized (toProcess) {
                    toProcess.add(configuration);
                    toProcess.notify();
                }
            }
        }

        private Configuration remove() {
            synchronized (toProcess) {
                while (toProcess.isEmpty()) {
                    activeProducerConsumers--;
                    if (activeProducerConsumers == 0) {
                        toProcess.notify();
                        return null;
                    }
                    try {
                        toProcess.wait();
                    } catch (InterruptedException e) {
                    }
                    activeProducerConsumers++;
                }
                return toProcess.remove();
            }
        }

        public int getCount() {
            return existingConfigurations.size();
        }
    }

    public Map<String, Race> findRaces() {
        ProcessingQueue toProcess = new ProcessingQueue();
        Map<String, Race> races = new HashMap<>();

        toProcess.add(new Configuration(modelTrace.getThreadCount(), computeInitialVariableValues()));

        /*
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            Thread thread = new Thread(() -> {
            */
                toProcess.registerProducerConsumer();
                for (Configuration configuration = toProcess.remove();
                     configuration != null;
                     configuration = toProcess.remove()) {
                    addRaces(configuration, races);

                    List<ConfigurationWithEvent> expandedConfigurations = expandOptimized(configuration);
                    expandedConfigurations.forEach(expanded -> {
                        Configuration expandedConfiguration = expanded.getConfiguration();
                        toProcess.add(expandedConfiguration);
                    });
                }
                /*
            });
            thread.start();
            threads.add(thread);
        }
        boolean interrupted;
        do {
            interrupted = false;
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } while (interrupted);*/
        return races;
    }

    private long[] computeInitialVariableValues() {
        Event[] firstEvents = modelTrace.computeFirstEventPerVariable();
        List<Set<Long>> allValues = modelTrace.computeAllValuesPerVariable();
        long [] initialValues = new long[firstEvents.length];
        for (int i = 0; i < initialValues.length; i++) {
            Event first = firstEvents[i];
            if (first.isWrite()) {
                initialValues[i] = getUnusedValue(allValues.get(i));
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

    private void addRaces(Configuration configuration, Map<String, Race> races) {
        List<Event> accesibleEvents = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < configuration.getThreadCount(); threadIndex++) {
            int eventIndex = configuration.getEventIndex(threadIndex);
            if (eventIndex == 0 && !threadCanStart(threadIndex, configuration)) {
                continue;
            }
            if (eventIndex >= modelTrace.getEventCount(threadIndex)) {
                continue;
            }
            Event event = modelTrace.getEvent(threadIndex, eventIndex);
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
                String signature = raceSignature(e1, e2);
                if (races.containsKey(signature)) {
                    continue;
                }
                races.computeIfAbsent(signature, s -> new Race(e1, e2, trace, globalConfiguration));
            }
        }
    }

    private String raceSignature(Event e1, Event e2) {
        int addr = Math.min(0, e1.getFieldIdOrArrayIndex()); // collapse all array indices to 0
        int loc1 = Math.min(e1.getLocId(), e2.getLocId());
        int loc2 = Math.max(e1.getLocId(), e2.getLocId());
        return "Race(" + addr + "," + loc1 + "," + loc2 + ")";
    }

    private class ConfigurationWithPartialExecution {
        private final Configuration configuration;
        private final Event event;
        // TODO(virgil): is it better to use a set?
        private final boolean[] readVariables;
        private final boolean[] writtenVariables;

        private ConfigurationWithPartialExecution(
                Configuration configuration, Event event, boolean[] readVariables, boolean[] writtenVariables) {
            this.configuration = configuration;
            this.event = event;
            this.readVariables = readVariables;
            this.writtenVariables = writtenVariables;
        }

        @Override
        public String toString() {
            return "{configuration=" + configuration.toString()
                    + ", event=" + event.toString()
                    + ", read=" + Arrays.toString(readVariables)
                    + ", written=" + Arrays.toString(writtenVariables) + "}";
        }
    }

    private List<ConfigurationWithEvent> expandOptimized(Configuration configuration) {
        // System.out.println("here");
        int[] breakPoints = new int[configuration.getThreadCount()];
        Arrays.fill(breakPoints, -1);
        ConfigurationWithPartialExecution[] configurations =
                expandConfigurationsWithMultipleStepsOnAThread(configuration, breakPoints);
        // System.out.println(Arrays.toString(configurations));
        boolean[] variableHasRace = new boolean[modelTrace.getVariableCount()];
        boolean racesExist = detectRaces(configurations, variableHasRace);
        if (!racesExist) {
            // Quick exit on the most common case.
            return toConfigurationsWithEvent(configurations);
        }
        for (int threadIndex = 0; threadIndex < configurations.length; threadIndex++) {
            ConfigurationWithPartialExecution expandedConfiguration = configurations[threadIndex];
            if (expandedConfiguration == null) {
                continue;
            }
            int startIndex = configuration.getEventIndex(threadIndex);
            int endIndex = Math.min(
                    // Should be fine to also look at the event on which we stopped. Looking at it helps with detecting
                    // races on undefined reads.
                    expandedConfiguration.configuration.getEventIndex(threadIndex) + 1,
                    modelTrace.getEventCount(threadIndex));
            for (int eventIndex = startIndex; eventIndex < endIndex; eventIndex++) {
                Event event = modelTrace.getEvent(threadIndex, eventIndex);
                if (event.isReadOrWrite() && variableHasRace[modelTrace.getVariableIndexOrNull(event.getAddr())]) {
                    breakPoints[threadIndex] = eventIndex;
                    break;
                }
            }
        }
        // TODO(virgil): This second call can be optimized, a lot of the things done here are not needed.
        configurations =
                expandConfigurationsWithMultipleStepsOnAThread(configuration, breakPoints);
        return toConfigurationsWithEvent(configurations);
    }

    private boolean detectRaces(ConfigurationWithPartialExecution[] configurations, boolean[] variableHasRace) {
        boolean racesExist = false;
        for (int i = 0; i < modelTrace.getVariableCount(); i++) {
            boolean readOnAPastThread = false;
            boolean writtenOnAPastThread = false;
            for (ConfigurationWithPartialExecution expandedConfiguration : configurations) {
                if (expandedConfiguration == null) {
                    continue;
                }
                if (expandedConfiguration.writtenVariables[i]) {
                    if (readOnAPastThread || writtenOnAPastThread) {
                        variableHasRace[i] = true;
                        racesExist = true;
                        break;
                    }
                } else if (expandedConfiguration.readVariables[i]) {
                    if (writtenOnAPastThread) {
                        variableHasRace[i] = true;
                        racesExist = true;
                        break;
                    }
                }
                writtenOnAPastThread |= expandedConfiguration.writtenVariables[i];
                readOnAPastThread |= expandedConfiguration.readVariables[i];
            }
        }
        return racesExist;
    }

    private List<ConfigurationWithEvent> toConfigurationsWithEvent(ConfigurationWithPartialExecution[] configurations) {
        List<ConfigurationWithEvent> finalConfigurations = new ArrayList<>();
        for (ConfigurationWithPartialExecution expandedConfiguration : configurations) {
            if (expandedConfiguration == null) {
                continue;
            }
            finalConfigurations.add(
                    new ConfigurationWithEvent(expandedConfiguration.configuration, expandedConfiguration.event));
        }
        return finalConfigurations;
    }

    private ConfigurationWithPartialExecution[] expandConfigurationsWithMultipleStepsOnAThread(
            Configuration configuration, int[] breakPoints) {
        ConfigurationWithPartialExecution[] finalConfigurations =
                new ConfigurationWithPartialExecution[configuration.getThreadCount()];
        for (int threadIndex = 0; threadIndex < configuration.getThreadCount(); threadIndex++) {
            int eventIndex = configuration.getEventIndex(threadIndex);
            if (eventIndex >= modelTrace.getEventCount(threadIndex)) {
                continue;
            }
            if (eventIndex == 0 && !threadCanStart(threadIndex, configuration)) {
                continue;
            }
            Event event = modelTrace.getEvent(threadIndex, eventIndex);
            if (isSynchronizationEvent(event) || breakPoints[threadIndex] == eventIndex) {
                Configuration expandedConfiguration = eventStepper.expandWithEvent(configuration, threadIndex, event);
                if (expandedConfiguration != null) {
                    // It's ok to use noReadOrWrite even when this is a break point because then we know that
                    // this is a possible race, we don't need to detect it again.
                    finalConfigurations[threadIndex] =
                            new ConfigurationWithPartialExecution(
                                    expandedConfiguration, event, noReadOrWrite, noReadOrWrite);
                }
                continue;
            }
            Event lastEvent = event;
            Configuration lastConfiguration = configuration;
            // TODO: One can skip allocation and fill these.
            boolean[] written = new boolean[modelTrace.getVariableCount()];
            boolean[] read = new boolean[modelTrace.getVariableCount()];
            while (!isSynchronizationEvent(event)) {
                // System.out.println(eventIndex + " -> " + event);
                Configuration expandedConfiguration = eventStepper.expandWithEvent(lastConfiguration, threadIndex, event);
                // This needs to be before the expandedConfiguration test below.
                if (event.isWrite()) {
                    int variableIndex = modelTrace.getVariableIndexOrNull(event.getAddr());
                    written[variableIndex] = true;
                } else if (event.isRead()) {
                    int variableIndex = modelTrace.getVariableIndexOrNull(event.getAddr());
                    read[variableIndex] = true;
                }
                if (expandedConfiguration == null) {
                    break;
                }
                lastEvent = event;
                lastConfiguration = expandedConfiguration;
                eventIndex = expandedConfiguration.getEventIndex(threadIndex);
                if (eventIndex == breakPoints[threadIndex]) {
                    lastConfiguration = expandedConfiguration;
                    break;
                }
                if (eventIndex >= modelTrace.getEventCount(threadIndex)) {
                    break;
                }
                event = modelTrace.getEvent(threadIndex, eventIndex);
            }
            // if (lastConfiguration != configuration) {  // Intentional pointer comparison.
                finalConfigurations[threadIndex] =
                        new ConfigurationWithPartialExecution(lastConfiguration, lastEvent, read, written);
            // }
        }
        return finalConfigurations;
    }

    private boolean isSynchronizationEvent(Event event) {
        return event.isLock() || event.isUnlock() || event.isFork() || event.isJoin() || event.isStart();
    }

    private List<ConfigurationWithEvent> expand(Configuration configuration) {
        List<ConfigurationWithEvent> expanded = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < modelTrace.getThreadCount(); threadIndex++) {
            int eventIndex = configuration.getEventIndex(threadIndex);
            if (eventIndex >= modelTrace.getEventCount(threadIndex)) {
                continue;
            }
            if (eventIndex == 0 && !threadCanStart(threadIndex, configuration)) {
                continue;
            }
            Event event = modelTrace.getEvent(threadIndex, eventIndex);
            Configuration expandedConfiguration = eventStepper.expandWithEvent(configuration, threadIndex, event);
            if (expandedConfiguration == null) {
                continue;
            }
            expanded.add(new ConfigurationWithEvent(expandedConfiguration, event));
        }
        return expanded;
    }

    private boolean threadCanStart(int threadIndex, Configuration configuration) {
        ModelTrace.ThreadLimits limits = modelTrace.getThreadLimits(threadIndex);
        return limits.getStartThreadIndex() < 0
                || limits.getStartEventIndex() < configuration.getEventIndex(limits.getStartThreadIndex());
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
}
