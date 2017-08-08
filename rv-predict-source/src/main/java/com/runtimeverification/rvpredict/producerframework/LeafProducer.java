package com.runtimeverification.rvpredict.producerframework;

import java.util.Optional;

public abstract class LeafProducer<T> extends Producer {
    private Optional<T> value = Optional.empty();

    void set(T value) {
        this.value = Optional.of(value);
    }

    protected T get() {
        assert value.isPresent() : this.getClass();
        return value.get();
    }

    @Override
    protected void reset() {
        this.value = Optional.empty();
    }

    @Override
    protected void ensureComputed() {
        assert value.isPresent() : this.getClass();
    }
}
