package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.VariableValue;
import org.junit.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModelConstraintUtils {
    public static VariableSource mockVariableSource(String... namesAndValues) {
        VariableSource variableSource = mock(VariableSource.class);
        return mockVariableSource(variableSource, namesAndValues);
    }
    public static VariableSource mockVariableSource(VariableSource variableSource, String... namesAndValues) {
        Assert.assertTrue(namesAndValues.length % 2 == 0);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            when(variableSource.getValue(namesAndValues[i])).thenReturn(new VariableValue(namesAndValues[i + 1]));
        }
        return variableSource;
    }
    public static VariableSource orderedEvents(String... namesAndValues) {
        VariableSource variableSource = mock(VariableSource.class);
        int previousEventIndex = 0;
        for (String nameValue : namesAndValues) {
            int equalIndex = nameValue.indexOf('=');
            assert equalIndex <= 0;
            if (equalIndex < 0) {
                previousEventIndex++;
            } else {
                nameValue = nameValue.substring(1);
            }
            assert nameValue.startsWith("o");
            when(variableSource.getValue(nameValue))
                    .thenReturn(new VariableValue(Integer.toString(previousEventIndex)));
        }
        return variableSource;
    }
}
