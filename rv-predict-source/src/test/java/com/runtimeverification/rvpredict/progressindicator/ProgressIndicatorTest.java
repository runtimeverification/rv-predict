package com.runtimeverification.rvpredict.progressindicator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProgressIndicatorTest {
    @Mock private ProgressIndicatorUI mockProgressIndicatorUI;
    @Mock private Clock mockClock;

    @Test
    public void noUiCallsIfThereIsNoDataToShow() {
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void displaysProgressAfterStartingComputation() {
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());
        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void displaysTickProgressAfterStartingComputation() {
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());
        progressIndicator.startComputation(Collections.singletonList(3));
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void displaysProgressAfterStartingRace() {
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void displaysProgressAfterStartingRaceAttempt() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void incrementsTimeForTicksDuringRaceAttempt() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(300L);
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 200L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void incrementsDoneTasksAndTimerWhenFinishingRaceAttempt() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(300L);
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 200L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.finishRaceAttempt();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void doesNotIncrementTimerBetweenRaceAttempts() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.finishRaceAttempt();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(600L);
        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void endsAllRaceAttemptsForRaceAndIncrementsRaceCounterWhenFindingRace() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.finishRaceAttempt();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.raceFound();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 1)),
                        eq(1L),
                        eq(new OneItemProgress(3, 3)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void endsAllRaceAttemptsForRaceAndIncrementsRaceCounterWhenFinishingRace() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.finishRaceAttempt();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.noRaceFound();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 1)),
                        eq(0L),
                        eq(new OneItemProgress(3, 3)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());
    }

    @Test
    public void fileProgress() {
        when(mockClock.millis()).thenReturn(100L);
        ProgressIndicator progressIndicator = new ProgressIndicator(
                10, 60, mockProgressIndicatorUI, mockClock);
        verify(mockProgressIndicatorUI, never()).reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Collections.singletonList(3));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRace(0);
        verify(mockProgressIndicatorUI, times(2))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(2))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startRaceAttempt();
        verify(mockProgressIndicatorUI, times(3))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 0)),
                        eq(new OneItemProgress(60000, 0)));
        verify(mockProgressIndicatorUI, times(3))
                .reportState(any(), any(), anyLong(), any(), any());

        when(mockClock.millis()).thenReturn(500L);
        progressIndicator.finishRaceAttempt();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 0)),
                        eq(0L),
                        eq(new OneItemProgress(3, 1)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(4))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.raceFound();
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 0)),
                        eq(new OneItemProgress(1, 1)),
                        eq(1L),
                        eq(new OneItemProgress(3, 3)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.endWindow(5);
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.timerTick();
        verify(mockProgressIndicatorUI, times(5))
                .reportState(any(), any(), anyLong(), any(), any());

        progressIndicator.startComputation(Arrays.asList(4, 5));
        verify(mockProgressIndicatorUI, times(1))
                .reportState(
                        eq(new OneItemProgress(10, 5)),
                        eq(new OneItemProgress(2, 0)),
                        eq(1L),
                        eq(new OneItemProgress(9, 0)),
                        eq(new OneItemProgress(60000, 400L)));
        verify(mockProgressIndicatorUI, times(6))
                .reportState(any(), any(), anyLong(), any(), any());
    }
}
