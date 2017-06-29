package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class TrueTest {
    @Test
    public void isAlwaysTrue() {
        ModelConstraint constraint = new True();
        Assert.assertTrue(constraint.evaluate(mockVariableSource()));
    }
}
