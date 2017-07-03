package com.runtimeverification.rvpredict.progressindicator;

import com.google.common.collect.ImmutableList;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

public class ProgressIndicator implements ProgressIndicatorInterface {
    private final ProgressIndicatorUI ui;
    private final Clock clock;
    private OneItemProgress smtTimeMillis;
    private OneItemProgress inputFile;
    private Optional<ComputationData> maybeComputationData;

    private final class ComputationData {
        private final ImmutableList<Integer> raceCounts;
        private OneItemProgress races;
        private OneItemProgress totalTasksProgress;
        private Optional<OneItemProgress> maybeCurrentRace;

        private ComputationData(List<Integer> raceCounts) {
            this.raceCounts = ImmutableList.copyOf(raceCounts);
            this.races = new OneItemProgress(this.raceCounts.size());
            this.totalTasksProgress =
                    new OneItemProgress(raceCounts.stream().mapToInt(counts -> counts).sum());
            this.maybeCurrentRace = Optional.of(new OneItemProgress(raceCounts.get(0)));
        }

        private void startRace(int raceIndex) {
            assert raceIndex == races.getDone();
            races = races.withProgress(raceIndex - races.getDone());
            maybeCurrentRace = Optional.of(new OneItemProgress(raceCounts.get(raceIndex)));
        }

        private void raceEnd() {
            races = races.withTaskDone();
            assert maybeCurrentRace.isPresent();
            OneItemProgress currentRace = maybeCurrentRace.get();
            long remainingTasks = currentRace.getTotal() - currentRace.getDone();
            currentRace.withProgress(remainingTasks);
            totalTasksProgress = totalTasksProgress.withProgress(remainingTasks);
        }

        private void finishRaceAttempt() {
            totalTasksProgress = totalTasksProgress.withTaskDone();
            assert maybeCurrentRace.isPresent();
            OneItemProgress currentRace = maybeCurrentRace.get();
            maybeCurrentRace = Optional.of(currentRace.withTaskDone());
        }
    }

    private long racesFound;

    private OptionalLong currentTaskUncountedTimeStart;

    public ProgressIndicator(long inputFileSize, int maximumSmtTimeSeconds, ProgressIndicatorUI ui, Clock clock) {
        this.ui = ui;
        this.clock = clock;
        this.inputFile = new OneItemProgress(inputFileSize);
        this.smtTimeMillis =
                new OneItemProgress(TimeUnit.SECONDS.toMillis(maximumSmtTimeSeconds));
        this.currentTaskUncountedTimeStart = OptionalLong.empty();
        this.racesFound = 0;
        this.maybeComputationData = Optional.empty();
    }

    public synchronized void startComputation(List<Integer> raceCounts) {
        this.currentTaskUncountedTimeStart = OptionalLong.empty();
        this.maybeComputationData = Optional.of(new ComputationData(raceCounts));
        reportUiState();
    }

    public synchronized void end() {
        reportUiState();
        ui.reportEnd();
    }
    public synchronized void endWindow(long filePosition) {
        inputFile = inputFile.withProgress(filePosition - inputFile.getDone());
        this.maybeComputationData = Optional.empty();
    }

    public synchronized void startRace(int raceIndex) {
        assert maybeComputationData.isPresent();
        ComputationData computationData = maybeComputationData.get();
        computationData.startRace(raceIndex);
        reportUiState();
    }

    public synchronized void noRaceFound() {
        raceEnd();
    }

    public synchronized void raceFound() {
        this.racesFound++;
        raceEnd();
    }

    public synchronized void startRaceAttempt() {
        currentTaskUncountedTimeStart = OptionalLong.of(clock.millis());
        reportUiState();
    }

    public synchronized void finishRaceAttempt() {
        assert currentTaskUncountedTimeStart.isPresent();
        long currentTimeMillis = clock.millis();
        smtTimeMillis = smtTimeMillis.withProgressCapped(
                currentTimeMillis - currentTaskUncountedTimeStart.orElse(currentTimeMillis));
        this.currentTaskUncountedTimeStart = OptionalLong.empty();

        assert maybeComputationData.isPresent();
        ComputationData computationData = maybeComputationData.get();
        computationData.finishRaceAttempt();

        reportUiState();
    }

    synchronized void timerTick() {
        long currentTimeMillis = clock.millis();
        if (this.currentTaskUncountedTimeStart.isPresent()) {
            smtTimeMillis = smtTimeMillis.withProgressCapped(
                    currentTimeMillis - currentTaskUncountedTimeStart.getAsLong());
            currentTaskUncountedTimeStart = OptionalLong.of(currentTimeMillis);
        }
        reportUiState();
    }

    private void raceEnd() {
        assert maybeComputationData.isPresent();
        ComputationData computationData = maybeComputationData.get();
        computationData.raceEnd();

        reportUiState();
    }

    private void reportUiState() {
        Optional<ComputationData> maybeComputationData = this.maybeComputationData;
        if (!maybeComputationData.isPresent()) {
            return;
        }
        ComputationData computationData = maybeComputationData.get();
        ui.reportState(
                inputFile,
                computationData.races,
                racesFound,
                computationData.totalTasksProgress,
                smtTimeMillis);
    }
}
