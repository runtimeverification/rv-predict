package com.runtimeverification.rvpredict.progressindicator;

import java.util.List;

public class NullProgressIndicator implements ProgressIndicatorInterface {
    @Override
    public void startComputation(List<Integer> raceCounts) {
    }

    @Override
    public void endWindow(long filePosition) {
    }

    @Override
    public void startRace(int raceIndex) {
    }

    @Override
    public void noRaceFound() {
    }

    @Override
    public void raceFound() {
    }

    @Override
    public void startRaceAttempt() {
    }

    @Override
    public void finishRaceAttempt() {
    }
}
