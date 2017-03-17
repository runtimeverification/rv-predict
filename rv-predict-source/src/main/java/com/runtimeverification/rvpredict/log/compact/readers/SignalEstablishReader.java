package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Address;
import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SignalEstablishReader implements Event.Reader {
    private ReaderInitializer<TraceElement> reader = new ReaderInitializer<>(TraceElement::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return Event.establishSignal(
                element.handler.getAsLong(),
                element.signalNumber.getAsLong(),
                element.mask.getAsLong());
    }

    private class TraceElement extends ReadableAggregateData {
        private Address handler;
        private VariableInt signalNumber;
        private VariableInt mask;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            handler = new Address(header);
            signalNumber = new VariableInt(header, 4);
            mask = new VariableInt(header, 4);

            setData(Arrays.asList(handler, signalNumber, mask));
        }
    }
}
