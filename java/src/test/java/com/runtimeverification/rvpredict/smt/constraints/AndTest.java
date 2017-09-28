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
public class AndTest {
    @Mock private VariableSource mockVariableSource;
    @Mock private ModelConstraint mockConstraint1;
    @Mock private ModelConstraint mockConstraint2;

    @Test
    public void emptyAndIsAlwaysTrue() {
        And and = new And();
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneTrueExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(true);
        And and = new And(mockConstraint1);
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneFalseExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(false);
        And and = new And(mockConstraint1);
        Assert.assertFalse(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithTwoTrueExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockConstraint2.evaluate(mockVariableSource)).thenReturn(true);
        And and = new And(mockConstraint1, mockConstraint2);
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneTrueOneFalseExpr() {
        when(mockConstraint1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockConstraint2.evaluate(mockVariableSource)).thenReturn(false);
        And and = new And(mockConstraint1, mockConstraint2);
        Assert.assertFalse(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }
}
