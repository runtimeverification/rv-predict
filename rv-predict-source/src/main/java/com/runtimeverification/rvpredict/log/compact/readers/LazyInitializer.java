package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

// TODO(virgil): This may not need to be thread safe, so it should be optimized.
class LazyInitializer<T> {
    private final Factory<T> factory;
    private volatile T object;

    LazyInitializer(Factory<T> factory) {
        this.factory = factory;
    }

    public interface Factory<S> {
        S create(TraceHeader header) throws InvalidTraceDataException;
    }

    T getInit(TraceHeader header) throws InvalidTraceDataException {
        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    object = factory.create(header);
                }
            }
        }
        return object;
    }
}
