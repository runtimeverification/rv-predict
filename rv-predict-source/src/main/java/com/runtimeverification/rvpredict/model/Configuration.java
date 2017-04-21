package com.runtimeverification.rvpredict.model;

import java.util.Arrays;

public class Configuration {
    private final int[] eventIndexes;
    private final long[] variableValues;

    Configuration(int threadCount, long[] variableValues) {
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

    Configuration advanceThread(int threadIndex) {
        this.eventIndexes[threadIndex]++;
        return this;
    }

    int getEventIndex(int threadIndex) {
        return eventIndexes[threadIndex];
    }

    long getValue(int variableIndex) {
        return variableValues[variableIndex];
    }

    int getThreadCount() {
        return eventIndexes.length;
    }

    Configuration setValue(int variableIndex, long value) {
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
        Configuration configuration = (Configuration) o;

        return Arrays.equals(eventIndexes, configuration.eventIndexes)
                && Arrays.equals(variableValues, configuration.variableValues);
    }

    @Override
    public String toString() {
        return "[eventIndexes=" + Arrays.toString(eventIndexes)
                + ",variableValues=" + Arrays.toString(variableValues) + "]";
    }
}
