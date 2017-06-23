package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FalseTest {
    @Mock private VariableSource mockVariableSource;

    @Test public void isAlwaysFalse() {
        ModelConstraint constraint = new False();
        Assert.assertFalse(constraint.evaluate(mockVariableSource));
    }
}
