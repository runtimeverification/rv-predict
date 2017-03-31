package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

class SimpleDataReader<T extends ReadableData> implements CompactEventReader.Reader {
    private final LazyInitializer<T> reader;
    private final ReadableDataToEventListConverter<T> converter;

    interface ReadableDataToEventListConverter<T> {
        List<CompactEvent> dataElementToEvent(
                Context context, CompactEventReader compactEventReader, T element)
                throws InvalidTraceDataException;
    }

    SimpleDataReader(
            LazyInitializer.Factory<T> readerFactory, ReadableDataToEventListConverter<T> converter) {
        this.reader = new LazyInitializer<T>(readerFactory);
        this.converter = converter;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        T element = reader.getInit(header);
        element.read(buffer);
        return converter.dataElementToEvent(context, compactEventReader, element);
    }
}
