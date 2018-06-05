package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class MaxSignalInterruptionDepthTest {
    @Test
    public void depthIsLessThanLimit() {
        ModelConstraint constraint = new MaxSignalInterruptionDepth(3, 2);
        Assert.assertTrue(constraint.evaluate(mockVariableSource("cidv3", "1")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("cidv3", "2")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("cidv3", "3")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("cidv3", "4")));
    }

}
