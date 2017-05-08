package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LLVMEventReader;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author EricPtS
 */
public class LLVMTraceCache extends TraceCache {
    private final Metadata metadata;

    public LLVMTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }

    private interface MetadataLogger {
         public void log(Object[] args);
    }

    private class BinaryReader {
        public Object[] read(BinaryParser in) throws IOException {
            Object[] args= new Object[2];
            args[0] = in.readLong().intValue();
            args[1] = in.readString();
            return args;
        }
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

    private void parseVarInfo() throws IOException {
        parseInfo(new MetadataLogger() {
            @Override
            public void log(Object[] args) {
                metadata.setVariableSig((Integer)args[0], (String)args[1]);
            }
        }, new BinaryReader() {
        }, "var");
    }

    private void parseLocInfo() throws IOException {
        parseInfo(new MetadataLogger() {
            @Override
            public void log(Object[] args) {
                metadata.setLocationSig((Integer)args[0], (String)args[1]);
            }
        }, new BinaryReader()
         , "loc");
    }

    private void parseThdInfo() throws IOException {
        parseInfo(new MetadataLogger() {

            @Override
            public void log(Object[] args) {
                metadata.addOriginalThreadCreationInfo((long)args[0], (long)args[1], (int)args[2]);

            }
        }, new BinaryReader() {

            @Override
            public Object[] read(BinaryParser in) throws IOException {
                Object[] args = new Object[3];
                args[0] = in.readLong();
                args[1] = in.readLong();
                args[2] = in.readInt();
                return args;
            }
        }, "thd");
    }

    private void readMetadata() throws IOException {
        metadata.setIsCompactTrace(config.isCompactTrace());
        try {
            parseVarInfo();
        } catch (Metadata.TooManyVariables e) {
            config.logger().report("Maximum number of variables allowed (" + metadata.MAX_NUM_OF_VARIABLES +
                    ") exceeded.", Logger.MSGTYPE.ERROR);
        }
        parseLocInfo();
        parseThdInfo();
    }

    @Override
    public void setup() throws IOException {
        readMetadata();
        if (config.isCompactTrace()) {
            try {
                readers.add(new CompactEventReader(config.getCompactTraceFilePath()));
            } catch (InvalidTraceDataException e) {
                throw new IOException(e);
            }
            return;
        }
        int logId = 0;
        Path path = config.getTraceFilePath(logId);
        while(path.toFile().exists()) {
            readers.add(new LLVMEventReader(path));
            ++logId;
            path = config.getTraceFilePath(logId);
        }
    }
}
