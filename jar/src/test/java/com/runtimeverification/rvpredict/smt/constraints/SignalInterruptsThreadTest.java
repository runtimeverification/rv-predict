package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class SignalInterruptsThreadTest {
    @Test
    public void signalMustInterruptGivenThread() {
        ModelConstraint constraint = new SignalInterruptsThread(3, 10);
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "citv3", "5", "cidv10", "1", "cidv3", "2")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                "citv3", "10", "cidv10", "1", "cidv3", "2")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "citv3", "15", "cidv10", "1", "cidv3", "2")));
    }

    @Test
    public void signalMustInterruptAtHigherDepth() {
        ModelConstraint constraint = new SignalInterruptsThread(3, 10);
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                "citv3", "10", "cidv10", "1", "cidv3", "2")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                "citv3", "10", "cidv10", "1", "cidv3", "3")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "citv3", "10", "cidv10", "1", "cidv3", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "citv3", "10", "cidv10", "1", "cidv3", "0")));
    }
}
