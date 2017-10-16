package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;

import java.nio.ByteBuffer;
import java.util.List;

public class ReaderUtils {
    public static List<ReadonlyEventInterface> readSimpleEvent(
            CompactEventReader.Reader reader,
            Context context,
            CompactEventFactory compactEventFactory,
            TraceHeader header,
            ByteBuffer buffer) throws InvalidTraceDataException {
        while (reader.stillHasPartsToRead()) {
            reader.addPart(buffer, header);
        }
        return reader.build(context, compactEventFactory, header);
    }
    public static int firstPartSize(
            CompactEventReader.Reader reader, TraceHeader header) throws InvalidTraceDataException {
        return reader.nextPartSize(header);
    }
}
