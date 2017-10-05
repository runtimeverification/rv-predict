package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * This class should be used for aggregate data types that do not have any actual data besides the
 * delta-operation header, e.g. the thread end event.
 */
public class NoDataReader implements CompactEventReader.Reader {
    @FunctionalInterface
    public interface BiFunctionWithException<T, U, R> {
        R apply(T t, U u) throws InvalidTraceDataException;
    }

    private final BiFunctionWithException<CompactEventFactory, Context, List<ReadonlyEventInterface>> eventFactory;

    public NoDataReader(
            BiFunctionWithException<CompactEventFactory, Context, List<ReadonlyEventInterface>> eventFactory) {
        this.eventFactory = eventFactory;
    }

    @Override
    public void startReading(TraceHeader header) {
    }

    @Override
    public boolean stillHasPartsToRead() {
        return false;
    }

    @Override
    public int nextPartSize(TraceHeader header) throws InvalidTraceDataException {
        return 0;
    }

    @Override
    public void addPart(ByteBuffer buffer, TraceHeader header) throws InvalidTraceDataException {
    }

    @Override
    public List<ReadonlyEventInterface> build(
            Context context, CompactEventFactory compactEventFactory, TraceHeader header)
            throws InvalidTraceDataException {
        return eventFactory.apply(compactEventFactory, context);
    }
}
