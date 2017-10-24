package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;

import java.util.Arrays;
import java.util.OptionalLong;

public class FunctionEnterReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventFactory, element) ->
                        compactEventFactory.enterFunction(
                                context, element.canonicalFrameAddress.getAsLong(),
                                optionalFromZeroable(element.callSite.getAsLong())));
    }

    private static OptionalLong optionalFromZeroable(long l) {
        return l == 0 ? OptionalLong.empty() : OptionalLong.of(l);
    }

    private static class TraceElement extends ReadableAggregateData {
        private final Address canonicalFrameAddress;
        private final Address callSite;

        private TraceElement(TraceHeader header)
                throws InvalidTraceDataException {
            canonicalFrameAddress = new Address(header);
            callSite = new Address(header);
            setData(Arrays.asList(canonicalFrameAddress, callSite));
        }
    }
}
