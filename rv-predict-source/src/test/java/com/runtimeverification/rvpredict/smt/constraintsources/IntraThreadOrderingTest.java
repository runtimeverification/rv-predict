package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.testutils.ModelConstraintUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IntraThreadOrderingTest {
    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;
    @Mock private ReadonlyEventInterface mockEvent3;
    @Mock private ReadonlyEventInterface mockEvent4;

    @Before
    public void setUp() {
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
        when(mockEvent3.getEventId()).thenReturn(3L);
        when(mockEvent4.getEventId()).thenReturn(4L);
    }

    @Test
    public void alwaysTrueWithoutEvents() {
        ConstraintSource constraintSource = new IntraThreadOrdering(Collections.emptyMap());
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneEmptyThread() {
        ConstraintSource constraintSource = new IntraThreadOrdering(Collections.singletonMap(5, Collections.emptyList()));
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneEventOneThread() {
        ConstraintSource constraintSource = new IntraThreadOrdering(
                Collections.singletonMap(5, Collections.singletonList(mockEvent1)));
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void eventsOnSingleThreadMustBeOrdered() {
        ConstraintSource constraintSource = new IntraThreadOrdering(
                Collections.singletonMap(5, Arrays.asList(mockEvent1, mockEvent2, mockEvent3)));
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "5", "o3", "30")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "10", "o3", "30")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "30")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "35", "o3", "30")));
    }

    @Test
    public void alwaysTrueWithTwoThreadsOneEventEach() {
        ConstraintSource constraintSource = new IntraThreadOrdering(
                ImmutableMap.of(
                        5, Collections.singletonList(mockEvent1),
                        6, Collections.singletonList(mockEvent2)));
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void eventsOnDifferentThreadsAreIndependent() {
        ConstraintSource constraintSource = new IntraThreadOrdering(
                ImmutableMap.of(
                        5, Arrays.asList(mockEvent1, mockEvent2),
                        6, Arrays.asList(mockEvent3, mockEvent4)));
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "15", "o4", "20")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "20", "o2", "10", "o3", "15", "o4", "20")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "20", "o4", "15")));
    }
}
