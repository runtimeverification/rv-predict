package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.ModelRestrictUtils.mockVariableSource;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DisjointSectionsTest {
    @Mock private ReadonlyEventInterface mockEvent11;
    @Mock private ReadonlyEventInterface mockEvent12;
    @Mock private ReadonlyEventInterface mockEvent21;
    @Mock private ReadonlyEventInterface mockEvent22;

    @org.junit.Before
    public void setUp() {
        when(mockEvent11.getEventId()).thenReturn(11L);
        when(mockEvent12.getEventId()).thenReturn(12L);
        when(mockEvent21.getEventId()).thenReturn(21L);
        when(mockEvent22.getEventId()).thenReturn(22L);
    }

    @Test
    public void beforeMeansDijoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o11", "10", "o12", "15", "o21", "18", "o22", "20")));
    }

    @Test
    public void afterMeansDijoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o11", "18", "o12", "20", "o21", "10", "o22", "15")));
    }

    @Test
    public void overlappingAtTheEndDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "10", "o12", "18", "o21", "15", "o22", "20")));
    }

    @Test
    public void overlappingAtTheStartDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "15", "o12", "20", "o21", "10", "o22", "18")));
    }

    @Test
    public void touchingAtTheEndDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "10", "o12", "15", "o21", "15", "o22", "20")));
    }

    @Test
    public void touchingAtTheStartDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "15", "o12", "20", "o21", "10", "o22", "15")));
    }

    @Test
    public void includingDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "10", "o12", "20", "o21", "15", "o22", "18")));
    }

    @Test
    public void includedDoesNotMeanDisjoint() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "15", "o12", "18", "o21", "10", "o22", "20")));
    }

    @Test
    public void optionalAtFirstStartMeansSecondIsAfter() {
        ModelRestrict restrict = new DisjointSections(
                Optional.empty(), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o12", "20", "o21", "30", "o22", "40")));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o12", "40", "o21", "20", "o22", "30")));
    }

    @Test
    public void optionalAtSecondStartMeansFirstIsAfter() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.empty(), Optional.of(mockEvent22));
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o11", "30", "o12", "40", "o22", "20")));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "20", "o12", "30", "o22", "40")));
    }

    @Test
    public void optionalAtFirstEndMeansSecondIsBefore() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.empty(), Optional.of(mockEvent21), Optional.of(mockEvent22));
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o11", "40", "o21", "20", "o22", "30")));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "20", "o21", "30", "o22", "40")));
    }

    @Test
    public void optionalAtSecondEndMeansFirstIsBefore() {
        ModelRestrict restrict = new DisjointSections(
                Optional.of(mockEvent11), Optional.of(mockEvent12), Optional.of(mockEvent21), Optional.empty());
        Assert.assertTrue(restrict.evaluate(
                mockVariableSource("o11", "20", "o12", "30", "o21", "40")));
        Assert.assertFalse(restrict.evaluate(
                mockVariableSource("o11", "30", "o12", "40", "o21", "20")));
    }
}
