package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;

import java.util.Arrays;

public class SignalEnterReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventFactory, element) ->
                        compactEventFactory.enterSignal(
                                context,
                                element.generation.getAsLong(),
                                element.signalNumber.getAsLong(),
                                element.handler.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final Address handler;
        private final Generation generation;
        private final SignalNumber signalNumber;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            handler = new Address(header);
            generation = new Generation(header);
            signalNumber = new SignalNumber(header);
            setData(Arrays.asList(handler, generation, signalNumber));
        }
    }
}
