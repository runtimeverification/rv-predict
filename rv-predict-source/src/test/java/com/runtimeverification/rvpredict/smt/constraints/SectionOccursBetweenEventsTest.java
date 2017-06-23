package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.VariableValue;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SectionOccursBetweenEventsTest {
    @Mock private VariableSource mockVariableSource;
    @Mock private ReadonlyEventInterface firstSectionEvent;
    @Mock private ReadonlyEventInterface lastSectionEvent;
    @Mock private ReadonlyEventInterface beforeFirstSectionEvent;
    @Mock private ReadonlyEventInterface afterLastSectionEvent;

    @org.junit.Before
    public void setUp() {
        when(firstSectionEvent.getEventId()).thenReturn(1L);
        when(lastSectionEvent.getEventId()).thenReturn(2L);
        when(beforeFirstSectionEvent.getEventId()).thenReturn(3L);
        when(afterLastSectionEvent.getEventId()).thenReturn(4L);
    }

    @Test
    public void sectionMustOccurBetweenEvents() {
        ModelConstraint constraint = new SectionOccursBetweenEvents(
                firstSectionEvent, lastSectionEvent,
                Optional.of(beforeFirstSectionEvent), Optional.of(afterLastSectionEvent));

        for (String o4Value : new String[] {"5", "10", "15", "20", "30", "40", "45"}) {
            setVariableSourceValues("o1", "20", "o2", "40", "o3", "10", "o4", o4Value);
            if (o4Value.equals("45")) {
                Assert.assertTrue(constraint.evaluate(mockVariableSource));
            } else {
                Assert.assertFalse(constraint.evaluate(mockVariableSource));
            }
        }

        for (String o3Value : new String[] {"20", "30", "40", "50"}) {
            for (String o4Value : new String[] {"10", "15", "20", "25", "30", "35", "40", "45", "50", "55"}) {
                setVariableSourceValues("o1", "20", "o2", "40", "o3", o3Value, "o4", o4Value);
                Assert.assertFalse(constraint.evaluate(mockVariableSource));
            }
        }
    }

    @Test
    public void sectionMustOccurBeforeLastEventIfFirstIsMissing() {
        ModelConstraint constraint = new SectionOccursBetweenEvents(
                firstSectionEvent, lastSectionEvent,
                Optional.empty(), Optional.of(afterLastSectionEvent));

        for (String o4Value : new String[] {"10", "20", "30", "40"}) {
            setVariableSourceValues("o1", "20", "o2", "40", "o4", o4Value);
            Assert.assertFalse(constraint.evaluate(mockVariableSource));
        }

        setVariableSourceValues("o1", "20", "o2", "40", "o4", "50");
        Assert.assertTrue(constraint.evaluate(mockVariableSource));
    }

    @Test
    public void sectionMustOccurAfterFirstEventIfLastIsMissing() {
        ModelConstraint constraint = new SectionOccursBetweenEvents(
                firstSectionEvent, lastSectionEvent,
                Optional.of(beforeFirstSectionEvent), Optional.empty());

        for (String o3Value : new String[] {"20", "30", "40", "50"}) {
            setVariableSourceValues("o1", "20", "o2", "40", "o3", o3Value);
            Assert.assertFalse(constraint.evaluate(mockVariableSource));
        }

        setVariableSourceValues("o1", "20", "o2", "40", "o3", "10");
        Assert.assertTrue(constraint.evaluate(mockVariableSource));
    }

    @Test
    public void noConstraintIfNoEventBeforeOrAfter() {
        ModelConstraint constraint = new SectionOccursBetweenEvents(
                firstSectionEvent, lastSectionEvent,
                Optional.empty(), Optional.empty());

        setVariableSourceValues();
        Assert.assertTrue(constraint.evaluate(mockVariableSource));
    }

    private void setVariableSourceValues(String... namesAndValues) {
        Assert.assertTrue(namesAndValues.length % 2 == 0);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            when(mockVariableSource.getValue(namesAndValues[i])).thenReturn(new VariableValue(namesAndValues[i + 1]));
        }
    }
}
