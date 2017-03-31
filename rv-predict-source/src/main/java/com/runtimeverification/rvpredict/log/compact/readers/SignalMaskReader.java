package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class SignalMaskReader implements CompactEventReader.Reader {
    private final LazyInitializer<SignalMaskNumber> reader = new LazyInitializer<>(SignalMaskNumber::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        SignalMaskNumber maskNumber = reader.getInit(header);
        maskNumber.read(buffer);
        return compactEventReader.signalMask(context, maskNumber.getAsLong());
    }
}