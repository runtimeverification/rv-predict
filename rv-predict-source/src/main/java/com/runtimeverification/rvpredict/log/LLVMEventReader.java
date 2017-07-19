package com.runtimeverification.rvpredict.log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;

import com.runtimeverification.rvpredict.trace.BinaryParser;

/**
 * An EventReader specialized for LLVM
 *
 * @author EricPtS
 *
 */
public class LLVMEventReader implements IEventReader {

    private final BinaryParser in;

    private Event lastReadEvent;

    public LLVMEventReader(Path path) throws IOException {
        in = new BinaryParser(path);
        readEvent();
    }

    @Override
    public final ReadonlyEventInterface readEvent() throws IOException {
        try {
            lastReadEvent = new Event(
                    in.readLong(),
                    in.readLong(),
                    in.readInt(),
                    in.readLong(),
                    in.readLong(),
                    EventType.values()[in.readByte()]
                    );
        } catch (EOFException e) {
            lastReadEvent = null;
            throw e;
        }
        return lastReadEvent;
    }

    @Override
    public ReadonlyEventInterface lastReadEvent() {
        return this.lastReadEvent;
    }

    @Override
    public long bytesRead() {
        return in.bytesRead();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
