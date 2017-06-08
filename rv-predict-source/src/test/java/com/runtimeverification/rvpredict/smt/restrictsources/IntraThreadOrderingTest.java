package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSource;
import com.runtimeverification.rvpredict.testutils.ModelRestrictUtils;
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
        RestrictSource restrictSource = new IntraThreadOrdering(Collections.emptyMap());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneEmptyThread() {
        RestrictSource restrictSource = new IntraThreadOrdering(Collections.singletonMap(5, Collections.emptyList()));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneEventOneThread() {
        RestrictSource restrictSource = new IntraThreadOrdering(
                Collections.singletonMap(5, Collections.singletonList(mockEvent1)));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void eventsOnSingleThreadMustBeOrdered() {
        RestrictSource restrictSource = new IntraThreadOrdering(
                Collections.singletonMap(5, Arrays.asList(mockEvent1, mockEvent2, mockEvent3)));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "5", "o3", "30")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "10", "o3", "30")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "30")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "35", "o3", "30")));
    }

    @Test
    public void alwaysTrueWithTwoThreadsOneEventEach() {
        RestrictSource restrictSource = new IntraThreadOrdering(
                ImmutableMap.of(
                        5, Collections.singletonList(mockEvent1),
                        6, Collections.singletonList(mockEvent2)));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void eventsOnDifferentThreadsAreIndependent() {
        RestrictSource restrictSource = new IntraThreadOrdering(
                ImmutableMap.of(
                        5, Arrays.asList(mockEvent1, mockEvent2),
                        6, Arrays.asList(mockEvent3, mockEvent4)));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "15", "o4", "20")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "20", "o2", "10", "o3", "15", "o4", "20")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "20", "o4", "15")));
    }
}
