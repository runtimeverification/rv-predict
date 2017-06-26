package com.runtimeverification.rvpredict.progressindicator;

import java.util.List;

public interface ProgressIndicatorInterface {
    void startComputation(List<Integer> raceCounts);
    void endWindow(long filePosition);
    void startRace(int raceIndex);
    void noRaceFound();
    void raceFound();
    void startRaceAttempt();
    void finishRaceAttempt();
}
