package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class SignalEstablishReader implements CompactEventReader.Reader {
    private final LazyInitializer<TraceElement> reader = new LazyInitializer<>(TraceElement::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return compactEventReader.establishSignal(
                context,
                element.handler.getAsLong(),
                element.signalNumber.getAsLong(),
                element.signalMask.getAsLong());
    }

    private class TraceElement extends ReadableAggregateData {
        private Address handler;
        private SignalNumber signalNumber;
        private SignalMaskNumber signalMask;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            handler = new Address(header);
            signalNumber = new SignalNumber(header);
            signalMask = new SignalMaskNumber(header);

            setData(Arrays.asList(handler, signalNumber, signalMask));
        }
    }
}
