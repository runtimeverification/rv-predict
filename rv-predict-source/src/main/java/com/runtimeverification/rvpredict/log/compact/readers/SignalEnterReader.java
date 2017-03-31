package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class SignalEnterReader implements CompactEvent.Reader {
    private final LazyInitializer<TraceElement> reader = new LazyInitializer<>(TraceElement::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return CompactEvent.enterSignal(
                context,
                element.generation.getAsLong(),
                element.signalNumber.getAsLong());
    }

    private static class TraceElement extends ReadableAggregateData {
        private final Generation generation;
        private final SignalNumber signalNumber;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            generation = new Generation(header);
            signalNumber = new SignalNumber(header);
            setData(Arrays.asList(generation, signalNumber));
        }
    }
}
