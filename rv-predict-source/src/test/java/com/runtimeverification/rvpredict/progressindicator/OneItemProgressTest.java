package com.runtimeverification.rvpredict.progressindicator;

import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

public class OneItemProgressTest {
    @Test
    public void returnsGivenValues() {
        OneItemProgress progress = new OneItemProgress(5);
        Assert.assertEquals(5, progress.getTotal());
        Assert.assertEquals(0, progress.getDone());
    }

    @Test
    public void oneTaskDoneIncrementsDone() {
        OneItemProgress progress = new OneItemProgress(5).withTaskDone();
        Assert.assertEquals(5, progress.getTotal());
        Assert.assertEquals(1, progress.getDone());
    }

    @Test
    public void recordsProgress() {
        OneItemProgress progress = new OneItemProgress(5).withProgress(3);
        Assert.assertEquals(5, progress.getTotal());
        Assert.assertEquals(3, progress.getDone());
    }

    @Test
    public void capsProgressWhenAsked() {
        OneItemProgress progress = new OneItemProgress(5).withProgressCapped(7);
        Assert.assertEquals(5, progress.getTotal());
        Assert.assertEquals(5, progress.getDone());
    }

    @Test
    public void throwsExceptionWhenProgressingTooMuch() {
        MoreAsserts.assertException(() -> new OneItemProgress(5).withProgress(7));
    }

    @Test
    public void computesPercentageDone() {
        Assert.assertEquals(60, new OneItemProgress(5).withProgress(3).intPercentageDone());
    }
}
