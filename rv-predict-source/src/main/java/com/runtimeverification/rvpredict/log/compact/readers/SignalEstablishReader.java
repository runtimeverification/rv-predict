package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;

import java.util.Arrays;

public class SignalEstablishReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventFactory, element) ->
                        compactEventFactory.establishSignal(
                                context,
                                element.handler.getAsLong(),
                                element.signalNumber.getAsLong(),
                                element.signalMask.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
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
