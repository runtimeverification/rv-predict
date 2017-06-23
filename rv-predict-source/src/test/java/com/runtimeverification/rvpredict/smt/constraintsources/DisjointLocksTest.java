package com.runtimeverification.rvpredict.smt.constraintsources;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.testutils.ModelConstraintUtils;
import com.runtimeverification.rvpredict.trace.LockRegion;
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
public class DisjointLocksTest {
    @Mock private ReadonlyEventInterface mockLock_1_1;
    @Mock private ReadonlyEventInterface mockUnlock_1_1;
    @Mock private ReadonlyEventInterface mockLock_1_2;
    @Mock private ReadonlyEventInterface mockUnlock_1_2;
    @Mock private ReadonlyEventInterface mockLock_2_1;
    @Mock private ReadonlyEventInterface mockUnlock_2_1;
    @Mock private ReadonlyEventInterface mockLock_2_2;
    @Mock private ReadonlyEventInterface mockUnlock_2_2;

    @Before
    public void setUp() {
        when(mockLock_1_1.getEventId()).thenReturn(1L);
        when(mockUnlock_1_1.getEventId()).thenReturn(2L);
        when(mockLock_1_2.getEventId()).thenReturn(3L);
        when(mockUnlock_1_2.getEventId()).thenReturn(4L);
        when(mockLock_2_1.getEventId()).thenReturn(5L);
        when(mockUnlock_2_1.getEventId()).thenReturn(6L);
        when(mockLock_2_2.getEventId()).thenReturn(7L);
        when(mockUnlock_2_2.getEventId()).thenReturn(8L);

        when(mockLock_1_1.getType()).thenReturn(EventType.WRITE_LOCK);
        when(mockLock_1_2.getType()).thenReturn(EventType.WRITE_LOCK);
        when(mockLock_2_1.getType()).thenReturn(EventType.WRITE_LOCK);
        when(mockLock_2_2.getType()).thenReturn(EventType.WRITE_LOCK);
        when(mockUnlock_1_1.getType()).thenReturn(EventType.WRITE_UNLOCK);
        when(mockUnlock_1_2.getType()).thenReturn(EventType.WRITE_UNLOCK);
        when(mockUnlock_2_1.getType()).thenReturn(EventType.WRITE_UNLOCK);
        when(mockUnlock_2_2.getType()).thenReturn(EventType.WRITE_UNLOCK);
    }

    @Test
    public void alwaysTrueWithoutLocks() {
        ConstraintSource constraintSource = new DisjointLocks(Collections.emptyList(), (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneLock() {
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Collections.singletonList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1)
                )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithLocksOnSameThread() {
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Arrays.asList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                        new LockRegion(mockLock_1_2, mockUnlock_1_2, 1)
                )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void disjointLockSectionsWithSameLock() {
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Arrays.asList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                        new LockRegion(mockLock_1_2, mockUnlock_1_2, 2)
                )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "20", "o2", "30", "o3", "10", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "20", "o2", "40", "o3", "10", "o4", "30")));
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "30", "o2", "40", "o3", "10", "o4", "20")));
    }

    @Test
    public void alwaysTrueWhenThreadsCannotOverlap() {
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Arrays.asList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                        new LockRegion(mockLock_1_2, mockUnlock_1_2, 2)
                )),
                (t1, t2) -> false);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void readLocksCanOverlap() {
        when(mockLock_1_1.getType()).thenReturn(EventType.READ_LOCK);
        when(mockLock_1_2.getType()).thenReturn(EventType.READ_LOCK);
        when(mockUnlock_1_1.getType()).thenReturn(EventType.READ_UNLOCK);
        when(mockUnlock_1_2.getType()).thenReturn(EventType.READ_UNLOCK);
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Arrays.asList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                        new LockRegion(mockLock_1_2, mockUnlock_1_2, 2)
                )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource()));
    }

    @Test
    public void readLocksCannotOverlapWithWriteLocks() {
        when(mockLock_1_1.getType()).thenReturn(EventType.READ_LOCK);
        when(mockLock_1_2.getType()).thenReturn(EventType.WRITE_LOCK);
        when(mockUnlock_1_1.getType()).thenReturn(EventType.READ_UNLOCK);
        when(mockUnlock_1_2.getType()).thenReturn(EventType.WRITE_UNLOCK);
        ConstraintSource constraintSource = new DisjointLocks(
                Collections.singletonList(Arrays.asList(
                        new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                        new LockRegion(mockLock_1_2, mockUnlock_1_2, 2)
                )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "20", "o2", "30", "o3", "10", "o4", "40")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "20", "o2", "40", "o3", "10", "o4", "30")));
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "30", "o2", "40", "o3", "10", "o4", "20")));
    }

    @Test
    public void lockSectionsWithDifferentLocksCanOverlap() {
        ConstraintSource constraintSource = new DisjointLocks(
                Arrays.asList(
                        Arrays.asList(
                                new LockRegion(mockLock_1_1, mockUnlock_1_1, 1),
                                new LockRegion(mockLock_1_2, mockUnlock_1_2, 2)
                        ),
                        Arrays.asList(
                                new LockRegion(mockLock_2_1, mockUnlock_2_1, 3),
                                new LockRegion(mockLock_2_2, mockUnlock_2_2, 4)
                        )),
                (t1, t2) -> true);
        ModelConstraint constraint = constraintSource.createConstraint();
        Assert.assertTrue(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "o5", "15", "o6", "25", "o7", "35", "o8", "45")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "o5", "15", "o6", "25", "o7", "35", "o8", "45")));
        Assert.assertFalse(constraint.evaluate(ModelConstraintUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "o5", "15", "o6", "35", "o7", "25", "o8", "45")));
    }
}
