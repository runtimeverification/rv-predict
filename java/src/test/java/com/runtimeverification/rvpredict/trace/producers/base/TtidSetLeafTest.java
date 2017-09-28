package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableSet;
import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;

public class TtidSetLeafTest {
    private TestProducerModule module = new TestProducerModule();

    @Test
    public void setsValue() {
        LeafProducerWrapper<Set<Integer>, TtidSetLeaf> producer = new LeafProducerWrapper<>(new TtidSetLeaf(), module);

        producer.set(ImmutableSet.of(1, 2, 3));

        Assert.assertThat(producer.getComputed().getTtids(), containsExactly(1, 2, 3));
        Assert.assertTrue(producer.getComputed().contains(1));
        Assert.assertTrue(producer.getComputed().contains(2));
        Assert.assertTrue(producer.getComputed().contains(3));
        Assert.assertFalse(producer.getComputed().contains(4));
    }
}
