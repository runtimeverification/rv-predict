package com.runtimeverification.rvpredict.trace;

import com.google.common.annotations.VisibleForTesting;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.IEventReader;

import java.io.IOException;
import java.util.List;

@VisibleForTesting
public class TestTraceCache extends TraceCache {
    private TestTraceCache(
            Configuration config, TraceState traceState, LockGraph lockGraph, List<IEventReader> defaultReaders) {
        super(config, traceState, lockGraph, defaultReaders);
    }

    @VisibleForTesting
    static TraceCache createForTesting(
            Configuration config, TraceState traceState, LockGraph lockGraph, List<IEventReader> readers) {
        return new TestTraceCache(config, traceState, lockGraph, readers);
    }

    @Override
    public void setup() throws IOException {

    }

    @Override
    public long getFileSize() {
        return 0;
    }
}
