package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;

public class SignalMaskAtWindowStartLeafTest {
    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void returnsValue() {
        LeafProducerWrapper<Map<Integer, SignalMask>, SignalMaskAtWindowStartLeaf> producer =
                new LeafProducerWrapper<>(new SignalMaskAtWindowStartLeaf(), module);

        module.reset();
        producer.set(ImmutableMap.of(1, SignalMask.UNKNOWN_MASK));
        Assert.assertThat(producer.getComputed().getMasks(), hasMapSize(1));
        // Intentional == comparison.
        Assert.assertTrue(SignalMask.UNKNOWN_MASK == producer.getComputed().getMasks().get(1));
    }
}
