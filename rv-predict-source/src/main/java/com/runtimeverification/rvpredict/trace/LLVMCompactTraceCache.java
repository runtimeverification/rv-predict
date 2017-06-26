package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.CompactMetadata;

import java.io.IOException;
import java.util.OptionalLong;

public class LLVMCompactTraceCache  extends TraceCache {
    private OptionalLong fileSize;
    public LLVMCompactTraceCache(Configuration config, CompactMetadata metadata) {
        super(config, metadata);
        fileSize = OptionalLong.empty();
    }

    @Override
    public void setup() throws IOException {
        try {
            readers.add(new CompactEventReader(config.getCompactTraceFilePath()));
            fileSize = OptionalLong.of(config.getCompactTraceFilePath().toFile().length());
        } catch (InvalidTraceDataException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getFileSize() {
        assert fileSize.isPresent();
        return fileSize.getAsLong();
    }
}
