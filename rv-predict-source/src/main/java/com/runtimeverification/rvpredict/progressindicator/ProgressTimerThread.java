package com.runtimeverification.rvpredict.progressindicator;

import java.util.Timer;
import java.util.TimerTask;

public class ProgressTimerThread extends TimerTask implements AutoCloseable {
    private final Timer timer;
    private ProgressIndicator progressIndicator;


    public ProgressTimerThread(ProgressIndicator progressIndicator) {
        this.timer = new Timer("progress-timer", true);
        this.progressIndicator = progressIndicator;

        this.timer.schedule(this, 100L, 100L);
    }

    @Override
    public void run() {
        progressIndicator.timerTick();
    }

    @Override
    public void close() {
        this.timer.cancel();
    }
}
