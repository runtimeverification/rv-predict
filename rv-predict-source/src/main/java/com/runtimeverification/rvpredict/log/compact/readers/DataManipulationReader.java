package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.util.Arrays;

public class DataManipulationReader {
    public static CompactEventReader.Reader createReader(
            int dataSizeInBytes,
            CompactEventReader.DataManipulationType dataManipulationType,
            CompactEventReader.Atomicity atomicity) {
        return new SimpleDataReader<>(
                header -> new TraceElement(header, dataSizeInBytes),
                (context, compactEventReader, element) ->
                        compactEventReader.dataManipulation(
                                context,
                                dataManipulationType,
                                dataSizeInBytes,
                                element.address.getAsLong(),
                                element.value.getAsLong(),
                                atomicity));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final Address address;
        private final VariableInt value;

        private TraceElement(TraceHeader header, int valueSizeInBytes)
                throws InvalidTraceDataException {
            address = new Address(header);
            value = new VariableInt(header, valueSizeInBytes);
            setData(Arrays.asList(address, value));
        }
    }
}
