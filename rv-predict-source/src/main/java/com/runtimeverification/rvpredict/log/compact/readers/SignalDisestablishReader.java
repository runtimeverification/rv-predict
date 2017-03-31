package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class SignalDisestablishReader implements CompactEventReader.Reader {
    private final LazyInitializer<SignalNumber> signalNumber = new LazyInitializer<>(SignalNumber::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return signalNumber.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(
            Context context, CompactEventReader compactEventReader, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        SignalNumber signalNumber = this.signalNumber.getInit(header);
        signalNumber.read(buffer);
        return compactEventReader.disestablishSignal(context, signalNumber.getAsLong());
    }
}
