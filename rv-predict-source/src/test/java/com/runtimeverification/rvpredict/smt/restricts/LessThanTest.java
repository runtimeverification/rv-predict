package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelRestrictUtils.mockVariableSource;

public class LessThanTest {
    @Test
    public void checksLessThanRelation() {
        ModelRestrict restrict =
                new LessThan(new InterruptedThreadVariable(1), new InterruptedThreadVariable(3));
        Assert.assertTrue(restrict.evaluate(mockVariableSource("citv1", "5", "citv3", "10")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("citv1", "10", "citv3", "10")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("citv1", "15", "citv3", "10")));
    }
}
