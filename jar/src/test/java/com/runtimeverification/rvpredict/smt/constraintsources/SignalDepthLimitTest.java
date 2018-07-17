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
public class SignalDepthLimitTest {
    private static final int NO_DEPTH_LIMIT = 0;
    private static final int ONE_SIGNAL = 1;
    private static final int THREAD_TTID_1 = 101;
    private static final int SIGNAL_D1_TTID_2 = 102;
    private static final int SIGNAL_D2_TTID_3 = 103;

    @Mock private Function<Integer, ThreadType> mockTtidToThreadType;
    @Mock private Function<Integer, Integer> mockTtidToSignalDepth;

    @Before
    public void setUp() {
        when(mockTtidToThreadType.apply(THREAD_TTID_1)).thenReturn(ThreadType.THREAD);
        when(mockTtidToThreadType.apply(SIGNAL_D1_TTID_2)).thenReturn(ThreadType.SIGNAL);
        when(mockTtidToThreadType.apply(SIGNAL_D2_TTID_3)).thenReturn(ThreadType.SIGNAL);

        when(mockTtidToSignalDepth.apply(SIGNAL_D1_TTID_2)).thenReturn(1);
        when(mockTtidToSignalDepth.apply(SIGNAL_D2_TTID_3)).thenReturn(2);
    }

    @Test
    public void alwaysTrueWithEmptyDataNoLimit() {
        ConstraintSource constraintSource =
                new SignalDepthLimit(
                        NO_DEPTH_LIMIT,
                        Collections.emptyList(),
                        mockTtidToThreadType,
                        mockTtidToSignalDepth);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithEmptyDataLimited() {
        ConstraintSource constraintSource =
                new SignalDepthLimit(
                        ONE_SIGNAL,
                        Collections.emptyList(),
                        mockTtidToThreadType,
                        mockTtidToSignalDepth);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneThread() {
        ConstraintSource constraintSource =
                new SignalDepthLimit(
                        ONE_SIGNAL,
                        Collections.singletonList(THREAD_TTID_1),
                        mockTtidToThreadType,
                        mockTtidToSignalDepth);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void sometimesTrueWithSignalInterruption() {
        ConstraintSource constraintSource =
                new SignalDepthLimit(
                        ONE_SIGNAL,
                        Arrays.asList(THREAD_TTID_1, SIGNAL_D1_TTID_2),
                        mockTtidToThreadType,
                        mockTtidToSignalDepth);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource("cidv102", "1")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource("cidv102", "2")));
    }

    @Test
    public void higherWindowDepthOverridesLimit() {
        ConstraintSource constraintSource =
                new SignalDepthLimit(
                        ONE_SIGNAL,
                        Arrays.asList(THREAD_TTID_1, SIGNAL_D1_TTID_2, SIGNAL_D2_TTID_3),
                        mockTtidToThreadType,
                        mockTtidToSignalDepth);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv102", "1", "cidv103", "1")));
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv102", "1", "cidv103", "2")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "cidv102", "1", "cidv103", "3")));
    }
}
