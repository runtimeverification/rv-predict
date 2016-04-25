package com.runtimeverification.rvpredict.log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.BinaryParser;

/**
 * An EventReader specialized for LLVM
 *
 * @author EricPtS
 *
 */
public class LLVMEventReader implements IEventReader {

    private final BinaryParser in;
    private final int logId;
    private static List<Long> globalVars;
    private static int currentGlobalVar;

    private Event lastReadEvent;

    public LLVMEventReader(int logId, Path path) throws IOException {
        in = new BinaryParser(path);
        this.logId = logId;
        readEvent();
    }

    public static void setGlobalVars(List<Long> globalVars) {
        LLVMEventReader.globalVars = globalVars;
        currentGlobalVar = 0;
    }

    @Override
    public final Event readEvent() throws IOException {
        if (logId == 0 && currentGlobalVar < globalVars.size()) {
            /* Currently we "initialize" all globals as the first events in the main thread (logId = 0).
               The main reason for this approach is that the information that a location is global is currently recorded
               only when the location is first accessed, and the initialization does not count as an access.
            */
            lastReadEvent = new Event(currentGlobalVar, 1, 1, globalVars.get(currentGlobalVar), 0, EventType.WRITE);
            currentGlobalVar++;
            return lastReadEvent;
        }
        try {
            lastReadEvent = new Event(
                    in.readLong() + globalVars.size(),
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
        if (Configuration.debug) {
            String object = null;
            if (lastReadEvent.isSyncEvent()) {
                object = Metadata.singleton().getVariableSig((int)lastReadEvent.getSyncObject());
            } else if (lastReadEvent.isReadOrWrite()){
                 object = Metadata.singleton().getVariableSig(lastReadEvent.getObjectHashCode());
            } else if (lastReadEvent.isStart() || lastReadEvent.isJoin()) {
                object = "Thread " + lastReadEvent.getSyncedThreadId();
            }
            System.out.format("#%d:%d %s%s at %s%n", lastReadEvent.getTID(), lastReadEvent.getGID(), lastReadEvent.getType().toString(), object != null ? " of " + object: "", Metadata.singleton().getLocationSig(lastReadEvent.getLocId()));
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
