package com.runtimeverification.rvpredict.producerframework;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ProducerModuleTest {
    @Mock private Producer mockProducer;

    @Test
    public void resetsProducers() {
        ProducerModule module = new ProducerModule();
        module.addProducer(mockProducer);
        module.reset();
        verify(mockProducer).reset();
    }
}
