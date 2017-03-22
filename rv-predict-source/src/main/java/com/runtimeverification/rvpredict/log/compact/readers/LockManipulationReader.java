package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class LockManipulationReader implements CompactEvent.Reader {
    private final CompactEvent.LockManipulationType lockManipulationType;
    private LazyInitializer<Address> reader = new LazyInitializer<>(Address::new);

    public LockManipulationReader(CompactEvent.LockManipulationType lockManipulationType) {
        this.lockManipulationType = lockManipulationType;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        Address address = reader.getInit(header);
        address.read(buffer);
        return CompactEvent.lockManipulation(context, lockManipulationType, address.getAsLong());
    }
}
