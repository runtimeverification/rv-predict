package com.runtimeverification.rvpredict.producerframework;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ComputingProducerWrapperTest {
    @Mock private ComputingProducer mockComputingProducer;
    @Mock private ComputingProducer mockDependingProducer;
    @Mock private ProducerModule mockProducerModule;

    @Test
    public void constructorRegistersTheProducer() {
        new ComputingProducerWrapper<>(mockComputingProducer, mockProducerModule);
        verify(mockProducerModule).addProducer(mockComputingProducer);
    }

    @Test
    public void getComputedEnsuresThatComputationTakesPlace() {
        ComputingProducerWrapper<ComputingProducer> wrapper =
                new ComputingProducerWrapper<>(mockComputingProducer, mockProducerModule);
        Assert.assertEquals(mockComputingProducer, wrapper.getComputed());
        verify(mockComputingProducer).ensureComputed();
    }

    @Test
    public void registersDependency() {
        ComputingProducerWrapper<ComputingProducer> wrapper =
                new ComputingProducerWrapper<>(mockComputingProducer, mockProducerModule);
        Assert.assertEquals(mockComputingProducer, wrapper.getAndRegister(mockDependingProducer));
        verify(mockDependingProducer).registerDependency(mockComputingProducer);
    }
}
