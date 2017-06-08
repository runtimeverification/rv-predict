package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.VariableValue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventCanOccurBetweenGoodEventAndBadEventTest {
    @Mock private VariableSource mockVariableSource;
    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;
    @Mock private ReadonlyEventInterface mockEvent3;
    @Mock private Predicate<ReadonlyEventInterface> mockIsGood;
    @Mock private EventCanOccurBetweenGoodEventAndBadEvent.HappensBefore mockHappensBefore;


    @org.junit.Before
    public void setUp() {
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
        when(mockEvent3.getEventId()).thenReturn(3L);
    }

    @Test
    public void needsGoodEventBefore() {
        when(mockIsGood.test(mockEvent2)).thenReturn(true);
        when(mockHappensBefore.check(mockEvent1, mockEvent2)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent1)).thenReturn(false);

        ModelRestrict restrict = new EventCanOccurBetweenGoodEventAndBadEvent(
                mockEvent1, false, Collections.singletonList(mockEvent2), mockIsGood, mockHappensBefore);
        setVariableSourceValues("o1", "15", "o2", "10");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "20");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
    }

    @Test
    public void badEventIsBeforeGoodEventOrAfterMainEvent() {
        when(mockIsGood.test(mockEvent2)).thenReturn(true);
        when(mockIsGood.test(mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent2)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent2)).thenReturn(false);

        ModelRestrict restrict = new EventCanOccurBetweenGoodEventAndBadEvent(
                mockEvent1, false, Arrays.asList(mockEvent2, mockEvent3), mockIsGood, mockHappensBefore);

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "5");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "12");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "20");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));


        setVariableSourceValues("o1", "10", "o2", "10", "o3", "5");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "10", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "10", "o3", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));


        setVariableSourceValues("o1", "10", "o2", "15", "o3", "5");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "12");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "20");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
    }

    @Test
    public void doesNotNeedGoodEventBeforeIfEnabledByDefault() {
        when(mockIsGood.test(mockEvent2)).thenReturn(true);
        when(mockIsGood.test(mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent2)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent2)).thenReturn(false);

        ModelRestrict restrict = new EventCanOccurBetweenGoodEventAndBadEvent(
                mockEvent1, true, Arrays.asList(mockEvent2, mockEvent3), mockIsGood, mockHappensBefore);

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "5");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "12");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "10", "o3", "20");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));


        setVariableSourceValues("o1", "10", "o2", "10", "o3", "5");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "10", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "10", "o3", "15");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));


        setVariableSourceValues("o1", "10", "o2", "15", "o3", "5");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "12");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "15");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "10", "o2", "15", "o3", "20");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));
    }

    @Test
    public void doesNotNeedEventsWhichAreAfterForEvaluation() {
        when(mockIsGood.test(mockEvent2)).thenReturn(true);
        when(mockIsGood.test(mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent2)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent3)).thenReturn(true);
        when(mockHappensBefore.check(mockEvent2, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent2)).thenReturn(false);

        ModelRestrict restrict = new EventCanOccurBetweenGoodEventAndBadEvent(
                mockEvent1, false, Arrays.asList(mockEvent2, mockEvent3), mockIsGood, mockHappensBefore);

        setVariableSourceValues("o1", "15", "o2", "10");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "15");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "15", "o2", "20");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
    }

    @Test
    public void ignoresEnabledByDefaultWithRelevantEventsBefore() {
        when(mockIsGood.test(mockEvent2)).thenReturn(true);
        when(mockIsGood.test(mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent2)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent1, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent1)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent2, mockEvent3)).thenReturn(false);
        when(mockHappensBefore.check(mockEvent3, mockEvent1)).thenReturn(true);
        when(mockHappensBefore.check(mockEvent3, mockEvent2)).thenReturn(false);

        ModelRestrict restrict = new EventCanOccurBetweenGoodEventAndBadEvent(
                mockEvent1, false, Arrays.asList(mockEvent2, mockEvent3), mockIsGood, mockHappensBefore);

        setVariableSourceValues("o1", "20", "o2", "5", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "20", "o2", "10", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "20", "o2", "15", "o3", "10");
        Assert.assertTrue(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "20", "o2", "20", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));

        setVariableSourceValues("o1", "20", "o2", "25", "o3", "10");
        Assert.assertFalse(restrict.evaluate(mockVariableSource));
    }

    private void setVariableSourceValues(String... namesAndValues) {
        Assert.assertTrue(namesAndValues.length % 2 == 0);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            when(mockVariableSource.getValue(namesAndValues[i])).thenReturn(new VariableValue(namesAndValues[i + 1]));
        }
    }
}
