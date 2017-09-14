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
        clock2.clocks.put(1, 0);
        clock2.clocks.put(2, 3);
        clock2.clocks.put(3, 7);

        clock3 = new VectorClock();
        clock3.clocks.put(1, 5);
        clock3.clocks.put(3, 4);
        clock3.clocks.put(5, 1);

    }

    @Test
    public void increment() throws Exception {
        clock1.increment(15);
        Assert.assertThat(clock1.clocks, hasMapSize(1));
        Assert.assertTrue(clock1.clocks.containsKey(15));
        Assert.assertEquals(1, clock1.clocks.get(15).longValue());

        VectorClock clock;
        clock = new VectorClock(clock2);
        clock2.increment(15);
        Assert.assertThat(clock2.clocks, hasMapSize(4));
        Assert.assertTrue(clock2.clocks.containsKey(15));
        Assert.assertEquals(1, clock2.clocks.get(15).longValue());
        includedIn(clock, clock2);

        clock = new VectorClock(clock2);
        clock2.increment(1);
        Assert.assertThat(clock2.clocks, hasMapSize(4));
        Assert.assertEquals(1, clock2.clocks.get(1).longValue());
        clock.clocks.forEach((c, value) -> {
            Assert.assertTrue(clock2.clocks.containsKey(c));
            if (c != 1) {
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
        Assert.assertEquals(5, clockTest.clocks.get(1).longValue());
        Assert.assertEquals(3, clockTest.clocks.get(2).longValue());
        Assert.assertEquals(7, clockTest.clocks.get(3).longValue());
        Assert.assertEquals(1, clockTest.clocks.get(5).longValue());

        clockTest = new VectorClock(clock3);
        clockTest.update(clock2);
        Assert.assertThat(clockTest.clocks, hasMapSize(4));
        Assert.assertEquals(5, clockTest.clocks.get(1).longValue());
        Assert.assertEquals(3, clockTest.clocks.get(2).longValue());
        Assert.assertEquals(7, clockTest.clocks.get(3).longValue());
        Assert.assertEquals(1, clockTest.clocks.get(5).longValue());
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
        clock2.clocks.put(2, 4);
        Assert.assertEquals(VectorClock.Comparison.AFTER, clock2.compareTo(clockTest));
        Assert.assertEquals(VectorClock.Comparison.BEFORE, clockTest.compareTo(clock2));
        clockTest.clocks.put(10, 0);
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clock2.compareTo(clockTest));
        Assert.assertEquals(VectorClock.Comparison.NOT_COMPARABLE, clockTest.compareTo(clock2));

    }

}