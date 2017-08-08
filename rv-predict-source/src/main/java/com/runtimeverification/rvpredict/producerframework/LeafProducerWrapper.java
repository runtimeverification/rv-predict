package com.runtimeverification.rvpredict.producerframework;

public class LeafProducerWrapper<S, T extends LeafProducer<S>> extends ComputingProducerWrapper<T> {
    private final T producer;

    public LeafProducerWrapper(T producer, ProducerModule module) {
        super(producer, module);
        this.producer = producer;
    }

    public void set(S value) {
        producer.set(value);
    }
}
