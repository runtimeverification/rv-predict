package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.MultipartReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateDataPart;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.StringData;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * Reads a shared library event. A shared library event is composed of:
 * 4-bytes library id
 * 4-bytes library name length
 * ASCII library name characters, padded to finish at a 4-bytes boundary.
 */
public class SharedLibraryReader extends MultipartDataReader<SharedLibraryReader.TraceElement> {
    private SharedLibraryReader() {
        super(new TraceElement(),
                ((context, compactEventFactory, element) -> compactEventFactory.sharedLibrary(
                        context,
                        element.firstPart.getValue().id.getAsLong(),
                        element.secondPart.getValue().name.getAsString())));
    }

    public static CompactEventReader.Reader createReader() {
        return new SharedLibraryReader();
    }

    static class TraceElement extends MultipartReadableAggregateData {
        private final ReadableAggregateDataPart<SharedLibraryReaderDataPart1, ReadableAggregateData> firstPart;
        private final ReadableAggregateDataPart<SharedLibraryReaderDataPart2, SharedLibraryReaderDataPart1>
                secondPart;

        private TraceElement() {
            this.firstPart = new ReadableAggregateDataPart<>(
                    Optional.empty(), (header, previous) -> new SharedLibraryReaderDataPart1(header));
            this.secondPart = new ReadableAggregateDataPart<>(
                    Optional.of(this.firstPart),
                    (header, previous) -> new SharedLibraryReaderDataPart2(header, previous.get()));
            setData(firstPart, secondPart);
        }
    }

    static class SharedLibraryReaderDataPart1 extends ReadableAggregateData {
        private final VariableInt id;
        private final VariableInt nameLength;

        private SharedLibraryReaderDataPart1(TraceHeader header) throws InvalidTraceDataException {
            id = new VariableInt(header, 4);
            nameLength = new VariableInt(header, 4);
            this.setData(Arrays.asList(id, nameLength));
        }
    }

    static class SharedLibraryReaderDataPart2 extends ReadableAggregateData {
        private final StringData name;
        private SharedLibraryReaderDataPart2(TraceHeader header, SharedLibraryReaderDataPart1 previousData)
                throws InvalidTraceDataException {
            name = new StringData(header, Math.toIntExact(previousData.nameLength.getAsLong()));
            this.setData(Collections.singletonList(name));
        }
    }
}
