package com.runtimeverification.rvpredict.performance;

import com.runtimeverification.rvpredict.util.Logger;

import java.time.Clock;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

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

    public void run(Runnable r) {
        if (timeSeconds.isPresent()) {
            if (timeout()) {
                return;
            }
            long start = clock.millis();
            try {
                runInnerTimer(r);
            } finally {
                updateUsedTime(start);
            }
        } else {
            runInnerTimer(r);
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

    private void runInnerTimer(Runnable r) {
        if (innerTimer.isPresent()) {
            innerTimer.get().run(r);
        } else {
            r.run();
        }
    }

    public void runWithException(RunnableWithException r) throws Exception {
        if (timeSeconds.isPresent()) {
            if (timeout()) {
                return;
            }
            long start = clock.millis();
            try {
                runInnerTimerWithException(r);
            } finally {
                updateUsedTime(start);
            }
        } else {
            runInnerTimerWithException(r);
        }
    }

    private void runInnerTimerWithException(RunnableWithException r) throws Exception {
        if (innerTimer.isPresent()) {
            innerTimer.get().runWithException(r);
        } else {
            r.run();
        }
    }
}
