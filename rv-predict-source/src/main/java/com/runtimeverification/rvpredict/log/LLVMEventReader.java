package com.runtimeverification.rvpredict.log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

import com.runtimeverification.rvpredict.trace.BinaryParser;

/**
 * An EventReader specialized for LLVM
 *
 * @author EricPtS
 *
 */
public class LLVMEventReader implements IEventReader {

    private final BinaryParser in;
    private final int threadIndex;
    private static Iterator<Long> globalVarsIterator;
    private static int currentGlobalVar;
    private static int globalVarsNo;

    private Event lastReadEvent;

    public LLVMEventReader(int threadIndex, Path path) throws IOException {
        in = new BinaryParser(path);
        this.threadIndex = threadIndex;
        readEvent();
    }

    public static void setGlobalVars(Collection<Long> globalVars) {
        LLVMEventReader.globalVarsIterator = globalVars.iterator();
        globalVarsNo = globalVars.size();
        currentGlobalVar = 0;
    }

    @Override
    public final Event readEvent() throws IOException {
        if (threadIndex == 0 && globalVarsIterator.hasNext()) {
            lastReadEvent = new Event(currentGlobalVar, 1, 0, globalVarsIterator.next(), 0, EventType.WRITE);
            currentGlobalVar++;
            return lastReadEvent;
        }
        try {
            lastReadEvent = new Event(
                    in.readLong() + globalVarsNo,
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
    public Event lastReadEvent() {
        return this.lastReadEvent;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
