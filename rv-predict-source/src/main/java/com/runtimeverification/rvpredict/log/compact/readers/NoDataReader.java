package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
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

    private final BiFunctionWithException<CompactEventFactory, Context, List<CompactEvent>> eventFactory;

    public NoDataReader(BiFunctionWithException<CompactEventFactory, Context, List<CompactEvent>> eventFactory) {
        this.eventFactory = eventFactory;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return 0;
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventFactory compactEventFactory, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        return eventFactory.apply(compactEventFactory, context);
    }
}
