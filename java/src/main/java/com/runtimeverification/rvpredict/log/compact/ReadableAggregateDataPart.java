package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Reader for a chunk of data, part of a multipart data. These are used when the data structure is not fully
 * known before reading it, e.g. when it has fields of variable size.
 */
public class ReadableAggregateDataPart<S extends ReadableData, T extends ReadableData> {
    private final Optional<ReadableAggregateDataPart<T, ? extends ReadableData>> previousReader;
    private final Initializer<S, T> initializer;
    private Optional<S> data = Optional.empty();

    /**
     * @param previousReader The previous data part, if any.
     * @param initializer Object that creates the current data.
     */
    public ReadableAggregateDataPart(
            Optional<ReadableAggregateDataPart<T, ? extends ReadableData>> previousReader,
            Initializer<S, T> initializer) {
        this.previousReader = previousReader;
        this.initializer = initializer;
    }

    /**
     * Initializes the current data. Assumes that the previous data (if any) was already read.
     */
    public void initialize(TraceHeader header) throws InvalidTraceDataException {
        Optional<T> previous = Optional.empty();
        if (previousReader.isPresent()) {
            previous = Optional.of(previousReader.get().getValue());
        }
        data = Optional.of(initializer.initialize(header, previous));
    }

    /**
     * {@link #initialize(TraceHeader)} must be called before this.
     */
    public int size() {
        assert data.isPresent();
        return data.get().size();

    }

    /**
     * {@link #initialize(TraceHeader)} must be called before this.
     */
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        assert data.isPresent();
        data.get().read(buffer);
    }

    public S getValue() {
        assert data.isPresent();
        return data.get();
    }

    /**
     * Creates data given the previous part data.
     */
    @FunctionalInterface
    public interface Initializer<S extends ReadableData, T extends ReadableData> {
        S initialize(TraceHeader header, Optional<T> previousReader) throws InvalidTraceDataException;
    }
}
