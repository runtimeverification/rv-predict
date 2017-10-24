package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;

public class LessThanTest {
    @Test
    public void checksLessThanRelation() {
        ModelConstraint constraint =
                new LessThan(new InterruptedThreadVariable(1), new InterruptedThreadVariable(3));
        Assert.assertTrue(constraint.evaluate(mockVariableSource("citv1", "5", "citv3", "10")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("citv1", "10", "citv3", "10")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource("citv1", "15", "citv3", "10")));
    }
}
