package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.ReadableAggregateData;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;

import java.util.Arrays;

public class ThreadBeginReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                TraceElement::new,
                (context, compactEventReader, element) ->
                        compactEventReader.beginThread(
                                context, element.threadId.getAsLong(), element.generation.getAsLong()));
    }

    private static class TraceElement extends ReadableAggregateData {
        private final ThreadId threadId;
        private final Generation generation;

        private TraceElement(TraceHeader header) throws InvalidTraceDataException {
            threadId = new ThreadId(header);
            generation = new Generation(header);
            setData(Arrays.asList(threadId, generation));
        }
    }
}
