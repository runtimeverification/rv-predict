package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

/**
 * Reads a shared library event segment. A shared library segment event is composed of:
 * 4-bytes library id
 * segment start pointer
 * 4-bytes segment length
 */
public class SharedLibrarySegment {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventFactory, element) ->
                        compactEventFactory.sharedLibrarySegment(
                                context,
                                element.libraryId.getAsLong(),
                                element.start.getAsLong(),
                                element.size.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final VariableInt libraryId;
        private final Address start;
        private final VariableInt size;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            libraryId = new VariableInt(header, 4);
            start = new Address(header);
            size = new VariableInt(header, 4);
            setData(libraryId, start, size);
        }
    }
}
