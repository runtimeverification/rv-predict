package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;

public class JavaTraceCache extends TraceCache {
    private OptionalLong fileSize;

    public JavaTraceCache(Configuration config, MetadataInterface metadata) {
        super(config, metadata);
        fileSize = OptionalLong.empty();
    }

    @Override
    public void setup() throws IOException {
        int logFileId = 0;
        if (config.isCompactTrace()) {
            try {
                Path path = config.getCompactTraceFilePath();
                readers.add(new CompactEventReader(path));
                fileSize = OptionalLong.of(path.toFile().length());
            } catch (InvalidTraceDataException e) {
                throw new IOException(e);
            }
            return;
        }
        long size = 0;
        while (true) {
            Path path = config.getTraceFilePath(logFileId++);
            if (!path.toFile().exists()) {
                break;
            }
            size += path.toFile().length();
            readers.add(new EventReader(path));
        }
        fileSize = OptionalLong.of(size);
    }

    @Override
    public long getFileSize() {
        assert fileSize.isPresent();
        return fileSize.getAsLong();
    }

}
