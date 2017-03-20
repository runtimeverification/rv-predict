package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

public class NoDataReader implements CompactEvent.Reader {
    private final Supplier<Event> eventSupplier;

    public NoDataReader(Supplier<Event> eventSupplier) {
        this.eventSupplier = eventSupplier;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return 0;
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        return eventSupplier.get();
    }
}
