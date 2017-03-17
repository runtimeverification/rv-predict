package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Address;
import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.VariableInt;

import java.nio.ByteBuffer;

public class AtomicReadModifyWriteReader implements Event.Reader{
    private final int dataSizeInBytes;
    private ReaderInitializer<TraceElement> reader;

    public AtomicReadModifyWriteReader(int dataSizeInBytes) {
        this.dataSizeInBytes = dataSizeInBytes;
        reader = new ReaderInitializer<>(header -> new TraceElement(header, dataSizeInBytes));
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return Event.atomicReadModifyWrite(
                dataSizeInBytes,
                element.address.getAsLong(),
                element.readValue.getAsLong(),
                element.writeValue.getAsLong());
    }

    private class TraceElement extends ReadableAggregateData {
        private final Address address;
        private final VariableInt readValue;
        private final VariableInt writeValue;

        private TraceElement(TraceHeader header, int valueSizeInBytes)
                throws InvalidTraceDataException {
            address = new Address(header);
            readValue = new VariableInt(header, valueSizeInBytes);
            writeValue = new VariableInt(header, valueSizeInBytes);
            throw new InvalidTraceDataException("Atomic read-modifi-write operations are not supported.");
            // setData(Arrays.asList(address, value));
        }
    }
}
