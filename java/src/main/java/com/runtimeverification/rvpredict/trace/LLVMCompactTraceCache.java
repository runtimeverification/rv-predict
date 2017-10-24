package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.CompactMetadata;

import java.io.EOFException;
import java.io.IOException;

public class LLVMCompactTraceCache  extends TraceCache {
    public LLVMCompactTraceCache(Configuration config, CompactMetadata metadata) {
        super(config, metadata);
    }

    @Override
    public void setup() throws IOException {
        try {
            readers.add(new CompactEventReader(config.getCompactTraceFilePath()));
        } catch (InvalidTraceDataException e) {
            throw new IOException(e);
        }
    }
}
