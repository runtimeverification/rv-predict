package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class DataManipulationReader implements CompactEvent.Reader {
    private final int dataSizeInBytes;
    private final CompactEvent.DataManipulationType dataManipulationType;
    private final CompactEvent.Atomicity atomicity;
    private final LazyInitializer<TraceElement> reader;

    public DataManipulationReader(
            int sizeInBytes, CompactEvent.DataManipulationType dataManipulationType, CompactEvent.Atomicity atomicity) {
        this.dataSizeInBytes = sizeInBytes;
        this.dataManipulationType = dataManipulationType;
        this.atomicity = atomicity;
        this.reader = new LazyInitializer<>(header -> new TraceElement(header, dataSizeInBytes));
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return CompactEvent.dataManipulation(
                context,
                dataManipulationType,
                dataSizeInBytes,
                element.address.getAsLong(),
                element.value.getAsLong(),
                atomicity);
    }

    private class TraceElement extends ReadableAggregateData {
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
