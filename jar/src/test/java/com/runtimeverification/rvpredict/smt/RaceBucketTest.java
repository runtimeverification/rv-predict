package com.runtimeverification.rvpredict.smt;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.violation.Race;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class RaceBucketTest {
    @Mock private Race mockRace1;
    @Mock private Race mockRace2;

    @Test
    public void iteratesThroughElements() {
        RaceBucket bucket = new RaceBucket("b1", ImmutableList.of(mockRace1, mockRace2));

        Optional<Race> race = bucket.nextRace();
        Assert.assertTrue(race.isPresent());
        Assert.assertEquals(mockRace1, race.get());

        race = bucket.nextRace();
        Assert.assertTrue(race.isPresent());
        Assert.assertEquals(mockRace2, race.get());

        race = bucket.nextRace();
        Assert.assertFalse(race.isPresent());
    }

    @Test
    public void stopsIterationAfterSolving() {
        RaceBucket bucket = new RaceBucket("b1", ImmutableList.of(mockRace1, mockRace2));

        Optional<Race> race = bucket.nextRace();
        Assert.assertTrue(race.isPresent());
        Assert.assertEquals(mockRace1, race.get());

        Assert.assertEquals("b1", bucket.getNameAndMarkAsSolved());

        race = bucket.nextRace();
        Assert.assertFalse(race.isPresent());
    }
}
