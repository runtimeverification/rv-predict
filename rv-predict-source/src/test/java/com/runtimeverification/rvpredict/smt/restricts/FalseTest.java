package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
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
        ModelRestrict restrict = new False();
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
    }
}
