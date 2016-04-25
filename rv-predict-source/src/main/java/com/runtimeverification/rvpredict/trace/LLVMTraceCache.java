package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LLVMEventReader;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.*;
import java.nio.file.Path;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author EricPtS
 */
public class LLVMTraceCache extends TraceCache {
    private final Metadata metadata;
    private final String pid;

    public LLVMTraceCache(Configuration config, Metadata metadata, String pid, ForkPoint forkPoint) {
        super(config, metadata, forkPoint);
        this.metadata = metadata;
        this.pid = pid;
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
        BinaryParser in = new BinaryParser(config.getLLVMMetadataPath(pid + prefix).toFile());
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
                metadata.addThreadCreationInfo((long)args[0], (long)args[1], (int)args[2]);

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

    private void parseGlobalsInfo() throws IOException {
        parseInfo(new MetadataLogger() {
            @Override
            public void log(Object[] args) {
                metadata.registerGlobal((long)args[0]);
            }
        }, new BinaryReader() {
            @Override
            public Object[] read(BinaryParser in) throws IOException {
                Object[] args = new Object[1];
                args[0] = in.readLong();
                return args;
            }
        }, "global");
    }

    private void readMetadata() throws IOException {
        try {
            parseVarInfo();
        } catch (Metadata.TooManyVariables e) {
            config.logger().report("Maximum number of variables allowed (" + metadata.MAX_NUM_OF_VARIABLES +
                    ") exceeded.", Logger.MSGTYPE.ERROR);
        }
        parseGlobalsInfo();
        parseLocInfo();
        parseThdInfo();
    }

    @Override
    public void setup() throws IOException {
        readMetadata();
        int logId = 0;
        LLVMEventReader.setGlobalVars(metadata.getGlobalVars());
        Path path = config.getTraceFilePath(logId, pid);
        while(path.toFile().exists()) {
            readers.add(new LLVMEventReader(logId, path));
            ++logId;
            path = config.getTraceFilePath(logId, pid);
        }
    }
}
