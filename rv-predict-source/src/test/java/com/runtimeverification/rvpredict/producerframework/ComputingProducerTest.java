package com.runtimeverification.rvpredict.producerframework;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ComputingProducerTest {
    @Mock private ProducerState mockProducerState;
    @Mock private Producer mockProducer;

    @Test
    public void ensureComputedCallsComputeByDefault() {
        MutableBoolean computed = new MutableBoolean(false);
        ComputingProducer<ProducerState> producer = new ComputingProducer<ProducerState>(mockProducerState) {
            @Override
            protected void compute() {
                computed.set(true);
            }
        };
        producer.ensureComputed();

        Assert.assertTrue(computed.get());
    }

    @Test
    public void ensureComputedIsNotCalledTwiceWithoutReset() {
        MutableBoolean computed = new MutableBoolean(false);
        ComputingProducer<ProducerState> producer = new ComputingProducer<ProducerState>(mockProducerState) {
            @Override
            protected void compute() {
                computed.set(true);
            }
        };
        producer.ensureComputed();
        Assert.assertTrue(computed.get());
        computed.set(false);
        producer.ensureComputed();
        Assert.assertFalse(computed.get());
        producer.reset();
        producer.ensureComputed();
        Assert.assertTrue(computed.get());
    }

    @Test
    public void dependenciesAreComputedBefore() {
        MutableBoolean computed = new MutableBoolean(false);
        ComputingProducer<ProducerState> producer = new ComputingProducer<ProducerState>(mockProducerState) {
            @Override
            protected void compute() {
                computed.set(true);
            }
        };
        producer.registerDependency(mockProducer);

        producer.ensureComputed();
        Assert.assertTrue(computed.get());
        verify(mockProducer, times(1)).ensureComputed();

        computed.set(false);
        producer.ensureComputed();
        verify(mockProducer, times(1)).ensureComputed();
        Assert.assertFalse(computed.get());

        producer.reset();
        producer.ensureComputed();
        verify(mockProducer, times(2)).ensureComputed();
        Assert.assertTrue(computed.get());
    }

    @Test
    public void resetAlsoResetsTheState() {
        MutableBoolean computed = new MutableBoolean(false);
        ComputingProducer<ProducerState> producer = new ComputingProducer<ProducerState>(mockProducerState) {
            @Override
            protected void compute() {
                computed.set(true);
            }
        };
        producer.ensureComputed();
        Assert.assertTrue(computed.get());
        computed.set(false);
        verify(mockProducerState, never()).reset();

        producer.ensureComputed();
        Assert.assertFalse(computed.get());
        verify(mockProducerState, never()).reset();

        producer.reset();
        producer.ensureComputed();
        Assert.assertTrue(computed.get());
        verify(mockProducerState).reset();
    }

    private class MutableBoolean {
        private boolean value;

        private MutableBoolean(boolean value) {
            this.value = value;
        }

        private void set(boolean value) {
            this.value = value;
        }

        private boolean get() {
            return value;
        }
    }
}
