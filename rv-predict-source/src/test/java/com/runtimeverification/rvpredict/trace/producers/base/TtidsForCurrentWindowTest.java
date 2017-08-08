package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;

public class TtidsForCurrentWindowTest {
    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void setsValue() {
        LeafProducerWrapper<Collection<Integer>, TtidsForCurrentWindow> producer =
                new LeafProducerWrapper<>(new TtidsForCurrentWindow(), module);
        producer.set(ImmutableList.of(1, 2, 3));

        Assert.assertThat(producer.getComputed().getTtids(), containsExactly(1, 2, 3));
    }
}
