package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Address;
import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataManipulationReader implements Event.Reader {
    private final int dataSizeInBytes;
    private final Event.DataManipulationType dataManipulationType;
    private final Event.Atomicity atomicity;
    private final ReaderInitializer<TraceElement> reader;

    public DataManipulationReader(
            int sizeInBytes, Event.DataManipulationType dataManipulationType, Event.Atomicity atomicity) {
        this.dataSizeInBytes = sizeInBytes;
        this.dataManipulationType = dataManipulationType;
        this.atomicity = atomicity;
        this.reader = new ReaderInitializer<>(header -> new TraceElement(header, dataSizeInBytes));
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return Event.dataManipulation(
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
