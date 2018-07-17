package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.VariableValue;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Assert;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModelConstraintUtils {
    private static final MutableBoolean safeToThrowException = new MutableBoolean(false);

    public static VariableSource mockVariableSource(String... namesAndValues) {
        VariableSource variableSource = mock(VariableSource.class);
        when(variableSource.getValue(anyString()))
                .then((Answer<VariableValue>) invocation -> {
                    if (safeToThrowException.booleanValue()) {
                        throw new IllegalArgumentException("Variable not found: " + invocation.getArguments()[0]);
                    }
                    return new VariableValue("");
                });
        variableSource = mockVariableSource(variableSource, namesAndValues);
        safeToThrowException.setTrue();
        return variableSource;
    }
    public static VariableSource mockVariableSource(VariableSource variableSource, String... namesAndValues) {
        Assert.assertEquals(0, namesAndValues.length % 2);
        safeToThrowException.setFalse();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            when(variableSource.getValue(namesAndValues[i])).thenReturn(new VariableValue(namesAndValues[i + 1]));
        }
        safeToThrowException.setTrue();
        return variableSource;
    }
    public static VariableSource orderedEvents(String... namesAndValues) {
        VariableSource variableSource = mockVariableSource();
        int previousEventIndex = 0;
        safeToThrowException.setFalse();
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
        safeToThrowException.setTrue();
        return variableSource;
    }
}
