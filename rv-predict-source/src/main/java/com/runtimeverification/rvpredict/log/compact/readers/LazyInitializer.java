package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

/**
 * Creates one object of type T at the first call of the {@link #getInit(TraceHeader)}
 * method, which then returns the object. All subsequent calls will return the same
 * object. The object is created by using the {@link Factory} provided to the constructor.
 *
 * Not thread safe.
 */
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
            object = factory.create(header);
        }
        return object;
    }
}
