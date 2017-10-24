package com.runtimeverification.rvpredict.producerframework;

public class ComputingProducerWrapper<T extends Producer> {
    private final T producer;
    public ComputingProducerWrapper(T producer, ProducerModule module) {
        this.producer = producer;
        module.addProducer(producer);
    }

    public T getComputed() {
        producer.ensureComputed();
        return producer;
    }

    public T getAndRegister(ComputingProducer dependentProducer) {
        dependentProducer.registerDependency(producer);
        return producer;
    }
}
