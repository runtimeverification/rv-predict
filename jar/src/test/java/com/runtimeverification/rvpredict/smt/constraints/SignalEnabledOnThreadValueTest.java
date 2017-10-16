package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class SignalEnabledOnThreadValueTest {
    @Test
    public void enabledSignal() {
        ModelConstraint constraint =
                new SignalEnabledOnThreadValue(1, 5, true);
        Assert.assertFalse(constraint.evaluate(mockVariableSource("sm_1_5", "0")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("sm_1_5", "1")));
    }

    @Test
    public void disabledSignal() {
        ModelConstraint constraint =
                new SignalEnabledOnThreadValue(1, 5, false);
        Assert.assertTrue(constraint.evaluate(mockVariableSource("sm_1_5", "0")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("sm_1_5", "1")));
    }
}
