package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.violation.FoundRace;
import com.runtimeverification.rvpredict.violation.SkippedRace;

import java.util.HashMap;
import java.util.Map;

public class RaceCheckResult {
    private final Map<String, FoundRace> allRaces = new HashMap<>();
    private final Map<String, FoundRace> newRaces = new HashMap<>();
    private final Map<String, SkippedRace> timeoutSkipped = new HashMap<>();


    public Map<String, FoundRace> getNewRaces() {
        return newRaces;
    }

    public Map<String, SkippedRace> getTimeoutRaces() {
        return timeoutSkipped;
    }

    public void startNewWindow() {
        newRaces.clear();
    }

    public boolean hadRace(String name) {
        return allRaces.containsKey(name);
    }

    void race(String name, FoundRace race) {
        newRaces.put(name, race);
        allRaces.put(name, race);
    }

    void timeout(String name, String timerName, SkippedRace race) {
        timeoutSkipped.computeIfAbsent(name, k -> race).increment(timerName);
    }
}
