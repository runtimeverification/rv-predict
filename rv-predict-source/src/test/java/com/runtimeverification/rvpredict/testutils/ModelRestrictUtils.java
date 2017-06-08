package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.VariableValue;
import org.junit.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModelRestrictUtils {
    public static VariableSource mockVariableSource(String... namesAndValues) {
        VariableSource variableSource = mock(VariableSource.class);
        Assert.assertTrue(namesAndValues.length % 2 == 0);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            when(variableSource.getValue(namesAndValues[i])).thenReturn(new VariableValue(namesAndValues[i + 1]));
        }
        return variableSource;
    }
}
