package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMask;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.util.Arrays;

public class SignalMaskMemoizationReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventReader, element) ->
                        compactEventReader.signalMaskMemoization(
                                context,
                                element.signalMask.getAsLong(),
                                element.originBitCount.getAsLong(),
                                element.signalMaskNumber.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final SignalMask signalMask;
        private final VariableInt originBitCount;
        private final SignalMaskNumber signalMaskNumber;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            signalMask = new SignalMask(header);
            originBitCount = new VariableInt(header, 4);
            signalMaskNumber = new SignalMaskNumber(header);

            setData(Arrays.asList(signalMask, originBitCount, signalMaskNumber));
        }
    }
}