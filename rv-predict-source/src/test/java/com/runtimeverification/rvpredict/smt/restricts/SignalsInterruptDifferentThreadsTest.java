package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import org.junit.Assert;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.ModelRestrictUtils.mockVariableSource;

public class SignalsInterruptDifferentThreadsTest {
    @Test
    public void signalsMustInterruptDifferentThreads() {
        ModelRestrict restrict = new SignalsInterruptDifferentThreads(3, 7);
        Assert.assertTrue(restrict.evaluate(mockVariableSource("citv3", "5", "citv7", "10")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("citv3", "10", "citv7", "10")));
        Assert.assertTrue(restrict.evaluate(mockVariableSource("citv3", "15", "citv7", "10")));
    }
}
