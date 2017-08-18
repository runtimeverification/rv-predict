package com.runtimeverification.rvpredict.producerframework;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LeafProducerWrapperTest {
    @Mock private LeafProducer<Integer> mockLeafProducer;
    @Mock private ComputingProducer mockDependingProducer;
    @Mock private ProducerModule mockProducerModule;

    @Test
    public void constructorRegistersTheProducer() {
        new LeafProducerWrapper<>(mockLeafProducer, mockProducerModule);
        verify(mockProducerModule).addProducer(mockLeafProducer);
    }

    @Test
    public void setsTheValue() {
        LeafProducerWrapper<Integer, LeafProducer<Integer>> wrapper =
                new LeafProducerWrapper<>(mockLeafProducer, mockProducerModule);
        wrapper.set(10);
        verify(mockLeafProducer).set(10);
    }

    @Test
    public void getComputedEnsuresThatComputationTakesPlace() {
        LeafProducerWrapper<Integer, LeafProducer<Integer>> wrapper =
                new LeafProducerWrapper<>(mockLeafProducer, mockProducerModule);
        Assert.assertEquals(mockLeafProducer, wrapper.getComputed());
        verify(mockLeafProducer).ensureComputed();
    }

    @Test
    public void registersDependency() {
        LeafProducerWrapper<Integer, LeafProducer<Integer>> wrapper =
                new LeafProducerWrapper<>(mockLeafProducer, mockProducerModule);
        Assert.assertEquals(mockLeafProducer, wrapper.getAndRegister(mockDependingProducer));
        verify(mockDependingProducer).registerDependency(mockLeafProducer);
    }
}
