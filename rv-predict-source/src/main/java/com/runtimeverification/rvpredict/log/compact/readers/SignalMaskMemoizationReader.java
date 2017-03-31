package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMask;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.VariableInt;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class SignalMaskMemoizationReader implements CompactEvent.Reader {
    private final LazyInitializer<TraceElement> reader = new LazyInitializer<>(TraceElement::new);

    @Override
    public int size(TraceHeader header) throws InvalidTraceDataException {
        return reader.getInit(header).size();
    }

    @Override
    public List<CompactEvent> readEvent(Context context, TraceHeader header, ByteBuffer buffer)
            throws InvalidTraceDataException {
        TraceElement memoization = reader.getInit(header);
        memoization.read(buffer);
        return CompactEvent.signalMaskMemoization(
                context,
                memoization.signalMask.getAsLong(),
                memoization.originBitCount.getAsLong(),
                memoization.signalMaskNumber.getAsLong());
    }

    private class TraceElement extends ReadableAggregateData {
        private final SignalMask signalMask;
        private final VariableInt originBitCount;
        private final SignalMaskNumber signalMaskNumber;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            signalMask = new SignalMask(header);
            originBitCount = new VariableInt(header, 4);
            signalMaskNumber = new SignalMaskNumber(header);

            setData(Arrays.asList(signalMask, originBitCount, signalMaskNumber));
        }
    }
}