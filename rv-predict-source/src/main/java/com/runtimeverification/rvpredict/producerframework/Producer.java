package com.runtimeverification.rvpredict.producerframework;

public abstract class Producer {
    abstract void reset();
    protected abstract void ensureComputed();

}
