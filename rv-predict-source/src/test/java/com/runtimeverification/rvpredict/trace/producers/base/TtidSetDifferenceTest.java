package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.TtidSetLeafUtils.fillMockTtidSetLeaf;

@RunWith(MockitoJUnitRunner.class)
public class TtidSetDifferenceTest {
    @Mock private TtidSetLeaf mockAllTtids;
    @Mock private TtidSetLeaf mockTtidsToBeRemoved;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void noTtidForEmptySets() {
        ComputingProducerWrapper<TtidSetDifference> producer =
                initProducer(module, mockAllTtids, mockTtidsToBeRemoved);

        module.reset();

        Assert.assertThat(producer.getComputed().getTtids(), isEmpty());
        Assert.assertFalse(producer.getComputed().contains(1));
        Assert.assertFalse(producer.getComputed().contains(2));
        Assert.assertFalse(producer.getComputed().contains(3));
        Assert.assertFalse(producer.getComputed().contains(4));
        Assert.assertFalse(producer.getComputed().contains(5));
        Assert.assertFalse(producer.getComputed().contains(6));
    }

    @Test
    public void noTtidForEmptyAllSet() {
        ComputingProducerWrapper<TtidSetDifference> producer =
                initProducer(module, mockAllTtids, mockTtidsToBeRemoved);

        fillMockTtidSetLeaf(mockTtidsToBeRemoved, 1, 2, 3);

        module.reset();

        Assert.assertThat(producer.getComputed().getTtids(), isEmpty());
        Assert.assertFalse(producer.getComputed().contains(1));
        Assert.assertFalse(producer.getComputed().contains(2));
        Assert.assertFalse(producer.getComputed().contains(3));
        Assert.assertFalse(producer.getComputed().contains(4));
        Assert.assertFalse(producer.getComputed().contains(5));
        Assert.assertFalse(producer.getComputed().contains(6));
    }

    @Test
    public void computesSetDifference() {
        ComputingProducerWrapper<TtidSetDifference> producer =
                initProducer(module, mockAllTtids, mockTtidsToBeRemoved);

        fillMockTtidSetLeaf(mockAllTtids, 2, 3, 4, 5);
        fillMockTtidSetLeaf(mockTtidsToBeRemoved, 1, 2, 3);

        module.reset();

        Assert.assertThat(producer.getComputed().getTtids(), containsExactly(4, 5));
        Assert.assertFalse(producer.getComputed().contains(1));
        Assert.assertFalse(producer.getComputed().contains(2));
        Assert.assertFalse(producer.getComputed().contains(3));
        Assert.assertTrue(producer.getComputed().contains(4));
        Assert.assertTrue(producer.getComputed().contains(5));
        Assert.assertFalse(producer.getComputed().contains(6));
    }

    @Test
    public void resets() {
        ComputingProducerWrapper<TtidSetDifference> producer =
                initProducer(module, mockAllTtids, mockTtidsToBeRemoved);

        fillMockTtidSetLeaf(mockAllTtids, 2, 3, 4, 5);
        fillMockTtidSetLeaf(mockTtidsToBeRemoved, 1, 2, 3);

        module.reset();

        Assert.assertThat(producer.getComputed().getTtids(), containsExactly(4, 5));
        Assert.assertFalse(producer.getComputed().contains(1));
        Assert.assertFalse(producer.getComputed().contains(2));
        Assert.assertFalse(producer.getComputed().contains(3));
        Assert.assertTrue(producer.getComputed().contains(4));
        Assert.assertTrue(producer.getComputed().contains(5));
        Assert.assertFalse(producer.getComputed().contains(6));

        fillMockTtidSetLeaf(mockAllTtids, 3, 6);
        fillMockTtidSetLeaf(mockTtidsToBeRemoved, 1, 2, 3);

        module.reset();

        Assert.assertThat(producer.getComputed().getTtids(), containsExactly(6));
        Assert.assertFalse(producer.getComputed().contains(1));
        Assert.assertFalse(producer.getComputed().contains(2));
        Assert.assertFalse(producer.getComputed().contains(3));
        Assert.assertFalse(producer.getComputed().contains(4));
        Assert.assertFalse(producer.getComputed().contains(5));
        Assert.assertTrue(producer.getComputed().contains(6));
    }

    private static ComputingProducerWrapper<TtidSetDifference> initProducer(
            ProducerModule module,
            TtidSetLeaf mockAllTtids,
            TtidSetLeaf mockTtidsToBeRemoved) {
        return new ComputingProducerWrapper<>(
                new TtidSetDifference(
                        new ComputingProducerWrapper<>(mockAllTtids, module),
                        new ComputingProducerWrapper<>(mockTtidsToBeRemoved, module)),
                module);
    }
}
