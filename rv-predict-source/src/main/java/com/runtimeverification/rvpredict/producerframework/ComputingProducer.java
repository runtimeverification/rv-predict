package com.runtimeverification.rvpredict.producerframework;

import java.util.ArrayList;
import java.util.List;

public abstract class ComputingProducer<T extends ProducerState> extends Producer {
    private final List<Producer> dependencies;
    private final T state;
    private boolean isComputed = false;

    protected ComputingProducer(T state) {
        this.dependencies = new ArrayList<>();
        this.state = state;
    }

    void registerDependency(Producer producer) {
        dependencies.add(producer);
    }

    @Override
    protected void reset() {
        isComputed = false;
        state.reset();
    }

    protected void ensureComputed() {
        if (!isComputed) {
            dependencies.forEach(Producer::ensureComputed);
            compute();
            isComputed = true;
        }
    }

    protected T getState() {
        return state;
    }

    protected abstract void compute();
}
