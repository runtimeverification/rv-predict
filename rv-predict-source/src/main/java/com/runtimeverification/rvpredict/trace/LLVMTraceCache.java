package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LLVMEventReader;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author EricPtS
 */
public class LLVMTraceCache extends TraceCache {
    private final Metadata metadata;
    private OptionalLong fileSize;

    public LLVMTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
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
        parseInfo(args -> metadata.setVariableSig((Integer)args[0], (String)args[1]), new BinaryReader() {
        }, "var");
    }

    private void parseLocInfo() throws IOException {
        parseInfo(args -> metadata.setLocationSig((Integer)args[0], (String)args[1]), new BinaryReader()
         , "loc");
    }

    private void parseThdInfo() throws IOException {
        parseInfo(args -> metadata.addOriginalThreadCreationInfo((long)args[0], (long)args[1], (int)args[2]), new BinaryReader() {

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
        try {
            parseVarInfo();
        } catch (Metadata.TooManyVariables e) {
            config.logger().report("Maximum number of variables allowed (" + Metadata.MAX_NUM_OF_VARIABLES +
                    ") exceeded.", Logger.MSGTYPE.ERROR);
        }
        parseLocInfo();
        parseThdInfo();
    }

    @Override
    public void setup() throws IOException {
        readMetadata();
        int logId = 0;
        Path path = config.getTraceFilePath(logId);
        long size = 0;
        while(path.toFile().exists()) {
            size += path.toFile().length();
            readers.add(new LLVMEventReader(path));
            ++logId;
            path = config.getTraceFilePath(logId);
        }
        fileSize = OptionalLong.of(size);
    }

    @Override
    public long getFileSize() {
        assert fileSize.isPresent();
        return fileSize.getAsLong();
    }
}
