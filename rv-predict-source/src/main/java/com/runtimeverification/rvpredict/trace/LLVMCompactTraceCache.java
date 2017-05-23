package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.CompactMetadata;

import java.io.EOFException;
import java.io.IOException;

public class LLVMCompactTraceCache  extends TraceCache {
    private final CompactMetadata metadata;

    public LLVMCompactTraceCache(Configuration config, CompactMetadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }

    @Override
    public void setup() throws IOException {
        readMetadata();
        try {
            readers.add(new CompactEventReader(config.getCompactTraceFilePath()));
        } catch (InvalidTraceDataException e) {
            throw new IOException(e);
        }
    }

    private void readMetadata() throws IOException {
        parseVarInfo();
        parseLocInfo();
        parseThdInfo();
    }

    private void parseVarInfo() throws IOException {
        parseInfo(
                args -> {},
                new BinaryReader() {},
                "var");
    }

    private void parseLocInfo() throws IOException {
        parseInfo(
                args -> {},
                new BinaryReader(),
                "loc");
    }

    private void parseThdInfo() throws IOException {
        parseInfo(
                args -> metadata.addOriginalThreadCreationInfo((long) args[0], (long) args[1], (int) args[2]),
                new BinaryReader() {
                    @Override
                    public Object[] read(BinaryParser in) throws IOException {
                        Object[] args = new Object[3];
                        args[0] = in.readLong();
                        args[1] = in.readLong();
                        args[2] = in.readInt();
                        return args;
                    }
                },
                "thd");
    }

    private void parseInfo(MetadataLogger logger, BinaryReader reader, String prefix) throws IOException {
        BinaryParser in = new BinaryParser(config.getLLVMMetadataPath(prefix).toFile());
        while(true) {
            try {
                Object[] args = reader.read(in);
                logger.log(args);
            } catch (EOFException e) {
                break;
            }
        }
        in.close();
    }

    private interface MetadataLogger {
        void log(Object[] args);
    }

    private class BinaryReader {
        public Object[] read(BinaryParser in) throws IOException {
            Object[] args= new Object[2];
            args[0] = in.readLong().intValue();
            args[1] = in.readString();
            return args;
        }
    }
}
