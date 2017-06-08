package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
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
    @Mock private ModelRestrict mockRestrict1;
    @Mock private ModelRestrict mockRestrict2;

    @Test
    public void emptyAndIsAlwaysTrue() {
        And and = new And();
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneTrueExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(true);
        And and = new And(mockRestrict1);
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneFalseExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(false);
        And and = new And(mockRestrict1);
        Assert.assertFalse(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithTwoTrueExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockRestrict2.evaluate(mockVariableSource)).thenReturn(true);
        And and = new And(mockRestrict1, mockRestrict2);
        Assert.assertTrue(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void andWithOneTrueOneFalseExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockRestrict2.evaluate(mockVariableSource)).thenReturn(false);
        And and = new And(mockRestrict1, mockRestrict2);
        Assert.assertFalse(and.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }
}
