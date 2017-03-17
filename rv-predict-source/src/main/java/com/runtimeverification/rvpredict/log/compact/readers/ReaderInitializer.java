package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

public class ReaderInitializer<T> {
    private final ReaderFactory<T> factory;
    private T reader;

    ReaderInitializer(ReaderFactory<T> factory) {
        this.factory = factory;
    }

    public interface ReaderFactory<S> {
        S create(TraceHeader header) throws InvalidTraceDataException;
    }

    public T getInit(TraceHeader header) throws InvalidTraceDataException {
        if (reader != null) {
            return reader;
        }
        return factory.create(header);
    }
}
