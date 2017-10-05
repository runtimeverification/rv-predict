package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.MultipartReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Reads multi-part data, building a list of events.
 * @param <T> The exact type of multipart reader to be used.
 */
public class MultipartDataReader<T extends MultipartReadableAggregateData> implements
        CompactEventReader.Reader {
    private final T data;
    private final SimpleDataReader.ReadableDataToEventListConverter<T> converter;

    MultipartDataReader(
            T data,
            SimpleDataReader.ReadableDataToEventListConverter<T> converter) {
        this.data = data;
        this.converter = converter;
    }

    @Override
    public void startReading(TraceHeader header) throws InvalidTraceDataException {
        data.startReading(header);
    }

    @Override
    public boolean stillHasPartsToRead() {
        return data.stillHasPartsToRead();
    }

    @Override
    public int nextPartSize(TraceHeader header) throws InvalidTraceDataException {
        return data.nextPartSize();
    }

    @Override
    public void addPart(ByteBuffer buffer, TraceHeader header) throws InvalidTraceDataException {
        data.readNextPartAndAdvance(header, buffer);
    }

    @Override
    public List<ReadonlyEventInterface> build(
            Context context, CompactEventFactory compactEventFactory, TraceHeader header)
            throws InvalidTraceDataException {
        return converter.dataElementToEvent(context, compactEventFactory, data);
    }
}
