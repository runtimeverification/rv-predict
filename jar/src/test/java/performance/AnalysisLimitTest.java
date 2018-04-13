package performance;

import com.runtimeverification.rvpredict.performance.AnalysisLimit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisLimitTest {
    @Mock private Clock mockClock;
    @Mock private Runnable mockTask;
    @Mock private AnalysisLimit.RunnableWithException mockExceptionTask;
    @Mock private AnalysisLimit mockAnalysisLimit;

    @Test
    public void zeroTimerDoesNotExpire() {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.empty(), 0);

        when(mockClock.millis()).thenReturn(0L).thenReturn(secondsToMillis(3600));
        limit.run(mockTask);
        verify(mockTask).run();

        when(mockClock.millis()).thenReturn(secondsToMillis(3600)).thenReturn(secondsToMillis(7200));
        limit.run(mockTask);
        verify(mockTask, times(2)).run();
    }

    @Test
    public void doesNotRunTasksAfterExpiration() {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.empty(), 10);

        when(mockClock.millis()).thenReturn(0L).thenReturn(secondsToMillis(11));
        limit.run(mockTask);
        verify(mockTask).run();

        when(mockClock.millis()).thenReturn(secondsToMillis(12)).thenReturn(secondsToMillis(13));
        limit.run(mockTask);
        verify(mockTask).run();
    }

    @Test
    public void accumulatesTaskTimes() {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.empty(), 10);

        // Task takes 4 seconds, 4 seconds used in total.
        when(mockClock.millis()).thenReturn(0L).thenReturn(secondsToMillis(4));
        limit.run(mockTask);
        verify(mockTask).run();

        // Task takes 4 seconds, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(10L)).thenReturn(secondsToMillis(14));
        limit.run(mockTask);
        verify(mockTask, times(2)).run();

        // Task takes 1 second, 9 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(20)).thenReturn(secondsToMillis(21));
        limit.run(mockTask);
        verify(mockTask, times(3)).run();

        // Task takes 2 seconds, 11 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(30)).thenReturn(secondsToMillis(32));
        limit.run(mockTask);
        verify(mockTask, times(4)).run();

        // Task takes 1 seconds, but does not run, 11 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(40)).thenReturn(secondsToMillis(41));
        limit.run(mockTask);
        verify(mockTask, times(4)).run();
    }

    @Test
    public void sendsTaskToInnerAnalysisLimit() {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.of(mockAnalysisLimit), 0);

        limit.run(mockTask);
        verify(mockTask, never()).run();
        verify(mockAnalysisLimit).run(mockTask);
    }

    @Test
    public void doesNotSendTaskToInnerAnalysisLimitAfterTimeout() {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.of(mockAnalysisLimit), 5);

        // Task takes 4 seconds, 4 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(0)).thenReturn(secondsToMillis(4));
        limit.run(mockTask);
        verify(mockTask, never()).run();
        verify(mockAnalysisLimit).run(mockTask);

        // Task takes 4 seconds, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(10)).thenReturn(secondsToMillis(14));
        limit.run(mockTask);
        verify(mockTask, never()).run();
        verify(mockAnalysisLimit, times(2)).run(mockTask);

        // Task takes 1 seconds, but does not run, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(40)).thenReturn(secondsToMillis(41));
        limit.run(mockTask);
        verify(mockTask, never()).run();
        verify(mockAnalysisLimit, times(2)).run(mockTask);
    }

    @Test
    public void runsTaskWithException() throws Exception {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.empty(), 10);

        // Task takes 4 seconds, 4 seconds used in total.
        when(mockClock.millis()).thenReturn(0L).thenReturn(secondsToMillis(4));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask).run();

        // Task takes 4 seconds, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(10L)).thenReturn(secondsToMillis(14));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, times(2)).run();

        // Task takes 1 second, 9 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(20)).thenReturn(secondsToMillis(21));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, times(3)).run();

        // Task takes 2 seconds, 11 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(30)).thenReturn(secondsToMillis(32));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, times(4)).run();

        // Task takes 1 seconds, but does not run, 11 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(40)).thenReturn(secondsToMillis(41));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, times(4)).run();
    }


    @Test
    public void sendsExceptionTaskToInnerAnalysisLimit() throws Exception {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.of(mockAnalysisLimit), 0);

        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, never()).run();
        verify(mockAnalysisLimit).runWithException(mockExceptionTask);
    }

    @Test
    public void doesNotSendExceptionTaskToInnerAnalysisLimitAfterTimeout() throws Exception {
        AnalysisLimit limit = new AnalysisLimit(mockClock,"Test", Optional.of(mockAnalysisLimit), 5);

        // Task takes 4 seconds, 4 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(0)).thenReturn(secondsToMillis(4));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, never()).run();
        verify(mockAnalysisLimit).runWithException(mockExceptionTask);

        // Task takes 4 seconds, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(10)).thenReturn(secondsToMillis(14));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, never()).run();
        verify(mockAnalysisLimit, times(2)).runWithException(mockExceptionTask);

        // Task takes 1 seconds, but does not run, 8 seconds used in total.
        when(mockClock.millis()).thenReturn(secondsToMillis(40)).thenReturn(secondsToMillis(41));
        limit.runWithException(mockExceptionTask);
        verify(mockExceptionTask, never()).run();
        verify(mockAnalysisLimit, times(2)).runWithException(mockExceptionTask);
    }

    private long secondsToMillis(long seconds) {
        return TimeUnit.SECONDS.toMillis(seconds);
    }
}
