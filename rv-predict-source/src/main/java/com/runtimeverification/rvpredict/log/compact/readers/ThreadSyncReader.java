package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;

public class ThreadSyncReader {
    public static CompactEventReader.Reader createReader(CompactEventReader.ThreadSyncType threadSyncType) {
        return new SimpleDataReader<>(
                ThreadId::new,
                (context, compactEventFactory, threadId) ->
                        compactEventFactory.threadSync(context, threadSyncType, threadId.getAsLong()));
    }
}
