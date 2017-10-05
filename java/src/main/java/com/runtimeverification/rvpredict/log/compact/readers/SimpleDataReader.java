package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

class SimpleDataReader<T extends ReadableData> implements CompactEventReader.Reader {
    private final LazyInitializer<T> reader;
    private final ReadableDataToEventListConverter<T> converter;
    private Optional<T> data = Optional.empty();

    @Override
    public void startReading(TraceHeader header) throws InvalidTraceDataException {
        data = Optional.empty();
    }

    @Override
    public boolean stillHasPartsToRead() {
        return !data.isPresent();
    }

    @Override
    public int nextPartSize(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public void addPart(ByteBuffer buffer, TraceHeader header) throws InvalidTraceDataException {
        T element = reader.getInit(header);
        element.read(buffer);
        data = Optional.of(element);
    }

    @Override
    public List<ReadonlyEventInterface> build(
        Context context, CompactEventFactory compactEventFactory, TraceHeader header)
            throws InvalidTraceDataException {
        assert data.isPresent();
        return converter.dataElementToEvent(context, compactEventFactory, data.get());
    }

    interface ReadableDataToEventListConverter<T> {
        List<ReadonlyEventInterface> dataElementToEvent(
                Context context, long originalEventId,
                CompactEventFactory compactEventFactory, T element)
                throws InvalidTraceDataException;
    }

    SimpleDataReader(
            LazyInitializer.Factory<T> readerFactory, ReadableDataToEventListConverter<T> converter) {
        this.reader = new LazyInitializer<>(readerFactory);
        this.converter = converter;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<ReadonlyEventInterface> readEvent(
            Context context, long originalEventId,
            CompactEventFactory compactEventFactory, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        T element = reader.getInit(header);
        element.read(buffer);
        return converter.dataElementToEvent(context, originalEventId, compactEventFactory, element);
    }
}
