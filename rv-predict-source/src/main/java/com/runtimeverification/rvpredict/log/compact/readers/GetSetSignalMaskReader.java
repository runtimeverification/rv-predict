package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;

import java.util.Arrays;

public class GetSetSignalMaskReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                header -> new TraceElement(header),
                (context, compactEventFactory, element) -> compactEventFactory.readWriteSignalMask(
                        context,
                        element.readMask.getAsLong(),
                        element.writeMask.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final SignalMaskNumber readMask;
        private final SignalMaskNumber writeMask;

        private TraceElement(TraceHeader header)
                throws InvalidTraceDataException {
            readMask = new SignalMaskNumber(header);
            writeMask = new SignalMaskNumber(header);
            setData(Arrays.asList(readMask, writeMask));
        }
    }
}
