package com.runtimeverification.rvpredict.performance;

import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

class ItemData {
    private final String tag;

    private OptionalLong startTimeMillis;
    private int count;
    private long totalTimeMillis;

    ItemData(String tag) {
        this.tag = tag;
        this.startTimeMillis = OptionalLong.empty();
        this.count = 0;
        this.totalTimeMillis = 0;
    }

    synchronized void start() {
        assert !startTimeMillis.isPresent();
        this.startTimeMillis = OptionalLong.of(System.currentTimeMillis());
    }

    synchronized void end() {
        OptionalLong localStartTimeMillis = this.startTimeMillis;
        this.startTimeMillis = OptionalLong.empty();

        assert localStartTimeMillis.isPresent();
        count++;
        totalTimeMillis += System.currentTimeMillis() - localStartTimeMillis.getAsLong();
    }

    @Override
    public String toString() {
        return tag + ": " + count + "x, " + timeApproximation(totalTimeMillis);
    }

    private String timeApproximation(long durationInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        durationInMillis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis);
        if (hours > 0) {
            return hours + "h " + ((minutes == 0) ? "" : minutes + "m");
        }
        durationInMillis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis);
        if (minutes > 0) {
            return minutes + "m " + ((seconds == 0) ? "" : seconds + "s");
        }
        durationInMillis -= TimeUnit.SECONDS.toMillis(seconds);
        if (seconds > 0) {
            return seconds + "s " + ((durationInMillis == 0) ? "" : durationInMillis + "ms");
        }
        return durationInMillis + "ms";
    }
}
