package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
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

public class AtomicReadModifyWriteReader implements CompactEventReader.Reader {
    private final int dataSizeInBytes;
    private final LazyInitializer<TraceElement> reader;

    public AtomicReadModifyWriteReader(int dataSizeInBytes) {
        this.dataSizeInBytes = dataSizeInBytes;
        reader = new LazyInitializer<>(header -> new TraceElement(header, dataSizeInBytes));
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement element = reader.getInit(header);
        element.read(buffer);
        return compactEventReader.atomicReadModifyWrite(
                context,
                dataSizeInBytes,
                element.address.getAsLong(),
                element.readValue.getAsLong(),
                element.writeValue.getAsLong());
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
