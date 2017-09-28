package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class SignalInterruptsThreadTest {
    @Test
    public void something() {
        ModelConstraint constraint = new SignalInterruptsThread(3, 10);
        Assert.assertFalse(constraint.evaluate(mockVariableSource("citv3", "5")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("citv3", "10")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("citv3", "15")));
    }
}
