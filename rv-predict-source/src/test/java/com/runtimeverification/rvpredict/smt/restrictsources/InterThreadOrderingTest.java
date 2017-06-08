package com.runtimeverification.rvpredict.smt.restrictsources;

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

import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InterThreadOrderingTest {
    @Mock private ReadonlyEventInterface mockStartThread2Event1;
    @Mock private ReadonlyEventInterface mockJoinThread2Event3;
    @Mock private ReadonlyEventInterface mockEvent2;

    @Before
    public void startUp() {
        when(mockStartThread2Event1.getEventId()).thenReturn(1L);
        when(mockStartThread2Event1.getSyncedThreadId()).thenReturn(2L);
        when(mockStartThread2Event1.isStart()).thenReturn(true);

        when(mockJoinThread2Event3.getEventId()).thenReturn(3L);
        when(mockJoinThread2Event3.getSyncedThreadId()).thenReturn(2L);
        when(mockJoinThread2Event3.isJoin()).thenReturn(true);

        when(mockEvent2.getEventId()).thenReturn(2L);
    }

    @Test
    public void alwaysTrueWithoutThreadOrderingEvents() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.emptyList(),
                otid -> OptionalInt.empty(),
                ttid -> Optional.empty(),
                ttid -> Optional.empty());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void firstThreadEventMustBeAfterStart() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockStartThread2Event1),
                otidToTtid(2, 3),
                ttidToEvent(3, mockEvent2),
                ttid -> Optional.empty());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "20", "o2", "10")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "10")));
    }

    @Test
    public void startEventIsIgnoredWithoutFirstEvent() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockStartThread2Event1),
                otidToTtid(2, 3),
                ttid -> Optional.empty(),
                ttid -> Optional.empty());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void startEventIsIgnoredWithoutOtidConversion() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockStartThread2Event1),
                otid -> OptionalInt.empty(),
                ttidToEvent(3, mockEvent2),
                ttid -> Optional.empty());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void lastThreadEventMustBeAfterJoin() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockJoinThread2Event3),
                otidToTtid(2, 5),
                ttid -> Optional.empty(),
                ttidToEvent(5, mockEvent2));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o3", "20", "o2", "10")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o3", "10", "o2", "20")));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o3", "20", "o2", "20")));
    }

    @Test
    public void joinEventIsIgnoredWithoutFirstEvent() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockJoinThread2Event3),
                otidToTtid(2, 5),
                ttid -> Optional.empty(),
                ttid -> Optional.empty());
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void joinEventIsIgnoredWithoutOtidConversion() {
        RestrictSource restrictSource = new InterThreadOrdering(
                Collections.singletonList(mockJoinThread2Event3),
                otid -> OptionalInt.empty(),
                ttid -> Optional.empty(),
                ttidToEvent(5, mockEvent2));
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    private Function<Long, OptionalInt> otidToTtid(int... mapping) {
        return otid -> {
            for (int i = 0; i < mapping.length; i+=2) {
                if (mapping[i] == otid) {
                    return OptionalInt.of(mapping[i + 1]);
                }
            }
            return OptionalInt.empty();
        };
    }

    private Function<Integer, Optional<ReadonlyEventInterface>> ttidToEvent(int ttid, ReadonlyEventInterface event) {
        return functionTtid -> ttid == functionTtid ? Optional.of(event) : Optional.empty();
    }
}
