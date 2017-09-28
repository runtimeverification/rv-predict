package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class SignalsInterruptDifferentThreadsTest {
    @Test
    public void signalsMustInterruptDifferentThreads() {
        ModelConstraint constraint = new SignalsInterruptDifferentThreads(3, 7);
        Assert.assertTrue(constraint.evaluate(mockVariableSource("citv3", "5", "citv7", "10")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("citv3", "10", "citv7", "10")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("citv3", "15", "citv7", "10")));
    }
}
