package com.runtimeverification.rvpredict.smt.constraintsources;

import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.ConstraintType;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.testutils.ModelConstraintUtils;
import com.runtimeverification.rvpredict.trace.ThreadType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NonSignalsHaveDepth0ConstraintSourceTest {
    private static final int THREAD_TTID_1 = 101;
    private static final int SIGNAL_TTID_2 = 102;

    @Mock private Function<Integer, ThreadType> mockTtidToThreadType;

    @Before
    public void setUp() {
        when(mockTtidToThreadType.apply(THREAD_TTID_1)).thenReturn(ThreadType.THREAD);
        when(mockTtidToThreadType.apply(SIGNAL_TTID_2)).thenReturn(ThreadType.SIGNAL);
    }

    @Test
    public void alwaysTrueWithEmptyData() {
        ConstraintSource constraintSource =
                new NonSignalsHaveDepth0ConstraintSource(
                        Collections.emptyList(),
                        mockTtidToThreadType);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithSignals() {
        ConstraintSource constraintSource =
                new NonSignalsHaveDepth0ConstraintSource(
                        Collections.singletonList(SIGNAL_TTID_2),
                        mockTtidToThreadType);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void threadDepthIs0() {
        ConstraintSource constraintSource =
                new NonSignalsHaveDepth0ConstraintSource(
                        Arrays.asList(THREAD_TTID_1, SIGNAL_TTID_2),
                        mockTtidToThreadType);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv101", "0")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv101", "-1")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv101", "1")));
    }
}
