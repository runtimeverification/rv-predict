package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.util.Arrays;

public class AtomicReadModifyWriteReader {
    public static CompactEventReader.Reader createReader(int dataSizeInBytes) {
        return new SimpleDataReader<>(
                header -> new TraceElement(header, dataSizeInBytes),
                (context, compactEventFactory, element) -> compactEventFactory.atomicReadModifyWrite(
                        context,
                        dataSizeInBytes,
                        element.address.getAsLong(),
                        element.readValue.getAsLong(),
                        element.writeValue.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final Address address;
        private final VariableInt readValue;
        private final VariableInt writeValue;

        private TraceElement(TraceHeader header, int valueSizeInBytes)
                throws InvalidTraceDataException {
            address = new Address(header);
            readValue = new VariableInt(header, valueSizeInBytes);
            writeValue = new VariableInt(header, valueSizeInBytes);
            setData(Arrays.asList(address, readValue, writeValue));
        }
    }
}
