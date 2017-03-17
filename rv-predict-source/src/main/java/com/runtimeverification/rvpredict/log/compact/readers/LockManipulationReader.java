package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.Address;
import com.runtimeverification.rvpredict.log.compact.Event;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;

public class LockManipulationReader implements Event.Reader {
    private final Event.LockManipulationType lockManipulationType;
    private ReaderInitializer<Address> reader = new ReaderInitializer<>(Address::new);

    public LockManipulationReader(Event.LockManipulationType lockManipulationType) {
        this.lockManipulationType = lockManipulationType;
    }

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public Event readEvent(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        Address address = reader.getInit(header);
        address.read(buffer);
        return Event.lockManipulation(lockManipulationType, address.getAsLong());
    }
}
