package com.runtimeverification.rvpredict.producerframework;

public abstract class Producer {
    protected abstract void reset();
    protected abstract void ensureComputed();
}
