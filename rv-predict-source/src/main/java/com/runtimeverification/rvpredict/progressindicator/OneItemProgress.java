package com.runtimeverification.rvpredict.progressindicator;

import com.google.common.annotations.VisibleForTesting;

class OneItemProgress {
    private final long total;
    private final long done;

    OneItemProgress(long total) {
        this.total = total;
        this.done = 0;
    }

    @VisibleForTesting
    OneItemProgress(long total, long done) {
        this.total = total;
        this.done = done;
    }

    OneItemProgress withTaskDone() {
        assert done < total;
        return new OneItemProgress(total, done + 1);
    }

    OneItemProgress withProgress(long progress) {
        long finalValue = this.done + progress;
        assert finalValue <= total;
        return new OneItemProgress(total, finalValue);
    }

    OneItemProgress withProgressCapped(long progress) {
        long finalValue = done + progress;
        if (finalValue > total) {
            finalValue = total;
        }
        return new OneItemProgress(total, finalValue);
    }

    long getTotal() {
        return total;
    }

    long getDone() {
        return done;
    }

    int intPercentageDone() {
        return Math.toIntExact(done * 100 / total);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(total) ^ Long.hashCode(done);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OneItemProgress)) {
            return false;
        }
        OneItemProgress other = (OneItemProgress) obj;
        return total == other.total && done == other.done;
    }

    @Override
    public String toString() {
        return "(" + done + " of " + total + ")";
    }
}
