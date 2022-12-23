package com.runtimeverification.rvpredict.performance;

import com.runtimeverification.rvpredict.util.Logger;

import java.time.Clock;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AnalysisLimit {
    private final Clock clock;
    private final String name;
    private final Optional<AnalysisLimit> innerTimer;
    private final OptionalInt timeSeconds;
    private long usedTimeMillis;
    private Logger logger;

    public interface RunnableWithException {
        void run() throws Exception;
    }

    public AnalysisLimit(Clock clock, String name, Optional<AnalysisLimit> innerTimer, int timeSeconds, Logger logger) {
        this.clock = clock;
        this.name = name;
        this.innerTimer = innerTimer;
        this.timeSeconds = timeSeconds == 0 ? OptionalInt.empty() : OptionalInt.of(timeSeconds);
        this.usedTimeMillis = 0;
        this.logger = logger;
    }

    public void run(Runnable r, Consumer<String> onTimeoutSkip) {
        if (timeSeconds.isPresent()) {
            if (timeout()) {
                onTimeoutSkip.accept(name);
                return;
            }
            long start = clock.millis();
            try {
                runInnerTimer(r, onTimeoutSkip);
            } finally {
                updateUsedTime(start);
            }
        } else {
            runInnerTimer(r, onTimeoutSkip);
        }
    }

    private void updateUsedTime(long start) {
        usedTimeMillis += clock.millis() - start;
        if (timeout()) {
            logger.report(name + " timeout.", Logger.MSGTYPE.ERROR);
        }
    }

    private boolean timeout() {
        return timeSeconds.isPresent() && TimeUnit.SECONDS.toMillis(timeSeconds.getAsInt()) < usedTimeMillis;
    }

    private void runInnerTimer(Runnable r, Consumer<String> onTimeoutSkip) {
        if (innerTimer.isPresent()) {
            innerTimer.get().run(r, onTimeoutSkip);
        } else {
            r.run();
        }
    }

    public void runWithException(RunnableWithException r, Consumer<String> onTimeoutSkip) throws Exception {
        if (timeSeconds.isPresent()) {
            if (timeout()) {
                onTimeoutSkip.accept(name);
                return;
            }
            long start = clock.millis();
            try {
                runInnerTimerWithException(r, onTimeoutSkip);
            } finally {
                updateUsedTime(start);
            }
        } else {
            runInnerTimerWithException(r, onTimeoutSkip);
        }
    }

    private void runInnerTimerWithException(RunnableWithException r, Consumer<String> onTimeoutSkip) throws Exception {
        if (innerTimer.isPresent()) {
            innerTimer.get().runWithException(r, onTimeoutSkip);
        } else {
            r.run();
        }
    }
}
