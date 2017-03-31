package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

/**
 * This class should be used for aggregate data types that do not have any actual data besides the
 * delta-operation header, e.g. the thread end event.
 */
public class NoDataReader implements CompactEventReader.Reader {
    private final Function<Context, List<CompactEvent>> eventFactory;

    public NoDataReader(Function<Context, List<CompactEvent>> eventFactory) {
        this.eventFactory = eventFactory;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return 0;
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        return eventFactory.apply(context);
    }
}
