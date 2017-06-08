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
public class OrTest {
    @Mock private VariableSource mockVariableSource;
    @Mock private ModelRestrict mockRestrict1;
    @Mock private ModelRestrict mockRestrict2;

    @Test
    public void emptyOrIsAlwaysFalse() {
        ModelRestrict restrict = new Or();
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneTrueExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(true);
        ModelRestrict restrict = new Or(mockRestrict1);
        Assert.assertTrue(restrict.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneFalseExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(false);
        ModelRestrict restrict = new Or(mockRestrict1);
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithTwoFalseExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(false);
        when(mockRestrict2.evaluate(mockVariableSource)).thenReturn(false);
        ModelRestrict restrict = new Or(mockRestrict1, mockRestrict2);
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }

    @Test
    public void orWithOneTrueOneFalseExpr() {
        when(mockRestrict1.evaluate(mockVariableSource)).thenReturn(true);
        when(mockRestrict2.evaluate(mockVariableSource)).thenReturn(false);
        ModelRestrict restrict = new Or(mockRestrict1, mockRestrict2);
        Assert.assertTrue(restrict.evaluate(mockVariableSource));
        verify(mockVariableSource, never()).getValue(any());
    }
}
