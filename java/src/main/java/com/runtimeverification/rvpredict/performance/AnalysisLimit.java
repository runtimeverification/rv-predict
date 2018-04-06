package com.runtimeverification.rvpredict.performance;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

public class AnalysisLimit {
    private final String name;
    private final Optional<AnalysisLimit> innerTimer;
    private final OptionalInt timeSeconds;
    private long usedTimeMillis;

    public interface RunnableWithException {
        void run() throws Exception;
    }

    public AnalysisLimit(String name, Optional<AnalysisLimit> innerTimer, int timeSeconds) {
        this.name = name;
        this.innerTimer = innerTimer;
        this.timeSeconds = timeSeconds == 0 ? OptionalInt.empty() : OptionalInt.of(timeSeconds);
        this.usedTimeMillis = 0;
    }

    public void run(Runnable r) {
        if (timeSeconds.isPresent()) {
            if (timeout()) {
                return;
            }
            long start = System.currentTimeMillis();
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
        usedTimeMillis += System.currentTimeMillis() - start;
        if (timeout()) {
            System.out.println(name + " timeout.");
        }
    }

    private boolean timeout() {
        return TimeUnit.SECONDS.toMillis(timeSeconds.getAsInt()) < usedTimeMillis;
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
            long start = System.currentTimeMillis();
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
