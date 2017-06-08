package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelRestrictUtils.mockVariableSource;

public class SignalInterruptsThreadTest {
    @Test
    public void something() {
        ModelRestrict restrict = new SignalInterruptsThread(3, 10);
        Assert.assertFalse(restrict.evaluate(mockVariableSource("citv3", "5")));
        Assert.assertTrue(restrict.evaluate(mockVariableSource("citv3", "10")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("citv3", "15")));
    }
}
