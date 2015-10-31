package com.runtimeverification.rvpredict.log;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;

import com.runtimeverification.rvpredict.trace.BinaryParser;

/**
 * An EventReader specialized for LLVM
 *
 * @author ericpts
 *
 */
public class LLVMEventReader implements Closeable {

    private BinaryParser in;

    private Event lastReadEvent;

    public LLVMEventReader(Path path) throws IOException {
        in = new BinaryParser(path);
        readEvent();
    }

    public final Event readEvent() throws IOException {
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

    public Event lastReadEvent() {
        return this.lastReadEvent;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
