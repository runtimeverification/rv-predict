package com.runtimeverification.rvpredict.producerframework;

import java.util.ArrayList;
import java.util.List;

public abstract class ComputingProducer extends Producer {
    private final List<Producer> dependencies;
    private boolean isComputed = false;

    protected ComputingProducer() {
        this.dependencies = new ArrayList<>();
    }

    void registerDependency(Producer producer) {
        dependencies.add(producer);
    }

    @Override
    protected void reset() {
        isComputed = false;
    }

    protected void ensureComputed() {
        if (!isComputed) {
            dependencies.forEach(Producer::ensureComputed);
            compute();
            isComputed = true;
        }
    }

    protected abstract void compute();
}
