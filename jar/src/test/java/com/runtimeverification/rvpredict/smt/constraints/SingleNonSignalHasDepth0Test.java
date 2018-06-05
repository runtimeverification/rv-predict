package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class SingleNonSignalHasDepth0Test {
    @Test
    public void depthIs0() {
        ModelConstraint constraint = new SingleNonSignalHasDepth0(3);
        Assert.assertTrue(constraint.evaluate(mockVariableSource("cidv3", "0")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("cidv3", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("cidv3", "-1")));
    }
}