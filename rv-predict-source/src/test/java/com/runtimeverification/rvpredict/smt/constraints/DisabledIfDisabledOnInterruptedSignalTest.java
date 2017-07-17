package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class DisabledIfDisabledOnInterruptedSignalTest {
    @Test
    public void disabledIfDisabledOnInterruptedSignal() {
        ModelConstraint constraint =
                new DisabledIfDisabledOnInterruptedSignal(1, 3, 5);
        Assert.assertTrue(constraint.evaluate(mockVariableSource("sm_1_5", "1", "sm_3_5", "1")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("sm_1_5", "0", "sm_3_5", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("sm_1_5", "1", "sm_3_5", "0")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("sm_1_5", "0", "sm_3_5", "0")));
    }
}
