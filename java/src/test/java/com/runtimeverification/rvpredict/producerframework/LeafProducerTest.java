package com.runtimeverification.rvpredict.producerframework;

import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

public class LeafProducerTest {
    @Test
    public void crashesWithoutAValueButWorksWithOne() {
        LeafProducer<Integer> producer = new LeafProducer<Integer>() {
        };

        MoreAsserts.assertException(IllegalStateException.class, producer::ensureComputed);
        MoreAsserts.assertException(IllegalStateException.class, producer::get);

        producer.set(10);

        producer.ensureComputed();
        Assert.assertEquals(10, producer.get().intValue());
    }

    @Test
    public void resetClearsTheValue() {
        LeafProducer<Integer> producer = new LeafProducer<Integer>() {
        };

        producer.set(10);

        producer.ensureComputed();
        Assert.assertEquals(10, producer.get().intValue());

        producer.reset();

        MoreAsserts.assertException(IllegalStateException.class, producer::ensureComputed);
        MoreAsserts.assertException(IllegalStateException.class, producer::get);
    }
}
