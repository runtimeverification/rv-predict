package com.runtimeverification.rvpredict.order;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.*;

public class VectorClockTest {
    VectorClock clock1, clock2, clock3;

    @Before
    public void setUp() throws Exception {
        clock1 = new VectorClock();
        clock2 = new VectorClock();
        clock2.clocks.put(1L, 0L);
        clock2.clocks.put(2L, 3L);
        clock2.clocks.put(3L, 7L);

        clock3 = new VectorClock();
        clock3.clocks.put(1L, 5L);
        clock3.clocks.put(3L, 4L);
        clock3.clocks.put(5L, 1L);

    }

    @Test
    public void increment() throws Exception {
        clock1.increment(15);
        Assert.assertThat(clock1.clocks, hasMapSize(1));
        Assert.assertTrue(clock1.clocks.containsKey(15L));
        Assert.assertEquals(1L, clock1.clocks.get(15L).longValue());

        VectorClock clock;
        clock = new VectorClock(clock2);
        clock2.increment(15);
        Assert.assertThat(clock2.clocks, hasMapSize(4));
        Assert.assertTrue(clock2.clocks.containsKey(15L));
        Assert.assertEquals(1L, clock2.clocks.get(15L).longValue());
        includedIn(clock, clock2);

        clock = new VectorClock(clock2);
        clock2.increment(1);
        Assert.assertThat(clock2.clocks, hasMapSize(4));
        Assert.assertEquals(1L, clock2.clocks.get(1L).longValue());
        clock.clocks.forEach((c, value) -> {
            Assert.assertTrue(clock2.clocks.containsKey(c));
            if (c != 1L) {
                Assert.assertEquals(value.longValue(), clock2.clocks.get(c).longValue());
            }
        });
    }

    private void includedIn(VectorClock clock, VectorClock clock2) {
        clock.clocks.forEach((c, value) -> {
            Assert.assertTrue(clock2.clocks.containsKey(c));
            Assert.assertEquals(value.longValue(), clock2.clocks.get(c).longValue());
        });
    }

    @Test
    public void update() throws Exception {
        VectorClock clockTest = new VectorClock(clock2);
        clockTest.update(clock1);
        includedIn(clock2, clockTest);
        includedIn(clockTest, clock2);

        clockTest.update(clock3);
        Assert.assertThat(clockTest.clocks, hasMapSize(4));
        Assert.assertEquals(5L, clockTest.clocks.get(1L).longValue());
        Assert.assertEquals(3L, clockTest.clocks.get(2L).longValue());
        Assert.assertEquals(7L, clockTest.clocks.get(3L).longValue());
        Assert.assertEquals(1L, clockTest.clocks.get(5L).longValue());

        clockTest = new VectorClock(clock3);
        clockTest.update(clock2);
        Assert.assertThat(clockTest.clocks, hasMapSize(4));
        Assert.assertEquals(5L, clockTest.clocks.get(1L).longValue());
        Assert.assertEquals(3L, clockTest.clocks.get(2L).longValue());
        Assert.assertEquals(7L, clockTest.clocks.get(3L).longValue());
        Assert.assertEquals(1L, clockTest.clocks.get(5L).longValue());
    }

    @Test
    public void compareTo() throws Exception {
        Assert.assertEquals(VectorClock.Comparison.BEFORE, clock1.compareTo(clock2));
        Assert.assertEquals(VectorClock.Comparison.AFTER, clock2.compareTo(clock1));
        Assert.assertEquals(VectorClock.Comparison.EQUAL, clock1.compareTo(clock1));
        Assert.assertEquals(VectorClock.Comparison.EQUAL, clock2.compareTo(clock2));
        Assert.assertEquals(VectorClock.Comparison.EQUAL, clock3.compareTo(clock3));
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clock2.compareTo(clock3));
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clock3.compareTo(clock2));

        VectorClock clockTest = new VectorClock(clock2);
        clock2.clocks.put(2L, 4L);
        Assert.assertEquals(VectorClock.Comparison.AFTER, clock2.compareTo(clockTest));
        Assert.assertEquals(VectorClock.Comparison.BEFORE, clockTest.compareTo(clock2));
        clockTest.clocks.put(10L, 0L);
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clock2.compareTo(clockTest));
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clockTest.compareTo(clock2));

    }

}