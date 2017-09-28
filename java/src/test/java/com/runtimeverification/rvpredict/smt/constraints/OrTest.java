package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrTest {
    @Mock private VariableSource mockVariableSource;
    @Mock private ModelConstraint mockConstraint1;
    @Mock private ModelConstraint mockConstraint2;

    @Test
    public void emptyOrIsAlwaysFalse() {
        ModelConstraint constraint = new Or();
        Assert.assertFalse(constraint.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneTrueExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(true);
        ModelConstraint constraint = new Or(mockConstraint1);
        Assert.assertTrue(constraint.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneFalseExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(false);
        ModelConstraint constraint = new Or(mockConstraint1);
        Assert.assertFalse(constraint.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithTwoFalseExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(false);
        when(mockConstraint2.evaluate(mockVariableSource)).thenReturn(false);
        ModelConstraint constraint = new Or(mockConstraint1, mockConstraint2);
        Assert.assertFalse(constraint.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneTrueOneFalseExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockConstraint2.evaluate(mockVariableSource)).thenReturn(false);
        ModelConstraint constraint = new Or(mockConstraint1, mockConstraint2);
        Assert.assertTrue(constraint.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }
}
